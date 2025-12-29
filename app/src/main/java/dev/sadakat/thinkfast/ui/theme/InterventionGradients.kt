package dev.sadakat.thinkfast.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradient backgrounds for intervention content types
 * Phase 2.3: Enhances visual appeal and content differentiation
 *
 * Each content type has a distinct gradient that reinforces its psychological purpose:
 * - Reflection: Deep blue (contemplation, introspection)
 * - Time Alternative: Warm orange (urgency, action)
 * - Breathing: Soft green (calm, balance)
 * - Stats: Cool gray (neutrality, data)
 * - Emotional: Rich purple (emotion, connection)
 * - Quote: Warm amber (wisdom, inspiration)
 * - Gamification: Energetic teal (achievement, progress)
 * - Activity: Vibrant multi-color (energy, variety)
 */
object InterventionGradients {

    /**
     * Reflection Question gradient - Deep blue for contemplation
     */
    val Reflection = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A237E), // Deep indigo
            Color(0xFF283593)  // Lighter indigo
        )
    )

    val ReflectionDark = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D1B3E), // Darker indigo
            Color(0xFF1A237E)  // Deep indigo
        )
    )

    /**
     * Time Alternative gradient - Warm orange for urgency
     */
    val TimeAlternative = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE65100), // Deep orange
            Color(0xFFF57C00)  // Orange
        )
    )

    val TimeAlternativeDark = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF992E00), // Darker orange
            Color(0xFFE65100)  // Deep orange
        )
    )

    /**
     * Breathing Exercise gradient - Soft green for calm
     */
    val Breathing = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2E7D32), // Forest green
            Color(0xFF43A047)  // Green
        )
    )

    val BreathingDark = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1B5E20), // Dark green
            Color(0xFF2E7D32)  // Forest green
        )
    )

    /**
     * Usage Stats gradient - Cool gray for neutrality
     */
    val Stats = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF455A64), // Blue gray
            Color(0xFF607D8B)  // Light blue gray
        )
    )

    val StatsDark = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF263238), // Dark blue gray
            Color(0xFF455A64)  // Blue gray
        )
    )

    /**
     * Emotional Appeal gradient - Rich purple for emotion
     */
    val Emotional = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF6A1B9A), // Deep purple
            Color(0xFF8E24AA)  // Purple
        )
    )

    val EmotionalDark = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF4A148C), // Darker purple
            Color(0xFF6A1B9A)  // Deep purple
        )
    )

    /**
     * Quote gradient - Warm amber for wisdom
     */
    val Quote = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE65100), // Deep orange (shared with time alternative)
            Color(0xFFFF6F00)  // Amber
        )
    )

    val QuoteDark = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF992E00), // Dark orange
            Color(0xFFE65100)  // Deep orange
        )
    )

    /**
     * Gamification gradient - Energetic teal for achievement
     */
    val Gamification = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF00695C), // Teal
            Color(0xFF00897B)  // Light teal
        )
    )

    val GamificationDark = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF004D40), // Dark teal
            Color(0xFF00695C)  // Teal
        )
    )

    /**
     * Activity Suggestion gradient - Vibrant multi-color for energy
     */
    val Activity = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFD84315), // Deep red-orange
            Color(0xFFE64A19)  // Red-orange
        )
    )

    val ActivityDark = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFBF360C), // Darker red-orange
            Color(0xFFD84315)  // Deep red-orange
        )
    )

    /**
     * Get gradient for intervention content type
     * @param contentType The content type class name
     * @param isDarkTheme Whether dark theme is active
     * @return Appropriate gradient brush
     */
    fun getGradientForContent(contentType: String?, isDarkTheme: Boolean): Brush {
        return when (contentType) {
            "ReflectionQuestion" -> if (isDarkTheme) ReflectionDark else Reflection
            "TimeAlternative" -> if (isDarkTheme) TimeAlternativeDark else TimeAlternative
            "BreathingExercise" -> if (isDarkTheme) BreathingDark else Breathing
            "UsageStats" -> if (isDarkTheme) StatsDark else Stats
            "EmotionalAppeal" -> if (isDarkTheme) EmotionalDark else Emotional
            "Quote" -> if (isDarkTheme) QuoteDark else Quote
            "Gamification" -> if (isDarkTheme) GamificationDark else Gamification
            "ActivitySuggestion" -> if (isDarkTheme) ActivityDark else Activity
            else -> if (isDarkTheme) StatsDark else Stats // Default to stats gradient
        }
    }
}
