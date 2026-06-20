package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// Geometric Balance Palette (M3 Lavender-Violet UI design)
val GeometricPrimary = Color(0xFF6449A7)
val GeometricSecondary = Color(0xFF625B71)
val GeometricTertiary = Color(0xFF7D5260)

val GeometricBgLight = Color(0xFFFEF7FF)
val GeometricBgDark = Color(0xFF141218)

val GeometricTextLight = Color(0xFF1D1B20)
val GeometricTextDark = Color(0xFFE6E1E5)

val GeometricSurfaceLight = Color(0xFFFFFFFF)
val GeometricSurfaceDark = Color(0xFF1D1B20)

val GeometricPrimaryContainerLight = Color(0xFFEADDFF)
val GeometricPrimaryContainerDark = Color(0xFF4F378B)

val GeometricSecondaryContainerLight = Color(0xFFE8DEF8)
val GeometricSecondaryContainerDark = Color(0xFF4A4458)

val GeometricOutlineLight = Color(0xFFCAC4D0)
val GeometricOutlineDark = Color(0xFF938F99)

val GeometricSurfaceVariantLight = Color(0xFFF3EDF7)
val GeometricSurfaceVariantDark = Color(0xFF49454F)

fun getMantraColor(colorName: String): Color {
    if (colorName.startsWith("#")) {
        return try {
            Color(android.graphics.Color.parseColor(colorName))
        } catch (e: Exception) {
            Color(0xFF6449A7) // Fallback to Royal
        }
    }
    return when (colorName.lowercase()) {
        "indigo" -> Color(0xFF6750A4)
        "teal" -> Color(0xFF009688)
        "saffron" -> Color(0xFFFF9800)
        "crimson" -> Color(0xFFDC143C)
        "emerald" -> Color(0xFF2E7D32)
        "amber" -> Color(0xFFFFBF00)
        "slate" -> Color(0xFF708090)
        "violet" -> Color(0xFF9C27B0)
        "royal" -> Color(0xFF6449A7)
        else -> Color(0xFF6449A7) // Default to Royal
    }
}

fun Color.toHexString(): String {
    return String.format("#%06X", 0xFFFFFF and this.toArgb())
}
