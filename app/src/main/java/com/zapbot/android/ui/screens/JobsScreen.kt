package com.zapbot.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zapbot.android.database.DownloadJobEntity
import com.zapbot.android.domain.DownloadStatus
import com.zapbot.android.domain.DownloadType
import com.zapbot.android.ui.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JobsScreen(modifier: Modifier, jobs: List<DownloadJobEntity>, language: String = "en") {
    fun t(key: String) = AppStrings.label(language, key)
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(t("jobs"), style = MaterialTheme.typography.headlineSmall)
        if (jobs.isEmpty()) {
            Text(t("no_jobs"))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(jobs, key = { it.id }) { job ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(job.title, style = MaterialTheme.typography.titleMedium)
                            Text("${t("time")}: ${formatJobTime(job.createdAt)}", style = MaterialTheme.typography.bodySmall)
                            Text("${typeLabel(job.type, ::t)} - ${statusLabel(job.status, ::t)} - ${t("chat")} ${job.chatId}")
                            LinearProgressIndicator(progress = { job.progress / 100f }, modifier = Modifier.fillMaxWidth())
                            if (job.errorMessage != null) Text(job.errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

private fun formatJobTime(timestamp: Long): String =
    SimpleDateFormat("h:mm a", Locale.US).format(Date(timestamp)).lowercase(Locale.US)

private fun typeLabel(type: DownloadType, t: (String) -> String): String = when (type) {
    DownloadType.VIDEO -> t("video")
    DownloadType.AUDIO -> t("audio_type")
}

private fun statusLabel(status: DownloadStatus, t: (String) -> String): String = when (status) {
    DownloadStatus.QUEUED -> t("queued")
    DownloadStatus.DOWNLOADING -> t("downloading")
    DownloadStatus.PROCESSING -> t("processing")
    DownloadStatus.SENDING -> t("sending")
    DownloadStatus.COMPLETED -> t("completed")
    DownloadStatus.FAILED -> t("failed")
    DownloadStatus.CANCELLED -> t("cancelled")
}
