package com.zapbot.android.downloader

fun safeFilename(value: String): String =
    value
        .replace(Regex("[^A-Za-z0-9._ -]"), "_")
        .replace("..", "_")
        .trim()
        .ifBlank { "media" }
        .take(80)
