package dev.sadakat.thinkfaster.domain.model

/**
 * Persona and Opportunity Analytics Data Classes
 * Phase 2 JITAI: Analytics for personalized intervention effectiveness
 */

/**
 * Stats for intervention effectiveness by persona
 */
data class PersonaEffectivenessStats(
    val personaName: String,
    val confidence: String,
    val total: Int,
    val goBackCount: Int,
    val avgDecisionTimeMs: Double?,
    val avgFinalDurationMs: Double?
) {
    val successRate: Double
        get() = if (total > 0) (goBackCount.toDouble() / total) * 100 else 0.0
}

/**
 * Stats for intervention effectiveness by opportunity level
 */
data class OpportunityLevelEffectivenessStats(
    val opportunityLevel: String,
    val opportunityScore: Int?,
    val total: Int,
    val goBackCount: Int,
    val avgDecisionTimeMs: Double?
) {
    val successRate: Double
        get() = if (total > 0) (goBackCount.toDouble() / total) * 100 else 0.0
}

/**
 * Stats for intervention effectiveness by persona and content type
 */
data class PersonaContentTypeStats(
    val personaName: String,
    val contentType: String,
    val total: Int,
    val goBackCount: Int,
    val avgDecisionTimeMs: Double?
) {
    val successRate: Double
        get() = if (total > 0) (goBackCount.toDouble() / total) * 100 else 0.0
}

/**
 * Persona history entry for tracking persona changes over time
 */
data class PersonaHistoryEntry(
    val day: String,
    val personaName: String,
    val count: Int
)
