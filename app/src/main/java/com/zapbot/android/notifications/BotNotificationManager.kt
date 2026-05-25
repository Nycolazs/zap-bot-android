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
                NotificationChannel(CHANNEL_ID, "WhatsApp Bot", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun build(state: WhatsAppConnectionState, activeDownloads: Int, lastError: String? = null): Notification {
        val (title, text) = when {
            state is WhatsAppConnectionState.WaitingForQr -> "Connect WhatsApp" to "Open the app and scan the QR code"
            state is WhatsAppConnectionState.Error -> "Bot issue" to state.message.take(120)
            lastError != null -> "Bot issue" to lastError.take(120)
            activeDownloads > 0 -> "WhatsApp bot running" to "$activeDownloads active download(s)"
            else -> "WhatsApp bot running" to "${label(state)} - 0 active downloads"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
            .addAction(0, "Stop bot", serviceIntent(BotForegroundService.ACTION_STOP))
            .addAction(0, "Open app", openAppIntent())
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
