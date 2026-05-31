package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color(0xFF0F0022),
    primaryContainer = BrightViolet,
    onPrimaryContainer = Color.White,
    secondary = DeepIndigo,
    onSecondary = Color.White,
    tertiary = AccentPink,
    onTertiary = Color.White,
    background = DeepDarkPurple,
    onBackground = Color(0xFFFAF5FF),
    surface = DarkSurface,
    onSurface = Color(0xFFFAF5FF),
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = Color(0xFFE8DDF4),
    error = NeonRed,
    onError = Color.Black,
    outline = Color(0xFF4C2770),
    outlineVariant = Color(0xFF2C104A)
)

private val LightColorScheme = lightColorScheme(
    primary = BrightViolet,
    onPrimary = Color.White,
    primaryContainer = NeonPurple,
    onPrimaryContainer = Color(0xFF1F053D),
    secondary = DeepIndigo,
    onSecondary = Color.White,
    tertiary = AccentPink,
    onTertiary = Color.White,
    background = Color(0xFFFAF5FF),
    onBackground = Color(0xFF1F053D),
    surface = Color.White,
    onSurface = Color(0xFF1F053D),
    surfaceVariant = Color(0xFFF3EAFD),
    onSurfaceVariant = Color(0xFF42354F),
    error = Color(0xFFDC2626),
    onError = Color.White,
    outline = Color(0xFFDED0EB),
    outlineVariant = Color(0xFFBDACD3)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We favor our custom high-fidelity purple color schemes instead of dynamic system colors
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
