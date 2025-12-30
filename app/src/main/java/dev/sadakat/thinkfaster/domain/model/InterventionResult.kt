package dev.sadakat.thinkfaster.domain.model

/**
 * Domain model for intervention result tracking
 * Phase G: Measures intervention effectiveness
 */
data class InterventionResult(
    val id: Long = 0,
    val sessionId: Long,
    val targetApp: String,
    val interventionType: InterventionType,
    val contentType: String,

    // Context at time of intervention
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val isWeekend: Boolean,
    val isLateNight: Boolean,
    val sessionCount: Int,
    val quickReopen: Boolean,
    val currentSessionDurationMs: Long,

    // User behavior
    val userChoice: UserChoice,
    val timeToShowDecisionMs: Long,

    // Outcome (optional - filled after session ends)
    val finalSessionDurationMs: Long? = null,
    val sessionEndedNormally: Boolean? = null,
    val timestamp: Long
)

/**
 * Type of intervention shown
 */
enum class InterventionType {
    REMINDER,   // Shown on app launch
    TIMER       // Shown at 10-minute alert
}

/**
 * User's choice when presented with intervention
 */
enum class UserChoice {
    PROCEED,    // User chose to continue to the app
    GO_BACK,    // User chose not to proceed (successful intervention!)
    DISMISSED   // Overlay was dismissed without explicit choice
}

/**
 * Statistics for content effectiveness
 */
data class ContentEffectivenessStats(
    val contentType: String,
    val total: Int,
    val goBackCount: Int,
    val avgDecisionTimeMs: Double?,
    val avgFinalDurationMs: Double?
) {
    val dismissalRate: Double
        get() = if (total > 0) (goBackCount.toDouble() / total) * 100 else 0.0

    val successRate: Double
        get() = dismissalRate  // Same as dismissal rate - higher is better

    val avgDecisionTimeSeconds: Double
        get() = (avgDecisionTimeMs ?: 0.0) / 1000.0

    val avgFinalDurationMinutes: Double
        get() = (avgFinalDurationMs ?: 0.0) / 1000.0 / 60.0
}

/**
 * Overall analytics summary
 */
data class OverallAnalytics(
    val totalInterventions: Int,
    val proceedCount: Int,
    val goBackCount: Int,
    val dismissalRate: Double,
    val avgDecisionTimeMs: Long?,
    val avgSessionAfterInterventionMs: Double?
) {
    val avgDecisionTimeSeconds: Double
        get() = (avgDecisionTimeMs ?: 0) / 1000.0

    val dismissalRatePercent: String
        get() = "%.1f%%".format(dismissalRate)

    val avgSessionAfterInterventionMinutes: Double
        get() = (avgSessionAfterInterventionMs ?: 0.0) / 1000.0 / 60.0
}
