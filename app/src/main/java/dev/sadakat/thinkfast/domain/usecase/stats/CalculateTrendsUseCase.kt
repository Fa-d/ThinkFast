package dev.sadakat.thinkfast.domain.usecase.stats

import dev.sadakat.thinkfast.domain.model.UsageTrend
import dev.sadakat.thinkfast.domain.model.TrendDirection
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Use case for calculating usage trends by comparing time periods
 */
class CalculateTrendsUseCase(
    private val getDailyStatisticsUseCase: GetDailyStatisticsUseCase,
    private val getWeeklyStatisticsUseCase: GetWeeklyStatisticsUseCase,
    private val getMonthlyStatisticsUseCase: GetMonthlyStatisticsUseCase
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Calculate weekly trend (current week vs previous week)
     */
    suspend fun calculateWeeklyTrend(date: String = dateFormatter.format(Date())): UsageTrend {
        val currentWeek = getWeeklyStatisticsUseCase(date)

        // Get previous week
        val calendar = Calendar.getInstance()
        calendar.time = dateFormatter.parse(date) ?: Date()
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val previousWeek = getWeeklyStatisticsUseCase(dateFormatter.format(calendar.time))

        val currentUsage = currentWeek.totalUsageMillis
        val previousUsage = previousWeek.totalUsageMillis

        return calculateTrend(
            currentUsage = currentUsage,
            previousUsage = previousUsage,
            periodName = "Week"
        )
    }

    /**
     * Calculate monthly trend (current month vs previous month)
     */
    suspend fun calculateMonthlyTrend(date: String = dateFormatter.format(Date())): UsageTrend {
        val currentMonth = getMonthlyStatisticsUseCase(date)

        // Get previous month
        val calendar = Calendar.getInstance()
        calendar.time = dateFormatter.parse(date) ?: Date()
        calendar.add(Calendar.MONTH, -1)
        val previousMonth = getMonthlyStatisticsUseCase(dateFormatter.format(calendar.time))

        val currentUsage = currentMonth.totalUsageMillis
        val previousUsage = previousMonth.totalUsageMillis

        return calculateTrend(
            currentUsage = currentUsage,
            previousUsage = previousUsage,
            periodName = "Month"
        )
    }

    /**
     * Calculate daily trend (today vs yesterday)
     */
    suspend fun calculateDailyTrend(date: String = dateFormatter.format(Date())): UsageTrend {
        val today = getDailyStatisticsUseCase(date)

        // Get yesterday
        val calendar = Calendar.getInstance()
        calendar.time = dateFormatter.parse(date) ?: Date()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = getDailyStatisticsUseCase(dateFormatter.format(calendar.time))

        val currentUsage = today.totalUsageMillis
        val previousUsage = yesterday.totalUsageMillis

        return calculateTrend(
            currentUsage = currentUsage,
            previousUsage = previousUsage,
            periodName = "Day"
        )
    }

    private fun calculateTrend(
        currentUsage: Long,
        previousUsage: Long,
        periodName: String
    ): UsageTrend {
        val difference = currentUsage - previousUsage
        val percentageChange = if (previousUsage > 0) {
            ((difference.toDouble() / previousUsage.toDouble()) * 100).toInt()
        } else {
            if (currentUsage > 0) 100 else 0
        }

        val direction = when {
            percentageChange > 5 -> TrendDirection.INCREASING
            percentageChange < -5 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }

        return UsageTrend(
            periodName = periodName,
            currentUsage = currentUsage,
            previousUsage = previousUsage,
            difference = difference,
            percentageChange = percentageChange,
            direction = direction
        )
    }
}
