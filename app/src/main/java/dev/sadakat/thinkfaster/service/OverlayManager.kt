package dev.sadakat.thinkfaster.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import dev.sadakat.thinkfaster.domain.model.OverlayStyle
import dev.sadakat.thinkfaster.domain.repository.SettingsRepository
import dev.sadakat.thinkfaster.presentation.overlay.CompactReminderOverlayWindow
import dev.sadakat.thinkfaster.presentation.overlay.CompactTimerOverlayWindow
import dev.sadakat.thinkfaster.presentation.overlay.ReminderOverlayWindow
import dev.sadakat.thinkfaster.presentation.overlay.TimerOverlayWindow
import dev.sadakat.thinkfaster.util.ErrorLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

/**
 * Centralized manager for overlay lifecycle and mutual exclusion
 * Ensures only one overlay shows at a time with smooth transitions
 *
 * Phase: Overlay UX Fix
 * Purpose: Prevent simultaneous overlay display and manage transitions
 *
 * Phase: Compact Overlay Mode
 * Purpose: Route to full-screen or compact overlays based on user preference
 */
class OverlayManager(
    private val context: Context,
    private val reminderOverlay: ReminderOverlayWindow,
    private val timerOverlay: TimerOverlayWindow,
    private val compactReminderOverlay: CompactReminderOverlayWindow,
    private val compactTimerOverlay: CompactTimerOverlayWindow,
    private val settingsRepository: SettingsRepository
) {
    private var currentOverlay: OverlayType? = null
    private val overlayLock = Any()
    private val handler = Handler(Looper.getMainLooper())

    // Coroutine scope for async operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    enum class OverlayType {
        REMINDER, TIMER
    }

    /**
     * Show reminder overlay - dismisses timer if showing
     * Routes to full-screen or compact based on user preference
     *
     * @param sessionId The current session ID
     * @param targetApp The target app package name
     * @return true if overlay was shown, false if already showing same overlay
     */
    fun showReminder(sessionId: Long, targetApp: String): Boolean {
        synchronized(overlayLock) {
            // Get overlay style from settings synchronously (using runBlocking is acceptable here
            // as this is called from background thread and we need the value immediately)
            val isCompactMode = runBlocking {
                settingsRepository.getSettingsOnce().overlayStyle == OverlayStyle.COMPACT
            }

            ErrorLogger.info(
                "showReminder called - current overlay: $currentOverlay, style: ${if (isCompactMode) "COMPACT" else "FULLSCREEN"}",
                context = "OverlayManager"
            )

            when (currentOverlay) {
                OverlayType.TIMER -> {
                    ErrorLogger.info(
                        "Timer overlay is showing - dismissing before showing reminder",
                        context = "OverlayManager"
                    )
                    // Dismiss both timer overlays to be safe
                    timerOverlay.dismiss()
                    compactTimerOverlay.dismiss()
                    // Small delay to ensure clean transition
                    handler.postDelayed({
                        showReminderInternal(sessionId, targetApp, isCompactMode)
                    }, TRANSITION_DELAY_MS)
                    return true
                }
                OverlayType.REMINDER -> {
                    ErrorLogger.info(
                        "Reminder overlay already showing - skipping",
                        context = "OverlayManager"
                    )
                    return false
                }
                null -> {
                    showReminderInternal(sessionId, targetApp, isCompactMode)
                    return true
                }
            }
        }
    }

    /**
     * Show timer overlay - dismisses reminder if showing
     * Routes to full-screen or compact based on user preference
     *
     * @param sessionId The current session ID
     * @param targetApp The target app package name
     * @param startTime Session start time in millis
     * @param duration Timer duration in millis
     * @return true if overlay was shown, false if already showing same overlay
     */
    fun showTimer(
        sessionId: Long,
        targetApp: String,
        startTime: Long,
        duration: Long
    ): Boolean {
        synchronized(overlayLock) {
            // Get overlay style from settings synchronously (using runBlocking is acceptable here
            // as this is called from background thread and we need the value immediately)
            val isCompactMode = runBlocking {
                settingsRepository.getSettingsOnce().overlayStyle == OverlayStyle.COMPACT
            }

            ErrorLogger.info(
                "showTimer called - current overlay: $currentOverlay, style: ${if (isCompactMode) "COMPACT" else "FULLSCREEN"}",
                context = "OverlayManager"
            )

            when (currentOverlay) {
                OverlayType.REMINDER -> {
                    ErrorLogger.info(
                        "Reminder overlay is showing - dismissing before showing timer",
                        context = "OverlayManager"
                    )
                    // Dismiss both reminder overlays to be safe
                    reminderOverlay.dismiss()
                    compactReminderOverlay.dismiss()
                    // Small delay to ensure clean transition
                    handler.postDelayed({
                        showTimerInternal(sessionId, targetApp, startTime, duration, isCompactMode)
                    }, TRANSITION_DELAY_MS)
                    return true
                }
                OverlayType.TIMER -> {
                    ErrorLogger.info(
                        "Timer overlay already showing - skipping",
                        context = "OverlayManager"
                    )
                    return false
                }
                null -> {
                    showTimerInternal(sessionId, targetApp, startTime, duration, isCompactMode)
                    return true
                }
            }
        }
    }

    /**
     * Internal method to show reminder overlay
     * Routes to compact or full-screen based on isCompactMode
     */
    private fun showReminderInternal(sessionId: Long, targetApp: String, isCompactMode: Boolean) {
        synchronized(overlayLock) {
            ErrorLogger.info(
                "showReminderInternal - ENTERING - isCompactMode=$isCompactMode, sessionId=$sessionId, app=$targetApp",
                context = "OverlayManager"
            )

            try {
                if (isCompactMode) {
                    ErrorLogger.info(
                        "Calling compactReminderOverlay.show()",
                        context = "OverlayManager"
                    )
                    compactReminderOverlay.show(sessionId, targetApp)
                } else {
                    ErrorLogger.info(
                        "Calling reminderOverlay.show()",
                        context = "OverlayManager"
                    )
                    reminderOverlay.show(sessionId, targetApp)
                }
                currentOverlay = OverlayType.REMINDER
                ErrorLogger.info(
                    "showReminderInternal - COMPLETED SUCCESSFULLY",
                    context = "OverlayManager"
                )
            } catch (e: Exception) {
                ErrorLogger.error(
                    e,
                    "showReminderInternal - FAILED",
                    context = "OverlayManager"
                )
            }
        }
    }

    /**
     * Internal method to show timer overlay
     * Routes to compact or full-screen based on isCompactMode
     */
    private fun showTimerInternal(
        sessionId: Long,
        targetApp: String,
        startTime: Long,
        duration: Long,
        isCompactMode: Boolean
    ) {
        synchronized(overlayLock) {
            ErrorLogger.info(
                "Showing ${if (isCompactMode) "compact" else "full-screen"} timer overlay for session $sessionId, app: $targetApp",
                context = "OverlayManager"
            )
            if (isCompactMode) {
                compactTimerOverlay.show(sessionId, targetApp, startTime, duration)
            } else {
                timerOverlay.show(sessionId, targetApp, startTime, duration)
            }
            currentOverlay = OverlayType.TIMER
        }
    }

    /**
     * Dismiss all overlays (both full-screen and compact)
     */
    fun dismissAll() {
        synchronized(overlayLock) {
            ErrorLogger.info(
                "Dismissing all overlays - current: $currentOverlay",
                context = "OverlayManager"
            )
            // Dismiss all 4 overlay windows to be safe
            reminderOverlay.dismiss()
            timerOverlay.dismiss()
            compactReminderOverlay.dismiss()
            compactTimerOverlay.dismiss()
            currentOverlay = null
        }
    }

    /**
     * Callback when reminder overlay is dismissed
     * Call this from UsageMonitorService's onReminderOverlayDismissed
     * Dismisses both full-screen and compact reminder overlays
     */
    fun onReminderDismissed() {
        synchronized(overlayLock) {
            if (currentOverlay == OverlayType.REMINDER) {
                ErrorLogger.info(
                    "Reminder overlay dismissed - clearing current overlay state",
                    context = "OverlayManager"
                )
                // Dismiss both to be safe (only one should be showing)
                reminderOverlay.dismiss()
                compactReminderOverlay.dismiss()
                currentOverlay = null
            }
        }
    }

    /**
     * Callback when timer overlay is dismissed
     * Call this from UsageMonitorService's onTimerOverlayDismissed
     * Dismisses both full-screen and compact timer overlays
     */
    fun onTimerDismissed() {
        synchronized(overlayLock) {
            if (currentOverlay == OverlayType.TIMER) {
                ErrorLogger.info(
                    "Timer overlay dismissed - clearing current overlay state",
                    context = "OverlayManager"
                )
                // Dismiss both to be safe (only one should be showing)
                timerOverlay.dismiss()
                compactTimerOverlay.dismiss()
                currentOverlay = null
            }
        }
    }

    /**
     * Check if any overlay is currently showing
     */
    fun isAnyOverlayShowing(): Boolean {
        synchronized(overlayLock) {
            return currentOverlay != null
        }
    }

    /**
     * Get the current overlay type, if any
     */
    fun getCurrentOverlayType(): OverlayType? {
        synchronized(overlayLock) {
            return currentOverlay
        }
    }

    companion object {
        /**
         * Delay in milliseconds between dismissing one overlay and showing another
         * Ensures smooth transition and prevents visual glitches
         */
        private const val TRANSITION_DELAY_MS = 100L
    }
}
