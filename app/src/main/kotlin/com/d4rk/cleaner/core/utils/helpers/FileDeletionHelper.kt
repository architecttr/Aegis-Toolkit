package com.d4rk.cleaner.core.utils.helpers

import java.io.File

/**
 * Deletes files using native [File.deleteRecursively].
 * Returns [FileDeletionResult] for each input file describing success or failure.
 */
data class FileDeletionResult(val file: File, val success: Boolean)

object FileDeletionHelper {
    fun deleteFiles(files: Collection<File>): List<FileDeletionResult> {
        val results = mutableListOf<FileDeletionResult>()

        files.forEach { file ->
            val deleted = runCatching {
                if (file.exists()) file.deleteRecursively() else false
            }.getOrDefault(false)
            results.add(FileDeletionResult(file, deleted))
        }

        return results
    }
}

