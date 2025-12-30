package dev.sadakat.thinkfaster.domain.usecase.stats

import dev.sadakat.thinkfaster.domain.model.SessionBreakdown
import dev.sadakat.thinkfaster.domain.model.UsageSession
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Use case for getting detailed session breakdown for a date range
 */
class GetSessionBreakdownUseCase(
    private val usageRepository: UsageRepository
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Get session breakdown for a date range
     * @param startDate Start date in yyyy-MM-dd format
     * @param endDate End date in yyyy-MM-dd format
     */
    suspend operator fun invoke(
        startDate: String = getTodayDate(),
        endDate: String = getTodayDate()
    ): SessionBreakdown {
        // Get all dates in the range
        val dates = getDateRange(startDate, endDate)

        // Get all sessions for all dates
        val allSessions = mutableListOf<UsageSession>()
        dates.forEach { date: String ->
            val sessions: List<UsageSession> = usageRepository.getSessionsByDate(date)
            allSessions.addAll(sessions)
        }

        // Sort by start time (most recent first)
        val sortedSessions = allSessions.sortedByDescending { it.startTimestamp }

        return SessionBreakdown(
            startDate = startDate,
            endDate = endDate,
            sessions = sortedSessions,
            totalSessions = sortedSessions.size,
            totalDuration = sortedSessions.sumOf { it.duration },
            averageDuration = if (sortedSessions.isNotEmpty()) {
                sortedSessions.sumOf { it.duration } / sortedSessions.size
            } else {
                0L
            },
            longestSession = sortedSessions.maxByOrNull { it.duration },
            shortestSession = sortedSessions.minByOrNull { it.duration }
        )
    }

    private fun getTodayDate(): String {
        return dateFormatter.format(Date())
    }

    private fun getDateRange(startDate: String, endDate: String): List<String> {
        val dates = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.time = dateFormatter.parse(startDate) ?: Date()

        val endCal = Calendar.getInstance()
        endCal.time = dateFormatter.parse(endDate) ?: Date()

        while (calendar.timeInMillis <= endCal.timeInMillis) {
            dates.add(dateFormatter.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return dates
    }
}
