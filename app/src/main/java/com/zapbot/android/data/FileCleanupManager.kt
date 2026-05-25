package com.zapbot.android.data

import java.io.File

class FileCleanupManager(private val cacheDir: File) {
    fun jobDir(jobId: Long): File = File(cacheDir, "bot_jobs/$jobId")

    fun cleanJob(jobId: Long) {
        jobDir(jobId).deleteRecursively()
    }

    fun cleanOldTempFiles(maxAgeMs: Long = 24 * 60 * 60 * 1_000L) {
        val root = File(cacheDir, "bot_jobs")
        val cutoff = System.currentTimeMillis() - maxAgeMs
        root.listFiles()?.forEach { if (it.lastModified() < cutoff) it.deleteRecursively() }
    }
}
