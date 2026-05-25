package com.zapbot.android

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zapbot.android.service.BotForegroundService
import com.zapbot.android.ui.MainApp
import com.zapbot.android.ui.MainViewModel
import com.zapbot.android.ui.theme.ZapBotTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as ZapBotApplication).container)
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val contactsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        setContent {
            val state by viewModel.state.collectAsState()
            ZapBotTheme(themeMode = state.settings.themeMode) {
                MainApp(
                    viewModel = viewModel,
                    onStart = { BotForegroundService.start(this) },
                    onStop = { startService(Intent(this, BotForegroundService::class.java).setAction(BotForegroundService.ACTION_STOP)) },
                    onBattery = {
                        runCatching {
                            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:$packageName")))
                        }.onFailure {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName")))
                        }
                    }
                )
            }
        }
    }
}
