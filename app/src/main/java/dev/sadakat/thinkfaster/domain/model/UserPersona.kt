package dev.sadakat.thinkfaster.domain.model

/**
 * User Persona and Intervention Strategy Definitions
 * Phase 2 JITAI: Behavioral user segmentation for personalized interventions
 */

/**
 * Confidence level in persona detection
 */
enum class ConfidenceLevel {
    LOW,    // < 7 days of data
    MEDIUM, // 7-13 days of data
    HIGH    // 14+ days of data
}

/**
 * Intervention decision types
 * Phase 2 JITAI: Decision outcomes for intervention timing
 */
enum class InterventionDecision {
    INTERVENE_NOW,                   // Show intervention immediately
    INTERVENE_WITH_CONSIDERATION,     // Show intervention with some conditions
    WAIT_FOR_BETTER_OPPORTUNITY,      // Wait, but opportunity may improve
    SKIP_INTERVENTION                 // Do not show intervention
}

/**
 * Opportunity level classification
 * Phase 2 JITAI: Categorizes intervention opportunity scores
 */
enum class OpportunityLevel {
    EXCELLENT,  // Score >= 70
    GOOD,       // Score >= 50
    MODERATE,   // Score >= 30
    POOR        // Score < 30
}

/**
 * Opportunity decision result
 * Phase 2 JITAI: Result of opportunity score calculation
 */
data class OpportunityDecision(
    val score: Int,  // 0-100
    val level: OpportunityLevel,
    val decision: InterventionDecision
)

/**
 * Usage trend classification
 */
enum class UsageTrendType {
    ESCALATING,   // Usage increasing rapidly
    INCREASING,   // Usage increasing
    STABLE,       // Usage consistent
    DECREASING,   // Usage decreasing
    DECLINING     // Usage decreasing rapidly
}

/**
 * Usage pattern classification
 * Phase 2 JITAI: Behavioral patterns based on session data
 */
enum class UsagePattern {
    COMPULSIVE_CHECKING,  // Many short sessions with quick reopens
    BINGE_SESSIONS,       // Fewer but longer sessions
    BALANCED,             // Moderate frequency and duration
    CASUAL,               // Light usage
    ESCALATING,           // Usage increasing over time
    UNKNOWN               // Not enough data
}

/**
 * Intervention frequency strategy
 * Phase 2 JITAI: How often to show interventions based on persona
 */
enum class InterventionFrequency {
    ONBOARDING,    // First-time users - minimal interventions
    MINIMAL,       // Problematic users - very few interventions
    CONSERVATIVE,  // Heavy users - reduced frequency
    BALANCED,      // Normal users - standard frequency
    MODERATE,      // Binge users - more frequent
    ADAPTIVE       // Casual users - context-dependent
}

/**
 * User Persona Definitions
 * Phase 2 JITAI: Behavioral user segmentation for personalized interventions
 *
 * Each persona has specific characteristics and intervention strategies.
 * Uses ContentType from domain.intervention package.
 */
enum class UserPersona(
    val displayName: String,
    val description: String,
    val minDailySessions: Int,
    val maxDailySessions: Int,
    val avgSessionLengthMin: Int,
    val quickReopenRate: Double,
    val usagePattern: UsagePattern,
    val frequency: InterventionFrequency,
    // Map of ContentType enum name to weight (using String to avoid circular dependency)
    val baseWeights: Map<String, Int>
) {
    HEAVY_COMPULSIVE_USER(
        displayName = "Heavy Compulsive User",
        description = "15+ sessions/day, 40%+ quick reopens. Needs strong friction to break patterns.",
        minDailySessions = 15,
        maxDailySessions = Int.MAX_VALUE,
        avgSessionLengthMin = 3,
        quickReopenRate = 0.40,
        usagePattern = UsagePattern.COMPULSIVE_CHECKING,
        frequency = InterventionFrequency.CONSERVATIVE,
        baseWeights = mapOf(
            "REFLECTION" to 50,
            "TIME_ALTERNATIVE" to 20,
            "BREATHING" to 15,
            "EMOTIONAL_APPEAL" to 10,
            "ACTIVITY_SUGGESTION" to 5
        )
    ),

    HEAVY_BINGE_USER(
        displayName = "Heavy Binge User",
        description = "6+ sessions/day, 25+ min sessions. Long engagement needs activity alternatives.",
        minDailySessions = 6,
        maxDailySessions = 14,
        avgSessionLengthMin = 25,
        quickReopenRate = 0.15,
        usagePattern = UsagePattern.BINGE_SESSIONS,
        frequency = InterventionFrequency.MODERATE,
        baseWeights = mapOf(
            "TIME_ALTERNATIVE" to 40,
            "REFLECTION" to 30,
            "ACTIVITY_SUGGESTION" to 15,
            "EMOTIONAL_APPEAL" to 10,
            "BREATHING" to 5
        )
    ),

    MODERATE_BALANCED_USER(
        displayName = "Moderate Balanced User",
        description = "8-12 sessions/day, 8-15 min sessions. Balanced approach needed.",
        minDailySessions = 8,
        maxDailySessions = 12,
        avgSessionLengthMin = 12,
        quickReopenRate = 0.20,
        usagePattern = UsagePattern.BALANCED,
        frequency = InterventionFrequency.BALANCED,
        baseWeights = mapOf(
            "REFLECTION" to 35,
            "TIME_ALTERNATIVE" to 30,
            "BREATHING" to 15,
            "ACTIVITY_SUGGESTION" to 10,
            "EMOTIONAL_APPEAL" to 10
        )
    ),

    CASUAL_USER(
        displayName = "Casual User",
        description = "4-6 sessions/day, 5-8 min sessions. Light touch interventions.",
        minDailySessions = 4,
        maxDailySessions = 7,
        avgSessionLengthMin = 6,
        quickReopenRate = 0.15,
        usagePattern = UsagePattern.CASUAL,
        frequency = InterventionFrequency.ADAPTIVE,
        baseWeights = mapOf(
            "REFLECTION" to 25,
            "BREATHING" to 20,
            "TIME_ALTERNATIVE" to 20,
            "ACTIVITY_SUGGESTION" to 20,
            "EMOTIONAL_APPEAL" to 15
        )
    ),

    PROBLEMATIC_PATTERN_USER(
        displayName = "Problematic Pattern User",
        description = "Escalating usage, 50%+ quick reopens. High priority intervention needed.",
        minDailySessions = 10,
        maxDailySessions = Int.MAX_VALUE,
        avgSessionLengthMin = 10,
        quickReopenRate = 0.50,
        usagePattern = UsagePattern.ESCALATING,
        frequency = InterventionFrequency.MINIMAL,
        baseWeights = mapOf(
            "REFLECTION" to 60,
            "TIME_ALTERNATIVE" to 20,
            "EMOTIONAL_APPEAL" to 15,
            "BREATHING" to 5,
            "ACTIVITY_SUGGESTION" to 0
        )
    ),

    NEW_USER(
        displayName = "New User",
        description = "First 14 days. Onboarding mode with gentle interventions.",
        minDailySessions = 0,
        maxDailySessions = Int.MAX_VALUE,
        avgSessionLengthMin = 0,
        quickReopenRate = 0.0,
        usagePattern = UsagePattern.UNKNOWN,
        frequency = InterventionFrequency.ONBOARDING,
        baseWeights = mapOf(
            "REFLECTION" to 25,
            "BREATHING" to 25,
            "TIME_ALTERNATIVE" to 20,
            "ACTIVITY_SUGGESTION" to 15,
            "EMOTIONAL_APPEAL" to 15
        )
    );

    companion object {
        /**
         * Detect persona based on behavioral metrics
         */
        fun detect(
            daysSinceInstall: Int,
            avgDailySessions: Double,
            avgSessionLengthMin: Double,
            quickReopenRate: Double,
            usageTrend: UsageTrendType
        ): UserPersona {
            // New user - first 14 days
            if (daysSinceInstall < 14) {
                return NEW_USER
            }

            // Problematic pattern - escalating usage
            if (usageTrend == UsageTrendType.ESCALATING && quickReopenRate > 0.40) {
                return PROBLEMATIC_PATTERN_USER
            }

            // Heavy compulsive - many short sessions with high quick reopen rate
            if (avgDailySessions >= 15 && quickReopenRate >= 0.35 && avgSessionLengthMin < 5) {
                return HEAVY_COMPULSIVE_USER
            }

            // Heavy binge - fewer but longer sessions
            if (avgDailySessions >= 6 && avgSessionLengthMin >= 20) {
                return HEAVY_BINGE_USER
            }

            // Moderate balanced - in the middle
            if (avgDailySessions >= 8 && avgDailySessions <= 13) {
                return MODERATE_BALANCED_USER
            }

            // Casual - light usage
            if (avgDailySessions < 8) {
                return CASUAL_USER
            }

            // Default to moderate balanced
            return MODERATE_BALANCED_USER
        }
    }
}
