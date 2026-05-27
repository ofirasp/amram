package com.binadev.times.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val Gold = Color(0xFFD4AF37)
val GoldLight = Color(0xFFF5D769)
val NavyDark = Color(0xFF0A0E1A)
val NavyPanel = Color(0xFF0F1629)
val NavySidebar = Color(0xFF0B1220)
val NavyDivider = Color(0xFF1E2D4A)
val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFFB0B8C8)
val TextMuted = Color(0xFF7A8499)

private val ColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = NavyDark,
    background = NavyDark,
    onBackground = TextPrimary,
    surface = NavyPanel,
    onSurface = TextPrimary,
    secondary = GoldLight,
    onSecondary = NavyDark,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TimesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content
    )
}
