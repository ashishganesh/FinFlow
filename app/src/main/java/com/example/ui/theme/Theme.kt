package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = Color(0xFF12001F),
    primaryContainer = Color(0xFF3E1B5E),
    onPrimaryContainer = Color.White,
    secondary = DarkAccent,
    onSecondary = Color.White,
    tertiary = DarkIncome,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkCard,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkSecondaryText,
    error = DarkExpense,
    onError = Color.White,
    outline = Color(0xFF4C2770),
    outlineVariant = Color(0xFF2A1242)
)

private val LightColorScheme = lightColorScheme(
    primary = LightButton,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E7EB),
    onPrimaryContainer = LightPrimaryText,
    secondary = LightButton,
    onSecondary = Color.White,
    tertiary = LightIncome,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightCard,
    onSurface = LightPrimaryText,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightSecondaryText,
    error = LightExpense,
    onError = Color.White,
    outline = LightBorder,
    outlineVariant = Color(0xFFE5E5E5)
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
