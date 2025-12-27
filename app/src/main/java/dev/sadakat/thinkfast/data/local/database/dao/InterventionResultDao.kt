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
