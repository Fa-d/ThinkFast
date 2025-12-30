package dev.sadakat.thinkfaster.domain.usecase.stats

import dev.sadakat.thinkfaster.domain.model.WeeklyStatistics
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Use case for getting statistics for a week (7 days)
 */
class GetWeeklyStatisticsUseCase(
    private val usageRepository: UsageRepository,
    private val getDailyStatisticsUseCase: GetDailyStatisticsUseCase
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Get statistics for the week containing the specified date
     * Week runs from Monday to Sunday
     * @param date Date in yyyy-MM-dd format, defaults to today
     */
    suspend operator fun invoke(date: String = dateFormatter.format(Date())): WeeklyStatistics {
        // Find Monday and Sunday of the week
        val calendar = Calendar.getInstance()
        calendar.time = dateFormatter.parse(date) ?: Date()

        // Set to Monday of the week
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = dateFormatter.format(calendar.time)

        // Set to Sunday of the week
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val weekEnd = dateFormatter.format(calendar.time)

        // Get daily statistics for each day
        calendar.time = dateFormatter.parse(weekStart)!!
        val dailyBreakdown = mutableListOf<dev.sadakat.thinkfaster.domain.model.DailyStatistics>()

        repeat(7) {
            val dayDate = dateFormatter.format(calendar.time)
            dailyBreakdown.add(getDailyStatisticsUseCase(dayDate))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Calculate weekly aggregates
        val totalUsage = dailyBreakdown.sumOf { it.totalUsageMillis }
        val dailyAverage = totalUsage / 7
        val sessionCount = dailyBreakdown.sumOf { it.sessionCount }
        val averageSession = if (sessionCount > 0) totalUsage / sessionCount else 0L
        val longestSession = dailyBreakdown.maxOfOrNull { it.longestSessionMillis } ?: 0L

        // Find most active day
        val mostActiveDay = dailyBreakdown.maxByOrNull { it.totalUsageMillis }
        val mostActiveDayDate = mostActiveDay?.date
        val mostActiveDayUsage = mostActiveDay?.totalUsageMillis ?: 0L

        // App-specific usage
        val facebookUsage = dailyBreakdown.sumOf { it.facebookUsageMillis }
        val instagramUsage = dailyBreakdown.sumOf { it.instagramUsageMillis }

        return WeeklyStatistics(
            weekStart = weekStart,
            weekEnd = weekEnd,
            totalUsageMillis = totalUsage,
            dailyAverage = dailyAverage,
            sessionCount = sessionCount,
            averageSessionMillis = averageSession,
            longestSessionMillis = longestSession,
            mostActiveDay = mostActiveDayDate,
            mostActiveDayUsage = mostActiveDayUsage,
            facebookUsageMillis = facebookUsage,
            instagramUsageMillis = instagramUsage,
            dailyBreakdown = dailyBreakdown
        )
    }
}
