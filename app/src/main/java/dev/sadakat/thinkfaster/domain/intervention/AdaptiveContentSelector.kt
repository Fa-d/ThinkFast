package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.local.database.dao.InterventionResultDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4: Adaptive Content Selector
 *
 * Uses Thompson Sampling to intelligently select intervention content types
 * that maximize user engagement. Learns from each intervention to improve
 * content selection over time.
 *
 * Phase 4 Expansion: Also supports timing and frequency optimization
 *
 * Key Features:
 * - Reinforcement learning with Thompson Sampling
 * - Automatic exploration vs exploitation
 * - Context-aware content filtering
 * - Gradual learning from zero data
 * - Reward feedback integration
 * - Timing optimization (when to intervene)
 * - Frequency optimization (how often to intervene)
 *
 * Content Selection Strategy:
 * 1. Apply context-based filters (time of day, persona, opportunity)
 * 2. Use Thompson Sampling to select among eligible content types
 * 3. Return selected content type ID
 * 4. Later: Update with reward based on outcome
 *
 * Timing Strategy:
 * 1. Learn which hours yield best outcomes
 * 2. Recommend delays if current time is suboptimal
 *
 * Frequency Strategy:
 * 1. Learn optimal intervention rate per user
 * 2. Adjust cooldowns based on recent effectiveness
 *
 * Usage:
 * ```
 * // Select content type
 * val selection = selector.selectContentType(context, persona)
 *
 * // Get timing recommendation
 * val timing = selector.recommendTiming(currentHour, context)
 *
 * // Get frequency adjustment
 * val frequencyMultiplier = selector.getFrequencyMultiplier()
 *
 * // After intervention completes
 * selector.recordOutcome(interventionId, reward, ...)
 * ```
 */
@Singleton
class AdaptiveContentSelector @Inject constructor(
    private val thompsonSampling: ThompsonSamplingEngine,
    private val rewardCalculator: RewardCalculator,
    private val interventionResultDao: InterventionResultDao,
    private val timingPatternLearner: TimingPatternLearner? = null,  // Phase 3: Optional timing learning
    private val contextualTimingOptimizer: ContextualTimingOptimizer? = null  // Phase 4: Timing optimization
) {

    companion object {
        // Tracking map: interventionId -> selected armId
        private val interventionArmMap = mutableMapOf<Long, String>()

        // Phase 4: Frequency learning
        private const val PREF_RECENT_EFFECTIVENESS = "rl_recent_effectiveness"
        private const val PREF_INTERVENTIONS_COUNT = "rl_intervention_count"
        private const val MIN_DATA_FOR_FREQUENCY = 20  // Minimum interventions before adjusting frequency
    }

    /**
     * Select best content type using Thompson Sampling with context awareness
     */
    suspend fun selectContentType(
        context: InterventionContext,
        persona: dev.sadakat.thinkfaster.domain.model.UserPersona? = null,
        opportunity: OpportunityDetection? = null
    ): ContentSelection = withContext(Dispatchers.IO) {

        // Apply context-based filters
        val excludedArms = getExcludedArms(context, persona, opportunity)

        // Select arm using Thompson Sampling
        val armSelection = thompsonSampling.selectArm(excludedArms)

        ContentSelection(
            contentType = armSelection.armId,
            confidence = armSelection.confidence,
            strategy = armSelection.strategy,
            reason = buildSelectionReason(armSelection, context, excludedArms)
        )
    }

    /**
     * Record intervention selection for later reward update
     */
    fun recordInterventionSelection(interventionId: Long, contentType: String) {
        interventionArmMap[interventionId] = contentType
    }

    /**
     * Update Thompson Sampling with outcome reward
     * Phase 3: Also records timing pattern for learning
     */
    suspend fun recordOutcome(
        interventionId: Long,
        userChoice: String,
        feedback: String? = null,
        sessionContinued: Boolean? = null,
        sessionDurationAfter: Long? = null,
        quickReopen: Boolean? = null,
        targetApp: String? = null,     // Phase 3: For timing learning
        hourOfDay: Int? = null,        // Phase 3: For timing learning
        isWeekend: Boolean? = null     // Phase 3: For timing learning
    ) = withContext(Dispatchers.IO) {
        // Get the arm that was used for this intervention
        val armId = interventionArmMap[interventionId] ?: return@withContext

        // Calculate reward
        val reward = rewardCalculator.calculateReward(
            userChoice = userChoice,
            feedback = feedback,
            sessionContinued = sessionContinued,
            sessionDurationAfter = sessionDurationAfter,
            quickReopen = quickReopen
        )

        // Update Thompson Sampling
        thompsonSampling.updateArm(armId, reward)

        // Phase 3: Record timing pattern for learning
        if (timingPatternLearner != null && targetApp != null && hourOfDay != null && isWeekend != null) {
            val wasSuccessful = rewardCalculator.isSuccessfulOutcome(userChoice, feedback)
            timingPatternLearner.recordTimingOutcome(hourOfDay, wasSuccessful, targetApp, isWeekend)
        }

        // Clean up tracking
        interventionArmMap.remove(interventionId)
    }

    /**
     * Get content type effectiveness summary
     */
    suspend fun getContentEffectiveness(): List<ContentEffectiveness> = withContext(Dispatchers.IO) {
        val armStats = thompsonSampling.getAllArmStats()

        armStats.map { stats ->
            ContentEffectiveness(
                contentType = stats.armId,
                displayName = getDisplayName(stats.armId),
                totalShown = stats.totalPulls,
                estimatedSuccessRate = stats.estimatedSuccessRate,
                uncertainty = stats.uncertainty,
                confidenceInterval = stats.getConfidenceInterval()
            )
        }.sortedByDescending { it.estimatedSuccessRate }
    }

    /**
     * Check if TS has enough data for reliable recommendations
     */
    suspend fun hasSufficientData(): Boolean {
        return thompsonSampling.hasSufficientData()
    }

    /**
     * Reset learning (for testing or fresh start)
     */
    suspend fun resetLearning() {
        thompsonSampling.resetAllArms()
        interventionArmMap.clear()
    }

    // ========== PHASE 4: TIMING & FREQUENCY OPTIMIZATION ==========

    /**
     * Phase 4: Recommend optimal intervention timing
     * Uses contextual timing optimizer to suggest when to intervene
     */
    suspend fun recommendTiming(
        targetApp: String,
        currentHour: Int,
        isWeekend: Boolean
    ): TimingRecommendation? = withContext(Dispatchers.IO) {
        contextualTimingOptimizer?.getOptimalTiming(targetApp, currentHour, isWeekend)
    }

    /**
     * Phase 4: Get frequency multiplier based on recent effectiveness
     * Returns a multiplier for cooldown periods
     * - <1.0 = increase frequency (shorter cooldowns)
     * - 1.0 = normal frequency
     * - >1.0 = decrease frequency (longer cooldowns)
     */
    suspend fun getFrequencyMultiplier(): Float = withContext(Dispatchers.IO) {
        // Check if we have sufficient data
        val interventionCount = getInterventionCount()
        if (interventionCount < MIN_DATA_FOR_FREQUENCY) {
            return@withContext 1.0f  // Normal frequency until we have data
        }

        // Get recent effectiveness from Thompson Sampling
        val recentEffectiveness = calculateRecentEffectiveness()

        // Adjust frequency based on effectiveness
        when {
            recentEffectiveness >= 0.60f -> 0.8f   // High success: intervene more (20% shorter cooldowns)
            recentEffectiveness >= 0.45f -> 1.0f   // Moderate success: normal frequency
            recentEffectiveness >= 0.30f -> 1.3f   // Low success: intervene less (30% longer cooldowns)
            else -> 1.5f                           // Very low success: reduce significantly (50% longer)
        }
    }

    /**
     * Phase 4: Calculate recent effectiveness across all arms
     */
    private suspend fun calculateRecentEffectiveness(): Float = withContext(Dispatchers.IO) {
        val armStats = thompsonSampling.getAllArmStats()
        if (armStats.isEmpty()) return@withContext 0.5f

        // Calculate weighted average success rate
        val totalPulls = armStats.sumOf { it.totalPulls }
        if (totalPulls == 0) return@withContext 0.5f

        val weightedSum = armStats.sumOf { (it.estimatedSuccessRate * it.totalPulls).toDouble() }
        (weightedSum / totalPulls.toDouble()).toFloat()
    }

    /**
     * Phase 4: Get total intervention count
     */
    private suspend fun getInterventionCount(): Int = withContext(Dispatchers.IO) {
        val armStats = thompsonSampling.getAllArmStats()
        armStats.sumOf { it.totalPulls }
    }

    /**
     * Phase 4: Get timing effectiveness summary
     * Shows which hours work best for interventions
     */
    suspend fun getTimingEffectiveness(): Map<Int, Float>? = withContext(Dispatchers.IO) {
        timingPatternLearner?.getEffectivenessSummary()
    }

    /**
     * Phase 4: Check if timing has reliable data
     */
    suspend fun hasReliableTimingData(): Boolean = withContext(Dispatchers.IO) {
        timingPatternLearner?.hasReliableData() ?: false
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Get arms to exclude based on context
     */
    private fun getExcludedArms(
        context: InterventionContext,
        persona: dev.sadakat.thinkfaster.domain.model.UserPersona?,
        opportunity: OpportunityDetection?
    ): Set<String> {
        val excluded = mutableSetOf<String>()

        // Time-based exclusions
        if (context.isLateNight) {
            // Avoid energetic content at night
            excluded.add(ThompsonSamplingEngine.ARM_BREATHING)  // Too stimulating
            excluded.add(ThompsonSamplingEngine.ARM_GAMIFICATION)  // Keep games for daytime
        }

        // Early morning - avoid heavy content
        if (context.timeOfDay in 3..5) {
            excluded.add(ThompsonSamplingEngine.ARM_USAGE_STATS)  // Too analytical
            excluded.add(ThompsonSamplingEngine.ARM_EMOTIONAL)  // Too heavy
        }

        // Persona-based exclusions
        when (persona) {
            dev.sadakat.thinkfaster.domain.model.UserPersona.PROBLEMATIC_PATTERN_USER -> {
                // Focus on reflection and emotional awareness
                excluded.add(ThompsonSamplingEngine.ARM_QUOTE)
                excluded.add(ThompsonSamplingEngine.ARM_GAMIFICATION)
            }
            dev.sadakat.thinkfaster.domain.model.UserPersona.CASUAL_USER -> {
                // Lighter interventions
                excluded.add(ThompsonSamplingEngine.ARM_EMOTIONAL)
            }
            dev.sadakat.thinkfaster.domain.model.UserPersona.NEW_USER -> {
                // Gentle onboarding
                excluded.add(ThompsonSamplingEngine.ARM_EMOTIONAL)
                excluded.add(ThompsonSamplingEngine.ARM_USAGE_STATS)  // Not enough data yet
            }
            else -> {}
        }

        // Session pattern exclusions
        if (context.sessionCount == 1) {
            // First session - avoid stats
            excluded.add(ThompsonSamplingEngine.ARM_USAGE_STATS)
        }

        if (context.quickReopenAttempt) {
            // Quick reopen - use impactful content
            excluded.add(ThompsonSamplingEngine.ARM_QUOTE)
            excluded.add(ThompsonSamplingEngine.ARM_GAMIFICATION)
        }

        // Opportunity-based exclusions
        opportunity?.let {
            if (it.level == dev.sadakat.thinkfaster.domain.model.OpportunityLevel.POOR) {
                // Low opportunity - use lighter touch
                excluded.add(ThompsonSamplingEngine.ARM_EMOTIONAL)
            }
        }

        return excluded
    }

    /**
     * Build human-readable selection reason
     */
    private fun buildSelectionReason(
        selection: ArmSelection,
        context: InterventionContext,
        excludedArms: Set<String>
    ): String {
        val parts = mutableListOf<String>()

        parts.add("TS selected ${getDisplayName(selection.armId)}")

        if (selection.confidence >= 0.75f) {
            parts.add("high confidence")
        } else if (selection.confidence >= 0.50f) {
            parts.add("medium confidence")
        } else {
            parts.add("exploring")
        }

        if (excludedArms.isNotEmpty()) {
            parts.add("${excludedArms.size} filtered by context")
        }

        return parts.joinToString(", ")
    }

    /**
     * Get display name for content type
     */
    private fun getDisplayName(armId: String): String {
        return when (armId) {
            ThompsonSamplingEngine.ARM_REFLECTION -> "Reflection"
            ThompsonSamplingEngine.ARM_TIME_ALTERNATIVE -> "Time Alternative"
            ThompsonSamplingEngine.ARM_BREATHING -> "Breathing"
            ThompsonSamplingEngine.ARM_USAGE_STATS -> "Usage Stats"
            ThompsonSamplingEngine.ARM_EMOTIONAL -> "Emotional Appeal"
            ThompsonSamplingEngine.ARM_QUOTE -> "Quote"
            ThompsonSamplingEngine.ARM_GAMIFICATION -> "Gamification"
            ThompsonSamplingEngine.ARM_ACTIVITY -> "Activity Suggestion"
            else -> armId
        }
    }
}

/**
 * Phase 4: Content selection result
 */
data class ContentSelection(
    val contentType: String,
    val confidence: Float,
    val strategy: String,
    val reason: String
)

/**
 * Phase 4: Content effectiveness summary
 */
data class ContentEffectiveness(
    val contentType: String,
    val displayName: String,
    val totalShown: Int,
    val estimatedSuccessRate: Float,
    val uncertainty: Float,
    val confidenceInterval: Pair<Float, Float>
) {
    fun formatEffectiveness(): String {
        val ci = confidenceInterval
        return "$displayName: ${(estimatedSuccessRate * 100).toInt()}% " +
               "(${(ci.first * 100).toInt()}-${(ci.second * 100).toInt()}%) " +
               "n=$totalShown"
    }
}
