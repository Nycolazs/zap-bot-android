package com.zapbot.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF006B5D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F2E5),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF365D7D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF001D34),
    tertiary = Color(0xFF765A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA3),
    onTertiaryContainer = Color(0xFF251A00),
    background = Color(0xFFF7FAF7),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDCEBE6),
    onSurfaceVariant = Color(0xFF3F4946),
    surfaceTint = Color(0xFF006B5D),
    inverseSurface = Color(0xFF2D3130),
    inverseOnSurface = Color(0xFFEFF1EE),
    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFC0C9C6),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FD8C8),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005045),
    onPrimaryContainer = Color(0xFF9CF4E3),
    secondary = Color(0xFFA3C9EB),
    onSecondary = Color(0xFF00344F),
    secondaryContainer = Color(0xFF164A67),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = Color(0xFFEAC46C),
    onTertiary = Color(0xFF3E2E00),
    tertiaryContainer = Color(0xFF594400),
    onTertiaryContainer = Color(0xFFFFDEA3),
    background = Color(0xFF101413),
    onBackground = Color(0xFFE0E3E0),
    surface = Color(0xFF171B1A),
    onSurface = Color(0xFFE0E3E0),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFC0C9C6),
    surfaceTint = Color(0xFF7FD8C8),
    inverseSurface = Color(0xFFE0E3E0),
    inverseOnSurface = Color(0xFF2D3130),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4946),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun ZapBotTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}
