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
 * Key Features:
 * - Reinforcement learning with Thompson Sampling
 * - Automatic exploration vs exploitation
 * - Context-aware content filtering
 * - Gradual learning from zero data
 * - Reward feedback integration
 *
 * Content Selection Strategy:
 * 1. Apply context-based filters (time of day, persona, opportunity)
 * 2. Use Thompson Sampling to select among eligible content types
 * 3. Return selected content type ID
 * 4. Later: Update with reward based on outcome
 *
 * Usage:
 * ```
 * // Select content type
 * val selection = selector.selectContentType(context, persona)
 *
 * // After intervention completes
 * selector.recordOutcome(interventionId, reward)
 * ```
 */
@Singleton
class AdaptiveContentSelector @Inject constructor(
    private val thompsonSampling: ThompsonSamplingEngine,
    private val rewardCalculator: RewardCalculator,
    private val interventionResultDao: InterventionResultDao
) {

    companion object {
        // Tracking map: interventionId -> selected armId
        private val interventionArmMap = mutableMapOf<Long, String>()
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
     */
    suspend fun recordOutcome(
        interventionId: Long,
        userChoice: String,
        feedback: String? = null,
        sessionContinued: Boolean? = null,
        sessionDurationAfter: Long? = null,
        quickReopen: Boolean? = null
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
