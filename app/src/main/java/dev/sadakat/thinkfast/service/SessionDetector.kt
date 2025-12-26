package dev.sadakat.thinkfast.service

import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.UsageEvent
import dev.sadakat.thinkfast.domain.model.UsageSession
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages session detection and lifecycle
 * Handles session start, continuation, and end based on app usage patterns
 */
class SessionDetector(
    private val usageRepository: UsageRepository,
    private val scope: CoroutineScope
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
        var hasShownTenMinuteAlert: Boolean = false
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
    fun onAppInForeground(targetApp: AppTarget, timestamp: Long) {
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

        // If gap threshold exceeded, end the session
        if (timeSinceLastActive >= Constants.SESSION_GAP_THRESHOLD) {
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
     * Get the duration of the current session in milliseconds
     */
    fun getCurrentSessionDuration(): Long {
        val current = currentSession ?: return 0L
        return System.currentTimeMillis() - current.startTimestamp
    }

    /**
     * Initialize by loading any active session from database
     * Call this when service starts
     */
    suspend fun initialize() {
        val activeSession = usageRepository.getActiveSession()
        if (activeSession != null) {
            // Resume the active session
            val targetApp = AppTarget.fromPackageName(activeSession.targetApp)
            if (targetApp != null) {
                currentSession = SessionState(
                    sessionId = activeSession.id,
                    targetApp = targetApp,
                    startTimestamp = activeSession.startTimestamp,
                    lastActiveTimestamp = System.currentTimeMillis(),
                    totalDuration = activeSession.duration
                )
            }
        }
    }

    /**
     * Start a new session
     */
    private fun startNewSession(targetApp: AppTarget, timestamp: Long) {
        scope.launch {
            // Create session in database
            val session = UsageSession(
                targetApp = targetApp.packageName,
                startTimestamp = timestamp,
                endTimestamp = null,
                duration = 0L,
                date = dateFormatter.format(Date(timestamp))
            )

            val sessionId = usageRepository.insertSession(session)

            // Create session state
            val sessionState = SessionState(
                sessionId = sessionId,
                targetApp = targetApp,
                startTimestamp = timestamp,
                lastActiveTimestamp = timestamp
            )

            currentSession = sessionState

            // Log session start event
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = sessionId,
                    eventType = Constants.EVENT_APP_OPENED,
                    timestamp = timestamp,
                    metadata = "App: ${targetApp.displayName}"
                )
            )

            // Notify callback
            onSessionStart?.invoke(sessionState)
        }
    }

    /**
     * Continue the current session
     */
    private fun continueSession(timestamp: Long) {
        val current = currentSession ?: return

        // Update last active timestamp
        current.lastActiveTimestamp = timestamp

        // Calculate total duration
        val duration = timestamp - current.startTimestamp
        current.totalDuration = duration

        // Check if 10-minute threshold is reached and alert hasn't been shown yet
        if (!current.hasShownTenMinuteAlert && duration >= Constants.TEN_MINUTES_MILLIS) {
            current.hasShownTenMinuteAlert = true
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
