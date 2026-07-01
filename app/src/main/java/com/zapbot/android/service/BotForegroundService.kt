package com.zapbot.android.service

import android.app.Service
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.zapbot.android.ZapBotApplication
import com.zapbot.android.domain.BotCommandParser
import com.zapbot.android.domain.BotEngine
import com.zapbot.android.domain.IncomingWhatsAppMessage
import com.zapbot.android.domain.WhatsAppConnectionState
import com.zapbot.android.network.NetworkTransport
import com.zapbot.android.notifications.BotNotificationManager
import com.zapbot.android.queue.DownloadQueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import kotlin.math.max

class BotForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val messageDispatcher = Executors
        .newFixedThreadPool(max(4, Runtime.getRuntime().availableProcessors())) { runnable ->
            Thread(runnable, "zappy-message").apply {
                priority = Thread.MAX_PRIORITY
            }
        }
        .asCoroutineDispatcher()
    private lateinit var queue: DownloadQueueManager
    private var started = false
    private var allowMobileNetworkForThisRun = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val container = (application as ZapBotApplication).container
            scope.launch(Dispatchers.IO) { container.logger.info("Service", "Stop action received") }
            stopSelf()
            return START_NOT_STICKY
        }
        allowMobileNetworkForThisRun = intent?.getBooleanExtra(EXTRA_ALLOW_MOBILE_NETWORK, false) == true
        startRuntime()
        return START_STICKY
    }

    private fun startRuntime() {
        if (started) {
            val container = (application as ZapBotApplication).container
            scope.launch(Dispatchers.IO) { container.logger.info("Service", "Start ignored because service is already running") }
            return
        }
        val container = (application as ZapBotApplication).container
        val hasWhatsAppSession = runBlocking(Dispatchers.IO) { container.whatsappClient.hasSavedSession.first() }
        if (!hasWhatsAppSession) {
            runBlocking(Dispatchers.IO) {
                container.logger.warn("Service", "Bot start blocked because WhatsApp has no saved linked-device session. Pair WhatsApp again in Settings.")
            }
            stopSelf()
            return
        }

        started = true
        BotRuntimeState.markStarted()
        runBlocking(Dispatchers.IO) { container.logger.info("Service", "Foreground service starting") }
        startForeground(
            BotNotificationManager.NOTIFICATION_ID,
            container.notifications.build(
                WhatsAppConnectionState.Connecting,
                0,
                BotRuntimeState.startedAt.value,
                themeMode = runBlocking(Dispatchers.IO) { container.settings.get().themeMode }
            )
        )
        acquirePerformanceLocks()
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
            welcomes = container.database.welcomeDao(),
            jobDao = container.database.downloadJobDao(),
            queue = queue,
            whatsapp = container.whatsappClient,
            settings = container.settings,
            logger = container.logger
        )
        scope.launch(Dispatchers.IO) {
            runCatching {
                container.logger.info("WhatsApp", "Starting WhatsApp linked-device bridge")
                container.whatsappClient.start()
                container.logger.info("WhatsApp", "WhatsApp bridge start call completed")
            }.onFailure {
                container.logger.error("WhatsApp", "WhatsApp bridge failed to start", it)
            }
        }
        scope.launch {
            container.whatsappClient.incomingMessages
                .catch { container.logger.error("Service", "Message stream failed", it) }
                .collect {
                    scope.launch(messageDispatcher) {
                        engine.handle(it)
                        scope.launch(Dispatchers.IO) {
                            container.logger.info("Service", "Message handled from ${it.senderLabel()}")
                        }
                    }
                }
        }
        scope.launch(Dispatchers.IO) {
            container.whatsappClient.connectionState
                .distinctUntilChanged()
                .collect { state ->
                    when (state) {
                        WhatsAppConnectionState.Disconnected -> container.logger.warn("WhatsApp", "WhatsApp bridge is disconnected")
                        WhatsAppConnectionState.Connecting -> container.logger.info("WhatsApp", "WhatsApp bridge is connecting")
                        WhatsAppConnectionState.Running -> container.logger.info("WhatsApp", "WhatsApp bridge is running")
                        is WhatsAppConnectionState.Connected -> container.logger.info("WhatsApp", "WhatsApp connected as ${state.phoneNumber.orEmpty()}")
                        is WhatsAppConnectionState.WaitingForQr -> container.logger.warn("WhatsApp", "WhatsApp pairing is required")
                        is WhatsAppConnectionState.Error -> container.logger.error("WhatsApp", "WhatsApp bridge error: ${state.message}", state.throwable)
                    }
                }
        }
        scope.launch {
            combine(
                container.whatsappClient.connectionState,
                container.database.downloadJobDao().observeActiveCount(),
                container.settings.settings
            ) { state, count, settings -> Triple(state, count, settings.themeMode) }
                .collect { (state, count, themeMode) ->
                    container.notifications.notify(
                        container.notifications.build(state, count, BotRuntimeState.startedAt.value, themeMode = themeMode)
                    )
                }
        }
        scope.launch(Dispatchers.IO) {
            container.networkMonitor.transports().collect { transport ->
                val settings = container.settings.get()
                if (
                    started &&
                    !allowMobileNetworkForThisRun &&
                    settings.networkPreference == "WIFI_ONLY" &&
                    transport == NetworkTransport.MOBILE
                ) {
                    container.logger.warn("Service", "Network changed to mobile data while Wi-Fi only is configured; keeping bot running to avoid interrupting active chats")
                }
            }
        }
    }

    override fun onDestroy() {
        val container = (application as ZapBotApplication).container
        runBlocking(Dispatchers.IO) { container.logger.info("Service", "Foreground service stopping") }
        runBlocking(Dispatchers.IO) { container.whatsappClient.stop() }
        BotRuntimeState.markStopped()
        releasePerformanceLocks()
        messageDispatcher.close()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.zapbot.android.START"
        const val ACTION_STOP = "com.zapbot.android.STOP"
        const val EXTRA_ALLOW_MOBILE_NETWORK = "com.zapbot.android.ALLOW_MOBILE_NETWORK"

        fun start(context: android.content.Context, allowMobileNetwork: Boolean = false) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BotForegroundService::class.java).setAction(ACTION_START)
                    .putExtra(EXTRA_ALLOW_MOBILE_NETWORK, allowMobileNetwork)
            )
        }
    }

    private fun acquirePerformanceLocks() {
        runCatching {
            val power = getSystemService(PowerManager::class.java)
            wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Zappy:Runtime").apply {
                setReferenceCounted(false)
                acquire()
            }
        }
        runCatching {
            val wifi = applicationContext.getSystemService(WifiManager::class.java)
            wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Zappy:Wifi").apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releasePerformanceLocks() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
        runCatching { wifiLock?.takeIf { it.isHeld }?.release() }
        wifiLock = null
    }
}

private fun IncomingWhatsAppMessage.senderLabel(): String =
    senderName?.takeIf { it.isNotBlank() } ?: chatId.toReadableChatId()

private fun String.toReadableChatId(): String =
    substringBefore("@")
        .filter { it.isDigit() || it == '+' }
        .takeIf { it.isNotBlank() }
        ?: "WhatsApp contact"
