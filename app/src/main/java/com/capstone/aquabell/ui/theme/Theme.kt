package com.capstone.aquabell.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = AquaPrimaryDark,
    onPrimary = AquaOnPrimaryDark,
    primaryContainer = AquaPrimaryContainerDark,
    onPrimaryContainer = AquaOnPrimaryContainerDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    outline = NeutralOutlineDark,
    secondary = AquaPrimaryDark,
    tertiary = AccentSuccessDark,
)

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = AquaPrimary,
    onPrimary = AquaOnPrimary,
    primaryContainer = AquaPrimaryContainer,
    onPrimaryContainer = AquaOnPrimaryContainer,
    surface = NeutralSurface,
    onSurface = NeutralOnSurface,
    outline = NeutralOutline,
    secondary = AquaPrimary,
    tertiary = AccentSuccess,
)

@Composable
fun AquabellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}