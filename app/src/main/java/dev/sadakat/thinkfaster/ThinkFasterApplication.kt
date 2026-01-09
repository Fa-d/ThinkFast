package dev.sadakat.thinkfaster

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dev.sadakat.thinkfaster.analytics.AppLifecycleObserver
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.data.preferences.NotificationPreferences
import dev.sadakat.thinkfaster.data.sync.supabase.SupabaseClientProvider
import dev.sadakat.thinkfaster.di.analyticsModule
import dev.sadakat.thinkfaster.di.databaseModule
import dev.sadakat.thinkfaster.di.interventionModule
import dev.sadakat.thinkfaster.di.repositoryModule
import dev.sadakat.thinkfaster.di.useCaseModule
import dev.sadakat.thinkfaster.di.viewModelModule
import dev.sadakat.thinkfaster.util.NotificationHelper
import dev.sadakat.thinkfaster.util.WorkManagerHelper
import dev.sadakat.thinkfaster.worker.AchievementWorker
import dev.sadakat.thinkfaster.worker.DailyStatsAggregatorWorker
import dev.sadakat.thinkfaster.worker.DataCleanupWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ThinkFasterApplication : Application(), KoinComponent {
    // Inject AnalyticsManager to schedule daily uploads
    private val analyticsManager: dev.sadakat.thinkfaster.analytics.AnalyticsManager by inject()

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ThinkFasterApplication)
            modules(
                databaseModule,
                repositoryModule,
                useCaseModule,
                viewModelModule,
                analyticsModule,
                interventionModule  // Phase 1: Intervention tracking components
            )
        }

        // Initialize Supabase client on main thread (required for lifecycle observers)
        // This must be done before any sync operations run on background threads
        try {
            SupabaseClientProvider.getInstance(this)
        } catch (e: Exception) {
            // Supabase not available in this build variant (firebase/selfHosted)
            // Silently ignore
        }

        // Initialize Facebook SDK for authentication
        try {
            FacebookSdk.sdkInitialize(applicationContext)
            AppEventsLogger.activateApp(this)
        } catch (e: Exception) {
            // Facebook SDK initialization failed - likely missing Facebook App ID
            // App will work but Facebook login won't be available
        }

        // Set install date if first launch
        val interventionPrefs = InterventionPreferences(this)
        if (interventionPrefs.getInstallDate() == 0L) {
            interventionPrefs.setInstallDate(System.currentTimeMillis())
        }

        // Initialize Firebase Crashlytics ONLY for release builds
        // Debug builds have analytics completely disabled via BuildConfig
        if (BuildConfig.ENABLE_CRASHLYTICS) {
            initializeCrashlytics()
        }

        // Track app launch AFTER analytics is initialized
        val daysSinceInstall = interventionPrefs.getDaysSinceInstall()
        analyticsManager.trackAppLaunched(daysSinceInstall)

        // Register lifecycle observer for session tracking
        val lifecycleObserver = AppLifecycleObserver(analyticsManager)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)

        // Initialize notification channels (must be done before scheduling workers)
        NotificationHelper.createNotificationChannels(this)

        // Set up global exception handler for crash reporting
        setupCrashHandler()

        // Schedule background workers
        scheduleWorkers()
    }

    /**
     * Initialize Firebase Crashlytics
     * Configures crash reporting with privacy settings
     */
    private fun initializeCrashlytics() {
        try {
            val crashlytics = Firebase.crashlytics

            // Enable crash collection (can be controlled by user preference later)
            crashlytics.setCrashlyticsCollectionEnabled(true)

            // FIX: Use persistent anonymous user ID
            val interventionPrefs = InterventionPreferences(this)
            val anonymousUserId = interventionPrefs.getAnonymousUserId()

            // Set user ID for Crashlytics
            crashlytics.setUserId(anonymousUserId)

            // Also set for Firebase Analytics
            Firebase.analytics.setUserId(anonymousUserId)

            // Set custom keys for additional context
            crashlytics.setCustomKey("app_version", getAppVersion())
            crashlytics.setCustomKey("build_type", getBuildType())
            crashlytics.setCustomKey("days_since_install", interventionPrefs.getDaysSinceInstall())

        } catch (e: Exception) {
            // Crashlytics not available (google-services.json missing)
            // Silently ignore - app should work fine without it
        }
    }

    private fun getAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getBuildType(): String {
        return if (BuildConfig.DEBUG) "debug" else "release"
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

        // Schedule motivational notifications (Push Notification Strategy)
        // Only schedule if user has enabled motivational notifications
        val notificationPrefs = NotificationPreferences(this)
        if (notificationPrefs.isMotivationalNotificationsEnabled()) {
            WorkManagerHelper.scheduleMorningNotification(this)
            WorkManagerHelper.scheduleEveningNotification(this)
            WorkManagerHelper.scheduleStreakMonitor(this)
        }

        // Schedule daily analytics upload (runs every 24 hours when connected to WiFi and charging)
        analyticsManager.scheduleDailyUpload()

        // Schedule background sync (Phase 4: Sync Worker)
        // Only schedule if auto-sync is enabled
        val syncPrefs = dev.sadakat.thinkfaster.data.preferences.SyncPreferences(this)
        if (syncPrefs.isAutoSyncEnabled() && syncPrefs.isAuthenticated()) {
            dev.sadakat.thinkfaster.util.SyncScheduler.schedulePeriodicSync(this)
        }

        // Schedule Phase 1 outcome collection workers
        // These run automatically to collect intervention outcomes at different time windows
        WorkManagerHelper.scheduleAllOutcomeCollectionWorkers(this)
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

    /**
     * Set up global exception handler for crash reporting
     * Reports crashes to both Crashlytics and AnalyticsManager
     */
    private fun setupCrashHandler() {
        // Store the default handler to call it after our reporting
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Report to AnalyticsManager (privacy-safe, aggregated only)
            try {
                val crashType = throwable::class.simpleName ?: "UnknownCrash"
                analyticsManager.reportAppQualityIssue(
                    crashType = crashType,
                    anrOccurred = false,
                    slowRender = false
                )
            } catch (e: Exception) {
                // Ignore reporting failures
            }

            // Also report to Crashlytics (if available)
            try {
                Firebase.crashlytics.recordException(throwable)
            } catch (e: Exception) {
                // Crashlytics not available
            }

            // Let the default handler handle the crash
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
