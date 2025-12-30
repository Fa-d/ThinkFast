package dev.sadakat.thinkfaster.domain.repository

import dev.sadakat.thinkfaster.domain.model.ContentEffectivenessStats
import dev.sadakat.thinkfaster.domain.model.InterventionResult
import dev.sadakat.thinkfaster.domain.model.OverallAnalytics

/**
 * Repository interface for intervention result tracking
 * Phase G: Effectiveness tracking and analytics
 */
interface InterventionResultRepository {

    /**
     * Record an intervention result
     * @return the ID of the inserted record
     */
    suspend fun recordResult(result: InterventionResult): Long

    /**
     * Update the outcome of a session after it ends
     * Called when session tracking completes
     */
    suspend fun updateSessionOutcome(
        sessionId: Long,
        finalDurationMs: Long,
        endedNormally: Boolean
    )

    /**
     * Get results for a specific session
     */
    suspend fun getResultBySession(sessionId: Long): InterventionResult?

    /**
     * Get recent results for a specific app
     */
    suspend fun getRecentResultsForApp(
        targetApp: String,
        limit: Int = 100
    ): List<InterventionResult>

    /**
     * Get recent results across all apps
     */
    suspend fun getRecentResults(limit: Int = 100): List<InterventionResult>

    /**
     * Get effectiveness statistics grouped by content type
     * Returns metrics like dismissal rate, average decision time, etc.
     */
    suspend fun getEffectivenessByContentType(): List<ContentEffectivenessStats>

    /**
     * Get overall analytics summary
     */
    suspend fun getOverallAnalytics(): OverallAnalytics

    /**
     * Get dismissal rate for a specific content type
     * @return percentage (0-100) of users who chose "Go Back"
     */
    suspend fun getDismissalRateForContentType(contentType: String): Double

    /**
     * Get overall dismissal rate across all interventions
     * @return percentage (0-100)
     */
    suspend fun getOverallDismissalRate(): Double

    /**
     * Get average decision time
     * @return average time in milliseconds users took to decide
     */
    suspend fun getAverageDecisionTime(): Long

    /**
     * Get the most effective content types
     * @return list of content types ranked by dismissal rate (highest first)
     */
    suspend fun getMostEffectiveContentTypes(limit: Int = 3): List<ContentEffectivenessStats>

    /**
     * Get stats by app
     */
    suspend fun getStatsByApp(): Map<String, AppInterventionStats>

    /**
     * Delete old results
     */
    suspend fun deleteResultsOlderThan(timestampMs: Long): Int

    /**
     * Get total count of recorded results
     */
    suspend fun getTotalResultCount(): Int

    // ========== Phase 3: Intervention Effectiveness Queries ==========

    /**
     * Get effectiveness by time window with date range filter
     */
    suspend fun getEffectivenessByTimeWindow(
        startTimestamp: Long,
        endTimestamp: Long
    ): List<TimeWindowStats>

    /**
     * Get effectiveness for specific contexts
     */
    suspend fun getEffectivenessByContext(
        startTimestamp: Long,
        endTimestamp: Long,
        contextFilter: String  // 'LATE_NIGHT', 'WEEKEND', 'QUICK_REOPEN', or 'ALL'
    ): List<ContextEffectivenessStats>

    /**
     * Get all intervention results in a date range
     */
    suspend fun getResultsInRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): List<InterventionResult>

    /**
     * Get effectiveness trend over time (by day)
     */
    suspend fun getEffectivenessTrendByDay(
        startTimestamp: Long,
        endTimestamp: Long
    ): List<DailyEffectivenessStat>
}

/**
 * Stats by time window (Phase 3)
 */
data class TimeWindowStats(
    val timeWindow: String,
    val total: Int,
    val goBackCount: Int,
    val avgDecisionTimeMs: Double?
) {
    val successRate: Double
        get() = if (total > 0) (goBackCount.toDouble() / total) * 100 else 0.0
}

/**
 * Stats by context (Phase 3)
 */
data class ContextEffectivenessStats(
    val contentType: String,
    val total: Int,
    val goBackCount: Int,
    val avgDecisionTimeMs: Double?
) {
    val successRate: Double
        get() = if (total > 0) (goBackCount.toDouble() / total) * 100 else 0.0
}

/**
 * Daily effectiveness stat for trend analysis (Phase 3)
 */
data class DailyEffectivenessStat(
    val day: String,
    val total: Int,
    val goBackCount: Int
) {
    val successRate: Double
        get() = if (total > 0) (goBackCount.toDouble() / total) * 100 else 0.0
}

/**
 * Stats for interventions on a specific app
 */
data class AppInterventionStats(
    val targetApp: String,
    val totalInterventions: Int,
    val goBackCount: Int,
    val dismissalRate: Double
)
