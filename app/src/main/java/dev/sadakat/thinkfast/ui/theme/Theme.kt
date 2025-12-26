package dev.sadakat.thinkfast.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light color scheme with better contrast and accessibility
 */
private val LightColorScheme = lightColorScheme(
    // Primary colors - calm purple tones
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005E),

    // Secondary colors - soft teal tones for better distinction
    secondary = Color(0xFF006C4C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF89F8C7),
    onSecondaryContainer = Color(0xFF002114),

    // Tertiary colors
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF31111D),

    // Error colors - high contrast for visibility
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Background colors - warm off-white for reduced eye strain
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),

    // Outline colors
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),

    // Inverse colors for elevated elements
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFD0BCFF),

    // scrim for overlays
    scrim = Color(0xFF000000)
)

/**
 * Dark color scheme with improved contrast and readability
 */
private val DarkColorScheme = darkColorScheme(
    // Primary colors - lighter purples for dark backgrounds
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),

    // Secondary colors
    secondary = Color(0xFF6CDBAA),
    onSecondary = Color(0xFF003828),
    secondaryContainer = Color(0xFF005138),
    onSecondaryContainer = Color(0xFF89F8C7),

    // Tertiary colors
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B49),
    onTertiaryContainer = Color(0xFFFFD9E3),

    // Error colors
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Background colors - true black for OLED efficiency
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),

    // Outline colors with better contrast
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),

    // Inverse colors
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF6750A4),

    // scrim for overlays
    scrim = Color(0xFF000000)
)

/**
 * Theme mode enum
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    FOLLOW_SYSTEM
}

/**
 * Main theme function with enhanced dark mode support
 *
 * @param darkTheme Whether to use dark theme (null = follow system)
 * @param dynamicColor Whether to use Material You dynamic colors
 * @param amoledDark Whether to use pure black for OLED screens (dark theme only)
 * @param content The composable content to be themed
 */
@Composable
fun ThinkFastTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = true,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    // Determine dark mode preference
    val systemDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = darkTheme ?: systemDarkTheme

    // Get the appropriate color scheme
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) {
                if (amoledDark) {
                    // Create AMOLED-friendly dark scheme with true blacks
                    dynamicDarkColorScheme(context).copy(
                        background = Color(0xFF000000),
                        surface = Color(0xFF000000)
                    )
                } else {
                    dynamicDarkColorScheme(context)
                }
            } else {
                dynamicLightColorScheme(context)
            }
        }

        useDarkTheme -> {
            if (amoledDark) {
                // Create AMOLED-friendly dark scheme with true blacks
                DarkColorScheme.copy(
                    background = Color(0xFF000000),
                    surface = Color(0xFF000000)
                )
            } else {
                DarkColorScheme
            }
        }

        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Only set status bar color if context is an Activity
            // (not when used in overlays from Service context)
            val context = view.context
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Theme with explicit mode selection
 */
@Composable
fun ThinkFastThemeWithMode(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    dynamicColor: Boolean = true,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.FOLLOW_SYSTEM -> null
    }

    ThinkFastTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        amoledDark = amoledDark,
        content = content
    )
}
