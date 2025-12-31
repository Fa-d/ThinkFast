package dev.sadakat.thinkfaster.analytics

import android.content.Context
import android.util.Log
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
 *
 * DEBUGGING: Added comprehensive logging to help identify why events may not appear in Firebase
 */
class AnalyticsManager(
    private val context: Context,
    private val repository: InterventionResultRepository,
    private val userPropertiesManager: UserPropertiesManager
) {
    companion object {
        private const val TAG = "AnalyticsManager"
    }

    private val privacySafeAnalytics = PrivacySafeAnalytics(context)
    private val firebaseReporter = FirebaseAnalyticsReporter()

    init {
        // Sync Firebase collection with user preference
        val isEnabled = privacySafeAnalytics.isAnalyticsEnabled()
        Log.i(TAG, "AnalyticsManager init - Privacy setting analytics_enabled=$isEnabled")
        firebaseReporter.setAnalyticsEnabled(isEnabled)

        // Log current state for debugging
        logAnalyticsState()
    }

    /**
     * Check if user has opted in to analytics
     */
    fun isAnalyticsEnabled(): Boolean = privacySafeAnalytics.isAnalyticsEnabled()

    /**
     * Enable or disable analytics (called from settings)
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        Log.i(TAG, "setAnalyticsEnabled: $enabled")
        privacySafeAnalytics.setAnalyticsEnabled(enabled)
        firebaseReporter.setAnalyticsEnabled(enabled)

        if (enabled) {
            // Log consent event
            firebaseReporter.logConsentGiven()
        }
    }

    /**
     * Log current analytics state for debugging
     * Call this to verify analytics is properly configured
     */
    fun logAnalyticsState() {
        val isEnabled = privacySafeAnalytics.isAnalyticsEnabled()
        Log.i(TAG, "========== ANALYTICS STATE ==========")
        Log.i(TAG, "Privacy enabled: $isEnabled")
        Log.i(TAG, "Firebase enabled: ${firebaseReporter.isAnalyticsEnabled()}")
        Log.i(TAG, "====================================")
    }

    /**
     * Track an intervention result
     * This is called after each intervention is completed
     */
    fun trackIntervention(result: InterventionResult) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackIntervention: Analytics DISABLED - skipping event")
            return
        }

        try {
            // Aggregate locally (raw data stays on device)
            val aggregatedEvent = privacySafeAnalytics.aggregateIntervention(result)
            Log.d(TAG, "trackIntervention: Logging aggregated event - type=${result.interventionType}, choice=${result.userChoice}")

            // Send only aggregated data to Firebase
            firebaseReporter.reportIntervention(aggregatedEvent)
        } catch (e: Exception) {
            Log.e(TAG, "trackIntervention: Failed to track intervention", e)
        }
    }

    /**
     * Track when user completes onboarding AND grants all 3 required permissions
     * This is a critical conversion event - user is now ready to use the app
     * This should only be tracked ONCE per user installation
     */
    fun trackUserReady(daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackUserReady: Analytics DISABLED - skipping")
            return
        }

        try {
            // Check if we've already tracked this event
            val prefs = context.getSharedPreferences("analytics_events", Context.MODE_PRIVATE)
            val key = "user_ready_tracked"
            if (prefs.getBoolean(key, false)) {
                Log.d(TAG, "trackUserReady: Already tracked, skipping")
                return
            }

            Log.d(TAG, "trackUserReady: Logging user_ready event (day $daysSinceInstall)")
            firebaseReporter.logEvent(
                AnalyticsEvents.USER_READY,
                mapOf(AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall)
            )

            // Mark as tracked so we don't track it again
            prefs.edit().putBoolean(key, true).apply()
            Log.d(TAG, "trackUserReady: Event logged and marked as tracked")
        } catch (e: Exception) {
            Log.e(TAG, "trackUserReady: Failed", e)
        }
    }

    /**
     * Reset the user_ready tracking flag (for testing purposes)
     */
    fun resetUserReadyTracking() {
        val prefs = context.getSharedPreferences("analytics_events", Context.MODE_PRIVATE)
        prefs.edit().remove("user_ready_tracked").apply()
        Log.d(TAG, "resetUserReadyTracking: Tracking flag reset")
    }

    /**
     * Generate and send daily summary
     * Call this once per day (e.g., via WorkManager)
     */
    suspend fun sendDailySummary() {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "sendDailySummary: Analytics DISABLED - skipping")
            return
        }

        try {
            // Get recent results (last 1000 = roughly all for a new app)
            val recentResults = repository.getRecentResults(limit = 1000)

            if (recentResults.isEmpty()) {
                Log.d(TAG, "sendDailySummary: No results to summarize")
                return
            }

            // Filter to last 24 hours
            val oneDayAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            val yesterdayResults = recentResults.filter { it.timestamp >= oneDayAgo }

            if (yesterdayResults.isEmpty()) {
                Log.d(TAG, "sendDailySummary: No results from last 24 hours")
                return
            }

            // Generate summary locally
            val summary = generateDailySummary(yesterdayResults)

            if (summary == null) {
                Log.w(TAG, "sendDailySummary: Failed to generate summary")
                return
            }

            Log.d(TAG, "sendDailySummary: Sending daily summary with ${summary.totalInterventions} interventions")

            // Send only summary to Firebase
            firebaseReporter.reportDailySummary(summary)
        } catch (e: Exception) {
            Log.e(TAG, "sendDailySummary: Failed to send daily summary", e)
        }
    }

    /**
     * Generate and send content performance report
     * Helps understand which content categories work best
     */
    suspend fun sendContentPerformanceReport() {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "sendContentPerformanceReport: Analytics DISABLED - skipping")
            return
        }

        try {
            val allResults = repository.getRecentResults(limit = 1000)
            if (allResults.isEmpty()) {
                Log.d(TAG, "sendContentPerformanceReport: No results to analyze")
                return
            }

            // Group by content category
            val byCategory = allResults.groupBy { categorizeContent(it.contentType) }.map { (category, results) ->
                ContentPerformanceReport(
                    contentCategory = category,
                    totalInterventions = results.size,
                    successRate = calculateSuccessRate(results),
                    avgDecisionTimeSeconds = results.map { it.timeToShowDecisionMs }.average()
                        .let { if (it.isNaN()) 0.0 else it / 1000.0 }.toInt()
                )
            }

            Log.d(TAG, "sendContentPerformanceReport: Sending ${byCategory.size} category reports")

            // Send each category's performance
            byCategory.forEach { report ->
                firebaseReporter.reportContentPerformance(
                    contentCategory = report.contentCategory,
                    successRate = report.successRate,
                    avgDecisionTime = report.avgDecisionTimeSeconds,
                    sampleSize = report.totalInterventions
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendContentPerformanceReport: Failed", e)
        }
    }

    /**
     * Report app quality issue (crash, ANR, slow render)
     */
    fun reportAppQualityIssue(
        crashType: String? = null, anrOccurred: Boolean = false, slowRender: Boolean = false
    ) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "reportAppQualityIssue: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "reportAppQualityIssue: crash=$crashType, anr=$anrOccurred, slow=$slowRender")
            firebaseReporter.reportAppQuality(crashType, anrOccurred, slowRender)
        } catch (e: Exception) {
            Log.e(TAG, "reportAppQualityIssue: Failed", e)
        }
    }

    // ========== Onboarding Analytics ==========

    fun trackOnboardingStarted() {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackOnboardingStarted: Analytics DISABLED - skipping")
            return
        }
        Log.d(TAG, "trackOnboardingStarted: Logging event")
        try {
            firebaseReporter.logEvent(AnalyticsEvents.ONBOARDING_STARTED)
        } catch (e: Exception) {
            Log.e(TAG, "trackOnboardingStarted: Failed", e)
        }
    }

    fun trackOnboardingStepCompleted(step: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackOnboardingStepCompleted: Analytics DISABLED - skipping")
            return
        }
        Log.d(TAG, "trackOnboardingStepCompleted: Logging step $step")
        try {
            firebaseReporter.logEvent(
                AnalyticsEvents.ONBOARDING_STEP_COMPLETED,
                mapOf(AnalyticsEvents.Params.ONBOARDING_STEP to step)
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackOnboardingStepCompleted: Failed", e)
        }
    }

    fun trackOnboardingCompleted(daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackOnboardingCompleted: Analytics DISABLED - skipping")
            return
        }
        Log.d(TAG, "trackOnboardingCompleted: Logging event (day $daysSinceInstall)")
        try {
            firebaseReporter.logEvent(
                AnalyticsEvents.ONBOARDING_COMPLETED,
                mapOf(AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall)
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackOnboardingCompleted: Failed", e)
        }
    }

    fun trackOnboardingSkipped(daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackOnboardingSkipped: Analytics DISABLED - skipping")
            return
        }
        Log.d(TAG, "trackOnboardingSkipped: Logging event (day $daysSinceInstall)")
        try {
            firebaseReporter.logEvent(
                AnalyticsEvents.ONBOARDING_SKIPPED,
                mapOf(AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall)
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackOnboardingSkipped: Failed", e)
        }
    }

    fun trackQuestStepCompleted(stepName: String, daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackQuestStepCompleted: Analytics DISABLED - skipping")
            return
        }
        Log.d(TAG, "trackQuestStepCompleted: Logging $stepName")
        try {
            firebaseReporter.logEvent(
                AnalyticsEvents.QUEST_STEP_COMPLETED,
                mapOf(
                    AnalyticsEvents.Params.STEP_NAME to stepName,
                    AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackQuestStepCompleted: Failed", e)
        }
    }

    fun trackQuestCompleted(daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackQuestCompleted: Analytics DISABLED - skipping")
            return
        }
        Log.d(TAG, "trackQuestCompleted: Logging event")
        try {
            firebaseReporter.logEvent(
                AnalyticsEvents.QUEST_COMPLETED,
                mapOf(AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall)
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackQuestCompleted: Failed", e)
        }
    }

    // ========== Goal Analytics ==========

    fun trackGoalCreated(targetApp: String, goalMinutes: Int, daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackGoalCreated: Analytics DISABLED - skipping")
            return
        }

        try {
            // Privacy: Use app category instead of package name
            val appCategory = categorizeApp(targetApp)
            Log.d(TAG, "trackGoalCreated: Logging goal $goalMinutes min for $appCategory")

            firebaseReporter.logEvent(
                AnalyticsEvents.GOAL_CREATED,
                mapOf(
                    AnalyticsEvents.Params.APP_CATEGORY to appCategory,
                    AnalyticsEvents.Params.GOAL_MINUTES to goalMinutes,
                    AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackGoalCreated: Failed", e)
        }
    }

    fun trackGoalAchieved(targetApp: String, streakDays: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackGoalAchieved: Analytics DISABLED - skipping")
            return
        }

        try {
            val appCategory = categorizeApp(targetApp)
            Log.d(TAG, "trackGoalAchieved: Logging goal achieved - $streakDays day streak")

            firebaseReporter.logEvent(
                AnalyticsEvents.GOAL_ACHIEVED,
                mapOf(
                    AnalyticsEvents.Params.APP_CATEGORY to appCategory,
                    AnalyticsEvents.Params.STREAK_DAYS to streakDays
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackGoalAchieved: Failed", e)
        }
    }

    fun trackGoalUpdated(targetApp: String, newGoalMinutes: Int, daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackGoalUpdated: Analytics DISABLED - skipping")
            return
        }

        try {
            val appCategory = categorizeApp(targetApp)
            Log.d(TAG, "trackGoalUpdated: Logging goal update to $newGoalMinutes min")

            firebaseReporter.logEvent(
                AnalyticsEvents.GOAL_UPDATED,
                mapOf(
                    AnalyticsEvents.Params.APP_CATEGORY to appCategory,
                    AnalyticsEvents.Params.GOAL_MINUTES to newGoalMinutes,
                    AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackGoalUpdated: Failed", e)
        }
    }

    fun trackGoalExceeded(targetApp: String, usageMinutes: Int, goalMinutes: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackGoalExceeded: Analytics DISABLED - skipping")
            return
        }

        try {
            val appCategory = categorizeApp(targetApp)
            val excessMinutes = usageMinutes - goalMinutes
            Log.d(TAG, "trackGoalExceeded: Logging goal exceeded by $excessMinutes min")

            firebaseReporter.logEvent(
                AnalyticsEvents.GOAL_EXCEEDED,
                mapOf(
                    AnalyticsEvents.Params.APP_CATEGORY to appCategory,
                    "excess_minutes" to excessMinutes
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackGoalExceeded: Failed", e)
        }
    }

    // ========== Streak Analytics ==========

    fun trackStreakMilestone(streakDays: Int, milestoneType: String) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackStreakMilestone: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "trackStreakMilestone: Logging $streakDays day milestone ($milestoneType)")
            firebaseReporter.logEvent(
                AnalyticsEvents.STREAK_MILESTONE,
                mapOf(
                    AnalyticsEvents.Params.STREAK_DAYS to streakDays,
                    AnalyticsEvents.Params.MILESTONE_TYPE to milestoneType
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackStreakMilestone: Failed", e)
        }
    }

    fun trackStreakBroken(previousStreak: Int, recoveryStarted: Boolean) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackStreakBroken: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "trackStreakBroken: Logging streak broken - was $previousStreak days")
            firebaseReporter.logEvent(
                AnalyticsEvents.STREAK_BROKEN,
                mapOf(
                    AnalyticsEvents.Params.PREVIOUS_STREAK to previousStreak,
                    AnalyticsEvents.Params.RECOVERY_STARTED to recoveryStarted
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackStreakBroken: Failed", e)
        }
    }

    fun trackStreakFreezeActivated(currentStreak: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackStreakFreezeActivated: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "trackStreakFreezeActivated: Logging freeze activated at $currentStreak days")
            firebaseReporter.logEvent(
                AnalyticsEvents.STREAK_FREEZE_ACTIVATED,
                mapOf(AnalyticsEvents.Params.STREAK_DAYS to currentStreak)
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackStreakFreezeActivated: Failed", e)
        }
    }

    fun trackStreakRecoveryStarted(previousStreak: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackStreakRecoveryStarted: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "trackStreakRecoveryStarted: Logging recovery started from $previousStreak days")
            firebaseReporter.logEvent(
                AnalyticsEvents.STREAK_RECOVERY_STARTED,
                mapOf(AnalyticsEvents.Params.PREVIOUS_STREAK to previousStreak)
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackStreakRecoveryStarted: Failed", e)
        }
    }

    fun trackStreakRecoveryCompleted(previousStreak: Int, daysToRecover: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackStreakRecoveryCompleted: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "trackStreakRecoveryCompleted: Logging recovery completed in $daysToRecover days")
            firebaseReporter.logEvent(
                AnalyticsEvents.STREAK_RECOVERY_COMPLETED,
                mapOf(
                    AnalyticsEvents.Params.PREVIOUS_STREAK to previousStreak,
                    "recovery_days" to daysToRecover
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackStreakRecoveryCompleted: Failed", e)
        }
    }

    // ========== Intervention Dismissal/Snooze Analytics ==========

    /**
     * Track when user snoozes an intervention
     * This is called from overlay ViewModels when user clicks snooze
     */
    fun trackInterventionSnoozed(snoozeDurationMinutes: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackInterventionSnoozed: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "trackInterventionSnoozed: Logging snooze for $snoozeDurationMinutes min")
            firebaseReporter.logEvent(
                AnalyticsEvents.INTERVENTION_SNOOZED,
                mapOf("snooze_duration_minutes" to snoozeDurationMinutes)
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackInterventionSnoozed: Failed", e)
        }
    }

    /**
     * Track when user dismisses an intervention (without making a choice)
     */
    fun trackInterventionDismissed() {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackInterventionDismissed: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "trackInterventionDismissed: Logging intervention dismissed")
            firebaseReporter.logEvent(AnalyticsEvents.INTERVENTION_DISMISSED)
        } catch (e: Exception) {
            Log.e(TAG, "trackInterventionDismissed: Failed", e)
        }
    }

    // ========== Settings Analytics ==========

    fun trackSettingChanged(settingName: String, newValue: String) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackSettingChanged: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "trackSettingChanged: Logging $settingName = $newValue")
            firebaseReporter.logEvent(
                AnalyticsEvents.SETTINGS_CHANGED,
                mapOf(
                    AnalyticsEvents.Params.SETTING_NAME to settingName,
                    AnalyticsEvents.Params.SETTING_VALUE to newValue
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackSettingChanged: Failed", e)
        }
    }

    // ========== App Lifecycle Analytics ==========

    fun trackAppLaunched(daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackAppLaunched: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "trackAppLaunched: Logging app launch (day $daysSinceInstall)")
            firebaseReporter.logEvent(
                AnalyticsEvents.APP_LAUNCHED,
                mapOf(AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall)
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackAppLaunched: Failed", e)
        }
    }

    fun trackSessionEnd(durationMs: Long) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackSessionEnd: Analytics DISABLED - skipping")
            return
        }

        try {
            val durationSeconds = durationMs / 1000
            Log.d(TAG, "trackSessionEnd: Logging session end (${durationSeconds}s)")
            firebaseReporter.logEvent(
                AnalyticsEvents.SESSION_END,
                mapOf(AnalyticsEvents.Params.SESSION_DURATION_MS to durationMs)
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackSessionEnd: Failed", e)
        }
    }

    fun trackBaselineCalculated(avgDailyMinutes: Int, daysSinceInstall: Int) {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "trackBaselineCalculated: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "trackBaselineCalculated: Logging baseline ($avgDailyMinutes min/day)")
            firebaseReporter.logEvent(
                AnalyticsEvents.BASELINE_CALCULATED,
                mapOf(
                    "avg_daily_minutes" to avgDailyMinutes,
                    AnalyticsEvents.Params.DAYS_SINCE_INSTALL to daysSinceInstall
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "trackBaselineCalculated: Failed", e)
        }
    }

    // ========== User Properties ==========

    /**
     * Update user properties for segmentation
     * Call this daily or after significant user actions
     */
    suspend fun updateUserProperties() {
        if (!privacySafeAnalytics.isAnalyticsEnabled()) {
            Log.w(TAG, "updateUserProperties: Analytics DISABLED - skipping")
            return
        }

        try {
            Log.d(TAG, "updateUserProperties: Updating user properties")
            userPropertiesManager.updateUserProperties()
            Log.d(TAG, "updateUserProperties: User properties updated")
        } catch (e: Exception) {
            Log.e(TAG, "updateUserProperties: Failed", e)
        }
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
