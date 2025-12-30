package dev.sadakat.thinkfaster.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sadakat.thinkfaster.data.preferences.NotificationPreferences
import dev.sadakat.thinkfaster.domain.usecase.notifications.GenerateEveningNotificationUseCase
import dev.sadakat.thinkfaster.util.NotificationHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Worker that sends evening review notifications
 * Runs daily at user's configured evening time (default: 8 PM)
 *
 * Push Notification Strategy: Daily usage summary with weekly progress
 */
class EveningNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val notificationPreferences: NotificationPreferences by inject()
    private val generateEveningNotificationUseCase: GenerateEveningNotificationUseCase by inject()

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "EveningNotificationWorker started")

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

            // 3. Generate notification content (includes daily summary + weekly progress)
            val notificationData = generateEveningNotificationUseCase()

            // 4. Send notification
            NotificationHelper.showEveningReviewNotification(
                context = context,
                title = notificationData.title,
                message = notificationData.message
            )

            // 5. Increment daily notification counter
            notificationPreferences.incrementNotificationCount()

            Log.d(TAG, "Evening notification sent successfully (count: ${notificationPreferences.getNotificationCountToday()})")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "EveningNotificationWorker failed", e)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "EveningNotificationWorker"
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "evening_notification"
    }
}
