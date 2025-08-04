package com.d4rk.cleaner.app.clean.whatsapp.summary.data

import android.app.Application
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.text.format.Formatter
import androidx.documentfile.provider.DocumentFile
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.model.DirectorySummary
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.model.WhatsAppMediaSummary
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.repository.WhatsAppCleanerRepository
import com.d4rk.cleaner.app.clean.whatsapp.utils.constants.WhatsAppMediaConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WhatsAppCleanerRepositoryImpl(private val application: Application) :
    WhatsAppCleanerRepository {

    private fun getWhatsAppMediaDir(): File {
        val scoped = File(
            Environment.getExternalStorageDirectory(),
            "Android/media/com.whatsapp/WhatsApp/Media"
        )
        val legacy = File(
            Environment.getExternalStorageDirectory(),
            "WhatsApp/Media"
        )
        return when {
            scoped.exists() -> scoped
            legacy.exists() -> legacy
            else -> scoped
        }
    }

    override suspend fun getMediaSummary(): WhatsAppMediaSummary = withContext(Dispatchers.IO) {
        val base = getWhatsAppMediaDir()

        fun collect(name: String): DirectorySummary {
            val dir = File(base, name)
            if (!dir.exists()) return DirectorySummary()
            val files = dir.walkTopDown()
                .filter { it.isFile && it.name != ".nomedia" }
            var count = 0
            var size = 0L
            files.forEach {
                count++
                size += it.length()
            }
            val formatted = Formatter.formatFileSize(application, size)
            return DirectorySummary(count, size, formatted)
        }

        val directories = WhatsAppMediaConstants.DIRECTORIES

        val collected = directories.mapValues { (_, dirName) -> collect(dirName) }

        val images = collected.getValue(WhatsAppMediaConstants.IMAGES)
        val videos = collected.getValue(WhatsAppMediaConstants.VIDEOS)
        val docs = collected.getValue(WhatsAppMediaConstants.DOCUMENTS)
        val audios = collected.getValue(WhatsAppMediaConstants.AUDIOS)
        val statuses = collected.getValue(WhatsAppMediaConstants.STATUSES)
        val voiceNotes = collected.getValue(WhatsAppMediaConstants.VOICE_NOTES)
        val videoNotes = collected.getValue(WhatsAppMediaConstants.VIDEO_NOTES)
        val gifs = collected.getValue(WhatsAppMediaConstants.GIFS)
        val wallpapers = collected.getValue(WhatsAppMediaConstants.WALLPAPERS)
        val stickers = collected.getValue(WhatsAppMediaConstants.STICKERS)
        val profile = collected.getValue(WhatsAppMediaConstants.PROFILE_PHOTOS)

        val totalSize = collected.values.sumOf { it.totalBytes }
        val totalFormatted = Formatter.formatFileSize(application, totalSize)

        WhatsAppMediaSummary(
            images = images,
            videos = videos,
            documents = docs,
            audios = audios,
            statuses = statuses,
            voiceNotes = voiceNotes,
            videoNotes = videoNotes,
            gifs = gifs,
            wallpapers = wallpapers,
            stickers = stickers,
            profilePhotos = profile,
            formattedTotalSize = totalFormatted,
        )
    }

    override suspend fun deleteFiles(files: List<File>) = withContext(Dispatchers.IO) {
        val androidDir = Environment.getExternalStorageDirectory().absolutePath + "/Android"
        files.forEach { file ->
            if (!file.exists()) return@forEach

            val hasManage =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
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
                val result = deleteWithSaf(file)
                println("Deletion method: SAF → path: ${file.path}")
                println("DocumentFile.delete() success: $result for ${file.path}")
                if (!result) throw RuntimeException("SAF_DELETE_FAILED")
            }
        }
    }

    private fun deleteWithSaf(target: File): Boolean {
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

    override suspend fun listMediaFiles(type: String, offset: Int, limit: Int): List<File> =
        withContext(Dispatchers.IO) {
            val base = getWhatsAppMediaDir()
            val dirName =
                WhatsAppMediaConstants.DIRECTORIES[type] ?: return@withContext emptyList<File>()
            val dir = File(base, dirName)
            if (!dir.exists()) return@withContext emptyList()
            dir.walkTopDown()
                .filter { it.isFile && it.name != ".nomedia" }
                .drop(offset)
                .take(limit)
                .toList()
        }

}
