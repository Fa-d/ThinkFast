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

/**
 * Enhanced contrast utilities for gradient backgrounds
 * Provides optimal alpha values and contrast checking for overlays
 */
object GradientContrastUtils {

    /**
     * Calculate optimal alpha for container elements on gradient backgrounds
     * Returns higher alpha for better visibility on complex backgrounds
     *
     * @param isOnGradient Whether the element is on a gradient background
     * @param baseAlpha The baseline alpha value (default 0.15f)
     * @return Optimal alpha value (0.0f to 1.0f)
     */
    fun getOptimalContainerAlpha(
        isOnGradient: Boolean,
        baseAlpha: Float = 0.15f
    ): Float {
        return if (isOnGradient) {
            // Increase alpha on gradients for better visibility
            // 10% -> 35%, 15% -> 52.5%, 18% -> 55%
            minOf(baseAlpha * 3.5f, 0.55f)  // Increased from 2.5f/0.45f for better contrast
        } else {
            baseAlpha
        }
    }

    /**
     * Get optimal border alpha for outlined elements on gradients
     *
     * @param isOnGradient Whether the element is on a gradient background
     * @param baseAlpha The baseline alpha value (default 0.3f)
     * @return Optimal alpha value for borders
     */
    fun getOptimalBorderAlpha(
        isOnGradient: Boolean,
        baseAlpha: Float = 0.3f
    ): Float {
        return if (isOnGradient) {
            // Increase border visibility on gradients
            // 30% -> 75%, capped at 85%
            minOf(baseAlpha * 2.5f, 0.85f)  // Increased from 2.0f/0.7f for better contrast
        } else {
            baseAlpha
        }
    }

    /**
     * Get optimal text alpha on gradients
     * Ensures readability without fully opaque text
     *
     * @param isPrimary Whether this is primary text (vs secondary/hint)
     * @param isOnGradient Whether the text is on a gradient background
     * @return Optimal alpha value for text
     */
    fun getOptimalTextAlpha(
        isPrimary: Boolean,
        isOnGradient: Boolean
    ): Float {
        return when {
            isPrimary && isOnGradient -> 1.0f    // Primary text on gradient: 100%
            isPrimary -> 1.0f                    // Primary on solid: 100%
            isOnGradient -> 0.95f                // Secondary on gradient: 95% (was 0.85f - improved WCAG AA compliance)
            else -> 0.85f                        // Secondary on solid: 85% (was 0.7f - improved readability)
        }
    }

    /**
     * Ensure button meets minimum contrast ratio for WCAG AA compliance
     * Returns adjusted color if needed
     *
     * @param buttonColor The original button color
     * @param backgroundColor The background color to check against
     * @param minContrast Minimum contrast ratio (default 3.0f for large text)
     * @return Color adjusted to meet minimum contrast if needed
     */
    fun ensureButtonContrast(
        buttonColor: Color,
        backgroundColor: Color,
        minContrast: Float = 3.0f  // WCAG AA for large text
    ): Color {
        val currentContrast = calculateContrastRatio(buttonColor, backgroundColor)
        return if (currentContrast < minContrast) {
            // Lighten or darken button color to meet contrast
            if (backgroundColor.luminance() > 0.5f) {
                // Light background - darken button
                buttonColor.copy(
                    red = buttonColor.red * 0.7f,
                    green = buttonColor.green * 0.7f,
                    blue = buttonColor.blue * 0.7f
                )
            } else {
                // Dark background - lighten button
                buttonColor.copy(
                    red = minOf(buttonColor.red * 1.3f, 1f),
                    green = minOf(buttonColor.green * 1.3f, 1f),
                    blue = minOf(buttonColor.blue * 1.3f, 1f)
                )
            }
        } else {
            buttonColor
        }
    }

    /**
     * Calculate WCAG contrast ratio between two colors
     */
    private fun calculateContrastRatio(color1: Color, color2: Color): Float {
        val lum1 = color1.luminance()
        val lum2 = color2.luminance()
        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * Calculate relative luminance of a color
     */
    private fun Color.luminance(): Float {
        val r = if (red <= 0.03928f) red / 12.92f else Math.pow(((red + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        val g = if (green <= 0.03928f) green / 12.92f else Math.pow(((green + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        val b = if (blue <= 0.03928f) blue / 12.92f else Math.pow(((blue + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
}

/**
 * Gradient-specific color system for optimal contrast across different backgrounds.
 * Each gradient type has unique color characteristics requiring tailored text/border colors.
 *
 * Gradient Categories:
 * - DARK: Indigo, Green, Purple, Teal, Blue Gray (luminance ~0.05-0.12)
 * - WARM: Orange, Amber, Red-Orange (luminance ~0.12-0.25)
 */
object GradientColorSystem {

    /**
     * Color palette configuration for a specific gradient type
     */
    data class GradientPalette(
        val primaryText: Color,
        val secondaryText: Color,
        val border: Color,
        val container: Color,
        val icon: Color
    )

    /**
     * Get optimized colors for the given content type (gradient background)
     *
     * @param contentType The intervention content type (determines gradient)
     * @param isDarkTheme Whether system dark theme is active
     * @return Optimized color palette for the specific gradient
     */
    fun getPaletteForContent(contentType: String?, isDarkTheme: Boolean): GradientPalette {
        return when (contentType) {
            // Dark gradients - standard light colors work well
            "ReflectionQuestion",
            "BreathingExercise",
            "EmotionalAppeal",
            "Gamification",
            "UsageStats" -> getDarkGradientPalette()

            // Warm/bright gradients - need higher contrast
            "TimeAlternative",
            "Quote",
            "ActivitySuggestion" -> getWarmGradientPalette()

            // Fallback to dark gradient palette
            else -> getDarkGradientPalette()
        }
    }

    /**
     * Colors for DARK gradients (Indigo, Green, Purple, Teal, Blue Gray)
     * These gradients have low luminance (~0.05-0.12), so pure light colors work best.
     */
    private fun getDarkGradientPalette(): GradientPalette {
        return GradientPalette(
            primaryText = Color(0xFFFFFFFF),  // Pure white
            secondaryText = Color(0xFFF5F5F5),  // Off-white with 96% opacity equivalent
            border = Color(0xFFFFFFFF).copy(alpha = 0.5f),  // Semi-transparent white border
            container = Color.White.copy(alpha = 0.55f),  // White container with good opacity
            icon = Color(0xFFFFFFFF).copy(alpha = 0.9f)  // Near-white icon
        )
    }

    /**
     * Colors for WARM gradients (Orange, Amber, Red-Orange)
     * These gradients have medium-high luminance (~0.12-0.25), requiring adjustments:
     * - Slightly darker text for better contrast
     * - Higher opacity borders for visibility
     * - Darker containers for separation
     */
    private fun getWarmGradientPalette(): GradientPalette {
        return GradientPalette(
            primaryText = Color(0xFFF5F5F5),  // Off-white (not pure white for better contrast on orange)
            secondaryText = Color(0xFFE0E0E0),  // Light gray with ~88% opacity equivalent
            border = Color(0xFF1A1A1A).copy(alpha = 0.35f),  // Dark border for visibility on warm backgrounds
            container = Color(0xFF1A1A1A).copy(alpha = 0.35f),  // Dark container for contrast
            icon = Color(0xFFF5F5F5).copy(alpha = 0.85f)  // Off-white icon
        )
    }

    /**
     * Get primary text color for a gradient
     */
    fun getPrimaryText(contentType: String?, isDarkTheme: Boolean): Color {
        return getPaletteForContent(contentType, isDarkTheme).primaryText
    }

    /**
     * Get secondary text color for a gradient
     */
    fun getSecondaryText(contentType: String?, isDarkTheme: Boolean): Color {
        return getPaletteForContent(contentType, isDarkTheme).secondaryText
    }

    /**
     * Get border color for outlined elements on a gradient
     */
    fun getBorder(contentType: String?, isDarkTheme: Boolean): Color {
        return getPaletteForContent(contentType, isDarkTheme).border
    }

    /**
     * Get container background color for cards/panels on a gradient
     */
    fun getContainer(contentType: String?, isDarkTheme: Boolean): Color {
        return getPaletteForContent(contentType, isDarkTheme).container
    }

    /**
     * Get icon/tint color for a gradient
     */
    fun getIcon(contentType: String?, isDarkTheme: Boolean): Color {
        return getPaletteForContent(contentType, isDarkTheme).icon
    }
}
