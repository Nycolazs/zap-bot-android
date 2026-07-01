package com.zapbot.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zapbot.android.domain.WhatsAppConnectionState
import com.zapbot.android.ui.AppStrings
import com.zapbot.android.ui.DashboardState

@Composable
fun HomeScreen(
    modifier: Modifier,
    state: DashboardState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onConnectWhatsAppClick: () -> Unit,
    onFailedClick: () -> Unit
) {
    fun t(key: String) = AppStrings.label(state.settings.appLanguage, key)
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { -it / 4 }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Zappy", style = MaterialTheme.typography.headlineMedium)
                Text(
                    t("subtitle"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        AnimatedVisibility(visible = true, enter = fadeIn(tween(550))) {
            StatusHero(state, ::t)
        }
        BotActionButton(state, ::t, onStart, onStop)
        AnimatedVisibility(
            visible = !state.hasWhatsAppSession && state.connection !is WhatsAppConnectionState.Running,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onConnectWhatsAppClick)
            ) {
                Text(
                    t("start_requires_whatsapp"),
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        AnimatedVisibility(
            visible = state.connection is WhatsAppConnectionState.WaitingForQr,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut()
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("QR Code", style = MaterialTheme.typography.titleMedium)
                    Text(
                        (state.connection as WhatsAppConnectionState.WaitingForQr).qrData,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        StatsCard(state, ::t, onFailedClick)
    }
}

@Composable
private fun BotActionButton(
    state: DashboardState,
    t: (String) -> String,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val connection = state.connection
    val running = connection !is WhatsAppConnectionState.Disconnected && connection !is WhatsAppConnectionState.Error
    Crossfade(targetState = running, label = "botAction") { isRunning ->
        if (isRunning) {
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Outlined.Stop, contentDescription = null)
                Text(t("stop_bot"))
            }
        } else {
            val pulse = rememberInfiniteTransition(label = "startPulse").animateFloat(
                initialValue = 0.98f,
                targetValue = 1.03f,
                animationSpec = infiniteRepeatable(animation = tween(850), repeatMode = RepeatMode.Reverse),
                label = "startScale"
            )
            Button(
                onClick = onStart,
                enabled = state.hasWhatsAppSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pulse.value)
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Text(t("start_bot"))
            }
        }
    }
}

@Composable
private fun StatusHero(state: DashboardState, t: (String) -> String) {
    val pulse = rememberInfiniteTransition(label = "statusPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "statusAlpha"
    )
    val label = when (val connection = state.connection) {
        WhatsAppConnectionState.Disconnected -> t("disconnected")
        is WhatsAppConnectionState.WaitingForQr -> t("waiting_qr")
        WhatsAppConnectionState.Connecting -> t("connecting")
        is WhatsAppConnectionState.Connected -> "${t("connected")} ${connection.phoneNumber.orEmpty()}"
        WhatsAppConnectionState.Running -> t("running")
        is WhatsAppConnectionState.Error -> "Error: ${connection.message}"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .size(14.dp)
                        .alpha(if (state.connection == WhatsAppConnectionState.Running) pulse.value else 1f)
                        .background(
                            if (state.connection == WhatsAppConnectionState.Running) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            CircleShape
                        )
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                    Text(t("bot_status"), style = MaterialTheme.typography.titleMedium)
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            val error = (state.connection as? WhatsAppConnectionState.Error)?.message
            if (error != null) {
                Text("${t("last_error")}: $error", color = MaterialTheme.colorScheme.error)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactMetric(t("active"), state.activeDownloads.toString(), modifier = Modifier.weight(1f))
                    CompactMetric(t("uptime"), state.uptimeText, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatsCard(state: DashboardState, t: (String) -> String, onFailedClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t("today"), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatItem(Icons.Outlined.Chat, t("messages"), state.messagesReceived.toString(), Modifier.weight(1f))
                StatItem(Icons.Outlined.Search, t("searches"), state.searchesPerformed.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatItem(Icons.Outlined.DownloadDone, t("completed"), state.downloadsCompleted.toString(), Modifier.weight(1f))
                StatItem(
                    Icons.Outlined.ErrorOutline,
                    t("failed"),
                    state.failedJobs.toString(),
                    Modifier.weight(1f),
                    onClick = onFailedClick,
                    isError = state.failedJobs > 0
                )
            }
        }
    }
}

@Composable
private fun CompactMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isError: Boolean = false
) {
    val scale = animateFloatAsState(
        targetValue = if (onClick != null && isError) 1.02f else 1f,
        animationSpec = tween(250),
        label = "statScale"
    )
    val containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val iconColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .scale(scale.value)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
