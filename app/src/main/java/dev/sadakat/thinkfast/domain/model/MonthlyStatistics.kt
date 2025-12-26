package dev.sadakat.thinkfast.domain.model

/**
 * Statistics for a month
 */
data class MonthlyStatistics(
    val month: String,                   // Format: yyyy-MM
    val monthName: String,               // e.g., "January 2025"
    val totalUsageMillis: Long,          // Total usage for the month
    val dailyAverage: Long,              // Average daily usage
    val sessionCount: Int,               // Total sessions in the month
    val averageSessionMillis: Long,      // Average session duration
    val longestSessionMillis: Long,      // Longest session in the month
    val mostActiveDay: String?,          // Day with highest usage
    val mostActiveDayUsage: Long,        // Usage on most active day
    val facebookUsageMillis: Long,       // Facebook usage for the month
    val instagramUsageMillis: Long,      // Instagram usage for the month
    val weeklyBreakdown: List<WeeklyStatistics>,  // Week-by-week breakdown
    val yearlyProjection: Long           // Projected yearly usage based on this month
) {
    /**
     * Format total usage as human-readable string
     */
    fun formatTotalUsage(): String = formatDuration(totalUsageMillis)

    /**
     * Format daily average as human-readable string
     */
    fun formatDailyAverage(): String = formatDuration(dailyAverage)

    /**
     * Format yearly projection as human-readable string
     */
    fun formatYearlyProjection(): String {
        val hours = yearlyProjection / (1000 * 60 * 60)
        val days = hours / 24

        return when {
            days > 0 -> "${days} days (${hours} hours)"
            hours > 0 -> "${hours} hours"
            else -> "<1 hour"
        }
    }

    private fun formatDuration(durationMillis: Long): String {
        val hours = durationMillis / (1000 * 60 * 60)
        val minutes = (durationMillis / (1000 * 60)) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }
}
