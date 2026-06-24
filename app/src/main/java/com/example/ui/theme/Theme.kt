package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NutriGreen,
    onPrimary = Color(0xFF08100C),
    primaryContainer = ForestGreenDeep,
    onPrimaryContainer = Color.White,
    secondary = ForestGreenSolid,
    onSecondary = Color.White,
    background = CosmicDarkBg,
    onBackground = TextPrimaryDark,
    surface = CosmicDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CosmicDarkSelected,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkGrayBorder,
    error = ActiveRed
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenDeep,
    onPrimary = Color.White,
    primaryContainer = NutriGreenLight,
    onPrimaryContainer = ForestGreenDeep,
    secondary = ForestGreenSolid,
    onSecondary = Color.White,
    secondaryContainer = NutriGreenAccent,
    onSecondaryContainer = ForestGreenDeep,
    background = PureLightBg,
    onBackground = TextPrimaryLight,
    surface = PureLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE9F5EB),
    onSurfaceVariant = TextSecondaryLight,
    outline = LightGrayBorder,
    error = ActiveRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force custom brand-identity design over default device colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
