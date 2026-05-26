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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.zapbot.android.ui.AppStrings
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
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
    fun t(key: String) = AppStrings.label(settings.appLanguage, key)
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
        Text(t("settings"), style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t("whatsapp_integration"), style = MaterialTheme.typography.titleMedium)
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
                            Text(t("session_saved"), style = MaterialTheme.typography.titleSmall)
                            Text(
                                sessionStatus(connection, ::t),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = !hasWhatsAppSession) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            t("pairing_intro"),
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
                            label = { Text(t("whatsapp_number")) },
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
                                    "${t("generate_again")} ${cooldownSeconds}s"
                                } else {
                                    t("generate_code")
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
                                    "${t("code")}: $pairingCode",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Outlined.ContentCopy, contentDescription = t("copy_code"))
                            }
                            Text(t("tap_copy"), style = MaterialTheme.typography.bodySmall)
                            Text(t("link_phone"))
                        }
                        if (pairingError != null) {
                            Text(pairingError, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Text(
                    t("regular_messages"),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        ToggleRow(t("start_boot"), settings.autoStartOnBoot) { onUpdate { it.copy(autoStartOnBoot = !it.autoStartOnBoot) } }
        ToggleRow(t("notifications"), settings.notificationsEnabled) { onUpdate { it.copy(notificationsEnabled = !it.notificationsEnabled) } }
        ToggleRow(t("detailed_notifications"), settings.detailedNotificationsEnabled) { onUpdate { it.copy(detailedNotificationsEnabled = !it.detailedNotificationsEnabled) } }
        ThemeModeSection(::t, settings.themeMode) { themeMode ->
            onUpdate { it.copy(themeMode = themeMode) }
        }
        LanguageSection(
            title = t("app_language"),
            selected = settings.appLanguage,
            onSelected = { language -> onUpdate { it.copy(appLanguage = language) } }
        )
        LanguageSection(
            title = t("bot_language"),
            selected = settings.botLanguage,
            onSelected = { language -> onUpdate { it.copy(botLanguage = language) } }
        )
        FilledTonalButton(onClick = onBattery, modifier = Modifier.fillMaxWidth()) {
            Text(t("battery"))
        }
        HorizontalDivider()
        Text(t("downloads"), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..8).forEach { value ->
                FilterChip(selected = settings.maxConcurrentDownloads == value, onClick = { onUpdate { it.copy(maxConcurrentDownloads = value) } }, label = { Text("$value") })
            }
        }
        Text(t("quality"), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("360p", "480p", "720p").forEach { value ->
                FilterChip(selected = settings.videoQualityLimit == value, onClick = { onUpdate { it.copy(videoQualityLimit = value) } }, label = { Text(value) })
            }
        }
        Text(t("audio"), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("96k", "128k", "160k").forEach { value ->
                FilterChip(selected = settings.audioBitrate == value, onClick = { onUpdate { it.copy(audioBitrate = value) } }, label = { Text(value) })
            }
        }
        Button(onClick = onClearSession, modifier = Modifier.fillMaxWidth()) { Text(t("clear_cache")) }
    }
}

private const val PAIRING_CODE_COOLDOWN_SECONDS = 15

@Composable
private fun ThemeModeSection(t: (String) -> String, themeMode: String, onThemeModeChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("appearance"), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "system" to t("system"),
                "light" to t("light"),
                "dark" to t("dark")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSection(title: String, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "en" to "English",
        "pt" to "Português",
        "es" to "Español",
        "ru" to "Русский"
    )
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: "English"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun sessionStatus(connection: WhatsAppConnectionState, t: (String) -> String): String = when (connection) {
    is WhatsAppConnectionState.Connected -> "${t("session_saved_detail")}${connection.phoneNumber?.let { " $it" }.orEmpty()}"
    WhatsAppConnectionState.Running -> t("session_running")
    WhatsAppConnectionState.Disconnected -> t("session_disconnected")
    else -> t("session_saved_detail")
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
