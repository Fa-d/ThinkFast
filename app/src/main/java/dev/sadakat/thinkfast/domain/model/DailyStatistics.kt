package dev.sadakat.thinkfast.domain.model

/**
 * Statistics for a single day
 */
data class DailyStatistics(
    val date: String,                    // Format: yyyy-MM-dd
    val totalUsageMillis: Long,          // Total usage time for all apps
    val sessionCount: Int,               // Number of sessions
    val averageSessionMillis: Long,      // Average session duration
    val longestSessionMillis: Long,      // Longest session duration
    val facebookUsageMillis: Long,       // Facebook-specific usage
    val instagramUsageMillis: Long,      // Instagram-specific usage
    val reminderShownCount: Int,         // How many reminders shown
    val timerAlertsCount: Int,           // How many 10-minute alerts shown
    val proceedClickCount: Int           // How many times user clicked proceed
) {
    /**
     * Format total usage as human-readable string
     */
    fun formatTotalUsage(): String = formatDuration(totalUsageMillis)

    /**
     * Format average session as human-readable string
     */
    fun formatAverageSession(): String = formatDuration(averageSessionMillis)

    /**
     * Format longest session as human-readable string
     */
    fun formatLongestSession(): String = formatDuration(longestSessionMillis)

    private fun formatDuration(durationMillis: Long): String {
        val hours = durationMillis / (1000 * 60 * 60)
        val minutes = (durationMillis / (1000 * 60)) % 60
        val seconds = (durationMillis / 1000) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
