package dev.sadakat.thinkfast.service

import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.UsageEvent
import dev.sadakat.thinkfast.domain.model.UsageSession
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.util.Constants
import dev.sadakat.thinkfast.util.ErrorLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages session detection and lifecycle
 * Handles session start, continuation, and end based on app usage patterns
 */
class SessionDetector(
    private val usageRepository: UsageRepository,
    private val scope: CoroutineScope,
    private val getTimerDurationMillis: () -> Long  // Function to get dynamic timer duration
) {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Data class representing the current session state
     */
    data class SessionState(
        val sessionId: Long,
        val targetApp: AppTarget,
        val startTimestamp: Long,
        var lastActiveTimestamp: Long,
        var totalDuration: Long = 0L,
        var timerStartTime: Long = 0L,  // When the current timer countdown started (reset on reminder shown)
        var lastTimerAlertTime: Long = 0L  // Track when the last timer alert was shown (relative to timerStartTime)
    )

    // Current active session (null if no session is active)
    private var currentSession: SessionState? = null

    // Callback for when a new session starts
    var onSessionStart: ((SessionState) -> Unit)? = null

    // Callback for when session needs 10-minute alert
    var onTenMinuteAlert: ((SessionState) -> Unit)? = null

    // Callback for when session ends
    var onSessionEnd: ((SessionState) -> Unit)? = null

    /**
     * Process app foreground event
     * This should be called when a target app is detected in foreground
     *
     * @param targetApp the detected target app
     * @param timestamp the timestamp when the app was detected
     */
    suspend fun onAppInForeground(targetApp: AppTarget, timestamp: Long) {
        val current = currentSession

        if (current == null) {
            // No active session - start a new one
            startNewSession(targetApp, timestamp)
        } else {
            // Active session exists
            val timeSinceLastActive = timestamp - current.lastActiveTimestamp

            if (current.targetApp == targetApp && timeSinceLastActive < Constants.SESSION_GAP_THRESHOLD) {
                // Same app, within gap threshold - continue session
                continueSession(timestamp)
            } else {
                // Either different app or gap threshold exceeded - end current and start new
                endCurrentSession(timestamp, wasInterrupted = false)
                startNewSession(targetApp, timestamp)
            }
        }
    }

    /**
     * Process app background event
     * This should be called periodically to check if session should end due to inactivity
     *
     * @param currentTime the current timestamp
     */
    fun checkForSessionTimeout(currentTime: Long) {
        val current = currentSession ?: return

        val timeSinceLastActive = currentTime - current.lastActiveTimestamp

        // Don't end the session if total duration is less than minimum session duration
        // This handles the case where our overlay covers the app temporarily
        val totalSessionDuration = currentTime - current.startTimestamp

        // Only end session if BOTH conditions are met:
        // 1. Gap threshold exceeded (30 seconds of no activity)
        // 2. Total session duration is at least the minimum (5 seconds)
        // This prevents ending sessions that were just interrupted by our reminder overlay
        if (timeSinceLastActive >= Constants.SESSION_GAP_THRESHOLD && totalSessionDuration >= Constants.MIN_SESSION_DURATION) {
            endCurrentSession(current.lastActiveTimestamp + Constants.SESSION_GAP_THRESHOLD, wasInterrupted = false)
        }
    }

    /**
     * Force end the current session (e.g., when screen turns off or user triggers 10-min alert)
     *
     * @param timestamp the timestamp when the session should end
     * @param wasInterrupted whether the session was ended due to an alert
     * @param interruptionType the type of interruption (if any)
     */
    fun forceEndSession(
        timestamp: Long = System.currentTimeMillis(),
        wasInterrupted: Boolean = false,
        interruptionType: String? = null
    ) {
        endCurrentSession(timestamp, wasInterrupted, interruptionType)
    }

    /**
     * Get the current active session
     */
    fun getCurrentSession(): SessionState? = currentSession

    /**
     * Check if there's an active session
     */
    fun hasActiveSession(): Boolean = currentSession != null

    /**
     * Reset the timer for the current session
     * This should be called when reminder overlay is shown
     * so the timer countdown starts fresh after reminder dismissal
     */
    fun resetTimer() {
        currentSession?.let {
            it.timerStartTime = System.currentTimeMillis()
            it.lastTimerAlertTime = 0L
        }
    }

    /**
     * Get the duration of the current session in milliseconds
     */
    fun getCurrentSessionDuration(): Long {
        val current = currentSession ?: return 0L
        return System.currentTimeMillis() - current.startTimestamp
    }

    /**
     * Initialize by loading any active session from database
     * Call this when service starts
     * Note: We don't resume old sessions to avoid timer triggering immediately
     * Old sessions will be ended by the database's automatic timeout logic
     */
    suspend fun initialize() {
        // Don't resume old sessions - always start fresh when app is opened
        // This prevents timer from triggering immediately after reminder dismissal
        currentSession = null
    }

    /**
     * Start a new session
     * NOTE: This is a suspend function that performs database insert non-blockingly
     */
    private suspend fun startNewSession(targetApp: AppTarget, timestamp: Long) {
        // Create session in database using withContext (non-blocking suspend)
        // This ensures callbacks have a valid session ID without blocking the thread
        val sessionId = withContext(Dispatchers.IO) {
            // Create session in database
            val session = UsageSession(
                targetApp = targetApp.packageName,
                startTimestamp = timestamp,
                endTimestamp = null,
                duration = 0L,
                date = dateFormatter.format(Date(timestamp))
            )
            usageRepository.insertSession(session)
        }

        // Create session state with valid session ID
        val sessionState = SessionState(
            sessionId = sessionId,
            targetApp = targetApp,
            startTimestamp = timestamp,
            lastActiveTimestamp = timestamp,
            timerStartTime = timestamp  // Timer starts when session begins
        )
        currentSession = sessionState

        // Log session start event asynchronously (non-blocking)
        scope.launch {
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = sessionId,
                    eventType = Constants.EVENT_APP_OPENED,
                    timestamp = timestamp,
                    metadata = "App: ${targetApp.displayName}"
                )
            )
        }

        // Notify callback with valid session state
        onSessionStart?.invoke(sessionState)
    }

    /**
     * Continue the current session
     */
    private fun continueSession(timestamp: Long) {
        val current = currentSession ?: return

        // Update last active timestamp
        current.lastActiveTimestamp = timestamp

        // Calculate total duration (for database tracking)
        val duration = timestamp - current.startTimestamp
        current.totalDuration = duration

        // Check if timer threshold is reached
        // The alert should show periodically at timerDuration intervals
        // Timer is based on timerStartTime (resets when reminder is shown)
        val timerDuration = getTimerDurationMillis()
        val timeSinceTimerStarted = timestamp - current.timerStartTime

        // Time since the last alert was shown (relative to timerStartTime)
        val timeSinceLastAlert = if (current.lastTimerAlertTime == 0L) {
            timeSinceTimerStarted  // No alert shown yet, use full time since timer started
        } else {
            timeSinceTimerStarted - current.lastTimerAlertTime  // Time since last alert
        }

        // Debug logging to help diagnose timer issues (only log when close to threshold or when triggered)
        val timeUntilThreshold = timerDuration - timeSinceLastAlert
        if (timeUntilThreshold <= 30000L || timeSinceLastAlert >= timerDuration) {
            ErrorLogger.info(
                "Timer check: duration=${timerDuration}ms (${timerDuration/60000}min), " +
                "timeSinceTimerStarted=${timeSinceTimerStarted}ms (${timeSinceTimerStarted/1000}s), " +
                "timeSinceLastAlert=${timeSinceLastAlert}ms (${timeSinceLastAlert/1000}s), " +
                "timeUntilThreshold=${timeUntilThreshold}ms (${timeUntilThreshold/1000}s), " +
                "willTrigger=${timeSinceLastAlert >= timerDuration}",
                context = "SessionDetector.continueSession"
            )
        }

        if (timeSinceLastAlert >= timerDuration) {
            ErrorLogger.info(
                "Timer threshold reached! Triggering alert for ${current.targetApp.displayName}",
                context = "SessionDetector.continueSession"
            )
            current.lastTimerAlertTime = timeSinceTimerStarted
            onTenMinuteAlert?.invoke(current)
        }

        // Update session duration in database periodically
        scope.launch {
            usageRepository.updateSession(
                UsageSession(
                    id = current.sessionId,
                    targetApp = current.targetApp.packageName,
                    startTimestamp = current.startTimestamp,
                    endTimestamp = null,
                    duration = duration,
                    date = dateFormatter.format(Date(current.startTimestamp))
                )
            )
        }
    }

    /**
     * End the current session
     */
    private fun endCurrentSession(
        endTimestamp: Long,
        wasInterrupted: Boolean = false,
        interruptionType: String? = null
    ) {
        val current = currentSession ?: return

        val duration = endTimestamp - current.startTimestamp

        // Only save session if it meets minimum duration threshold
        if (duration >= Constants.MIN_SESSION_DURATION) {
            scope.launch {
                // Update session in database
                usageRepository.endSession(
                    sessionId = current.sessionId,
                    endTimestamp = endTimestamp,
                    wasInterrupted = wasInterrupted,
                    interruptionType = interruptionType
                )

                // Log session end event
                usageRepository.insertEvent(
                    UsageEvent(
                        sessionId = current.sessionId,
                        eventType = Constants.EVENT_SESSION_ENDED,
                        timestamp = endTimestamp,
                        metadata = "Duration: ${duration}ms, Interrupted: $wasInterrupted"
                    )
                )
            }

            // Notify callback
            onSessionEnd?.invoke(current)
        } else {
            // Session too short - delete it
            scope.launch {
                usageRepository.endSession(
                    sessionId = current.sessionId,
                    endTimestamp = endTimestamp,
                    wasInterrupted = false,
                    interruptionType = null
                )
            }
        }

        // Clear current session
        currentSession = null
    }

    /**
     * Reset the detector state
     */
    fun reset() {
        currentSession = null
    }
}
