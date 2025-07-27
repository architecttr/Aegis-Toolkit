package com.d4rk.cleaner.app.clean.scanner.domain.operations

import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.DeleteFilesUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.MoveToTrashUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.UpdateTrashSizeUseCase
import com.d4rk.cleaner.core.domain.model.network.Errors
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Coordinates cleaning related operations such as deleting files or
 * moving them to trash. This keeps the ViewModel focused on UI logic.
 */
class CleaningManager(
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val moveToTrashUseCase: MoveToTrashUseCase,
    private val updateTrashSizeUseCase: UpdateTrashSizeUseCase,
) {
    suspend fun deleteFiles(files: Set<File>): DataState<Unit, Errors> {
        return deleteFilesUseCase(files).first()
    }

    suspend fun moveToTrash(files: List<File>): DataState<Unit, Errors> {
        return moveToTrashUseCase(files).first()
    }

    suspend fun updateTrashSize(sizeChange: Long): DataState<Unit, Errors> {
        return updateTrashSizeUseCase(sizeChange).first()
    }
}
