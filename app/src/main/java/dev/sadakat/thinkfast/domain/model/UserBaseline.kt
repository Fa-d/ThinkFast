package dev.sadakat.thinkfast.domain.model

/**
 * UserBaseline - Domain model for user's baseline usage
 * First-Week Retention Feature - Phase 1.4: Domain Models
 *
 * Represents calculated baseline from user's first week
 * Provides comparison logic and motivational messaging
 */
data class UserBaseline(
    val firstWeekStartDate: String,
    val firstWeekEndDate: String,
    val averageDailyMinutes: Int,
    val facebookAverageMinutes: Int,
    val instagramAverageMinutes: Int
) {
    companion object {
        // Population average (hardcoded benchmark)
        const val POPULATION_AVERAGE_MINUTES = 45
    }

    /**
     * Calculate difference from population average
     * @return Positive if better than average (lower usage), negative if worse
     */
    fun getComparisonToPopulation(): Int {
        return POPULATION_AVERAGE_MINUTES - averageDailyMinutes
    }

    /**
     * Get motivational message based on performance vs population
     * @return Encouraging message with comparison
     */
    fun getComparisonMessage(): String {
        val diff = getComparisonToPopulation()
        return when {
            diff > 0 -> "You're saving $diff min/day vs average user!"
            diff < 0 -> "You're ${-diff} min above average. Let's improve!"
            else -> "You're right at the average. Room to grow!"
        }
    }
}

/**
 * OnboardingQuest - Domain model for 7-day quest state
 * First-Week Retention Feature - Phase 1.4: Domain Models
 *
 * Represents current quest progress and status
 * Used for displaying quest card on home screen
 */
data class OnboardingQuest(
    val isActive: Boolean,
    val currentDay: Int,              // 1-7, or 0 if not active
    val totalDays: Int = 7,
    val daysCompleted: Int,
    val progressPercentage: Float,    // daysCompleted / 7.0
    val isCompleted: Boolean,
    val nextMilestone: String?        // "Complete today to unlock Day 3 reward!"
)

/**
 * QuickWinType - Enum for quick win celebrations
 * First-Week Retention Feature - Phase 1.4: Domain Models
 *
 * Defines types of quick win celebrations for Day 1-3
 */
enum class QuickWinType {
    FIRST_SESSION,        // Day 1: First time tracking a session
    FIRST_UNDER_GOAL,     // Day 1: First session under daily limit
    DAY_ONE_COMPLETE,     // Day 1→2: Successfully completed Day 1
    DAY_TWO_COMPLETE      // Day 2→3: Successfully completed Day 2
    // Day 3 uses existing StreakMilestoneCelebration
}
