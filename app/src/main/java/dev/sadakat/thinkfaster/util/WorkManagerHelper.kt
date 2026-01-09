package dev.sadakat.thinkfaster.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.sadakat.thinkfaster.data.preferences.NotificationPreferences
import dev.sadakat.thinkfaster.worker.EveningNotificationWorker
import dev.sadakat.thinkfaster.worker.LongTermOutcomeCollectionWorker
import dev.sadakat.thinkfaster.worker.MediumTermOutcomeCollectionWorker
import dev.sadakat.thinkfaster.worker.MorningNotificationWorker
import dev.sadakat.thinkfaster.worker.ShortTermOutcomeCollectionWorker
import dev.sadakat.thinkfaster.worker.StreakMonitorWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Utility object for scheduling notification workers
 * Handles WorkManager scheduling with proper timing and constraints
 *
 * Push Notification Strategy: Centralized worker scheduling management
 */
object WorkManagerHelper {

    /**
     * Schedule morning intention notification worker
     * Runs daily at user's configured time (default: 8 AM)
     */
    fun scheduleMorningNotification(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val prefs = NotificationPreferences(context)

        // Calculate delay to user's morning time
        val initialDelay = calculateDelayToTime(
            targetHour = prefs.getMorningHour(),
            targetMinute = prefs.getMorningMinute()
        )

        // Create periodic work request (runs daily)
        val request = PeriodicWorkRequestBuilder<MorningNotificationWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .build()

        // Schedule with UPDATE policy to reschedule if time changes
        workManager.enqueueUniquePeriodicWork(
            MorningNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Schedule evening review notification worker
     * Runs daily at user's configured time (default: 8 PM)
     */
    fun scheduleEveningNotification(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val prefs = NotificationPreferences(context)

        // Calculate delay to user's evening time
        val initialDelay = calculateDelayToTime(
            targetHour = prefs.getEveningHour(),
            targetMinute = prefs.getEveningMinute()
        )

        // Create periodic work request (runs daily)
        val request = PeriodicWorkRequestBuilder<EveningNotificationWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .build()

        // Schedule with UPDATE policy to reschedule if time changes
        workManager.enqueueUniquePeriodicWork(
            EveningNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Schedule streak monitoring worker
     * Runs every 2 hours to check for streak risk (only effective 6 PM - 11 PM)
     */
    fun scheduleStreakMonitor(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Create periodic work request (runs every 2 hours)
        val request = PeriodicWorkRequestBuilder<StreakMonitorWorker>(
            repeatInterval = 2,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(Constraints.NONE)
            .build()

        // Use KEEP policy to avoid rescheduling (no time customization needed)
        workManager.enqueueUniquePeriodicWork(
            StreakMonitorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Cancel all motivational notification workers
     * Called when user disables motivational notifications
     */
    fun cancelMotivationalNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(MorningNotificationWorker.WORK_NAME)
        workManager.cancelUniqueWork(EveningNotificationWorker.WORK_NAME)
        workManager.cancelUniqueWork(StreakMonitorWorker.WORK_NAME)
    }

    /**
     * Calculate delay in milliseconds from now to target time
     * If target time has passed today, schedule for tomorrow
     *
     * @param targetHour Hour in 24-hour format (0-23)
     * @param targetMinute Minute (0-59)
     * @return Delay in milliseconds
     */
    private fun calculateDelayToTime(targetHour: Int, targetMinute: Int): Long {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If target time has passed today, schedule for tomorrow
            if (timeInMillis <= currentTime.timeInMillis) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return targetTime.timeInMillis - currentTime.timeInMillis
    }

    // ========== Phase 1: Outcome Collection Workers ==========

    /**
     * Schedule short-term outcome collection worker
     * Runs every 30 minutes to collect outcomes 5-30 min after interventions
     *
     * Phase 1: Collects proximal and short-term intervention outcomes
     */
    fun scheduleShortTermOutcomeCollection(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Create periodic work request (runs every 30 minutes)
        val request = PeriodicWorkRequestBuilder<ShortTermOutcomeCollectionWorker>(
            repeatInterval = 30,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(Constraints.NONE)
            .build()

        // Use KEEP policy to avoid rescheduling unnecessarily
        workManager.enqueueUniquePeriodicWork(
            ShortTermOutcomeCollectionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Schedule medium-term outcome collection worker
     * Runs every 6 hours to collect outcomes from earlier the same day
     *
     * Phase 1: Collects same-day intervention outcomes
     */
    fun scheduleMediumTermOutcomeCollection(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Create periodic work request (runs every 6 hours)
        val request = PeriodicWorkRequestBuilder<MediumTermOutcomeCollectionWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(Constraints.NONE)
            .build()

        // Use KEEP policy to avoid rescheduling unnecessarily
        workManager.enqueueUniquePeriodicWork(
            MediumTermOutcomeCollectionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Schedule long-term outcome collection worker
     * Runs daily to collect outcomes 7-30 days after interventions
     *
     * Phase 1: Collects long-term behavior change data and calculates reward scores
     */
    fun scheduleLongTermOutcomeCollection(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Create periodic work request (runs daily at 3 AM)
        val initialDelay = calculateDelayToTime(targetHour = 3, targetMinute = 0)
        val request = PeriodicWorkRequestBuilder<LongTermOutcomeCollectionWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .build()

        // Use KEEP policy to avoid rescheduling unnecessarily
        workManager.enqueueUniquePeriodicWork(
            LongTermOutcomeCollectionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Schedule all Phase 1 outcome collection workers
     * Call this once on app initialization
     */
    fun scheduleAllOutcomeCollectionWorkers(context: Context) {
        scheduleShortTermOutcomeCollection(context)
        scheduleMediumTermOutcomeCollection(context)
        scheduleLongTermOutcomeCollection(context)
    }

    /**
     * Cancel all Phase 1 outcome collection workers
     * Useful for testing or disabling the feature
     */
    fun cancelAllOutcomeCollectionWorkers(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(ShortTermOutcomeCollectionWorker.WORK_NAME)
        workManager.cancelUniqueWork(MediumTermOutcomeCollectionWorker.WORK_NAME)
        workManager.cancelUniqueWork(LongTermOutcomeCollectionWorker.WORK_NAME)
    }
}
