package com.zapbot.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zapbot.android.AppContainer
import com.zapbot.android.BuildConfig
import com.zapbot.android.database.BotLogEntity
import com.zapbot.android.database.BotSettingsEntity
import com.zapbot.android.database.DownloadJobEntity
import com.zapbot.android.domain.WhatsAppConnectionState
import com.zapbot.android.service.BotRuntimeState
import com.zapbot.android.updates.UpdateCheckResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val settings: BotSettingsEntity = BotSettingsEntity(),
    val connection: WhatsAppConnectionState = WhatsAppConnectionState.Disconnected,
    val activeDownloads: Int = 0,
    val logs: List<BotLogEntity> = emptyList(),
    val jobs: List<DownloadJobEntity> = emptyList(),
    val uptimeText: String = "0 min",
    val messagesReceived: Int = 0,
    val searchesPerformed: Int = 0,
    val downloadsCompleted: Int = 0,
    val failedJobs: Int = 0,
    val hasWhatsAppSession: Boolean = false,
    val lastPairingCode: String? = null,
    val pairingError: String? = null
)

class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val pairingCode = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val pairingError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    val state: StateFlow<DashboardState> = combine(
        container.settings.settings,
        container.whatsappClient.connectionState,
        container.database.downloadJobDao().observeActiveCount(),
        container.database.logDao().observeRecent(),
        container.database.downloadJobDao().observeRecent(),
        container.whatsappClient.hasSavedSession,
        pairingCode,
        pairingError,
        BotRuntimeState.startedAt,
        ticker
    ) { values ->
        val settings = values[0] as BotSettingsEntity
        val connection = values[1] as WhatsAppConnectionState
        val active = values[2] as Int
        val logs = values[3] as List<BotLogEntity>
        val jobs = values[4] as List<DownloadJobEntity>
        DashboardState(
            settings = settings,
            connection = connection,
            activeDownloads = active,
            logs = logs,
            jobs = jobs.filter { it.createdAt >= System.currentTimeMillis() - 24 * 60 * 60 * 1_000L },
            uptimeText = uptimeText(values[8] as Long?, values[9] as Long),
            messagesReceived = logs.count { it.tag == "BotEngine" || it.tag == "Queue" },
            searchesPerformed = logs.count { it.message.contains("search", ignoreCase = true) },
            downloadsCompleted = jobs.count { it.status.name == "COMPLETED" },
            failedJobs = jobs.count { it.status.name == "FAILED" },
            hasWhatsAppSession = values[5] as Boolean,
            lastPairingCode = values[6] as String?,
            pairingError = values[7] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    fun updateSettings(transform: (BotSettingsEntity) -> BotSettingsEntity) {
        viewModelScope.launch { container.settings.update(transform) }
    }

    fun clearLogs() {
        viewModelScope.launch { container.database.logDao().clear() }
    }

    fun clearSession() {
        viewModelScope.launch {
            container.whatsappClient.clearSession()
            pairingCode.value = null
            pairingError.value = null
        }
    }

    fun requestPairingCode(phoneNumber: String) {
        viewModelScope.launch {
            val normalized = phoneNumber.filter { it.isDigit() }
            if (normalized.length < 10) {
                pairingError.value = "Enter the WhatsApp number with country and area code, for example 15551234567."
                return@launch
            }
            pairingError.value = null
            pairingCode.value = runCatching {
                container.whatsappClient.requestPairingCode(normalized)
            }.onFailure {
                pairingError.value = it.message ?: "Could not generate a code."
            }.getOrNull()
        }
    }

    suspend fun checkForUpdates(language: String): String =
        when (val result = container.updateChecker.check(language, BuildConfig.VERSION_NAME)) {
            is UpdateCheckResult.Message -> result.message
        }

    private fun uptimeText(startedAt: Long?, now: Long): String {
        if (startedAt == null) return "0s"
        val totalSeconds = ((now - startedAt) / 1_000).coerceAtLeast(0)
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(container) as T
    }
}
