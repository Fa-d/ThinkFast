package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.domain.model.InterventionContent
import dev.sadakat.thinkfaster.domain.model.InterventionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4: Unified Content Selector
 *
 * Orchestrates content selection between rule-based (PersonaAware) and
 * RL-based (Adaptive) selectors using RLRolloutController for A/B testing.
 *
 * Features:
 * - Automatic variant assignment (Control vs RL Treatment)
 * - Effectiveness tracking for both variants
 * - Automatic rollback if RL underperforms
 * - Seamless selector switching
 *
 * Usage:
 * ```
 * val content = unifiedSelector.selectContent(context, interventionType)
 * // Returns content from either rule-based or RL selector based on user variant
 * ```
 */
@Singleton
class UnifiedContentSelector @Inject constructor(
    private val personaAwareSelector: PersonaAwareContentSelector,
    private val adaptiveSelector: AdaptiveContentSelector,
    private val rolloutController: RLRolloutController
) {

    /**
     * Select content using appropriate selector based on user variant
     */
    suspend fun selectContent(
        context: InterventionContext,
        interventionType: InterventionType,
        effectivenessData: List<dev.sadakat.thinkfaster.domain.model.ContentEffectivenessStats> = emptyList()
    ): UnifiedContentSelection = withContext(Dispatchers.IO) {

        // Get user variant (Control or RL Treatment)
        val variant = rolloutController.getUserVariant()

        when (variant) {
            RLVariant.CONTROL -> {
                // Use rule-based persona selector
                // Enable parallel RL prediction logging for learning
                val selection = personaAwareSelector.selectContent(
                    context = context,
                    interventionType = interventionType,
                    effectivenessData = effectivenessData,
                    logRLPrediction = true  // Phase 4: Log RL predictions in parallel
                )

                UnifiedContentSelection(
                    content = selection.content,
                    contentType = selection.contentType.name,
                    variant = variant,
                    selectionReason = "${selection.selectionReason} [Control]",
                    confidence = 0.8f  // Rule-based has consistent confidence
                )
            }

            RLVariant.RL_TREATMENT -> {
                // Use RL-based adaptive selector
                val rlSelection = adaptiveSelector.selectContentType(
                    context = context,
                    persona = null,
                    opportunity = null
                )

                // Generate content using base selector
                val baseSelector = ContentSelector()
                val content = baseSelector.generateContentByType(
                    rlSelection.contentType,
                    context
                )

                // Record selection for tracking
                // Note: interventionId would be assigned later when intervention is shown
                // adaptiveSelector.recordInterventionSelection(interventionId, rlSelection.contentType)

                UnifiedContentSelection(
                    content = content,
                    contentType = rlSelection.contentType,
                    variant = variant,
                    selectionReason = "${rlSelection.reason} [RL]",
                    confidence = rlSelection.confidence
                )
            }
        }
    }

    /**
     * Record intervention outcome and update effectiveness metrics
     */
    suspend fun recordOutcome(
        interventionId: Long,
        variant: RLVariant,
        userChoice: String,
        feedback: String? = null,
        sessionContinued: Boolean? = null,
        sessionDurationAfter: Long? = null,
        quickReopen: Boolean? = null,
        targetApp: String? = null,
        hourOfDay: Int? = null,
        isWeekend: Boolean? = null
    ) = withContext(Dispatchers.IO) {

        // Calculate if intervention was successful
        val wasSuccessful = userChoice == "GO_BACK" ||
                           (userChoice == "CONTINUE" && feedback == "HELPFUL")

        // Update rollout controller effectiveness metrics
        rolloutController.recordEffectiveness(variant, wasSuccessful)

        // If RL variant, also update Thompson Sampling
        if (variant == RLVariant.RL_TREATMENT) {
            adaptiveSelector.recordOutcome(
                interventionId = interventionId,
                userChoice = userChoice,
                feedback = feedback,
                sessionContinued = sessionContinued,
                sessionDurationAfter = sessionDurationAfter,
                quickReopen = quickReopen,
                targetApp = targetApp,
                hourOfDay = hourOfDay,
                isWeekend = isWeekend
            )
        }
    }

    /**
     * Get frequency multiplier from RL (applies to both variants)
     */
    suspend fun getFrequencyMultiplier(): Float = withContext(Dispatchers.IO) {
        adaptiveSelector.getFrequencyMultiplier()
    }

    /**
     * Get timing recommendation from RL (applies to both variants)
     */
    suspend fun recommendTiming(
        targetApp: String,
        currentHour: Int,
        isWeekend: Boolean
    ): TimingRecommendation? = withContext(Dispatchers.IO) {
        adaptiveSelector.recommendTiming(targetApp, currentHour, isWeekend)
    }

    /**
     * Get rollout metrics for monitoring
     */
    suspend fun getRolloutMetrics(): RLEffectivenessMetrics = withContext(Dispatchers.IO) {
        rolloutController.getEffectivenessMetrics()
    }

    /**
     * Get content effectiveness from RL
     */
    suspend fun getContentEffectiveness(): List<ContentEffectiveness> = withContext(Dispatchers.IO) {
        adaptiveSelector.getContentEffectiveness()
    }
}

/**
 * Phase 4: Unified content selection result
 */
data class UnifiedContentSelection(
    val content: InterventionContent,
    val contentType: String,
    val variant: RLVariant,
    val selectionReason: String,
    val confidence: Float
)
