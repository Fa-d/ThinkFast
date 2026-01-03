package dev.sadakat.thinkfaster.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.sadakat.thinkfaster.data.local.database.entities.UsageSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UsageSessionEntity): Long

    @Update
    suspend fun updateSession(session: UsageSessionEntity)

    @Query("SELECT * FROM usage_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): UsageSessionEntity?

    @Query("SELECT * FROM usage_sessions WHERE date = :date ORDER BY startTimestamp DESC")
    suspend fun getSessionsByDate(date: String): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_sessions WHERE date = :date AND targetApp = :targetApp ORDER BY startTimestamp DESC")
    suspend fun getSessionsByDateAndApp(date: String, targetApp: String): List<UsageSessionEntity>

    @Query("""
        SELECT * FROM usage_sessions
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY startTimestamp DESC
    """)
    suspend fun getSessionsInRange(startDate: String, endDate: String): List<UsageSessionEntity>

    @Query("""
        SELECT * FROM usage_sessions
        WHERE date >= :startDate AND date <= :endDate AND targetApp = :targetApp
        ORDER BY startTimestamp DESC
    """)
    suspend fun getSessionsInRangeForApp(
        startDate: String,
        endDate: String,
        targetApp: String
    ): List<UsageSessionEntity>

    @Query("""
        SELECT * FROM usage_sessions
        WHERE date >= :startDate AND date <= :endDate AND targetApp = :targetApp
        ORDER BY startTimestamp DESC
    """)
    suspend fun getSessionsByAppInRange(
        targetApp: String,
        startDate: String,
        endDate: String
    ): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_sessions WHERE endTimestamp IS NULL LIMIT 1")
    suspend fun getActiveSession(): UsageSessionEntity?

    @Query("UPDATE usage_sessions SET endTimestamp = :endTimestamp, duration = :endTimestamp - startTimestamp, wasInterrupted = :wasInterrupted, interruptionType = :interruptionType WHERE id = :sessionId")
    suspend fun endSession(
        sessionId: Long,
        endTimestamp: Long,
        wasInterrupted: Boolean,
        interruptionType: String?
    )

    @Query("SELECT * FROM usage_sessions ORDER BY duration DESC LIMIT 1")
    suspend fun getLongestSession(): UsageSessionEntity?

    @Query("""
        SELECT * FROM usage_sessions
        WHERE date >= :startDate AND date <= :endDate AND targetApp = :targetApp
        ORDER BY duration DESC
        LIMIT 1
    """)
    suspend fun getLongestSessionInRange(
        targetApp: String,
        startDate: String,
        endDate: String
    ): UsageSessionEntity?

    @Query("SELECT COUNT(*) FROM usage_sessions WHERE date = :date")
    suspend fun getSessionCountForDate(date: String): Int

    @Query("SELECT SUM(duration) FROM usage_sessions WHERE date = :date AND targetApp = :targetApp")
    suspend fun getTotalDurationForDate(date: String, targetApp: String): Long?

    @Query("DELETE FROM usage_sessions WHERE date < :cutoffDate")
    suspend fun deleteSessionsOlderThan(cutoffDate: String): Int

    @Query("SELECT * FROM usage_sessions WHERE date = :date ORDER BY startTimestamp DESC")
    fun observeSessionsByDate(date: String): Flow<List<UsageSessionEntity>>

    // Phase 2: Behavioral pattern queries

    /**
     * Get sessions that occurred during late night hours (22:00 - 05:00)
     */
    @Query("""
        SELECT * FROM usage_sessions
        WHERE date >= :startDate AND date <= :endDate
        AND (
            CAST(strftime('%H', datetime(startTimestamp/1000, 'unixepoch', 'localtime')) AS INTEGER) >= 22
            OR CAST(strftime('%H', datetime(startTimestamp/1000, 'unixepoch', 'localtime')) AS INTEGER) <= 5
        )
        ORDER BY startTimestamp DESC
    """)
    suspend fun getLateNightSessions(startDate: String, endDate: String): List<UsageSessionEntity>

    /**
     * Get sessions by day of week (0=Sunday, 1=Monday, etc.)
     * Useful for weekend vs weekday analysis
     */
    @Query("""
        SELECT * FROM usage_sessions
        WHERE date >= :startDate AND date <= :endDate
        AND CAST(strftime('%w', date) AS INTEGER) IN (:daysOfWeek)
        ORDER BY startTimestamp DESC
    """)
    suspend fun getSessionsByDayOfWeek(
        startDate: String,
        endDate: String,
        daysOfWeek: List<Int>  // 0=Sunday, 6=Saturday
    ): List<UsageSessionEntity>

    /**
     * Get sessions longer than specified duration (for binge detection)
     */
    @Query("""
        SELECT * FROM usage_sessions
        WHERE date >= :startDate AND date <= :endDate
        AND duration >= :minDurationMillis
        ORDER BY duration DESC
    """)
    suspend fun getLongSessions(
        startDate: String,
        endDate: String,
        minDurationMillis: Long
    ): List<UsageSessionEntity>

    /**
     * Get all sessions grouped by hour of day (for peak time analysis)
     * Returns sessions with their hour extracted
     */
    @Query("""
        SELECT
            *,
            CAST(strftime('%H', datetime(startTimestamp/1000, 'unixepoch', 'localtime')) AS INTEGER) as hourOfDay
        FROM usage_sessions
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY startTimestamp ASC
    """)
    suspend fun getSessionsWithHourOfDay(startDate: String, endDate: String): List<UsageSessionEntity>

    // ========== Sync Methods ==========

    @Query("SELECT * FROM usage_sessions WHERE user_id = :userId")
    suspend fun getSessionsByUserId(userId: String): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_sessions WHERE sync_status = :status")
    suspend fun getSessionsBySyncStatus(status: String): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_sessions WHERE user_id = :userId AND sync_status = :status")
    suspend fun getSessionsByUserAndSyncStatus(userId: String, status: String): List<UsageSessionEntity>

    @Query("UPDATE usage_sessions SET sync_status = :status, cloud_id = :cloudId, last_modified = :lastModified WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, cloudId: String?, lastModified: Long)

    @Query("UPDATE usage_sessions SET user_id = :userId WHERE id = :id")
    suspend fun updateUserId(id: Long, userId: String)

    @Query("UPDATE usage_sessions SET last_modified = :lastModified WHERE id = :id")
    suspend fun updateLastModified(id: Long, lastModified: Long)

    @Query("SELECT * FROM usage_sessions")
    suspend fun getAllSessions(): List<UsageSessionEntity>
}
