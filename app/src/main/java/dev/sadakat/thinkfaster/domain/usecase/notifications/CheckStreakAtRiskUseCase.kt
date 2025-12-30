package dev.sadakat.thinkfaster.domain.usecase.notifications

import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CheckStreakAtRiskUseCase(
    private val usageRepository: UsageRepository,
    private val goalRepository: GoalRepository
) {
    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    suspend operator fun invoke(): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Only check between 6 PM and 11 PM
        if (currentHour !in 18..23) return false

        val today = DATE_FORMAT.format(Date())
        val goals = goalRepository.getAllGoals()

        if (goals.isEmpty()) return false

        var totalUsageMinutes = 0
        var totalGoalMinutes = 0

        goals.forEach { goal ->
            val sessions = usageRepository.getSessionsByAppInRange(goal.targetApp, today, today)
            totalUsageMinutes += (sessions.sumOf { it.duration } / 1000 / 60).toInt()
            totalGoalMinutes += goal.dailyLimitMinutes
        }

        // Streak at risk if:
        // 1. Haven't tracked any usage today (might lose streak)
        // 2. OR approaching/exceeding limit (80%+)
        return when {
            totalUsageMinutes == 0 && currentHour >= 20 -> true  // No tracking after 8 PM
            totalUsageMinutes >= (totalGoalMinutes * 0.8) -> true  // 80%+ of limit
            else -> false
        }
    }
}
