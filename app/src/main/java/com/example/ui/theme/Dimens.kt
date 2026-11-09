package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared dimensions, spacing scale, corner radii, and layout clearance constants
 * for consistent Material 3 styling throughout the app.
 */
object Dimens {
    // Spacing scale
    val spaceExtraSmall: Dp = 4.dp
    val spaceSmall: Dp = 8.dp
    val spaceMedium: Dp = 12.dp
    val spaceLarge: Dp = 16.dp
    val spaceExtraLarge: Dp = 24.dp
    val spaceHuge: Dp = 32.dp

    // Corner Radii
    val cornerExtraSmall = RoundedCornerShape(8.dp)
    val cornerSmall = RoundedCornerShape(12.dp)
    val cornerMedium = RoundedCornerShape(16.dp)
    val cornerLarge = RoundedCornerShape(24.dp)
    val cornerExtraLarge = RoundedCornerShape(28.dp)
    val cornerFull = RoundedCornerShape(50)

    // Navigation Bar Dimensions
    val navBarHeight: Dp = 58.dp
    val navBarMargin: Dp = 12.dp
    val navBarTotalHeight: Dp = navBarHeight + navBarMargin // 70.dp
    val navBarClearance: Dp = 140.dp
    val bulkEditBottomPadding: Dp = navBarHeight + navBarMargin + 14.dp // 84.dp
}

val NavBarClearance: Dp = Dimens.navBarClearance
