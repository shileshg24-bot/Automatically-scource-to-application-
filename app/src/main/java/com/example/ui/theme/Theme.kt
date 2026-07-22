package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    secondary = CyberBlue,
    onSecondary = Color.Black,
    tertiary = NeonPurple,
    onTertiary = Color.White,
    background = DarkObsidian,
    onBackground = TextLight,
    surface = DarkVioletSurface,
    onSurface = TextLight,
    surfaceVariant = DarkVioletCard,
    onSurfaceVariant = TextMuted,
    outline = AccentBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
