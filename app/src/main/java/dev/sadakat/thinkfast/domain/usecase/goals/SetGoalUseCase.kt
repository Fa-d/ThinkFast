package dev.sadakat.thinkfast.domain.usecase.goals

import dev.sadakat.thinkfast.domain.model.Goal
import dev.sadakat.thinkfast.domain.repository.GoalRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Use case for setting or updating a goal for a target app
 */
class SetGoalUseCase(
    private val goalRepository: GoalRepository
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Set or update a goal
     * @param targetApp Package name of the app
     * @param dailyLimitMinutes Daily usage limit in minutes
     */
    suspend operator fun invoke(
        targetApp: String,
        dailyLimitMinutes: Int
    ) {
        // Validate limit
        require(dailyLimitMinutes > 0) {
            "Daily limit must be greater than 0 minutes"
        }
        require(dailyLimitMinutes <= 1440) {
            "Daily limit cannot exceed 24 hours (1440 minutes)"
        }

        // Check if goal already exists
        val existingGoal = goalRepository.getGoalByApp(targetApp)

        val goal = if (existingGoal != null) {
            // Update existing goal, preserve streaks if limit hasn't changed significantly
            val limitsAreSimilar = kotlin.math.abs(existingGoal.dailyLimitMinutes - dailyLimitMinutes) <= 5
            existingGoal.copy(
                dailyLimitMinutes = dailyLimitMinutes,
                lastUpdated = System.currentTimeMillis(),
                // Reset streaks if limit changed significantly
                currentStreak = if (limitsAreSimilar) existingGoal.currentStreak else 0
            )
        } else {
            // Create new goal
            Goal(
                targetApp = targetApp,
                dailyLimitMinutes = dailyLimitMinutes,
                startDate = dateFormatter.format(Date()),
                currentStreak = 0,
                longestStreak = 0,
                lastUpdated = System.currentTimeMillis()
            )
        }

        goalRepository.upsertGoal(goal)
    }
}
