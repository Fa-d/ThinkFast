package dev.sadakat.thinkfaster.domain.usecase.stats

import dev.sadakat.thinkfaster.domain.model.AggregateProgress
import dev.sadakat.thinkfaster.domain.model.AppProgress
import dev.sadakat.thinkfaster.domain.model.AppTarget
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Use case for getting aggregate daily progress across all tracked apps
 * Used by the home screen widget to show combined progress
 */
class GetAggregateDailyProgressUseCase(
    private val goalRepository: GoalRepository,
    private val usageRepository: UsageRepository
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Get aggregate progress for all tracked apps for a specific date
     * @param date Date in yyyy-MM-dd format, defaults to today
     * @return AggregateProgress with combined usage and goals
     */
    suspend operator fun invoke(date: String = dateFormatter.format(Date())): AggregateProgress {
        // Get all active goals
        val allGoals = goalRepository.getAllGoals()

        // If no goals set, return empty state
        if (allGoals.isEmpty()) {
            return AggregateProgress.empty().copy(date = date)
        }

        // Build breakdown of individual app progress
        val appBreakdown = mutableListOf<AppProgress>()
        var totalUsedMinutes = 0
        var totalGoalMinutes = 0

        for (goal in allGoals) {
            // Get sessions for this app on the specified date
            val sessions = usageRepository.getSessionsByAppInRange(
                targetApp = goal.targetApp,
                startDate = date,
                endDate = date
            )

            // Calculate usage in minutes
            val totalUsageMillis = sessions.sumOf { it.duration }
            val usedMinutes = (totalUsageMillis / 1000 / 60).toInt()

            // Get app display name
            val appName = AppTarget.fromPackageName(goal.targetApp)?.displayName
                ?: goal.getAppDisplayName()

            // Add to totals
            totalUsedMinutes += usedMinutes
            totalGoalMinutes += goal.dailyLimitMinutes

            // Add to breakdown
            appBreakdown.add(
                AppProgress(
                    packageName = goal.targetApp,
                    appName = appName,
                    usedMinutes = usedMinutes,
                    goalMinutes = goal.dailyLimitMinutes
                )
            )
        }

        return AggregateProgress(
            totalUsedMinutes = totalUsedMinutes,
            totalGoalMinutes = totalGoalMinutes,
            appBreakdown = appBreakdown,
            date = date
        )
    }
}
