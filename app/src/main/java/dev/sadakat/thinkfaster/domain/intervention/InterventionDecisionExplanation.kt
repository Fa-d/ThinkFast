package dev.sadakat.thinkfaster.domain.intervention

/**
 * Phase 1: Intervention Decision Explanation
 *
 * Provides complete transparency into why each intervention decision was made.
 * This is critical for:
 * - Debugging and optimization
 * - Understanding algorithm behavior
 * - Building user trust
 * - Identifying improvement opportunities
 *
 * Research shows that transparency in decision logic helps evaluate,
 * replicate, and optimize JITAI design.
 */
data class InterventionDecisionExplanation(
    val timestamp: Long = System.currentTimeMillis(),
    val targetApp: String,

    // ========== DECISION OUTCOME ==========

    /**
     * Final decision: SHOW or SKIP
     */
    val decision: InterventionDecision,

    /**
     * If SKIP, what was the blocking reason?
     */
    val blockingReason: BlockingReason?,

    // ========== OPPORTUNITY SCORING BREAKDOWN ==========

    /**
     * Overall opportunity score (0-100)
     */
    val opportunityScore: Int,

    /**
     * Opportunity level based on score
     */
    val opportunityLevel: OpportunityLevel,

    /**
     * Breakdown of each factor's contribution to opportunity score
     * Map of factor name to points awarded
     */
    val opportunityBreakdown: Map<String, Int>,

    // ========== PERSONA DETECTION ==========

    /**
     * Detected user persona at time of decision
     */
    val personaDetected: Persona,

    /**
     * Confidence in persona detection
     */
    val personaConfidence: PersonaConfidence,

    // ========== RATE LIMITING FACTORS ==========

    /**
     * Did the decision pass basic rate limiting check?
     * (Standard cooldown timers)
     */
    val passedBasicRateLimit: Boolean,

    /**
     * Time since last intervention (seconds)
     * null if this is first intervention
     */
    val timeSinceLastIntervention: Long?,

    /**
     * Did the decision pass persona-specific frequency rules?
     */
    val passedPersonaFrequency: Boolean,

    /**
     * Persona-specific frequency rule that was applied
     */
    val personaFrequencyRule: String?,

    /**
     * Did the decision pass JITAI opportunity filter?
     */
    val passedJitaiFilter: Boolean,

    /**
     * JITAI decision (INTERVENE_NOW, WAIT_FOR_BETTER_OPPORTUNITY, etc.)
     */
    val jitaiDecision: String?,

    // ========== BURDEN CONSIDERATIONS ==========

    /**
     * Current burden level at time of decision
     */
    val burdenLevel: BurdenLevel?,

    /**
     * Burden score (if available)
     */
    val burdenScore: Int?,

    /**
     * Was burden mitigation applied?
     */
    val burdenMitigationApplied: Boolean,

    /**
     * Cooldown multiplier applied due to burden
     */
    val burdenCooldownMultiplier: Float?,

    // ========== CONTENT SELECTION (if shown) ==========

    /**
     * Content type that was selected to show
     * null if intervention was skipped
     */
    val contentTypeSelected: ContentType?,

    /**
     * Weights used for content selection
     * Map of content type to weight
     */
    val contentWeights: Map<String, Int>?,

    /**
     * Reason for content selection
     */
    val contentSelectionReason: String?,

    // ========== REINFORCEMENT LEARNING (if enabled) ==========

    /**
     * RL-predicted reward for showing intervention
     * null if RL not yet enabled
     */
    val rlPredictedReward: Double?,

    /**
     * Was this an exploration or exploitation decision?
     * null if RL not enabled
     */
    val rlExplorationVsExploitation: String?,

    // ========== CONTEXT SNAPSHOT ==========

    /**
     * Full intervention context at time of decision
     */
    val contextSnapshot: Map<String, Any>,

    // ========== HUMAN-READABLE EXPLANATION ==========

    /**
     * Clear, concise explanation of the decision
     */
    val explanation: String,

    /**
     * Detailed explanation with all factors
     */
    val detailedExplanation: String
)

/**
 * Final intervention decision
 */
enum class InterventionDecision {
    SHOW,    // Intervention was shown to user
    SKIP     // Intervention was skipped
}

/**
 * Reasons why intervention might be blocked/skipped
 */
enum class BlockingReason {
    BASIC_RATE_LIMIT,              // Standard cooldown not met
    PERSONA_FREQUENCY_LIMIT,       // Persona-specific frequency rule
    JITAI_POOR_OPPORTUNITY,        // JITAI scored opportunity as poor
    BURDEN_MITIGATION,             // User experiencing high burden
    SNOOZE_ACTIVE,                 // User has snoozed interventions
    PERMISSION_DENIED,             // Overlay permission not granted
    ALWAYS_SHOW_DISABLED,          // User disabled "always show" setting
    OTHER                          // Other reason
}

/**
 * Opportunity levels for intervention timing
 */
enum class OpportunityLevel {
    EXCELLENT,   // Score >= 70
    GOOD,        // Score >= 50
    MODERATE,    // Score >= 30
    POOR         // Score < 30
}

/**
 * User personas
 */
enum class Persona {
    NEW_USER,
    PROBLEMATIC_PATTERN_USER,
    HEAVY_COMPULSIVE_USER,
    HEAVY_BINGE_USER,
    MODERATE_BALANCED_USER,
    CASUAL_USER,
    UNKNOWN
}

/**
 * Persona detection confidence
 */
enum class PersonaConfidence {
    LOW,      // < 7 days of data
    MEDIUM,   // 7-13 days of data
    HIGH      // 14+ days of data
}

/**
 * Content types for interventions
 */
enum class ContentType {
    REFLECTION,
    TIME_ALTERNATIVE,
    BREATHING,
    STATS,
    EMOTIONAL_APPEAL,
    QUOTE,
    GAMIFICATION,
    ACTIVITY_SUGGESTION
}

/**
 * Extension function to generate human-readable explanation
 */
fun InterventionDecisionExplanation.generateExplanation(): String {
    return when (decision) {
        InterventionDecision.SKIP -> {
            when (blockingReason) {
                BlockingReason.BASIC_RATE_LIMIT -> {
                    "SKIPPED: Cooldown period active (${timeSinceLastIntervention}s since last intervention)"
                }
                BlockingReason.PERSONA_FREQUENCY_LIMIT -> {
                    "SKIPPED: Persona frequency limit ($personaFrequencyRule for $personaDetected)"
                }
                BlockingReason.JITAI_POOR_OPPORTUNITY -> {
                    "SKIPPED: Poor timing opportunity (score: $opportunityScore/$opportunityLevel)"
                }
                BlockingReason.BURDEN_MITIGATION -> {
                    "SKIPPED: User experiencing ${burdenLevel?.name} burden (mitigation active)"
                }
                BlockingReason.SNOOZE_ACTIVE -> {
                    "SKIPPED: User has snoozed interventions"
                }
                else -> "SKIPPED: ${blockingReason?.name ?: "Unknown reason"}"
            }
        }

        InterventionDecision.SHOW -> {
            buildString {
                append("SHOWN: Opportunity score $opportunityScore ($opportunityLevel) ")
                append("for $personaDetected user. ")

                // Add key context factors
                val context = contextSnapshot
                if (context["quickReopen"] == true) append("Quick reopen detected. ")
                if (context["isExtendedSession"] == true) {
                    val minutes = (context["currentSessionMinutes"] as? Int) ?: 0
                    append("Extended session ($minutes min). ")
                }
                if (context["isLateNight"] == true) append("Late night usage. ")
                if (context["isOverGoal"] == true) append("Over daily goal. ")

                // Add content info
                contentTypeSelected?.let {
                    append("Showing $it content. ")
                }

                // Add burden info if relevant
                if (burdenMitigationApplied) {
                    append("(Burden mitigation applied: ${burdenCooldownMultiplier}x cooldown)")
                }
            }
        }
    }
}

/**
 * Extension function to generate detailed explanation
 */
fun InterventionDecisionExplanation.generateDetailedExplanation(): String {
    return buildString {
        appendLine("=== Intervention Decision Explanation ===")
        appendLine("Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp)}")
        appendLine("App: $targetApp")
        appendLine()

        appendLine("DECISION: ${decision.name}")
        if (blockingReason != null) {
            appendLine("Blocking Reason: ${blockingReason.name}")
        }
        appendLine()

        appendLine("=== Opportunity Scoring ===")
        appendLine("Overall Score: $opportunityScore/100 ($opportunityLevel)")
        appendLine("Breakdown:")
        opportunityBreakdown.forEach { (factor, points) ->
            appendLine("  - $factor: $points points")
        }
        appendLine()

        appendLine("=== Persona Detection ===")
        appendLine("Persona: $personaDetected (confidence: $personaConfidence)")
        appendLine()

        appendLine("=== Rate Limiting Checks ===")
        appendLine("Basic Rate Limit: ${if (passedBasicRateLimit) "PASS" else "FAIL"}")
        if (timeSinceLastIntervention != null) {
            appendLine("  Time since last: ${timeSinceLastIntervention}s")
        }
        appendLine("Persona Frequency: ${if (passedPersonaFrequency) "PASS" else "FAIL"}")
        if (personaFrequencyRule != null) {
            appendLine("  Rule: $personaFrequencyRule")
        }
        appendLine("JITAI Filter: ${if (passedJitaiFilter) "PASS" else "FAIL"}")
        if (jitaiDecision != null) {
            appendLine("  JITAI Decision: $jitaiDecision")
        }
        appendLine()

        if (burdenLevel != null) {
            appendLine("=== Burden Considerations ===")
            appendLine("Burden Level: $burdenLevel")
            if (burdenScore != null) appendLine("Burden Score: $burdenScore")
            appendLine("Mitigation Applied: $burdenMitigationApplied")
            if (burdenCooldownMultiplier != null) {
                appendLine("Cooldown Multiplier: ${burdenCooldownMultiplier}x")
            }
            appendLine()
        }

        if (contentTypeSelected != null) {
            appendLine("=== Content Selection ===")
            appendLine("Selected: $contentTypeSelected")
            if (contentSelectionReason != null) {
                appendLine("Reason: $contentSelectionReason")
            }
            if (contentWeights != null) {
                appendLine("Weights:")
                contentWeights.forEach { (type, weight) ->
                    appendLine("  - $type: $weight")
                }
            }
            appendLine()
        }

        if (rlPredictedReward != null) {
            appendLine("=== Reinforcement Learning ===")
            appendLine("Predicted Reward: $rlPredictedReward")
            appendLine("Mode: $rlExplorationVsExploitation")
            appendLine()
        }

        appendLine("=== Context ===")
        contextSnapshot.forEach { (key, value) ->
            appendLine("  $key: $value")
        }
    }
}

/**
 * Extension function to check if decision was optimal
 * (For post-hoc analysis)
 */
fun InterventionDecisionExplanation.wasDecisionOptimal(actualOutcome: UserChoice): Boolean {
    return when (decision) {
        InterventionDecision.SHOW -> {
            // Good decision if user went back
            actualOutcome == UserChoice.GO_BACK
        }
        InterventionDecision.SKIP -> {
            // Always hard to judge skips, but if opportunity was poor, likely correct
            opportunityLevel == OpportunityLevel.POOR
        }
    }
}
