package com.d4rk.cleaner.app.clean.scanner.utils.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import coil3.video.videoFramePercent
import com.d4rk.cleaner.R
import com.google.common.io.Files.getFileExtension
import java.nio.ByteBuffer
import kotlin.math.abs
import java.io.File

/**
 * Helper used for displaying file previews. All composables that need to render
 * a file preview should delegate to [FilePreviewHelper.preview] so that the
 * logic for determining how a file should be presented lives in a single place.
 *
 * Extend [PreviewType] when supporting new file formats.
 */
object FilePreviewHelper {

    private val bitmapCache: MutableMap<String, Bitmap?> = mutableMapOf()

    private fun loadAlbumArt(file: File): Bitmap? {
        return bitmapCache.getOrPut(file.path) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.path)
                val art = retriever.embeddedPicture
                retriever.release()
                art?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            }.getOrNull()
        }
    }

    private fun generateWaveform(file: File, width: Int = 64, height: Int = 32): Bitmap? {
        return runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.path)
            var trackIndex = 0
            while (trackIndex < extractor.trackCount) {
                val format = extractor.getTrackFormat(trackIndex)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) break else trackIndex++
            }
            if (trackIndex >= extractor.trackCount) {
                extractor.release()
                return null
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val maxInput = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            val buffer = ByteBuffer.allocate(maxInput)
            val data = IntArray(width)
            var i = 0
            while (i < width) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                var sum = 0
                for (b in 0 until size step 2) {
                    sum += abs(buffer.getShort(b).toInt())
                }
                data[i] = if (size > 0) sum / (size / 2) else 0
                extractor.advance()
                i++
            }
            extractor.release()
            val max = data.maxOrNull() ?: 0
            if (max == 0) return null
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint().apply { color = Color.WHITE }
            data.forEachIndexed { idx, amp ->
                val ratio = amp.toFloat() / max
                val barHeight = ratio * height
                val x = idx.toFloat()
                canvas.drawLine(x, height - barHeight, x, height.toFloat(), paint)
            }
            bmp
        }.getOrNull()
    }

    /** Represents the available preview strategies for files. */
    sealed class PreviewType {
        object Directory : PreviewType()
        object Image : PreviewType()
        object Video : PreviewType()
        object Apk : PreviewType()
        object Pdf : PreviewType()
        object Office : PreviewType()
        object Audio : PreviewType()
        object Archive : PreviewType()
        object Unknown : PreviewType()
    }

    fun getPreviewType(file: File, context: Context): PreviewType {
        if (file.isDirectory) return PreviewType.Directory
        val ext = getFileExtension(file.name).lowercase()
        val res = context.resources
        return when {
            ext in res.getStringArray(R.array.image_extensions) -> PreviewType.Image
            ext in res.getStringArray(R.array.video_extensions) -> PreviewType.Video
            ext in res.getStringArray(R.array.apk_extensions) -> PreviewType.Apk
            ext == "pdf" -> PreviewType.Pdf
            ext in res.getStringArray(R.array.microsoft_office_extensions) -> PreviewType.Office
            ext in res.getStringArray(R.array.audio_extensions) -> PreviewType.Audio
            ext in res.getStringArray(R.array.archive_extensions) -> PreviewType.Archive
            else -> PreviewType.Unknown
        }
    }

    /**
     * Displays a preview of the provided [file]. Call this from any UI where a
     * preview icon or thumbnail is required.
     */
    @Composable
    fun Preview(file: File, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val type = remember(file.path) { getPreviewType(file, context) }
        val imageLoader = LocalContext.current.imageLoader
        when (type) {
            PreviewType.Directory -> {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = modifier.size(24.dp)
                )
            }

            PreviewType.Image -> {
                AsyncImage(
                    model = remember(file) {
                        ImageRequest.Builder(context).data(file).size(64).crossfade(true).build()
                    },
                    imageLoader = imageLoader,
                    contentDescription = file.name,
                    contentScale = ContentScale.FillWidth,
                    modifier = modifier.fillMaxWidth()
                )
            }

            PreviewType.Video -> {
                AsyncImage(
                    model = remember(file) {
                        ImageRequest.Builder(context).data(file)
                            .decoderFactory { result, options, _ ->
                                VideoFrameDecoder(result.source, options)
                            }.videoFramePercent(0.5).crossfade(true).build()
                    },
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.fillMaxSize()
                )
            }

            PreviewType.Apk -> {
                val icon = remember(file.path) {
                    context.packageManager.getPackageArchiveInfo(file.path, 0)?.applicationInfo?.apply {
                        sourceDir = file.path
                        publicSourceDir = file.path
                    }?.loadIcon(context.packageManager)
                }
                if (icon != null) {
                    AsyncImage(
                        model = icon,
                        contentDescription = file.name,
                        modifier = modifier.size(48.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_apk_document),
                        contentDescription = null,
                        modifier = modifier.size(24.dp)
                    )
                }
            }

            PreviewType.Pdf -> {
                val pdfBitmap = remember(file) { loadPdfThumbnail(file) }
                if (pdfBitmap != null) {
                    Image(
                        bitmap = pdfBitmap.asImageBitmap(),
                        contentDescription = file.name,
                        contentScale = ContentScale.FillWidth,
                        modifier = modifier.fillMaxWidth()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        modifier = modifier.size(24.dp)
                    )
                }
            }

            PreviewType.Office -> {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = modifier.size(24.dp)
                )
            }

            PreviewType.Audio -> {
                val bitmap = remember(file.path) {
                    loadAlbumArt(file) ?: generateWaveform(file)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = modifier.fillMaxWidth()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_audio_file),
                        contentDescription = null,
                        modifier = modifier.size(24.dp)
                    )
                }
            }

            PreviewType.Archive -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_archive_filter),
                    contentDescription = null,
                    modifier = modifier.size(24.dp)
                )
            }

            PreviewType.Unknown -> {
                val icon = remember(getFileExtension(file.name)) {
                    getFileIcon(getFileExtension(file.name), context)
                }
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = modifier.size(24.dp)
                )
            }
        }
    }
}
