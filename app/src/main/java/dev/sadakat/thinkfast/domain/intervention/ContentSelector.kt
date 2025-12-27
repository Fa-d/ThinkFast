package dev.sadakat.thinkfast.domain.intervention

import dev.sadakat.thinkfast.domain.model.*
import kotlin.random.Random

/**
 * Content type categories for weighted randomization
 */
enum class ContentType {
    REFLECTION,
    TIME_ALTERNATIVE,
    BREATHING,
    STATS,
    EMOTIONAL_APPEAL,
    QUOTE,
    GAMIFICATION
}

/**
 * Selects appropriate intervention content based on context using weighted randomization.
 *
 * This is the CORE algorithm that determines what users see on intervention screens.
 * Effectiveness depends on:
 * 1. Variety (prevent habituation)
 * 2. Context awareness (right message at right time)
 * 3. Progressive complexity (gentle → firm)
 */
class ContentSelector {

    // Track last shown content to prevent immediate repeats
    private val recentContent = mutableListOf<String>()
    private val maxRecentContentSize = 10

    /**
     * Selects appropriate intervention content based on current context.
     *
     * @param context Current intervention context
     * @param interventionType Whether this is a reminder or timer intervention
     * @return Selected intervention content
     */
    fun selectContent(
        context: InterventionContext,
        interventionType: InterventionType
    ): InterventionContent {
        // Determine content weights based on context
        val weights = determineWeights(context, interventionType)

        // Select content type using weighted randomization
        val contentType = weightedRandomSelection(weights)

        // Generate content of selected type
        val content = generateContent(contentType, context)

        // Track this content to prevent immediate repeats
        trackShownContent(content)

        return content
    }

    /**
     * Determines content type weights based on context.
     *
     * Base weights (from research):
     * - Reflection: 40%
     * - Time Alternative: 30%
     * - Breathing: 20%
     * - Stats: 10%
     *
     * Adjusted based on context for maximum effectiveness.
     */
    private fun determineWeights(
        context: InterventionContext,
        interventionType: InterventionType
    ): Map<ContentType, Int> {
        val weights = mutableMapOf(
            ContentType.REFLECTION to 40,
            ContentType.TIME_ALTERNATIVE to 30,
            ContentType.BREATHING to 20,
            ContentType.STATS to 10,
            ContentType.EMOTIONAL_APPEAL to 0,  // Used selectively
            ContentType.QUOTE to 0,             // Rare
            ContentType.GAMIFICATION to 0       // Conditional
        )

        // CONTEXT-AWARE ADJUSTMENTS

        // Late night (22:00-05:00): Promote sleep
        if (context.isLateNight) {
            weights[ContentType.BREATHING] = 50          // Breathing helps sleep
            weights[ContentType.REFLECTION] = 30         // Self-awareness
            weights[ContentType.EMOTIONAL_APPEAL] = 15   // "Tomorrow-you will regret this"
            weights[ContentType.TIME_ALTERNATIVE] = 5
            weights[ContentType.STATS] = 0
        }

        // Weekend morning: Encourage better use of precious time
        if (context.isWeekendMorning) {
            weights[ContentType.REFLECTION] = 50         // "Is this how you want to spend Saturday?"
            weights[ContentType.TIME_ALTERNATIVE] = 35
            weights[ContentType.EMOTIONAL_APPEAL] = 10
            weights[ContentType.BREATHING] = 5
        }

        // Quick reopen (< 2 min since last close): Address compulsive behavior
        if (context.quickReopenAttempt) {
            weights[ContentType.REFLECTION] = 60         // Strong reflection needed
            weights[ContentType.EMOTIONAL_APPEAL] = 25   // "This is becoming compulsive"
            weights[ContentType.TIME_ALTERNATIVE] = 10
            weights[ContentType.BREATHING] = 5
            weights[ContentType.STATS] = 0
        }

        // Extended session (15+ min): Show loss framing
        if (context.isExtendedSession) {
            weights[ContentType.TIME_ALTERNATIVE] = 50   // "This 15 min could have been..."
            weights[ContentType.REFLECTION] = 30
            weights[ContentType.EMOTIONAL_APPEAL] = 15
            weights[ContentType.BREATHING] = 5
        }

        // Timer intervention (10-minute alert): Always show time alternatives
        if (interventionType == InterventionType.TIMER) {
            weights[ContentType.TIME_ALTERNATIVE] = 60
            weights[ContentType.REFLECTION] = 25
            weights[ContentType.STATS] = 10
            weights[ContentType.BREATHING] = 5
        }

        // Good progress vs yesterday: Positive reinforcement
        if (context.usageVsYesterday == UsageComparison.LESS) {
            weights[ContentType.STATS] = 40              // Show progress!
            weights[ContentType.REFLECTION] = 35
            weights[ContentType.TIME_ALTERNATIVE] = 20
            weights[ContentType.BREATHING] = 5
        }

        // Over goal: Gentle reminder
        if (context.isOverGoal) {
            weights[ContentType.STATS] = 35              // Show goal status
            weights[ContentType.REFLECTION] = 35
            weights[ContentType.TIME_ALTERNATIVE] = 25
            weights[ContentType.BREATHING] = 5
        }

        // High frequency day (10+ sessions): Address pattern
        if (context.isHighFrequencyDay) {
            weights[ContentType.REFLECTION] = 50
            weights[ContentType.EMOTIONAL_APPEAL] = 20
            weights[ContentType.TIME_ALTERNATIVE] = 20
            weights[ContentType.BREATHING] = 10
        }

        // Streak milestone: Gamification
        if (context.streakDays >= 7) {
            weights[ContentType.GAMIFICATION] = 25       // Celebrate streak!
            weights[ContentType.STATS] = 25
            weights[ContentType.REFLECTION] = 30
            weights[ContentType.TIME_ALTERNATIVE] = 20
        }

        // Occasionally show quotes (5% of the time)
        if (Random.nextInt(100) < 5) {
            weights[ContentType.QUOTE] = 30
        }

        return weights.filterValues { it > 0 }  // Remove zero-weight types
    }

    /**
     * Performs weighted random selection of content type.
     */
    private fun weightedRandomSelection(weights: Map<ContentType, Int>): ContentType {
        val totalWeight = weights.values.sum()
        var randomValue = Random.nextInt(totalWeight)

        for ((contentType, weight) in weights) {
            randomValue -= weight
            if (randomValue < 0) {
                return contentType
            }
        }

        // Fallback (should never reach here)
        return ContentType.REFLECTION
    }

    /**
     * Generates actual content based on selected type and context.
     */
    private fun generateContent(
        contentType: ContentType,
        context: InterventionContext
    ): InterventionContent {
        return when (contentType) {
            ContentType.REFLECTION -> generateReflectionQuestion(context)
            ContentType.TIME_ALTERNATIVE -> generateTimeAlternative(context)
            ContentType.BREATHING -> generateBreathingExercise(context)
            ContentType.STATS -> generateUsageStats(context)
            ContentType.EMOTIONAL_APPEAL -> generateEmotionalAppeal(context)
            ContentType.QUOTE -> generateQuote()
            ContentType.GAMIFICATION -> generateGamification(context)
        }
    }

    /**
     * Generates a reflection question appropriate to context.
     */
    private fun generateReflectionQuestion(context: InterventionContext): InterventionContent.ReflectionQuestion {
        val (category, pool) = when {
            context.isLateNight ->
                ReflectionCategory.LATE_NIGHT to InterventionContentPools.lateNightQuestions

            context.quickReopenAttempt ->
                ReflectionCategory.QUICK_REOPEN to InterventionContentPools.quickReopenQuestions

            context.isFirstSessionOfDay ->
                ReflectionCategory.TRIGGER_AWARENESS to InterventionContentPools.triggerAwarenessQuestions

            context.sessionCount > 5 ->
                ReflectionCategory.PATTERN_RECOGNITION to InterventionContentPools.patternRecognitionQuestions

            Random.nextBoolean() ->
                ReflectionCategory.PRIORITY_CHECK to InterventionContentPools.priorityCheckQuestions

            else ->
                ReflectionCategory.EMOTIONAL_AWARENESS to InterventionContentPools.emotionalAwarenessQuestions
        }

        var question = pool.random()

        // Format quick reopen questions with session count
        if (category == ReflectionCategory.QUICK_REOPEN && question.contains("%d")) {
            question = question.format(context.sessionCount)
        }

        return InterventionContent.ReflectionQuestion(
            question = question,
            subtext = "Take a moment to honestly answer",
            category = category
        )
    }

    /**
     * Generates a time alternative with loss framing.
     */
    private fun generateTimeAlternative(context: InterventionContext): InterventionContent.TimeAlternative {
        val sessionMinutes = context.currentSessionMinutes.coerceAtLeast(1)

        val alternativePool = when {
            sessionMinutes <= 2 -> InterventionContentPools.twoMinuteAlternatives
            sessionMinutes <= 5 -> InterventionContentPools.fiveMinuteAlternatives
            sessionMinutes <= 10 -> InterventionContentPools.tenMinuteAlternatives
            else -> InterventionContentPools.twentyMinuteAlternatives
        }

        val alternative = alternativePool.random()

        return InterventionContent.TimeAlternative(
            sessionMinutes = sessionMinutes,
            alternative = alternative,
            prefix = "This ${sessionMinutes} min could have been"
        )
    }

    /**
     * Generates a breathing exercise.
     */
    private fun generateBreathingExercise(context: InterventionContext): InterventionContent.BreathingExercise {
        val instruction = InterventionContentPools.breathingInstructions.random()

        val variant = when {
            context.isLateNight -> BreathingVariant.CALM_BREATHING  // Gentler for sleep
            Random.nextBoolean() -> BreathingVariant.FOUR_SEVEN_EIGHT
            else -> BreathingVariant.BOX_BREATHING
        }

        return InterventionContent.BreathingExercise(
            duration = when (variant) {
                BreathingVariant.FOUR_SEVEN_EIGHT -> 19  // 4+7+8
                BreathingVariant.BOX_BREATHING -> 16     // 4+4+4+4
                BreathingVariant.CALM_BREATHING -> 20    // 5+5+5+5
            },
            instruction = instruction,
            variant = variant
        )
    }

    /**
     * Generates usage statistics with motivational message.
     */
    private fun generateUsageStats(context: InterventionContext): InterventionContent.UsageStats {
        val message = InterventionContentPools.getStatsMessage(
            todayMinutes = context.totalUsageToday.toInt(),
            yesterdayMinutes = context.totalUsageYesterday.toInt(),
            goalMinutes = context.goalMinutes
        )

        return InterventionContent.UsageStats(
            todayMinutes = context.totalUsageToday.toInt(),
            yesterdayMinutes = context.totalUsageYesterday.toInt(),
            weekAverage = context.weeklyAverage.toInt(),
            goalMinutes = context.goalMinutes,
            message = message
        )
    }

    /**
     * Generates an emotional appeal based on context.
     */
    private fun generateEmotionalAppeal(context: InterventionContext): InterventionContent.EmotionalAppeal {
        val appeals = when {
            context.isLateNight -> InterventionContentPools.lateNightAppeals
            context.isWeekendMorning -> InterventionContentPools.weekendMorningAppeals
            context.quickReopenAttempt -> InterventionContentPools.rapidReopenAppeals
            context.isExtendedSession -> InterventionContentPools.extendedSessionAppeals
            else -> InterventionContentPools.lateNightAppeals  // Fallback
        }

        return appeals.random()
    }

    /**
     * Generates an inspirational quote.
     */
    private fun generateQuote(): InterventionContent.Quote {
        return InterventionContentPools.inspirationalQuotes.random()
    }

    /**
     * Generates a gamification challenge.
     */
    private fun generateGamification(context: InterventionContext): InterventionContent {
        val gamification = InterventionContentPools.getGamificationChallenge(
            currentSessionMinutes = context.currentSessionMinutes,
            bestSessionMinutes = context.bestSessionMinutes,
            streakDays = context.streakDays
        )

        // Fallback to reflection if no gamification available
        return gamification ?: generateReflectionQuestion(context)
    }

    /**
     * Tracks shown content to prevent immediate repeats.
     */
    private fun trackShownContent(content: InterventionContent) {
        val contentKey = when (content) {
            is InterventionContent.ReflectionQuestion -> "R:${content.question}"
            is InterventionContent.TimeAlternative -> "T:${content.alternative.activity}"
            is InterventionContent.BreathingExercise -> "B:${content.variant}"
            is InterventionContent.UsageStats -> "S:stats"
            is InterventionContent.EmotionalAppeal -> "E:${content.message}"
            is InterventionContent.Quote -> "Q:${content.author}"
            is InterventionContent.Gamification -> "G:${content.challenge}"
        }

        recentContent.add(contentKey)

        // Keep only last N items
        if (recentContent.size > maxRecentContentSize) {
            recentContent.removeAt(0)
        }
    }

    /**
     * Checks if content was recently shown (for testing/debugging).
     */
    fun wasRecentlyShown(contentKey: String): Boolean {
        return contentKey in recentContent
    }

    /**
     * Clears recent content history (for testing).
     */
    fun clearHistory() {
        recentContent.clear()
    }
}
