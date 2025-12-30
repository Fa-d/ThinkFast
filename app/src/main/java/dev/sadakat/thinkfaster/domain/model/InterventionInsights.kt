package dev.sadakat.thinkfaster.domain.model

/**
 * Domain model for intervention effectiveness insights
 * Shows which reminder types work best and when
 */
data class InterventionInsights(
    val period: String,                      // Date range for insights

    // Overall effectiveness
    val totalInterventions: Int,             // Total interventions shown
    val successCount: Int,                   // Times user went back (GO_BACK)
    val proceedCount: Int,                   // Times user proceeded anyway
    val overallSuccessRate: Float,           // Success rate as percentage (0-100)

    // Content type effectiveness
    val contentTypeEffectiveness: List<ContentTypeEffectiveness>,
    val mostEffectiveType: ContentTypeEffectiveness?,
    val leastEffectiveType: ContentTypeEffectiveness?,

    // Time-based effectiveness
    val timeBasedEffectiveness: Map<String, Float>,  // "Morning": 65.0, "Evening": 45.0
    val bestTimeForSuccess: TimeWindow?,

    // Context-based effectiveness
    val contextBasedEffectiveness: Map<String, Float>,  // "Late Night": 70.0, "Quick Reopen": 45.0

    // Decision-making patterns
    val avgDecisionTimeSeconds: Double,      // Average time to make decision
    val trendingUp: Boolean                  // Is effectiveness improving over time?
) {
    /**
     * Get primary insight message (empowering tone)
     */
    fun getPrimaryInsight(): String {
        return when {
            mostEffectiveType != null && mostEffectiveType.successRate > 60 ->
                "${mostEffectiveType.displayName} helps you go back ${mostEffectiveType.successRate.toInt()}% of the time"
            overallSuccessRate > 50 ->
                "Reminders help you go back ${overallSuccessRate.toInt()}% of the time"
            totalInterventions > 0 ->
                "$totalInterventions reminders shown this period"
            else -> "No interventions yet this period"
        }
    }

    /**
     * Get decision time insight
     */
    fun getDecisionTimeInsight(): String {
        val seconds = avgDecisionTimeSeconds.toInt()
        return when {
            seconds < 3 -> "You decide quickly (${seconds}s average) - very mindful!"
            seconds < 5 -> "You take ${seconds}s on average to decide"
            else -> "You take time to reflect (${seconds}s average)"
        }
    }

    /**
     * Format decision time for display
     */
    fun formatDecisionTime(): String {
        val seconds = avgDecisionTimeSeconds.toInt()
        return when {
            seconds < 60 -> "${seconds}s"
            else -> {
                val mins = seconds / 60
                val secs = seconds % 60
                if (secs > 0) "${mins}m ${secs}s" else "${mins}m"
            }
        }
    }

    /**
     * Get trending message
     */
    fun getTrendingMessage(): String? {
        return if (trendingUp) {
            "Interventions are becoming more effective over time"
        } else null
    }
}

/**
 * Effectiveness data for a specific content type
 */
data class ContentTypeEffectiveness(
    val contentType: String,                 // e.g., "ReflectionQuestion"
    val displayName: String,                 // e.g., "Reflection Questions"
    val totalShown: Int,                     // How many times shown
    val successCount: Int,                   // Times user went back
    val successRate: Float                   // Success rate as percentage
) {
    fun formatEffectiveness(): String {
        return "${displayName}: ${successRate.toInt()}% effective (${successCount}/${totalShown})"
    }
}

/**
 * Time window effectiveness
 */
data class TimeWindow(
    val label: String,                       // "Morning (6 AM - 12 PM)"
    val successRate: Float,                  // Success rate as percentage
    val totalInterventions: Int              // Number of interventions in this window
)
