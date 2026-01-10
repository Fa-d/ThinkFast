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
import kotlinx.coroutines.async
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

    // JITAI dependencies - Phase 2
    private val personaDetector: dev.sadakat.thinkfaster.domain.intervention.PersonaDetector by inject()
    private val opportunityDetector: dev.sadakat.thinkfaster.domain.intervention.OpportunityDetector by inject()
    private val personaAwareContentSelector: dev.sadakat.thinkfaster.domain.intervention.PersonaAwareContentSelector by inject()

    // Phase 1 dependencies - Outcome tracking and decision logging
    private val decisionLogger: dev.sadakat.thinkfaster.domain.intervention.DecisionLogger by inject()
    private val burdenTracker: dev.sadakat.thinkfaster.domain.intervention.InterventionBurdenTracker by inject()

    // Phase 4 dependencies - RL-based adaptive content selection and timing optimization
    private val adaptiveContentSelector: dev.sadakat.thinkfaster.domain.intervention.AdaptiveContentSelector by inject()
    private val unifiedContentSelector: dev.sadakat.thinkfaster.domain.intervention.UnifiedContentSelector by inject()

    private lateinit var appLaunchDetector: AppLaunchDetector
    private lateinit var sessionDetector: SessionDetector
    private lateinit var contextDetector: ContextDetector
    private lateinit var rateLimiter: InterventionRateLimiter
    private lateinit var adaptiveRateLimiter: AdaptiveInterventionRateLimiter  // JITAI-enhanced rate limiter

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

    // Phase 3: Behavioral tracking for enhanced cues
    private var screenOnTimestamp = 0L  // When screen was turned on (for continuous screen-on tracking)
    private val unlockTimestamps = mutableListOf<Long>()  // Track unlock times for frequency calculation
    private val appLaunchTimestamps = mutableListOf<Pair<Long, String>>()  // Track app launches for rapid switching detection
    private val quickReopenTimestamps = mutableListOf<Long>()  // Track quick reopens for compulsive behavior

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
                    screenOnTimestamp = System.currentTimeMillis()  // Phase 3: Track screen-on time
                    onScreenOn()
                }
                Intent.ACTION_USER_PRESENT -> {
                    // Phase 3: Track screen unlock events for unlock frequency
                    val currentTime = System.currentTimeMillis()
                    unlockTimestamps.add(currentTime)

                    // Keep only last hour of unlock events
                    val oneHourAgo = currentTime - (60 * 60 * 1000)
                    unlockTimestamps.removeAll { it < oneHourAgo }

                    ErrorLogger.debug(
                        "Screen unlocked. Unlock count in last hour: ${unlockTimestamps.size}",
                        context = "UsageMonitorService.screenStateReceiver"
                    )
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
                // Phase 4: Apply dynamic threshold with frequency multiplier
                val baseTimerDuration = runBlocking {
                    settingsRepository.getSettingsOnce().getTimerDurationMillis()
                }

                // Apply RL-learned frequency multiplier
                val frequencyMultiplier = runBlocking {
                    try {
                        adaptiveContentSelector.getFrequencyMultiplier()
                    } catch (e: Exception) {
                        ErrorLogger.warning(
                            "Failed to get frequency multiplier, using default: ${e.message}",
                            context = "UsageMonitorService.getTimerDurationMillis"
                        )
                        1.0f  // Default to no adjustment
                    }
                }

                val adjustedDuration = (baseTimerDuration * frequencyMultiplier).toLong()

                ErrorLogger.debug(
                    "Dynamic timer threshold: base=${baseTimerDuration}ms, " +
                    "multiplier=$frequencyMultiplier, adjusted=${adjustedDuration}ms",
                    context = "UsageMonitorService.getTimerDurationMillis"
                )

                adjustedDuration
            },
            checkBehavioralDistress = { sessionState ->
                // Phase 3: Check for behavioral cues requiring early intervention
                runBlocking {
                    try {
                        val interventionContext = buildInterventionContext(sessionState)

                        // Check for distress signals from Phase 3 enhanced behavioral cues
                        val hasDistress = interventionContext.compulsiveBehaviorDetected ||
                                         interventionContext.rapidAppSwitching ||
                                         interventionContext.unusualUsageTime ||
                                         interventionContext.isLongScreenSession ||
                                         interventionContext.isExcessiveUnlocking

                        if (hasDistress) {
                            ErrorLogger.info(
                                "Behavioral distress detected: " +
                                "compulsive=${interventionContext.compulsiveBehaviorDetected}, " +
                                "rapidSwitch=${interventionContext.rapidAppSwitching}, " +
                                "unusualTime=${interventionContext.unusualUsageTime}, " +
                                "longScreen=${interventionContext.isLongScreenSession}, " +
                                "excessiveUnlock=${interventionContext.isExcessiveUnlocking}",
                                context = "UsageMonitorService.checkBehavioralDistress"
                            )
                        }

                        hasDistress
                    } catch (e: Exception) {
                        ErrorLogger.warning(
                            "Failed to check behavioral distress: ${e.message}",
                            context = "UsageMonitorService.checkBehavioralDistress"
                        )
                        false  // Default to no distress
                    }
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

        // Initialize JITAI-enhanced adaptive rate limiter - Phase 2
        adaptiveRateLimiter = AdaptiveInterventionRateLimiter(
            context = this,
            interventionPreferences = interventionPrefs,
            baseRateLimiter = rateLimiter,
            personaDetector = personaDetector,
            opportunityDetector = opportunityDetector,
            decisionLogger = decisionLogger,
            burdenTracker = burdenTracker
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
            addAction(Intent.ACTION_USER_PRESENT)  // Phase 3: Track screen unlocks
        }
        registerReceiver(screenStateReceiver, screenFilter)

        // Register power save mode receiver
        val powerFilter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        registerReceiver(powerSaveReceiver, powerFilter)

        // Update power save mode status
        updatePowerSaveMode()

        // Phase 3: Initialize screen-on timestamp if screen is currently on
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager?.isInteractive == true) {
            screenOnTimestamp = System.currentTimeMillis()
            ErrorLogger.debug(
                "Service started with screen already on. Tracking screen-on duration.",
                context = "UsageMonitorService.onCreate"
            )
        }

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
        // Phase 3: Track app launches for rapid switching detection
        val currentTime = System.currentTimeMillis()
        appLaunchTimestamps.add(Pair(currentTime, launchEvent.packageName))

        // Keep only last 30 seconds of app launches
        val thirtySecondsAgo = currentTime - (30 * 1000)
        appLaunchTimestamps.removeAll { it.first < thirtySecondsAgo }

        // Phase 3: Track quick reopens for compulsive behavior detection
        val sessionsToday = usageRepository.getSessionsInRange(
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        )
        val lastSessionOfThisApp = sessionsToday
            .filter { it.targetApp == launchEvent.packageName && it.endTimestamp != null }
            .maxByOrNull { it.endTimestamp ?: 0L }

        if (lastSessionOfThisApp != null && lastSessionOfThisApp.endTimestamp != null) {
            val timeSinceLastSession = currentTime - lastSessionOfThisApp.endTimestamp!!
            if (timeSinceLastSession < 2 * 60 * 1000) {  // < 2 minutes = quick reopen
                quickReopenTimestamps.add(currentTime)

                // Keep only last 5 minutes of quick reopens
                val fiveMinutesAgo = currentTime - (5 * 60 * 1000)
                quickReopenTimestamps.removeAll { it < fiveMinutesAgo }

                ErrorLogger.debug(
                    "Quick reopen detected for ${launchEvent.packageName}. " +
                    "Quick reopens in last 5 min: ${quickReopenTimestamps.size}",
                    context = "UsageMonitorService.onTargetAppDetected"
                )
            }
        }

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
        // Phase 4: Algorithm then decides whether to actually show overlay based on JITAI factors
        sessionDetector.onTimerThresholdReached = { sessionState ->
            ErrorLogger.info(
                "Timer threshold reached for ${sessionState.targetApp}, " +
                "sessionId=${sessionState.sessionId}, duration=${sessionState.totalDuration}ms | " +
                "Consulting AdaptiveInterventionRateLimiter for JITAI decision",
                context = "UsageMonitorService.setupSessionCallbacks"
            )

            // Algorithm decides whether to show overlay based on:
            // - Opportunity detection (is this a good moment to interrupt?)
            // - Burden metrics (is user fatigued?)
            // - Timing optimization (is this the optimal time?)
            // - Rate limiting (have we shown too many interventions?)
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
     * Build InterventionContext from session state
     * Phase 2 JITAI: Creates rich context for opportunity detection and content selection
     *
     * Performance Optimization: Uses parallel async queries to minimize decision time
     * - Target: <100ms for JITAI decision making
     * - Parallelizes independent database calls
     * - Batches weekly sessions query into single range call
     * - Tracks performance metrics for monitoring
     */
    private suspend fun buildInterventionContext(
        sessionState: SessionDetector.SessionState
    ): dev.sadakat.thinkfaster.domain.intervention.InterventionContext {
        val startTime = System.currentTimeMillis()

        // Prepare date strings once
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
        val weekAgo = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(System.currentTimeMillis() - 6 * 24L * 60 * 60 * 1000))

        // Parallelize all independent database queries using async/await
        val todaySessionsDeferred = serviceScope.async {
            usageRepository.getSessionsInRange(today, today)
        }
        val yesterdaySessionsDeferred = serviceScope.async {
            usageRepository.getSessionsInRange(yesterday, yesterday)
        }
        val weeklySessionsDeferred = serviceScope.async {
            // Batch weekly query into single range call instead of 7 separate calls
            usageRepository.getSessionsInRange(weekAgo, today)
        }
        val goalDeferred = serviceScope.async {
            goalRepository.getGoalByApp(sessionState.targetApp)
        }
        val bestSessionDeferred = serviceScope.async {
            try {
                interventionResultRepository.getFirstResult()?.currentSessionDurationMs?.div(1000 * 60)?.toInt() ?: 0
            } catch (e: Exception) {
                0
            }
        }

        // Get install date synchronously (fast SharedPreferences read)
        val installDate = dev.sadakat.thinkfaster.data.preferences.InterventionPreferences.getInstance(this)
            .getInstallDate()

        // Await all parallel results
        val sessionsToday = todaySessionsDeferred.await()
        val sessionsYesterday = yesterdaySessionsDeferred.await()
        val weeklySessions = weeklySessionsDeferred.await()
        val goal = goalDeferred.await()
        val bestSessionMinutes = bestSessionDeferred.await()

        // Calculate total usage today (in minutes)
        val totalUsageTodayMs = sessionsToday.sumOf { it.duration }
        val totalUsageTodayMin = totalUsageTodayMs / 1000 / 60

        // Calculate total usage yesterday (in minutes)
        val totalUsageYesterdayMin = sessionsYesterday.sumOf { it.duration } / 1000 / 60

        // Calculate weekly average
        val weeklyAverageMin = if (weeklySessions.isNotEmpty()) {
            weeklySessions.sumOf { it.duration } / 1000 / 60 / 7
        } else {
            0L
        }

        val goalMinutes = goal?.dailyLimitMinutes
        val streakDays = goal?.currentStreak ?: 0

        val daysSinceInstall = if (installDate > 0) {
            ((System.currentTimeMillis() - installDate) / (1000 * 60 * 60 * 24)).toInt()
        } else {
            0
        }

        // Determine user friction level
        val frictionLevel = dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.fromDaysSinceInstall(daysSinceInstall)

        // Track performance
        val decisionTimeMs = System.currentTimeMillis() - startTime
        if (decisionTimeMs > 50) {
            ErrorLogger.warning(
                "buildInterventionContext took ${decisionTimeMs}ms (target: <100ms)",
                context = "UsageMonitorService.buildInterventionContext"
            )
        }

        // Phase 3: Calculate behavioral cues
        val currentTime = System.currentTimeMillis()

        // 1. Rapid app switching: 3+ different apps in last 30 seconds
        val uniqueAppsIn30Sec = appLaunchTimestamps.map { it.second }.distinct().size
        val rapidAppSwitching = uniqueAppsIn30Sec >= 3

        // 2. Compulsive behavior: 3+ quick reopens in last 5 minutes
        val compulsiveBehavior = quickReopenTimestamps.size >= 3

        // 3. Unusual usage time: using between 1 AM - 5 AM or 23:00 - 01:00
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val unusualUsageTime = currentHour in 1..5 || currentHour == 23 || currentHour == 0

        // 4. Screen-on duration: continuous screen-on time
        val screenOnDuration = if (screenOnTimestamp > 0 && isScreenOn) {
            currentTime - screenOnTimestamp
        } else {
            0L
        }

        // 5. Unlock frequency: number of unlocks in last hour
        val unlockFrequency = unlockTimestamps.size

        ErrorLogger.debug(
            "Phase 3 Behavioral Cues: " +
            "rapidSwitch=$rapidAppSwitching ($uniqueAppsIn30Sec apps/30s), " +
            "compulsive=$compulsiveBehavior (${quickReopenTimestamps.size} quick reopens/5min), " +
            "unusualTime=$unusualUsageTime (hour=$currentHour), " +
            "screenOn=${screenOnDuration}ms, " +
            "unlocks=$unlockFrequency/hour",
            context = "UsageMonitorService.buildInterventionContext"
        )

        return dev.sadakat.thinkfaster.domain.intervention.InterventionContext.create(
            targetApp = sessionState.targetApp,
            currentSessionDuration = System.currentTimeMillis() - sessionState.startTimestamp,
            sessionCount = sessionsToday.size,
            lastSessionEndTime = if (sessionsToday.size > 1) {
                sessionsToday[sessionsToday.size - 2].endTimestamp ?: sessionsToday[sessionsToday.size - 2].startTimestamp
            } else {
                0L
            },
            totalUsageToday = totalUsageTodayMin,
            totalUsageYesterday = totalUsageYesterdayMin,
            weeklyAverage = weeklyAverageMin,
            goalMinutes = goalMinutes,
            streakDays = streakDays,
            userFrictionLevel = frictionLevel,
            installDate = installDate,
            bestSessionMinutes = bestSessionMinutes,
            // Phase 3: Pass behavioral cues
            rapidAppSwitching = rapidAppSwitching,
            compulsiveBehaviorDetected = compulsiveBehavior,
            unusualUsageTime = unusualUsageTime,
            screenOnDuration = screenOnDuration,
            unlockFrequency = unlockFrequency
        )
    }

    /**
     * Show reminder overlay when app is first opened
     * Phase 2 JITAI: Enhanced with persona and opportunity detection
     */
    private fun showReminderOverlay(sessionState: SessionDetector.SessionState) {
        // Build intervention context for JITAI decision making
        serviceScope.launch {
            try {
                val interventionContext = buildInterventionContext(sessionState)

                // Phase 2 JITAI: Use AdaptiveInterventionRateLimiter with persona and opportunity detection
                val sessionDuration = System.currentTimeMillis() - sessionState.startTimestamp
                val adaptiveResult = adaptiveRateLimiter.canShowIntervention(
                    interventionContext = interventionContext,
                    interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER,
                    sessionDurationMs = sessionDuration,
                    forceRefreshPersona = false
                )

                if (!adaptiveResult.allowed) {
                    ErrorLogger.info(
                        "Skipping reminder overlay - ${adaptiveResult.reason} | " +
                        "Persona: ${adaptiveResult.persona?.name}, " +
                        "Opportunity: ${adaptiveResult.opportunityLevel} (${adaptiveResult.opportunityScore}/100)",
                        context = "UsageMonitorService.showReminderOverlay"
                    )
                    return@launch
                }

                // Store JITAI context for overlay to retrieve via JitaiContextHolder
                adaptiveResult.persona?.let { persona ->
                    adaptiveResult.personaConfidence?.let { confidence ->
                        adaptiveResult.opportunityScore?.let { score ->
                            adaptiveResult.opportunityLevel?.let { level ->
                                adaptiveResult.decision?.let { decision ->
                                    val jitaiContext = dev.sadakat.thinkfaster.domain.intervention.JitaiContext(
                                        persona = persona,
                                        personaConfidence = confidence,
                                        opportunityScore = score,
                                        opportunityLevel = level,
                                        decision = decision,
                                        decisionSource = adaptiveResult.decisionSource
                                    )
                                    dev.sadakat.thinkfaster.domain.intervention.JitaiContextHolder.setContext(jitaiContext)
                                }
                            }
                        }
                    }
                }

                // Check if interventions are snoozed
                val interventionPrefs = dev.sadakat.thinkfaster.data.preferences.InterventionPreferences.getInstance(this@UsageMonitorService)
                if (interventionPrefs.isSnoozed()) {
                    val remainingMinutes = interventionPrefs.getSnoozeRemainingMinutes()
                    ErrorLogger.info(
                        "Skipping reminder overlay - snoozed for $remainingMinutes more minutes",
                        context = "UsageMonitorService.showReminderOverlay"
                    )
                    return@launch
                }

                // Check if overlay permission is granted
                if (!PermissionHelper.hasOverlayPermission(this@UsageMonitorService)) {
                    ErrorLogger.warning(
                        "Cannot show reminder overlay - overlay permission not granted",
                        context = "UsageMonitorService.showReminderOverlay"
                    )
                    return@launch
                }

                // Phase 4: Check timing optimization - delay if suboptimal timing
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val isWeekend = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) in
                    listOf(java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY)

                val timingRecommendation = try {
                    adaptiveContentSelector.recommendTiming(
                        targetApp = sessionState.targetApp,
                        currentHour = currentHour,
                        isWeekend = isWeekend
                    )
                } catch (e: Exception) {
                    ErrorLogger.warning(
                        "Failed to get timing recommendation: ${e.message}",
                        context = "UsageMonitorService.showReminderOverlay"
                    )
                    null  // Proceed without timing optimization
                }

                // Check if we should delay intervention based on learned timing patterns
                if (timingRecommendation != null && timingRecommendation.shouldDelay) {
                    val alternativeHours = if (timingRecommendation.alternativeHours.isNotEmpty()) {
                        timingRecommendation.alternativeHours.joinToString(", ")
                    } else {
                        "N/A"
                    }
                    ErrorLogger.info(
                        "Delaying reminder intervention - ${timingRecommendation.reason}. " +
                        "Current hour: ${currentHour}h, Alternative hours: [$alternativeHours], " +
                        "Delay: ${timingRecommendation.recommendedDelayMs}ms, " +
                        "Confidence: ${timingRecommendation.confidence}",
                        context = "UsageMonitorService.showReminderOverlay"
                    )
                    return@launch  // Skip showing overlay now
                }

                // Track that reminder overlay is showing
                reminderOverlayShowing = true
                reminderOverlayShownTime = System.currentTimeMillis()

                // Record that we showed an intervention (for rate limiting)
                adaptiveRateLimiter.recordIntervention(
                    dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER
                )

                // Show WindowManager-based overlay via OverlayManager (prevents simultaneity)
                overlayManager.showReminder(
                    sessionId = sessionState.sessionId,
                    targetApp = sessionState.targetApp
                )

                ErrorLogger.info(
                    "Reminder overlay shown with JITAI context: " +
                    "Persona=${adaptiveResult.persona?.name}, " +
                    "Opportunity=${adaptiveResult.opportunityLevel}(${adaptiveResult.opportunityScore}/100)",
                    context = "UsageMonitorService.showReminderOverlay"
                )
            } catch (e: Exception) {
                ErrorLogger.error(
                    e,
                    message = "Failed to show reminder overlay with JITAI",
                    context = "UsageMonitorService.showReminderOverlay"
                )
            }
        }
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
     * Phase 2 JITAI: Enhanced with persona and opportunity detection
     */
    private fun showTimerOverlay(sessionState: SessionDetector.SessionState) {
        ErrorLogger.info(
            "showTimerOverlay called for ${sessionState.targetApp}",
            context = "UsageMonitorService.showTimerOverlay"
        )

        // Build intervention context for JITAI decision making
        serviceScope.launch {
            try {
                val interventionContext = buildInterventionContext(sessionState)

                // Phase 2 JITAI: Use AdaptiveInterventionRateLimiter with persona and opportunity detection
                val sessionDuration = System.currentTimeMillis() - sessionState.startTimestamp
                val adaptiveResult = adaptiveRateLimiter.canShowIntervention(
                    interventionContext = interventionContext,
                    interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.TIMER,
                    sessionDurationMs = sessionDuration,
                    forceRefreshPersona = false
                )

                if (!adaptiveResult.allowed) {
                    ErrorLogger.info(
                        "Skipping timer overlay - ${adaptiveResult.reason} | " +
                        "Persona: ${adaptiveResult.persona?.name}, " +
                        "Opportunity: ${adaptiveResult.opportunityLevel} (${adaptiveResult.opportunityScore}/100)",
                        context = "UsageMonitorService.showTimerOverlay"
                    )
                    return@launch
                }

                // Store JITAI context for overlay to retrieve via JitaiContextHolder
                adaptiveResult.persona?.let { persona ->
                    adaptiveResult.personaConfidence?.let { confidence ->
                        adaptiveResult.opportunityScore?.let { score ->
                            adaptiveResult.opportunityLevel?.let { level ->
                                adaptiveResult.decision?.let { decision ->
                                    val jitaiContext = dev.sadakat.thinkfaster.domain.intervention.JitaiContext(
                                        persona = persona,
                                        personaConfidence = confidence,
                                        opportunityScore = score,
                                        opportunityLevel = level,
                                        decision = decision,
                                        decisionSource = adaptiveResult.decisionSource
                                    )
                                    dev.sadakat.thinkfaster.domain.intervention.JitaiContextHolder.setContext(jitaiContext)
                                }
                            }
                        }
                    }
                }

                // Check if interventions are snoozed
                val interventionPrefs = dev.sadakat.thinkfaster.data.preferences.InterventionPreferences.getInstance(this@UsageMonitorService)
                if (interventionPrefs.isSnoozed()) {
                    val remainingMinutes = interventionPrefs.getSnoozeRemainingMinutes()
                    ErrorLogger.info(
                        "Skipping timer overlay - snoozed for $remainingMinutes more minutes",
                        context = "UsageMonitorService.showTimerOverlay"
                    )
                    return@launch
                }

                // Check if overlay permission is granted
                if (!PermissionHelper.hasOverlayPermission(this@UsageMonitorService)) {
                    ErrorLogger.warning(
                        "Cannot show timer overlay - overlay permission not granted",
                        context = "UsageMonitorService.showTimerOverlay"
                    )
                    return@launch
                }

                // Phase 4: Check timing optimization - delay if suboptimal timing
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val isWeekend = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) in
                    listOf(java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY)

                val timingRecommendation = try {
                    adaptiveContentSelector.recommendTiming(
                        targetApp = sessionState.targetApp,
                        currentHour = currentHour,
                        isWeekend = isWeekend
                    )
                } catch (e: Exception) {
                    ErrorLogger.warning(
                        "Failed to get timing recommendation: ${e.message}",
                        context = "UsageMonitorService.showTimerOverlay"
                    )
                    null  // Proceed without timing optimization
                }

                // Check if we should delay intervention based on learned timing patterns
                if (timingRecommendation != null && timingRecommendation.shouldDelay) {
                    val alternativeHours = if (timingRecommendation.alternativeHours.isNotEmpty()) {
                        timingRecommendation.alternativeHours.joinToString(", ")
                    } else {
                        "N/A"
                    }
                    ErrorLogger.info(
                        "Delaying timer intervention - ${timingRecommendation.reason}. " +
                        "Current hour: ${currentHour}h, Alternative hours: [$alternativeHours], " +
                        "Delay: ${timingRecommendation.recommendedDelayMs}ms, " +
                        "Confidence: ${timingRecommendation.confidence}",
                        context = "UsageMonitorService.showTimerOverlay"
                    )
                    return@launch  // Skip showing overlay now, will check again next time
                }

                // Track that timer overlay is being shown
                timerOverlayShownTime = System.currentTimeMillis()

                // Record that we showed an intervention (for rate limiting)
                adaptiveRateLimiter.recordIntervention(
                    dev.sadakat.thinkfaster.domain.intervention.InterventionType.TIMER
                )

                // Show WindowManager-based overlay via OverlayManager (prevents simultaneity)
                overlayManager.showTimer(
                    sessionId = sessionState.sessionId,
                    targetApp = sessionState.targetApp,
                    startTime = sessionState.startTimestamp,
                    duration = sessionState.totalDuration
                )

                ErrorLogger.info(
                    "Timer overlay shown with JITAI context: " +
                    "Persona=${adaptiveResult.persona?.name}, " +
                    "Opportunity=${adaptiveResult.opportunityLevel}(${adaptiveResult.opportunityScore}/100)",
                    context = "UsageMonitorService.showTimerOverlay"
                )

                // Note: Session continues after timer alert dismissal
                // The alert will show again after another timer duration elapses
            } catch (e: Exception) {
                ErrorLogger.error(
                    e,
                    message = "Failed to show timer overlay with JITAI",
                    context = "UsageMonitorService.showTimerOverlay"
                )
            }
        }
    }

    /**
     * Handle screen off event
     */
    private fun onScreenOff() {
        // Phase 3: Reset screen-on duration tracking
        screenOnTimestamp = 0L

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
