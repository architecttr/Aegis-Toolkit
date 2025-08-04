package com.d4rk.cleaner.core.utils.helpers

import android.app.Application
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Deletes files using native deletion first and falling back to SAF when needed.
 * Returns [FileDeletionResult] for each input file describing success or failure.
 */
data class FileDeletionResult(val file: File, val success: Boolean)

object FileDeletionHelper {
    fun deleteFiles(application: Application, files: Collection<File>): List<FileDeletionResult> {
        val androidDir = Environment.getExternalStorageDirectory().absolutePath + "/Android"
        val results = mutableListOf<FileDeletionResult>()

        files.forEach { file ->
            if (!file.exists()) {
                results.add(FileDeletionResult(file, false))
                return@forEach
            }

            val hasManage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager()
            var usingSaf = !hasManage ||
                file.absolutePath.startsWith(androidDir) || !file.canWrite()
            var deleted = false

            if (!usingSaf) {
                val success = file.deleteRecursively()
                if (success) {
                    println("Deletion method: Native → path: ${file.path}")
                    deleted = true
                } else {
                    println("deleteRecursively failed for ${file.path}")
                    usingSaf = true
                    println("Fallback to SAF for ${file.path}")
                }
            }

            if (usingSaf && !deleted) {
                val safResult = deleteWithSaf(application, file)
                println("Deletion method: SAF → path: ${file.path}")
                println("DocumentFile.delete() success: $safResult for ${file.path}")
                deleted = safResult
            }

            if (!deleted) {
                println("Deletion failed for ${file.path}")
            }

            results.add(FileDeletionResult(file, deleted))
        }

        return results
    }

    private fun deleteWithSaf(application: Application, target: File): Boolean {
        val absPath = target.absolutePath
        val base = Environment.getExternalStorageDirectory().absolutePath
        val resolver = application.contentResolver
        var document: DocumentFile? = null
        var hasPermission = false
        for (perm in resolver.persistedUriPermissions) {
            val docId = DocumentsContract.getTreeDocumentId(perm.uri)
            val rootPath = base + "/" + docId.substringAfter(':')
            if (absPath.startsWith(rootPath)) {
                hasPermission = true
                var current = DocumentFile.fromTreeUri(application, perm.uri)
                val relative = absPath.removePrefix(rootPath).trimStart('/')
                if (relative.isNotEmpty()) {
                    for (part in relative.split('/')) {
                        current = current?.listFiles()?.firstOrNull { it.name == part }
                        if (current == null) break
                    }
                }
                document = current
                break
            }
        }
        println("FileCleanupWorker ---> SAF delete attempt for path: $absPath")
        println("FileCleanupWorker ---> Found document: ${document != null} | hasPermission: $hasPermission")
        return hasPermission && document?.delete() == true
    }
}

