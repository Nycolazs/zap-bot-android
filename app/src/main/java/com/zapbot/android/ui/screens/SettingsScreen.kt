package com.zapbot.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    pairingError: String?,
    appVersion: String,
    onCheckUpdates: suspend (String) -> String
) {
    fun t(key: String) = AppStrings.label(settings.appLanguage, key)
    var phone by remember { mutableStateOf("") }
    var cooldownSeconds by remember { mutableIntStateOf(0) }
    var confirmAnyNetwork by remember { mutableStateOf(false) }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(t("settings"), style = MaterialTheme.typography.headlineSmall)
        Text(
            t("settings_subtitle"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingsSection(
            title = t("whatsapp_integration"),
            subtitle = t("whatsapp_section_desc")
        ) {
            AnimatedVisibility(visible = hasWhatsAppSession) {
                StatusRow(::t, connection)
            }

            AnimatedVisibility(visible = !hasWhatsAppSession) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        t("pairing_intro"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text(t("link_phone"), style = MaterialTheme.typography.bodySmall)
                    }
                    if (pairingError != null) {
                        Text(pairingError, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(
                t("regular_messages"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Text(t("blacklist"), style = MaterialTheme.typography.titleSmall)
            Text(
                t("blacklist_desc"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BlacklistEditor(
                value = settings.blacklistedNumbers,
                t = ::t,
                onChange = { value -> onUpdate { it.copy(blacklistedNumbers = value) } }
            )
        }

        SettingsSection(
            title = t("preferences"),
            subtitle = t("preferences_desc")
        ) {
            ThemeModeSection(::t, settings.themeMode) { themeMode ->
                onUpdate { it.copy(themeMode = themeMode) }
            }
            HorizontalDivider()
            LanguageSection(
                title = t("app_language"),
                selected = settings.appLanguage,
                systemLabel = t("system_language"),
                onSelected = { language -> onUpdate { it.copy(appLanguage = language) } }
            )
            LanguageSection(
                title = t("bot_language"),
                selected = settings.botLanguage,
                systemLabel = t("system_language"),
                onSelected = { language -> onUpdate { it.copy(botLanguage = language) } }
            )
            HorizontalDivider()
            NetworkPreferenceSection(::t, settings.networkPreference) { value ->
                if (value == "ANY_NETWORK" && settings.networkPreference != "ANY_NETWORK") {
                    confirmAnyNetwork = true
                } else {
                    onUpdate { it.copy(networkPreference = value) }
                }
            }
        }

        SettingsSection(
            title = t("automation"),
            subtitle = t("automation_desc")
        ) {
            ToggleRow(t("start_boot"), t("start_boot_desc"), settings.autoStartOnBoot) {
                onUpdate { it.copy(autoStartOnBoot = !it.autoStartOnBoot) }
            }
            ToggleRow(t("notifications"), t("notifications_desc"), settings.notificationsEnabled) {
                onUpdate { it.copy(notificationsEnabled = !it.notificationsEnabled) }
            }
            ToggleRow(t("detailed_notifications"), t("detailed_notifications_desc"), settings.detailedNotificationsEnabled) {
                onUpdate { it.copy(detailedNotificationsEnabled = !it.detailedNotificationsEnabled) }
            }
            ToggleRow(t("welcome_messages"), t("welcome_messages_desc"), settings.welcomeMessagesEnabled) {
                onUpdate { it.copy(welcomeMessagesEnabled = !it.welcomeMessagesEnabled) }
            }
            FilledTonalButton(onClick = onBattery, modifier = Modifier.fillMaxWidth()) {
                Text(t("battery"))
            }
        }

        SettingsSection(
            title = t("downloads"),
            subtitle = t("downloads_desc")
        ) {
            Text(t("simultaneous_downloads"), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..8).forEach { value ->
                    FilterChip(selected = settings.maxConcurrentDownloads == value, onClick = { onUpdate { it.copy(maxConcurrentDownloads = value) } }, label = { Text("$value") })
                }
            }
            Text(t("quality"), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("360p", "480p", "720p").forEach { value ->
                    FilterChip(selected = settings.videoQualityLimit == value, onClick = { onUpdate { it.copy(videoQualityLimit = value) } }, label = { Text(value) })
                }
            }
            Text(t("audio"), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("96k", "128k", "160k").forEach { value ->
                    FilterChip(selected = settings.audioBitrate == value, onClick = { onUpdate { it.copy(audioBitrate = value) } }, label = { Text(value) })
                }
            }
            Button(onClick = onClearSession, modifier = Modifier.fillMaxWidth()) {
                Text(t("clear_cache"))
            }
        }

        AboutCard(
            language = settings.appLanguage,
            appVersion = appVersion,
            onCheckUpdates = onCheckUpdates
        )
    }

    if (confirmAnyNetwork) {
        AlertDialog(
            onDismissRequest = { confirmAnyNetwork = false },
            title = { Text(t("network_preference")) },
            text = { Text(t("any_network_warning")) },
            confirmButton = {
                Button(onClick = {
                    confirmAnyNetwork = false
                    onUpdate { it.copy(networkPreference = "ANY_NETWORK") }
                }) {
                    Text(t("confirm"))
                }
            },
            dismissButton = {
                FilledTonalButton(onClick = { confirmAnyNetwork = false }) {
                    Text(t("cancel"))
                }
            }
        )
    }
}

private const val PAIRING_CODE_COOLDOWN_SECONDS = 15

@Composable
private fun BlacklistEditor(
    value: String,
    t: (String) -> String,
    onChange: (String) -> Unit
) {
    val numbers = remember(value) {
        value.lineSequence()
            .map { it.filter(Char::isDigit) }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }
    var draft by remember { mutableStateOf("") }

    fun persist(next: List<String>) {
        onChange(next.map { it.filter(Char::isDigit) }.filter { it.isNotBlank() }.distinct().joinToString("\n"))
    }

    fun addDraft() {
        val normalized = draft.filter(Char::isDigit)
        if (normalized.isBlank()) return
        persist(numbers + normalized)
        draft = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.filter(Char::isDigit) },
                label = { Text(t("blocklist_new_number")) },
                placeholder = { Text("5511999999999") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = ::addDraft,
                enabled = draft.filter(Char::isDigit).isNotBlank()
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(t("add_blocklist_number"), modifier = Modifier.padding(start = 6.dp))
            }
        }

        if (numbers.isEmpty()) {
            EmptyBlocklistState(t("blocklist_empty"))
        } else {
            Text(
                t("blocklist_active_numbers"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        numbers.forEach { number ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BlockedNumberItem(number = number, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { persist(numbers - number) }
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = t("remove_blocklist_number"))
                }
            }
        }
    }
}

@Composable
private fun EmptyBlocklistState(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BlockedNumberItem(number: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(number, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun StatusRow(t: (String) -> String, connection: WhatsAppConnectionState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                sessionStatus(connection, t),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeModeSection(t: (String) -> String, themeMode: String, onThemeModeChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("appearance"), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetworkPreferenceSection(t: (String) -> String, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("network_preference"), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "WIFI_ONLY" to t("wifi_only"),
                "ANY_NETWORK" to t("any_network")
            ).forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSection(title: String, selected: String, systemLabel: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "system" to systemLabel,
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
private fun ToggleRow(label: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
