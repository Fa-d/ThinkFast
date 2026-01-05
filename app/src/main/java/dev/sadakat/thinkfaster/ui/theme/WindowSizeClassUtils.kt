package dev.sadakat.thinkfaster.ui.theme

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Material3 WindowSizeClass integration for ThinkFast
 * Replaces custom ScreenSize enum with industry standard
 */

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val size = DpSize(
        width = configuration.screenWidthDp.dp,
        height = configuration.screenHeightDp.dp
    )
    return WindowSizeClass.calculateFromSize(size)
}

/**
 * Check if current layout should use two-column design
 */
@Composable
fun shouldUseTwoColumnLayout(): Boolean {
    val windowSizeClass = rememberWindowSizeClass()
    val isLandscape = isLandscape()

    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> true // 840dp+, always two columns
        WindowWidthSizeClass.Medium -> isLandscape // 600-839dp, only in landscape
        else -> false
    }
}

/**
 * Get adaptive column count for grids/lists
 */
@Composable
fun getAdaptiveColumnCount(minColumnWidth: Int = 300): Int {
    val windowSizeClass = rememberWindowSizeClass()
    val isLandscape = isLandscape()

    return when {
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> 3
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium && isLandscape -> 2
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> 1
        else -> 1
    }
}

/**
 * Get adaptive max content width for centering on large screens
 */
@Composable
fun getMaxContentWidth(): Dp {
    val windowSizeClass = rememberWindowSizeClass()
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 1200.dp
        WindowWidthSizeClass.Medium -> 840.dp
        else -> Dp.Unspecified
    }
}
