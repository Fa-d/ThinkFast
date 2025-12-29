package dev.sadakat.thinkfast.domain.model

/**
 * Domain model for behavioral pattern insights
 * Helps users understand WHEN and WHY they use apps
 */
data class BehavioralInsights(
    val date: String,                        // Date or date range for these insights
    val period: StatsPeriod,                 // DAILY, WEEKLY, or MONTHLY

    // Weekend vs Weekday patterns
    val weekendVsWeekdayRatio: Float,        // e.g., 1.4 means 40% more on weekends
    val weekendUsageMinutes: Int,            // Total weekend usage
    val weekdayUsageMinutes: Int,            // Total weekday usage

    // Late-night habits
    val lateNightSessionCount: Int,          // Sessions between 22:00-05:00
    val lateNightUsageMinutes: Int,          // Total late-night usage

    // Quick reopens (within 2 minutes)
    val quickReopenCount: Int,               // Number of quick reopens

    // Peak usage patterns
    val peakUsageHour: Int,                  // Hour with most usage (0-23)
    val peakUsageContext: String,            // "Morning (9 AM)" or "Evening (9 PM)"
    val sessionsInPeakHour: Int,             // Number of sessions in peak hour
    val peakHourUsageMinutes: Int,           // Usage during peak hour

    // Session clustering (binge patterns)
    val bingeSessionCount: Int,              // Sessions longer than 30 minutes
    val averageSessionMinutes: Int           // Average session duration
) {
    /**
     * Get primary insight message (empowering, non-judgmental tone)
     */
    fun getPrimaryInsight(): String {
        return when {
            quickReopenCount > 2 -> "You reopened apps $quickReopenCount times within 2 minutes"
            lateNightSessionCount > 3 -> "You tend to use apps late at night ($lateNightSessionCount sessions)"
            weekendVsWeekdayRatio > 1.3 -> "Weekend usage is ${((weekendVsWeekdayRatio - 1) * 100).toInt()}% higher than weekdays"
            bingeSessionCount > 3 -> "$bingeSessionCount sessions were longer than 30 minutes"
            else -> "Most activity happens at $peakUsageContext"
        }
    }

    /**
     * Format weekend vs weekday comparison
     */
    fun formatWeekendComparison(): String {
        return when {
            weekendVsWeekdayRatio > 1.2 -> "${((weekendVsWeekdayRatio - 1) * 100).toInt()}% more on weekends"
            weekendVsWeekdayRatio < 0.8 -> "${((1 - weekendVsWeekdayRatio) * 100).toInt()}% more on weekdays"
            else -> "Similar usage on weekends and weekdays"
        }
    }

    /**
     * Get late-night insight message
     */
    fun getLateNightInsight(): String? {
        return if (lateNightSessionCount > 0) {
            "$lateNightSessionCount late-night sessions (10 PM - 5 AM)"
        } else null
    }

    /**
     * Get quick reopen insight message
     */
    fun getQuickReopenInsight(): String? {
        return if (quickReopenCount > 0) {
            "Apps reopened $quickReopenCount times within 2 minutes"
        } else null
    }
}

/**
 * Stats period enum for behavioral insights
 */
enum class StatsPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}
