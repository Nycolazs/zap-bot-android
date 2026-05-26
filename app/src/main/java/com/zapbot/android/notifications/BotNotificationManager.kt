package com.zapbot.android.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zapbot.android.MainActivity
import com.zapbot.android.R
import com.zapbot.android.domain.WhatsAppConnectionState
import com.zapbot.android.service.BotForegroundService

class BotNotificationManager(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "ZapTube Bot", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Bot runtime status and active downloads"
                }
            )
        }
    }

    fun build(
        state: WhatsAppConnectionState,
        activeDownloads: Int,
        startedAt: Long? = null,
        themeMode: String = "system",
        lastError: String? = null
    ): Notification {
        val (title, text) = when {
            state is WhatsAppConnectionState.WaitingForQr -> "ZapTube Bot" to "Connect WhatsApp"
            state is WhatsAppConnectionState.Error -> "ZapTube Bot" to "Needs attention"
            lastError != null -> "ZapTube Bot" to "Needs attention"
            activeDownloads > 0 -> "ZapTube Bot" to "$activeDownloads active"
            else -> "ZapTube Bot" to label(state)
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(notificationColor(themeMode))
            .setColorized(false)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(startedAt != null)
            .setWhen(startedAt ?: System.currentTimeMillis())
            .setUsesChronometer(startedAt != null)
            .setContentIntent(openAppIntent())
            .addAction(0, "Stop", serviceIntent(BotForegroundService.ACTION_STOP))
            .build()
    }

    fun notify(notification: Notification) {
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun label(state: WhatsAppConnectionState): String = when (state) {
        WhatsAppConnectionState.Disconnected -> "Disconnected"
        is WhatsAppConnectionState.WaitingForQr -> "Waiting for QR"
        WhatsAppConnectionState.Connecting -> "Connecting"
        is WhatsAppConnectionState.Connected -> "Connected"
        WhatsAppConnectionState.Running -> "Running"
        is WhatsAppConnectionState.Error -> "Error"
    }

    private fun notificationColor(themeMode: String): Int = when (themeMode) {
        "dark" -> 0xFF7ADBCB.toInt()
        "light" -> 0xFF126C5E.toInt()
        else -> if ((context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            0xFF7ADBCB.toInt()
        } else {
            0xFF126C5E.toInt()
        }
    }

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun serviceIntent(action: String): PendingIntent =
        PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, BotForegroundService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    companion object {
        const val CHANNEL_ID = "bot_runtime"
        const val NOTIFICATION_ID = 1001
    }
}
