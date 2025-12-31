package dev.sadakat.thinkfaster.analytics

import android.content.Context
import androidx.work.*
import dev.sadakat.thinkfaster.domain.model.InterventionResult
import dev.sadakat.thinkfaster.domain.model.InterventionType
import dev.sadakat.thinkfaster.domain.model.UserChoice
import dev.sadakat.thinkfaster.domain.repository.InterventionResultRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Main Analytics Manager
 *
 * Coordinates local data aggregation and Firebase reporting
 * All processing happens locally - only summaries are sent
 */
class AnalyticsManager(
    private val context: Context,
    private val repository: InterventionResultRepository,
    private val userPropertiesManager: UserPropertiesManager
) {
    private val privacySafeAnalytics = PrivacySafeAnalytics(context)
    private val firebaseReporter = FirebaseAnalyticsReporter()

    init {
        // Sync Firebase collection with user preference
        firebaseReporter.setAnalyticsEnabled(privacySafeAnalytics.isAnalyticsEnabled())
    }

    /**
     * Check if user has opted in to analytics
     */
    fun isAnalyticsEnabled(): Boolean = privacySafeAnalytics.isAnalyticsEnabled()

    /**
     * Enable or disable analytics (called from settings)
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        privacySafeAnalytics.setAnalyticsEnabled(enabled)
        firebaseReporter.setAnalyticsEnabled(enabled)

        if (enabled) {
            // Log consent event
            firebaseReporter.logConsentGiven()
        }
    }

    /**
     * Track an intervention result
     * This is called after each intervention is completed
     */
    fun trackIntervention(result: InterventionResult) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        // Aggregate locally (raw data stays on device)
        val aggregatedEvent = privacySafeAnalytics.aggregateIntervention(result)

        // Send only aggregated data to Firebase
        firebaseReporter.reportIntervention(aggregatedEvent)
    }

    /**
     * Generate and send daily summary
     * Call this once per day (e.g., via WorkManager)
     */
    suspend fun sendDailySummary() {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        // Get recent results (last 1000 = roughly all for a new app)
        val recentResults = repository.getRecentResults(limit = 1000)

        if (recentResults.isEmpty()) return

        // Filter to last 24 hours
        val oneDayAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        val yesterdayResults = recentResults.filter { it.timestamp >= oneDayAgo }

        if (yesterdayResults.isEmpty()) return

        // Generate summary locally
        val summary = generateDailySummary(yesterdayResults) ?: return

        // Send only summary to Firebase
        firebaseReporter.reportDailySummary(summary)
    }

    /**
     * Generate and send content performance report
     * Helps understand which content categories work best
     */
    suspend fun sendContentPerformanceReport() {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        val allResults = repository.getRecentResults(limit = 1000)
        if (allResults.isEmpty()) return

        // Group by content category
        val byCategory =
            allResults.groupBy { categorizeContent(it.contentType) }.map { (category, results) ->
                    ContentPerformanceReport(
                        contentCategory = category,
                        totalInterventions = results.size,
                        successRate = calculateSuccessRate(results),
                        avgDecisionTimeSeconds = results.map { it.timeToShowDecisionMs }.average()
                            .let { if (it.isNaN()) 0.0 else it / 1000.0 }.toInt())
                }

        // Send each category's performance
        byCategory.forEach { report ->
            firebaseReporter.reportContentPerformance(
                contentCategory = report.contentCategory,
                successRate = report.successRate,
                avgDecisionTime = report.avgDecisionTimeSeconds,
                sampleSize = report.totalInterventions
            )
        }
    }

    /**
     * Report app quality issue (crash, ANR, slow render)
     */
    fun reportAppQualityIssue(
        crashType: String? = null, anrOccurred: Boolean = false, slowRender: Boolean = false
    ) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        firebaseReporter.reportAppQuality(crashType, anrOccurred, slowRender)
    }

    // ========== Onboarding Analytics ==========

    fun trackOnboardingStarted() {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return
        firebaseReporter.logEvent(AnalyticsEvents.ONBOARDING_STARTED)
    }

    fun trackOnboardingStepCompleted(step: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return
        firebaseReporter.logEvent(
            AnalyticsEvents.ONBOARDING_STEP_COMPLETED,
            mapOf(AnalyticsEvents.Params.ONBOARDING_STEP to step)
        )
    }

    fun trackOnboardingCompleted(daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return
        firebaseReporter.logEvent(
            AnalyticsEvents.ONBOARDING_COMPLETED,
            mapOf(AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall)
        )
    }

    fun trackOnboardingSkipped(daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return
        firebaseReporter.logEvent(
            AnalyticsEvents.ONBOARDING_SKIPPED,
            mapOf(AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall)
        )
    }

    fun trackQuestStepCompleted(stepName: String, daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return
        firebaseReporter.logEvent(
            AnalyticsEvents.QUEST_STEP_COMPLETED,
            mapOf(
                AnalyticsEvents.Params.STEP_NAME to stepName,
                AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall
            )
        )
    }

    fun trackQuestCompleted(daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return
        firebaseReporter.logEvent(
            AnalyticsEvents.QUEST_COMPLETED,
            mapOf(AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall)
        )
    }

    // ========== Goal Analytics ==========

    fun trackGoalCreated(targetApp: String, goalMinutes: Int, daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        // Privacy: Use app category instead of package name
        val appCategory = categorizeApp(targetApp)

        firebaseReporter.logEvent(
            AnalyticsEvents.GOAL_CREATED,
            mapOf(
                AnalyticsEvents.Params.APP_CATEGORY to appCategory,
                AnalyticsEvents.Params.GOAL_MINUTES to goalMinutes,
                AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall
            )
        )
    }

    fun trackGoalAchieved(targetApp: String, streakDays: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        val appCategory = categorizeApp(targetApp)

        firebaseReporter.logEvent(
            AnalyticsEvents.GOAL_ACHIEVED,
            mapOf(
                AnalyticsEvents.Params.APP_CATEGORY to appCategory,
                AnalyticsEvents.Params.STREAK_DAYS to streakDays
            )
        )
    }

    fun trackGoalUpdated(targetApp: String, newGoalMinutes: Int, daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        val appCategory = categorizeApp(targetApp)

        firebaseReporter.logEvent(
            AnalyticsEvents.GOAL_UPDATED,
            mapOf(
                AnalyticsEvents.Params.APP_CATEGORY to appCategory,
                AnalyticsEvents.Params.GOAL_MINUTES to newGoalMinutes,
                AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall
            )
        )
    }

    fun trackGoalExceeded(targetApp: String, usageMinutes: Int, goalMinutes: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        val appCategory = categorizeApp(targetApp)
        val excessMinutes = usageMinutes - goalMinutes

        firebaseReporter.logEvent(
            AnalyticsEvents.GOAL_EXCEEDED,
            mapOf(
                AnalyticsEvents.Params.APP_CATEGORY to appCategory,
                "excess_minutes" to excessMinutes
            )
        )
    }

    // ========== Streak Analytics ==========

    fun trackStreakMilestone(streakDays: Int, milestoneType: String) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        firebaseReporter.logEvent(
            AnalyticsEvents.STREAK_MILESTONE,
            mapOf(
                AnalyticsEvents.Params.STREAK_DAYS to streakDays,
                AnalyticsEvents.Params.MILESTONE_TYPE to milestoneType
            )
        )
    }

    fun trackStreakBroken(previousStreak: Int, recoveryStarted: Boolean) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        firebaseReporter.logEvent(
            AnalyticsEvents.STREAK_BROKEN,
            mapOf(
                AnalyticsEvents.Params.PREVIOUS_STREAK to previousStreak,
                AnalyticsEvents.Params.RECOVERY_STARTED to recoveryStarted
            )
        )
    }

    fun trackStreakFreezeActivated(currentStreak: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        firebaseReporter.logEvent(
            AnalyticsEvents.STREAK_FREEZE_ACTIVATED,
            mapOf(AnalyticsEvents.Params.STREAK_DAYS to currentStreak)
        )
    }

    fun trackStreakRecoveryStarted(previousStreak: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        firebaseReporter.logEvent(
            AnalyticsEvents.STREAK_RECOVERY_STARTED,
            mapOf(AnalyticsEvents.Params.PREVIOUS_STREAK to previousStreak)
        )
    }

    fun trackStreakRecoveryCompleted(previousStreak: Int, daysToRecover: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        firebaseReporter.logEvent(
            AnalyticsEvents.STREAK_RECOVERY_COMPLETED,
            mapOf(
                AnalyticsEvents.Params.PREVIOUS_STREAK to previousStreak,
                "recovery_days" to daysToRecover
            )
        )
    }

    // ========== Settings Analytics ==========

    fun trackSettingChanged(settingName: String, newValue: String) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        firebaseReporter.logEvent(
            AnalyticsEvents.SETTINGS_CHANGED,
            mapOf(
                AnalyticsEvents.Params.SETTING_NAME to settingName,
                AnalyticsEvents.Params.SETTING_VALUE to newValue
            )
        )
    }

    // ========== App Lifecycle Analytics ==========

    fun trackAppLaunched(daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        firebaseReporter.logEvent(
            AnalyticsEvents.APP_LAUNCHED,
            mapOf(AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall)
        )
    }

    fun trackSessionEnd(durationMs: Long) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        firebaseReporter.logEvent(
            AnalyticsEvents.SESSION_END,
            mapOf(AnalyticsEvents.Params.SESSION_DURATION_MS to durationMs)
        )
    }

    fun trackBaselineCalculated(avgDailyMinutes: Int, daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        firebaseReporter.logEvent(
            AnalyticsEvents.BASELINE_CALCULATED,
            mapOf(
                "avg_daily_minutes" to avgDailyMinutes,
                AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall
            )
        )
    }

    // ========== User Properties ==========

    /**
     * Update user properties for segmentation
     * Call this daily or after significant user actions
     */
    suspend fun updateUserProperties() {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) return

        userPropertiesManager.updateUserProperties()
    }

    // ========== Private Helper Methods ==========

    private fun categorizeApp(packageName: String): String {
        return when {
            packageName.contains("facebook") -> "social_facebook"
            packageName.contains("instagram") -> "social_instagram"
            packageName.contains("twitter") || packageName.contains("x.com") -> "social_twitter"
            packageName.contains("tiktok") -> "social_tiktok"
            packageName.contains("snapchat") -> "social_snapchat"
            else -> "social_other"
        }
    }

    private fun categorizeContent(contentType: String): String {
        return when {
            contentType.contains("Reflection", ignoreCase = true) -> "reflection"
            contentType.contains("Question", ignoreCase = true) -> "question"
            contentType.contains("Time", ignoreCase = true) -> "time_alternative"
            contentType.contains("Breathing", ignoreCase = true) -> "breathing"
            contentType.contains("Fact", ignoreCase = true) -> "fact"
            else -> "other"
        }
    }

    private fun calculateSuccessRate(results: List<InterventionResult>): Int {
        val goBackCount = results.count { it.userChoice == UserChoice.GO_BACK }
        return if (results.isNotEmpty()) {
            ((goBackCount.toDouble() / results.size) * 100).toInt()
        } else 0
    }

    private fun generateDailySummary(results: List<InterventionResult>): DailySummary? {
        if (results.isEmpty()) return null

        // Group by intervention type
        val byType = results.groupBy { it.interventionType }

        val avgDecisionTimeSeconds = results.map { it.timeToShowDecisionMs }.average()
            .let { if (it.isNaN()) 0.0 else it / 1000.0 }.toInt()

        return DailySummary(
            date = getDayBucket(results.first().timestamp),
            totalInterventions = results.size,
            successRatePercent = calculateSuccessRate(results),
            avgDecisionTimeSeconds = avgDecisionTimeSeconds,
            reminderStats = byType[InterventionType.REMINDER]?.let { stats ->
                TypeStats(
                    count = stats.size, successRate = calculateSuccessRate(stats)
                )
            },
            timerStats = byType[InterventionType.TIMER]?.let { stats ->
                TypeStats(
                    count = stats.size, successRate = calculateSuccessRate(stats)
                )
            })
    }

    private fun getDayBucket(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return formatter.format(date)
    }

    /**
     * Schedule daily analytics upload
     * Runs once per day to send aggregated summaries
     */
    fun scheduleDailyUpload() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true) // Only upload when charging to save battery
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyAnalyticsUploadWorker>(
            24, TimeUnit.HOURS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_analytics_upload", ExistingPeriodicWorkPolicy.KEEP, dailyWorkRequest
        )
    }
}

/**
 * WorkManager worker for daily analytics upload
 * Runs daily to send aggregated analytics to Firebase
 */
class DailyAnalyticsUploadWorker(
    context: Context, workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val analyticsManager: AnalyticsManager by inject()

    override suspend fun doWork(): Result {
        return try {
            // Send daily summary of interventions
            analyticsManager.sendDailySummary()

            // Send content performance report
            analyticsManager.sendContentPerformanceReport()

            // Update user properties for segmentation
            analyticsManager.updateUserProperties()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

private data class ContentPerformanceReport(
    val contentCategory: String,
    val totalInterventions: Int,
    val successRate: Int,
    val avgDecisionTimeSeconds: Int
)
