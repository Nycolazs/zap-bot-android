package com.zapbot.android.service

import android.app.Service
import android.os.BatteryManager
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.zapbot.android.ZapBotApplication
import com.zapbot.android.domain.BotCommandParser
import com.zapbot.android.domain.BotEngine
import com.zapbot.android.domain.IncomingWhatsAppMessage
import com.zapbot.android.domain.WhatsAppConnectionState
import com.zapbot.android.notifications.BotNotificationManager
import com.zapbot.android.queue.DownloadQueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BotForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var queue: DownloadQueueManager
    private var started = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startRuntime()
        return START_STICKY
    }

    private fun startRuntime() {
        if (started) return
        started = true
        val container = (application as ZapBotApplication).container
        startForeground(
            BotNotificationManager.NOTIFICATION_ID,
            container.notifications.build(WhatsAppConnectionState.Connecting, 0)
        )
        queue = DownloadQueueManager(
            scope = scope,
            jobDao = container.database.downloadJobDao(),
            settingsRepository = container.settings,
            downloader = container.downloader,
            whatsappClient = container.whatsappClient,
            cleanup = container.cleanup,
            logger = container.logger
        )
        queue.start()
        val engine = BotEngine(
            parser = BotCommandParser(),
            youtube = container.youtubeClient,
            sessions = container.sessions,
            jobDao = container.database.downloadJobDao(),
            queue = queue,
            whatsapp = container.whatsappClient,
            logger = container.logger
        )
        scope.launch(Dispatchers.IO) { container.whatsappClient.start() }
        scope.launch {
            container.whatsappClient.incomingMessages
                .catch { container.logger.error("Service", "Message stream failed", it) }
                .collect {
                    scope.launch(Dispatchers.IO) {
                        container.logger.info("Service", "Message received from ${it.senderLabel()}")
                        engine.handle(it)
                    }
                }
        }
        scope.launch {
            combine(
                container.whatsappClient.connectionState,
                container.database.downloadJobDao().observeActiveCount()
            ) { state, count -> state to count }
                .collect { (state, count) -> container.notifications.notify(container.notifications.build(state, count)) }
        }
        scope.launch(Dispatchers.IO) {
            var lastBatteryAlertAt = 0L
            while (true) {
                val battery = getSystemService(BatteryManager::class.java)
                val level = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
                val now = System.currentTimeMillis()
                if (level in 0..14 && now - lastBatteryAlertAt > 15 * 60 * 1_000L) {
                    lastBatteryAlertAt = now
                    runCatching {
                        container.whatsappClient.sendTextToGroupName(
                            "Alerta Music Bot",
                            "🔋 *Bateria baixa no celular do bot*\n\n_Nível atual:_ *$level%*\n\nColoque o aparelho para carregar para evitar que o bot caia."
                        )
                    }
                }
                delay(5 * 60 * 1_000L)
            }
        }
    }

    override fun onDestroy() {
        val container = (application as ZapBotApplication).container
        runBlocking(Dispatchers.IO) { container.whatsappClient.stop() }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.zapbot.android.START"
        const val ACTION_STOP = "com.zapbot.android.STOP"

        fun start(context: android.content.Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BotForegroundService::class.java).setAction(ACTION_START)
            )
        }
    }
}

private fun IncomingWhatsAppMessage.senderLabel(): String =
    senderName?.takeIf { it.isNotBlank() } ?: chatId.toReadableChatId()

private fun String.toReadableChatId(): String =
    substringBefore("@")
        .filter { it.isDigit() || it == '+' }
        .takeIf { it.isNotBlank() }
        ?: "WhatsApp contact"
