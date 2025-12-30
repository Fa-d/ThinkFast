package dev.sadakat.thinkfaster.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sadakat.thinkfaster.data.preferences.NotificationPreferences
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.usecase.notifications.GenerateMorningNotificationUseCase
import dev.sadakat.thinkfaster.util.NotificationHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker that sends morning intention notifications
 * Runs daily at user's configured morning time (default: 8 AM)
 *
 * Push Notification Strategy: Morning motivation to set daily intentions
 */
class MorningNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val notificationPreferences: NotificationPreferences by inject()
    private val goalRepository: GoalRepository by inject()
    private val generateMorningNotificationUseCase: GenerateMorningNotificationUseCase by inject()

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "MorningNotificationWorker started")

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

            // 3. Generate notification content
            val notificationData = generateMorningNotificationUseCase()

            // 4. Get current streak for notification
            val goals = goalRepository.getAllGoals()
            val currentStreak = goals.maxOfOrNull { it.currentStreak } ?: 0

            // 5. Send notification
            NotificationHelper.showMorningIntentionNotification(
                context = context,
                title = notificationData.title,
                message = notificationData.message,
                currentStreak = currentStreak
            )

            // 6. Increment daily notification counter
            notificationPreferences.incrementNotificationCount()

            Log.d(TAG, "Morning notification sent successfully (count: ${notificationPreferences.getNotificationCountToday()})")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "MorningNotificationWorker failed", e)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "MorningNotificationWorker"
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "morning_notification"
    }
}
