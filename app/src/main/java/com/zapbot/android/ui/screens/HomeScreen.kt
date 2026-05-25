package com.zapbot.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.zapbot.android.domain.WhatsAppConnectionState
import com.zapbot.android.ui.DashboardState

@Composable
fun HomeScreen(
    modifier: Modifier,
    state: DashboardState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onBattery: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { -it / 4 }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ZapTube Bot", style = MaterialTheme.typography.headlineLarge)
                Text("WhatsApp automation console", style = MaterialTheme.typography.bodyMedium)
            }
        }
        AnimatedVisibility(visible = true, enter = fadeIn(tween(550))) {
            StatusCard(state)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStart) { Text("Start bot") }
            FilledTonalButton(onClick = onStop) { Text("Stop") }
        }
        FilledTonalButton(onClick = onBattery, modifier = Modifier.fillMaxWidth()) {
            Text("Open battery settings")
        }
        AnimatedVisibility(
            visible = state.connection is WhatsAppConnectionState.WaitingForQr,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut()
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("QR Code", style = MaterialTheme.typography.titleMedium)
                    Text((state.connection as WhatsAppConnectionState.WaitingForQr).qrData, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        StatsCard(state)
    }
}

@Composable
private fun StatusCard(state: DashboardState) {
    val pulse = rememberInfiniteTransition(label = "statusPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "statusAlpha"
    )
    val label = when (val connection = state.connection) {
        WhatsAppConnectionState.Disconnected -> "Disconnected"
        is WhatsAppConnectionState.WaitingForQr -> "Waiting for QR code"
        WhatsAppConnectionState.Connecting -> "Connecting"
        is WhatsAppConnectionState.Connected -> "Connected ${connection.phoneNumber.orEmpty()}"
        WhatsAppConnectionState.Running -> "Running"
        is WhatsAppConnectionState.Error -> "Error: ${connection.message}"
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(12.dp)
                        .alpha(if (state.connection == WhatsAppConnectionState.Running) pulse.value else 1f)
                        .background(
                            if (state.connection == WhatsAppConnectionState.Running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                )
                Text("Status", style = MaterialTheme.typography.titleMedium)
            }
            Text(label)
            Text("Active downloads: ${state.activeDownloads}")
            val error = (state.connection as? WhatsAppConnectionState.Error)?.message
            if (error != null) Text("Last error: $error", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun StatsCard(state: DashboardState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Stats", style = MaterialTheme.typography.titleMedium)
            Text("Uptime: ${state.uptimeText}")
            Text("Messages received: ${state.messagesReceived}")
            Text("Searches completed: ${state.searchesPerformed}")
            Text("Completed downloads: ${state.downloadsCompleted}")
            Text("Failed jobs: ${state.failedJobs}")
            Text("Active jobs: ${state.activeDownloads}")
        }
    }
}
