package dev.sadakat.thinkfast

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.sadakat.thinkfast.di.databaseModule
import dev.sadakat.thinkfast.di.repositoryModule
import dev.sadakat.thinkfast.di.useCaseModule
import dev.sadakat.thinkfast.di.viewModelModule
import dev.sadakat.thinkfast.util.NotificationHelper
import dev.sadakat.thinkfast.worker.AchievementWorker
import dev.sadakat.thinkfast.worker.DailyStatsAggregatorWorker
import dev.sadakat.thinkfast.worker.DataCleanupWorker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ThinkFastApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ThinkFastApplication)
            modules(
                databaseModule,
                repositoryModule,
                useCaseModule,
                viewModelModule
            )
        }

        // Initialize notification channels (must be done before scheduling workers)
        NotificationHelper.createNotificationChannels(this)

        // Schedule background workers
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        // Schedule daily stats aggregation (runs every day at midnight)
        val dailyStatsRequest = PeriodicWorkRequestBuilder<DailyStatsAggregatorWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DailyStatsAggregatorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyStatsRequest
        )

        // Schedule data cleanup (runs every 7 days)
        val dataCleanupRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
            repeatInterval = 7,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(Constraints.NONE)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DataCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dataCleanupRequest
        )

        // Schedule achievement notifications (runs every day, slightly after midnight)
        // This runs after DailyStatsAggregatorWorker has updated the streaks
        val achievementRequest = PeriodicWorkRequestBuilder<AchievementWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(calculateInitialDelay() + TimeUnit.MINUTES.toMillis(5), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .build()

        workManager.enqueueUniquePeriodicWork(
            AchievementWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            achievementRequest
        )
    }

    /**
     * Calculate delay until next midnight
     */
    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }

        return midnight.timeInMillis - currentTime.timeInMillis
    }
}
