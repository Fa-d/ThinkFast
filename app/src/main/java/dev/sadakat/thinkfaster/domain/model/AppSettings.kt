package dev.sadakat.thinkfaster.domain.model

/**
 * Domain model for app settings/preferences
 */
data class AppSettings(
    val timerAlertMinutes: Int = 10,        // Duration before showing timer alert (default 10 minutes)
    val alwaysShowReminder: Boolean = true,  // Whether to show reminder every time app is opened (default true)
    val lockedMode: Boolean = false          // Phase F: Maximum friction mode for users who want extra control
) {
    /**
     * Get timer duration in milliseconds
     */
    fun getTimerDurationMillis(): Long {
        return timerAlertMinutes * 60 * 1000L
    }

    /**
     * Format timer duration for display
     */
    fun formatTimerDuration(): String {
        return when {
            timerAlertMinutes >= 60 -> {
                val hours = timerAlertMinutes / 60
                val mins = timerAlertMinutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
            else -> "$timerAlertMinutes minutes"
        }
    }
}
