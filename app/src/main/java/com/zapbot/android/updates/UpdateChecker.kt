package com.zapbot.android.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.zapbot.android.domain.LanguageResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.Properties

class UpdateChecker(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun check(language: String, currentVersion: String): UpdateCheckResult {
        val lang = LanguageResolver.resolve(language)
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                withContext(Dispatchers.Main) { openInstallPermissionSettings() }
                return UpdateCheckResult.Message(message(lang, "permission"))
            }

            val release = withContext(Dispatchers.IO) { latestRelease() }
                ?: return UpdateCheckResult.Message(message(lang, "unavailable"))
            if (compareVersions(release.version, currentVersion) <= 0) {
                return UpdateCheckResult.Message(message(lang, "up_to_date", release.version))
            }

            val cachedApk = withContext(Dispatchers.IO) { UpdateApkCache.cachedApk(context.cacheDir, release) }
            val apk = cachedApk ?: withContext(Dispatchers.IO) { downloadApk(release) }
            withContext(Dispatchers.Main) { openInstaller(apk) }
            val messageKey = if (cachedApk != null) "already_downloaded" else "ready"
            UpdateCheckResult.Message(message(lang, messageKey, release.version))
        }.getOrElse {
            UpdateCheckResult.Message(message(lang, "error", it.message ?: it.javaClass.simpleName))
        }
    }

    private fun latestRelease(): ReleaseInfo? {
        val request = Request.Builder()
            .url(GITHUB_LATEST_RELEASE)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Zappy-Android")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("GitHub returned HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val tag = json.getString("tag_name")
            val assets = json.getJSONArray("assets")
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                val name = asset.getString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    return ReleaseInfo(
                        version = tag.removePrefix("v"),
                        tag = tag,
                        apkName = name,
                        apkUrl = asset.getString("browser_download_url")
                    )
                }
            }
        }
        return null
    }

    private fun downloadApk(release: ReleaseInfo): File {
        val outputDir = UpdateApkCache.outputDir(context.cacheDir).apply { mkdirs() }
        val output = File(outputDir, release.apkName)
        val tempOutput = File(outputDir, "${release.apkName}.download")
        tempOutput.delete()
        val request = Request.Builder()
            .url(release.apkUrl)
            .header("User-Agent", "Zappy-Android")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Download returned HTTP ${response.code}")
            val body = response.body ?: error("Download body was empty")
            tempOutput.outputStream().use { file ->
                body.byteStream().use { input -> input.copyTo(file) }
            }
        }
        if (tempOutput.length() <= 0L) error("Downloaded APK is empty")
        outputDir.listFiles()?.forEach { file ->
            if (file != tempOutput) file.deleteRecursively()
        }
        if (!tempOutput.renameTo(output)) {
            tempOutput.copyTo(output, overwrite = true)
            tempOutput.delete()
        }
        if (output.length() <= 0L) error("Downloaded APK is empty")
        UpdateApkCache.writeMetadata(context.cacheDir, release)
        return output
    }

    private fun openInstaller(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    private fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun compareVersions(latest: String, current: String): Int {
        val left = latest.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val right = current.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(left.size, right.size)
        for (index in 0 until size) {
            val diff = (left.getOrNull(index) ?: 0) - (right.getOrNull(index) ?: 0)
            if (diff != 0) return diff
        }
        return 0
    }

    private fun message(language: String, key: String, value: String = ""): String = when (language) {
        "pt" -> when (key) {
            "permission" -> "Permita que o Zappy instale atualizações e toque em Verificar atualizações novamente."
            "unavailable" -> "Não encontrei um APK disponível na última release do GitHub."
            "up_to_date" -> "Você já está na versão mais recente ($value)."
            "already_downloaded" -> "Atualização $value já baixada. Confirme a instalação no Android."
            "ready" -> "Atualização $value pronta. Confirme a instalação na tela do Android."
            else -> "Não consegui verificar atualizações: $value"
        }
        "es" -> when (key) {
            "permission" -> "Permite que Zappy instale actualizaciones y toca Buscar actualizaciones de nuevo."
            "unavailable" -> "No encontré un APK disponible en la última release de GitHub."
            "up_to_date" -> "Ya tienes la versión más reciente ($value)."
            "already_downloaded" -> "Actualización $value ya descargada. Confirma la instalación en Android."
            "ready" -> "Actualización $value lista. Confirma la instalación en Android."
            else -> "No pude buscar actualizaciones: $value"
        }
        "ru" -> when (key) {
            "permission" -> "Разрешите Zappy устанавливать обновления и нажмите Проверить обновления снова."
            "unavailable" -> "APK не найден в последнем релизе GitHub."
            "up_to_date" -> "У вас уже последняя версия ($value)."
            "already_downloaded" -> "Обновление $value уже загружено. Подтвердите установку в Android."
            "ready" -> "Обновление $value готово. Подтвердите установку в Android."
            else -> "Не удалось проверить обновления: $value"
        }
        else -> when (key) {
            "permission" -> "Allow Zappy to install updates, then tap Check for updates again."
            "unavailable" -> "I could not find an APK in the latest GitHub release."
            "up_to_date" -> "You are already on the latest version ($value)."
            "already_downloaded" -> "Update $value already downloaded. Confirm installation in Android."
            "ready" -> "Update $value ready. Confirm installation in Android."
            else -> "Could not check for updates: $value"
        }
    }

    internal data class ReleaseInfo(
        val version: String,
        val tag: String,
        val apkName: String,
        val apkUrl: String
    )

    private companion object {
        const val GITHUB_LATEST_RELEASE = "https://api.github.com/repos/Nycolazs/zap-bot-android/releases/latest"
    }
}

internal object UpdateApkCache {
    private const val METADATA_FILE = "latest-release.properties"

    fun outputDir(cacheDir: File): File = File(cacheDir, "updates")

    fun cachedApk(cacheDir: File, release: UpdateChecker.ReleaseInfo): File? {
        val outputDir = outputDir(cacheDir)
        val apk = File(outputDir, release.apkName)
        if (!apk.isFile || apk.length() <= 0L) return null

        val metadata = File(outputDir, METADATA_FILE)
        if (!metadata.isFile) return null

        return runCatching {
            val properties = Properties()
            metadata.inputStream().use { properties.load(it) }
            val matchesRelease = properties.getProperty("tag") == release.tag &&
                properties.getProperty("version") == release.version &&
                properties.getProperty("apkName") == release.apkName &&
                properties.getProperty("apkUrl") == release.apkUrl
            if (matchesRelease) apk else null
        }.getOrNull()
    }

    fun writeMetadata(cacheDir: File, release: UpdateChecker.ReleaseInfo) {
        val outputDir = outputDir(cacheDir).apply { mkdirs() }
        val properties = Properties().apply {
            setProperty("tag", release.tag)
            setProperty("version", release.version)
            setProperty("apkName", release.apkName)
            setProperty("apkUrl", release.apkUrl)
        }
        File(outputDir, METADATA_FILE).outputStream().use { output ->
            properties.store(output, "Zappy update cache")
        }
    }
}

sealed interface UpdateCheckResult {
    data class Message(val message: String) : UpdateCheckResult
}
