package com.zapbot.android.domain

import java.net.URI

data class MediaUrl(
    val originalUrl: String,
    val source: Source,
    val kind: Kind
) {
    enum class Source {
        YOUTUBE,
        INSTAGRAM,
        TIKTOK
    }

    enum class Kind {
        VIDEO,
        PLAYLIST
    }

    val supportsAudio: Boolean
        get() = source == Source.YOUTUBE
}

object MediaUrlParser {
    fun parse(value: String): MediaUrl? {
        YouTubeUrlParser.parse(value)?.let { youtube ->
            return MediaUrl(
                originalUrl = youtube.originalUrl,
                source = MediaUrl.Source.YOUTUBE,
                kind = when (youtube.kind) {
                    YouTubeUrl.Kind.VIDEO -> MediaUrl.Kind.VIDEO
                    YouTubeUrl.Kind.PLAYLIST -> MediaUrl.Kind.PLAYLIST
                }
            )
        }

        val trimmed = value.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        val pathSegments = uri.rawPath.orEmpty().trim('/').split('/').filter { it.isNotBlank() }

        return when {
            host in INSTAGRAM_HOSTS && pathSegments.firstOrNull() in INSTAGRAM_VIDEO_PATHS -> {
                MediaUrl(trimmed, MediaUrl.Source.INSTAGRAM, MediaUrl.Kind.VIDEO)
            }
            host in TIKTOK_HOSTS && isTikTokVideoUrl(host, pathSegments) -> {
                MediaUrl(trimmed, MediaUrl.Source.TIKTOK, MediaUrl.Kind.VIDEO)
            }
            else -> null
        }
    }

    fun isInstagramOrTikTok(value: String): Boolean {
        val parsed = parse(value) ?: return false
        return parsed.source == MediaUrl.Source.INSTAGRAM || parsed.source == MediaUrl.Source.TIKTOK
    }

    private fun isTikTokVideoUrl(host: String, pathSegments: List<String>): Boolean =
        (host == "tiktok.com" && pathSegments.size >= 3 && pathSegments.first().startsWith("@") && pathSegments.getOrNull(1) == "video") ||
            host == "vm.tiktok.com" ||
            host == "vt.tiktok.com"

    private val INSTAGRAM_HOSTS = setOf("instagram.com", "m.instagram.com")
    private val INSTAGRAM_VIDEO_PATHS = setOf("reel", "reels", "p", "tv")
    private val TIKTOK_HOSTS = setOf("tiktok.com", "m.tiktok.com", "vm.tiktok.com", "vt.tiktok.com")
}
