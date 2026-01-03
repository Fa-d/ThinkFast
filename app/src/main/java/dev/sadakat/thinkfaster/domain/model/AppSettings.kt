package dev.sadakat.thinkfaster.domain.model

/**
 * Overlay display style options
 */
enum class OverlayStyle {
    FULLSCREEN,  // Full-screen coverage (default)
    COMPACT      // Center card popup (~70% screen)
}

/**
 * Domain model for app settings/preferences
 */
data class AppSettings(
    val timerAlertMinutes: Int = 10,        // Duration before showing timer alert (default 10 minutes)
    val alwaysShowReminder: Boolean = false,  // Whether to show reminder every time app is opened (default false)
    val lockedMode: Boolean = false,         // Phase F: Maximum friction mode for users who want extra control
    val overlayStyle: OverlayStyle = OverlayStyle.FULLSCREEN,  // Overlay display style (full-screen vs compact)

    // Push Notification Strategy: Motivational notification settings
    val motivationalNotificationsEnabled: Boolean = true,
    val morningNotificationHour: Int = 8,
    val morningNotificationMinute: Int = 0,
    val eveningNotificationHour: Int = 20,
    val eveningNotificationMinute: Int = 0
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

    /**
     * Format morning notification time for display (12-hour format with AM/PM)
     */
    fun getMorningTimeFormatted(): String {
        val amPm = if (morningNotificationHour < 12) "AM" else "PM"
        val displayHour = when {
            morningNotificationHour == 0 -> 12
            morningNotificationHour > 12 -> morningNotificationHour - 12
            else -> morningNotificationHour
        }
        return String.format("%02d:%02d %s", displayHour, morningNotificationMinute, amPm)
    }

    /**
     * Format evening notification time for display (12-hour format with AM/PM)
     */
    fun getEveningTimeFormatted(): String {
        val amPm = if (eveningNotificationHour < 12) "AM" else "PM"
        val displayHour = when {
            eveningNotificationHour == 0 -> 12
            eveningNotificationHour > 12 -> eveningNotificationHour - 12
            else -> eveningNotificationHour
        }
        return String.format("%02d:%02d %s", displayHour, eveningNotificationMinute, amPm)
    }
}
