package dev.sadakat.thinkfast.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: String = "current_goal",  // Single active goal
    val dailyLimitMinutes: Int,
    val targetApp: String,
    val startDate: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastUpdated: Long
)
