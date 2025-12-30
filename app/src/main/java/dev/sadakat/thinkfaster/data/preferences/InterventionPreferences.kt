package dev.sadakat.thinkfaster.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dev.sadakat.thinkfaster.domain.intervention.FrictionLevel

/**
 * Manages user preferences for intervention system
 * Phase F: Stores friction level override and related settings
 */
class InterventionPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Get the user's preferred friction level
     * Returns null if user hasn't set an override (use automatic calculation)
     */
    fun getFrictionLevelOverride(): FrictionLevel? {
        val levelName = prefs.getString(KEY_FRICTION_OVERRIDE, null) ?: return null
        return try {
            FrictionLevel.valueOf(levelName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Set a friction level override
     * Pass null to clear the override and use automatic calculation
     */
    fun setFrictionLevelOverride(level: FrictionLevel?) {
        prefs.edit().apply {
            if (level == null) {
                remove(KEY_FRICTION_OVERRIDE)
            } else {
                putString(KEY_FRICTION_OVERRIDE, level.name)
            }
            apply()
        }
    }

    /**
     * Check if user has enabled "Locked Mode" (maximum friction)
     */
    fun isLockedModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCKED_MODE, false)
    }

    /**
     * Enable or disable Locked Mode
     * When enabled, this sets friction to LOCKED level
     */
    fun setLockedMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCKED_MODE, enabled).apply()
        // Also update the friction override when locked mode changes
        if (enabled) {
            setFrictionLevelOverride(FrictionLevel.LOCKED)
        } else {
            // Clear override when disabled, return to automatic
            setFrictionLevelOverride(null)
        }
    }

    /**
     * Get the install date timestamp
     * Returns 0 if not set (first install)
     */
    fun getInstallDate(): Long {
        return prefs.getLong(KEY_INSTALL_DATE, 0L)
    }

    /**
     * Set the install date (should be called once on first launch)
     */
    fun setInstallDate(timestamp: Long) {
        // Only set if not already set
        if (!prefs.contains(KEY_INSTALL_DATE)) {
            prefs.edit().putLong(KEY_INSTALL_DATE, timestamp).apply()
        }
    }

    /**
     * Get the effective friction level considering user override
     * @param calculatedLevel The level calculated from install date
     * @return The friction level to use (user override takes precedence)
     */
    fun getEffectiveFrictionLevel(calculatedLevel: FrictionLevel): FrictionLevel {
        // Locked mode always wins
        if (isLockedModeEnabled()) {
            return FrictionLevel.LOCKED
        }
        // User override takes precedence over calculated level
        return getFrictionLevelOverride() ?: calculatedLevel
    }

    /**
     * Get days since install for friction calculation
     */
    fun getDaysSinceInstall(): Int {
        val installDate = getInstallDate()
        if (installDate == 0L) return 0
        val now = System.currentTimeMillis()
        val daysDiff = (now - installDate) / (1000 * 60 * 60 * 24)
        return daysDiff.toInt()
    }

    // ========== Phase 2: Snooze Functionality ==========

    /**
     * Set snooze until timestamp
     * @param until Timestamp in milliseconds when snooze expires
     */
    fun setSnoozeUntil(until: Long) {
        prefs.edit().putLong(KEY_SNOOZE_UNTIL, until).apply()
    }

    /**
     * Get snooze expiration timestamp
     * @return Timestamp when snooze expires, or 0 if not snoozed
     */
    fun getSnoozeUntil(): Long {
        return prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
    }

    /**
     * Check if interventions are currently snoozed
     */
    fun isSnoozed(): Boolean {
        val snoozeUntil = getSnoozeUntil()
        if (snoozeUntil == 0L) return false

        val now = System.currentTimeMillis()
        if (now >= snoozeUntil) {
            // Snooze expired - clear it
            clearSnooze()
            return false
        }

        return true
    }

    /**
     * Clear snooze
     */
    fun clearSnooze() {
        prefs.edit().putLong(KEY_SNOOZE_UNTIL, 0L).apply()
    }

    /**
     * Get remaining snooze time in minutes
     * @return Minutes remaining, or 0 if not snoozed
     */
    fun getSnoozeRemainingMinutes(): Int {
        if (!isSnoozed()) return 0
        val snoozeUntil = getSnoozeUntil()
        val now = System.currentTimeMillis()
        val remainingMs = snoozeUntil - now
        return (remainingMs / 1000 / 60).toInt()
    }

    /**
     * Enable/disable "I'm working" mode (2 hour snooze)
     */
    fun setWorkingMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WORKING_MODE, enabled).apply()

        if (enabled) {
            // Set 2-hour snooze
            val twoHoursInMs = 2 * 60 * 60 * 1000L
            setSnoozeUntil(System.currentTimeMillis() + twoHoursInMs)
        } else {
            clearSnooze()
        }
    }

    /**
     * Check if working mode is enabled
     */
    fun isWorkingModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_WORKING_MODE, false)
    }

    /**
     * Track frequent dismissals to auto-suggest working mode
     */
    fun incrementDismissalCount() {
        val count = prefs.getInt(KEY_DISMISSAL_COUNT, 0)
        val today = getCurrentDate()
        val lastDate = prefs.getString(KEY_LAST_DISMISSAL_DATE, "")

        if (today == lastDate) {
            // Same day - increment
            prefs.edit().putInt(KEY_DISMISSAL_COUNT, count + 1).apply()
        } else {
            // New day - reset
            prefs.edit()
                .putInt(KEY_DISMISSAL_COUNT, 1)
                .putString(KEY_LAST_DISMISSAL_DATE, today)
                .apply()
        }
    }

    /**
     * Get dismissal count for today
     */
    fun getDismissalCountToday(): Int {
        val today = getCurrentDate()
        val lastDate = prefs.getString(KEY_LAST_DISMISSAL_DATE, "")
        return if (today == lastDate) {
            prefs.getInt(KEY_DISMISSAL_COUNT, 0)
        } else {
            0
        }
    }

    /**
     * Check if we should suggest working mode (3+ dismissals today)
     */
    fun shouldSuggestWorkingMode(): Boolean {
        return getDismissalCountToday() >= 3 && !isWorkingModeEnabled()
    }

    /**
     * Get current date string for dismissal tracking
     */
    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
    }

    companion object {
        private const val PREFS_NAME = "intervention_preferences"
        private const val KEY_FRICTION_OVERRIDE = "friction_level_override"
        private const val KEY_LOCKED_MODE = "locked_mode_enabled"
        private const val KEY_INSTALL_DATE = "install_date"

        // Phase 2: Snooze functionality keys
        private const val KEY_SNOOZE_UNTIL = "snooze_until"
        private const val KEY_WORKING_MODE = "working_mode_enabled"
        private const val KEY_DISMISSAL_COUNT = "dismissal_count_today"
        private const val KEY_LAST_DISMISSAL_DATE = "last_dismissal_date"

        @Volatile
        private var instance: InterventionPreferences? = null

        /**
         * Get singleton instance of InterventionPreferences
         */
        fun getInstance(context: Context): InterventionPreferences {
            return instance ?: synchronized(this) {
                instance ?: InterventionPreferences(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
