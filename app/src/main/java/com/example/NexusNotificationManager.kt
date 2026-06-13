package com.example

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class NotificationType {
    SUCCESS,
    ERROR,
    INFO,
    WARNING
}

data class InAppNotification(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: NotificationType,
    val durationMs: Long = 4500L
)

object NexusNotificationManager {
    private const val CHANNEL_TRANSACTIONS_ID = "nexus_transactions_channel"
    private const val CHANNEL_ALERTS_ID = "nexus_alerts_channel"
    private const val CHANNEL_ONGOING_ID = "nexus_ongoing_channel"

    private val _activeNotification = MutableStateFlow<InAppNotification?>(null)
    val activeNotification = _activeNotification.asStateFlow()

    private val notificationQueue = mutableListOf<InAppNotification>()
    private var isProcessingQueue = false
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Posts a notification both inside the app (rich animated UI banner)
     * and optionally as a system notification depending on settings and state.
     */
    fun notify(
        context: Context,
        message: String,
        type: NotificationType = NotificationType.INFO,
        showSystem: Boolean = true
    ) {
        Logger.log("[Notification] :: $type :: $message")
        
        // 1. Post to In-App system
        val inApp = InAppNotification(message = message, type = type)
        synchronized(notificationQueue) {
            notificationQueue.add(inApp)
        }
        processQueue()

        // 2. Post to System (outside the app)
        if (showSystem && hasNotificationPermission(context)) {
            showSystemNotification(context, message, type)
        }
    }

    /**
     * Clear current notification and move to next
     */
    fun dismissCurrent() {
        _activeNotification.value = null
    }

    private fun processQueue() {
        if (isProcessingQueue) return
        scope.launch {
            isProcessingQueue = true
            while (true) {
                val next: InAppNotification?
                synchronized(notificationQueue) {
                    next = if (notificationQueue.isNotEmpty()) notificationQueue.removeAt(0) else null
                }
                if (next == null) {
                    break
                }
                _activeNotification.value = next
                delay(next.durationMs)
                // If it's still the active one, clear it
                if (_activeNotification.value?.id == next.id) {
                    _activeNotification.value = null
                }
                delay(400) // Small pause between notifications for smooth exit transition
            }
            isProcessingQueue = false
        }
    }

    /**
     * Prepares and checks dynamic permissions on Android 13+ (POST_NOTIFICATIONS)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Explicitly setup notification channels required for Android Oreo+
     */
    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel 1: Data sync, transfers and transactions
            val transChannel = NotificationChannel(
                CHANNEL_TRANSACTIONS_ID,
                "Transferências e Ações Nexus",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificações de uploads, downloads e conexões ativas"
                enableVibration(true)
            }
            manager.createNotificationChannel(transChannel)

            // Channel 2: Alerts and System warnings
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Alertas do Sistema Nexus",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Erros críticos de IP, espaço em disco insuficiente ou permissões perdidas"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(alertChannel)

            // Channel 3: Server state / Ongoing task
            val ongoingChannel = NotificationChannel(
                CHANNEL_ONGOING_ID,
                "Servidor Host Ativo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Indica que o servidor de arquivos WiFi do Nexus está de pé"
                setShowBadge(false)
            }
            manager.createNotificationChannel(ongoingChannel)
            
            Logger.log("[Notification] Canais de notificações configurados com sucesso.")
        }
    }

    /**
     * Show a native system notification outside the application
     */
    private fun showSystemNotification(
        context: Context,
        message: String,
        type: NotificationType
    ) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val title = when (type) {
                NotificationType.SUCCESS -> "Sucesso - Nexus Explorer"
                NotificationType.ERROR -> "Erro Crítico - Nexus Explorer"
                NotificationType.WARNING -> "Aviso - Nexus Explorer"
                NotificationType.INFO -> "Informativo - Nexus Explorer"
            }

            val iconRes = when (type) {
                NotificationType.SUCCESS -> android.R.drawable.stat_sys_download_done
                NotificationType.ERROR -> android.R.drawable.stat_notify_error
                NotificationType.WARNING -> android.R.drawable.stat_sys_warning
                NotificationType.INFO -> android.R.drawable.stat_notify_chat
            }

            val channelId = if (type == NotificationType.ERROR || type == NotificationType.WARNING) {
                CHANNEL_ALERTS_ID
            } else {
                CHANNEL_TRANSACTIONS_ID
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

            val builder = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(iconRes)
                .setAutoCancel(true)
                .setPriority(
                    if (type == NotificationType.ERROR) NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT
                )
                .setContentIntent(pendingIntent)
                .setCategory(
                    if (type == NotificationType.ERROR) NotificationCompat.CATEGORY_ERROR
                    else NotificationCompat.CATEGORY_EVENT
                )

            val notificationId = when (type) {
                NotificationType.SUCCESS -> 1001
                NotificationType.ERROR -> 1002
                NotificationType.WARNING -> 1003
                NotificationType.INFO -> 1004
            }

            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Logger.log("Falha ao lançar notificação nativa: ${e.message}")
        }
    }

    /**
     * Helper to build the ongoing background service notification
     */
    fun createOngoingServerNotification(
        context: Context,
        serverUrl: String
    ): Notification {
        // Ensure channels are ready
        initChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        return NotificationCompat.Builder(context, CHANNEL_ONGOING_ID)
            .setContentTitle("Servidor WiFi Nexus Pro Ativo")
            .setContentText("Acesse de outros dispositivos: $serverUrl")
            .setSubText("Compartilhamento Ativo")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setLocalOnly(true)
            .build()
    }
}
