package dev.sadakat.thinkfaster.domain.usecase.insights

import dev.sadakat.thinkfaster.domain.model.ComparativeAnalytics
import dev.sadakat.thinkfaster.domain.model.ComparisonMetric
import dev.sadakat.thinkfaster.domain.model.PersonalBests
import dev.sadakat.thinkfaster.domain.model.UsageRecord
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Use case for calculating comparative analytics
 * Phase 4: Show personal bests, improvements, and comparisons over time
 */
class CalculateComparativeAnalyticsUseCase(
    private val usageRepository: UsageRepository,
    private val goalRepository: GoalRepository
) {
    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    /**
     * Calculate comparative analytics
     * @param targetApp Package name of the app (optional, uses all if null)
     */
    suspend operator fun invoke(targetApp: String? = null): ComparativeAnalytics? {
        val calendar = Calendar.getInstance()
        val today = DATE_FORMAT.format(calendar.time)

        // Get goal info (if exists)
        val goal = if (targetApp != null) {
            goalRepository.getGoalByApp(targetApp)
        } else {
            goalRepository.getAllGoals().firstOrNull()
        }

        val daysSinceGoalSet = goal?.let { g ->
            val goalStartDate = DATE_FORMAT.parse(g.startDate)
            val todayDate = DATE_FORMAT.parse(today)
            if (goalStartDate != null && todayDate != null) {
                TimeUnit.MILLISECONDS.toDays(todayDate.time - goalStartDate.time).toInt()
            } else null
        }

        // Calculate date ranges
        calendar.time = DATE_FORMAT.parse(today)!!

        // This week (last 7 days)
        val thisWeekEnd = today
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val thisWeekStart = DATE_FORMAT.format(calendar.time)

        // Look back 8 weeks for historical data
        calendar.time = DATE_FORMAT.parse(today)!!
        calendar.add(Calendar.DAY_OF_YEAR, -56)
        val historicalStart = DATE_FORMAT.format(calendar.time)

        // Get all historical sessions
        val historicalSessions = if (targetApp != null) {
            usageRepository.getSessionsByAppInRange(targetApp, historicalStart, today)
        } else {
            usageRepository.getSessionsInRange(historicalStart, today)
        }

        if (historicalSessions.isEmpty()) {
            return null // Not enough data
        }

        // Calculate personal bests
        val personalBests = calculatePersonalBests(
            sessions = historicalSessions,
            longestStreak = goal?.longestStreak ?: 0,
            dailyGoalMinutes = goal?.dailyLimitMinutes
        )

        // Calculate comparisons
        val comparisons = calculateComparisons(
            targetApp = targetApp,
            thisWeekStart = thisWeekStart,
            thisWeekEnd = thisWeekEnd,
            historicalStart = historicalStart,
            today = today
        )

        // Calculate improvement rate (since goal was set)
        val improvementRate = goal?.let { g ->
            calculateImprovementRate(
                targetApp = targetApp,
                goalStartDate = g.startDate,
                today = today,
                dailyGoalMinutes = g.dailyLimitMinutes
            )
        }

        // Calculate consistency score (0-1, how consistent is daily usage)
        val consistencyScore = calculateConsistencyScore(historicalSessions)

        return ComparativeAnalytics(
            period = "Last 8 Weeks",
            personalBests = personalBests,
            comparisons = comparisons,
            improvementRate = improvementRate,
            consistencyScore = consistencyScore,
            daysSinceGoalSet = daysSinceGoalSet
        )
    }

    /**
     * Calculate personal best records
     */
    private fun calculatePersonalBests(
        sessions: List<dev.sadakat.thinkfaster.domain.model.UsageSession>,
        longestStreak: Int,
        dailyGoalMinutes: Int?
    ): PersonalBests {
        // Group sessions by date
        val sessionsByDate = sessions.groupBy { it.date }

        // Find lowest usage day
        val lowestUsageDay = sessionsByDate
            .mapNotNull { (date, daySessions) ->
                val totalMinutes = (daySessions.sumOf { it.duration } / 1000 / 60).toInt()
                if (totalMinutes > 0) {
                    val avgMinutes = sessionsByDate.values
                        .map { (it.sumOf { s -> s.duration } / 1000 / 60).toInt() }
                        .average()
                        .toInt()

                    val percentBelowAverage = if (avgMinutes > 0) {
                        ((avgMinutes - totalMinutes).toFloat() / avgMinutes * 100).toInt()
                    } else null

                    UsageRecord(
                        date = date,
                        usageMinutes = totalMinutes,
                        percentBelowAverage = percentBelowAverage
                    )
                } else null
            }
            .minByOrNull { it.usageMinutes }

        // Find lowest usage week
        val lowestUsageWeek = findLowestUsageWeek(sessions)

        return PersonalBests(
            longestStreak = longestStreak,
            lowestUsageDay = lowestUsageDay,
            lowestUsageWeek = lowestUsageWeek
        )
    }

    /**
     * Find the week with lowest usage
     */
    private fun findLowestUsageWeek(
        sessions: List<dev.sadakat.thinkfaster.domain.model.UsageSession>
    ): UsageRecord? {
        if (sessions.isEmpty()) return null

        // Group sessions into weeks
        val weeklyUsage = mutableMapOf<String, Int>()

        sessions.forEach { session ->
            val calendar = Calendar.getInstance()
            calendar.time = DATE_FORMAT.parse(session.date) ?: return@forEach

            // Get week start (Monday)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val weekStart = DATE_FORMAT.format(calendar.time)

            val minutes = (session.duration / 1000 / 60).toInt()
            weeklyUsage[weekStart] = (weeklyUsage[weekStart] ?: 0) + minutes
        }

        // Find week with minimum usage
        val lowestWeek = weeklyUsage.minByOrNull { it.value } ?: return null

        val avgWeeklyUsage = weeklyUsage.values.average().toInt()
        val percentBelowAverage = if (avgWeeklyUsage > 0) {
            ((avgWeeklyUsage - lowestWeek.value).toFloat() / avgWeeklyUsage * 100).toInt()
        } else null

        return UsageRecord(
            date = lowestWeek.key,
            usageMinutes = lowestWeek.value,
            percentBelowAverage = percentBelowAverage
        )
    }

    /**
     * Calculate comparison metrics
     */
    private suspend fun calculateComparisons(
        targetApp: String?,
        thisWeekStart: String,
        thisWeekEnd: String,
        historicalStart: String,
        today: String
    ): List<ComparisonMetric> {
        val comparisons = mutableListOf<ComparisonMetric>()

        // This week vs last week
        val thisWeekSessions = if (targetApp != null) {
            usageRepository.getSessionsByAppInRange(targetApp, thisWeekStart, thisWeekEnd)
        } else {
            usageRepository.getSessionsInRange(thisWeekStart, thisWeekEnd)
        }

        val calendar = Calendar.getInstance()
        calendar.time = DATE_FORMAT.parse(thisWeekStart)!!
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val lastWeekEnd = DATE_FORMAT.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val lastWeekStart = DATE_FORMAT.format(calendar.time)

        val lastWeekSessions = if (targetApp != null) {
            usageRepository.getSessionsByAppInRange(targetApp, lastWeekStart, lastWeekEnd)
        } else {
            usageRepository.getSessionsInRange(lastWeekStart, lastWeekEnd)
        }

        val thisWeekMinutes = (thisWeekSessions.sumOf { it.duration } / 1000 / 60).toFloat()
        val lastWeekMinutes = (lastWeekSessions.sumOf { it.duration } / 1000 / 60).toFloat()

        if (lastWeekMinutes > 0) {
            val percentDiff = abs((thisWeekMinutes - lastWeekMinutes) / lastWeekMinutes * 100)
            val isImprovement = thisWeekMinutes < lastWeekMinutes

            comparisons.add(
                ComparisonMetric(
                    label = "This week vs last week",
                    current = thisWeekMinutes,
                    comparison = lastWeekMinutes,
                    percentageDiff = percentDiff,
                    isImprovement = isImprovement
                )
            )
        }

        // This week vs best week (from historical data)
        val allWeeks = groupSessionsByWeek(
            if (targetApp != null) {
                usageRepository.getSessionsByAppInRange(targetApp, historicalStart, today)
            } else {
                usageRepository.getSessionsInRange(historicalStart, today)
            }
        )

        val bestWeekMinutes = allWeeks.values.minOrNull() ?: 0f
        if (bestWeekMinutes > 0 && bestWeekMinutes < thisWeekMinutes) {
            val percentDiff = abs((thisWeekMinutes - bestWeekMinutes) / bestWeekMinutes * 100)

            comparisons.add(
                ComparisonMetric(
                    label = "This week vs your best week",
                    current = thisWeekMinutes,
                    comparison = bestWeekMinutes,
                    percentageDiff = percentDiff,
                    isImprovement = false
                )
            )
        }

        return comparisons
    }

    /**
     * Group sessions by week
     */
    private fun groupSessionsByWeek(
        sessions: List<dev.sadakat.thinkfaster.domain.model.UsageSession>
    ): Map<String, Float> {
        val weeklyUsage = mutableMapOf<String, Float>()

        sessions.forEach { session ->
            val calendar = Calendar.getInstance()
            calendar.time = DATE_FORMAT.parse(session.date) ?: return@forEach

            // Get week start (Monday)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val weekStart = DATE_FORMAT.format(calendar.time)

            val minutes = (session.duration / 1000 / 60).toFloat()
            weeklyUsage[weekStart] = (weeklyUsage[weekStart] ?: 0f) + minutes
        }

        return weeklyUsage
    }

    /**
     * Calculate improvement rate since goal was set
     */
    private suspend fun calculateImprovementRate(
        targetApp: String?,
        goalStartDate: String,
        today: String,
        dailyGoalMinutes: Int
    ): Float? {
        val goalStartCal = Calendar.getInstance()
        goalStartCal.time = DATE_FORMAT.parse(goalStartDate) ?: return null

        // Get first week after goal was set
        val firstWeekEnd = goalStartCal.clone() as Calendar
        firstWeekEnd.add(Calendar.DAY_OF_YEAR, 6)
        val firstWeekEndStr = DATE_FORMAT.format(firstWeekEnd.time)

        val firstWeekSessions = if (targetApp != null) {
            usageRepository.getSessionsByAppInRange(targetApp, goalStartDate, firstWeekEndStr)
        } else {
            usageRepository.getSessionsInRange(goalStartDate, firstWeekEndStr)
        }

        // Get last week (current week)
        val calendar = Calendar.getInstance()
        calendar.time = DATE_FORMAT.parse(today)!!
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val thisWeekStart = DATE_FORMAT.format(calendar.time)

        val thisWeekSessions = if (targetApp != null) {
            usageRepository.getSessionsByAppInRange(targetApp, thisWeekStart, today)
        } else {
            usageRepository.getSessionsInRange(thisWeekStart, today)
        }

        val firstWeekAvg = firstWeekSessions.sumOf { it.duration } / 1000 / 60 / 7f
        val thisWeekAvg = thisWeekSessions.sumOf { it.duration } / 1000 / 60 / 7f

        return if (firstWeekAvg > 0) {
            ((firstWeekAvg - thisWeekAvg) / firstWeekAvg * 100)
        } else null
    }

    /**
     * Calculate consistency score (0-1, where 1 is very consistent)
     * Based on standard deviation of daily usage
     */
    private fun calculateConsistencyScore(
        sessions: List<dev.sadakat.thinkfaster.domain.model.UsageSession>
    ): Float {
        // Group by date
        val dailyUsage = sessions.groupBy { it.date }
            .mapValues { (_, daySessions) ->
                (daySessions.sumOf { it.duration } / 1000 / 60).toFloat()
            }

        if (dailyUsage.size < 7) return 0.5f // Not enough data

        val values = dailyUsage.values.toList()
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()
        val stdDev = kotlin.math.sqrt(variance)

        // Coefficient of variation (lower is more consistent)
        val cv = if (mean > 0) stdDev / mean else 1f

        // Convert to 0-1 score (inverse, so higher is better)
        return (1f - cv.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    }
}
