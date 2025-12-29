package dev.sadakat.thinkfast.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Responsive layout utilities for adaptive screen sizes
 * Phase 4.1: Adaptive padding and layout based on screen size
 */

enum class ScreenSize {
    SMALL,      // < 360dp width
    MEDIUM,     // 360-599dp width
    LARGE,      // 600-839dp width
    EXTRA_LARGE // >= 840dp width
}

enum class Orientation {
    PORTRAIT,
    LANDSCAPE
}

/**
 * Get current screen size category
 */
@Composable
fun rememberScreenSize(): ScreenSize {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    return when {
        screenWidthDp < 360 -> ScreenSize.SMALL
        screenWidthDp < 600 -> ScreenSize.MEDIUM
        screenWidthDp < 840 -> ScreenSize.LARGE
        else -> ScreenSize.EXTRA_LARGE
    }
}

/**
 * Get current orientation
 */
@Composable
fun rememberOrientation(): Orientation {
    val configuration = LocalConfiguration.current
    return if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Orientation.LANDSCAPE
    } else {
        Orientation.PORTRAIT
    }
}

/**
 * Adaptive padding based on screen size
 */
object ResponsivePadding {
    /**
     * Standard horizontal padding for content
     */
    @Composable
    fun horizontal(): Dp {
        return when (rememberScreenSize()) {
            ScreenSize.SMALL -> 16.dp
            ScreenSize.MEDIUM -> 24.dp
            ScreenSize.LARGE -> 32.dp
            ScreenSize.EXTRA_LARGE -> 48.dp
        }
    }

    /**
     * Standard vertical padding for content
     */
    @Composable
    fun vertical(): Dp {
        return when (rememberScreenSize()) {
            ScreenSize.SMALL -> 20.dp
            ScreenSize.MEDIUM -> 32.dp
            ScreenSize.LARGE -> 40.dp
            ScreenSize.EXTRA_LARGE -> 56.dp
        }
    }

    /**
     * Overlay container padding
     */
    @Composable
    fun overlay(): Dp {
        return when (rememberScreenSize()) {
            ScreenSize.SMALL -> 24.dp
            ScreenSize.MEDIUM -> 32.dp
            ScreenSize.LARGE -> 48.dp
            ScreenSize.EXTRA_LARGE -> 64.dp
        }
    }

    /**
     * Spacing between elements
     */
    @Composable
    fun elementSpacing(): Dp {
        return when (rememberScreenSize()) {
            ScreenSize.SMALL -> 12.dp
            ScreenSize.MEDIUM -> 16.dp
            ScreenSize.LARGE -> 20.dp
            ScreenSize.EXTRA_LARGE -> 24.dp
        }
    }
}

/**
 * Adaptive font sizes based on screen size
 */
object ResponsiveFontSize {
    /**
     * App name in overlay
     */
    @Composable
    fun appName(): TextUnit {
        return when (rememberScreenSize()) {
            ScreenSize.SMALL -> 18.sp
            ScreenSize.MEDIUM -> 20.sp
            ScreenSize.LARGE -> 24.sp
            ScreenSize.EXTRA_LARGE -> 28.sp
        }
    }

    /**
     * Primary content text (questions, quotes)
     */
    @Composable
    fun primaryContent(): TextUnit {
        return when (rememberScreenSize()) {
            ScreenSize.SMALL -> 22.sp
            ScreenSize.MEDIUM -> 26.sp
            ScreenSize.LARGE -> 30.sp
            ScreenSize.EXTRA_LARGE -> 34.sp
        }
    }

    /**
     * Secondary content text
     */
    @Composable
    fun secondaryContent(): TextUnit {
        return when (rememberScreenSize()) {
            ScreenSize.SMALL -> 16.sp
            ScreenSize.MEDIUM -> 18.sp
            ScreenSize.LARGE -> 20.sp
            ScreenSize.EXTRA_LARGE -> 22.sp
        }
    }

    /**
     * Button text
     */
    @Composable
    fun button(): TextUnit {
        return when (rememberScreenSize()) {
            ScreenSize.SMALL -> 16.sp
            ScreenSize.MEDIUM -> 18.sp
            ScreenSize.LARGE -> 20.sp
            ScreenSize.EXTRA_LARGE -> 22.sp
        }
    }
}

/**
 * Check if device is in landscape mode
 */
@Composable
fun isLandscape(): Boolean {
    return rememberOrientation() == Orientation.LANDSCAPE
}

/**
 * Check if device is a tablet (large screen)
 */
@Composable
fun isTablet(): Boolean {
    val screenSize = rememberScreenSize()
    return screenSize == ScreenSize.LARGE || screenSize == ScreenSize.EXTRA_LARGE
}

/**
 * Get adaptive column count for grid layouts
 */
@Composable
fun adaptiveColumnCount(): Int {
    val isLandscape = isLandscape()
    val screenSize = rememberScreenSize()

    return when {
        isLandscape && screenSize >= ScreenSize.LARGE -> 2
        isLandscape && screenSize >= ScreenSize.MEDIUM -> 2
        screenSize == ScreenSize.EXTRA_LARGE -> 2
        else -> 1
    }
}
