package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FormulaColorScheme = darkColorScheme(
    primary = ApexRed,
    onPrimary = TextPrimary,
    primaryContainer = ApexRedDark,
    onPrimaryContainer = TextPrimary,
    secondary = ApexCyan,
    onSecondary = CarbonBlack,
    secondaryContainer = CarbonBorder,
    onSecondaryContainer = ApexCyan,
    tertiary = ApexGold,
    onTertiary = CarbonBlack,
    background = CarbonBlack,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = CarbonBorder,
    onSurfaceVariant = TextSecondary,
    outline = CarbonBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent high-performance motorsport identity
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FormulaColorScheme,
        typography = Typography,
        content = content
    )
}
