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
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StickerHelper(private val cacheDir: File) {
    suspend fun convertToWebp(source: File): File = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid sticker image" }

        var maxSide = STICKER_SIDE
        var output: ByteArray
        do {
            output = addStickerMetadata(encodeScaled(source, bounds.outWidth, bounds.outHeight, maxSide))
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
                    if (bytes.size <= TARGET_IMAGE_BYTES || quality == 34) {
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

    private fun addStickerMetadata(webp: ByteArray): ByteArray {
        if (webp.size < RIFF_HEADER_SIZE || !webp.startsWith("RIFF", 0) || !webp.startsWith("WEBP", 8)) {
            return webp
        }
        val exif = stickerExif()
        val out = ByteArrayOutputStream(webp.size + exif.size + CHUNK_HEADER_SIZE + 1)
        out.write("RIFF".toByteArray(Charsets.US_ASCII))
        out.write(intLe(webp.size - RIFF_HEADER_SIZE + CHUNK_HEADER_SIZE + exif.size + exif.size.padding()))
        out.write("WEBP".toByteArray(Charsets.US_ASCII))

        var offset = RIFF_HEADER_SIZE
        while (offset + CHUNK_HEADER_SIZE <= webp.size) {
            val chunkId = String(webp, offset, 4, Charsets.US_ASCII)
            val chunkSize = intLe(webp, offset + 4)
            val paddedSize = chunkSize + chunkSize.padding()
            val next = offset + CHUNK_HEADER_SIZE + paddedSize
            if (next > webp.size) break
            if (chunkId != "EXIF") {
                out.write(webp, offset, CHUNK_HEADER_SIZE + paddedSize)
            }
            offset = next
        }

        out.write("EXIF".toByteArray(Charsets.US_ASCII))
        out.write(intLe(exif.size))
        out.write(exif)
        if (exif.size.padding() == 1) out.write(0)
        return out.toByteArray()
    }

    private fun stickerExif(): ByteArray {
        val metadata = """
            {"sticker-pack-id":"com.zapbot.android","sticker-pack-name":"$STICKER_PACK_NAME","sticker-pack-publisher":"@nycolazs","emojis":["🤖"]}
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val header = byteArrayOf(
            0x49, 0x49, 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x41, 0x57, 0x07, 0x00
        ) + intLe(metadata.size) + intLe(0x16)
        return header + metadata
    }

    private fun ByteArray.startsWith(value: String, offset: Int): Boolean {
        val bytes = value.toByteArray(Charsets.US_ASCII)
        if (offset + bytes.size > size) return false
        return bytes.indices.all { this[offset + it] == bytes[it] }
    }

    private fun Int.padding(): Int = this and 1

    private fun intLe(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun intLe(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private companion object {
        const val STICKER_SIDE = 512
        const val MIN_SIDE = 256
        const val TARGET_BYTES = 100 * 1024
        const val TARGET_IMAGE_BYTES = 94 * 1024
        const val TEMP_TTL_MILLIS = 24L * 60L * 60L * 1000L
        const val RIFF_HEADER_SIZE = 12
        const val CHUNK_HEADER_SIZE = 8
        const val STICKER_PACK_NAME = "Sticker created with Zappy BOT 🤖 by @nycolazs"
    }
}
