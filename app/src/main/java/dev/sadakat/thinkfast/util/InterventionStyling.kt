package dev.sadakat.thinkfast.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import dev.sadakat.thinkfast.domain.model.InterventionContent
import dev.sadakat.thinkfast.ui.theme.InterventionColors
import dev.sadakat.thinkfast.ui.theme.InterventionTypography

/**
 * Visual styling properties for intervention content
 */
data class InterventionStyle(
    val backgroundColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val primaryTextStyle: TextStyle,
    val secondaryTextStyle: TextStyle
)

/**
 * Helper object for applying consistent styling to intervention overlays
 * based on content type.
 *
 * This ensures that each content type has appropriate visual treatment
 * according to psychological research on color and typography.
 */
object InterventionStyling {

    /**
     * Gets the appropriate styling for a given intervention content type
     *
     * @param content The intervention content to style
     * @param isDarkTheme Whether dark theme is active
     * @return Complete styling properties
     */
    @Composable
    fun getStyleForContent(
        content: InterventionContent,
        isDarkTheme: Boolean = isSystemInDarkTheme()
    ): InterventionStyle {
        return when (content) {
            is InterventionContent.ReflectionQuestion -> reflectionStyle(isDarkTheme)
            is InterventionContent.TimeAlternative -> timeAlternativeStyle(isDarkTheme)
            is InterventionContent.BreathingExercise -> breathingStyle(isDarkTheme)
            is InterventionContent.UsageStats -> statsStyle(isDarkTheme)
            is InterventionContent.EmotionalAppeal -> emotionalAppealStyle(isDarkTheme)
            is InterventionContent.Quote -> quoteStyle(isDarkTheme)
            is InterventionContent.Gamification -> gamificationStyle(isDarkTheme)
            is InterventionContent.ActivitySuggestion -> activitySuggestionStyle(isDarkTheme)
        }
    }

    /**
     * Reflection question styling - Calm blue with serif typography
     */
    private fun reflectionStyle(isDarkTheme: Boolean) = InterventionStyle(
        backgroundColor = if (isDarkTheme)
            InterventionColors.ReflectionBackgroundDark
        else
            InterventionColors.ReflectionBackground,
        textColor = if (isDarkTheme)
            InterventionColors.InterventionTextPrimaryDark
        else
            InterventionColors.InterventionTextPrimary,
        accentColor = InterventionColors.GoalLine,
        primaryTextStyle = InterventionTypography.ReflectionQuestion,
        secondaryTextStyle = InterventionTypography.ReflectionSubtext
    )

    /**
     * Time alternative styling - Soft red with clear typography
     */
    private fun timeAlternativeStyle(isDarkTheme: Boolean) = InterventionStyle(
        backgroundColor = if (isDarkTheme)
            InterventionColors.TimerAlertBackgroundDark
        else
            InterventionColors.TimerAlertBackground,
        textColor = if (isDarkTheme)
            InterventionColors.InterventionTextPrimaryDark
        else
            InterventionColors.InterventionTextPrimary,
        accentColor = InterventionColors.Warning,
        primaryTextStyle = InterventionTypography.TimeAlternativeActivity,
        secondaryTextStyle = InterventionTypography.TimeAlternativePrefix
    )

    /**
     * Breathing exercise styling - Nature green with calm typography
     */
    private fun breathingStyle(isDarkTheme: Boolean) = InterventionStyle(
        backgroundColor = if (isDarkTheme)
            InterventionColors.BreathingBackgroundDark
        else
            InterventionColors.BreathingBackground,
        textColor = if (isDarkTheme)
            InterventionColors.InterventionTextPrimaryDark
        else
            InterventionColors.InterventionTextPrimary,
        accentColor = InterventionColors.Success,
        primaryTextStyle = InterventionTypography.BreathingInstruction,
        secondaryTextStyle = InterventionTypography.BreathingPhase
    )

    /**
     * Usage stats styling - Neutral purple with monospace typography
     */
    private fun statsStyle(isDarkTheme: Boolean) = InterventionStyle(
        backgroundColor = if (isDarkTheme)
            InterventionColors.StatsBackgroundDark
        else
            InterventionColors.StatsBackground,
        textColor = if (isDarkTheme)
            InterventionColors.InterventionTextPrimaryDark
        else
            InterventionColors.InterventionTextPrimary,
        accentColor = InterventionColors.GoalLine,
        primaryTextStyle = InterventionTypography.StatsNumber,
        secondaryTextStyle = InterventionTypography.StatsMessage
    )

    /**
     * Emotional appeal styling - Deep amber with bold typography
     */
    private fun emotionalAppealStyle(isDarkTheme: Boolean) = InterventionStyle(
        backgroundColor = if (isDarkTheme)
            InterventionColors.EmotionalAppealBackgroundDark
        else
            InterventionColors.EmotionalAppealBackground,
        textColor = if (isDarkTheme)
            InterventionColors.InterventionTextPrimaryDark
        else
            InterventionColors.InterventionTextPrimary,
        accentColor = InterventionColors.Error,
        primaryTextStyle = InterventionTypography.EmotionalAppealMessage,
        secondaryTextStyle = InterventionTypography.EmotionalAppealSubtext
    )

    /**
     * Quote styling - Soft purple with elegant serif
     */
    private fun quoteStyle(isDarkTheme: Boolean) = InterventionStyle(
        backgroundColor = if (isDarkTheme)
            InterventionColors.QuoteBackgroundDark
        else
            InterventionColors.QuoteBackground,
        textColor = if (isDarkTheme)
            InterventionColors.InterventionTextPrimaryDark
        else
            InterventionColors.InterventionTextPrimary,
        accentColor = InterventionColors.GoalLine,
        primaryTextStyle = InterventionTypography.QuoteText,
        secondaryTextStyle = InterventionTypography.QuoteAuthor
    )

    /**
     * Gamification styling - Vibrant blue with energetic typography
     */
    private fun gamificationStyle(isDarkTheme: Boolean) = InterventionStyle(
        backgroundColor = if (isDarkTheme)
            InterventionColors.GamificationBackgroundDark
        else
            InterventionColors.GamificationBackground,
        textColor = if (isDarkTheme)
            InterventionColors.InterventionTextPrimaryDark
        else
            InterventionColors.InterventionTextPrimary,
        accentColor = InterventionColors.Success,
        primaryTextStyle = InterventionTypography.GamificationChallenge,
        secondaryTextStyle = InterventionTypography.GamificationReward
    )

    /**
     * Activity suggestion styling - Soft teal with action-oriented typography
     */
    private fun activitySuggestionStyle(isDarkTheme: Boolean) = InterventionStyle(
        backgroundColor = if (isDarkTheme)
            InterventionColors.ActivitySuggestionBackgroundDark
        else
            InterventionColors.ActivitySuggestionBackground,
        textColor = if (isDarkTheme)
            InterventionColors.InterventionTextPrimaryDark
        else
            InterventionColors.InterventionTextPrimary,
        accentColor = InterventionColors.Success,
        primaryTextStyle = InterventionTypography.InterventionMessage,
        secondaryTextStyle = InterventionTypography.InterventionTitle
    )

    /**
     * Gets background color for gentle reminder overlay (default fallback)
     */
    @Composable
    fun getGentleReminderBackground(
        isDarkTheme: Boolean = isSystemInDarkTheme()
    ): Color {
        return if (isDarkTheme)
            InterventionColors.GentleReminderBackgroundDark
        else
            InterventionColors.GentleReminderBackground
    }

    /**
     * Gets background color for urgent alert (extended sessions)
     */
    @Composable
    fun getUrgentAlertBackground(
        isDarkTheme: Boolean = isSystemInDarkTheme()
    ): Color {
        return if (isDarkTheme)
            InterventionColors.UrgentAlertBackgroundDark
        else
            InterventionColors.UrgentAlertBackground
    }

    /**
     * Gets button colors (Go Back vs Proceed)
     */
    @Composable
    fun getButtonColors(
        isDarkTheme: Boolean = isSystemInDarkTheme()
    ): Pair<Color, Color> {
        val goBackColor = if (isDarkTheme)
            InterventionColors.GoBackButtonDark
        else
            InterventionColors.GoBackButton

        val proceedColor = if (isDarkTheme)
            InterventionColors.ProceedButtonDark
        else
            InterventionColors.ProceedButton

        return Pair(goBackColor, proceedColor)
    }

    /**
     * Determines if we should show urgent styling based on session duration
     */
    fun shouldUseUrgentStyling(sessionMinutes: Int): Boolean {
        return sessionMinutes >= 15
    }

    /**
     * Gets app brand color (Facebook or Instagram)
     */
    fun getAppBrandColor(appPackageName: String): Color {
        return when {
            appPackageName.contains("facebook", ignoreCase = true) ->
                InterventionColors.Facebook
            appPackageName.contains("instagram", ignoreCase = true) ->
                InterventionColors.InstagramPink
            else ->
                InterventionColors.ProceedButton
        }
    }

    /**
     * Gets progress state color based on goal comparison
     */
    fun getProgressStateColor(
        currentUsage: Long,
        goalMinutes: Int?
    ): Color {
        if (goalMinutes == null) return InterventionColors.GoalLine

        val currentMinutes = currentUsage / (1000 * 60)
        val percentOfGoal = (currentMinutes.toFloat() / goalMinutes) * 100

        return when {
            percentOfGoal <= 80 -> InterventionColors.Success  // On track
            percentOfGoal <= 100 -> InterventionColors.Warning // Approaching limit
            else -> InterventionColors.Error                   // Over limit
        }
    }
}
