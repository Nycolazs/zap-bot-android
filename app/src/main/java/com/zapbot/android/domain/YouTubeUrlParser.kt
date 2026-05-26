package com.zapbot.android.domain

import java.net.URI
import java.net.URLDecoder

data class YouTubeUrl(
    val originalUrl: String,
    val kind: Kind
) {
    enum class Kind {
        VIDEO,
        PLAYLIST
    }
}

object YouTubeUrlParser {
    fun parse(value: String): YouTubeUrl? {
        val trimmed = value.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        if (host !in YOUTUBE_HOSTS) return null

        val query = uri.rawQuery.queryParameters()
        val pathSegments = uri.rawPath.orEmpty().trim('/').split('/').filter { it.isNotBlank() }
        val hasVideoId = query["v"].isYouTubeId() ||
            (host == "youtu.be" && pathSegments.firstOrNull().isYouTubeId()) ||
            (pathSegments.firstOrNull() in SHORTS_PATHS && pathSegments.getOrNull(1).isYouTubeId())
        val hasPlaylistId = query["list"]?.isNotBlank() == true ||
            pathSegments.firstOrNull() == "playlist"

        val kind = when {
            hasVideoId -> YouTubeUrl.Kind.VIDEO
            hasPlaylistId -> YouTubeUrl.Kind.PLAYLIST
            else -> return null
        }
        return YouTubeUrl(trimmed, kind)
    }

    private fun String?.isYouTubeId(): Boolean =
        this != null && matches(Regex("[A-Za-z0-9_-]{6,}"))

    private fun String?.queryParameters(): Map<String, String> {
        if (isNullOrBlank()) return emptyMap()
        return split('&')
            .mapNotNull { part ->
                val key = part.substringBefore('=').decodeUrl().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val value = part.substringAfter('=', "").decodeUrl()
                key to value
            }
            .toMap()
    }

    private fun String.decodeUrl(): String =
        runCatching { URLDecoder.decode(this, "UTF-8") }.getOrDefault(this)

    private val YOUTUBE_HOSTS = setOf("youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be")
    private val SHORTS_PATHS = setOf("shorts", "live", "embed")
}
