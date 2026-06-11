package com.example

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

class NexusService : Service() {
    private var fileServer: FileServer? = null

    override fun onCreate() {
        super.onCreate()
        Logger.log("Nexus Foreground Service initialized.")
        fileServer = FileServer(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.log("Foreground Service starting server threads...")
        
        createNotificationChannel()
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Spin Netty server asynchronously
        Thread {
            fileServer?.start()
        }.start()

        return START_STICKY
    }

    override fun onDestroy() {
        Logger.log("Stopping Foreground Service thread systems...")
        Thread {
            fileServer?.stop()
        }.start()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nexus Explorer Pro Services",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "System notification channel for Nexus server background host"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val localIp = NetworkManager.getLocalIpAddress()
        val serverUrl = "http://$localIp:${AppConfig.PORT}"
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nexus Explorer Pro Core Online")
            .setContentText("Open on your mobile: $serverUrl")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Safe built-in asset icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "nexus_service_channel_id"
        private const val NOTIFICATION_ID = 5309
    }
}
