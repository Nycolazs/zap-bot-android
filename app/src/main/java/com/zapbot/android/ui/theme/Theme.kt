package com.zapbot.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF126C5E),
    secondary = Color(0xFF4C5D77),
    tertiary = Color(0xFF7A5C8C),
    surface = Color(0xFFFAFBF8),
    surfaceVariant = Color(0xFFE7EFEA),
    error = Color(0xFFB3261E)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7ADBCB),
    secondary = Color(0xFFB8C7E2),
    tertiary = Color(0xFFD4B3E7),
    surface = Color(0xFF111413),
    surfaceVariant = Color(0xFF26302D)
)

@Composable
fun ZapBotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
