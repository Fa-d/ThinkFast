package dev.sadakat.thinkfast.domain.usecase.stats

import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.DailyStatistics
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.util.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Use case for getting statistics for a specific day
 */
class GetDailyStatisticsUseCase(
    private val usageRepository: UsageRepository
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Get statistics for a specific date
     * @param date Date in yyyy-MM-dd format, defaults to today
     */
    suspend operator fun invoke(date: String = dateFormatter.format(Date())): DailyStatistics {
        // Get all sessions for the date
        val sessions = usageRepository.getSessionsByDate(date)

        // Get all events for the date
        val events = usageRepository.getEventsInRange(date, date)

        // Calculate statistics
        val totalUsage = sessions.sumOf { it.duration }
        val sessionCount = sessions.size
        val averageSession = if (sessionCount > 0) totalUsage / sessionCount else 0L
        val longestSession = sessions.maxOfOrNull { it.duration } ?: 0L

        // App-specific usage
        val facebookUsage = sessions
            .filter { it.targetApp == AppTarget.FACEBOOK.packageName }
            .sumOf { it.duration }

        val instagramUsage = sessions
            .filter { it.targetApp == AppTarget.INSTAGRAM.packageName }
            .sumOf { it.duration }

        // Event counts
        val reminderShown = events.count { event -> event.eventType == Constants.EVENT_REMINDER_SHOWN }
        val timerAlerts = events.count { event -> event.eventType == Constants.EVENT_TIMER_ALERT_SHOWN }
        val proceedClicks = events.count { event -> event.eventType == Constants.EVENT_PROCEED_CLICKED }

        return DailyStatistics(
            date = date,
            totalUsageMillis = totalUsage,
            sessionCount = sessionCount,
            averageSessionMillis = averageSession,
            longestSessionMillis = longestSession,
            facebookUsageMillis = facebookUsage,
            instagramUsageMillis = instagramUsage,
            reminderShownCount = reminderShown,
            timerAlertsCount = timerAlerts,
            proceedClickCount = proceedClicks
        )
    }
}
