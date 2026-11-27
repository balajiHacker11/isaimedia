package com.isai.app.core

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.isai.app.data.model.AppSettings
import com.isai.app.data.model.ThemeMode

val AccentColors = listOf(
    Color(0xFF9D8CFF), Color(0xFFFF7A6B), Color(0xFF3DDC97), Color(0xFFFFC145), Color(0xFF4CC2FF),
)
val AccentNames = listOf("Violet", "Coral", "Mint", "Amber", "Sky")

@Composable
fun IsaiTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val accent = AccentColors[settings.accentIndex.coerceIn(0, AccentColors.lastIndex)]
    val scheme = if (dark) darkColorScheme(
        primary = accent, onPrimary = Color(0xFF0B0B10),
        background = Color(0xFF0B0B10), onBackground = Color(0xFFF2F1F7),
        surface = Color(0xFF14141C), onSurface = Color(0xFFF2F1F7),
        surfaceVariant = Color(0xFF22222D), onSurfaceVariant = Color(0xFFB4B2C2),
        surfaceContainerHigh = Color(0xFF1B1B25),
        secondaryContainer = accent.copy(alpha = 0.22f), onSecondaryContainer = Color.White,
    ) else lightColorScheme(
        primary = accent.copy(alpha = 1f), onPrimary = Color.White,
        background = Color(0xFFF7F6FB), onBackground = Color(0xFF14141C),
        surface = Color.White, onSurface = Color(0xFF14141C),
        surfaceVariant = Color(0xFFE9E7F2), onSurfaceVariant = Color(0xFF55536A),
        surfaceContainerHigh = Color(0xFFEFEDF7),
        secondaryContainer = accent.copy(alpha = 0.25f), onSecondaryContainer = Color(0xFF14141C),
    )
    CompositionLocalProvider(LocalSettings provides settings) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
