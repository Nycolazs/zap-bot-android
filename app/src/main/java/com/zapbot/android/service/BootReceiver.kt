package com.zapbot.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zapbot.android.ZapBotApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ZapBotApplication
                val settings = app.container.settings.get()
                if (settings.autoStartOnBoot) {
                    if (settings.networkPreference == "ANY_NETWORK" || app.container.networkMonitor.isOnWifi()) {
                        BotForegroundService.start(context)
                    } else {
                        app.container.logger.warn("BootReceiver", "Auto start skipped because network preference is Wi-Fi only and current network is not Wi-Fi")
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
