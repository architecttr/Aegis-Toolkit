package com.d4rk.cleaner.app.clean.scanner.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.d4rk.cleaner.R

/**
 * Foreground service used for long running file deletion and move operations.
 *
 * Google Play requires declaring the `fileManagement` foreground service type
 * and the `FOREGROUND_SERVICE_FILE_MANAGEMENT` permission when manipulating
 * local files. See
 * https://developer.android.com/guide/components/foreground-services
 * for policy details.
 *
 * A persistent notification is shown while work is active. If the policy ever
 * changes to disallow foreground services for local-only file operations,
 * migrate this logic to WorkManager.
 */
class FileOperationService : Service() {

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cleaner_notify)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.cleaning_in_progress))
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File operations",
                NotificationManager.IMPORTANCE_LOW
            )
            NotificationManagerCompat.from(this).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "file_ops"
        private const val NOTIFICATION_ID = 2001
        // TODO: Review Google Play FGS policy periodically to confirm that
        // `fileManagement` remains an allowed foreground service type for
        // local cleanup tasks. Migrate to WorkManager if requirements change.
    }
}
