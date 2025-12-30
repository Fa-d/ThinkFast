package dev.sadakat.thinkfaster.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sadakat.thinkfaster.domain.repository.StatsRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Worker that runs periodically to clean up old data
 * Deletes:
 * - Usage sessions older than RETENTION_DAYS
 * - Daily stats older than RETENTION_DAYS
 *
 * Scheduled via WorkManager with PeriodicWorkRequest
 */
class DataCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val usageRepository: UsageRepository by inject()
    private val statsRepository: StatsRepository by inject()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "DataCleanupWorker started")

            // Calculate the cutoff date (RETENTION_DAYS ago)
            val cutoffDate = getCutoffDate()
            Log.d(TAG, "Deleting data older than: $cutoffDate")

            // Delete old sessions
            val sessionsDeleted = try {
                usageRepository.deleteSessionsOlderThan(cutoffDate)
                Log.d(TAG, "Old sessions deleted successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete old sessions", e)
                false
            }

            // Delete old stats
            val statsDeleted = try {
                statsRepository.deleteStatsOlderThan(cutoffDate)
                Log.d(TAG, "Old stats deleted successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete old stats", e)
                false
            }

            // Consider it successful if at least one deletion succeeded
            if (sessionsDeleted || statsDeleted) {
                Log.d(TAG, "DataCleanupWorker completed successfully")
                Result.success()
            } else {
                Log.e(TAG, "DataCleanupWorker failed - no deletions succeeded")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "DataCleanupWorker failed with exception", e)
            // Retry on failure
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun getCutoffDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -RETENTION_DAYS)
        return dateFormatter.format(calendar.time)
    }

    companion object {
        private const val TAG = "DataCleanupWorker"
        private const val MAX_RETRIES = 3

        // Retention policy: Keep data for 90 days
        private const val RETENTION_DAYS = 90

        const val WORK_NAME = "data_cleanup"
    }
}
