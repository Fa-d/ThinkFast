package dev.sadakat.thinkfaster.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sadakat.thinkfaster.data.local.database.entities.InterventionResultEntity

/**
 * DAO for InterventionResult entity
 * Phase G: Tracks intervention effectiveness metrics
 */
@Dao
interface InterventionResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: InterventionResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<InterventionResultEntity>)

    @Query("""
        UPDATE intervention_results
        SET finalSessionDurationMs = :finalDuration,
            sessionEndedNormally = :endedNormally
        WHERE sessionId = :sessionId
    """)
    suspend fun updateSessionOutcome(
        sessionId: Long,
        finalDuration: Long,
        endedNormally: Boolean
    )

    // ========== Phase 1: Feedback System Queries ==========

    @Query("""
        UPDATE intervention_results
        SET user_feedback = :feedback,
            feedback_timestamp = :timestamp
        WHERE sessionId = :sessionId
    """)
    suspend fun updateFeedback(
        sessionId: Long,
        feedback: String,
        timestamp: Long
    )

    @Query("""
        UPDATE intervention_results
        SET audio_active = :audioActive
        WHERE sessionId = :sessionId
    """)
    suspend fun updateAudioActive(
        sessionId: Long,
        audioActive: Boolean
    )

    @Query("""
        UPDATE intervention_results
        SET was_snoozed = 1,
            snooze_duration_ms = :snoozeDurationMs
        WHERE sessionId = :sessionId
    """)
    suspend fun updateSnooze(
        sessionId: Long,
        snoozeDurationMs: Long
    )

    @Query("""
        SELECT user_feedback as feedback,
               COUNT(*) as count
        FROM intervention_results
        GROUP BY user_feedback
    """)
    suspend fun getFeedbackBreakdown(): List<FeedbackStat>

    @Query("""
        SELECT * FROM intervention_results
        WHERE timestamp >= :startTime
          AND timestamp <= :endTime
          AND user_feedback != 'NONE'
        ORDER BY timestamp DESC
    """)
    suspend fun getInterventionsWithFeedback(
        startTime: Long,
        endTime: Long
    ): List<InterventionResultEntity>

    @Query("SELECT * FROM intervention_results WHERE id = :id")
    suspend fun getResultById(id: Long): InterventionResultEntity?

    @Query("SELECT * FROM intervention_results WHERE sessionId = :sessionId")
    suspend fun getResultBySessionId(sessionId: Long): InterventionResultEntity?

    @Query("SELECT * FROM intervention_results WHERE targetApp = :targetApp ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecentResultsForApp(targetApp: String): List<InterventionResultEntity>

    @Query("SELECT * FROM intervention_results ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentResults(limit: Int = 100): List<InterventionResultEntity>

    @Query("""
        SELECT contentType,
               COUNT(*) as total,
               SUM(CASE WHEN userChoice = 'GO_BACK' THEN 1 ELSE 0 END) as goBackCount,
               AVG(timeToShowDecisionMs) as avgDecisionTimeMs,
               AVG(finalSessionDurationMs) as avgFinalDurationMs
        FROM intervention_results
        WHERE finalSessionDurationMs IS NOT NULL
        GROUP BY contentType
    """)
    suspend fun getEffectivenessByContentType(): List<ContentEffectivenessStats>

    @Query("""
        SELECT userChoice,
               COUNT(*) as count
        FROM intervention_results
        GROUP BY userChoice
    """)
    suspend fun getUserChoiceBreakdown(): List<UserChoiceStat>

    @Query("""
        SELECT
            AVG(CASE WHEN userChoice = 'GO_BACK' THEN 100.0 ELSE 0.0 END) as dismissalRate
        FROM intervention_results
        WHERE finalSessionDurationMs IS NOT NULL
    """)
    suspend fun getOverallDismissalRate(): Double?

    @Query("""
        SELECT AVG(timeToShowDecisionMs) FROM intervention_results
    """)
    suspend fun getAverageDecisionTime(): Long?

    @Query("""
        SELECT targetApp,
               COUNT(*) as interventions,
               SUM(CASE WHEN userChoice = 'GO_BACK' THEN 1 ELSE 0 END) as goBackCount
        FROM intervention_results
        GROUP BY targetApp
        ORDER BY interventions DESC
    """)
    suspend fun getStatsByApp(): List<AppStats>

    @Query("DELETE FROM intervention_results WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteResultsOlderThan(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM intervention_results")
    suspend fun getTotalResultCount(): Int

    // Phase 3: Intervention effectiveness queries

    /**
     * Get effectiveness by time window with date range filter
     */
    @Query("""
        SELECT
            CASE
                WHEN hourOfDay BETWEEN 22 AND 23 OR hourOfDay BETWEEN 0 AND 5 THEN 'Late Night'
                WHEN hourOfDay BETWEEN 6 AND 11 THEN 'Morning'
                WHEN hourOfDay BETWEEN 12 AND 17 THEN 'Afternoon'
                ELSE 'Evening'
            END as timeWindow,
            COUNT(*) as total,
            SUM(CASE WHEN userChoice = 'GO_BACK' THEN 1 ELSE 0 END) as goBackCount,
            AVG(timeToShowDecisionMs) as avgDecisionTimeMs
        FROM intervention_results
        WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp
        GROUP BY timeWindow
    """)
    suspend fun getEffectivenessByTimeWindow(
        startTimestamp: Long,
        endTimestamp: Long
    ): List<TimeWindowStats>

    /**
     * Get effectiveness for specific contexts
     */
    @Query("""
        SELECT
            contentType,
            COUNT(*) as total,
            SUM(CASE WHEN userChoice = 'GO_BACK' THEN 1 ELSE 0 END) as goBackCount,
            AVG(timeToShowDecisionMs) as avgDecisionTimeMs
        FROM intervention_results
        WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp
        AND (
            CASE
                WHEN :contextFilter = 'LATE_NIGHT' THEN (hourOfDay >= 22 OR hourOfDay <= 5)
                WHEN :contextFilter = 'WEEKEND' THEN isWeekend = 1
                WHEN :contextFilter = 'QUICK_REOPEN' THEN quickReopen = 1
                ELSE 1 = 1
            END
        )
        GROUP BY contentType
    """)
    suspend fun getEffectivenessByContext(
        startTimestamp: Long,
        endTimestamp: Long,
        contextFilter: String  // 'LATE_NIGHT', 'WEEKEND', 'QUICK_REOPEN', or 'ALL'
    ): List<ContextEffectivenessStats>

    /**
     * Get all intervention results in a date range
     */
    @Query("""
        SELECT * FROM intervention_results
        WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp
        ORDER BY timestamp DESC
    """)
    suspend fun getResultsInRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): List<InterventionResultEntity>

    /**
     * Get effectiveness trend over time (by day)
     */
    @Query("""
        SELECT
            date(timestamp/1000, 'unixepoch', 'localtime') as day,
            COUNT(*) as total,
            SUM(CASE WHEN userChoice = 'GO_BACK' THEN 1 ELSE 0 END) as goBackCount
        FROM intervention_results
        WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp
        GROUP BY day
        ORDER BY day ASC
    """)
    suspend fun getEffectivenessTrendByDay(
        startTimestamp: Long,
        endTimestamp: Long
    ): List<DailyEffectivenessStat>

    // ========== Sync Methods ==========

    @Query("SELECT * FROM intervention_results WHERE user_id = :userId")
    suspend fun getResultsByUserId(userId: String): List<InterventionResultEntity>

    @Query("SELECT * FROM intervention_results WHERE sync_status = :status")
    suspend fun getResultsBySyncStatus(status: String): List<InterventionResultEntity>

    @Query("SELECT * FROM intervention_results WHERE user_id = :userId AND sync_status = :status")
    suspend fun getResultsByUserAndSyncStatus(userId: String, status: String): List<InterventionResultEntity>

    @Query("UPDATE intervention_results SET sync_status = :status, cloud_id = :cloudId, last_modified = :lastModified WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, cloudId: String?, lastModified: Long)

    @Query("UPDATE intervention_results SET user_id = :userId WHERE id = :id")
    suspend fun updateUserId(id: Long, userId: String)

    @Query("UPDATE intervention_results SET last_modified = :lastModified WHERE id = :id")
    suspend fun updateLastModified(id: Long, lastModified: Long)

    @Query("SELECT * FROM intervention_results")
    suspend fun getAllResults(): List<InterventionResultEntity>
}

/**
 * Stats for content effectiveness
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
}

/**
 * Stats for user choices
 */
data class UserChoiceStat(
    val userChoice: String,
    val count: Int
)

/**
 * Phase 1: Stats for user feedback
 */
data class FeedbackStat(
    val feedback: String,  // "HELPFUL" | "DISRUPTIVE" | "NONE"
    val count: Int
)

/**
 * Stats per app
 */
data class AppStats(
    val targetApp: String,
    val interventions: Int,
    val goBackCount: Int
) {
    val dismissalRate: Double
        get() = if (interventions > 0) (goBackCount.toDouble() / interventions) * 100 else 0.0
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
