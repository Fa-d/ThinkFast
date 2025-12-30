package dev.sadakat.thinkfaster.domain.usecase.notifications

import dev.sadakat.thinkfaster.domain.model.NotificationData
import dev.sadakat.thinkfaster.domain.model.NotificationType
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GenerateStreakWarningNotificationUseCase(
    private val goalRepository: GoalRepository,
    private val usageRepository: UsageRepository
) {
    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    suspend operator fun invoke(): NotificationData {
        val goals = goalRepository.getAllGoals()
        val maxStreak = goals.maxOfOrNull { it.currentStreak } ?: 0

        val today = DATE_FORMAT.format(Date())
        var totalUsageMinutes = 0

        goals.forEach { goal ->
            val sessions = usageRepository.getSessionsByAppInRange(goal.targetApp, today, today)
            totalUsageMinutes += (sessions.sumOf { it.duration } / 1000 / 60).toInt()
        }

        val message = when {
            totalUsageMinutes == 0 ->
                "You haven't tracked usage today. Don't lose your $maxStreak-day streak!"
            else ->
                "You're close to your limit. One more heavy session could break your $maxStreak-day streak. You got this! 💪"
        }

        return NotificationData(
            title = "Streak Warning ⚠️",
            message = message,
            type = NotificationType.STREAK_WARNING
        )
    }
}
