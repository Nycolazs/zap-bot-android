package com.zapbot.android.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zapbot.android.database.BotLogEntity
import com.zapbot.android.domain.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    modifier: Modifier,
    logs: List<BotLogEntity>,
    showErrorsOnly: Boolean = false,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val visibleLogs = remember(logs, showErrorsOnly) {
        if (showErrorsOnly) {
            logs.filter { it.level == LogLevel.ERROR || it.level == LogLevel.WARN }
        } else {
            logs
        }
    }
    var formattedMessages by remember(visibleLogs) { mutableStateOf<Map<Long, String>>(emptyMap()) }

    LaunchedEffect(visibleLogs) {
        formattedMessages = withContext(Dispatchers.IO) {
            visibleLogs.associate { it.id to formatPeople(context, it.message) }
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(if (showErrorsOnly) "Recent errors" else "Logs", style = MaterialTheme.typography.headlineSmall)
                if (showErrorsOnly) {
                    Text("Warnings and failures from the bot", style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = onClear) { Text("Clear") }
        }
        if (visibleLogs.isEmpty()) {
            Text(if (showErrorsOnly) "No recent errors." else "No logs yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleLogs, key = { it.id }) { log ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${log.level} - ${log.tag} - ${time(log.createdAt)}", style = MaterialTheme.typography.labelMedium)
                            Text(formattedMessages[log.id] ?: log.message)
                            if (log.throwable != null) Text(log.throwable, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

private fun time(value: Long): String =
    SimpleDateFormat("h:mm a", Locale.US).format(Date(value)).lowercase(Locale.US)

private fun formatPeople(context: Context, text: String): String {
    val phoneFormatted = PHONE_JID_REGEX.replace(text) { match ->
        val phone = match.groupValues[1]
        lookupContactName(context, phone) ?: phoneDisplay(phone)
    }
    return LID_REGEX.replace(phoneFormatted) { "WhatsApp contact" }
}

private fun lookupContactName(context: Context, phone: String): String? = runCatching {
    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
    context.contentResolver.query(
        uri,
        arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()?.takeIf { it.isNotBlank() }

private fun phoneDisplay(phone: String): String = if (phone.startsWith("+")) phone else "+$phone"

private val PHONE_JID_REGEX = Regex("""\b(\+?\d{8,20})@(s\.whatsapp\.net|c\.us)\b""")
private val LID_REGEX = Regex("""\b[A-Za-z0-9._-]+@lid\b""")
