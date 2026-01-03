package dev.sadakat.thinkfaster.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "daily_stats",
    primaryKeys = ["date", "targetApp"],
    indices = [
        Index(value = ["date"]),
        Index(value = ["targetApp"])
    ]
)
data class DailyStatsEntity(
    val date: String,  // YYYY-MM-DD
    val targetApp: String,
    val totalDuration: Long,
    val sessionCount: Int,
    val longestSession: Long,
    val averageSession: Long,
    val alertsShown: Int,
    val alertsProceeded: Int,
    val lastUpdated: Long,

    // Sync metadata for multi-device sync
    @ColumnInfo(name = "user_id", defaultValue = "NULL")
    val userId: String? = null,

    @ColumnInfo(name = "sync_status", defaultValue = "PENDING")
    val syncStatus: String = "PENDING",

    @ColumnInfo(name = "last_modified", defaultValue = "0")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "cloud_id", defaultValue = "NULL")
    val cloudId: String? = null
)
