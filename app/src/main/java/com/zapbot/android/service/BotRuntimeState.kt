package com.zapbot.android.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BotRuntimeState {
    private val startedAtState = MutableStateFlow<Long?>(null)
    val startedAt: StateFlow<Long?> = startedAtState

    fun markStarted(startedAt: Long = System.currentTimeMillis()) {
        startedAtState.value = startedAt
    }

    fun markStopped() {
        startedAtState.value = null
    }
}
