package dev.sadakat.thinkfast.domain.usecase.goals

import dev.sadakat.thinkfast.domain.repository.GoalRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Use case for updating goal streaks based on daily performance
 * Should be run daily (midnight) by DailyStatsAggregatorWorker
 */
class UpdateStreakUseCase(
    private val goalRepository: GoalRepository,
    private val usageRepository: UsageRepository
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Update streaks for all active goals based on yesterday's performance
     * This should be called at the start of a new day to evaluate the previous day
     */
    suspend operator fun invoke() {
        // Get yesterday's date
        val yesterday = getYesterdayDate()

        // Get all active goals
        val allGoals = goalRepository.getAllGoals()

        allGoals.forEach { goal ->
            updateStreakForGoal(goal.targetApp, yesterday)
        }
    }

    /**
     * Update streak for a specific goal
     * @param targetApp Package name of the app
     * @param date Date to check (usually yesterday)
     */
    suspend fun updateStreakForGoal(targetApp: String, date: String) {
        // Get the goal
        val goal = goalRepository.getGoalByApp(targetApp) ?: return

        // Get all sessions for the date
        val sessions = usageRepository.getSessionsByAppInRange(
            targetApp = targetApp,
            startDate = date,
            endDate = date
        )

        // Calculate total usage in minutes
        val totalUsageMillis = sessions.sumOf { it.duration }
        val usageMinutes = (totalUsageMillis / 1000 / 60).toInt()

        // Check if goal was met
        val goalMet = usageMinutes <= goal.dailyLimitMinutes

        if (goalMet) {
            // Increment streak
            val newStreak = goal.currentStreak + 1
            goalRepository.updateCurrentStreak(targetApp, newStreak)

            // Update best streak if needed
            goalRepository.updateBestStreakIfNeeded(targetApp, newStreak)
        } else {
            // Reset streak
            goalRepository.resetCurrentStreak(targetApp)
        }
    }

    /**
     * Force check today's progress (for real-time feedback)
     * @param targetApp Package name of the app
     * @return true if currently meeting goal, false otherwise
     */
    suspend fun checkTodayProgress(targetApp: String): Boolean {
        val today = dateFormatter.format(Date())
        val goal = goalRepository.getGoalByApp(targetApp) ?: return false

        val sessions = usageRepository.getSessionsByAppInRange(
            targetApp = targetApp,
            startDate = today,
            endDate = today
        )

        val totalUsageMillis = sessions.sumOf { it.duration }
        val usageMinutes = (totalUsageMillis / 1000 / 60).toInt()

        return usageMinutes <= goal.dailyLimitMinutes
    }

    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return dateFormatter.format(calendar.time)
    }
}
