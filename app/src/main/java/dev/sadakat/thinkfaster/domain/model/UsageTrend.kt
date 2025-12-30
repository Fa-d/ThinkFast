package dev.sadakat.thinkfaster.domain.model

/**
 * Represents a usage trend comparing two time periods
 */
data class UsageTrend(
    val periodName: String,              // e.g., "Week", "Month", "Day"
    val currentUsage: Long,              // Current period usage in millis
    val previousUsage: Long,             // Previous period usage in millis
    val difference: Long,                // Difference in millis (current - previous)
    val percentageChange: Int,           // Percentage change (positive or negative)
    val direction: TrendDirection        // Direction of trend
) {
    /**
     * Format the trend message for display
     */
    fun formatMessage(): String {
        val absPercentage = kotlin.math.abs(percentageChange)
        return when (direction) {
            TrendDirection.INCREASING -> "↑ $absPercentage% more than last $periodName"
            TrendDirection.DECREASING -> "↓ $absPercentage% less than last $periodName"
            TrendDirection.STABLE -> "≈ Similar to last $periodName"
        }
    }

    /**
     * Get emoji representing the trend direction
     */
    fun getEmoji(): String = when (direction) {
        TrendDirection.INCREASING -> "📈"
        TrendDirection.DECREASING -> "📉"
        TrendDirection.STABLE -> "➡️"
    }
}

/**
 * Direction of usage trend
 */
enum class TrendDirection {
    INCREASING,   // Usage is going up (bad)
    DECREASING,   // Usage is going down (good)
    STABLE        // Usage is relatively stable
}
