package dev.sadakat.thinkfast.data.local.database.entities

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
    val lastUpdated: Long
)
