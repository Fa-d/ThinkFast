package dev.sadakat.thinkfaster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dev.sadakat.thinkfaster.MainActivity
import dev.sadakat.thinkfaster.R
import dev.sadakat.thinkfaster.domain.model.UsageEvent
import dev.sadakat.thinkfaster.domain.repository.InterventionResultRepository
import dev.sadakat.thinkfaster.domain.repository.SettingsRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import dev.sadakat.thinkfaster.presentation.overlay.ReminderOverlayWindow
import dev.sadakat.thinkfaster.presentation.overlay.TimerOverlayWindow
import dev.sadakat.thinkfaster.presentation.widget.updateWidgetData
import dev.sadakat.thinkfaster.util.Constants
import dev.sadakat.thinkfaster.util.ErrorLogger
import dev.sadakat.thinkfaster.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

/**
 * Foreground service that monitors app usage and shows overlays
 * Polls UsageStatsManager with adaptive intervals based on:
 * - Screen state (on/off)
 * - Target app activity
 * - Power save mode
 */
class UsageMonitorService : Service() {

    private val usageRepository: UsageRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val interventionResultRepository: InterventionResultRepository by inject()
    private val trackedAppsRepository: dev.sadakat.thinkfaster.domain.repository.TrackedAppsRepository by inject()
    private val goalRepository: dev.sadakat.thinkfaster.domain.repository.GoalRepository by inject()

    private lateinit var appLaunchDetector: AppLaunchDetector
    private lateinit var sessionDetector: SessionDetector
    private lateinit var contextDetector: ContextDetector
    private lateinit var rateLimiter: InterventionRateLimiter

    // WindowManager-based overlays (full-screen and compact)
    private lateinit var reminderOverlay: ReminderOverlayWindow
    private lateinit var timerOverlay: TimerOverlayWindow
    private lateinit var compactReminderOverlay: dev.sadakat.thinkfaster.presentation.overlay.CompactReminderOverlayWindow
    private lateinit var compactTimerOverlay: dev.sadakat.thinkfaster.presentation.overlay.CompactTimerOverlayWindow
    private lateinit var overlayManager: OverlayManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitoringJob: Job? = null

    private var isScreenOn = true
    private var isTargetAppActive = false
    private var lastTargetAppDetectedTime = 0L
    private var isPowerSaveMode = false

    // Track when overlays are shown to handle app detection delays
    private var reminderOverlayShowing = false
    private var reminderOverlayShownTime = 0L  // When reminder overlay was first shown
    private var timerOverlayShownTime = 0L     // When timer overlay was shown

    // Broadcast receiver for screen on/off events
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    onScreenOff()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    onScreenOn()
                }
            }
        }
    }

    // Broadcast receiver for power save mode changes
    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    updatePowerSaveMode()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Verify critical permissions before initializing overlays
        val missingPermissions = PermissionHelper.getMissingPermissions(this)
        if (missingPermissions.isNotEmpty()) {
            ErrorLogger.warning(
                "Service started with missing permissions: ${missingPermissions.joinToString()}",
                context = "UsageMonitorService.onCreate"
            )

            // Log specific guidance for overlay permission
            if (!PermissionHelper.hasOverlayPermission(this)) {
                ErrorLogger.warning(
                    "Overlay permission not granted - overlays will not be shown. " +
                    "User must enable 'Display Over Other Apps' in app settings.",
                    context = "UsageMonitorService.onCreate"
                )
            }
        } else {
            ErrorLogger.info(
                "All required permissions granted",
                context = "UsageMonitorService.onCreate"
            )
        }

        // Initialize detectors
        appLaunchDetector = AppLaunchDetector(
            context = this,
            trackedAppsRepository = trackedAppsRepository
        )
        sessionDetector = SessionDetector(
            usageRepository = usageRepository,
            scope = serviceScope,
            getTimerDurationMillis = {
                // Get timer duration from settings (blocking call is acceptable here)
                runBlocking {
                    settingsRepository.getSettingsOnce().getTimerDurationMillis()
                }
            }
        )
        contextDetector = ContextDetector(
            context = this
        )

        // Initialize rate limiter for intervention frequency control
        val interventionPrefs = dev.sadakat.thinkfaster.data.preferences.InterventionPreferences.getInstance(this)
        rateLimiter = InterventionRateLimiter(
            context = this,
            interventionPreferences = interventionPrefs
        )

        // Initialize WindowManager-based overlays (full-screen and compact)
        // These will be created even without permission, but won't show until granted
        reminderOverlay = ReminderOverlayWindow(this) {
            // Callback when reminder overlay is dismissed
            onReminderOverlayDismissed()
        }
        timerOverlay = TimerOverlayWindow(this) {
            // Callback when timer overlay is dismissed
            onTimerOverlayDismissed()
        }
        compactReminderOverlay = dev.sadakat.thinkfaster.presentation.overlay.CompactReminderOverlayWindow(this) {
            // Callback when compact reminder overlay is dismissed
            onReminderOverlayDismissed()
        }
        compactTimerOverlay = dev.sadakat.thinkfaster.presentation.overlay.CompactTimerOverlayWindow(this) {
            // Callback when compact timer overlay is dismissed
            onTimerOverlayDismissed()
        }

        // Initialize overlay manager for coordination
        // Routes to full-screen or compact overlays based on user preference
        overlayManager = OverlayManager(
            context = this,
            reminderOverlay = reminderOverlay,
            timerOverlay = timerOverlay,
            compactReminderOverlay = compactReminderOverlay,
            compactTimerOverlay = compactTimerOverlay,
            settingsRepository = settingsRepository
        )

        // Initialize debug overlay manager for testing intervention UI
        dev.sadakat.thinkfaster.presentation.overlay.DebugOverlayManager.initialize(
            manager = overlayManager!!,
            reminder = reminderOverlay,
            compactReminder = compactReminderOverlay
        )

        // Set up session detector callbacks
        setupSessionCallbacks()

        // Register screen state receiver
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, screenFilter)

        // Register power save mode receiver
        val powerFilter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        registerReceiver(powerSaveReceiver, powerFilter)

        // Update power save mode status
        updatePowerSaveMode()

        // Initialize session detector
        serviceScope.launch {
            sessionDetector.initialize()
        }

        // Observe tracked apps changes and invalidate cache when apps are added/removed
        serviceScope.launch {
            trackedAppsRepository.observeTrackedApps().collect { trackedApps ->
                // Invalidate cache when tracked apps list changes
                appLaunchDetector.invalidateCache()
                ErrorLogger.info(
                    "Tracked apps changed: ${trackedApps.size} apps tracked",
                    context = "UsageMonitorService.onCreate"
                )
            }
        }

        // Start foreground service with notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        // Start monitoring
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure monitoring is running
        if (monitoringJob == null || !monitoringJob!!.isActive) {
            startMonitoring()
        }

        // Service should be restarted if killed by system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This is a started service, not a bound service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop monitoring
        monitoringJob?.cancel()

        // Dismiss any showing overlays via OverlayManager
        try {
            overlayManager.dismissAll()
        } catch (e: Exception) {
            // OverlayManager might not be initialized
        }

        // Unregister receivers
        try {
            unregisterReceiver(screenStateReceiver)
            unregisterReceiver(powerSaveReceiver)
        } catch (e: Exception) {
            // Receivers might not be registered
        }

        // End any active session
        sessionDetector.forceEndSession()
    }

    /**
     * Update power save mode status
     */
    private fun updatePowerSaveMode() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        isPowerSaveMode = powerManager?.isPowerSaveMode == true
    }

    /**
     * Calculate the adaptive polling interval based on current state
     */
    private fun calculatePollingInterval(): Long {
        // In power save mode, always use slower polling
        if (isPowerSaveMode) {
            return Constants.POLLING_INTERVAL_POWER_SAVE
        }

        // Screen off - very slow polling
        if (!isScreenOn) {
            return Constants.POLLING_INTERVAL_INACTIVE
        }

        // Target app is active - fast polling for accurate detection
        if (isTargetAppActive) {
            return Constants.POLLING_INTERVAL_ACTIVE
        }

        // No target app active - check if we've been idle for a while
        val timeSinceLastDetection = System.currentTimeMillis() - lastTargetAppDetectedTime
        if (timeSinceLastDetection > Constants.IDLE_THRESHOLD_MS) {
            // Been idle for more than a minute - use slower polling
            return Constants.POLLING_INTERVAL_IDLE
        }

        // Recently had target app active - use active polling
        return Constants.POLLING_INTERVAL_ACTIVE
    }

    /**
     * Start the monitoring loop with adaptive polling
     */
    private fun startMonitoring() {
        monitoringJob = serviceScope.launch {
            while (isActive) {
                if (isScreenOn) {
                    // Only monitor when screen is on
                    checkForAppUsage()
                }

                // Calculate adaptive polling interval
                val pollingInterval = calculatePollingInterval()

                // Wait for polling interval
                delay(pollingInterval)
            }
        }
    }

    /**
     * Check for target app usage
     */
    private suspend fun checkForAppUsage() {
        // Check for app launch (detects NEW launches only)
        val launchEvent = appLaunchDetector.checkForAppLaunch()

        // Also check if a target app is currently in foreground (for continuing sessions)
        val currentForegroundApp = appLaunchDetector.isTargetAppInForeground()

        if (launchEvent != null) {
            // New launch detected
            lastTargetAppDetectedTime = launchEvent.timestamp
            isTargetAppActive = true
            onTargetAppDetected(launchEvent)
        } else if (currentForegroundApp != null) {
            // Target app is in foreground (continuing from previous detection)
            val currentTime = System.currentTimeMillis()
            lastTargetAppDetectedTime = currentTime
            isTargetAppActive = true

            // Reset overlay tracking since app is now detected
            reminderOverlayShownTime = 0L
            timerOverlayShownTime = 0L

            // Continue the session (this will trigger timer check)
            sessionDetector.onAppInForeground(currentForegroundApp, currentTime)
        } else {
            // No target app in foreground
            isTargetAppActive = false

            // Check if we should continue the session despite app not being detected
            // This happens after overlay dismissal when UsageStatsManager hasn't updated yet
            val currentSession = sessionDetector.getCurrentSession()
            val currentTime = System.currentTimeMillis()

            // Continue session if an overlay was shown recently (within 30 seconds)
            // This gives UsageStatsManager time to update after overlay dismissal
            val timeSinceReminderOverlay = if (reminderOverlayShownTime > 0) currentTime - reminderOverlayShownTime else Long.MAX_VALUE
            val timeSinceTimerOverlay = if (timerOverlayShownTime > 0) currentTime - timerOverlayShownTime else Long.MAX_VALUE
            val gracePeriod = 30000L // 30 seconds

            val shouldContinueSession = currentSession != null &&
                    (timeSinceReminderOverlay < gracePeriod || timeSinceTimerOverlay < gracePeriod)

            if (shouldContinueSession) {
                // During grace period, verify the tracked app is still in the current process
                // by checking if it's the topmost app in the usage events
                val trackedAppStillActive = isAppStillActive(currentSession.targetApp)

                if (trackedAppStillActive) {
                    // App is still active - continue the session
                    sessionDetector.onAppInForeground(currentSession.targetApp, currentTime)
                } else {
                    // User switched to a different app - end the session
                    sessionDetector.checkForSessionTimeout(currentTime)
                }
            } else {
                // Check for session timeout
                sessionDetector.checkForSessionTimeout(currentTime)
            }
        }
    }

    /**
     * Check if a specific app is still the active/foreground app
     * This is used during grace period to verify the user hasn't switched apps
     */
    private fun isAppStillActive(packageName: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val queryStartTime = currentTime - 10000L // Look back 10 seconds

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            ?: return false

        val usageEvents = usageStatsManager.queryEvents(queryStartTime, currentTime)
        var latestEventApp: String? = null
        var latestEventTime: Long = 0L

        val event = android.app.usage.UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            // Check for any event that indicates activity
            when (event.eventType) {
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED,
                android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND,
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (event.timeStamp > latestEventTime) {
                        latestEventApp = event.packageName
                        latestEventTime = event.timeStamp
                    }
                }
            }
        }

        // App is considered still active if the most recent event is for this app
        return latestEventApp == packageName
    }

    /**
     * Handle tracked app detection
     */
    private suspend fun onTargetAppDetected(launchEvent: AppLaunchDetector.AppLaunchEvent) {
        // Update session detector first (this will trigger onSessionStart for new sessions)
        sessionDetector.onAppInForeground(launchEvent.packageName, launchEvent.timestamp)

        // Check if we should always show reminder
        val settings = settingsRepository.getSettingsOnce()

        // If always show reminder is enabled, show overlay every time app is opened
        if (settings.alwaysShowReminder) {
            // Get current session state (should exist now after onAppInForeground)
            val currentSession = sessionDetector.getCurrentSession()
            if (currentSession != null) {
                showReminderOverlay(currentSession)

                // Log event
                serviceScope.launch {
                    usageRepository.insertEvent(
                        UsageEvent(
                            sessionId = currentSession.sessionId,
                            eventType = Constants.EVENT_REMINDER_SHOWN,
                            timestamp = System.currentTimeMillis(),
                            metadata = "App: ${launchEvent.packageName} (Every Time)"
                        )
                    )
                }
            }
        }
    }

    /**
     * Set up session detector callbacks
     */
    private fun setupSessionCallbacks() {
        // Called when a new session starts
        sessionDetector.onSessionStart = { sessionState ->
            // Reminder overlay is handled in onTargetAppDetected() based on alwaysShowReminder setting
            // This callback is triggered BEFORE onTargetAppDetected() completes the reminder check,
            // so we don't show overlay here to avoid duplicate overlays
            // The onTargetAppDetected() method is the single source of truth for reminder display
        }

        // Called when configured timer threshold is reached
        sessionDetector.onTenMinuteAlert = { sessionState ->
            ErrorLogger.info(
                "onTenMinuteAlert callback triggered for ${sessionState.targetApp}, " +
                "sessionId=${sessionState.sessionId}, duration=${sessionState.totalDuration}ms",
                context = "UsageMonitorService.setupSessionCallbacks"
            )

            // Show timer overlay
            showTimerOverlay(sessionState)

            // Log event
            serviceScope.launch {
                usageRepository.insertEvent(
                    UsageEvent(
                        sessionId = sessionState.sessionId,
                        eventType = Constants.EVENT_TEN_MIN_ALERT,
                        timestamp = System.currentTimeMillis(),
                        metadata = "App: ${sessionState.targetApp}, Duration: ${sessionState.totalDuration}ms"
                    )
                )
            }
        }

        // Called when session ends
        sessionDetector.onSessionEnd = { sessionState ->
            isTargetAppActive = false

            // Phase G: Update intervention results with final session outcome
            serviceScope.launch {
                try {
                    val finalDuration = sessionState.totalDuration
                    val endedNormally = sessionState.lastActiveTimestamp > 0

                    interventionResultRepository.updateSessionOutcome(
                        sessionId = sessionState.sessionId,
                        finalDurationMs = finalDuration,
                        endedNormally = endedNormally
                    )

                    ErrorLogger.info(
                        "Updated intervention result for session ${sessionState.sessionId} " +
                        "with final duration: ${finalDuration}ms",
                        context = "UsageMonitorService.onSessionEnd"
                    )

                    // Update widget with latest data
                    try {
                        val goals = goalRepository.getAllGoals()
                        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date())

                        var totalUsedMinutes = 0
                        var totalGoalMinutes = 0

                        for (goal in goals) {
                            totalGoalMinutes += goal.dailyLimitMinutes
                            val sessions = usageRepository.getSessionsByAppInRange(
                                targetApp = goal.targetApp,
                                startDate = today,
                                endDate = today
                            )
                            totalUsedMinutes += (sessions.sumOf { it.duration } / 1000 / 60).toInt()
                        }

                        updateWidgetData(this@UsageMonitorService, totalUsedMinutes, totalGoalMinutes)
                    } catch (e: Exception) {
                        ErrorLogger.error(
                            e,
                            message = "Failed to trigger widget update",
                            context = "UsageMonitorService.onSessionEnd"
                        )
                    }
                } catch (e: Exception) {
                    ErrorLogger.error(
                        e,
                        message = "Failed to update intervention result for session ${sessionState.sessionId}",
                        context = "UsageMonitorService.onSessionEnd"
                    )
                }
            }
        }
    }

    /**
     * Show reminder overlay when app is first opened
     */
    private fun showReminderOverlay(sessionState: SessionDetector.SessionState) {
        // Phase 1: Rate limiting check
        val sessionDuration = System.currentTimeMillis() - sessionState.startTimestamp
        val rateLimitResult = rateLimiter.canShowIntervention(
            InterventionRateLimiter.InterventionType.REMINDER,
            sessionDuration
        )

        if (!rateLimitResult.allowed) {
            ErrorLogger.info(
                "Skipping reminder overlay - rate limit: ${rateLimitResult.reason}",
                context = "UsageMonitorService.showReminderOverlay"
            )
            return
        }

        // Phase 2: Check if interventions are snoozed
        val interventionPrefs = dev.sadakat.thinkfaster.data.preferences.InterventionPreferences.getInstance(this)
        if (interventionPrefs.isSnoozed()) {
            val remainingMinutes = interventionPrefs.getSnoozeRemainingMinutes()
            ErrorLogger.info(
                "Skipping reminder overlay - snoozed for $remainingMinutes more minutes",
                context = "UsageMonitorService.showReminderOverlay"
            )
            return
        }

        // Phase 3: Check context-based conditions
        val sessionDurationForContext = System.currentTimeMillis() - sessionState.startTimestamp
        val contextResult = contextDetector.shouldShowInterventionType(
            ContextDetector.InterventionType.REMINDER,
            sessionDurationForContext
        )
        if (!contextResult.shouldShowIntervention) {
            ErrorLogger.info(
                "Skipping reminder overlay - context check failed: ${contextResult.reason}",
                context = "UsageMonitorService.showReminderOverlay"
            )
            return
        }

        // Phase 4: Check heuristic-based timing predictions
        val prediction = checkHeuristicPrediction()
        if (!prediction.shouldShowIntervention) {
            ErrorLogger.info(
                "Skipping reminder overlay - heuristic prediction: effectiveness=${prediction.effectivenessScore}, confidence=${prediction.confidence}",
                context = "UsageMonitorService.showReminderOverlay"
            )
            return
        }

        // Check if overlay permission is granted
        if (!PermissionHelper.hasOverlayPermission(this)) {
            ErrorLogger.warning(
                "Cannot show reminder overlay - overlay permission not granted",
                context = "UsageMonitorService.showReminderOverlay"
            )
            return
        }

        // Track that reminder overlay is showing
        reminderOverlayShowing = true
        reminderOverlayShownTime = System.currentTimeMillis()

        // Record that we showed an intervention (for rate limiting)
        rateLimiter.recordIntervention(InterventionRateLimiter.InterventionType.REMINDER)

        // Show WindowManager-based overlay via OverlayManager (prevents simultaneity)
        overlayManager.showReminder(
            sessionId = sessionState.sessionId,
            targetApp = sessionState.targetApp
        )
    }

    /**
     * Called when reminder overlay is dismissed
     * Resume session tracking by calling onAppInForeground to update timestamps
     */
    private fun onReminderOverlayDismissed() {
        reminderOverlayShowing = false

        // Notify overlay manager that reminder was dismissed
        overlayManager.onReminderDismissed()

        // Immediately check if the target app is still in foreground
        // This resumes session tracking and updates lastActiveTimestamp
        serviceScope.launch {
            val currentForegroundApp = appLaunchDetector.isTargetAppInForeground()
            if (currentForegroundApp != null) {
                // Resume session tracking - this updates lastActiveTimestamp and checks timer
                sessionDetector.onAppInForeground(currentForegroundApp, System.currentTimeMillis())
                isTargetAppActive = true
            } else {
                // Even if we can't detect the app immediately, assume it's still active
                // Update the session's lastActiveTimestamp to keep the session alive
                val currentSession = sessionDetector.getCurrentSession()
                if (currentSession != null) {
                    val currentTime = System.currentTimeMillis()
                    sessionDetector.onAppInForeground(currentSession.targetApp, currentTime)
                    isTargetAppActive = true
                }
            }
        }
    }

    /**
     * Called when timer overlay is dismissed
     * Notify overlay manager to clear state
     */
    private fun onTimerOverlayDismissed() {
        // Notify overlay manager that timer was dismissed
        overlayManager.onTimerDismissed()

        ErrorLogger.info(
            "Timer overlay dismissed",
            context = "UsageMonitorService.onTimerOverlayDismissed"
        )
    }

    /**
     * Show timer overlay after configured duration
     */
    private fun showTimerOverlay(sessionState: SessionDetector.SessionState) {
        ErrorLogger.info(
            "showTimerOverlay called for ${sessionState.targetApp}",
            context = "UsageMonitorService.showTimerOverlay"
        )

        // Phase 1: Rate limiting check
        val sessionDuration = System.currentTimeMillis() - sessionState.startTimestamp
        val rateLimitResult = rateLimiter.canShowIntervention(
            InterventionRateLimiter.InterventionType.TIMER,
            sessionDuration
        )

        if (!rateLimitResult.allowed) {
            ErrorLogger.info(
                "Skipping timer overlay - rate limit: ${rateLimitResult.reason}",
                context = "UsageMonitorService.showTimerOverlay"
            )
            return
        }

        // Phase 2: Check if interventions are snoozed
        val interventionPrefs = dev.sadakat.thinkfaster.data.preferences.InterventionPreferences.getInstance(this)
        if (interventionPrefs.isSnoozed()) {
            val remainingMinutes = interventionPrefs.getSnoozeRemainingMinutes()
            ErrorLogger.info(
                "Skipping timer overlay - snoozed for $remainingMinutes more minutes",
                context = "UsageMonitorService.showTimerOverlay"
            )
            return
        }

        // Phase 3: Check context-based conditions
        val sessionDurationForContext = System.currentTimeMillis() - sessionState.startTimestamp
        val contextResult = contextDetector.shouldShowInterventionType(
            ContextDetector.InterventionType.TIMER,
            sessionDurationForContext
        )
        if (!contextResult.shouldShowIntervention) {
            ErrorLogger.info(
                "Skipping timer overlay - context check failed: ${contextResult.reason}",
                context = "UsageMonitorService.showTimerOverlay"
            )
            return
        }

        // Phase 4: Check heuristic-based timing predictions
        val prediction = checkHeuristicPrediction()
        if (!prediction.shouldShowIntervention) {
            ErrorLogger.info(
                "Skipping timer overlay - heuristic prediction: effectiveness=${prediction.effectivenessScore}, confidence=${prediction.confidence}",
                context = "UsageMonitorService.showTimerOverlay"
            )
            return
        }

        // Check if overlay permission is granted
        if (!PermissionHelper.hasOverlayPermission(this)) {
            ErrorLogger.warning(
                "Cannot show timer overlay - overlay permission not granted",
                context = "UsageMonitorService.showTimerOverlay"
            )
            return
        }

        ErrorLogger.info(
            "Overlay permission granted, showing timer overlay",
            context = "UsageMonitorService.showTimerOverlay"
        )

        // Track that timer overlay is being shown
        timerOverlayShownTime = System.currentTimeMillis()

        // Record that we showed an intervention (for rate limiting)
        rateLimiter.recordIntervention(InterventionRateLimiter.InterventionType.TIMER)

        // Show WindowManager-based overlay via OverlayManager (prevents simultaneity)
        overlayManager.showTimer(
            sessionId = sessionState.sessionId,
            targetApp = sessionState.targetApp,
            startTime = sessionState.startTimestamp,
            duration = sessionState.totalDuration
        )

        ErrorLogger.info(
            "Timer overlay shown successfully",
            context = "UsageMonitorService.showTimerOverlay"
        )

        // Note: Session continues after timer alert dismissal
        // The alert will show again after another timer duration elapses
    }

    /**
     * Handle screen off event
     */
    private fun onScreenOff() {
        // End any active session when screen turns off
        sessionDetector.forceEndSession(
            timestamp = System.currentTimeMillis(),
            wasInterrupted = false,
            interruptionType = Constants.INTERRUPTION_SCREEN_OFF
        )
    }

    /**
     * Handle screen on event
     */
    private fun onScreenOn() {
        // Screen is back on - monitoring will resume automatically
        // Reset detector state to avoid stale data
        appLaunchDetector.reset()
    }

    /**
     * Check heuristic-based prediction for intervention timing
     * Uses simple rules instead of ML model to determine when to show interventions
     */
    private fun checkHeuristicPrediction(): PredictionResult {
        val calendar = java.util.Calendar.getInstance()
        val hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)

        // Simple heuristic: avoid showing interventions during late night hours
        // and during typical work hours (9 AM - 5 PM) on weekdays
        val isLateNight = hourOfDay >= 22 || hourOfDay < 6
        val isWorkHours = hourOfDay in 9..16 && dayOfWeek in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY

        // Higher effectiveness score during reasonable hours
        val effectivenessScore = when {
            isLateNight -> 0.2f
            isWorkHours -> 0.5f
            else -> 0.8f
        }

        // Threshold for showing intervention - skip if effectiveness is too low
        val threshold = 0.4f

        // Show intervention only if effectiveness score meets threshold
        return PredictionResult(
            shouldShowIntervention = effectivenessScore >= threshold,
            effectivenessScore = effectivenessScore,
            confidence = 0.7f
        )
    }

    /**
     * Simple prediction result data class
     */
    private data class PredictionResult(
        val shouldShowIntervention: Boolean,
        val effectivenessScore: Float,
        val confidence: Float
    )

    /**
     * Create the foreground notification
     */
    private fun createNotification(): Notification {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ThinkFast is monitoring")
            .setContentText("Helping you stay mindful of social media usage")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Usage Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for usage monitoring service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "usage_monitoring_channel"

        /**
         * Start the monitoring service
         */
        fun start(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the monitoring service
         */
        fun stop(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
