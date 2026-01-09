package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.domain.model.*
import dev.sadakat.thinkfaster.domain.repository.InterventionResultRepository
import dev.sadakat.thinkfaster.domain.repository.StatsRepository
import dev.sadakat.thinkfaster.util.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Opportunity detection result with JITAI decision
 * Phase 2 JITAI: Smart intervention timing based on context
 */
data class OpportunityDetection(
    val score: Int,                      // 0-100
    val level: OpportunityLevel,
    val decision: InterventionDecision,
    val breakdown: OpportunityBreakdown,
    val detectedAt: Long = System.currentTimeMillis()
)

/**
 * Detailed breakdown of opportunity scoring
 * Phase 2 JITAI: Shows how each factor contributed to the score
 * Phase 3: Added behavioral cues factor
 */
data class OpportunityBreakdown(
    val timeReceptiveness: Int,          // 0-25
    val sessionPattern: Int,             // 0-20
    val cognitiveLoad: Int,              // 0-15
    val historicalSuccess: Int,          // 0-20
    val userState: Int,                  // 0-20
    val behavioralCues: Int = 0,         // 0-15 (Phase 3)
    val factors: Map<String, String>     // Human-readable factor descriptions
)

/**
 * Opportunity Detector
 * Phase 2 JITAI: Determines optimal moments for interventions
 * Phase 3: Personalized timing and behavioral cues
 *
 * Calculates a 0-115 opportunity score based on six factors:
 * 1. Time Receptiveness (25 pts): Personalized optimal intervention times
 * 2. Session Pattern (20 pts): Quick reopen, first session, extended
 * 3. Cognitive Load (15 pts): Lower cognitive load = better reception
 * 4. Historical Success (20 pts): Past success in similar contexts
 * 5. User State (20 pts): Positive state, on streak
 * 6. Behavioral Cues (15 pts): Immediate behavioral signals (Phase 3)
 *
 * Caching: Results are cached for 5 minutes within a session
 */
class OpportunityDetector(
    private val interventionRepository: InterventionResultRepository,
    private val preferences: InterventionPreferences,
    private val contextualTimingOptimizer: ContextualTimingOptimizer? = null,  // Phase 3: Optional personalized timing
    private val timingPatternLearner: TimingPatternLearner? = null  // Phase 3: Optional timing learning
) {

    companion object {
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        private const val MIN_HISTORICAL_DATA = 10 // Minimum interventions for historical analysis

        // Cached detection result per app session
        private var cachedDetection: OpportunityDetection? = null
        private var cacheTimestamp: Long = 0L
        private var cachedApp: String? = null

        // Time ranges (hour of day)
        private const val LATE_NIGHT_START = 22  // 10 PM
        private const val LATE_NIGHT_END = 2     // 2 AM
        private const val EARLY_MORNING_START = 3 // 3 AM
        private const val EARLY_MORNING_END = 5   // 5 AM
        private const val MORNING_START = 6       // 6 AM
        private const val MORNING_END = 9         // 9 AM
        private const val MID_DAY_START = 10      // 10 AM
        private const val MID_DAY_END = 16        // 4 PM
        private const val EVENING_START = 17      // 5 PM
        private const val EVENING_END = 21        // 9 PM
    }

    /**
     * Detect intervention opportunity with caching
     * @param context Current intervention context
     * @param forceRefresh Force re-calculation even if cache is valid
     * @return Opportunity detection with JITAI decision
     */
    suspend fun detectOpportunity(
        context: InterventionContext,
        forceRefresh: Boolean = false
    ): OpportunityDetection = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // Return cached result if still valid for same app
        if (!forceRefresh && cachedDetection != null &&
            cachedApp == context.targetApp &&
            (now - cacheTimestamp) < CACHE_DURATION_MS) {
            ErrorLogger.debug(
                message = "Using cached opportunity: ${cachedDetection?.score} (${cachedDetection?.level})",
                context = "OpportunityDetector"
            )
            return@withContext cachedDetection!!
        }

        // Calculate fresh opportunity score
        val breakdown = calculateBreakdown(context)
        val score = breakdown.timeReceptiveness +
                    breakdown.sessionPattern +
                    breakdown.cognitiveLoad +
                    breakdown.historicalSuccess +
                    breakdown.userState +
                    breakdown.behavioralCues  // Phase 3

        val level = when {
            score >= 70 -> OpportunityLevel.EXCELLENT
            score >= 50 -> OpportunityLevel.GOOD
            score >= 30 -> OpportunityLevel.MODERATE
            else -> OpportunityLevel.POOR
        }

        val decision = when {
            score >= 70 -> InterventionDecision.INTERVENE_NOW
            score >= 50 -> InterventionDecision.INTERVENE_WITH_CONSIDERATION
            score >= 30 -> InterventionDecision.WAIT_FOR_BETTER_OPPORTUNITY
            else -> InterventionDecision.SKIP_INTERVENTION
        }

        val detection = OpportunityDetection(
            score = score,
            level = level,
            decision = decision,
            breakdown = breakdown,
            detectedAt = now
        )

        // Cache the result
        cachedDetection = detection
        cacheTimestamp = now
        cachedApp = context.targetApp

        ErrorLogger.info(
            message = "Opportunity: $score/100 ($level) - $decision",
            context = "OpportunityDetector"
        )

        detection
    }

    /**
     * Calculate detailed breakdown of opportunity score
     */
    private suspend fun calculateBreakdown(context: InterventionContext): OpportunityBreakdown {
        val timeScore = calculateTimeReceptiveness(context)
        val sessionScore = calculateSessionPattern(context)
        val cognitiveScore = calculateCognitiveLoad(context)
        val historicalScore = calculateHistoricalSuccess(context)
        val userStateScore = calculateUserState(context)
        val behavioralScore = calculateBehavioralCues(context)  // Phase 3

        val factors = mutableMapOf<String, String>()
        factors["time"] = getTimeDescription(context.timeOfDay, context.dayOfWeek)
        factors["session"] = getSessionPatternDescription(context)
        factors["cognitive"] = getCognitiveLoadDescription(context)
        factors["historical"] = getHistoricalSuccessDescription(historicalScore)
        factors["user_state"] = getUserStateDescription(context)
        factors["behavioral"] = getBehavioralCuesDescription(context)  // Phase 3

        return OpportunityBreakdown(
            timeReceptiveness = timeScore,
            sessionPattern = sessionScore,
            cognitiveLoad = cognitiveScore,
            historicalSuccess = historicalScore,
            userState = userStateScore,
            behavioralCues = behavioralScore,  // Phase 3
            factors = factors
        )
    }

    /**
     * Time Receptiveness (25 points)
     * Phase 3: Uses personalized timing when available, falls back to population defaults
     */
    private suspend fun calculateTimeReceptiveness(context: InterventionContext): Int {
        val hour = context.timeOfDay
        val isWeekend = context.isWeekend

        // Phase 3: Try personalized timing first
        timingPatternLearner?.let { learner ->
            val pattern = learner.getTimingPattern(hour, context.targetApp, isWeekend)
            if (pattern != null && pattern.isReliable) {
                // Convert success rate to 0-25 points
                return when {
                    pattern.successRate >= 0.70f -> 25  // Excellent
                    pattern.successRate >= 0.55f -> 20  // Good
                    pattern.successRate >= 0.40f -> 15  // Moderate
                    pattern.successRate >= 0.25f -> 10  // Below average
                    else -> 5  // Poor
                }
            }
        }

        // Fall back to population-level defaults
        return when {
            // Late night (22:00-02:00) - high receptiveness if over goal
            (hour >= LATE_NIGHT_START || hour <= LATE_NIGHT_END) -> {
                if (context.isOverGoal) 25 else 20
            }

            // Early morning (03:00-05:00) - low receptiveness
            (hour in EARLY_MORNING_START..EARLY_MORNING_END) -> 5

            // Morning (06:00-09:00) - high receptiveness, especially on weekends
            (hour in MORNING_START..MORNING_END) -> {
                when {
                    isWeekend && context.sessionCount == 1 -> 25  // Weekend morning + first session
                    isWeekend -> 22
                    context.sessionCount == 1 -> 23
                    else -> 20
                }
            }

            // Mid-day (10:00-16:00) - moderate receptiveness
            (hour in MID_DAY_START..MID_DAY_END) -> {
                when {
                    context.isOverGoal -> 18
                    context.currentSessionMinutes >= 15 -> 15 // Extended session
                    else -> 12
                }
            }

            // Evening (17:00-21:00) - moderate to high receptiveness
            (hour in EVENING_START..EVENING_END) -> {
                when {
                    isWeekend && context.isOverGoal -> 23
                    context.isOverGoal -> 20
                    else -> 15
                }
            }

            // Default
            else -> 10
        }
    }

    /**
     * Session Pattern (20 points)
     * Based on quick reopen, first session, or extended session
     */
    private fun calculateSessionPattern(context: InterventionContext): Int {
        return when {
            // Quick reopen - highest priority for intervention
            context.quickReopenAttempt -> 20

            // First session of the day
            context.sessionCount == 1 -> 15

            // Extended session (> 15 minutes)
            context.currentSessionMinutes >= 15 -> {
                when {
                    context.currentSessionMinutes >= 30 -> 18  // >= 30 min
                    else -> 12
                }
            }

            // Moderate session (5-15 minutes)
            context.currentSessionMinutes >= 5 -> 8

            // Short session (< 5 minutes) - may not need intervention yet
            else -> 5
        }
    }

    /**
     * Cognitive Load (15 points)
     * Lower cognitive load = better intervention reception
     *
     * In a production system, this would integrate with system APIs to detect:
     * - Active phone calls
     * - Playing media/audio
     * - Driving mode
     * - Screen sharing/mirroring
     *
     * For now, uses proxy indicators from session data
     */
    private suspend fun calculateCognitiveLoad(context: InterventionContext): Int {
        var score = 15 // Start with maximum

        // Reduce score if likely high cognitive load situations
        // Quick reopens suggest compulsive checking (lower cognitive resistance)
        if (!context.quickReopenAttempt) {
            score -= 3
        }

        // Longer sessions suggest deeper engagement (higher cognitive load)
        if (context.currentSessionMinutes >= 20) {
            score -= 5
        } else if (context.currentSessionMinutes >= 10) {
            score -= 2
        }

        // Late night interventions may be more effective (less cognitive resistance)
        if (context.isLateNight) {
            score += 2
        }

        return score.coerceIn(0, 15)
    }

    /**
     * Historical Success (20 points)
     * Based on past intervention success in similar contexts
     */
    private suspend fun calculateHistoricalSuccess(context: InterventionContext): Int {
        // Get recent interventions for this app
        val recentResults = try {
            interventionRepository.getRecentResultsForApp(context.targetApp, limit = 50)
        } catch (e: Exception) {
            ErrorLogger.error(
                exception = e,
                message = "Failed to get historical results",
                context = "OpportunityDetector"
            )
            return 10 // Neutral score if data unavailable
        }

        // Not enough data yet
        if (recentResults.size < MIN_HISTORICAL_DATA) {
            return 12 // Neutral-leaning positive
        }

        // Filter for similar context (same time of day)
        val similarContextResults = recentResults.filter {
            val hourDiff = kotlin.math.abs(it.hourOfDay - context.timeOfDay)
            hourDiff <= 2 || hourDiff >= 22 // Within 2 hours
        }

        // If no similar context data, use overall stats
        val relevantResults = if (similarContextResults.size >= 5) {
            similarContextResults
        } else {
            recentResults
        }

        // Calculate go-back rate (success metric)
        val goBackCount = relevantResults.count { it.userChoice == UserChoice.GO_BACK }
        val successRate = if (relevantResults.isNotEmpty()) {
            (goBackCount.toDouble() / relevantResults.size) * 100
        } else {
            50.0 // Neutral
        }

        // Convert success rate to 0-20 points
        return when {
            successRate >= 60 -> 20  // Excellent success
            successRate >= 50 -> 17  // Good success
            successRate >= 40 -> 14  // Moderate success
            successRate >= 30 -> 10  // Lower success
            else -> 5                // Poor success
        }
    }

    /**
     * User State (20 points)
     * Based on positive state indicators like streaks, goals met
     */
    private suspend fun calculateUserState(context: InterventionContext): Int {
        var score = 10 // Start with neutral

        // Check if user is on a streak (from context)
        val currentStreak = context.streakDays
        when {
            currentStreak >= 7 -> score += 5    // Week+ streak
            currentStreak >= 3 -> score += 3    // 3+ day streak
        }

        // Check if user is improving (using context data)
        if (context.totalUsageToday < context.totalUsageYesterday && context.totalUsageYesterday > 0) {
            score += 3
        }

        // Check if user is under their weekly average
        if (context.totalUsageToday < context.weeklyAverage && context.weeklyAverage > 0) {
            score += 2
        }

        // Add points if over goal (user needs intervention)
        if (context.isOverGoal) {
            score += 3
        }

        // Bonus for extended streaks (2+ weeks)
        if (currentStreak >= 14) {
            score += 2
        }

        return score.coerceIn(0, 20)
    }

    /**
     * Behavioral Cues (15 points)
     * Phase 3: Immediate behavioral signals for intervention readiness
     */
    private fun calculateBehavioralCues(context: InterventionContext): Int {
        var score = 0

        // High-priority intervention signals
        if (context.compulsiveBehaviorDetected) score += 15  // Immediate intervention needed
        if (context.rapidAppSwitching) score += 10           // User is distracted/anxious
        if (context.unusualUsageTime) score += 8             // Breaking normal patterns

        // Environmental factors
        if (context.isLongScreenSession) score += 6          // 45+ min continuous
        if (context.isExcessiveUnlocking) score += 5         // Excessive phone checking

        return score.coerceIn(0, 15)
    }

    // ========== Helper functions for factor descriptions ==========

    private fun getTimeDescription(hour: Int, dayOfWeek: Int): String {
        val timeOfDayName = when (hour) {
            in 0..2 -> "Late Night"
            in 3..5 -> "Early Morning"
            in 6..9 -> "Morning"
            in 10..16 -> "Mid-Day"
            in 17..21 -> "Evening"
            else -> "Night"
        }

        val dayName = when (dayOfWeek) {
            Calendar.SATURDAY, Calendar.SUNDAY -> "Weekend"
            else -> "Weekday"
        }

        return "$timeOfDayName on $dayName"
    }

    private fun getSessionPatternDescription(context: InterventionContext): String {
        return when {
            context.quickReopenAttempt -> "Quick Reopen"
            context.sessionCount == 1 -> "First Session"
            context.currentSessionMinutes >= 30 -> "Extended (>30min)"
            context.currentSessionMinutes >= 15 -> "Extended (>15min)"
            context.currentSessionMinutes >= 5 -> "Moderate (5-15min)"
            else -> "Short (<5min)"
        }
    }

    private fun getCognitiveLoadDescription(context: InterventionContext): String {
        return when {
            context.quickReopenAttempt -> "Lower (compulsive)"
            context.currentSessionMinutes >= 20 -> "Higher (deep engagement)"
            else -> "Moderate"
        }
    }

    private fun getHistoricalSuccessDescription(score: Int): String {
        return when (score) {
            20 -> "Excellent success rate"
            17 -> "Good success rate"
            14 -> "Moderate success rate"
            10 -> "Lower success rate"
            5 -> "Poor success rate"
            else -> "Neutral (insufficient data)"
        }
    }

    private fun getUserStateDescription(context: InterventionContext): String {
        val parts = mutableListOf<String>()
        if (context.isOverGoal) parts.add("Over goal")
        if (context.isLateNight) parts.add("Late night")
        return if (parts.isNotEmpty()) parts.joinToString(", ") else "Normal state"
    }

    private fun getBehavioralCuesDescription(context: InterventionContext): String {
        val cues = mutableListOf<String>()
        if (context.compulsiveBehaviorDetected) cues.add("Compulsive")
        if (context.rapidAppSwitching) cues.add("Rapid switching")
        if (context.unusualUsageTime) cues.add("Unusual time")
        if (context.isLongScreenSession) cues.add("Long screen")
        if (context.isExcessiveUnlocking) cues.add("Excessive unlocks")
        return if (cues.isNotEmpty()) cues.joinToString(", ") else "Normal behavior"
    }

    /**
     * Clear cached opportunity detection
     * Call when significant context changes
     */
    fun clearCache() {
        cachedDetection = null
        cacheTimestamp = 0L
        cachedApp = null
        ErrorLogger.debug(
            message = "Opportunity detection cache cleared",
            context = "OpportunityDetector"
        )
    }
}
