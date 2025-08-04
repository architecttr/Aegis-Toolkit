package com.d4rk.cleaner.app.clean.largefiles.ui

import android.app.Application
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.ScreenState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiSnackbar
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.updateData
import com.d4rk.android.libs.apptoolkit.core.ui.base.ScreenViewModel
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.UiTextHelper
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.largefiles.domain.actions.LargeFilesAction
import com.d4rk.cleaner.app.clean.largefiles.domain.actions.LargeFilesEvent
import com.d4rk.cleaner.app.clean.largefiles.domain.data.model.ui.UiLargeFilesModel
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetLargestFilesUseCase
import com.d4rk.cleaner.app.clean.scanner.work.FileCleanupWorker
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.utils.helpers.FileGroupingHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID

class LargeFilesViewModel(
    private val application: Application,
    private val getLargestFilesUseCase: GetLargestFilesUseCase,
    private val dataStore: DataStore,
    private val dispatchers: DispatcherProvider
) : ScreenViewModel<UiLargeFilesModel, LargeFilesEvent, LargeFilesAction>(
    initialState = UiStateScreen(data = UiLargeFilesModel())
) {

    private val limit = 20

    private var activeWorkObserver: Job? = null

    init {
        onEvent(LargeFilesEvent.LoadLargeFiles)
        launch(dispatchers.io) {
            dataStore.largeFilesCleanWorkId.first()?.let { id ->
                val uuid = UUID.fromString(id)
                val info = WorkManager.getInstance(application)
                    .getWorkInfoByIdFlow(uuid)
                    .first()
                if (info == null || info.state.isFinished) {
                    dataStore.clearLargeFilesCleanWorkId()
                } else {
                    observeWork(uuid)
                }
            }
        }
    }

    override fun onEvent(event: LargeFilesEvent) {
        when (event) {
            LargeFilesEvent.LoadLargeFiles -> loadLargeFiles()
            is LargeFilesEvent.OnFileSelectionChange -> onFileSelectionChange(
                event.file,
                event.isChecked
            )

            LargeFilesEvent.DeleteSelectedFiles -> deleteSelected()
        }
    }

    private fun loadLargeFiles() {
        launch(context = dispatchers.io) {
            getLargestFilesUseCase(limit).collectLatest { result ->
                _uiState.update { current ->
                    when (result) {
                        is DataState.Loading -> current.copy(screenState = ScreenState.IsLoading())
                        is DataState.Success -> {
                            val groupedByDate = FileGroupingHelper.groupFilesByDate(result.data)
                            current.copy(
                                screenState = if (result.data.isEmpty()) ScreenState.NoData() else ScreenState.Success(),
                                data = current.data?.copy(
                                    files = result.data,
                                    filesByDate = groupedByDate,
                                    fileSelectionStates = emptyMap(),
                                    selectedFileCount = 0
                                )
                                    ?: UiLargeFilesModel(files = result.data, filesByDate = groupedByDate)
                            )
                        }

                        is DataState.Error -> current.copy(
                            screenState = ScreenState.Error(),
                            errors = current.errors + UiSnackbar(
                                message = UiTextHelper.DynamicString("${result.error}"),
                                isError = true
                            )
                        )
                    }
                }
            }
        }
    }

    private fun onFileSelectionChange(file: File, isChecked: Boolean) {
        _uiState.updateData(newState = _uiState.value.screenState) { current ->
            val updated = current.fileSelectionStates.toMutableMap()
                .apply { this[file.absolutePath] = isChecked }
            current.copy(
                fileSelectionStates = updated,
                selectedFileCount = updated.count { it.value }
            )
        }
    }

    private fun deleteSelected() {
        launch(context = dispatchers.io) {
            val filePaths =
                _uiState.value.data?.fileSelectionStates?.filter { it.value }?.keys ?: emptySet()
            val files = filePaths.map { File(it) }.toSet()
            if (files.isEmpty()) {
                sendAction(
                    LargeFilesAction.ShowSnackbar(
                        UiSnackbar(
                            message = UiTextHelper.DynamicString("No files selected")
                        )
                    )
                )
                return@launch
            }

            val activeId = dataStore.largeFilesCleanWorkId.first()
            if (activeId != null) {
                val info = WorkManager.getInstance(application)
                    .getWorkInfoByIdFlow(UUID.fromString(activeId))
                    .first()
                if (info != null && !info.state.isFinished) {
                    sendAction(
                        LargeFilesAction.ShowSnackbar(
                            UiSnackbar(message = UiTextHelper.StringResource(R.string.cleaning_in_progress))
                        )
                    )
                    return@launch
                } else {
                    dataStore.clearLargeFilesCleanWorkId()
                }
            }

            val workManager = WorkManager.getInstance(application)
            val chunks = filePaths.toList().chunked(FileCleanupWorker.MAX_PATHS_PER_WORKER)
            var continuation: androidx.work.WorkContinuation? = null
            var lastRequest: androidx.work.OneTimeWorkRequest? = null
            for (chunk in chunks) {
                val request = OneTimeWorkRequestBuilder<FileCleanupWorker>()
                    .setInputData(
                        workDataOf(
                            FileCleanupWorker.KEY_ACTION to FileCleanupWorker.ACTION_DELETE,
                            FileCleanupWorker.KEY_PATHS to chunk.toTypedArray()
                        )
                    ).build()
                lastRequest = request
                continuation = continuation?.then(request) ?: workManager.beginWith(request)
            }

            val finalRequest = lastRequest

            runCatching {
                continuation?.enqueue()
                finalRequest?.let { req ->
                    dataStore.saveLargeFilesCleanWorkId(req.id.toString())
                    observeWork(req.id)
                }
                sendAction(
                    LargeFilesAction.ShowSnackbar(
                        UiSnackbar(message = UiTextHelper.StringResource(R.string.cleaning_in_progress))
                    )
                )
            }.onFailure {
                finalRequest?.let { req -> workManager.cancelWorkById(req.id) }
                sendAction(
                    LargeFilesAction.ShowSnackbar(
                        UiSnackbar(
                            message = UiTextHelper.StringResource(R.string.failed_to_delete_files),
                            isError = true
                        )
                    )
                )
            }
        }
    }
    private fun observeWork(id: UUID) {
        activeWorkObserver?.cancel()
        activeWorkObserver = launch(dispatchers.io) {
            WorkManager.getInstance(application).getWorkInfoByIdFlow(id).collect { info ->
                when (info?.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                        _uiState.update { it.copy(screenState = ScreenState.IsLoading()) }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        dataStore.clearLargeFilesCleanWorkId()

                        val failedPaths =
                            info.outputData.getStringArray(FileCleanupWorker.KEY_FAILED_PATHS)
                        val failedCount = failedPaths?.size ?: 0
                        val selectedCount =
                            _uiState.value.data?.fileSelectionStates?.count { it.value } ?: 0
                        val successCount = selectedCount - failedCount

                        onEvent(LargeFilesEvent.LoadLargeFiles)

                        val message = if (failedCount > 0) {
                            UiTextHelper.StringResource(
                                R.string.cleanup_partial,
                                listOf(successCount, failedCount)
                            )
                        } else {
                            UiTextHelper.StringResource(R.string.all_clean)
                        }

                        sendAction(
                            LargeFilesAction.ShowSnackbar(
                                UiSnackbar(message = message)
                            )
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        dataStore.clearLargeFilesCleanWorkId()
                        _uiState.update {
                            it.copy(
                                screenState = ScreenState.Error(),
                                errors = it.errors + UiSnackbar(
                                    message = UiTextHelper.StringResource(R.string.failed_to_delete_files),
                                    isError = true
                                )
                            )
                        }
                    }
                    WorkInfo.State.CANCELLED -> {
                        dataStore.clearLargeFilesCleanWorkId()
                        _uiState.update { it.copy(screenState = ScreenState.Success()) }
                    }
                    null -> {
                        dataStore.clearLargeFilesCleanWorkId()
                        _uiState.update { it.copy(screenState = ScreenState.Success()) }
                    }
                }
            }
        }
    }
}
