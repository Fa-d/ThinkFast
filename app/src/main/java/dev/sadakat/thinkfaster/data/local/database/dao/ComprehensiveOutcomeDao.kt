package dev.sadakat.thinkfaster.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.sadakat.thinkfaster.data.local.database.entities.ComprehensiveOutcomeEntity

/**
 * Phase 1: DAO for Comprehensive Outcome tracking
 *
 * Provides queries for:
 * - Finding outcomes that need updates
 * - RL training data retrieval
 * - Analytics and effectiveness metrics
 */
@Dao
interface ComprehensiveOutcomeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outcome: ComprehensiveOutcomeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(outcomes: List<ComprehensiveOutcomeEntity>)

    @Update
    suspend fun update(outcome: ComprehensiveOutcomeEntity)

    @Query("SELECT * FROM comprehensive_outcomes WHERE id = :id")
    suspend fun getById(id: Long): ComprehensiveOutcomeEntity?

    @Query("SELECT * FROM comprehensive_outcomes WHERE intervention_id = :interventionId")
    suspend fun getByInterventionId(interventionId: Long): ComprehensiveOutcomeEntity?

    // ========== OUTCOME COLLECTION QUERIES ==========

    /**
     * Get outcomes that need short-term collection (5-30 min after intervention)
     * Returns outcomes where:
     * - Proximal collected
     * - Short-term NOT collected
     * - At least 5 minutes have passed since intervention
     */
    @Query("""
        SELECT * FROM comprehensive_outcomes
        WHERE proximal_collected = 1
          AND short_term_collected = 0
          AND timestamp <= :fiveMinutesAgo
        ORDER BY timestamp ASC
        LIMIT :limit
    """)
    suspend fun getOutcomesNeedingShortTermCollection(
        fiveMinutesAgo: Long = System.currentTimeMillis() - (5 * 60 * 1000),
        limit: Int = 50
    ): List<ComprehensiveOutcomeEntity>

    /**
     * Get outcomes that need medium-term collection (same day, after session ends)
     * Returns outcomes where:
     * - Short-term collected
     * - Medium-term NOT collected
     * - At least 1 hour has passed since intervention
     */
    @Query("""
        SELECT * FROM comprehensive_outcomes
        WHERE short_term_collected = 1
          AND medium_term_collected = 0
          AND timestamp <= :oneHourAgo
        ORDER BY timestamp ASC
        LIMIT :limit
    """)
    suspend fun getOutcomesNeedingMediumTermCollection(
        oneHourAgo: Long = System.currentTimeMillis() - (60 * 60 * 1000),
        limit: Int = 50
    ): List<ComprehensiveOutcomeEntity>

    /**
     * Get outcomes that need long-term collection (7-30 days after)
     * Returns outcomes where:
     * - Medium-term collected
     * - Long-term NOT collected
     * - At least 7 days have passed since intervention
     */
    @Query("""
        SELECT * FROM comprehensive_outcomes
        WHERE medium_term_collected = 1
          AND long_term_collected = 0
          AND timestamp <= :sevenDaysAgo
        ORDER BY timestamp ASC
        LIMIT :limit
    """)
    suspend fun getOutcomesNeedingLongTermCollection(
        sevenDaysAgo: Long = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000),
        limit: Int = 50
    ): List<ComprehensiveOutcomeEntity>

    // ========== RL TRAINING QUERIES ==========

    /**
     * Get fully collected outcomes for RL training
     * Returns outcomes with all collection windows completed
     */
    @Query("""
        SELECT * FROM comprehensive_outcomes
        WHERE proximal_collected = 1
          AND short_term_collected = 1
          AND medium_term_collected = 1
          AND long_term_collected = 1
          AND reward_score IS NOT NULL
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getFullyCollectedOutcomes(limit: Int = 1000): List<ComprehensiveOutcomeEntity>

    /**
     * Get outcomes for a specific app (for app-specific RL models)
     */
    @Query("""
        SELECT * FROM comprehensive_outcomes
        WHERE target_app = :targetApp
          AND proximal_collected = 1
          AND reward_score IS NOT NULL
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getOutcomesForApp(
        targetApp: String,
        limit: Int = 500
    ): List<ComprehensiveOutcomeEntity>

    /**
     * Get outcomes by immediate choice (for effectiveness analysis)
     */
    @Query("""
        SELECT * FROM comprehensive_outcomes
        WHERE immediate_choice = :choice
          AND timestamp >= :startTime
        ORDER BY timestamp DESC
    """)
    suspend fun getOutcomesByChoice(
        choice: String,
        startTime: Long
    ): List<ComprehensiveOutcomeEntity>

    // ========== ANALYTICS QUERIES ==========

    /**
     * Get average reward score by immediate choice
     */
    @Query("""
        SELECT immediate_choice,
               COUNT(*) as count,
               AVG(reward_score) as avgReward,
               AVG(response_time) as avgResponseTime
        FROM comprehensive_outcomes
        WHERE reward_score IS NOT NULL
          AND timestamp >= :startTime
        GROUP BY immediate_choice
    """)
    suspend fun getRewardByChoice(startTime: Long): List<ChoiceRewardStats>

    /**
     * Get average reward score by interaction depth
     */
    @Query("""
        SELECT interaction_depth,
               COUNT(*) as count,
               AVG(reward_score) as avgReward
        FROM comprehensive_outcomes
        WHERE reward_score IS NOT NULL
          AND timestamp >= :startTime
        GROUP BY interaction_depth
    """)
    suspend fun getRewardByInteractionDepth(startTime: Long): List<InteractionRewardStats>

    /**
     * Get outcomes with high rewards (for identifying successful patterns)
     */
    @Query("""
        SELECT * FROM comprehensive_outcomes
        WHERE reward_score >= :minReward
          AND timestamp >= :startTime
        ORDER BY reward_score DESC
        LIMIT :limit
    """)
    suspend fun getHighRewardOutcomes(
        minReward: Double = 20.0,
        startTime: Long,
        limit: Int = 100
    ): List<ComprehensiveOutcomeEntity>

    /**
     * Get outcomes with low rewards (for identifying failure patterns)
     */
    @Query("""
        SELECT * FROM comprehensive_outcomes
        WHERE reward_score <= :maxReward
          AND timestamp >= :startTime
        ORDER BY reward_score ASC
        LIMIT :limit
    """)
    suspend fun getLowRewardOutcomes(
        maxReward: Double = -5.0,
        startTime: Long,
        limit: Int = 100
    ): List<ComprehensiveOutcomeEntity>

    /**
     * Get daily average reward trend
     */
    @Query("""
        SELECT date(timestamp/1000, 'unixepoch', 'localtime') as day,
               COUNT(*) as count,
               AVG(reward_score) as avgReward
        FROM comprehensive_outcomes
        WHERE reward_score IS NOT NULL
          AND timestamp >= :startTime
        GROUP BY day
        ORDER BY day DESC
    """)
    suspend fun getDailyRewardTrend(startTime: Long): List<DailyRewardStat>

    // ========== MAINTENANCE QUERIES ==========

    /**
     * Delete old outcomes (after 90 days)
     */
    @Query("DELETE FROM comprehensive_outcomes WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    /**
     * Get count of outcomes by collection status
     */
    @Query("""
        SELECT
            SUM(CASE WHEN proximal_collected = 1 THEN 1 ELSE 0 END) as proximalCount,
            SUM(CASE WHEN short_term_collected = 1 THEN 1 ELSE 0 END) as shortTermCount,
            SUM(CASE WHEN medium_term_collected = 1 THEN 1 ELSE 0 END) as mediumTermCount,
            SUM(CASE WHEN long_term_collected = 1 THEN 1 ELSE 0 END) as longTermCount
        FROM comprehensive_outcomes
    """)
    suspend fun getCollectionStatusCounts(): CollectionStatusCounts

    @Query("SELECT COUNT(*) FROM comprehensive_outcomes")
    suspend fun getTotalCount(): Int
}

// ========== DATA CLASSES FOR QUERY RESULTS ==========

data class ChoiceRewardStats(
    val immediate_choice: String,
    val count: Int,
    val avgReward: Double?,
    val avgResponseTime: Double?
)

data class InteractionRewardStats(
    val interaction_depth: String,
    val count: Int,
    val avgReward: Double?
)

data class DailyRewardStat(
    val day: String,
    val count: Int,
    val avgReward: Double?
)

data class CollectionStatusCounts(
    val proximalCount: Int,
    val shortTermCount: Int,
    val mediumTermCount: Int,
    val longTermCount: Int
)
