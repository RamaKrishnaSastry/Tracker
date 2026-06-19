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

private val DarkColorScheme =
  darkColorScheme(
    primary = GeometricPrimary,
    secondary = GeometricSecondary,
    tertiary = GeometricTertiary,
    background = GeometricBgDark,
    surface = GeometricSurfaceDark,
    primaryContainer = GeometricPrimaryContainerDark,
    secondaryContainer = GeometricSecondaryContainerDark,
    surfaceVariant = GeometricSurfaceVariantDark,
    outline = GeometricOutlineDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = GeometricTextDark,
    onSurface = GeometricTextDark,
    onSurfaceVariant = GeometricTextDark.copy(alpha = 0.7f),
    outlineVariant = Color(0xFF49454F)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GeometricPrimary,
    secondary = GeometricSecondary,
    tertiary = GeometricTertiary,
    background = GeometricBgLight,
    surface = GeometricSurfaceLight,
    primaryContainer = GeometricPrimaryContainerLight,
    secondaryContainer = GeometricSecondaryContainerLight,
    surfaceVariant = GeometricSurfaceVariantLight,
    outline = GeometricOutlineLight,
    onPrimary = Color.White,
    onSecondary = Color(0xFF1D192B),
    onTertiary = Color.White,
    onBackground = GeometricTextLight,
    onSurface = GeometricTextLight,
    onSurfaceVariant = Color(0xFF49454F),
    outlineVariant = Color(0xFFEADDFF)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled to retain the exact Geometric Balance branding colorways
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
