package com.zapbot.android.youtube

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.zapbot.android.domain.YouTubeVideoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class YouTubeWebSearchClient(moshi: Moshi) : YouTubeSearchClient {
    private val client = OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.SECONDS)
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    override suspend fun searchVideos(query: String, maxResults: Int): List<YouTubeVideoResult> = withContext(Dispatchers.IO) {
        val url = "https://www.youtube.com/results".toHttpUrl().newBuilder()
            .addQueryParameter("search_query", query)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DESKTOP_USER_AGENT)
            .build()
        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("YouTube respondeu HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
        val initialData = extractInitialData(html) ?: error("Não consegui ler os resultados do YouTube")
        val root = mapAdapter.fromJson(initialData) ?: error("Resposta do YouTube inválida")
        val seen = linkedSetOf<String>()
        collectVideoRenderers(root)
            .mapNotNull { it.toVideoResult() }
            .filter { it.durationSeconds > 0L }
            .filter { it.durationSeconds <= MAX_SEARCH_DURATION_SECONDS }
            .filter { seen.add(it.videoId) }
            .take(maxResults.coerceIn(1, 10))
            .toList()
    }

    private fun collectVideoRenderers(value: Any?): Sequence<Map<String, Any?>> = sequence {
        when (value) {
            is Map<*, *> -> {
                val renderer = value["videoRenderer"]
                if (renderer is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    yield(renderer as Map<String, Any?>)
                }
                value.values.forEach { yieldAll(collectVideoRenderers(it)) }
            }
            is List<*> -> value.forEach { yieldAll(collectVideoRenderers(it)) }
        }
    }

    private fun Map<String, Any?>.toVideoResult(): YouTubeVideoResult? {
        val videoId = stringValue("videoId") ?: return null
        val title = textValue(this["title"]) ?: return null
        val duration = parseDuration(textValue(this["lengthText"]) ?: return null)
        val channel = textValue(this["ownerText"])
            ?: textValue(this["longBylineText"])
            ?: textValue(this["shortBylineText"])
            ?: ""
        val thumbnail = thumbnails(this["thumbnail"]).firstOrNull()
        return YouTubeVideoResult(
            title = cleanText(title),
            channel = cleanText(channel),
            videoId = videoId,
            url = "https://www.youtube.com/watch?v=$videoId",
            durationSeconds = duration,
            thumbnailUrl = thumbnail
        )
    }

    private fun extractInitialData(html: String): String? {
        val marker = "ytInitialData"
        val markerIndex = html.indexOf(marker)
        if (markerIndex < 0) return null
        val start = html.indexOf('{', markerIndex)
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until html.length) {
            val char = html[index]
            when {
                escaped -> escaped = false
                inString && char == '\\' -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> depth++
                !inString && char == '}' -> {
                    depth--
                    if (depth == 0) return html.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private fun textValue(value: Any?): String? = when (value) {
        is String -> value
        is Map<*, *> -> {
            (value["simpleText"] as? String)
                ?: (value["runs"] as? List<*>)?.joinToString("") { run ->
                    ((run as? Map<*, *>)?.get("text") as? String).orEmpty()
                }?.takeIf { it.isNotBlank() }
        }
        else -> null
    }

    private fun Map<String, Any?>.stringValue(key: String): String? =
        (this[key] as? String)?.takeIf { it.isNotBlank() }

    private fun thumbnails(value: Any?): List<String> {
        val raw = (value as? Map<*, *>)?.get("thumbnails") as? List<*> ?: return emptyList()
        return raw.mapNotNull { (it as? Map<*, *>)?.get("url") as? String }
    }

    private fun parseDuration(value: String): Long {
        val parts = value.split(":").mapNotNull { it.trim().toLongOrNull() }
        if (parts.isEmpty()) return 0L
        return parts.fold(0L) { total, part -> total * 60 + part }
    }

    private fun cleanText(value: String): String =
        value.replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .trim()

    private companion object {
        const val MAX_SEARCH_DURATION_SECONDS = 70L * 60L
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Safari/537.36"
    }
}
