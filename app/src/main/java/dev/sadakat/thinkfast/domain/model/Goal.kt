package dev.sadakat.thinkfast.domain.model

/**
 * Domain model for a usage goal
 */
data class Goal(
    val targetApp: String,           // Package name of target app
    val dailyLimitMinutes: Int,      // Daily usage limit in minutes
    val startDate: String,           // Date goal was set (yyyy-MM-dd)
    val currentStreak: Int,          // Current days meeting goal
    val longestStreak: Int,          // Longest streak ever achieved
    val lastUpdated: Long            // Timestamp of last update
) {
    /**
     * Format daily limit for display
     */
    fun formatDailyLimit(): String {
        return when {
            dailyLimitMinutes >= 60 -> {
                val hours = dailyLimitMinutes / 60
                val mins = dailyLimitMinutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
            else -> "${dailyLimitMinutes}m"
        }
    }

    /**
     * Get app display name from package name
     */
    fun getAppDisplayName(): String {
        return when (targetApp) {
            AppTarget.FACEBOOK.packageName -> "Facebook"
            AppTarget.INSTAGRAM.packageName -> "Instagram"
            else -> targetApp
        }
    }

    /**
     * Check if currently on a streak
     */
    fun hasActiveStreak(): Boolean = currentStreak > 0

    /**
     * Get streak status message
     */
    fun getStreakMessage(): String {
        return when {
            currentStreak == 0 -> "No active streak"
            currentStreak == 1 -> "1 day streak!"
            else -> "$currentStreak days streak!"
        }
    }

    /**
     * Get longest streak message
     */
    fun getLongestStreakMessage(): String {
        return when {
            longestStreak == 0 -> "No streaks yet"
            longestStreak == 1 -> "Best: 1 day"
            else -> "Best: $longestStreak days"
        }
    }
}
