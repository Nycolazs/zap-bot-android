package com.zapbot.android.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zapbot.android.domain.DownloadStatus
import com.zapbot.android.domain.LogLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface BotSettingsDao {
    @Query("SELECT * FROM bot_settings WHERE id = 1")
    fun observe(): Flow<BotSettingsEntity?>

    @Query("SELECT * FROM bot_settings WHERE id = 1")
    suspend fun get(): BotSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: BotSettingsEntity)
}

@Dao
interface ChatSearchSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ChatSearchSessionEntity)

    @Query("SELECT * FROM chat_search_sessions WHERE chatId = :chatId")
    suspend fun get(chatId: String): ChatSearchSessionEntity?

    @Query("DELETE FROM chat_search_sessions WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long)
}

@Dao
interface ChatWelcomeDao {
    @Query("SELECT EXISTS(SELECT 1 FROM chat_welcomes WHERE chatId = :chatId)")
    suspend fun hasWelcomed(chatId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(welcome: ChatWelcomeEntity): Long
}

@Dao
interface DownloadJobDao {
    @Insert
    suspend fun insert(job: DownloadJobEntity): Long

    @Update
    suspend fun update(job: DownloadJobEntity)

    @Query("SELECT * FROM download_jobs WHERE id = :id")
    suspend fun get(id: Long): DownloadJobEntity?

    @Query("SELECT * FROM download_jobs WHERE chatId = :chatId AND status IN ('QUEUED','DOWNLOADING','PROCESSING','SENDING') ORDER BY createdAt DESC LIMIT 1")
    suspend fun activeForChat(chatId: String): DownloadJobEntity?

    @Query("SELECT * FROM download_jobs WHERE status IN ('QUEUED','DOWNLOADING','PROCESSING','SENDING') ORDER BY createdAt")
    suspend fun unfinished(): List<DownloadJobEntity>

    @Query("SELECT * FROM download_jobs ORDER BY createdAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<DownloadJobEntity>>

    @Query("SELECT COUNT(*) FROM download_jobs WHERE status IN ('QUEUED','DOWNLOADING','PROCESSING','SENDING')")
    fun observeActiveCount(): Flow<Int>

    @Query("UPDATE download_jobs SET status = :status, errorMessage = :error, updatedAt = :now, completedAt = :now WHERE id = :id")
    suspend fun finish(id: Long, status: DownloadStatus, error: String?, now: Long = System.currentTimeMillis())
}

@Dao
interface BotLogDao {
    @Insert
    suspend fun insert(log: BotLogEntity)

    @Query("SELECT * FROM bot_logs ORDER BY createdAt DESC LIMIT 300")
    fun observeRecent(): Flow<List<BotLogEntity>>

    @Query("SELECT * FROM bot_logs WHERE level = :level ORDER BY createdAt DESC LIMIT 300")
    fun observeByLevel(level: LogLevel): Flow<List<BotLogEntity>>

    @Query("DELETE FROM bot_logs")
    suspend fun clear()
}
