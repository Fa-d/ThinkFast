package dev.sadakat.thinkfast.domain.model

/**
 * Domain model for comparative analytics
 * Shows personal bests, improvements, and comparisons over time
 */
data class ComparativeAnalytics(
    val period: String,                      // Current period being analyzed

    // Personal bests
    val personalBests: PersonalBests,

    // Comparisons
    val comparisons: List<ComparisonMetric>,

    // Overall improvement
    val improvementRate: Float?,             // Overall improvement percentage since setting goals
    val consistencyScore: Float,             // How consistent is usage? (0-1, higher is more consistent)
    val daysSinceGoalSet: Int?               // Days since user set their first goal
) {
    /**
     * Get primary insight message (celebrating progress)
     */
    fun getPrimaryInsight(): String {
        return when {
            improvementRate != null && improvementRate > 10 ->
                "You've reduced usage by ${improvementRate.toInt()}% since setting your goal"
            personalBests.longestStreak > 7 ->
                "Personal best: ${personalBests.longestStreak}-day streak!"
            comparisons.any { it.isImprovement && it.percentageDiff > 10 } -> {
                val bestImprovement = comparisons.filter { it.isImprovement }.maxByOrNull { it.percentageDiff }
                "Great progress: ${bestImprovement?.label}"
            }
            else -> "Keep tracking to see your progress over time"
        }
    }

    /**
     * Get consistency message
     */
    fun getConsistencyMessage(): String {
        return when {
            consistencyScore > 0.8 -> "Very consistent usage patterns"
            consistencyScore > 0.6 -> "Moderately consistent usage"
            else -> "Usage varies day to day"
        }
    }

    /**
     * Get improvement summary
     */
    fun getImprovementSummary(): String? {
        return if (improvementRate != null && daysSinceGoalSet != null) {
            "Since setting your goal $daysSinceGoalSet days ago, you've improved by ${improvementRate.toInt()}%"
        } else null
    }
}

/**
 * Personal best records
 */
data class PersonalBests(
    val longestStreak: Int,                  // Longest consecutive days meeting goal
    val lowestUsageDay: UsageRecord?,        // Best single day
    val lowestUsageWeek: UsageRecord?        // Best week
) {
    fun formatLongestStreak(): String {
        return "$longestStreak-day streak"
    }
}

/**
 * Usage record for a specific period
 */
data class UsageRecord(
    val date: String,                        // Date or date range
    val usageMinutes: Int,                   // Total usage
    val percentBelowAverage: Int?            // How much better than average
) {
    fun formatUsage(): String {
        val hours = usageMinutes / 60
        val mins = usageMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }
}

/**
 * Comparison between two periods
 */
data class ComparisonMetric(
    val label: String,                       // "This week vs best week"
    val current: Float,                      // Current value (e.g., minutes)
    val comparison: Float,                   // Comparison value
    val percentageDiff: Float,               // Percentage difference
    val isImprovement: Boolean               // Is this a positive change?
) {
    fun formatComparison(): String {
        val direction = if (isImprovement) "better" else "higher"
        return "$label: ${percentageDiff.toInt()}% $direction"
    }

    fun formatValues(): String {
        val currentHours = (current / 60).toInt()
        val currentMins = (current % 60).toInt()
        val compHours = (comparison / 60).toInt()
        val compMins = (comparison % 60).toInt()

        return "${currentHours}h ${currentMins}m vs ${compHours}h ${compMins}m"
    }
}
