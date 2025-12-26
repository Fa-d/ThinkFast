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

    @Query("DELETE FROM usage_events WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteEventsOlderThan(cutoffTimestamp: Long): Int
}
