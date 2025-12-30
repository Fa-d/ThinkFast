package dev.sadakat.thinkfaster.domain.usecase.notifications

import dev.sadakat.thinkfaster.domain.model.NotificationData
import dev.sadakat.thinkfaster.domain.model.NotificationType
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GenerateEveningNotificationUseCase(
    private val usageRepository: UsageRepository,
    private val goalRepository: GoalRepository
) {
    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    suspend operator fun invoke(): NotificationData {
        val today = DATE_FORMAT.format(Date())
        val goals = goalRepository.getAllGoals()

        var totalUsageMinutes = 0
        var totalGoalMinutes = 0

        goals.forEach { goal ->
            val sessions = usageRepository.getSessionsByAppInRange(goal.targetApp, today, today)
            totalUsageMinutes += (sessions.sumOf { it.duration } / 1000 / 60).toInt()
            totalGoalMinutes += goal.dailyLimitMinutes
        }

        val underGoalBy = totalGoalMinutes - totalUsageMinutes
        val overGoalBy = totalUsageMinutes - totalGoalMinutes

        val message = when {
            totalUsageMinutes <= totalGoalMinutes ->
                "Great job! You used $totalUsageMinutes min today. $underGoalBy min under goal! 🔥"
            overGoalBy <= 10 ->
                "You used $totalUsageMinutes min today. Just $overGoalBy min over. So close!"
            else ->
                "You used $totalUsageMinutes min today ($overGoalBy min over). Tomorrow is a fresh start! 💪"
        }

        // Calculate weekly progress
        val weeklyGoal = totalGoalMinutes * 7
        val weeklyUsage = calculateWeeklyUsage(goals)
        val weeklyProgress = if (weeklyGoal > 0) {
            ((weeklyUsage.toDouble() / weeklyGoal) * 100).toInt()
        } else {
            0
        }

        val fullMessage = "$message\n\nWeekly progress: $weeklyProgress% of goal"

        return NotificationData(
            title = "Day Review 🌙",
            message = fullMessage,
            type = NotificationType.EVENING_REVIEW
        )
    }

    private suspend fun calculateWeeklyUsage(goals: List<dev.sadakat.thinkfaster.domain.model.Goal>): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = DATE_FORMAT.format(calendar.time)

        val today = DATE_FORMAT.format(Date())

        var weeklyUsageMinutes = 0
        goals.forEach { goal ->
            val sessions = usageRepository.getSessionsByAppInRange(goal.targetApp, weekStart, today)
            weeklyUsageMinutes += (sessions.sumOf { it.duration } / 1000 / 60).toInt()
        }

        return weeklyUsageMinutes
    }
}
