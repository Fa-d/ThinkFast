package dev.sadakat.thinkfaster.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sadakat.thinkfaster.data.preferences.NotificationPreferences
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.usecase.notifications.CheckStreakAtRiskUseCase
import dev.sadakat.thinkfaster.domain.usecase.notifications.GenerateStreakWarningNotificationUseCase
import dev.sadakat.thinkfaster.util.NotificationHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker that monitors streak risk and sends warnings
 * Runs every 2 hours (only effective 6 PM - 11 PM due to CheckStreakAtRiskUseCase logic)
 *
 * Push Notification Strategy: Streak-at-risk warnings to prevent streak breaks
 */
class StreakMonitorWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val notificationPreferences: NotificationPreferences by inject()
    private val goalRepository: GoalRepository by inject()
    private val checkStreakAtRiskUseCase: CheckStreakAtRiskUseCase by inject()
    private val generateStreakWarningNotificationUseCase: GenerateStreakWarningNotificationUseCase by inject()

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "StreakMonitorWorker started")

            // 1. Check if motivational notifications are enabled
            if (!notificationPreferences.isMotivationalNotificationsEnabled()) {
                Log.d(TAG, "Motivational notifications disabled, skipping")
                return Result.success()
            }

            // 2. Check daily notification limit (2-3 per day max)
            if (!notificationPreferences.canSendNotificationToday()) {
                Log.d(TAG, "Daily notification limit reached, skipping")
                return Result.success()
            }

            // 3. Check if streak is at risk
            // CheckStreakAtRiskUseCase only returns true between 6 PM - 11 PM
            // and if usage is 80%+ OR no tracking after 8 PM
            val isAtRisk = checkStreakAtRiskUseCase()

            if (!isAtRisk) {
                Log.d(TAG, "Streak not at risk, skipping notification")
                return Result.success()
            }

            // 4. Generate warning notification
            val notificationData = generateStreakWarningNotificationUseCase()

            // 5. Get current streak for notification
            val goals = goalRepository.getAllGoals()
            val currentStreak = goals.maxOfOrNull { it.currentStreak } ?: 0

            // 6. Send urgent warning notification
            NotificationHelper.showStreakWarningNotification(
                context = context,
                title = notificationData.title,
                message = notificationData.message,
                streakDays = currentStreak
            )

            // 7. Increment daily notification counter
            notificationPreferences.incrementNotificationCount()

            Log.d(TAG, "Streak warning notification sent (count: ${notificationPreferences.getNotificationCountToday()})")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "StreakMonitorWorker failed", e)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "StreakMonitorWorker"
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "streak_monitor"
    }
}
