package dev.sadakat.thinkfaster.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sadakat.thinkfaster.data.local.database.entities.UsageEventEntity

@Dao
interface UsageEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: UsageEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<UsageEventEntity>): List<Long>

    @Query("SELECT * FROM usage_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsBySession(sessionId: Long): List<UsageEventEntity>

    @Query("SELECT * FROM usage_events WHERE eventType = :eventType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getEventsByType(eventType: String, limit: Int = 100): List<UsageEventEntity>

    @Query("SELECT COUNT(*) FROM usage_events WHERE eventType = :eventType AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getEventCountByType(eventType: String, startTime: Long, endTime: Long): Int

    @Query("""
        SELECT e.* FROM usage_events e
        INNER JOIN usage_sessions s ON e.sessionId = s.id
        WHERE s.date >= :startDate AND s.date <= :endDate
        ORDER BY e.timestamp DESC
    """)
    suspend fun getEventsInRange(startDate: String, endDate: String): List<UsageEventEntity>

    @Query("""
        SELECT COUNT(*) FROM usage_events e
        INNER JOIN usage_sessions s ON e.sessionId = s.id
        WHERE e.eventType = :eventType AND s.date >= :startDate AND s.date <= :endDate
    """)
    suspend fun countEventsByTypeInRange(eventType: String, startDate: String, endDate: String): Int

    @Query("DELETE FROM usage_events WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteEventsOlderThan(cutoffTimestamp: Long): Int

    // ========== Sync Methods ==========

    @Query("SELECT * FROM usage_events WHERE user_id = :userId")
    suspend fun getEventsByUserId(userId: String): List<UsageEventEntity>

    @Query("SELECT * FROM usage_events WHERE sync_status = :status")
    suspend fun getEventsBySyncStatus(status: String): List<UsageEventEntity>

    @Query("SELECT * FROM usage_events WHERE user_id = :userId AND sync_status = :status")
    suspend fun getEventsByUserAndSyncStatus(userId: String, status: String): List<UsageEventEntity>

    @Query("UPDATE usage_events SET sync_status = :status, cloud_id = :cloudId, last_modified = :lastModified WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, cloudId: String?, lastModified: Long)

    @Query("UPDATE usage_events SET user_id = :userId WHERE id = :id")
    suspend fun updateUserId(id: Long, userId: String)

    @Query("UPDATE usage_events SET last_modified = :lastModified WHERE id = :id")
    suspend fun updateLastModified(id: Long, lastModified: Long)

    @Query("SELECT * FROM usage_events")
    suspend fun getAllEvents(): List<UsageEventEntity>
}
