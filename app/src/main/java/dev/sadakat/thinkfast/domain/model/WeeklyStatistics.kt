package dev.sadakat.thinkfast.domain.model

/**
 * Statistics for a week (7 days)
 */
data class WeeklyStatistics(
    val weekStart: String,               // Format: yyyy-MM-dd (Monday)
    val weekEnd: String,                 // Format: yyyy-MM-dd (Sunday)
    val totalUsageMillis: Long,          // Total usage for the week
    val dailyAverage: Long,              // Average daily usage
    val sessionCount: Int,               // Total sessions in the week
    val averageSessionMillis: Long,      // Average session duration
    val longestSessionMillis: Long,      // Longest session in the week
    val mostActiveDay: String?,          // Day with highest usage
    val mostActiveDayUsage: Long,        // Usage on most active day
    val facebookUsageMillis: Long,       // Facebook usage for the week
    val instagramUsageMillis: Long,      // Instagram usage for the week
    val dailyBreakdown: List<DailyStatistics>  // Day-by-day breakdown
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
     * Format longest session as human-readable string
     */
    fun formatLongestSession(): String = formatDuration(longestSessionMillis)

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
