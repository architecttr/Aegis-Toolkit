package com.d4rk.cleaner.core.utils.helpers

import com.d4rk.cleaner.core.utils.extensions.deleteRecursivelySafe
import java.io.File

/**
 * Deletes files using a single safe recursive delete call.
 * Returns [FileDeletionResult] for each input file describing success or failure.
 */
data class FileDeletionResult(val file: File, val success: Boolean)

object FileDeletionHelper {
    fun deleteFiles(files: Collection<File>): List<FileDeletionResult> {
        val results = mutableListOf<FileDeletionResult>()

        files.forEach { file ->
            val deleted = if (file.exists()) {
                file.deleteRecursivelySafe()
            } else {
                false
            }
            results.add(FileDeletionResult(file, deleted))
        }

        return results
    }
}

