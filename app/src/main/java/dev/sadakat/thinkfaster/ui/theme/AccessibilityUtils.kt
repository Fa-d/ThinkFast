package dev.sadakat.thinkfaster.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Accessibility utilities for enhanced app usability
 * Phase 4.2: TalkBack, high-contrast, large text, reduced motion support
 */

/**
 * Check if TalkBack (screen reader) is enabled
 */
fun Context.isTalkBackEnabled(): Boolean {
    val accessibilityEnabled = try {
        Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (e: Settings.SettingNotFoundException) {
        0
    }
    return accessibilityEnabled == 1
}

/**
 * Check if high contrast mode is enabled
 */
fun Context.isHighContrastEnabled(): Boolean {
    return try {
        Settings.Secure.getInt(
            contentResolver,
            "high_text_contrast_enabled"
        ) == 1
    } catch (e: Settings.SettingNotFoundException) {
        false
    }
}

/**
 * Get font scale (for large text support)
 */
fun Context.getFontScale(): Float {
    return resources.configuration.fontScale
}

/**
 * Check if large text is enabled (font scale > 1.0)
 */
fun Context.isLargeTextEnabled(): Boolean {
    return getFontScale() > 1.0f
}

/**
 * Check if animations should be reduced (accessibility preference)
 */
fun Context.shouldReduceMotion(): Boolean {
    return try {
        val animatorDurationScale = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        animatorDurationScale == 0f
    } catch (e: Settings.SettingNotFoundException) {
        false
    }
}

/**
 * Composable wrapper for accessibility checks
 */
@Composable
fun rememberAccessibilityState(): AccessibilityState {
    val context = LocalContext.current
    return remember(context) {
        AccessibilityState(
            isTalkBackEnabled = context.isTalkBackEnabled(),
            isHighContrastEnabled = context.isHighContrastEnabled(),
            isLargeTextEnabled = context.isLargeTextEnabled(),
            shouldReduceMotion = context.shouldReduceMotion(),
            fontScale = context.getFontScale()
        )
    }
}

data class AccessibilityState(
    val isTalkBackEnabled: Boolean,
    val isHighContrastEnabled: Boolean,
    val isLargeTextEnabled: Boolean,
    val shouldReduceMotion: Boolean,
    val fontScale: Float
)

/**
 * Get appropriate animation spec based on reduce motion setting
 */
@Composable
fun <T> adaptiveAnimationSpec(
    normalSpec: AnimationSpec<T>,
    reducedSpec: AnimationSpec<T> = tween(0)
): AnimationSpec<T> {
    val a11yState = rememberAccessibilityState()
    return if (a11yState.shouldReduceMotion) reducedSpec else normalSpec
}

/**
 * Adaptive colors for high contrast mode
 */
object AccessibleColors {
    @Composable
    fun text(normalColor: Color, highContrastColor: Color = Color.Black): Color {
        val a11yState = rememberAccessibilityState()
        return if (a11yState.isHighContrastEnabled) highContrastColor else normalColor
    }

    @Composable
    fun background(normalColor: Color, highContrastColor: Color = Color.White): Color {
        val a11yState = rememberAccessibilityState()
        return if (a11yState.isHighContrastEnabled) highContrastColor else normalColor
    }

    @Composable
    fun accent(normalColor: Color, highContrastColor: Color): Color {
        val a11yState = rememberAccessibilityState()
        return if (a11yState.isHighContrastEnabled) highContrastColor else normalColor
    }

    /**
     * Ensure minimum contrast ratio of 4.5:1 for WCAG AA compliance
     */
    fun ensureContrast(foreground: Color, background: Color): Color {
        val contrast = calculateContrastRatio(foreground, background)
        return if (contrast < 4.5f) {
            // Adjust foreground to meet contrast requirements
            if (background.luminance() > 0.5f) Color.Black else Color.White
        } else {
            foreground
        }
    }

    private fun calculateContrastRatio(color1: Color, color2: Color): Float {
        val lum1 = color1.luminance()
        val lum2 = color2.luminance()
        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun Color.luminance(): Float {
        val r = if (red <= 0.03928f) red / 12.92f else Math.pow(((red + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        val g = if (green <= 0.03928f) green / 12.92f else Math.pow(((green + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        val b = if (blue <= 0.03928f) blue / 12.92f else Math.pow(((blue + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
}

/**
 * Adaptive text size respecting system font scale
 */
@Composable
fun adaptiveTextSize(baseSize: TextUnit): TextUnit {
    val a11yState = rememberAccessibilityState()
    // Cap font scale at 2.0 to prevent extreme scaling that breaks layout
    val cappedScale = minOf(a11yState.fontScale, 2.0f)
    return baseSize * cappedScale
}

/**
 * Adaptive spacing respecting system font scale
 */
@Composable
fun adaptiveSpacing(baseSpacing: Dp): Dp {
    val a11yState = rememberAccessibilityState()
    val density = LocalDensity.current
    // Scale spacing proportionally but cap at 1.5x to maintain layout
    val cappedScale = minOf(a11yState.fontScale, 1.5f)
    return baseSpacing * cappedScale
}

/**
 * Minimum touch target size for accessibility (48dp per Material Design)
 */
val MinimumTouchTarget = 48.dp

/**
 * Semantic helper for intervention content
 */
fun SemanticsPropertyReceiver.interventionContentDescription(
    contentType: String,
    mainText: String,
    additionalInfo: String = ""
) {
    val description = buildString {
        append("Intervention screen. ")
        append("Content type: $contentType. ")
        append(mainText)
        if (additionalInfo.isNotEmpty()) {
            append(". ")
            append(additionalInfo)
        }
    }
    contentDescription = description
}

/**
 * Semantic helper for buttons
 */
fun SemanticsPropertyReceiver.buttonContentDescription(
    label: String,
    action: String,
    state: String = ""
) {
    val description = buildString {
        append("Button. ")
        append(label)
        append(". ")
        append(action)
        if (state.isNotEmpty()) {
            append(". ")
            append(state)
        }
    }
    contentDescription = description
}

/**
 * TalkBack-optimized delay announcement
 */
@Composable
fun talkBackDelay(normalDelay: Long): Long {
    val a11yState = rememberAccessibilityState()
    // Add extra time for TalkBack users to hear announcements
    return if (a11yState.isTalkBackEnabled) {
        (normalDelay * 1.5f).toLong()
    } else {
        normalDelay
    }
}
