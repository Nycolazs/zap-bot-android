package com.zapbot.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zapbot.android.ui.screens.HomeScreen
import com.zapbot.android.ui.screens.JobsScreen
import com.zapbot.android.ui.screens.LogsScreen
import com.zapbot.android.ui.screens.SettingsScreen

@Composable
fun MainApp(
    viewModel: MainViewModel,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onBattery: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Jobs", "Logs", "Settings")
    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = {
                            Icon(
                                when (index) {
                                    0 -> Icons.Outlined.Home
                                    1 -> Icons.Outlined.Work
                                    2 -> Icons.Outlined.List
                                    else -> Icons.Outlined.Settings
                                },
                                contentDescription = label
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            0 -> HomeScreen(modifier, state, onStart, onStop, onBattery)
            1 -> JobsScreen(modifier, state.jobs)
            2 -> LogsScreen(modifier, state.logs, onClear = viewModel::clearLogs)
            3 -> SettingsScreen(
                modifier,
                state.settings,
                state.connection,
                state.hasWhatsAppSession,
                viewModel::updateSettings,
                viewModel::clearSession,
                viewModel::requestPairingCode,
                state.lastPairingCode,
                state.pairingError
            )
        }
    }
}
