package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GeometricLightColorScheme = lightColorScheme(
    primary = GeoPrimary,
    onPrimary = Color.White,
    primaryContainer = GeoPrimaryContainer,
    onPrimaryContainer = GeoOnPrimaryContainer,
    secondary = GeoSecondary,
    background = GeoBackground,
    surface = Color.White,
    onBackground = GeoTextPrimary,
    onSurface = GeoTextPrimary,
    outline = GeoBorder
)

// Cohesive premium Dark theme using deep purple/slate to align with the Geo theme
private val GeometricDarkColorScheme = darkColorScheme(
    primary = GeoFabBg,
    onPrimary = GeoOnPrimaryContainer,
    primaryContainer = Color(0xFF311062),
    onPrimaryContainer = GeoPrimaryContainer,
    secondary = Color(0xFFCCC2DC),
    background = Color(0xFF141218), // M3 dark mode theme background
    surface = Color(0xFF1D1B20), // M3 dark mode surface
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    outline = Color(0xFF49454F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // We can support darkTheme, but standard lightTheme of Geometric Balance is incredibly beautiful
    // Let's use the Geometric light theme by default, or dark if the system is explicitly dark
    val colorScheme = if (darkTheme) GeometricDarkColorScheme else GeometricLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
