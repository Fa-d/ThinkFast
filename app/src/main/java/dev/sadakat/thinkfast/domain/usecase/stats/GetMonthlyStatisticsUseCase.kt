package dev.sadakat.thinkfast.domain.usecase.stats

import dev.sadakat.thinkfast.domain.model.MonthlyStatistics
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Use case for getting statistics for a month
 */
class GetMonthlyStatisticsUseCase(
    private val usageRepository: UsageRepository,
    private val getDailyStatisticsUseCase: GetDailyStatisticsUseCase,
    private val getWeeklyStatisticsUseCase: GetWeeklyStatisticsUseCase
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val monthNameFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    /**
     * Get statistics for the month containing the specified date
     * @param date Date in yyyy-MM-dd format, defaults to today
     */
    suspend operator fun invoke(date: String = dateFormatter.format(Date())): MonthlyStatistics {
        val calendar = Calendar.getInstance()
        calendar.time = dateFormatter.parse(date) ?: Date()

        // Get month identifiers
        val month = monthFormatter.format(calendar.time)
        val monthName = monthNameFormatter.format(calendar.time)

        // Set to first day of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = dateFormatter.format(calendar.time)

        // Set to last day of month
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, daysInMonth)
        val monthEnd = dateFormatter.format(calendar.time)

        // Get daily statistics for each day
        calendar.time = dateFormatter.parse(monthStart)!!
        val dailyStatsList = mutableListOf<dev.sadakat.thinkfast.domain.model.DailyStatistics>()

        repeat(daysInMonth) {
            val dayDate = dateFormatter.format(calendar.time)
            dailyStatsList.add(getDailyStatisticsUseCase(dayDate))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Build weekly breakdown (get all weeks that overlap this month)
        calendar.time = dateFormatter.parse(monthStart)!!
        val weeklyBreakdown = mutableListOf<dev.sadakat.thinkfast.domain.model.WeeklyStatistics>()
        val processedWeeks = mutableSetOf<String>()

        // Get up to 5 weeks (max weeks in a month)
        repeat(5) {
            val weekStats = getWeeklyStatisticsUseCase(dateFormatter.format(calendar.time))
            if (weekStats.weekStart !in processedWeeks) {
                weeklyBreakdown.add(weekStats)
                processedWeeks.add(weekStats.weekStart)
            }
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        // Calculate monthly aggregates
        val totalUsage = dailyStatsList.sumOf { it.totalUsageMillis }
        val dailyAverage = if (daysInMonth > 0) totalUsage / daysInMonth else 0L
        val sessionCount = dailyStatsList.sumOf { it.sessionCount }
        val averageSession = if (sessionCount > 0) totalUsage / sessionCount else 0L
        val longestSession = dailyStatsList.maxOfOrNull { it.longestSessionMillis } ?: 0L

        // Find most active day
        val mostActiveDay = dailyStatsList.maxByOrNull { it.totalUsageMillis }
        val mostActiveDayDate = mostActiveDay?.date
        val mostActiveDayUsage = mostActiveDay?.totalUsageMillis ?: 0L

        // App-specific usage
        val facebookUsage = dailyStatsList.sumOf { it.facebookUsageMillis }
        val instagramUsage = dailyStatsList.sumOf { it.instagramUsageMillis }

        // Yearly projection (current month's usage * 12)
        val yearlyProjection = totalUsage * 12

        return MonthlyStatistics(
            month = month,
            monthName = monthName,
            totalUsageMillis = totalUsage,
            dailyAverage = dailyAverage,
            sessionCount = sessionCount,
            averageSessionMillis = averageSession,
            longestSessionMillis = longestSession,
            mostActiveDay = mostActiveDayDate,
            mostActiveDayUsage = mostActiveDayUsage,
            facebookUsageMillis = facebookUsage,
            instagramUsageMillis = instagramUsage,
            weeklyBreakdown = weeklyBreakdown,
            yearlyProjection = yearlyProjection
        )
    }
}
