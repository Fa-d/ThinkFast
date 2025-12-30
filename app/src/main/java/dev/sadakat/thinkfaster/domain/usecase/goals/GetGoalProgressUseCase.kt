package dev.sadakat.thinkfaster.domain.usecase.goals

import dev.sadakat.thinkfaster.domain.model.GoalProgress
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Use case for getting current goal progress
 */
class GetGoalProgressUseCase(
    private val goalRepository: GoalRepository,
    private val usageRepository: UsageRepository
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Get progress for a specific goal
     * @param targetApp Package name of the app
     * @return GoalProgress or null if no goal exists
     */
    suspend operator fun invoke(targetApp: String): GoalProgress? {
        // Get the goal
        val goal = goalRepository.getGoalByApp(targetApp) ?: return null

        // Get today's date
        val today = dateFormatter.format(Date())

        // Get all sessions for today for this app
        val todaySessions = usageRepository.getSessionsByAppInRange(
            targetApp = targetApp,
            startDate = today,
            endDate = today
        )

        // Calculate total usage in minutes
        val totalUsageMillis = todaySessions.sumOf { it.duration }
        val todayUsageMinutes = (totalUsageMillis / 1000 / 60).toInt()

        // Calculate remaining minutes
        val remainingMinutes = goal.dailyLimitMinutes - todayUsageMinutes

        // Calculate percentage used
        val percentageUsed = if (goal.dailyLimitMinutes > 0) {
            ((todayUsageMinutes.toDouble() / goal.dailyLimitMinutes.toDouble()) * 100).toInt()
        } else {
            0
        }

        // Check if over limit
        val isOverLimit = todayUsageMinutes > goal.dailyLimitMinutes

        // Check if streak is at risk (over 80% of limit used)
        val streakAtRisk = percentageUsed >= 80 && !isOverLimit

        return GoalProgress(
            goal = goal,
            todayUsageMinutes = todayUsageMinutes,
            remainingMinutes = remainingMinutes,
            percentageUsed = percentageUsed,
            isOverLimit = isOverLimit,
            streakAtRisk = streakAtRisk
        )
    }

    /**
     * Get progress for all active goals
     */
    suspend fun getAllProgress(): List<GoalProgress> {
        val allGoals = goalRepository.getAllGoals()
        return allGoals.mapNotNull { goal ->
            invoke(goal.targetApp)
        }
    }
}
