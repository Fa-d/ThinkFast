package dev.sadakat.thinkfast.domain.usecase.insights

import dev.sadakat.thinkfast.domain.model.HighRiskTimeWindow
import dev.sadakat.thinkfast.domain.model.PredictiveInsights
import dev.sadakat.thinkfast.domain.repository.GoalRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.domain.usecase.goals.GetGoalProgressUseCase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * Use case for generating predictive insights
 * Phase 4: Help users anticipate and prepare for high-risk times
 */
class GeneratePredictiveInsightsUseCase(
    private val usageRepository: UsageRepository,
    private val goalRepository: GoalRepository,
    private val getGoalProgressUseCase: GetGoalProgressUseCase
) {
    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private const val HIGH_RISK_THRESHOLD = 3 // Minimum sessions to consider a time high-risk
    }

    /**
     * Generate predictive insights for today
     * @param targetApp Package name of the app (optional, uses all if null)
     */
    suspend operator fun invoke(targetApp: String? = null): PredictiveInsights? {
        val today = DATE_FORMAT.format(Calendar.getInstance().time)

        // Get goal progress (if goal exists)
        val goalProgress = if (targetApp != null) {
            getGoalProgressUseCase(targetApp)
        } else {
            getGoalProgressUseCase.getAllProgress().firstOrNull()
        }

        // Get current usage for today
        val todaySessions = if (targetApp != null) {
            usageRepository.getSessionsByAppInRange(targetApp, today, today)
        } else {
            usageRepository.getSessionsInRange(today, today)
        }

        val todayUsageMs = todaySessions.sumOf { it.duration }
        val todayUsageMinutes = (todayUsageMs / 1000 / 60).toInt()

        // Calculate current usage percentage
        val dailyGoalMinutes = goalProgress?.goal?.dailyLimitMinutes ?: 60 // Default 60 min if no goal
        val currentUsagePercentage = ((todayUsageMinutes.toFloat() / dailyGoalMinutes) * 100).toInt()

        // Check if streak is at risk
        val streakAtRisk = goalProgress?.streakAtRisk ?: (currentUsagePercentage >= 80)
        val streakRiskReason = when {
            goalProgress?.isOverLimit == true -> "You've exceeded your daily limit"
            streakAtRisk -> "You're at $currentUsagePercentage% of your daily goal"
            else -> null
        }

        // Project end-of-day usage
        val (projectedUsage, willMeetGoal, probability) = calculateProjection(
            currentUsageMinutes = todayUsageMinutes,
            dailyGoalMinutes = dailyGoalMinutes,
            currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        )

        // Get historical patterns for high-risk time windows
        val highRiskTimeWindows = calculateHighRiskTimeWindows(targetApp)
        val nextHighRiskTime = findNextHighRiskTime(highRiskTimeWindows)

        // Generate recommended actions
        val recommendedActions = generateRecommendedActions(
            streakAtRisk = streakAtRisk,
            willMeetGoal = willMeetGoal,
            nextHighRiskTime = nextHighRiskTime,
            currentUsagePercentage = currentUsagePercentage
        )

        return PredictiveInsights(
            date = today,
            streakAtRisk = streakAtRisk,
            streakRiskReason = streakRiskReason,
            currentUsagePercentage = currentUsagePercentage,
            projectedEndOfDayUsageMinutes = projectedUsage,
            willMeetGoal = willMeetGoal,
            goalAchievementProbability = probability,
            highRiskTimeWindows = highRiskTimeWindows,
            nextHighRiskTime = nextHighRiskTime,
            recommendedActions = recommendedActions
        )
    }

    /**
     * Calculate end-of-day projection based on current usage and time
     */
    private fun calculateProjection(
        currentUsageMinutes: Int,
        dailyGoalMinutes: Int,
        currentHour: Int
    ): Triple<Int, Boolean, Float> {
        // If it's past 9 PM, projection is roughly what we have now
        if (currentHour >= 21) {
            val projected = (currentUsageMinutes * 1.1).toInt() // Add 10% buffer
            val willMeet = projected <= dailyGoalMinutes
            val probability = if (willMeet) 0.9f else 0.2f
            return Triple(projected, willMeet, probability)
        }

        // Calculate hours remaining in the day (assume active until 11 PM)
        val hoursRemaining = 23 - currentHour
        val hoursElapsed = currentHour - 6 // Assume day starts at 6 AM
        val totalActiveHours = 17 // 6 AM to 11 PM

        // Calculate usage rate (minutes per hour)
        val usageRate = if (hoursElapsed > 0) {
            currentUsageMinutes.toFloat() / hoursElapsed
        } else {
            currentUsageMinutes.toFloat()
        }

        // Project remaining usage (assume slight decrease in rate towards evening)
        val projectedRemainingUsage = (usageRate * hoursRemaining * 0.8).toInt()
        val projectedTotal = currentUsageMinutes + projectedRemainingUsage

        // Calculate probability of meeting goal
        val willMeet = projectedTotal <= dailyGoalMinutes
        val probability = when {
            projectedTotal <= dailyGoalMinutes * 0.8 -> 0.95f // Very likely
            projectedTotal <= dailyGoalMinutes -> 0.75f // Likely
            projectedTotal <= dailyGoalMinutes * 1.1 -> 0.5f // 50/50
            projectedTotal <= dailyGoalMinutes * 1.3 -> 0.25f // Unlikely
            else -> 0.1f // Very unlikely
        }

        return Triple(projectedTotal, willMeet, probability)
    }

    /**
     * Calculate high-risk time windows based on historical patterns
     */
    private suspend fun calculateHighRiskTimeWindows(targetApp: String?): List<HighRiskTimeWindow> {
        val calendar = Calendar.getInstance()
        val endDate = DATE_FORMAT.format(calendar.time)

        // Look back 2 weeks
        calendar.add(Calendar.DAY_OF_YEAR, -14)
        val startDate = DATE_FORMAT.format(calendar.time)

        // Get all sessions with hour of day
        val sessions = if (targetApp != null) {
            usageRepository.getSessionsByAppInRange(targetApp, startDate, endDate)
        } else {
            usageRepository.getSessionsWithHourOfDay(startDate, endDate)
        }

        // Group sessions by hour of day
        val hourMap = mutableMapOf<Int, MutableList<Long>>()
        sessions.forEach { session ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = session.startTimestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)

            hourMap.getOrPut(hour) { mutableListOf() }.add(session.duration)
        }

        // Calculate high-risk windows (hours with frequent or long sessions)
        return hourMap.mapNotNull { (hour, durations) ->
            val frequency = durations.size
            val avgUsageMinutes = (durations.average() / 1000 / 60).toInt()

            // Consider high-risk if >= 3 sessions in that hour over 2 weeks
            if (frequency >= HIGH_RISK_THRESHOLD) {
                val confidence = min(frequency / 10f, 1f) // Confidence based on frequency

                HighRiskTimeWindow(
                    timeLabel = formatHourLabel(hour),
                    hourOfDay = hour,
                    historicalFrequency = frequency,
                    averageUsageMinutes = avgUsageMinutes,
                    confidence = confidence
                )
            } else {
                null
            }
        }.sortedByDescending { it.historicalFrequency }
    }

    /**
     * Find the next high-risk time window (upcoming today)
     */
    private fun findNextHighRiskTime(windows: List<HighRiskTimeWindow>): HighRiskTimeWindow? {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Find the next hour that's a high-risk window
        return windows.firstOrNull { it.hourOfDay > currentHour }
            ?: windows.firstOrNull() // Or return the first one (for tomorrow)
    }

    /**
     * Generate recommended actions based on predictions
     */
    private fun generateRecommendedActions(
        streakAtRisk: Boolean,
        willMeetGoal: Boolean,
        nextHighRiskTime: HighRiskTimeWindow?,
        currentUsagePercentage: Int
    ): List<String> {
        val actions = mutableListOf<String>()

        when {
            streakAtRisk -> {
                actions.add("You're close to your limit - be mindful of app opens")
                actions.add("Try a 10-minute break before opening apps")
            }
            currentUsagePercentage >= 60 && willMeetGoal -> {
                actions.add("You're on track - keep it up!")
                actions.add("Plan ahead for later today")
            }
            currentUsagePercentage < 50 -> {
                actions.add("Great progress so far today")
            }
        }

        nextHighRiskTime?.let { riskTime ->
            if (riskTime.confidence >= 0.5f) {
                actions.add("You usually use apps around ${riskTime.timeLabel} - prepare ahead")
            }
        }

        if (!willMeetGoal && !streakAtRisk) {
            actions.add("Current pace may exceed your goal - adjust usage if needed")
        }

        return actions.ifEmpty { listOf("Keep tracking your usage patterns") }
    }

    /**
     * Format hour as readable label
     */
    private fun formatHourLabel(hour: Int): String {
        return when (hour) {
            0 -> "12 AM"
            in 1..11 -> "$hour AM"
            12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }
}
