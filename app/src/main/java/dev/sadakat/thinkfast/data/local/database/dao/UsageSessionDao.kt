package dev.sadakat.thinkfast.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.sadakat.thinkfast.data.local.database.entities.UsageSessionEntity
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
}
