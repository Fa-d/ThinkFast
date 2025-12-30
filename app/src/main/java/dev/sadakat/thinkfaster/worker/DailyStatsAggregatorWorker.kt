package dev.sadakat.thinkfaster.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sadakat.thinkfaster.domain.repository.StatsRepository
import dev.sadakat.thinkfaster.domain.repository.StreakRecoveryRepository
import dev.sadakat.thinkfaster.domain.usecase.streaks.ResetMonthlyFreezesUseCase
import dev.sadakat.thinkfaster.domain.usecase.streaks.UpdateStreakWithRecoveryUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Worker that runs daily at midnight to:
 * 1. Aggregate yesterday's usage data into daily stats
 * 2. Reset monthly streak freezes if new month
 * 3. Update goal streaks with freeze/recovery logic
 * 4. Clean up old completed recoveries
 *
 * Scheduled via WorkManager with PeriodicWorkRequest
 */
class DailyStatsAggregatorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val statsRepository: StatsRepository by inject()
    private val updateStreakWithRecoveryUseCase: UpdateStreakWithRecoveryUseCase by inject()
    private val resetMonthlyFreezesUseCase: ResetMonthlyFreezesUseCase by inject()
    private val streakRecoveryRepository: StreakRecoveryRepository by inject()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val yearMonthFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "DailyStatsAggregatorWorker started")

            // Get yesterday's date and current year-month
            val yesterday = getYesterdayDate()
            val currentYearMonth = getCurrentYearMonth()
            Log.d(TAG, "Processing date: $yesterday, month: $currentYearMonth")

            // 1. Aggregate daily stats for yesterday
            statsRepository.aggregateDailyStats(yesterday)
            Log.d(TAG, "Daily stats aggregated successfully")

            // 2. Reset monthly freezes if new month
            resetMonthlyFreezesUseCase.invoke(currentYearMonth)
            Log.d(TAG, "Monthly freeze reset check complete")

            // 3. Update goal streaks with freeze/recovery logic
            updateStreakWithRecoveryUseCase.invoke()
            Log.d(TAG, "Goal streaks updated with recovery logic")

            // 4. Clean up old completed recoveries (older than 30 days)
            streakRecoveryRepository.cleanupOldRecoveries(30)
            Log.d(TAG, "Old recovery data cleaned up")

            Log.d(TAG, "DailyStatsAggregatorWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "DailyStatsAggregatorWorker failed", e)
            // Retry on failure
            if (runAttemptCount < MAX_RETRIES) {
                Log.w(TAG, "Retrying... (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
                Result.retry()
            } else {
                Log.e(TAG, "Max retries reached, giving up")
                Result.failure()
            }
        }
    }

    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return dateFormatter.format(calendar.time)
    }

    private fun getCurrentYearMonth(): String {
        return yearMonthFormatter.format(Date())
    }

    companion object {
        private const val TAG = "DailyStatsAggregator"
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "daily_stats_aggregation"
    }
}
