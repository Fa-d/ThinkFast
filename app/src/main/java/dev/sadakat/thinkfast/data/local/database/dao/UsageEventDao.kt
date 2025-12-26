package dev.sadakat.thinkfast.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sadakat.thinkfast.data.local.database.entities.UsageEventEntity

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
}
