package dev.sadakat.thinkfast.data.local.database.entities

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
    val lastUpdated: Long
)
