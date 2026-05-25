package com.zapbot.android.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.zapbot.android.database.ChatSearchSessionDao
import com.zapbot.android.database.ChatSearchSessionEntity
import com.zapbot.android.domain.YouTubeVideoResult

class SearchSessionRepository(
    private val dao: ChatSearchSessionDao,
    moshi: Moshi
) {
    private val listType = Types.newParameterizedType(List::class.java, YouTubeVideoResult::class.java)
    private val adapter = moshi.adapter<List<YouTubeVideoResult>>(listType)

    suspend fun save(chatId: String, query: String, results: List<YouTubeVideoResult>, messageId: String? = null) {
        val now = System.currentTimeMillis()
        val expiresAt = now + 30 * 60 * 1_000L
        val json = adapter.toJson(results)
        dao.upsert(ChatSearchSessionEntity(chatId = latestKey(chatId), query = query, resultsJson = json, createdAt = now, expiresAt = expiresAt))
        if (!messageId.isNullOrBlank()) {
            dao.upsert(ChatSearchSessionEntity(chatId = quotedKey(chatId, messageId), query = query, resultsJson = json, createdAt = now, expiresAt = expiresAt))
        }
    }

    suspend fun select(chatId: String, oneBasedIndex: Int, quotedMessageId: String? = null): SelectionResult {
        dao.deleteExpired(System.currentTimeMillis())
        val session = if (quotedMessageId.isNullOrBlank()) {
            dao.get(latestKey(chatId))
        } else {
            dao.get(quotedKey(chatId, quotedMessageId)) ?: return SelectionResult.Expired
        } ?: return SelectionResult.Expired
        if (session.expiresAt < System.currentTimeMillis()) return SelectionResult.Expired
        val results = adapter.fromJson(session.resultsJson).orEmpty()
        val video = results.getOrNull(oneBasedIndex - 1) ?: return SelectionResult.InvalidIndex(results.size)
        return SelectionResult.Selected(video)
    }

    suspend fun saveQuoted(chatId: String, quotedMessageId: String, query: String, results: List<YouTubeVideoResult>) {
        val now = System.currentTimeMillis()
        dao.upsert(
            ChatSearchSessionEntity(
                chatId = quotedKey(chatId, quotedMessageId),
                query = query,
                resultsJson = adapter.toJson(results),
                createdAt = now,
                expiresAt = now + 30 * 60 * 1_000L
            )
        )
    }

    private fun latestKey(chatId: String): String = chatId
    private fun quotedKey(chatId: String, messageId: String): String = "$chatId\n$messageId"
}

sealed interface SelectionResult {
    data class Selected(val video: YouTubeVideoResult) : SelectionResult
    data object Expired : SelectionResult
    data class InvalidIndex(val available: Int) : SelectionResult
}
