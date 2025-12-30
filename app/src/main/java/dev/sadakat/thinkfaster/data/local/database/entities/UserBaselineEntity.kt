package dev.sadakat.thinkfaster.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * UserBaselineEntity - Stores calculated baseline from first week
 * First-Week Retention Feature - Phase 1.2: Data Layer
 *
 * Represents user's baseline usage calculated from their first 7 days
 * Single row per user (id always = 1)
 */
@Entity(tableName = "user_baseline")
data class UserBaselineEntity(
    @PrimaryKey
    val id: Int = 1,  // Single row (user-wide baseline)

    val firstWeekStartDate: String,           // "yyyy-MM-dd"
    val firstWeekEndDate: String,             // "yyyy-MM-dd"
    val totalUsageMinutes: Int,               // Sum of all usage in first week
    val averageDailyMinutes: Int,             // totalUsage / 7
    val facebookAverageMinutes: Int,          // Facebook baseline
    val instagramAverageMinutes: Int,         // Instagram baseline
    val calculatedDate: String,               // When baseline was calculated
    val timestamp: Long                       // System.currentTimeMillis()
)
