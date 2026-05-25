package com.zapbot.android

import com.zapbot.android.downloader.safeFilename
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeFilenameTest {
    @Test fun removesPathTraversalCharacters() {
        val name = safeFilename("../../video:name?")
        assertFalse(name.contains(".."))
        assertFalse(name.contains("/"))
        assertTrue(name.isNotBlank())
    }
}
