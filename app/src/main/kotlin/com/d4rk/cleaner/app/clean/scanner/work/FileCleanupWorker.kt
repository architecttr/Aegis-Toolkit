package com.d4rk.cleaner.app.clean.scanner.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.scanner.domain.operations.CleaningManager
import com.d4rk.cleaner.core.utils.helpers.CleaningEventBus
import com.google.android.material.color.MaterialColors
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import android.Manifest

/**
 * Worker responsible for deleting files or moving them to the trash.
 *
 * A notification is posted for the entire duration of the work:
 * - For small jobs (<500 files) an indeterminate progress bar is shown.
 * - For larger jobs a determinate progress notification is shown and updated as
 *   each batch of files is processed.
 * The notification is dismissed or updated once the operation finishes or an
 * error occurs.
 */
class FileCleanupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val cleaningManager: CleaningManager by inject()

    override suspend fun doWork(): Result {
        val paths = inputData.getStringArray(KEY_PATHS)?.toList() ?: return Result.success()
        val action = inputData.getString(KEY_ACTION) ?: ACTION_DELETE
        val files = paths.map { File(it) }
        if (files.isEmpty()) return Result.success()

        createChannelIfNeeded()
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_cleaner_notify)
            .setContentTitle(applicationContext.getString(R.string.cleaning))
            .setColor(
                MaterialColors.getColor(
                    applicationContext,
                    com.google.android.material.R.attr.colorPrimary,
                    0,
                ),
            )
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        val total = files.size
        val result: DataState<Unit, *> = if (total < PROGRESS_THRESHOLD) {
            builder.setContentText(applicationContext.getString(R.string.cleaning_in_progress))
                .setProgress(0, 0, true)
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
            performAction(action, files)
        } else {
            // Determinate progress with batching
            var processed = 0
            builder.setProgress(total, processed, false)
                .setContentText(applicationContext.getString(R.string.cleaning_in_progress))
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }

            var error: DataState.Error<Unit, *>? = null
            for (batch in files.chunked(BATCH_SIZE)) {
                val res = performAction(action, batch)
                if (res is DataState.Error) {
                    error = res
                    break
                }
                processed += batch.size
                builder.setProgress(total, processed, false)
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
            }
            error ?: DataState.Success(Unit)
        }

        builder.setOngoing(false).setAutoCancel(true).setProgress(0, 0, false)
        return when (result) {
            is DataState.Error -> {
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    builder.setContentText(
                        applicationContext.getString(R.string.something_went_wrong),
                    )
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
                notificationManager.cancel(NOTIFICATION_ID)
                Result.failure(
                    Data.Builder().putString(KEY_ERROR, result.error.toString()).build(),
                )
            }
            else -> {
                CleaningEventBus.notifyCleaned()
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    builder.setContentText(applicationContext.getString(R.string.all_clean))
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
                notificationManager.cancel(NOTIFICATION_ID)
                Result.success()
            }
        }
    }

    private suspend fun performAction(action: String, files: List<File>): DataState<Unit, *> {
        return when (action) {
            ACTION_TRASH -> {
                when (val result = cleaningManager.moveToTrash(files)) {
                    is DataState.Error -> result
                    else -> DataState.Success(Unit)
                }
            }
            else -> cleaningManager.deleteFiles(files.toSet())
        }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.getString(R.string.cleaning)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                name,
                NotificationManager.IMPORTANCE_LOW,
            )
            NotificationManagerCompat.from(applicationContext).createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_PATHS = "paths"
        const val KEY_ACTION = "action"
        const val KEY_ERROR = "error"
        const val ACTION_DELETE = "delete"
        const val ACTION_TRASH = "trash"

        private const val PROGRESS_THRESHOLD = 500
        private const val BATCH_SIZE = 100
        private const val NOTIFICATION_ID = 2001
        private const val NOTIFICATION_CHANNEL = "file_cleanup"
    }
}