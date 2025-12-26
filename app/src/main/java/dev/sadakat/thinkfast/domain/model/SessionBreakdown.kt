package dev.sadakat.thinkfast.domain.model

/**
 * Detailed breakdown of sessions for a date range
 */
data class SessionBreakdown(
    val startDate: String,               // Start of date range (yyyy-MM-dd)
    val endDate: String,                 // End of date range (yyyy-MM-dd)
    val sessions: List<UsageSession>,    // All sessions in the range
    val totalSessions: Int,              // Total number of sessions
    val totalDuration: Long,             // Sum of all session durations
    val averageDuration: Long,           // Average session duration
    val longestSession: UsageSession?,   // Longest session
    val shortestSession: UsageSession?   // Shortest session (excluding very short ones)
) {
    /**
     * Format total duration as human-readable string
     */
    fun formatTotalDuration(): String = formatDuration(totalDuration)

    /**
     * Format average duration as human-readable string
     */
    fun formatAverageDuration(): String = formatDuration(averageDuration)

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
