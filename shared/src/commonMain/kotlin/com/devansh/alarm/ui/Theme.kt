package com.devansh.alarm.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Background = Color(0xFF1A1B2E)
    val Surface = Color(0xFF242640)
    val Primary = Color(0xFF7C83FF)
    val OnPrimary = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFFF2F3FF)
    val TextSecondary = Color(0xFF8A8FB5)
    val Danger = Color(0xFFFF6B6B)
}

private val DarkColors = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.OnPrimary,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.Surface,
    onSurfaceVariant = AppColors.TextSecondary,
    error = AppColors.Danger,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
