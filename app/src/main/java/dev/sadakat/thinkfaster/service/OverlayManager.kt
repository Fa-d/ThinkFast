package dev.sadakat.thinkfaster.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import dev.sadakat.thinkfaster.presentation.overlay.ReminderOverlayWindow
import dev.sadakat.thinkfaster.presentation.overlay.TimerOverlayWindow
import dev.sadakat.thinkfaster.util.ErrorLogger

/**
 * Centralized manager for overlay lifecycle and mutual exclusion
 * Ensures only one overlay shows at a time with smooth transitions
 *
 * Phase: Overlay UX Fix
 * Purpose: Prevent simultaneous overlay display and manage transitions
 */
class OverlayManager(
    private val context: Context,
    private val reminderOverlay: ReminderOverlayWindow,
    private val timerOverlay: TimerOverlayWindow
) {
    private var currentOverlay: OverlayType? = null
    private val overlayLock = Any()
    private val handler = Handler(Looper.getMainLooper())

    enum class OverlayType {
        REMINDER, TIMER
    }

    /**
     * Show reminder overlay - dismisses timer if showing
     *
     * @param sessionId The current session ID
     * @param targetApp The target app package name
     * @return true if overlay was shown, false if already showing same overlay
     */
    fun showReminder(sessionId: Long, targetApp: String): Boolean {
        synchronized(overlayLock) {
            ErrorLogger.info(
                "showReminder called - current overlay: $currentOverlay",
                context = "OverlayManager"
            )

            when (currentOverlay) {
                OverlayType.TIMER -> {
                    ErrorLogger.info(
                        "Timer overlay is showing - dismissing before showing reminder",
                        context = "OverlayManager"
                    )
                    timerOverlay.dismiss()
                    // Small delay to ensure clean transition
                    handler.postDelayed({
                        showReminderInternal(sessionId, targetApp)
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
                    showReminderInternal(sessionId, targetApp)
                    return true
                }
            }
        }
    }

    /**
     * Show timer overlay - dismisses reminder if showing
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
            ErrorLogger.info(
                "showTimer called - current overlay: $currentOverlay",
                context = "OverlayManager"
            )

            when (currentOverlay) {
                OverlayType.REMINDER -> {
                    ErrorLogger.info(
                        "Reminder overlay is showing - dismissing before showing timer",
                        context = "OverlayManager"
                    )
                    reminderOverlay.dismiss()
                    // Small delay to ensure clean transition
                    handler.postDelayed({
                        showTimerInternal(sessionId, targetApp, startTime, duration)
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
                    showTimerInternal(sessionId, targetApp, startTime, duration)
                    return true
                }
            }
        }
    }

    /**
     * Internal method to show reminder overlay
     */
    private fun showReminderInternal(sessionId: Long, targetApp: String) {
        synchronized(overlayLock) {
            ErrorLogger.info(
                "Showing reminder overlay for session $sessionId, app: $targetApp",
                context = "OverlayManager"
            )
            reminderOverlay.show(sessionId, targetApp)
            currentOverlay = OverlayType.REMINDER
        }
    }

    /**
     * Internal method to show timer overlay
     */
    private fun showTimerInternal(
        sessionId: Long,
        targetApp: String,
        startTime: Long,
        duration: Long
    ) {
        synchronized(overlayLock) {
            ErrorLogger.info(
                "Showing timer overlay for session $sessionId, app: $targetApp",
                context = "OverlayManager"
            )
            timerOverlay.show(sessionId, targetApp, startTime, duration)
            currentOverlay = OverlayType.TIMER
        }
    }

    /**
     * Dismiss all overlays
     */
    fun dismissAll() {
        synchronized(overlayLock) {
            ErrorLogger.info(
                "Dismissing all overlays - current: $currentOverlay",
                context = "OverlayManager"
            )
            reminderOverlay.dismiss()
            timerOverlay.dismiss()
            currentOverlay = null
        }
    }

    /**
     * Callback when reminder overlay is dismissed
     * Call this from UsageMonitorService's onReminderOverlayDismissed
     */
    fun onReminderDismissed() {
        synchronized(overlayLock) {
            if (currentOverlay == OverlayType.REMINDER) {
                ErrorLogger.info(
                    "Reminder overlay dismissed - clearing current overlay state",
                    context = "OverlayManager"
                )
                currentOverlay = null
            }
        }
    }

    /**
     * Callback when timer overlay is dismissed
     * Call this from UsageMonitorService's onTimerOverlayDismissed
     */
    fun onTimerDismissed() {
        synchronized(overlayLock) {
            if (currentOverlay == OverlayType.TIMER) {
                ErrorLogger.info(
                    "Timer overlay dismissed - clearing current overlay state",
                    context = "OverlayManager"
                )
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
