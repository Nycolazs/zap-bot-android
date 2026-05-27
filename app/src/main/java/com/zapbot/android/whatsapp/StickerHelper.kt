package com.zapbot.android.whatsapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class StickerHelper(private val cacheDir: File) {
    suspend fun convertToWebp(source: File): File = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid sticker image" }

        var maxSide = STICKER_SIDE
        var output: ByteArray
        do {
            output = encodeScaled(source, bounds.outWidth, bounds.outHeight, maxSide)
            maxSide = (maxSide * 0.9f).toInt()
        } while (output.size > TARGET_BYTES && maxSide >= MIN_SIDE)

        val stickerDir = File(cacheDir, "stickers").apply { mkdirs() }
        cleanupOld(stickerDir)
        File(stickerDir, "sticker-${System.currentTimeMillis()}.webp").apply {
            writeBytes(output)
        }
    }

    fun deleteWhenSafe(file: File) {
        if (file.parentFile?.name == "stickers") {
            file.delete()
        }
    }

    private fun encodeScaled(source: File, width: Int, height: Int, maxSide: Int): ByteArray {
        val sample = calculateSampleSize(width, height, maxSide)
        val decoded = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: error("Invalid sticker image")

        val scale = minOf(maxSide.toFloat() / decoded.width, maxSide.toFloat() / decoded.height, 1f)
        val targetWidth = (decoded.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (decoded.height * scale).toInt().coerceAtLeast(1)
        val scaledContent = if (targetWidth == decoded.width && targetHeight == decoded.height) {
            decoded
        } else {
            Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true).also { decoded.recycle() }
        }
        val sticker = Bitmap.createBitmap(maxSide, maxSide, Bitmap.Config.ARGB_8888)
        Canvas(sticker).apply {
            drawColor(Color.TRANSPARENT)
            drawBitmap(
                scaledContent,
                ((maxSide - scaledContent.width) / 2f).coerceAtLeast(0f),
                ((maxSide - scaledContent.height) / 2f).coerceAtLeast(0f),
                null
            )
        }

        try {
            for (quality in listOf(92, 82, 72, 62, 52, 42, 34)) {
                ByteArrayOutputStream().use { out ->
                    @Suppress("DEPRECATION")
                    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        Bitmap.CompressFormat.WEBP
                    }
                    sticker.compress(format, quality, out)
                    val bytes = out.toByteArray()
                    if (bytes.size <= TARGET_BYTES || quality == 34) {
                        return bytes
                    }
                }
            }
            error("Sticker conversion failed")
        } finally {
            sticker.recycle()
            scaledContent.recycle()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (width / sample > maxSide * 2 || height / sample > maxSide * 2) {
            sample *= 2
        }
        return sample
    }

    private fun cleanupOld(dir: File) {
        val cutoff = System.currentTimeMillis() - TEMP_TTL_MILLIS
        dir.listFiles()?.forEach {
            if (it.lastModified() < cutoff) it.delete()
        }
    }

    private companion object {
        const val STICKER_SIDE = 512
        const val MIN_SIDE = 256
        const val TARGET_BYTES = 100 * 1024
        const val TEMP_TTL_MILLIS = 24L * 60L * 60L * 1000L
    }
}
