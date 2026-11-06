package com.senku.headshotai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SenkuGamingColorScheme = darkColorScheme(
    primary = SenkuCyan,
    onPrimary = Color(0xFF001F24),
    primaryContainer = SenkuElectricBlue,
    onPrimaryContainer = SenkuTextPrimary,
    secondary = SenkuElectricBlue,
    onSecondary = SenkuTextPrimary,
    tertiary = SenkuHeadshotRed,
    onTertiary = SenkuTextPrimary,
    background = SenkuNearBlack,
    onBackground = SenkuTextPrimary,
    surface = SenkuDeepSurface,
    onSurface = SenkuTextPrimary,
    surfaceVariant = SenkuCharcoalCard,
    onSurfaceVariant = SenkuTextSecondary,
    error = SenkuHeadshotRed,
    outline = SenkuCardBorder
)

@Composable
fun SenkuHeadshotAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SenkuGamingColorScheme,
        typography = SenkuTypography,
        content = content
    )
}
