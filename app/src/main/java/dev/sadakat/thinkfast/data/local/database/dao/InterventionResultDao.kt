package dev.sadakat.thinkfast.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sadakat.thinkfast.data.local.database.entities.InterventionResultEntity

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
