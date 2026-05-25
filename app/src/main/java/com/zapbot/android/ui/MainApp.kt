package com.zapbot.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zapbot.android.ui.screens.HomeScreen
import com.zapbot.android.ui.screens.JobsScreen
import com.zapbot.android.ui.screens.LogsScreen
import com.zapbot.android.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainApp(
    viewModel: MainViewModel,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onBattery: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val items = listOf("Home", "Jobs", "Logs", "Settings")
    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()
    var showErrorsOnly by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 2) showErrorsOnly = false
    }

    fun openErrors() {
        showErrorsOnly = true
        scope.launch { pagerState.animateScrollToPage(2) }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            if (index != 2) showErrorsOnly = false
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
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
        HorizontalPager(state = pagerState, modifier = modifier) { page ->
            when (page) {
                0 -> HomeScreen(Modifier, state, onStart, onStop, onFailedClick = ::openErrors)
                1 -> JobsScreen(Modifier, state.jobs)
                2 -> LogsScreen(Modifier, state.logs, showErrorsOnly = showErrorsOnly, onClear = viewModel::clearLogs)
                3 -> SettingsScreen(
                    Modifier,
                    state.settings,
                    state.connection,
                    state.hasWhatsAppSession,
                    viewModel::updateSettings,
                    viewModel::clearSession,
                    onBattery,
                    viewModel::requestPairingCode,
                    state.lastPairingCode,
                    state.pairingError
                )
            }
        }
    }
}
