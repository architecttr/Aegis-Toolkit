package com.d4rk.cleaner.app.clean.scanner.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.ScreenState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiSnackbar
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.applyResult
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.UiTextHelper
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningType
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileEntry
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.app.clean.scanner.domain.operations.CleaningManager
import com.d4rk.cleaner.app.clean.scanner.domain.operations.FileAnalyzer
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.AnalyzeFilesUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetEmptyFoldersUseCase
import com.d4rk.cleaner.app.clean.scanner.services.FileOperationService
import com.d4rk.cleaner.app.settings.cleaning.utils.constants.ExtensionsConstants
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.domain.model.network.Errors
import com.d4rk.cleaner.core.utils.helpers.CleaningEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Encapsulates heavy cleaning operations so the ViewModel stays lean.
 */
class CleanOperationHandler(
    private val application: Application,
    private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val dataStore: DataStore,
    private val analyzeFilesUseCase: AnalyzeFilesUseCase,
    private val getEmptyFoldersUseCase: GetEmptyFoldersUseCase,
    private val cleaningManager: CleaningManager,
    private val fileAnalyzer: FileAnalyzer,
    private val uiState: MutableStateFlow<UiStateScreen<UiScannerModel>>,
    private val loadInitialData: () -> Unit,
    private val loadWhatsAppMedia: () -> Unit,
    private val loadClipboardData: () -> Unit,
    private val loadEmptyFoldersPreview: () -> Unit,
    private val postSnackbar: (UiTextHelper, Boolean) -> Unit,
    private val updateTrashSize: (Long) -> Unit,
) {
    private val resultDelayMs = 3600L

    fun analyzeFiles() {
        if (uiState.value.data?.analyzeState?.state != CleaningState.Idle) {
            return
        }
        scope.launch(dispatchers.io) {
            val scannedFiles = mutableListOf<File>()
            val emptyFolders = mutableListOf<File>()

            analyzeFilesUseCase().collect { result ->
                uiState.update { currentState ->
                    val currentData: UiScannerModel = currentState.data ?: UiScannerModel()
                    when (result) {
                        is DataState.Loading -> currentState.copy(
                            screenState = ScreenState.IsLoading(),
                            data = currentData.copy(
                                analyzeState = currentData.analyzeState.copy(
                                    state = CleaningState.Analyzing,
                                    cleaningType = CleaningType.NONE
                                )
                            )
                        )
                        is DataState.Success -> {
                            scannedFiles.add(result.data)
                            currentState
                        }
                        is DataState.Error -> currentState.copy(
                            screenState = ScreenState.Error(),
                            data = currentData.copy(
                                analyzeState = currentData.analyzeState.copy(
                                    state = CleaningState.Error,
                                    cleaningType = CleaningType.NONE
                                )
                            ),
                            errors = currentState.errors + UiSnackbar(
                                message = UiTextHelper.StringResource(R.string.failed_to_analyze_files),
                                isError = true
                            )
                        )
                    }
                }
            }

            getEmptyFoldersUseCase().collect { result: DataState<File, Errors> ->
                if (result is DataState.Success) {
                    emptyFolders.add(result.data)
                }
            }

            val currentData = uiState.value.data ?: UiScannerModel()
            val fileTypesData = currentData.analyzeState.fileTypesData
            val preferences = mapOf(
                ExtensionsConstants.GENERIC_EXTENSIONS to dataStore.genericFilter.first(),
                ExtensionsConstants.IMAGE_EXTENSIONS to dataStore.deleteImageFiles.first(),
                ExtensionsConstants.VIDEO_EXTENSIONS to dataStore.deleteVideoFiles.first(),
                ExtensionsConstants.AUDIO_EXTENSIONS to dataStore.deleteAudioFiles.first(),
                ExtensionsConstants.OFFICE_EXTENSIONS to dataStore.deleteOfficeFiles.first(),
                ExtensionsConstants.ARCHIVE_EXTENSIONS to dataStore.deleteArchives.first(),
                ExtensionsConstants.APK_EXTENSIONS to dataStore.deleteApkFiles.first(),
                ExtensionsConstants.FONT_EXTENSIONS to dataStore.deleteFontFiles.first(),
                ExtensionsConstants.WINDOWS_EXTENSIONS to dataStore.deleteWindowsFiles.first(),
                ExtensionsConstants.EMPTY_FOLDERS to dataStore.deleteEmptyFolders.first(),
                ExtensionsConstants.OTHER_EXTENSIONS to dataStore.deleteOtherFiles.first()
            )

            val includeDuplicates = dataStore.deleteDuplicateFiles.first() &&
                    dataStore.duplicateScanEnabled.first()
            val (groupedFiles, duplicateOriginals, duplicateGroups) =
                withContext(dispatchers.io) {
                    fileAnalyzer.computeGroupedFiles(
                        scannedFiles = scannedFiles,
                        emptyFolders = emptyFolders,
                        fileTypesData = fileTypesData,
                        preferences = preferences,
                        includeDuplicates = includeDuplicates
                    )
                }

            uiState.update { state ->
                val data = state.data ?: UiScannerModel()
                state.copy(
                    screenState = ScreenState.Success(),
                    data = data.copy(
                        analyzeState = data.analyzeState.copy(
                            scannedFileList = scannedFiles.map {
                                FileEntry(it.absolutePath, it.length(), it.lastModified())
                            },
                            emptyFolderList = emptyFolders.map {
                                FileEntry(it.absolutePath, 0, it.lastModified())
                            },
                            groupedFiles = groupedFiles,
                            duplicateOriginals = duplicateOriginals,
                            duplicateGroups = duplicateGroups,
                            state = CleaningState.ReadyToClean,
                            cleaningType = CleaningType.NONE
                        )
                    )
                )
            }
        }
    }

    fun cleanFiles(screenData: UiScannerModel?) {
        if (uiState.value.data?.analyzeState?.state != CleaningState.ReadyToClean) {
            return
        }

        scope.launch(dispatchers.io) {
            ContextCompat.startForegroundService(
                application,
                Intent(application, FileOperationService::class.java)
            )
            try {
            uiState.update { state ->
                val currentData = state.data ?: UiScannerModel()
                state.copy(
                    data = currentData.copy(
                        analyzeState = currentData.analyzeState.copy(
                            state = CleaningState.Cleaning,
                            cleaningType = CleaningType.DELETE
                        )
                    )
                )
            }

            val currentScreenData: UiScannerModel = screenData ?: run {
                postSnackbar(UiTextHelper.StringResource(R.string.data_not_available), true)
                return@launch
            }

            val selectedPaths: Set<String> = currentScreenData.analyzeState.selectedFiles
            val filesToDelete: Set<File> = selectedPaths.map { File(it) }.toSet()
            if (filesToDelete.isEmpty()) {
                postSnackbar(UiTextHelper.StringResource(R.string.no_files_selected_to_clean), false)
                return@launch
            }

            val includeDuplicates = dataStore.deleteDuplicateFiles.first() &&
                    dataStore.duplicateScanEnabled.first()
            val result = cleaningManager.deleteFiles(filesToDelete)
            uiState.applyResult(
                result = result,
                errorMessage = UiTextHelper.StringResource(R.string.failed_to_delete_files),
            ) { _: Unit, currentData: UiScannerModel ->
                    val (groupedFilesUpdated, duplicateOriginals, duplicateGroups) = fileAnalyzer.computeGroupedFiles(
                        scannedFiles = currentData.analyzeState.scannedFileList.filterNot { it.path in selectedPaths }
                            .map { it.toFile() },
                        emptyFolders = currentData.analyzeState.emptyFolderList.map { it.toFile() },
                        fileTypesData = currentData.analyzeState.fileTypesData,
                        preferences = mapOf(),
                        includeDuplicates = includeDuplicates
                    )

                    currentData.copy(
                        analyzeState = currentData.analyzeState.copy(
                            scannedFileList = currentData.analyzeState.scannedFileList.filterNot { it.path in selectedPaths },
                            groupedFiles = groupedFilesUpdated,
                            duplicateOriginals = duplicateOriginals,
                            duplicateGroups = duplicateGroups,
                            selectedFilesCount = 0,
                            areAllFilesSelected = false,
                            selectedFiles = mutableSetOf(),
                            isAnalyzeScreenVisible = false
                        ),
                        storageInfo = currentData.storageInfo.copy(
                            isFreeSpaceLoading = true,
                            isCleanedSpaceLoading = true
                        )
                    )
                }

            if (result is DataState.Success) {
                    delay(resultDelayMs)
                    uiState.update { state ->
                        val current = state.data ?: UiScannerModel()
                        state.copy(
                            data = current.copy(
                                analyzeState = current.analyzeState.copy(
                                    state = CleaningState.Result
                                )
                            )
                        )
                    }
                    scope.launch { dataStore.saveLastScanTimestamp(timestamp = System.currentTimeMillis()) }
                    loadInitialData()
                    loadWhatsAppMedia()
                    loadClipboardData()
                    loadEmptyFoldersPreview()
                    CleaningEventBus.notifyCleaned()
            } else if (result is DataState.Error) {
                    uiState.update { s ->
                        val currentErrorData = s.data ?: UiScannerModel()
                        s.copy(
                            data = currentErrorData.copy(
                                analyzeState = currentErrorData.analyzeState.copy(
                                    state = CleaningState.Error
                                )
                            )
                        )
                    }
            }
            } finally {
                application.stopService(Intent(application, FileOperationService::class.java))
            }
        }
    }

    fun moveToTrash(files: List<FileEntry>) {
        scope.launch(dispatchers.io) {
            ContextCompat.startForegroundService(
                application,
                Intent(application, FileOperationService::class.java)
            )
            try {
                if (files.isEmpty()) {
                    postSnackbar(UiTextHelper.StringResource(R.string.no_files_selected_move_to_trash), false)
                    return@launch
                }

                uiState.update { state: UiStateScreen<UiScannerModel> ->
                    val currentData: UiScannerModel = state.data ?: UiScannerModel()
                    state.copy(
                        data = currentData.copy(
                            analyzeState = currentData.analyzeState.copy(
                                state = CleaningState.Cleaning,
                                cleaningType = CleaningType.MOVE_TO_TRASH
                            )
                        )
                    )
                }

                val fileObjs = files.map { it.toFile() }
                val totalFileSizeToMove: Long = fileObjs.sumOf { it.length() }

                val includeDuplicates = dataStore.deleteDuplicateFiles.first() &&
                        dataStore.duplicateScanEnabled.first()
                val result = cleaningManager.moveToTrash(fileObjs)
                uiState.applyResult(
                    result = result,
                    errorMessage = UiTextHelper.StringResource(R.string.failed_to_move_files_to_trash),
                ) { _: Unit, currentData: UiScannerModel ->
                    val (groupedFilesUpdated2, duplicateOriginals2, duplicateGroups2) = fileAnalyzer.computeGroupedFiles(
                        scannedFiles = currentData.analyzeState.scannedFileList.filterNot { existingFile ->
                            files.any { moved -> existingFile.path == moved.path }
                        }.map { it.toFile() },
                        emptyFolders = currentData.analyzeState.emptyFolderList.map { it.toFile() },
                        fileTypesData = currentData.analyzeState.fileTypesData,
                        preferences = mapOf(),
                        includeDuplicates = includeDuplicates
                    )

                    currentData.copy(
                        analyzeState = currentData.analyzeState.copy(
                            scannedFileList = currentData.analyzeState.scannedFileList.filterNot { existingFile ->
                                files.any { moved -> existingFile.path == moved.path }
                            },
                            groupedFiles = groupedFilesUpdated2,
                            duplicateOriginals = duplicateOriginals2,
                            duplicateGroups = duplicateGroups2,
                            selectedFilesCount = 0,
                            areAllFilesSelected = false,
                            isAnalyzeScreenVisible = false,
                            selectedFiles = mutableSetOf()
                        )
                    )
                }

                if (result is DataState.Success) {
                    delay(resultDelayMs)
                    uiState.update { state ->
                        val current = state.data ?: UiScannerModel()
                        state.copy(
                            data = current.copy(
                                analyzeState = current.analyzeState.copy(
                                    state = CleaningState.Result
                                )
                            )
                        )
                    }
                    updateTrashSize(totalFileSizeToMove)
                    loadInitialData()
                    loadWhatsAppMedia()
                    loadClipboardData()
                    loadEmptyFoldersPreview()
                    CleaningEventBus.notifyCleaned()
                }
            } finally {
                application.stopService(Intent(application, FileOperationService::class.java))
            }
        }
    }
}
