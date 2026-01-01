package dev.sadakat.thinkfaster.presentation.overlay.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.domain.model.InterventionContent

/**
 * Shared intervention content renderers used by both full-screen and compact overlays
 * Extracted from ReminderOverlayWindow.kt for code reuse
 */

/**
 * Main dispatcher that renders different types of intervention content
 */
@Composable
fun InterventionContentRenderer(
    content: InterventionContent,
    textColor: Color,
    secondaryTextColor: Color
) {
    when (content) {
        is InterventionContent.ReflectionQuestion -> ReflectionQuestionContent(content, textColor, secondaryTextColor)
        is InterventionContent.TimeAlternative -> TimeAlternativeContent(content, textColor, secondaryTextColor)
        is InterventionContent.BreathingExercise -> BreathingExerciseContent(content, textColor)
        is InterventionContent.UsageStats -> UsageStatsContent(content, textColor, secondaryTextColor)
        is InterventionContent.EmotionalAppeal -> EmotionalAppealContent(content, textColor, secondaryTextColor)
        is InterventionContent.Quote -> QuoteContent(content, textColor, secondaryTextColor)
        is InterventionContent.Gamification -> GamificationContent(content, textColor, secondaryTextColor)
        is InterventionContent.ActivitySuggestion -> ActivitySuggestionContent(content, textColor, secondaryTextColor)
    }
}

/**
 * Reflection question content
 */
@Composable
private fun ReflectionQuestionContent(
    content: InterventionContent.ReflectionQuestion,
    textColor: Color,
    secondaryTextColor: Color
) {
    Text(
        text = content.question,
        fontSize = 26.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Serif,
        color = textColor,
        textAlign = TextAlign.Center,
        lineHeight = 36.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = content.subtext,
        fontSize = 16.sp,
        color = secondaryTextColor,
        textAlign = TextAlign.Center,
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
    )
}

/**
 * Time alternative content (loss framing)
 * Shows fewer alternatives for reminder overlay (simpler intervention)
 */
@Composable
private fun TimeAlternativeContent(
    content: InterventionContent.TimeAlternative,
    textColor: Color,
    secondaryTextColor: Color
) {
    // Emoji prefix: "💡 This could have been:"
    Text(
        text = content.prefix,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Show first 2 alternatives for reminder overlay (keep it simple)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        content.alternatives.take(2).forEach { alternative ->
            Text(
                text = "${alternative.emoji} ${alternative.activity}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
            if (alternative != content.alternatives.take(2).last()) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Breathing exercise content
 * Uses the shared CompactBreathingExercise component with improved animation
 */
@Composable
private fun BreathingExerciseContent(
    content: InterventionContent.BreathingExercise,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Instruction text above the breathing exercise
        Text(
            text = content.instruction,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Use the shared CompactBreathingExercise component with improved animation
        // It uses the same natural EaseInOutCubic easing and smooth transitions
        CompactBreathingExercise(
            variant = content.variant,
            isDarkTheme = isSystemInDarkTheme(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Usage stats content
 * Phase 2.5: Enhanced with visual progress indicators
 */
@Composable
private fun UsageStatsContent(
    content: InterventionContent.UsageStats,
    textColor: Color,
    secondaryTextColor: Color
) {
    // Show header message if present
    if (content.message.isNotEmpty()) {
        Text(
            text = content.message,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Use enhanced stats visualization
    EnhancedUsageStatsContent(
        todayMinutes = content.todayMinutes,
        yesterdayMinutes = content.yesterdayMinutes,
        weekAvgMinutes = content.weekAverage,
        goalMinutes = content.goalMinutes,
        textColor = textColor,
        secondaryTextColor = secondaryTextColor
    )
}

/**
 * Emotional appeal content
 */
@Composable
private fun EmotionalAppealContent(
    content: InterventionContent.EmotionalAppeal,
    textColor: Color,
    secondaryTextColor: Color
) {
    Text(
        text = content.message,
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        color = textColor,
        textAlign = TextAlign.Center,
        lineHeight = 32.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = content.subtext,
        fontSize = 16.sp,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )
}

/**
 * Quote content with decorative styling
 * Phase 3.4: Enhanced with large quotation marks and better typography
 */
@Composable
private fun QuoteContent(
    content: InterventionContent.Quote,
    textColor: Color,
    secondaryTextColor: Color
) {
    EnhancedQuoteContent(
        quote = content.quote,
        author = content.author,
        textColor = textColor,
        secondaryTextColor = secondaryTextColor
    )
}

/**
 * Gamification content with progress visualization
 * Phase 3.2: Enhanced with circular progress ring and milestones
 */
@Composable
private fun GamificationContent(
    content: InterventionContent.Gamification,
    textColor: Color,
    secondaryTextColor: Color
) {
    EnhancedGamificationContent(
        challenge = content.challenge,
        reward = content.reward,
        currentProgress = content.currentProgress,
        target = content.target,
        textColor = textColor,
        secondaryTextColor = secondaryTextColor
    )
}

/**
 * Activity suggestion content with interactivity
 * Phase 3.3: Enhanced with tap interactions and completion button
 */
@Composable
private fun ActivitySuggestionContent(
    content: InterventionContent.ActivitySuggestion,
    textColor: Color,
    secondaryTextColor: Color
) {
    EnhancedActivitySuggestionContent(
        suggestion = content.suggestion,
        emoji = content.emoji,
        timeEstimate = getActivityTimeEstimate(content.suggestion),
        textColor = textColor,
        secondaryTextColor = secondaryTextColor,
        onActivityCompleted = {
            // User marked activity as completed - positive reinforcement
            // Could track this in analytics later
        }
    )
}
