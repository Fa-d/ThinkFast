package dev.sadakat.thinkfast.domain.usecase.insights

import dev.sadakat.thinkfast.domain.model.BehavioralInsights
import dev.sadakat.thinkfast.domain.model.StatsPeriod
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Use case for calculating behavioral insights from usage patterns
 * Phase 2: Self-awareness & pattern recognition
 */
class CalculateBehavioralInsightsUseCase(
    private val usageRepository: UsageRepository
) {
    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private const val QUICK_REOPEN_THRESHOLD_MS = 2 * 60 * 1000L // 2 minutes
        private const val BINGE_SESSION_THRESHOLD_MS = 30 * 60 * 1000L // 30 minutes
    }

    /**
     * Calculate behavioral insights for a given period
     * @param period The time period to analyze (DAILY, WEEKLY, MONTHLY)
     * @param endDate The end date (defaults to today)
     */
    suspend operator fun invoke(
        period: StatsPeriod,
        endDate: String = DATE_FORMAT.format(Calendar.getInstance().time)
    ): BehavioralInsights? {
        val calendar = Calendar.getInstance()
        calendar.time = DATE_FORMAT.parse(endDate) ?: return null

        // Calculate start date based on period
        val startDate = when (period) {
            StatsPeriod.DAILY -> endDate
            StatsPeriod.WEEKLY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                DATE_FORMAT.format(calendar.time)
            }
            StatsPeriod.MONTHLY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                DATE_FORMAT.format(calendar.time)
            }
        }

        // Get all sessions in the range
        val allSessions = usageRepository.getSessionsInRange(startDate, endDate)

        if (allSessions.isEmpty()) {
            return null // Not enough data
        }

        // Calculate weekend vs weekday usage
        val weekendSessions = usageRepository.getSessionsByDayOfWeek(
            startDate,
            endDate,
            listOf(0, 6) // Sunday and Saturday
        )
        val weekdaySessions = usageRepository.getSessionsByDayOfWeek(
            startDate,
            endDate,
            listOf(1, 2, 3, 4, 5) // Monday to Friday
        )

        val weekendUsageMs = weekendSessions.sumOf { it.duration }
        val weekdayUsageMs = weekdaySessions.sumOf { it.duration }
        val weekendUsageMinutes = (weekendUsageMs / 1000 / 60).toInt()
        val weekdayUsageMinutes = (weekdayUsageMs / 1000 / 60).toInt()

        val weekendVsWeekdayRatio = if (weekdayUsageMs > 0) {
            weekendUsageMs.toFloat() / weekdayUsageMs.toFloat()
        } else {
            0f
        }

        // Get late night sessions
        val lateNightSessions = usageRepository.getLateNightSessions(startDate, endDate)
        val lateNightUsageMinutes = (lateNightSessions.sumOf { it.duration } / 1000 / 60).toInt()

        // Calculate quick reopens (sessions within 2 minutes of each other)
        val sortedSessions = allSessions.sortedBy { it.startTimestamp }
        var quickReopenCount = 0
        for (i in 1 until sortedSessions.size) {
            val prevEndTime = sortedSessions[i - 1].endTimestamp ?: continue
            val currentStartTime = sortedSessions[i].startTimestamp
            val timeBetween = currentStartTime - prevEndTime

            if (timeBetween in 0..QUICK_REOPEN_THRESHOLD_MS) {
                quickReopenCount++
            }
        }

        // Find peak usage hour
        val sessionsWithHour = usageRepository.getSessionsWithHourOfDay(startDate, endDate)
        val hourUsageMap = mutableMapOf<Int, Pair<Int, Long>>() // hour -> (count, totalDuration)

        for (session in sessionsWithHour) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = session.startTimestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            val current = hourUsageMap[hour] ?: Pair(0, 0L)
            hourUsageMap[hour] = Pair(current.first + 1, current.second + session.duration)
        }

        val peakHour = hourUsageMap.maxByOrNull { it.value.first }
        val peakUsageHour = peakHour?.key ?: 0
        val sessionsInPeakHour = peakHour?.value?.first ?: 0
        val peakHourUsageMinutes = ((peakHour?.value?.second ?: 0L) / 1000 / 60).toInt()

        // Determine peak usage context
        val peakUsageContext = when (peakUsageHour) {
            in 22..23, in 0..5 -> "Late Night"
            in 6..11 -> "Morning"
            in 12..17 -> "Afternoon"
            else -> "Evening"
        }

        // Count binge sessions (>30 minutes)
        val bingeSessions = usageRepository.getLongSessions(
            startDate,
            endDate,
            BINGE_SESSION_THRESHOLD_MS
        )
        val bingeSessionCount = bingeSessions.size

        // Calculate average session duration
        val totalUsageMs = allSessions.sumOf { it.duration }
        val averageSessionMinutes = if (allSessions.isNotEmpty()) {
            (totalUsageMs / allSessions.size / 1000 / 60).toInt()
        } else {
            0
        }

        return BehavioralInsights(
            date = endDate,
            period = period,
            weekendVsWeekdayRatio = weekendVsWeekdayRatio,
            weekendUsageMinutes = weekendUsageMinutes,
            weekdayUsageMinutes = weekdayUsageMinutes,
            lateNightSessionCount = lateNightSessions.size,
            lateNightUsageMinutes = lateNightUsageMinutes,
            quickReopenCount = quickReopenCount,
            peakUsageHour = peakUsageHour,
            peakUsageContext = peakUsageContext,
            sessionsInPeakHour = sessionsInPeakHour,
            peakHourUsageMinutes = peakHourUsageMinutes,
            bingeSessionCount = bingeSessionCount,
            averageSessionMinutes = averageSessionMinutes
        )
    }
}
