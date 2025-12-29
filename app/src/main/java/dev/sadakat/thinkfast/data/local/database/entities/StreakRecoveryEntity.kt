package dev.sadakat.thinkfast.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak_recovery")
data class StreakRecoveryEntity(
    @PrimaryKey
    val targetApp: String,  // One recovery per app (e.g., "com.facebook.katana")
    val previousStreak: Int,  // The streak that was broken
    val recoveryStartDate: String,  // When recovery started (yyyy-MM-dd)
    val currentRecoveryDays: Int,  // Days back on track (0 initially)
    val isRecoveryComplete: Boolean,  // True when "back on track" milestone reached
    val recoveryCompletedDate: String?,  // When recovery was completed
    val notificationShown: Boolean,  // Whether we've shown the initial break notification
    val timestamp: Long  // When this recovery started
)
