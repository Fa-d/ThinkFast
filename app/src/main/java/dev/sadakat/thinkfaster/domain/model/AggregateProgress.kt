package dev.sadakat.thinkfaster.domain.model

/**
 * Domain model for aggregate progress across all tracked apps
 * Used by the home screen widget to show combined daily progress
 */
data class AggregateProgress(
    val totalUsedMinutes: Int,         // Total usage across all apps in minutes
    val totalGoalMinutes: Int,         // Sum of all daily goals in minutes
    val appBreakdown: List<AppProgress>,  // Individual app progress for details
    val date: String                   // Date in yyyy-MM-dd format
) {
    /**
     * Percentage of total goal used (0-100+)
     */
    val percentageUsed: Int
        get() = when {
            totalGoalMinutes == 0 -> 0
            else -> ((totalUsedMinutes.toFloat() / totalGoalMinutes.toFloat()) * 100).toInt()
        }

    /**
     * Whether user has exceeded the total goal
     */
    val isOverLimit: Boolean
        get() = totalUsedMinutes > totalGoalMinutes

    /**
     * Minutes remaining before hitting total goal (negative if over)
     */
    val remainingMinutes: Int
        get() = totalGoalMinutes - totalUsedMinutes

    /**
     * Get progress color state
     */
    fun getProgressColor(): ProgressColor {
        return when {
            isOverLimit -> ProgressColor.RED
            percentageUsed >= 90 -> ProgressColor.ORANGE
            percentageUsed >= 75 -> ProgressColor.YELLOW
            else -> ProgressColor.GREEN
        }
    }

    /**
     * Get status message for the widget
     */
    fun getStatusMessage(): String {
        return when {
            isOverLimit -> "Over limit!"
            percentageUsed >= 90 -> "Almost at limit"
            percentageUsed >= 75 -> "Doing well"
            percentageUsed >= 50 -> "On track"
            else -> "Great start!"
        }
    }

    /**
     * Format total usage for display (e.g., "45m" or "1h 30m")
     */
    fun formatTotalUsage(): String {
        return when {
            totalUsedMinutes >= 60 -> {
                val hours = totalUsedMinutes / 60
                val mins = totalUsedMinutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
            else -> "${totalUsedMinutes}m"
        }
    }

    /**
     * Format total goal for display (e.g., "60m" or "2h")
     */
    fun formatTotalGoal(): String {
        return when {
            totalGoalMinutes >= 60 -> {
                val hours = totalGoalMinutes / 60
                val mins = totalGoalMinutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
            else -> "${totalGoalMinutes}m"
        }
    }

    /**
     * Widget display text: "Xm / Ym"
     */
    fun formatWidgetText(): String {
        return "${formatTotalUsage()} / ${formatTotalGoal()}"
    }

    /**
     * Empty state when no goals are set
     */
    companion object {
        fun empty() = AggregateProgress(
            totalUsedMinutes = 0,
            totalGoalMinutes = 0,
            appBreakdown = emptyList(),
            date = ""
        )
    }
}

/**
 * Individual app progress for breakdown display
 */
data class AppProgress(
    val packageName: String,
    val appName: String,
    val usedMinutes: Int,
    val goalMinutes: Int
)
