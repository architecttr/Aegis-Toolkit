package com.d4rk.cleaner.app.clean.whatsapp.summary.domain.usecases

import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.repository.WhatsAppCleanerRepository
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.model.DeleteResult
import com.d4rk.cleaner.core.domain.model.network.Errors
import com.d4rk.cleaner.core.utils.extensions.toError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class DeleteWhatsAppMediaUseCase(private val repository: WhatsAppCleanerRepository) {
    operator fun invoke(files: List<File>): Flow<DataState<DeleteResult, Errors>> = flow {
        runCatching { repository.deleteFiles(files) }
            .onSuccess { emit(DataState.Success(it)) }
            .onFailure { emit(DataState.Error(error = it.toError())) }
    }
}
