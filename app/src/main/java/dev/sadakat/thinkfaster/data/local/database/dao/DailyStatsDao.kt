package dev.sadakat.thinkfaster.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import dev.sadakat.thinkfaster.data.local.database.entities.DailyStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {
    @Upsert
    suspend fun upsertDailyStats(stats: DailyStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStats(stats: DailyStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stats: List<DailyStatsEntity>)

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsByDate(date: String): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getAllDailyStatsForDate(date: String): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats WHERE date = :date AND targetApp = :targetApp")
    suspend fun getStatsByDateAndApp(date: String, targetApp: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE date = :date AND targetApp = :targetApp")
    suspend fun getDailyStats(date: String, targetApp: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE date = :date AND targetApp = :targetApp")
    fun observeDailyStats(date: String, targetApp: String): Flow<DailyStatsEntity?>

    @Query("""
        SELECT * FROM daily_stats
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date DESC
    """)
    suspend fun getStatsInRange(startDate: String, endDate: String): List<DailyStatsEntity>

    @Query("""
        SELECT * FROM daily_stats
        WHERE date >= :startDate AND date <= :endDate AND targetApp = :targetApp
        ORDER BY date DESC
    """)
    suspend fun getStatsInRangeForApp(
        startDate: String,
        endDate: String,
        targetApp: String
    ): List<DailyStatsEntity>

    @Query("""
        SELECT * FROM daily_stats
        WHERE date >= :startDate AND date <= :endDate AND targetApp = :targetApp
        ORDER BY date DESC
    """)
    suspend fun getDailyStatsInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): List<DailyStatsEntity>

    @Query("""
        SELECT * FROM daily_stats
        WHERE date >= :startDate AND date <= :endDate AND targetApp = :targetApp
        ORDER BY date ASC
    """)
    fun observeDailyStatsInRange(startDate: String, endDate: String, targetApp: String): Flow<List<DailyStatsEntity>>

    @Query("SELECT SUM(totalDuration) FROM daily_stats WHERE date >= :startDate AND date <= :endDate AND targetApp = :targetApp")
    suspend fun getTotalDurationInRange(startDate: String, endDate: String, targetApp: String): Long?

    @Query("SELECT AVG(totalDuration) FROM daily_stats WHERE date >= :startDate AND date <= :endDate AND targetApp = :targetApp")
    suspend fun getAverageDurationInRange(startDate: String, endDate: String, targetApp: String): Long?

    @Query("SELECT MAX(totalDuration) FROM daily_stats WHERE date >= :startDate AND date <= :endDate AND targetApp = :targetApp")
    suspend fun getMaxDurationInRange(startDate: String, endDate: String, targetApp: String): Long?

    @Query("SELECT MAX(longestSession) FROM daily_stats WHERE date >= :startDate AND date <= :endDate AND targetApp = :targetApp")
    suspend fun getLongestSessionInRange(startDate: String, endDate: String, targetApp: String): Long?

    @Query("SELECT SUM(sessionCount) FROM daily_stats WHERE date >= :startDate AND date <= :endDate AND targetApp = :targetApp")
    suspend fun getTotalSessionCountInRange(startDate: String, endDate: String, targetApp: String): Int?

    @Query("DELETE FROM daily_stats WHERE date < :cutoffDate")
    suspend fun deleteStatsOlderThan(cutoffDate: String): Int

    @Query("SELECT * FROM daily_stats WHERE date = :date ORDER BY totalDuration DESC")
    fun observeStatsByDate(date: String): Flow<List<DailyStatsEntity>>
}
