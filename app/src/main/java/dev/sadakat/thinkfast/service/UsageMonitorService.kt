package dev.sadakat.thinkfast.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dev.sadakat.thinkfast.MainActivity
import dev.sadakat.thinkfast.R
import dev.sadakat.thinkfast.domain.model.UsageEvent
import dev.sadakat.thinkfast.domain.repository.SettingsRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.presentation.overlay.ReminderOverlayWindow
import dev.sadakat.thinkfast.presentation.overlay.TimerOverlayWindow
import dev.sadakat.thinkfast.util.Constants
import dev.sadakat.thinkfast.util.ErrorLogger
import dev.sadakat.thinkfast.util.PermissionHelper
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

    private lateinit var appLaunchDetector: AppLaunchDetector
    private lateinit var sessionDetector: SessionDetector

    // WindowManager-based overlays
    private lateinit var reminderOverlay: ReminderOverlayWindow
    private lateinit var timerOverlay: TimerOverlayWindow

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitoringJob: Job? = null

    private var isScreenOn = true
    private var isTargetAppActive = false
    private var lastTargetAppDetectedTime = 0L
    private var isPowerSaveMode = false

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
        appLaunchDetector = AppLaunchDetector(this)
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

        // Initialize WindowManager-based overlays
        // These will be created even without permission, but won't show until granted
        reminderOverlay = ReminderOverlayWindow(this)
        timerOverlay = TimerOverlayWindow(this)

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

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())

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

        // Dismiss any showing overlays
        try {
            reminderOverlay.dismiss()
            timerOverlay.dismiss()
        } catch (e: Exception) {
            // Overlays might not be initialized
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

            // Continue the session (this will trigger timer check)
            sessionDetector.onAppInForeground(currentForegroundApp, currentTime)
        } else {
            // No target app in foreground
            isTargetAppActive = false
            // Check for session timeout
            sessionDetector.checkForSessionTimeout(System.currentTimeMillis())
        }
    }

    /**
     * Handle target app detection
     */
    private suspend fun onTargetAppDetected(launchEvent: AppLaunchDetector.AppLaunchEvent) {
        // Update session detector first (this will trigger onSessionStart for new sessions)
        sessionDetector.onAppInForeground(launchEvent.appTarget, launchEvent.timestamp)

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
                            metadata = "App: ${launchEvent.appTarget.displayName} (Every Time)"
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
            // Get settings to check reminder behavior
            val settings = runBlocking {
                settingsRepository.getSettingsOnce()
            }

            // Only show reminder here if alwaysShowReminder is false
            // (If true, it will be shown in onTargetAppDetected instead)
            if (!settings.alwaysShowReminder) {
                showReminderOverlay(sessionState)

                // Log event
                serviceScope.launch {
                    usageRepository.insertEvent(
                        UsageEvent(
                            sessionId = sessionState.sessionId,
                            eventType = Constants.EVENT_REMINDER_SHOWN,
                            timestamp = System.currentTimeMillis(),
                            metadata = "App: ${sessionState.targetApp.displayName} (Session Start)"
                        )
                    )
                }
            }
        }

        // Called when configured timer threshold is reached
        sessionDetector.onTenMinuteAlert = { sessionState ->
            // Show timer overlay
            showTimerOverlay(sessionState)

            // Log event
            serviceScope.launch {
                usageRepository.insertEvent(
                    UsageEvent(
                        sessionId = sessionState.sessionId,
                        eventType = Constants.EVENT_TEN_MIN_ALERT,
                        timestamp = System.currentTimeMillis(),
                        metadata = "Duration: ${sessionState.totalDuration}ms"
                    )
                )
            }
        }

        // Called when session ends
        sessionDetector.onSessionEnd = { sessionState ->
            isTargetAppActive = false
        }
    }

    /**
     * Show reminder overlay when app is first opened
     */
    private fun showReminderOverlay(sessionState: SessionDetector.SessionState) {
        // Check if overlay permission is granted
        if (!PermissionHelper.hasOverlayPermission(this)) {
            ErrorLogger.warning(
                "Cannot show reminder overlay - overlay permission not granted",
                context = "UsageMonitorService.showReminderOverlay"
            )
            return
        }

        ErrorLogger.debug(
            "Showing reminder overlay for ${sessionState.targetApp.displayName}",
            context = "UsageMonitorService.showReminderOverlay"
        )

        // Show WindowManager-based overlay (appears on top of current app)
        reminderOverlay.show(
            sessionId = sessionState.sessionId,
            targetApp = sessionState.targetApp
        )
    }

    /**
     * Show timer overlay after configured duration
     */
    private fun showTimerOverlay(sessionState: SessionDetector.SessionState) {
        // Check if overlay permission is granted
        if (!PermissionHelper.hasOverlayPermission(this)) {
            ErrorLogger.warning(
                "Cannot show timer overlay - overlay permission not granted",
                context = "UsageMonitorService.showTimerOverlay"
            )
            return
        }

        ErrorLogger.debug(
            "Showing timer overlay for ${sessionState.targetApp.displayName} (duration: ${sessionState.totalDuration}ms)",
            context = "UsageMonitorService.showTimerOverlay"
        )

        // Show WindowManager-based overlay (appears on top of current app)
        timerOverlay.show(
            sessionId = sessionState.sessionId,
            targetApp = sessionState.targetApp,
            sessionStartTime = sessionState.startTimestamp,
            sessionDuration = sessionState.totalDuration
        )

        // Force end the session after showing timer alert
        sessionDetector.forceEndSession(
            timestamp = System.currentTimeMillis(),
            wasInterrupted = true,
            interruptionType = Constants.INTERRUPTION_TEN_MINUTE_ALERT
        )
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
