package dev.sadakat.thinkfaster.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val targetApp: String,  // One goal per app
    val dailyLimitMinutes: Int,
    val startDate: String,
    val currentStreak: Int,
    val longestStreak: Int,
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
