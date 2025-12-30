package dev.sadakat.thinkfaster.domain.model

/**
 * Domain model for predictive insights
 * Helps users anticipate and prepare for high-risk times
 */
data class PredictiveInsights(
    val date: String,                        // Date for predictions

    // Streak risk
    val streakAtRisk: Boolean,               // Is current streak at risk?
    val streakRiskReason: String?,           // Why streak is at risk
    val currentUsagePercentage: Int,         // Percentage of daily goal used

    // End-of-day projection
    val projectedEndOfDayUsageMinutes: Int,  // Projected usage by end of day
    val willMeetGoal: Boolean,               // Likely to meet goal?
    val goalAchievementProbability: Float,   // Probability (0-1)

    // High-risk time predictions
    val highRiskTimeWindows: List<HighRiskTimeWindow>,
    val nextHighRiskTime: HighRiskTimeWindow?,

    // Recommended actions
    val recommendedActions: List<String>     // Actionable suggestions
) {
    /**
     * Get primary insight message (empowering, non-judgmental)
     */
    fun getPrimaryInsight(): String {
        return when {
            streakAtRisk -> "Heads up: you're at $currentUsagePercentage% of your daily goal"
            willMeetGoal -> "At this rate, you'll use ${projectedEndOfDayUsageMinutes}m today - right on track!"
            else -> "Projected usage: ${projectedEndOfDayUsageMinutes}m by end of day"
        }
    }

    /**
     * Get streak warning message
     */
    fun getStreakWarning(): String? {
        return if (streakAtRisk && streakRiskReason != null) {
            "⚡ Streak alert: $streakRiskReason"
        } else null
    }

    /**
     * Get high-risk time alert
     */
    fun getHighRiskAlert(): String? {
        return nextHighRiskTime?.let {
            "You usually open apps around ${it.timeLabel}"
        }
    }

    /**
     * Format goal probability
     */
    fun formatGoalProbability(): String {
        val percentage = (goalAchievementProbability * 100).toInt()
        return when {
            percentage >= 80 -> "Very likely to meet goal ($percentage%)"
            percentage >= 60 -> "Likely to meet goal ($percentage%)"
            percentage >= 40 -> "May exceed goal ($percentage% chance of meeting it)"
            else -> "At risk of exceeding goal"
        }
    }
}

/**
 * High-risk time window based on historical patterns
 */
data class HighRiskTimeWindow(
    val timeLabel: String,                   // "9 PM" or "Evening"
    val hourOfDay: Int,                      // 0-23
    val historicalFrequency: Int,            // How often user opens apps at this time
    val averageUsageMinutes: Int,            // Average usage during this window
    val confidence: Float                     // Confidence level (0-1)
)
