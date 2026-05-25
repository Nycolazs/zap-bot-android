package com.zapbot.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zapbot.android.database.BotSettingsEntity
import com.zapbot.android.domain.WhatsAppConnectionState
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    modifier: Modifier,
    settings: BotSettingsEntity,
    connection: WhatsAppConnectionState,
    hasWhatsAppSession: Boolean,
    onUpdate: ((BotSettingsEntity) -> BotSettingsEntity) -> Unit,
    onClearSession: () -> Unit,
    onBattery: () -> Unit,
    onRequestPairingCode: (String) -> Unit,
    pairingCode: String?,
    pairingError: String?
) {
    var phone by remember { mutableStateOf("") }
    var cooldownSeconds by remember { mutableIntStateOf(0) }
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1_000)
            cooldownSeconds -= 1
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("WhatsApp integration", style = MaterialTheme.typography.titleMedium)
                AnimatedVisibility(visible = hasWhatsAppSession) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("OK - WhatsApp session saved", style = MaterialTheme.typography.titleSmall)
                            Text(
                                sessionStatus(connection),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = !hasWhatsAppSession) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "This bot connects as a real linked device for your personal WhatsApp account. Enter the number from the main phone, then approve the code in WhatsApp > Linked devices.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                val digitsOnly = it.filter(Char::isDigit)
                                if (digitsOnly != phone) {
                                    phone = digitsOnly
                                    cooldownSeconds = 0
                                }
                            },
                            label = { Text("WhatsApp number") },
                            placeholder = { Text("15551234567") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                cooldownSeconds = PAIRING_CODE_COOLDOWN_SECONDS
                                onRequestPairingCode(phone)
                            },
                            enabled = phone.length >= 10 && cooldownSeconds == 0,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (cooldownSeconds > 0) {
                                    "Generate again in ${cooldownSeconds}s"
                                } else {
                                    "Generate Linked Devices code"
                                }
                            )
                        }
                        if (pairingCode != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { clipboard.setText(AnnotatedString(pairingCode)) },
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Code: $pairingCode",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy code")
                            }
                            Text("Tap the code to copy it.", style = MaterialTheme.typography.bodySmall)
                            Text("In WhatsApp: Linked devices > Link with phone number.")
                        }
                        if (pairingError != null) {
                            Text(pairingError, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Text(
                    "Regular messages are ignored silently. The bot only replies to slash commands such as /help, /music search, /v, /a, /status, and /cancel.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        ToggleRow("Start when the phone boots", settings.autoStartOnBoot) { onUpdate { it.copy(autoStartOnBoot = !it.autoStartOnBoot) } }
        ToggleRow("Notifications", settings.notificationsEnabled) { onUpdate { it.copy(notificationsEnabled = !it.notificationsEnabled) } }
        ToggleRow("Detailed notifications", settings.detailedNotificationsEnabled) { onUpdate { it.copy(detailedNotificationsEnabled = !it.detailedNotificationsEnabled) } }
        ThemeModeSection(settings.themeMode) { themeMode ->
            onUpdate { it.copy(themeMode = themeMode) }
        }
        FilledTonalButton(onClick = onBattery, modifier = Modifier.fillMaxWidth()) {
            Text("Open battery settings")
        }
        HorizontalDivider()
        Text("Simultaneous downloads", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { value ->
                FilterChip(selected = settings.maxConcurrentDownloads == value, onClick = { onUpdate { it.copy(maxConcurrentDownloads = value) } }, label = { Text("$value") })
            }
        }
        Text("Video quality", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("360p", "480p", "720p").forEach { value ->
                FilterChip(selected = settings.videoQualityLimit == value, onClick = { onUpdate { it.copy(videoQualityLimit = value) } }, label = { Text(value) })
            }
        }
        Text("Audio", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("96k", "128k", "160k").forEach { value ->
                FilterChip(selected = settings.audioBitrate == value, onClick = { onUpdate { it.copy(audioBitrate = value) } }, label = { Text(value) })
            }
        }
        Button(onClick = onClearSession, modifier = Modifier.fillMaxWidth()) { Text("Clear WhatsApp cache") }
    }
}

private const val PAIRING_CODE_COOLDOWN_SECONDS = 15

@Composable
private fun ThemeModeSection(themeMode: String, onThemeModeChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "system" to "System",
                "light" to "Light",
                "dark" to "Dark"
            ).forEach { (value, label) ->
                FilterChip(
                    selected = themeMode == value,
                    onClick = { onThemeModeChange(value) },
                    label = { Text(label) }
                )
            }
        }
    }
}

private fun sessionStatus(connection: WhatsAppConnectionState): String = when (connection) {
    is WhatsAppConnectionState.Connected -> "This bot already has a saved WhatsApp session${connection.phoneNumber?.let { " for $it" }.orEmpty()}."
    WhatsAppConnectionState.Running -> "This bot already has a saved WhatsApp session and is running."
    WhatsAppConnectionState.Disconnected -> "This bot already has a saved WhatsApp session. Start the bot to connect."
    else -> "This bot already has a saved WhatsApp session."
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
