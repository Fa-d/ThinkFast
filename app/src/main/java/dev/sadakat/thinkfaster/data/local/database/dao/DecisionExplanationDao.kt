package dev.sadakat.thinkfaster.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sadakat.thinkfaster.data.local.database.entities.DecisionExplanationEntity

/**
 * Phase 1: DAO for Decision Explanation tracking
 *
 * Provides queries for analyzing intervention decision patterns
 */
@Dao
interface DecisionExplanationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(explanation: DecisionExplanationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(explanations: List<DecisionExplanationEntity>)

    @Query("SELECT * FROM decision_explanations WHERE id = :id")
    suspend fun getById(id: Long): DecisionExplanationEntity?

    @Query("SELECT * FROM decision_explanations WHERE intervention_id = :interventionId")
    suspend fun getByInterventionId(interventionId: Long): DecisionExplanationEntity?

    // ========== DECISION ANALYSIS QUERIES ==========

    /**
     * Get recent decisions (last N)
     */
    @Query("""
        SELECT * FROM decision_explanations
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentDecisions(limit: Int = 100): List<DecisionExplanationEntity>

    /**
     * Get decisions in time range
     */
    @Query("""
        SELECT * FROM decision_explanations
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        ORDER BY timestamp DESC
    """)
    suspend fun getDecisionsInRange(
        startTime: Long,
        endTime: Long
    ): List<DecisionExplanationEntity>

    /**
     * Get skip statistics by blocking reason
     */
    @Query("""
        SELECT blocking_reason,
               COUNT(*) as count
        FROM decision_explanations
        WHERE decision = 'SKIP'
          AND timestamp >= :startTime
        GROUP BY blocking_reason
        ORDER BY count DESC
    """)
    suspend fun getSkipReasonStats(startTime: Long): List<SkipReasonStat>

    /**
     * Get decisions by opportunity level
     */
    @Query("""
        SELECT opportunity_level,
               decision,
               COUNT(*) as count
        FROM decision_explanations
        WHERE timestamp >= :startTime
        GROUP BY opportunity_level, decision
    """)
    suspend fun getDecisionsByOpportunityLevel(startTime: Long): List<OpportunityLevelStat>

    /**
     * Get decisions by persona
     */
    @Query("""
        SELECT persona_detected,
               decision,
               COUNT(*) as count,
               AVG(opportunity_score) as avgOpportunityScore
        FROM decision_explanations
        WHERE timestamp >= :startTime
        GROUP BY persona_detected, decision
    """)
    suspend fun getDecisionsByPersona(startTime: Long): List<PersonaDecisionStat>

    /**
     * Get decisions where burden mitigation was applied
     */
    @Query("""
        SELECT * FROM decision_explanations
        WHERE burden_mitigation_applied = 1
          AND timestamp >= :startTime
        ORDER BY timestamp DESC
    """)
    suspend fun getDecisionsWithBurdenMitigation(startTime: Long): List<DecisionExplanationEntity>

    /**
     * Get show rate by opportunity level
     * (What % of each opportunity level actually resulted in SHOW?)
     */
    @Query("""
        SELECT opportunity_level,
               COUNT(*) as total,
               SUM(CASE WHEN decision = 'SHOW' THEN 1 ELSE 0 END) as showCount
        FROM decision_explanations
        WHERE timestamp >= :startTime
        GROUP BY opportunity_level
    """)
    suspend fun getShowRateByOpportunity(startTime: Long): List<ShowRateStat>

    /**
     * Get average opportunity score by decision
     */
    @Query("""
        SELECT decision,
               COUNT(*) as count,
               AVG(opportunity_score) as avgScore,
               MIN(opportunity_score) as minScore,
               MAX(opportunity_score) as maxScore
        FROM decision_explanations
        WHERE timestamp >= :startTime
        GROUP BY decision
    """)
    suspend fun getOpportunityScoreByDecision(startTime: Long): List<OpportunityScoreStat>

    /**
     * Get decisions for specific app
     */
    @Query("""
        SELECT * FROM decision_explanations
        WHERE target_app = :targetApp
          AND timestamp >= :startTime
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getDecisionsForApp(
        targetApp: String,
        startTime: Long,
        limit: Int = 100
    ): List<DecisionExplanationEntity>

    /**
     * Get rate limiting failures
     * (Decisions that were SKIP due to rate limiting)
     */
    @Query("""
        SELECT
            SUM(CASE WHEN passed_basic_rate_limit = 0 THEN 1 ELSE 0 END) as basicRateLimitFailures,
            SUM(CASE WHEN passed_persona_frequency = 0 THEN 1 ELSE 0 END) as personaFrequencyFailures,
            SUM(CASE WHEN passed_jitai_filter = 0 THEN 1 ELSE 0 END) as jitaiFilterFailures
        FROM decision_explanations
        WHERE timestamp >= :startTime
    """)
    suspend fun getRateLimitingFailures(startTime: Long): RateLimitingStats

    // ========== MAINTENANCE QUERIES ==========

    /**
     * Delete old explanations (after 90 days)
     * Keep storage manageable while retaining enough for analysis
     */
    @Query("DELETE FROM decision_explanations WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM decision_explanations")
    suspend fun getTotalCount(): Int

    /**
     * Get decision rate over time (decisions per day)
     */
    @Query("""
        SELECT date(timestamp/1000, 'unixepoch', 'localtime') as day,
               decision,
               COUNT(*) as count
        FROM decision_explanations
        WHERE timestamp >= :startTime
        GROUP BY day, decision
        ORDER BY day DESC
    """)
    suspend fun getDecisionRateByDay(startTime: Long): List<DailyDecisionStat>
}

// ========== DATA CLASSES FOR QUERY RESULTS ==========

data class SkipReasonStat(
    val blocking_reason: String?,
    val count: Int
)

data class OpportunityLevelStat(
    val opportunity_level: String,
    val decision: String,
    val count: Int
)

data class PersonaDecisionStat(
    val persona_detected: String,
    val decision: String,
    val count: Int,
    val avgOpportunityScore: Double?
)

data class ShowRateStat(
    val opportunity_level: String,
    val total: Int,
    val showCount: Int
) {
    val showRate: Double
        get() = if (total > 0) showCount.toDouble() / total else 0.0
}

data class OpportunityScoreStat(
    val decision: String,
    val count: Int,
    val avgScore: Double?,
    val minScore: Int?,
    val maxScore: Int?
)

data class RateLimitingStats(
    val basicRateLimitFailures: Int,
    val personaFrequencyFailures: Int,
    val jitaiFilterFailures: Int
)

data class DailyDecisionStat(
    val day: String,
    val decision: String,
    val count: Int
)
