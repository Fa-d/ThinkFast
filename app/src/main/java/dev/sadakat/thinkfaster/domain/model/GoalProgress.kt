package dev.sadakat.thinkfaster.domain.model

/**
 * Domain model for tracking goal progress
 */
data class GoalProgress(
    val goal: Goal,                  // The goal being tracked
    val todayUsageMinutes: Int,      // Usage today in minutes
    val remainingMinutes: Int,       // Minutes remaining before limit
    val percentageUsed: Int,         // Percentage of daily limit used (0-100+)
    val isOverLimit: Boolean,        // Whether user exceeded limit
    val streakAtRisk: Boolean        // Whether current streak is at risk
) {
    /**
     * Format today's usage for display
     */
    fun formatTodayUsage(): String {
        return when {
            todayUsageMinutes >= 60 -> {
                val hours = todayUsageMinutes / 60
                val mins = todayUsageMinutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
            else -> "${todayUsageMinutes}m"
        }
    }

    /**
     * Format remaining time for display
     */
    fun formatRemainingTime(): String {
        return when {
            isOverLimit -> "Over limit by ${kotlin.math.abs(remainingMinutes)}m"
            remainingMinutes >= 60 -> {
                val hours = remainingMinutes / 60
                val mins = remainingMinutes % 60
                if (mins > 0) "${hours}h ${mins}m left" else "${hours}h left"
            }
            else -> "${remainingMinutes}m left"
        }
    }

    /**
     * Get progress status message
     */
    fun getStatusMessage(): String {
        return when {
            isOverLimit -> "🔴 Over limit!"
            percentageUsed >= 90 -> "🟡 Almost at limit"
            percentageUsed >= 75 -> "🟢 Doing well"
            percentageUsed >= 50 -> "🟢 On track"
            else -> "🟢 Great start!"
        }
    }

    /**
     * Get progress color (for UI)
     */
    fun getProgressColor(): ProgressColor {
        return when {
            isOverLimit -> ProgressColor.RED
            percentageUsed >= 90 -> ProgressColor.ORANGE
            percentageUsed >= 75 -> ProgressColor.YELLOW
            else -> ProgressColor.GREEN
        }
    }
}

/**
 * Progress color indicator
 */
enum class ProgressColor {
    GREEN,
    YELLOW,
    ORANGE,
    RED
}
