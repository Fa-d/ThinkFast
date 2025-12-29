package dev.sadakat.thinkfast.domain.model

/**
 * Domain model for AI-powered smart insights
 * Selects the most relevant insight to show the user RIGHT NOW
 * based on priority algorithm and current context
 */
data class SmartInsight(
    val type: InsightType,                   // Type of insight
    val priority: InsightPriority,           // Priority level
    val message: String,                     // Main insight message
    val icon: String,                        // Emoji icon for visual appeal
    val details: String?,                    // Optional detailed explanation
    val actionable: Boolean,                 // Is this actionable right now?
    val contextData: Map<String, Any>        // Additional context for UI
) {
    /**
     * Get formatted display message with icon
     */
    fun getDisplayMessage(): String {
        return "$icon $message"
    }

    /**
     * Get color code based on priority
     */
    fun getColorCode(): String {
        return when (priority) {
            InsightPriority.URGENT -> "#FF5252"       // Red
            InsightPriority.HIGH -> "#FFC107"         // Amber
            InsightPriority.MEDIUM -> "#2196F3"       // Blue
            InsightPriority.LOW -> "#4CAF50"          // Green
        }
    }
}

/**
 * Types of smart insights
 */
enum class InsightType {
    // Urgent/Actionable
    STREAK_AT_RISK,              // "Heads up: you're at 80% of your daily goal"
    GOAL_ALMOST_REACHED,         // "You're at 90% of your goal - doing great!"

    // Behavioral Patterns
    QUICK_REOPEN_PATTERN,        // "Apps reopened 3 times within 2 minutes today"
    LATE_NIGHT_PATTERN,          // "You tend to use apps after 10 PM"
    WEEKEND_PATTERN,             // "Weekend usage is 40% higher"
    PEAK_TIME_PATTERN,           // "Most activity happens at 9 PM"

    // Intervention Effectiveness
    INTERVENTION_SUCCESS,        // "Reflection questions work well for you (65%)"
    IMPROVING_EFFECTIVENESS,     // "Interventions are becoming more effective"

    // Predictive
    HIGH_RISK_TIME_AHEAD,        // "You usually open Instagram around 9 PM"
    GOAL_ACHIEVEMENT_LIKELY,     // "At this rate, you'll use 45 min today"

    // Comparative
    PERSONAL_BEST,               // "Personal best: 7-day streak!"
    IMPROVEMENT_SINCE_GOAL,      // "You've reduced usage by 22%"
    THIS_WEEK_VS_BEST,           // "Your best week was 2h 15m"

    // General Awareness
    ON_TRACK,                    // "You're on track with your goal"
    NO_DATA_YET                  // "Keep using the app to see insights"
}

/**
 * Priority levels for insight selection
 */
enum class InsightPriority {
    URGENT,      // Needs immediate attention (streak at risk, goal almost exceeded)
    HIGH,        // Important but not urgent (interesting patterns discovered)
    MEDIUM,      // Informative (positive reinforcement, effectiveness)
    LOW          // General awareness (on track, no specific action needed)
}

/**
 * Builder for creating smart insights from different data sources
 */
object SmartInsightBuilder {
    /**
     * Create streak risk insight
     */
    fun streakAtRisk(currentPercentage: Int): SmartInsight {
        return SmartInsight(
            type = InsightType.STREAK_AT_RISK,
            priority = InsightPriority.URGENT,
            message = "Heads up: you're at $currentPercentage% of your daily goal",
            icon = "⚡",
            details = "Your streak might be at risk. You're approaching your daily limit.",
            actionable = true,
            contextData = mapOf("percentage" to currentPercentage)
        )
    }

    /**
     * Create goal almost reached insight
     */
    fun goalAlmostReached(remainingMinutes: Int): SmartInsight {
        return SmartInsight(
            type = InsightType.GOAL_ALMOST_REACHED,
            priority = InsightPriority.URGENT,
            message = "You're at 90% of your goal - ${remainingMinutes}m remaining",
            icon = "🎯",
            details = "Great job! You're very close to meeting your daily goal.",
            actionable = true,
            contextData = mapOf("remainingMinutes" to remainingMinutes)
        )
    }

    /**
     * Create quick reopen pattern insight
     */
    fun quickReopenPattern(count: Int): SmartInsight {
        return SmartInsight(
            type = InsightType.QUICK_REOPEN_PATTERN,
            priority = InsightPriority.HIGH,
            message = "You reopened apps $count times within 2 minutes today",
            icon = "🔄",
            details = "Quick reopens can indicate habitual checking. Try waiting a few minutes before reopening.",
            actionable = true,
            contextData = mapOf("count" to count)
        )
    }

    /**
     * Create late night pattern insight
     */
    fun lateNightPattern(sessionCount: Int, totalMinutes: Int): SmartInsight {
        return SmartInsight(
            type = InsightType.LATE_NIGHT_PATTERN,
            priority = InsightPriority.HIGH,
            message = "You tend to use apps late at night ($sessionCount sessions)",
            icon = "🌙",
            details = "Late-night usage totaled ${totalMinutes}m this period.",
            actionable = false,
            contextData = mapOf("sessions" to sessionCount, "minutes" to totalMinutes)
        )
    }

    /**
     * Create weekend pattern insight
     */
    fun weekendPattern(percentageHigher: Int): SmartInsight {
        return SmartInsight(
            type = InsightType.WEEKEND_PATTERN,
            priority = InsightPriority.HIGH,
            message = "Weekend usage is $percentageHigher% higher than weekdays",
            icon = "📅",
            details = "You use apps more on weekends. This is common for many people.",
            actionable = false,
            contextData = mapOf("percentageDiff" to percentageHigher)
        )
    }

    /**
     * Create intervention success insight
     */
    fun interventionSuccess(contentType: String, successRate: Int): SmartInsight {
        return SmartInsight(
            type = InsightType.INTERVENTION_SUCCESS,
            priority = InsightPriority.MEDIUM,
            message = "$contentType helps you go back $successRate% of the time",
            icon = "💡",
            details = "This reminder type is particularly effective for you.",
            actionable = false,
            contextData = mapOf("contentType" to contentType, "successRate" to successRate)
        )
    }

    /**
     * Create personal best insight
     */
    fun personalBest(streakDays: Int): SmartInsight {
        return SmartInsight(
            type = InsightType.PERSONAL_BEST,
            priority = InsightPriority.MEDIUM,
            message = "Personal best: $streakDays-day streak!",
            icon = "🏆",
            details = "This is your longest streak of meeting your daily goal.",
            actionable = false,
            contextData = mapOf("days" to streakDays)
        )
    }

    /**
     * Create improvement since goal insight
     */
    fun improvementSinceGoal(percentageReduction: Int): SmartInsight {
        return SmartInsight(
            type = InsightType.IMPROVEMENT_SINCE_GOAL,
            priority = InsightPriority.MEDIUM,
            message = "You've reduced usage by $percentageReduction% since setting your goal",
            icon = "📈",
            details = "Great progress! Keep up the good work.",
            actionable = false,
            contextData = mapOf("improvement" to percentageReduction)
        )
    }

    /**
     * Create on track insight
     */
    fun onTrack(projectedMinutes: Int): SmartInsight {
        return SmartInsight(
            type = InsightType.ON_TRACK,
            priority = InsightPriority.LOW,
            message = "At this rate, you'll use ${projectedMinutes}m today - right on track!",
            icon = "🟢",
            details = "You're making good progress toward your daily goal.",
            actionable = false,
            contextData = mapOf("projected" to projectedMinutes)
        )
    }

    /**
     * Create peak time pattern insight
     */
    fun peakTimePattern(timeContext: String): SmartInsight {
        return SmartInsight(
            type = InsightType.PEAK_TIME_PATTERN,
            priority = InsightPriority.LOW,
            message = "Most activity happens at $timeContext",
            icon = "📊",
            details = "This is when you use apps most frequently.",
            actionable = false,
            contextData = mapOf("peakTime" to timeContext)
        )
    }
}
