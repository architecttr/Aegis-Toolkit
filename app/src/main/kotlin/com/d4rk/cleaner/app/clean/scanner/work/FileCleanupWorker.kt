package com.d4rk.cleaner.app.clean.scanner.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.cleaner.app.clean.scanner.domain.operations.CleaningManager
import com.d4rk.cleaner.core.utils.helpers.CleaningEventBus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class FileCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val cleaningManager: CleaningManager by inject()

    override suspend fun doWork(): Result {
        val paths = inputData.getStringArray(KEY_PATHS)?.toList() ?: return Result.success()
        val action = inputData.getString(KEY_ACTION) ?: ACTION_DELETE
        val files = paths.map { File(it) }
        return when (action) {
            ACTION_DELETE -> handleDelete(files.toSet())
            ACTION_TRASH -> handleMoveToTrash(files)
            else -> Result.success()
        }
    }

    private suspend fun handleDelete(files: Set<File>): Result {
        return when (val result = cleaningManager.deleteFiles(files)) {
            is DataState.Success -> {
                CleaningEventBus.notifyCleaned()
                Result.success()
            }
            is DataState.Error -> Result.failure(
                Data.Builder().putString(KEY_ERROR, result.error.toString()).build()
            )
            else -> Result.success()
        }
    }

    private suspend fun handleMoveToTrash(files: List<File>): Result {
        return when (val result = cleaningManager.moveToTrash(files)) {
            is DataState.Success -> {
                CleaningEventBus.notifyCleaned()
                Result.success()
            }
            is DataState.Error -> Result.failure(
                Data.Builder().putString(KEY_ERROR, result.error.toString()).build()
            )
            else -> Result.success()
        }
    }

    companion object {
        const val KEY_PATHS = "paths"
        const val KEY_ACTION = "action"
        const val KEY_ERROR = "error"
        const val ACTION_DELETE = "delete"
        const val ACTION_TRASH = "trash"
    }
}
