package dev.sadakat.thinkfast.domain.repository

import dev.sadakat.thinkfast.domain.model.ContentEffectivenessStats
import dev.sadakat.thinkfast.domain.model.InterventionResult
import dev.sadakat.thinkfast.domain.model.InterventionType
import dev.sadakat.thinkfast.domain.model.OverallAnalytics
import dev.sadakat.thinkfast.domain.model.UserChoice
import kotlinx.coroutines.flow.Flow

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
