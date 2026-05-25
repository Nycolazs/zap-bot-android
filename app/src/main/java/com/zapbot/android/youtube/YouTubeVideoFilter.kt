package com.zapbot.android.youtube

object YouTubeVideoFilter {
    fun isAllowed(
        durationSeconds: Long,
        privacyStatus: String?,
        hasLiveStreamingDetails: Boolean,
        liveBroadcastContent: String?
    ): Boolean {
        if (durationSeconds < 61) return false
        if (privacyStatus != null && privacyStatus != "public") return false
        if (hasLiveStreamingDetails) return false
        if (liveBroadcastContent == "live" || liveBroadcastContent == "upcoming") return false
        return true
    }
}
