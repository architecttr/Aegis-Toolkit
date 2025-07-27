package com.yourname.sanitizr.util

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.ooxml.POIXMLProperties.CoreProperties

import android.graphics.BitmapFactory

import androidx.exifinterface.media.ExifInterface

import java.util.Date
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.FileInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream

import org.tukaani.xz.XZInputStream
import org.tukaani.xz.XZOutputStream
import org.tukaani.xz.LZMA2Options

object FileSanitizer {

    // Define the supported file types and their common extensions
    val supportedFileTypes = mapOf(
        "image" to listOf(
            "jpg", "jpeg", "png", "gif", "webp", "tiff", "bmp", "heic"
        ),
        "video" to listOf(
            "mp4", "mov", "avi", "mkv", "webm", "3gp", "flv", "mpeg"
        ),
        "audio" to listOf(
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "alac"
        ),
        "pdf" to listOf(
            "pdf"
        ),
        "document" to listOf(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "odt", "ods", "odp", "rtf", "txt", "csv"
        ),
        "ebook" to listOf(
            "epub", "mobi", "azw3", "fb2"
        ),
        "archive" to listOf(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz"
        )
    )

    fun sanitizeFile(file: File, fileType: String): Boolean {
        return when (fileType) {
            "image" -> sanitizeImage(file)
            "video" -> sanitizeVideo(file)
            "audio" -> sanitizeAudio(file)
            "pdf"   -> sanitizePdf(file)
            "document" -> sanitizeDocument(file)
            "ebook" -> sanitizeEbook(file)
            "archive" -> {
                when (file.extension.lowercase()) {
                    "zip" -> sanitizeZip(file)
                    "tar" -> sanitizeTar(file)
                    "gz"  -> sanitizeGz(file)
                    "bz2" -> sanitizeBz2(file)
                    "xz"  -> sanitizeXz(file)
                    else -> {
                        println("Archive format not supported for sanitizing: ${file.extension}")
                        false
                    }
                }
            }
            else -> false
        }
    }
    
    fun detectFileType(file: File): String? {
        val ext = file.extension.lowercase()
        return supportedFileTypes.entries.find { ext in it.value }?.key
    }

    private fun fullyStripImageMetadata(file: File): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val format = when (file.extension.lowercase()) {
                "png" -> Bitmap.CompressFormat.PNG
                else -> Bitmap.CompressFormat.JPEG
            }

            val tempFile = File(file.parent, "sanitized_${file.name}")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(format, 100, out)
            }

            if (file.delete()) {
                tempFile.renameTo(file)
            } else {
                tempFile.delete()
                return false
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun sanitizeImage(file: File): Boolean {
        return try {
            // --- STEP 1 ---
            // Attempt to scrub metadata directly
            val exif = ExifInterface(file)

            exif.setAttribute(ExifInterface.TAG_DATETIME, "")
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "")
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, "")
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, "")
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, "")
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, "")

            exif.setAttribute(ExifInterface.TAG_MAKE, "")
            exif.setAttribute(ExifInterface.TAG_MODEL, "")
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "")
            exif.setAttribute(ExifInterface.TAG_ARTIST, "")
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, "")
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "")
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)

            exif.saveAttributes()

            // --- STEP 2 ---
            // For absolute privacy, re-encode the bitmap to strip all EXIF blocks completely
            val success = fullyStripImageMetadata(file)
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun sanitizeVideo(file: File): Boolean {
        try {
            val sanitizedPath = file.parent + "/sanitized_" + file.name
            val cmd = arrayOf(
                "-i", file.absolutePath,
                "-map", "0",       // copy all streams
                "-map_metadata", "-1", // remove all metadata
                "-c", "copy",      // copy codec, no re-encode
                sanitizedPath
            )

            val session = FFmpegKit.execute(cmd.joinToString(" "))
            if (ReturnCode.isSuccess(session.returnCode)) {
                // Replace original file with sanitized
                val originalDeleted = file.delete()
                val renamed = File(sanitizedPath).renameTo(file)
                return originalDeleted && renamed
            } else {
                println("FFmpeg failed: ${session.returnCode} ${session.failStackTrace}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun sanitizeAudio(file: File): Boolean {
        return try {
            val sanitizedPath = file.parent + "/sanitized_" + file.name
            val cmd = arrayOf(
                "-i", file.absolutePath,
                "-map_metadata", "-1", // remove all metadata
                "-c", "copy",          // copy audio, no re-encode
                sanitizedPath
            )

            val session = FFmpegKit.execute(cmd.joinToString(" "))
            if (ReturnCode.isSuccess(session.returnCode)) {
                // Replace original file with sanitized
                val originalDeleted = file.delete()
                val renamed = File(sanitizedPath).renameTo(file)
                return originalDeleted && renamed
            } else {
                println("FFmpeg failed: ${session.returnCode} ${session.failStackTrace}")
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun sanitizePdf(file: File): Boolean {
        return try {
            val reader = PdfReader(file.absolutePath)
            val sanitizedPath = file.parent + "/sanitized_" + file.name
            val stamper = PdfStamper(reader, FileOutputStream(sanitizedPath))

            // Clear all metadata
            val emptyInfo = HashMap<Any, Any>()
            stamper.moreInfo = emptyInfo

            stamper.close()
            reader.close()

            // Replace original file
            val originalDeleted = file.delete()
            val renamed = File(sanitizedPath).renameTo(file)
            originalDeleted && renamed
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    

    private fun sanitizeDocument(file: File): Boolean {
        return try {
            val opc = OPCPackage.open(file)
            val doc = XWPFDocument(opc)

            val props = doc.properties.coreProperties
            props.setTitle("")
            props.setCreator("")
            props.setDescription("")
            props.setSubjectProperty("")
            props.setKeywords("")

            val sanitizedPath = file.parent + "/sanitized_" + file.name
            FileOutputStream(sanitizedPath).use { out ->
                doc.write(out)
            }
            doc.close()

            val originalDeleted = file.delete()
            val renamed = File(sanitizedPath).renameTo(file)
            originalDeleted && renamed
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun sanitizeEbook(file: File): Boolean {
        return try {
            val tempFile = File(file.parent, "temp_${file.name}")
            ZipFile(file).use { zip ->
                ZipOutputStream(tempFile.outputStream()).use { zos ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val entryData = zip.getInputStream(entry).readBytes()

                        if (entry.name.endsWith(".opf")) {
                            // This is the metadata XML - sanitize it
                            val sanitizedXml = sanitizeEpubMetadata(entryData)
                            zos.putNextEntry(ZipEntry(entry.name))
                            zos.write(sanitizedXml)
                            zos.closeEntry()
                        } else {
                            zos.putNextEntry(ZipEntry(entry.name))
                            zos.write(entryData)
                            zos.closeEntry()
                        }
                    }
                }
            }
            // Replace original file
            if (file.delete()) {
                tempFile.renameTo(file)
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun sanitizeEpubMetadata(xmlData: ByteArray): ByteArray {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(xmlData.inputStream())
        doc.documentElement.normalize()

        val metadataNodes = doc.getElementsByTagName("metadata")
        if (metadataNodes.length > 0) {
            val metadata = metadataNodes.item(0)

            // Remove all child nodes (clears metadata)
            while (metadata.hasChildNodes()) {
                metadata.removeChild(metadata.firstChild)
            }
        }

        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        val outputStream = ByteArrayOutputStream()
        transformer.transform(DOMSource(doc), StreamResult(outputStream))
        return outputStream.toByteArray()
    }
    
    private fun sanitizeZip(file: File): Boolean {
        if (!file.exists()) return false
        val tempFile = File(file.parentFile, "temp_sanitized.zip")

        try {
            ZipFile(file).use { zip ->
                ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()

                        if (entry.isDirectory) {
                            val dirEntry = ZipEntry(entry.name)
                            dirEntry.time = 0
                            zos.putNextEntry(dirEntry)
                            zos.closeEntry()
                        } else {
                            val newEntry = ZipEntry(entry.name)
                            newEntry.time = 0
                            zos.putNextEntry(newEntry)
                            zip.getInputStream(entry).use { input ->
                                input.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            if (file.delete()) {
                tempFile.renameTo(file)
            } else {
                tempFile.delete()
                return false
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            return false
        }
    }
    
    private fun sanitizeTar(file: File): Boolean {
        println("Sanitizing TAR archive: ${file.name}")

        val tempFile = File(file.parentFile, "temp_sanitized.tar")

        try {
            TarArchiveInputStream(file.inputStream()).use { tis ->
                TarArchiveOutputStream(tempFile.outputStream()).use { tos ->
                    var entry: TarArchiveEntry? = tis.nextTarEntry

                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val sanitizedEntry = TarArchiveEntry(entry.name)

                            sanitizedEntry.size = entry.size
                            sanitizedEntry.modTime = Date(0L) // clear modification time
                            sanitizedEntry.userName = ""
                            sanitizedEntry.groupName = ""
                            sanitizedEntry.mode = 0b110100100 // rw-r--r--

                            tos.putArchiveEntry(sanitizedEntry)
                            tis.copyTo(tos)
                            tos.closeArchiveEntry()
                        } else {
                            // Add directory as empty entry
                            val dirEntry = TarArchiveEntry(entry.name)
                            dirEntry.modTime = Date(0L)
                            dirEntry.userName = ""
                            dirEntry.groupName = ""
                            tos.putArchiveEntry(dirEntry)
                            tos.closeArchiveEntry()
                        }

                        entry = tis.nextTarEntry
                    }

                    tos.finish()
                }
            }

            if (file.delete()) {
                tempFile.renameTo(file)
            } else {
                tempFile.delete()
                return false
            }

            println("TAR archive sanitized successfully: ${file.name}")
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            return false
        }
    }

    private fun sanitizeGz(file: File): Boolean {
        println("Sanitizing GZ archive: ${file.name}")

        val tempFile = File(file.parentFile, "temp_sanitized.gz")

        try {
            FileInputStream(file).use { fis ->
                GZIPInputStream(fis).use { gis ->
                    FileOutputStream(tempFile).use { fos ->
                        GZIPOutputStream(fos).use { gos ->
                            gis.copyTo(gos)
                        }
                    }
                }
            }

            if (file.delete()) {
                tempFile.renameTo(file)
            } else {
                tempFile.delete()
                return false
            }

            println("GZ archive sanitized successfully: ${file.name}")
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            return false
        }
    }

    private fun sanitizeBz2(file: File): Boolean {
        println("Sanitizing BZ2 archive: ${file.name}")

        val tempFile = File(file.parentFile, "temp_sanitized.bz2")

        try {
            FileInputStream(file).use { fis ->
                BZip2CompressorInputStream(fis).use { bzis ->
                    FileOutputStream(tempFile).use { fos ->
                        BZip2CompressorOutputStream(fos).use { bzos ->
                            bzis.copyTo(bzos)
                        }
                    }
                }
            }

            if (file.delete()) {
                tempFile.renameTo(file)
            } else {
                tempFile.delete()
                return false
            }

            println("BZ2 archive sanitized successfully: ${file.name}")
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            return false
        }
    }

    private fun sanitizeXz(file: File): Boolean {
        println("Sanitizing XZ archive: ${file.name}")

        val tempFile = File(file.parentFile, "temp_sanitized.xz")

        try {
            FileInputStream(file).use { fis ->
                XZInputStream(fis).use { xzis ->
                    FileOutputStream(tempFile).use { fos ->
                        XZOutputStream(fos, org.tukaani.xz.LZMA2Options()).use { xzos ->
                            xzis.copyTo(xzos)
                        }
                    }
                }
            }

            if (file.delete()) {
                tempFile.renameTo(file)
            } else {
                tempFile.delete()
                return false
            }

            println("XZ archive sanitized successfully: ${file.name}")
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            return false
        }
    }

}

