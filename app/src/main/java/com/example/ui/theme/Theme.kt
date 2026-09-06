package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CleanMinimalPrimaryContainer,
    onPrimary = CleanMinimalOnPrimaryContainer,
    primaryContainer = CleanMinimalPrimary,
    onPrimaryContainer = CleanMinimalOnPrimary,
    secondary = CleanMinimalBorderLight,
    onSecondary = CleanMinimalTextPrimary,
    secondaryContainer = Color(0xFF2C2F36),
    onSecondaryContainer = CleanMinimalTerminalText,
    tertiary = AccentPurple,
    onTertiary = Color.White,
    error = CleanMinimalTerminalDot,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkSurfaceBorder,
    outlineVariant = Color(0xFF353945)
)

private val LightColorScheme = lightColorScheme(
    primary = CleanMinimalPrimary,
    onPrimary = CleanMinimalOnPrimary,
    primaryContainer = CleanMinimalPrimaryContainer,
    onPrimaryContainer = CleanMinimalOnPrimaryContainer,
    secondary = CleanMinimalTextSecondary,
    onSecondary = Color.White,
    secondaryContainer = CleanMinimalBorderLight,
    onSecondaryContainer = CleanMinimalTextSecondary,
    tertiary = AccentPurple,
    onTertiary = Color.White,
    error = CleanMinimalTerminalDot,
    background = CleanMinimalBackground,
    onBackground = CleanMinimalTextPrimary,
    surface = CleanMinimalSurface,
    onSurface = CleanMinimalTextPrimary,
    surfaceVariant = CleanMinimalSurfaceVariant,
    onSurfaceVariant = CleanMinimalTextSecondary,
    outline = CleanMinimalBorder,
    outlineVariant = CleanMinimalBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to Clean Minimalism Light as defined in design
    dynamicColor: Boolean = false, // Preserve bespoke Clean Minimalism brand palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
