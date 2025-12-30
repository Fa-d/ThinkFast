package dev.sadakat.thinkfaster.data.seed.generator

import dev.sadakat.thinkfaster.data.seed.config.PersonaConfig
import dev.sadakat.thinkfaster.data.seed.model.InterventionResultData
import dev.sadakat.thinkfaster.data.seed.model.SessionData
import dev.sadakat.thinkfaster.data.seed.util.TimeDistributionHelper
import kotlin.random.Random

/**
 * Generates intervention results with context-aware content selection and user response patterns.
 * Mirrors the real ContentSelector logic for realistic seed data.
 */
class InterventionResultGenerator(
    private val config: PersonaConfig,
    private val random: Random = Random.Default
) {

    companion object {
        // Intervention probabilities
        const val REMINDER_PROBABILITY = 0.6   // 60% of sessions get reminder (on app open)
        const val TIMER_PROBABILITY = 0.2      // 20% of long sessions get timer alert

        // Content types (matching ContentType enum in real code)
        const val REFLECTION_QUESTION = "ReflectionQuestion"
        const val TIME_ALTERNATIVE = "TimeAlternative"
        const val EMOTIONAL_APPEAL = "EmotionalAppeal"
        const val BREATHING_EXERCISE = "BreathingExercise"
        const val ACTIVITY_SUGGESTION = "ActivitySuggestion"
        const val GAMIFICATION = "Gamification"

        // User choice types
        const val PROCEED = "PROCEED"
        const val GO_BACK = "GO_BACK"
        const val DISMISSED = "DISMISSED"

        // Decision time buckets (milliseconds)
        const val INSTANT_MAX = 2_000L      // <2s
        const val QUICK_MAX = 5_000L        // 2-5s
        const val MODERATE_MAX = 15_000L    // 5-15s
        // 15s+ is DELIBERATE
    }

    /**
     * Generates intervention result for a session if applicable.
     * Returns null if no intervention shown for this session.
     */
    fun generateForSession(
        session: SessionData,
        sessionIndex: Int,
        sessionsToday: Int,
        isQuickReopen: Boolean,
        currentStreak: Int = 0
    ): InterventionResultData? {
        // Determine if this session gets an intervention
        val interventionType = determineInterventionType(session, sessionIndex)
            ?: return null

        // Get session context
        val hourOfDay = TimeDistributionHelper.getHourOfDay(session.startTimestamp)
        val dayOfWeek = TimeDistributionHelper.getDayOfWeek(session.startTimestamp)
        val isWeekend = TimeDistributionHelper.isWeekend(session.startTimestamp)
        val isLateNight = TimeDistributionHelper.isLateNight(hourOfDay)
        val isExtendedSession = (session.duration / (60 * 1000)) >= 15  // 15+ minutes

        // Select content type based on context
        val contentType = selectContentType(
            interventionType = interventionType,
            isLateNight = isLateNight,
            isQuickReopen = isQuickReopen,
            isExtendedSession = isExtendedSession,
            currentStreak = currentStreak
        )

        // Determine user choice based on persona + content + context
        val userChoice = determineUserChoice(
            contentType = contentType,
            isQuickReopen = isQuickReopen,
            isLateNight = isLateNight,
            sessionCount = sessionsToday
        )

        // Determine decision time based on persona + context + choice
        val decisionTimeMs = determineDecisionTime(
            userChoice = userChoice,
            isLateNight = isLateNight,
            contentType = contentType
        )

        // Calculate final session duration (if user went back, session ends early)
        val finalSessionDurationMs = if (userChoice == GO_BACK) {
            // User went back, session ended shortly after intervention
            val earlyEndDuration = random.nextLong(30_000, 120_000)  // 30s-2min
            minOf(earlyEndDuration, session.duration)
        } else {
            session.duration
        }

        val sessionEndedNormally = userChoice != GO_BACK

        return InterventionResultData(
            sessionId = session.id ?: 0L,
            targetApp = session.targetApp,
            interventionType = interventionType,
            contentType = contentType,
            hourOfDay = hourOfDay,
            dayOfWeek = dayOfWeek,
            isWeekend = isWeekend,
            isLateNight = isLateNight,
            sessionCount = sessionsToday,
            quickReopen = isQuickReopen,
            currentSessionDurationMs = session.duration,
            userChoice = userChoice,
            timeToShowDecisionMs = decisionTimeMs,
            finalSessionDurationMs = finalSessionDurationMs,
            sessionEndedNormally = sessionEndedNormally,
            timestamp = session.startTimestamp
        )
    }

    /**
     * Determines if this session gets an intervention and what type.
     */
    private fun determineInterventionType(session: SessionData, sessionIndex: Int): String? {
        // First session of the day always gets REMINDER
        if (sessionIndex == 0) {
            return "REMINDER"
        }

        // 60% chance of REMINDER on app open
        if (random.nextDouble() < REMINDER_PROBABILITY) {
            return "REMINDER"
        }

        // Long sessions (>10 min) have 20% chance of TIMER alert
        val sessionMinutes = session.duration / (60 * 1000)
        if (sessionMinutes >= 10 && random.nextDouble() < TIMER_PROBABILITY) {
            return "TIMER"
        }

        return null  // No intervention
    }

    /**
     * Selects content type based on context.
     * Mirrors ContentSelector logic from the real app.
     */
    private fun selectContentType(
        interventionType: String,
        isLateNight: Boolean,
        isQuickReopen: Boolean,
        isExtendedSession: Boolean,
        currentStreak: Int
    ): String {
        // Build weighted content options based on context
        val contentWeights = mutableMapOf<String, Double>()

        // Base weights
        contentWeights[REFLECTION_QUESTION] = 0.20
        contentWeights[TIME_ALTERNATIVE] = 0.20
        contentWeights[EMOTIONAL_APPEAL] = 0.15
        contentWeights[BREATHING_EXERCISE] = 0.15
        contentWeights[ACTIVITY_SUGGESTION] = 0.15
        contentWeights[GAMIFICATION] = 0.15

        // Context adjustments
        when {
            isLateNight -> {
                // Late night: prefer breathing and activity suggestions
                contentWeights[ACTIVITY_SUGGESTION] = 0.40
                contentWeights[BREATHING_EXERCISE] = 0.30
                contentWeights[REFLECTION_QUESTION] = 0.15
                contentWeights[TIME_ALTERNATIVE] = 0.10
                contentWeights[EMOTIONAL_APPEAL] = 0.05
                contentWeights[GAMIFICATION] = 0.0
            }
            isQuickReopen -> {
                // Quick reopen: prefer reflection and emotional appeal
                contentWeights[REFLECTION_QUESTION] = 0.60
                contentWeights[EMOTIONAL_APPEAL] = 0.25
                contentWeights[BREATHING_EXERCISE] = 0.10
                contentWeights[TIME_ALTERNATIVE] = 0.05
                contentWeights[ACTIVITY_SUGGESTION] = 0.0
                contentWeights[GAMIFICATION] = 0.0
            }
            isExtendedSession -> {
                // Extended session: prefer time alternatives
                contentWeights[TIME_ALTERNATIVE] = 0.50
                contentWeights[ACTIVITY_SUGGESTION] = 0.25
                contentWeights[REFLECTION_QUESTION] = 0.15
                contentWeights[BREATHING_EXERCISE] = 0.10
                contentWeights[EMOTIONAL_APPEAL] = 0.0
                contentWeights[GAMIFICATION] = 0.0
            }
            interventionType == "TIMER" -> {
                // Timer: prefer time alternatives
                contentWeights[TIME_ALTERNATIVE] = 0.60
                contentWeights[ACTIVITY_SUGGESTION] = 0.20
                contentWeights[BREATHING_EXERCISE] = 0.10
                contentWeights[REFLECTION_QUESTION] = 0.10
                contentWeights[EMOTIONAL_APPEAL] = 0.0
                contentWeights[GAMIFICATION] = 0.0
            }
            currentStreak >= 7 -> {
                // Good streak: add gamification
                contentWeights[GAMIFICATION] = 0.25
                contentWeights[REFLECTION_QUESTION] = 0.20
                contentWeights[TIME_ALTERNATIVE] = 0.20
                contentWeights[EMOTIONAL_APPEAL] = 0.15
                contentWeights[BREATHING_EXERCISE] = 0.10
                contentWeights[ACTIVITY_SUGGESTION] = 0.10
            }
        }

        // Normalize weights
        val total = contentWeights.values.sum()
        val normalizedWeights = contentWeights.mapValues { it.value / total }

        // Weighted random selection
        return TimeDistributionHelper.weightedRandom(
            items = normalizedWeights.keys.toList(),
            weights = normalizedWeights.values.toList(),
            random = random
        )
    }

    /**
     * Determines user choice based on persona patterns and context.
     */
    private fun determineUserChoice(
        contentType: String,
        isQuickReopen: Boolean,
        isLateNight: Boolean,
        sessionCount: Int
    ): String {
        // Start with persona base rates
        var proceedRate = config.interventionResponse.proceedRate
        var goBackRate = config.interventionResponse.goBackRate
        var dismissedRate = config.interventionResponse.dismissedRate

        // Context adjustments
        if (isQuickReopen) {
            // Quick reopen: users more likely to proceed (habit override)
            proceedRate += 0.15
        }

        if (isLateNight) {
            // Late night: users more likely to proceed (tired, less willpower)
            proceedRate += 0.10
        }

        if (sessionCount >= 10) {
            // High session count: users more likely to dismiss
            dismissedRate += 0.10
        }

        // Content effectiveness adjustments
        when (contentType) {
            REFLECTION_QUESTION, EMOTIONAL_APPEAL -> {
                // These are more effective at getting users to go back
                goBackRate += 0.15
                proceedRate -= 0.10
            }
            BREATHING_EXERCISE, ACTIVITY_SUGGESTION -> {
                // Moderately effective
                goBackRate += 0.08
                proceedRate -= 0.05
            }
            TIME_ALTERNATIVE -> {
                // Less effective (users rationalize)
                goBackRate += 0.05
            }
            GAMIFICATION -> {
                // Gamification: users more engaged, slightly less proceed
                goBackRate += 0.10
                proceedRate -= 0.08
            }
        }

        // Normalize to ensure they sum to 1.0
        val total = proceedRate + goBackRate + dismissedRate
        proceedRate /= total
        goBackRate /= total
        dismissedRate /= total

        // Weighted random selection
        return TimeDistributionHelper.weightedRandom(
            items = listOf(PROCEED, GO_BACK, DISMISSED),
            weights = listOf(proceedRate, goBackRate, dismissedRate),
            random = random
        )
    }

    /**
     * Determines decision time based on persona distribution and context.
     */
    private fun determineDecisionTime(
        userChoice: String,
        isLateNight: Boolean,
        contentType: String
    ): Long {
        // Start with persona distribution
        var instantRate = config.decisionTimeDistribution.instantRate
        var quickRate = config.decisionTimeDistribution.quickRate
        var moderateRate = config.decisionTimeDistribution.moderateRate
        var deliberateRate = config.decisionTimeDistribution.deliberateRate

        // Context adjustments
        if (isLateNight) {
            // Late night: slower decisions (tired)
            deliberateRate += 0.20
            instantRate -= 0.15
            moderateRate += 0.05
            quickRate -= 0.10
        }

        if (userChoice == GO_BACK) {
            // Going back requires more thought
            deliberateRate += 0.20
            moderateRate += 0.15
            instantRate -= 0.20
            quickRate -= 0.15
        } else if (userChoice == PROCEED) {
            // Proceeding is often habitual/quick
            instantRate += 0.15
            quickRate += 0.10
            moderateRate -= 0.10
            deliberateRate -= 0.15
        }

        // Content complexity
        if (contentType in listOf(REFLECTION_QUESTION, EMOTIONAL_APPEAL)) {
            // These require reading/thinking
            moderateRate += 0.10
            deliberateRate += 0.05
            instantRate -= 0.10
            quickRate -= 0.05
        }

        // Normalize
        val total = instantRate + quickRate + moderateRate + deliberateRate
        instantRate /= total
        quickRate /= total
        moderateRate /= total
        deliberateRate /= total

        // Select bucket
        val bucket = TimeDistributionHelper.weightedRandom(
            items = listOf("instant", "quick", "moderate", "deliberate"),
            weights = listOf(instantRate, quickRate, moderateRate, deliberateRate),
            random = random
        )

        // Sample from bucket
        return when (bucket) {
            "instant" -> random.nextLong(200, INSTANT_MAX)
            "quick" -> random.nextLong(INSTANT_MAX, QUICK_MAX)
            "moderate" -> random.nextLong(QUICK_MAX, MODERATE_MAX)
            else -> random.nextLong(MODERATE_MAX, 30_000)  // 15-30s for deliberate
        }
    }

    /**
     * Generates intervention results for a list of sessions.
     */
    fun generateForSessions(
        sessions: List<SessionData>,
        quickReopenMap: Map<Int, Boolean>,
        currentStreak: Int = 0
    ): List<InterventionResultData> {
        val results = mutableListOf<InterventionResultData>()

        // Group sessions by date to track session count per day
        val sessionsByDate = sessions.groupBy { it.date }

        for ((index, session) in sessions.withIndex()) {
            val sessionsToday = sessionsByDate[session.date]?.size ?: 0
            val sessionIndexToday = sessionsByDate[session.date]?.indexOf(session) ?: 0
            val isQuickReopen = quickReopenMap[index] ?: false

            val result = generateForSession(
                session = session,
                sessionIndex = sessionIndexToday,
                sessionsToday = sessionsToday,
                isQuickReopen = isQuickReopen,
                currentStreak = currentStreak
            )

            if (result != null) {
                results.add(result)
            }
        }

        return results
    }
}
