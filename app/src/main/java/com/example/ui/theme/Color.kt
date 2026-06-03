package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

// Day Mode (Light Theme) Palette
val LightBackground = Color(0xFFF5F5F7)
val LightCard = Color(0xFFFFFFFF)
val LightPrimaryText = Color(0xFF222222)
val LightSecondaryText = Color(0xFF666666)
val LightBorder = Color(0xFFE5E5E5)
val LightIncome = Color(0xFF16A34A)
val LightExpense = Color(0xFFDC2626)
val LightButton = Color(0xFF6366F1)

// Night Mode (Dark Theme) Palette
val DarkBackground = Color(0xFF12001F)
val DarkCard = Color(0xFF2A1242)
val DarkPrimaryText = Color(0xFFF5EFFF)
val DarkSecondaryText = Color(0xFFC8B6E2)
val DarkAccent = Color(0xFFB57CFF) // Lavender
val DarkIncome = Color(0xFF4ADE80)
val DarkExpense = Color(0xFFF87171)

// Semantic Indicator and Style Extensions for MaterialTheme.colorScheme
val ColorScheme.income: Color
    get() = tertiary

val ColorScheme.expense: Color
    get() = error

val ColorScheme.border: Color
    get() = outline

val ColorScheme.buttonGradient: List<Color>
    get() = if (this.primary == LightButton) {
        // Solid/soft professional gradient for Day Mode
        listOf(LightButton, LightButton)
    } else {
        // Premium purple to pink/lavender gradient with highlights for Night Mode
        listOf(Color(0xFF8B5CF6), Color(0xFFB57CFF), Color(0xFFD946EF))
    }

val ColorScheme.isDark: Boolean
    get() = this.background == DarkBackground

