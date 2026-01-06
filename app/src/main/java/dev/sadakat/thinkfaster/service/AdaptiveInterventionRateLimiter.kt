package dev.sadakat.thinkfaster.service

import android.content.Context
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.domain.intervention.InterventionContext
import dev.sadakat.thinkfaster.domain.intervention.OpportunityDetection
import dev.sadakat.thinkfaster.domain.intervention.OpportunityDetector
import dev.sadakat.thinkfaster.domain.intervention.PersonaDetector
import dev.sadakat.thinkfaster.domain.model.*
import dev.sadakat.thinkfaster.util.ErrorLogger
import kotlinx.coroutines.Dispatchers
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
    private val opportunityDetector: OpportunityDetector
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

        // Step 3: Basic rate limit checks (existing logic)
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

        // If basic checks fail, return with JITAI context for analytics
        if (!basicResult.allowed) {
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

        // Step 4: Apply persona-specific frequency rules
        // Extended "daytime" window to 6 AM - 11 PM (from 6 AM - 8 PM)
        // This accommodates evening usage while avoiding late-night/early-morning hours
        val personaAllowed = checkPersonaFrequencyRules(
            persona = persona,
            opportunityScore = opportunityScore,
            opportunityLevel = opportunityLevel,
            isDaytime = interventionContext.timeOfDay in 6..23
        )

        if (!personaAllowed) {
            val reason = generatePersonaBlockReason(
                persona = persona,
                opportunityScore = opportunityScore,
                opportunityLevel = opportunityLevel
            )

            // Calculate cooldown based on persona
            val personaCooldownMs = calculatePersonaCooldown(persona)

            return@withContext AdaptiveRateLimitResult(
                allowed = false,
                reason = reason,
                cooldownRemainingMs = personaCooldownMs,
                persona = persona,
                personaConfidence = personaConfidence,
                opportunityScore = opportunityScore,
                opportunityLevel = opportunityLevel,
                decision = InterventionDecision.SKIP_INTERVENTION,
                decisionSource = "PERSONA_FREQUENCY"
            )
        }

        // Step 5: Apply JITAI decision filter
        val finalDecision = when (jitaiDecision) {
            InterventionDecision.SKIP_INTERVENTION -> {
                AdaptiveRateLimitResult(
                    allowed = false,
                    reason = "Low opportunity score ($opportunityScore/100)",
                    cooldownRemainingMs = 5 * 60 * 1000L,  // 5 minutes
                    persona = persona,
                    personaConfidence = personaConfidence,
                    opportunityScore = opportunityScore,
                    opportunityLevel = opportunityLevel,
                    decision = jitaiDecision,
                    decisionSource = "OPPORTUNITY_DETECTION"
                )
            }
            else -> {
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
