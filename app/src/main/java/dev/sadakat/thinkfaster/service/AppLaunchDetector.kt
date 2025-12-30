package dev.sadakat.thinkfaster.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import dev.sadakat.thinkfaster.domain.repository.TrackedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Detects when tracked apps are launched or brought to foreground
 * Uses UsageStatsManager to query recent app usage
 * Dynamically checks against user-configured tracked apps
 */
class AppLaunchDetector(
    private val context: Context,
    private val trackedAppsRepository: TrackedAppsRepository
) {

    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Track the last known foreground app to detect changes
    private var lastForegroundApp: String? = null

    // Track the last query timestamp to avoid re-processing events
    private var lastQueryTime: Long = System.currentTimeMillis()

    // Cache tracked apps to avoid frequent repository queries
    private var cachedTrackedApps: Set<String> = emptySet()
    private var lastCacheUpdate: Long = 0L
    private val cacheValidityDuration = 5000L // 5 seconds

    /**
     * Data class representing a detected app launch
     */
    data class AppLaunchEvent(
        val packageName: String,
        val timestamp: Long
    )

    /**
     * Refresh cached tracked apps if cache is stale
     * This is a suspend function that performs the refresh asynchronously
     */
    private suspend fun refreshTrackedAppsCacheAsync() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCacheUpdate > cacheValidityDuration) {
            cachedTrackedApps = withContext(Dispatchers.IO) {
                trackedAppsRepository.getTrackedApps().toSet()
            }
            lastCacheUpdate = currentTime
        }
    }

    /**
     * Triggers an async cache refresh without blocking
     * Call this when tracked apps are added/removed to ensure quick detection
     */
    fun invalidateCache() {
        lastCacheUpdate = 0L
        scope.launch {
            refreshTrackedAppsCacheAsync()
        }
    }

    /**
     * Check if a package name is in the tracked apps list
     * This is now a suspend function to avoid blocking
     */
    private suspend fun isTrackedApp(packageName: String): Boolean {
        refreshTrackedAppsCacheAsync()
        return cachedTrackedApps.contains(packageName)
    }

    /**
     * Check if any target app has been launched since the last check
     * Returns the launch event if detected, null otherwise
     *
     * This method should be called frequently (every 1.5 seconds) to minimize detection delay
     */
    suspend fun checkForAppLaunch(): AppLaunchEvent? {
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

            // Check if it's a tracked app
            if (isTrackedApp(latestForegroundApp)) {
                return AppLaunchEvent(
                    packageName = latestForegroundApp,
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
     * Check if a tracked app is currently in the foreground
     * Returns the package name if a tracked app is detected, null otherwise
     *
     * IMPORTANT: This uses the cached lastForegroundApp to handle continuous usage
     * where there might not be recent events. It's updated by checkForAppLaunch().
     */
    suspend fun isTargetAppInForeground(): String? {
        // First try to get from recent events
        val foregroundApp = getCurrentForegroundApp()

        // If we got a recent event, update our cache and check if it's tracked
        if (foregroundApp != null) {
            lastForegroundApp = foregroundApp
            return if (isTrackedApp(foregroundApp)) foregroundApp else null
        }

        // No recent events - use cached lastForegroundApp for continuous usage
        // This handles the case where user is actively using the app but there are no new events
        return if (lastForegroundApp != null && isTrackedApp(lastForegroundApp!!)) {
            lastForegroundApp
        } else {
            null
        }
    }

    /**
     * Check if a specific app (by package name) is currently in the foreground
     */
    fun isAppInForeground(packageName: String): Boolean {
        val foregroundApp = getCurrentForegroundApp()
        return foregroundApp == packageName
    }

    /**
     * Reset the detector state (useful when service restarts)
     */
    fun reset() {
        lastForegroundApp = null
        lastQueryTime = System.currentTimeMillis()
    }
}
