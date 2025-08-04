package com.d4rk.cleaner.app.clean.scanner.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.scanner.domain.operations.CleaningManager
import com.d4rk.cleaner.core.utils.helpers.CleaningEventBus
import com.google.android.material.color.MaterialColors
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Worker responsible for deleting files or moving them to the trash.
 *
 * A notification with a determinate progress bar is shown for the entire
 * duration of the work and is updated as files are processed. Once the
 * operation finishes, the notification is updated with the final result and
 * dismissed after a short delay.
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
        var processed = 0
        builder.setProgress(total, processed, false)
            .setContentText(
                applicationContext.getString(
                    R.string.cleanup_progress,
                    processed,
                    total,
                ),
            )

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasPermission) {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } else {
            Log.w(TAG, "Notification permission not granted")
        }

        val chunkSize = if (total <= MAX_PATHS_PER_WORKER) 1 else MAX_PATHS_PER_WORKER
        var error: DataState.Error<Unit, *>? = null
        for (batch in files.chunked(chunkSize)) {
            if (isStopped) {
                if (hasPermission) {
                    builder.setProgress(0, 0, false)
                        .setContentTitle(applicationContext.getString(R.string.cleanup_cancelled))
                        .setContentText(applicationContext.getString(R.string.cleanup_cancelled))
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                    delay(FINISH_DELAY_MS)
                    notificationManager.cancel(NOTIFICATION_ID)
                } else {
                    Log.w(TAG, "Notification permission not granted")
                }
                CleaningEventBus.notifyCleaned(success = false)
                return Result.failure()
            }

            val res = performAction(action, batch)
            if (res is DataState.Error) {
                error = res
                break
            }
            processed += batch.size
            builder.setProgress(total, processed, false)
                .setContentText(
                    applicationContext.getString(
                        R.string.cleanup_progress,
                        processed,
                        total,
                    ),
                )
            if (hasPermission) {
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        }

        if (isStopped) {
            if (hasPermission) {
                builder.setProgress(0, 0, false)
                    .setContentTitle(applicationContext.getString(R.string.cleanup_cancelled))
                    .setContentText(applicationContext.getString(R.string.cleanup_cancelled))
                notificationManager.notify(NOTIFICATION_ID, builder.build())
                delay(FINISH_DELAY_MS)
                notificationManager.cancel(NOTIFICATION_ID)
            } else {
                Log.w(TAG, "Notification permission not granted")
            }
            CleaningEventBus.notifyCleaned(success = false)
            return Result.failure()
        }
        builder.setProgress(0, 0, false)

        return if (error != null) {
            if (hasPermission) {
                builder.setContentTitle(applicationContext.getString(R.string.cleanup_failed))
                    .setContentText(applicationContext.getString(R.string.cleanup_failed_details))
                notificationManager.notify(NOTIFICATION_ID, builder.build())
                delay(FINISH_DELAY_MS)
                notificationManager.cancel(NOTIFICATION_ID)
            } else {
                Log.w(TAG, "Notification permission not granted")
            }
            CleaningEventBus.notifyCleaned(success = false)
            println(message = "Error for cleaning is: ${error.error}")
            Result.failure(
                Data.Builder().putString(KEY_ERROR, error.error.toString()).build(),
            )
        } else {
            CleaningEventBus.notifyCleaned(success = true)
            if (hasPermission) {
                builder.setContentTitle(applicationContext.getString(R.string.cleanup_finished))
                    .setContentText(applicationContext.getString(R.string.all_clean))
                notificationManager.notify(NOTIFICATION_ID, builder.build())
                delay(FINISH_DELAY_MS)
                notificationManager.cancel(NOTIFICATION_ID)
            } else {
                Log.w(TAG, "Notification permission not granted")
            }
            Result.success()
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

        /**
         * Maximum number of file paths accepted by a single work request.
         * Enqueuing code splits larger lists using this value to stay under
         * WorkManager's Data size limit and within expedited work quotas.
         */
        const val MAX_PATHS_PER_WORKER = 100
        private const val NOTIFICATION_ID = 2001
        private const val NOTIFICATION_CHANNEL = "file_cleanup"
        private const val FINISH_DELAY_MS = 10000L
        private const val TAG = "FileCleanupWorker"
    }
}