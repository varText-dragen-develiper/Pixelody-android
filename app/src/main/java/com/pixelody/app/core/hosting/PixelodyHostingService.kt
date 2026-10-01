package com.pixelody.app.core.hosting

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pixelody.app.MainActivity

class PixelodyHostingService : Service() {

    companion object {
        const val CHANNEL_ID = "pixelody_hosting_channel"
        const val NOTIFICATION_ID = 2002
        const val ACTION_START = "com.pixelody.app.action.START_HOSTING"
        const val ACTION_STOP = "com.pixelody.app.action.STOP_HOSTING"
        const val EXTRA_HOST_URL = "extra_host_url"
        const val EXTRA_TRACK_COUNT = "extra_track_count"

        fun start(context: Context, hostUrl: String, trackCount: Int) {
            val intent = Intent(context, PixelodyHostingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_HOST_URL, hostUrl)
                putExtra(EXTRA_TRACK_COUNT, trackCount)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PixelodyHostingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val hostUrl = intent.getStringExtra(EXTRA_HOST_URL).orEmpty()
                val trackCount = intent.getIntExtra(EXTRA_TRACK_COUNT, 0)
                val notification = buildNotification(hostUrl, trackCount)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                        } else {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                        }
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pixelody Portable Music Host",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status while hosting your device library over Wi-Fi"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(hostUrl: String, trackCount: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (hostUrl.isNotBlank()) {
            "Hosting $trackCount tracks at $hostUrl"
        } else {
            "Portable music library host active"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pixelody Portable Host Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
