package dev.sadakat.thinkfast.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.util.Constants

/**
 * Detects when target apps (Facebook, Instagram) are launched or brought to foreground
 * Uses UsageStatsManager to query recent app usage
 */
class AppLaunchDetector(private val context: Context) {

    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    // Track the last known foreground app to detect changes
    private var lastForegroundApp: String? = null

    // Track the last query timestamp to avoid re-processing events
    private var lastQueryTime: Long = System.currentTimeMillis()

    /**
     * Data class representing a detected app launch
     */
    data class AppLaunchEvent(
        val packageName: String,
        val appTarget: AppTarget,
        val timestamp: Long
    )

    /**
     * Check if any target app has been launched since the last check
     * Returns the launch event if detected, null otherwise
     *
     * This method should be called frequently (every 1.5 seconds) to minimize detection delay
     */
    fun checkForAppLaunch(): AppLaunchEvent? {
        val currentTime = System.currentTimeMillis()

        // Query events from the last check until now
        // We add a 5-second buffer to account for UsageStatsManager reporting delays
        val queryStartTime = lastQueryTime - 5000L

        val usageEvents = usageStatsManager.queryEvents(queryStartTime, currentTime)

        var latestForegroundApp: String? = null
        var latestForegroundTime: Long = 0L

        // Process all events to find the most recent foreground app
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            // We care about ACTIVITY_RESUMED events (app comes to foreground)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // Update the latest foreground app if this event is more recent
                    if (event.timeStamp > latestForegroundTime) {
                        latestForegroundApp = event.packageName
                        latestForegroundTime = event.timeStamp
                    }
                }
            }
        }

        // Update the last query time for next iteration
        lastQueryTime = currentTime

        // Check if the foreground app has changed
        if (latestForegroundApp != null && latestForegroundApp != lastForegroundApp) {
            lastForegroundApp = latestForegroundApp

            // Check if it's a target app
            val targetApp = AppTarget.fromPackageName(latestForegroundApp)
            if (targetApp != null) {
                return AppLaunchEvent(
                    packageName = latestForegroundApp,
                    appTarget = targetApp,
                    timestamp = latestForegroundTime
                )
            }
        }

        return null
    }

    /**
     * Get the current foreground app package name
     * Returns null if unable to determine or if no app is in foreground
     */
    fun getCurrentForegroundApp(): String? {
        val currentTime = System.currentTimeMillis()
        val queryStartTime = currentTime - 5000L // Look back 5 seconds (increased from 1s)

        val usageEvents = usageStatsManager.queryEvents(queryStartTime, currentTime)

        var latestForegroundApp: String? = null
        var latestForegroundTime: Long = 0L

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (event.timeStamp > latestForegroundTime) {
                        latestForegroundApp = event.packageName
                        latestForegroundTime = event.timeStamp
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    // If the latest foreground app was paused, it's no longer in foreground
                    if (event.packageName == latestForegroundApp && event.timeStamp > latestForegroundTime) {
                        latestForegroundApp = null
                        latestForegroundTime = event.timeStamp
                    }
                }
            }
        }

        return latestForegroundApp
    }

    /**
     * Check if a target app is currently in the foreground
     * Returns the AppTarget if a target app is detected, null otherwise
     *
     * IMPORTANT: This uses the cached lastForegroundApp to handle continuous usage
     * where there might not be recent events. It's updated by checkForAppLaunch().
     */
    fun isTargetAppInForeground(): AppTarget? {
        // First try to get from recent events
        val foregroundApp = getCurrentForegroundApp()

        // If we got a recent event, update our cache and return
        if (foregroundApp != null) {
            lastForegroundApp = foregroundApp
            return AppTarget.fromPackageName(foregroundApp)
        }

        // No recent events - use cached lastForegroundApp for continuous usage
        // This handles the case where user is actively using the app but there are no new events
        return if (lastForegroundApp != null) {
            AppTarget.fromPackageName(lastForegroundApp!!)
        } else {
            null
        }
    }

    /**
     * Check if a specific target app is currently in the foreground
     */
    fun isAppInForeground(targetApp: AppTarget): Boolean {
        val foregroundApp = getCurrentForegroundApp()
        return foregroundApp == targetApp.packageName
    }

    /**
     * Reset the detector state (useful when service restarts)
     */
    fun reset() {
        lastForegroundApp = null
        lastQueryTime = System.currentTimeMillis()
    }
}
