package dev.sadakat.thinkfast.domain.repository

import dev.sadakat.thinkfast.domain.model.DailyStats
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing daily statistics and aggregations
 */
interface StatsRepository {

    /**
     * Insert or update daily stats
     */
    suspend fun upsertDailyStats(stats: DailyStats)

    /**
     * Get daily stats for a specific date and app
     * @param date the date in YYYY-MM-DD format
     * @param targetApp the package name of the target app
     */
    suspend fun getDailyStats(date: String, targetApp: String): DailyStats?

    /**
     * Observe daily stats for a specific date and app (real-time updates)
     * @param date the date in YYYY-MM-DD format
     * @param targetApp the package name of the target app
     */
    fun observeDailyStats(date: String, targetApp: String): Flow<DailyStats?>

    /**
     * Get all daily stats for a specific date (all apps)
     * @param date the date in YYYY-MM-DD format
     */
    suspend fun getAllDailyStatsForDate(date: String): List<DailyStats>

    /**
     * Get daily stats within a date range for a specific app
     * @param startDate the start date in YYYY-MM-DD format
     * @param endDate the end date in YYYY-MM-DD format
     * @param targetApp the package name of the target app
     */
    suspend fun getDailyStatsInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): List<DailyStats>

    /**
     * Observe daily stats within a date range (real-time updates)
     * @param startDate the start date in YYYY-MM-DD format
     * @param endDate the end date in YYYY-MM-DD format
     * @param targetApp the package name of the target app
     */
    fun observeDailyStatsInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): Flow<List<DailyStats>>

    /**
     * Get the total duration for a specific app within a date range
     * @param startDate the start date in YYYY-MM-DD format
     * @param endDate the end date in YYYY-MM-DD format
     * @param targetApp the package name of the target app
     */
    suspend fun getTotalDurationInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): Long

    /**
     * Get the average daily duration for a specific app within a date range
     * @param startDate the start date in YYYY-MM-DD format
     * @param endDate the end date in YYYY-MM-DD format
     * @param targetApp the package name of the target app
     */
    suspend fun getAverageDurationInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): Long

    /**
     * Get the maximum single-day duration within a date range
     * @param startDate the start date in YYYY-MM-DD format
     * @param endDate the end date in YYYY-MM-DD format
     * @param targetApp the package name of the target app
     */
    suspend fun getMaxDurationInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): Long

    /**
     * Get the total session count within a date range
     * @param startDate the start date in YYYY-MM-DD format
     * @param endDate the end date in YYYY-MM-DD format
     * @param targetApp the package name of the target app
     */
    suspend fun getTotalSessionCountInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): Int

    /**
     * Delete stats older than a specific date
     * @param beforeDate the date before which stats should be deleted (YYYY-MM-DD)
     */
    suspend fun deleteStatsOlderThan(beforeDate: String)

    /**
     * Aggregate and update daily stats from usage sessions
     * This should be called at the end of each day or when needed
     * @param date the date to aggregate (YYYY-MM-DD)
     */
    suspend fun aggregateDailyStats(date: String)
}
