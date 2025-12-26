package dev.sadakat.thinkfast.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sadakat.thinkfast.domain.repository.StatsRepository
import dev.sadakat.thinkfast.domain.usecase.goals.UpdateStreakUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Worker that runs daily at midnight to:
 * 1. Aggregate yesterday's usage data into daily stats
 * 2. Update goal streaks based on yesterday's performance
 *
 * Scheduled via WorkManager with PeriodicWorkRequest
 */
class DailyStatsAggregatorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val statsRepository: StatsRepository by inject()
    private val updateStreakUseCase: UpdateStreakUseCase by inject()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "DailyStatsAggregatorWorker started")

            // Get yesterday's date
            val yesterday = getYesterdayDate()
            Log.d(TAG, "Aggregating stats for: $yesterday")

            // Aggregate daily stats for yesterday
            statsRepository.aggregateDailyStats(yesterday)
            Log.d(TAG, "Daily stats aggregated successfully")

            // Update goal streaks based on yesterday's performance
            updateStreakUseCase.invoke()
            Log.d(TAG, "Goal streaks updated successfully")

            Log.d(TAG, "DailyStatsAggregatorWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "DailyStatsAggregatorWorker failed", e)
            // Retry on failure
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return dateFormatter.format(calendar.time)
    }

    companion object {
        private const val TAG = "DailyStatsAggregator"
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "daily_stats_aggregation"
    }
}
