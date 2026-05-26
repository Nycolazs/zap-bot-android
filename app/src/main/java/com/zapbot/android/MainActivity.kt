package com.zapbot.android

import android.Manifest
import android.content.res.Configuration
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import com.zapbot.android.service.BotForegroundService
import com.zapbot.android.ui.AppStrings
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
        val container = (application as ZapBotApplication).container
        if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        setContent {
            val state by viewModel.state.collectAsState()
            var selectedPage by rememberSaveable { mutableStateOf(0) }
            var showErrorsOnly by rememberSaveable { mutableStateOf(false) }
            var showMobileStartDialog by rememberSaveable { mutableStateOf(false) }
            var showWhatsAppRequiredDialog by rememberSaveable { mutableStateOf(false) }
            Crossfade(targetState = state.settings.themeMode, animationSpec = tween(450), label = "themeMode") { themeMode ->
                ZapBotTheme(themeMode = themeMode) {
                    val configuration = LocalConfiguration.current
                    val darkSystemBars = when (themeMode) {
                        "dark" -> true
                        "light" -> false
                        else -> configuration.isNightMode()
                    }
                    LaunchedEffect(darkSystemBars) {
                        window.statusBarColor = Color.TRANSPARENT
                        window.navigationBarColor = Color.TRANSPARENT
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            isAppearanceLightStatusBars = !darkSystemBars
                            isAppearanceLightNavigationBars = !darkSystemBars
                        }
                    }
                    fun t(key: String) = AppStrings.label(state.settings.appLanguage, key)
                    if (showWhatsAppRequiredDialog) {
                        AlertDialog(
                            onDismissRequest = { showWhatsAppRequiredDialog = false },
                            title = { Text(t("whatsapp_integration")) },
                            text = { Text(t("start_requires_whatsapp")) },
                            confirmButton = {
                                TextButton(onClick = {
                                    showWhatsAppRequiredDialog = false
                                    selectedPage = 3
                                }) {
                                    Text(t("settings"))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showWhatsAppRequiredDialog = false }) {
                                    Text(t("cancel"))
                                }
                            }
                        )
                    }
                    if (showMobileStartDialog) {
                        AlertDialog(
                            onDismissRequest = { showMobileStartDialog = false },
                            title = { Text(t("network_preference")) },
                            text = { Text(t("mobile_start_warning")) },
                            confirmButton = {
                                TextButton(onClick = {
                                    showMobileStartDialog = false
                                    BotForegroundService.start(this, allowMobileNetwork = true)
                                }) {
                                    Text(t("start_anyway"))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showMobileStartDialog = false }) {
                                    Text(t("cancel"))
                                }
                            }
                        )
                    }
                    MainApp(
                        viewModel = viewModel,
                        selectedPage = selectedPage,
                        onSelectedPageChange = { selectedPage = it },
                        showErrorsOnly = showErrorsOnly,
                        onShowErrorsOnlyChange = { showErrorsOnly = it },
                        onStart = {
                            if (!state.hasWhatsAppSession) {
                                showWhatsAppRequiredDialog = true
                            } else if (state.settings.networkPreference == "WIFI_ONLY" && !container.networkMonitor.isOnWifi()) {
                                showMobileStartDialog = true
                            } else {
                                BotForegroundService.start(this)
                            }
                        },
                        onStop = { startService(Intent(this, BotForegroundService::class.java).setAction(BotForegroundService.ACTION_STOP)) },
                        onOpenWhatsAppSettings = {
                            selectedPage = 3
                        },
                        onBattery = {
                            runCatching {
                                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:$packageName")))
                            }.onFailure {
                                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName")))
                            }
                        },
                        appVersion = BuildConfig.VERSION_NAME
                    )
                }
            }
        }
    }
}

private fun Configuration.isNightMode(): Boolean =
    (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
