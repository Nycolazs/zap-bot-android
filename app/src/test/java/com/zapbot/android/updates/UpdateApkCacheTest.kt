package com.zapbot.android.updates

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateApkCacheTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun returnsApkWhenFileIsPresentAndReleaseMetadataMatches() {
        val cacheDir = temp.newFolder()
        val release = release()
        val apk = File(UpdateApkCache.outputDir(cacheDir).apply { mkdirs() }, release.apkName)
        apk.writeBytes(byteArrayOf(1, 2, 3))
        UpdateApkCache.writeMetadata(cacheDir, release)

        assertEquals(apk, UpdateApkCache.cachedApk(cacheDir, release))
    }

    @Test fun ignoresMissingOrEmptyApk() {
        val cacheDir = temp.newFolder()
        val release = release()
        UpdateApkCache.outputDir(cacheDir).mkdirs()
        UpdateApkCache.writeMetadata(cacheDir, release)

        assertNull(UpdateApkCache.cachedApk(cacheDir, release))

        File(UpdateApkCache.outputDir(cacheDir), release.apkName).writeBytes(ByteArray(0))

        assertNull(UpdateApkCache.cachedApk(cacheDir, release))
    }

    @Test fun ignoresApkWhenReleaseChanges() {
        val cacheDir = temp.newFolder()
        val release = release()
        val apk = File(UpdateApkCache.outputDir(cacheDir).apply { mkdirs() }, release.apkName)
        apk.writeBytes(byteArrayOf(1, 2, 3))
        UpdateApkCache.writeMetadata(cacheDir, release)

        assertNull(UpdateApkCache.cachedApk(cacheDir, release(tag = "v1.4.4", version = "1.4.4")))
        assertNull(UpdateApkCache.cachedApk(cacheDir, release(apkName = "zapbot-1.4.4.apk")))
        assertNull(UpdateApkCache.cachedApk(cacheDir, release(apkUrl = "https://example.com/zapbot-1.4.4.apk")))
    }

    @Test fun ignoresApkWithoutReleaseMetadata() {
        val cacheDir = temp.newFolder()
        val release = release()
        val apk = File(UpdateApkCache.outputDir(cacheDir).apply { mkdirs() }, release.apkName)
        apk.writeBytes(byteArrayOf(1, 2, 3))

        assertNull(UpdateApkCache.cachedApk(cacheDir, release))
    }

    private fun release(
        tag: String = "v1.4.3",
        version: String = "1.4.3",
        apkName: String = "zapbot-1.4.3.apk",
        apkUrl: String = "https://example.com/zapbot-1.4.3.apk"
    ) = UpdateChecker.ReleaseInfo(
        version = version,
        tag = tag,
        apkName = apkName,
        apkUrl = apkUrl
    )
}
