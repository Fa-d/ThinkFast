package dev.sadakat.thinkfaster.service

import android.content.Context
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.domain.intervention.BlockingReason
import dev.sadakat.thinkfaster.domain.intervention.BurdenLevel
import dev.sadakat.thinkfaster.domain.intervention.DecisionLogger
import dev.sadakat.thinkfaster.domain.intervention.InterventionBurdenTracker
import dev.sadakat.thinkfaster.domain.intervention.InterventionDecisionExplanation
import dev.sadakat.thinkfaster.domain.intervention.InterventionOutcome
import dev.sadakat.thinkfaster.domain.intervention.InterventionType
import dev.sadakat.thinkfaster.domain.intervention.OpportunityDetector
import dev.sadakat.thinkfaster.domain.intervention.PersonaDetector
import dev.sadakat.thinkfaster.domain.intervention.calculateBurdenLevel
import dev.sadakat.thinkfaster.domain.intervention.calculateBurdenScore
import dev.sadakat.thinkfaster.domain.intervention.isReliable
import dev.sadakat.thinkfaster.domain.intervention.getRecommendedCooldownMultiplier
import dev.sadakat.thinkfaster.domain.intervention.InterventionContext
import dev.sadakat.thinkfaster.domain.model.ConfidenceLevel
import dev.sadakat.thinkfaster.domain.model.InterventionDecision
import dev.sadakat.thinkfaster.domain.model.InterventionFeedback
import dev.sadakat.thinkfaster.domain.model.InterventionFrequency
import dev.sadakat.thinkfaster.domain.model.OpportunityLevel
import dev.sadakat.thinkfaster.domain.model.UserPersona
import dev.sadakat.thinkfaster.util.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Enhanced rate limit result with JITAI decision context
 * Phase 2 JITAI: Rich context for intervention decisions
 */
data class AdaptiveRateLimitResult(
    val allowed: Boolean,
    val reason: String,
    val cooldownRemainingMs: Long = 0,
    // JITAI context
    val persona: UserPersona? = null,
    val personaConfidence: ConfidenceLevel? = null,
    val opportunityScore: Int? = null,
    val opportunityLevel: OpportunityLevel? = null,
    val decision: InterventionDecision? = null,
    val decisionSource: String = "BASIC_RATE_LIMIT"
)

/**
 * Adaptive Intervention Rate Limiter
 * Phase 2 JITAI: Smart rate limiting with persona and opportunity awareness
 *
 * Enhances the base InterventionRateLimiter with:
 * 1. Persona detection (always runs for analytics)
 * 2. Opportunity score calculation (always runs for analytics)
 * 3. Basic rate limit checks (existing logic)
 * 4. Persona-specific frequency adjustments
 * 5. Rich decision context for analytics
 *
 * Frequency by Persona:
 * - PROBLEMATIC_PATTERN_USER: MINIMAL - Only EXCELLENT opportunities
 * - HEAVY_COMPULSIVE_USER: CONSERVATIVE - Only GOOD or better
 * - MODERATE_BALANCED_USER: BALANCED - Anything except POOR
 * - HEAVY_BINGE_USER: MODERATE - Score >= 25
 * - CASUAL_USER: ADAPTIVE - Context-dependent
 * - NEW_USER: ONBOARDING - Moderate+, daytime only
 *
 * Cooldown Adjustments:
 * - Applies persona-specific multiplier (0.5x to 2.0x)
 * - Adjusts based on feedback (HELPFUL = -10%, DISRUPTIVE = +20%)
 */
class AdaptiveInterventionRateLimiter(
    private val context: Context,
    private val interventionPreferences: InterventionPreferences,
    private val baseRateLimiter: InterventionRateLimiter,
    private val personaDetector: PersonaDetector,
    private val opportunityDetector: OpportunityDetector,
    private val decisionLogger: DecisionLogger,
    private val burdenTracker: InterventionBurdenTracker
) {

    companion object {
        // Persona-specific cooldown multipliers
        private const val PROBLEMATIC_MULTIPLIER = 2.0f    // 2x cooldown for problematic users
        private const val HEAVY_COMPULSIVE_MULTIPLIER = 1.5f  // 1.5x cooldown
        private const val BINGE_MULTIPLIER = 1.0f           // Normal cooldown
        private const val MODERATE_MULTIPLIER = 1.0f        // Normal cooldown
        private const val CASUAL_MULTIPLIER = 0.7f          // 0.7x cooldown (more frequent)
        private const val NEW_USER_MULTIPLIER = 0.5f        // 0.5x cooldown (gentle onboarding)

        // Feedback adjustments
        private const val HELPFUL_COOLDOWN_REDUCTION = 0.9f    // -10% cooldown
        private const val DISRUPTIVE_COOLDOWN_INCREASE = 1.2f  // +20% cooldown
    }

    /**
     * Check if we can show an intervention with JITAI intelligence
     *
     * IMPORTANT: This ALWAYS runs persona and opportunity detection, even when
     * basic rate limit checks fail. This provides rich context for analytics and
     * better understanding of user behavior.
     *
     * @param interventionContext Current intervention context
     * @param interventionType Type of intervention (REMINDER or TIMER)
     * @param sessionDurationMs Duration of current session
     * @param forceRefreshPersona Force persona re-detection
     * @return AdaptiveRateLimitResult with rich JITAI context (always includes persona and opportunity)
     */
    suspend fun canShowIntervention(
        interventionContext: InterventionContext,
        interventionType: dev.sadakat.thinkfaster.domain.intervention.InterventionType,
        sessionDurationMs: Long,
        forceRefreshPersona: Boolean = false
    ): AdaptiveRateLimitResult = withContext(Dispatchers.IO) {

        // Step 1: Always detect user persona (for analytics and better context)
        val detectedPersona = personaDetector.detectPersona(forceRefreshPersona)
        val persona = detectedPersona.persona
        val personaConfidence = detectedPersona.confidence

        // Step 2: Always calculate opportunity score (for analytics and better context)
        val opportunityDetection = opportunityDetector.detectOpportunity(
            context = interventionContext
        )
        val opportunityScore = opportunityDetection.score
        val opportunityLevel = opportunityDetection.level
        val jitaiDecision = opportunityDetection.decision

        // Step 3: Get burden metrics for cooldown adjustment
        val burdenMetrics = burdenTracker.calculateCurrentBurdenMetrics()
        val burdenCooldownMultiplier = if (burdenMetrics.isReliable()) {
            burdenTracker.getRecommendedCooldownAdjustment()
        } else {
            1.0f  // No adjustment if insufficient data
        }

        // Step 4: Basic rate limit checks (existing logic)
        val basicResult = baseRateLimiter.canShowIntervention(
            interventionType = when (interventionType) {
                dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER ->
                    InterventionRateLimiter.InterventionType.REMINDER
                dev.sadakat.thinkfaster.domain.intervention.InterventionType.TIMER ->
                    InterventionRateLimiter.InterventionType.TIMER
                dev.sadakat.thinkfaster.domain.intervention.InterventionType.CUSTOM ->
                    InterventionRateLimiter.InterventionType.REMINDER  // Treat custom as reminder
            },
            sessionDurationMs = sessionDurationMs
        )

        // If basic checks fail, log decision and return with JITAI context
        if (!basicResult.allowed) {
            val explanation = buildDecisionExplanation(
                targetApp = interventionContext.targetApp,
                decision = InterventionOutcome.SKIP_INTERVENTION,
                blockingReason = BlockingReason.BASIC_RATE_LIMIT,
                opportunityScore = opportunityScore,
                opportunityLevel = opportunityLevel,
                opportunityBreakdown = opportunityDetection.breakdown.toMap(),
                persona = persona,
                personaConfidence = personaConfidence,
                passedBasicRateLimit = false,
                timeSinceLastIntervention = basicResult.cooldownRemainingMs,
                passedPersonaFrequency = true,
                passedJitaiFilter = true,
                jitaiDecision = jitaiDecision.name,
                burdenLevel = burdenMetrics.calculateBurdenLevel(),
                burdenScore = burdenMetrics.calculateBurdenScore(),
                burdenMitigationApplied = false,
                burdenCooldownMultiplier = null,
                interventionContext = interventionContext,
                interventionType = interventionType
            )
            logDecisionAsync(explanation)

            return@withContext AdaptiveRateLimitResult(
                allowed = false,
                reason = basicResult.reason,
                cooldownRemainingMs = basicResult.cooldownRemainingMs,
                persona = persona,
                personaConfidence = personaConfidence,
                opportunityScore = opportunityScore,
                opportunityLevel = opportunityLevel,
                decision = jitaiDecision,
                decisionSource = "BASIC_RATE_LIMIT"
            )
        }

        // Step 5: Apply persona-specific frequency rules
        // Extended "daytime" window to 6 AM - 11 PM (from 6 AM - 8 PM)
        // This accommodates evening usage while avoiding late-night/early-morning hours
        val personaAllowed = checkPersonaFrequencyRules(
            persona = persona,
            opportunityScore = opportunityScore,
            opportunityLevel = opportunityLevel,
            isDaytime = interventionContext.timeOfDay in 6..23
        )

        if (!personaAllowed) {
            val personaRule = getPersonaFrequencyRule(persona)
            val reason = generatePersonaBlockReason(
                persona = persona,
                opportunityScore = opportunityScore,
                opportunityLevel = opportunityLevel
            )

            // Calculate cooldown based on persona AND burden
            val baseCooldownMs = calculatePersonaCooldown(persona)
            val adjustedCooldownMs = (baseCooldownMs * burdenCooldownMultiplier).toLong()

            val explanation = buildDecisionExplanation(
                targetApp = interventionContext.targetApp,
                decision = InterventionOutcome.SKIP_INTERVENTION,
                blockingReason = BlockingReason.PERSONA_FREQUENCY_LIMIT,
                opportunityScore = opportunityScore,
                opportunityLevel = opportunityLevel,
                opportunityBreakdown = opportunityDetection.breakdown.toMap(),
                persona = persona,
                personaConfidence = personaConfidence,
                passedBasicRateLimit = true,
                timeSinceLastIntervention = null,
                passedPersonaFrequency = false,
                personaFrequencyRule = personaRule,
                passedJitaiFilter = true,
                jitaiDecision = jitaiDecision.name,
                burdenLevel = burdenMetrics.calculateBurdenLevel(),
                burdenScore = burdenMetrics.calculateBurdenScore(),
                burdenMitigationApplied = burdenCooldownMultiplier > 1.0f,
                burdenCooldownMultiplier = burdenCooldownMultiplier,
                interventionContext = interventionContext,
                interventionType = interventionType
            )
            logDecisionAsync(explanation)

            return@withContext AdaptiveRateLimitResult(
                allowed = false,
                reason = reason,
                cooldownRemainingMs = adjustedCooldownMs,
                persona = persona,
                personaConfidence = personaConfidence,
                opportunityScore = opportunityScore,
                opportunityLevel = opportunityLevel,
                decision = dev.sadakat.thinkfaster.domain.model.InterventionDecision.SKIP_INTERVENTION,
                decisionSource = "PERSONA_FREQUENCY"
            )
        }

        // Step 6: Apply JITAI decision filter
        val finalDecision = when (jitaiDecision) {
            dev.sadakat.thinkfaster.domain.model.InterventionDecision.SKIP_INTERVENTION -> {
                val adjustedCooldownMs = (5 * 60 * 1000L * burdenCooldownMultiplier).toLong()

                val explanation = buildDecisionExplanation(
                    targetApp = interventionContext.targetApp,
                    decision = InterventionOutcome.SKIP_INTERVENTION,
                    blockingReason = BlockingReason.JITAI_POOR_OPPORTUNITY,
                    opportunityScore = opportunityScore,
                    opportunityLevel = opportunityLevel,
                    opportunityBreakdown = opportunityDetection.breakdown.toMap(),
                    persona = persona,
                    personaConfidence = personaConfidence,
                    passedBasicRateLimit = true,
                    timeSinceLastIntervention = null,
                    passedPersonaFrequency = true,
                    passedJitaiFilter = false,
                    jitaiDecision = jitaiDecision.name,
                    burdenLevel = burdenMetrics.calculateBurdenLevel(),
                    burdenScore = burdenMetrics.calculateBurdenScore(),
                    burdenMitigationApplied = burdenCooldownMultiplier > 1.0f,
                    burdenCooldownMultiplier = burdenCooldownMultiplier,
                    interventionContext = interventionContext,
                    interventionType = interventionType
                )
                logDecisionAsync(explanation)

                AdaptiveRateLimitResult(
                    allowed = false,
                    reason = "Low opportunity score ($opportunityScore/100)",
                    cooldownRemainingMs = adjustedCooldownMs,
                    persona = persona,
                    personaConfidence = personaConfidence,
                    opportunityScore = opportunityScore,
                    opportunityLevel = opportunityLevel,
                    decision = jitaiDecision,
                    decisionSource = "OPPORTUNITY_DETECTION"
                )
            }
            else -> {
                // All checks passed - log SHOW decision
                val explanation = buildDecisionExplanation(
                    targetApp = interventionContext.targetApp,
                    decision = InterventionOutcome.SHOW_INTERVENTION,
                    blockingReason = null,
                    opportunityScore = opportunityScore,
                    opportunityLevel = opportunityLevel,
                    opportunityBreakdown = opportunityDetection.breakdown.toMap(),
                    persona = persona,
                    personaConfidence = personaConfidence,
                    passedBasicRateLimit = true,
                    timeSinceLastIntervention = null,
                    passedPersonaFrequency = true,
                    passedJitaiFilter = true,
                    jitaiDecision = jitaiDecision.name,
                    burdenLevel = burdenMetrics.calculateBurdenLevel(),
                    burdenScore = burdenMetrics.calculateBurdenScore(),
                    burdenMitigationApplied = false,
                    burdenCooldownMultiplier = null,
                    interventionContext = interventionContext,
                    interventionType = interventionType
                )
                logDecisionAsync(explanation)

                // All checks passed
                AdaptiveRateLimitResult(
                    allowed = true,
                    reason = generateSuccessReason(
                        persona = persona,
                        opportunityScore = opportunityScore,
                        opportunityLevel = opportunityLevel,
                        jitaiDecision = jitaiDecision
                    ),
                    persona = persona,
                    personaConfidence = personaConfidence,
                    opportunityScore = opportunityScore,
                    opportunityLevel = opportunityLevel,
                    decision = jitaiDecision,
                    decisionSource = "JITAI_APPROVED"
                )
            }
        }

        finalDecision
    }

    /**
     * Build InterventionDecisionExplanation for decision logging
     * Phase 1: Captures complete decision rationale
     */
    private fun buildDecisionExplanation(
        targetApp: String,
        decision: InterventionOutcome,
        blockingReason: BlockingReason?,
        opportunityScore: Int,
        opportunityLevel: OpportunityLevel,
        opportunityBreakdown: Map<String, Int>,
        persona: UserPersona,
        personaConfidence: ConfidenceLevel,
        passedBasicRateLimit: Boolean,
        timeSinceLastIntervention: Long?,
        passedPersonaFrequency: Boolean,
        passedJitaiFilter: Boolean,
        jitaiDecision: String,
        burdenLevel: BurdenLevel?,
        burdenScore: Int?,
        burdenMitigationApplied: Boolean,
        burdenCooldownMultiplier: Float?,
        interventionContext: InterventionContext,
        interventionType: dev.sadakat.thinkfaster.domain.intervention.InterventionType,
        personaFrequencyRule: String? = null
    ): InterventionDecisionExplanation {
        // InterventionDecisionExplanation now uses model types directly, no mapping needed

        // Build context snapshot
        val contextSnapshot = mapOf(
            "targetApp" to interventionContext.targetApp,
            "currentSessionMinutes" to interventionContext.currentSessionMinutes,
            "sessionCount" to interventionContext.sessionCount,
            "totalUsageToday" to interventionContext.totalUsageToday,
            "weeklyAverage" to interventionContext.weeklyAverage,
            "goalMinutes" to (interventionContext.goalMinutes ?: 0),
            "streakDays" to interventionContext.streakDays,
            "isWeekend" to interventionContext.isWeekend,
            "isLateNight" to (interventionContext.timeOfDay < 6 || interventionContext.timeOfDay >= 22),
            "quickReopen" to interventionContext.quickReopenAttempt,
            "interventionType" to interventionType.name
        )

        val explanation = when (decision) {
            InterventionOutcome.SHOW_INTERVENTION -> {
                "Showing intervention: $opportunityLevel opportunity ($opportunityScore/100) for $persona user"
            }
            InterventionOutcome.SKIP_INTERVENTION -> {
                "Skipping intervention: ${blockingReason?.name} (opportunity: $opportunityScore/100, persona: $persona)"
            }
        }

        val detailedExplanation = buildString {
            appendLine("=== Intervention Decision ===")
            appendLine("Decision: ${decision.name}")
            appendLine("Target App: $targetApp")
            appendLine("Reason: ${blockingReason?.name ?: "N/A"}")
            appendLine()
            appendLine("Persona: $persona ($personaConfidence)")
            appendLine("Opportunity: $opportunityLevel ($opportunityScore/100)")
            appendLine("JITAI Decision: $jitaiDecision")
            appendLine("Basic Rate Limit: ${if (passedBasicRateLimit) "PASS" else "FAIL"}")
            appendLine("Persona Frequency: ${if (passedPersonaFrequency) "PASS" else "FAIL"}")
            appendLine("JITAI Filter: ${if (passedJitaiFilter) "PASS" else "FAIL"}")
            if (burdenLevel != null) {
                appendLine("Burden Level: $burdenLevel")
                appendLine("Burden Score: $burdenScore")
                if (burdenMitigationApplied) {
                    appendLine("Burden Mitigation: Applied (${burdenCooldownMultiplier}x cooldown)")
                }
            }
        }

        return InterventionDecisionExplanation(
            timestamp = System.currentTimeMillis(),
            targetApp = targetApp,
            decision = decision,
            blockingReason = blockingReason,
            opportunityScore = opportunityScore,
            opportunityLevel = opportunityLevel,
            opportunityBreakdown = opportunityBreakdown,
            personaDetected = persona,
            personaConfidence = personaConfidence,
            passedBasicRateLimit = passedBasicRateLimit,
            timeSinceLastIntervention = timeSinceLastIntervention?.div(1000), // Convert to seconds
            passedPersonaFrequency = passedPersonaFrequency,
            personaFrequencyRule = personaFrequencyRule,
            passedJitaiFilter = passedJitaiFilter,
            jitaiDecision = jitaiDecision,
            burdenLevel = burdenLevel,
            burdenScore = burdenScore,
            burdenMitigationApplied = burdenMitigationApplied,
            burdenCooldownMultiplier = burdenCooldownMultiplier,
            contentTypeSelected = null,  // Content selection happens later
            contentWeights = null,
            contentSelectionReason = null,
            rlPredictedReward = null,
            rlExplorationVsExploitation = null,
            contextSnapshot = contextSnapshot,
            explanation = explanation,
            detailedExplanation = detailedExplanation
        )
    }

    /**
     * Log decision asynchronously (non-blocking)
     * Phase 1: Logs every intervention decision for transparency and optimization
     */
    private fun logDecisionAsync(explanation: InterventionDecisionExplanation) {
        // Launch in a separate coroutine to avoid blocking the decision flow
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                decisionLogger.logDecision(explanation)
                ErrorLogger.info(
                    "Decision logged: ${explanation.decision.name} for ${explanation.targetApp}",
                    context = "AdaptiveInterventionRateLimiter"
                )
            } catch (e: Exception) {
                // Don't let logging failures affect intervention delivery
                ErrorLogger.error(
                    e,
                    message = "Failed to log decision",
                    context = "AdaptiveInterventionRateLimiter.logDecisionAsync"
                )
            }
        }
    }

    /**
     * Get persona frequency rule description
     */
    private fun getPersonaFrequencyRule(persona: UserPersona): String {
        return when (persona) {
            UserPersona.PROBLEMATIC_PATTERN_USER -> "MINIMAL - Only EXCELLENT opportunities"
            UserPersona.HEAVY_COMPULSIVE_USER -> "CONSERVATIVE - Only GOOD or EXCELLENT"
            UserPersona.HEAVY_BINGE_USER -> "MODERATE - Score >= 25"
            UserPersona.MODERATE_BALANCED_USER -> "BALANCED - Anything except POOR"
            UserPersona.CASUAL_USER -> "ADAPTIVE - Context-dependent"
            UserPersona.NEW_USER -> "ONBOARDING - Moderate+, daytime only"
        }
    }

    /**
     * Check persona-specific frequency rules
     * Returns true if intervention is allowed for this persona at this opportunity level
     */
    private fun checkPersonaFrequencyRules(
        persona: UserPersona,
        opportunityScore: Int,
        opportunityLevel: OpportunityLevel,
        isDaytime: Boolean
    ): Boolean {
        return when (persona.frequency) {
            // MINIMAL: Only EXCELLENT opportunities
            InterventionFrequency.MINIMAL -> {
                opportunityLevel == OpportunityLevel.EXCELLENT
            }

            // CONSERVATIVE: Only GOOD or EXCELLENT
            InterventionFrequency.CONSERVATIVE -> {
                opportunityLevel == OpportunityLevel.EXCELLENT ||
                opportunityLevel == OpportunityLevel.GOOD
            }

            // BALANCED: Anything except POOR
            InterventionFrequency.BALANCED -> {
                opportunityLevel != OpportunityLevel.POOR
            }

            // MODERATE: Score >= 25 (includes MODERATE and above)
            InterventionFrequency.MODERATE -> {
                opportunityScore >= 25
            }

            // ADAPTIVE: Context-dependent
            InterventionFrequency.ADAPTIVE -> {
                when {
                    opportunityLevel == OpportunityLevel.EXCELLENT -> true
                    opportunityLevel == OpportunityLevel.GOOD && isDaytime -> true
                    opportunityScore >= 40 -> true
                    else -> false
                }
            }

            // ONBOARDING: Moderate+, daytime only (6 AM - 11 PM)
            // Extended from 8 PM to 11 PM to accommodate evening usage while avoiding late-night disruptions
            InterventionFrequency.ONBOARDING -> {
                isDaytime && (opportunityScore >= 30)
            }
        }
    }

    /**
     * Generate explanation for why intervention was blocked by persona rules
     */
    private fun generatePersonaBlockReason(
        persona: UserPersona,
        opportunityScore: Int,
        opportunityLevel: OpportunityLevel
    ): String {
        return when (persona.frequency) {
            InterventionFrequency.MINIMAL ->
                "Problematic pattern: Only EXCELLENT opportunities allowed (score: $opportunityScore, level: $opportunityLevel)"

            InterventionFrequency.CONSERVATIVE ->
                "Heavy compulsive: Only GOOD or EXCELLENT opportunities (score: $opportunityScore, level: $opportunityLevel)"

            InterventionFrequency.BALANCED ->
                "Opportunity level too low: $opportunityLevel (score: $opportunityScore)"

            InterventionFrequency.MODERATE ->
                "Score below threshold: $opportunityScore < 25"

            InterventionFrequency.ADAPTIVE ->
                "Adaptive filtering: Current context not optimal (score: $opportunityScore)"

            InterventionFrequency.ONBOARDING ->
                "New user onboarding: Daytime, moderate+ opportunities only"
        }
    }

    /**
     * Generate success reason explaining why intervention was allowed
     */
    private fun generateSuccessReason(
        persona: UserPersona,
        opportunityScore: Int,
        opportunityLevel: OpportunityLevel,
        jitaiDecision: InterventionDecision
    ): String {
        val parts = mutableListOf<String>()

        parts.add("Persona: ${persona.name}")
        parts.add("Opportunity: $opportunityLevel ($opportunityScore/100)")
        parts.add("Decision: $jitaiDecision")

        return parts.joinToString(" | ")
    }

    /**
     * Calculate persona-specific cooldown in milliseconds
     */
    private fun calculatePersonaCooldown(persona: UserPersona): Long {
        val baseMultiplier = when (persona) {
            UserPersona.PROBLEMATIC_PATTERN_USER -> PROBLEMATIC_MULTIPLIER
            UserPersona.HEAVY_COMPULSIVE_USER -> HEAVY_COMPULSIVE_MULTIPLIER
            UserPersona.HEAVY_BINGE_USER -> BINGE_MULTIPLIER
            UserPersona.MODERATE_BALANCED_USER -> MODERATE_MULTIPLIER
            UserPersona.CASUAL_USER -> CASUAL_MULTIPLIER
            UserPersona.NEW_USER -> NEW_USER_MULTIPLIER
        }

        // Apply feedback adjustments
        val currentMultiplier = interventionPreferences.getCooldownMultiplier()
        val adjustedMultiplier = baseMultiplier * currentMultiplier

        // Base cooldown is 5 minutes
        return (5 * 60 * 1000L * adjustedMultiplier).toLong()
    }

    /**
     * Record that an intervention was shown
     * Delegates to base rate limiter
     */
    fun recordIntervention(
        interventionType: dev.sadakat.thinkfaster.domain.intervention.InterventionType
    ) {
        baseRateLimiter.recordIntervention(
            when (interventionType) {
                dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER ->
                    InterventionRateLimiter.InterventionType.REMINDER
                dev.sadakat.thinkfaster.domain.intervention.InterventionType.TIMER ->
                    InterventionRateLimiter.InterventionType.TIMER
                dev.sadakat.thinkfaster.domain.intervention.InterventionType.CUSTOM ->
                    InterventionRateLimiter.InterventionType.REMINDER  // Treat custom as reminder
            }
        )
    }

    /**
     * Adjust cooldown based on user feedback
     * HELPFUL feedback reduces cooldown by 10%
     * DISRUPTIVE feedback increases cooldown by 20%
     */
    fun adjustCooldownForFeedback(feedback: InterventionFeedback) {
        val currentMultiplier = interventionPreferences.getCooldownMultiplier()
        val newMultiplier = when (feedback) {
            InterventionFeedback.HELPFUL -> {
                val reduced = currentMultiplier * HELPFUL_COOLDOWN_REDUCTION
                reduced.coerceAtLeast(0.5f)  // Minimum 0.5x
            }
            InterventionFeedback.DISRUPTIVE -> {
                val increased = currentMultiplier * DISRUPTIVE_COOLDOWN_INCREASE
                increased.coerceAtMost(3.0f)  // Maximum 3.0x
            }
            else -> currentMultiplier
        }

        if (newMultiplier != currentMultiplier) {
            interventionPreferences.setCooldownMultiplier(newMultiplier)
            ErrorLogger.info(
                message = "Cooldown adjusted for $feedback: $currentMultiplier → $newMultiplier",
                context = "AdaptiveInterventionRateLimiter"
            )
        }
    }

    /**
     * Escalate cooldown when user repeatedly dismisses
     * Delegates to base rate limiter
     */
    fun escalateCooldown() {
        baseRateLimiter.escalateCooldown()
    }

    /**
     * Reset cooldown when user engages positively
     * Delegates to base rate limiter
     */
    fun resetCooldown() {
        baseRateLimiter.resetCooldown()
    }
}

/**
 * Extension function to convert OpportunityBreakdown to Map<String, Int>
 */
private fun dev.sadakat.thinkfaster.domain.intervention.OpportunityBreakdown.toMap(): Map<String, Int> {
    return mapOf(
        "timeReceptiveness" to timeReceptiveness,
        "sessionPattern" to sessionPattern,
        "cognitiveLoad" to cognitiveLoad,
        "historicalSuccess" to historicalSuccess,
        "userState" to userState
    )
}
