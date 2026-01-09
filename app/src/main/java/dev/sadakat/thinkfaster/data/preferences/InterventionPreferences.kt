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

    /**
     * Get or generate persistent anonymous user ID
     * This UUID persists across app launches and reinstalls (until app data cleared)
     * Used for Firebase Analytics and Crashlytics to track user behavior over time
     */
    fun getAnonymousUserId(): String {
        val existing = prefs.getString(KEY_ANONYMOUS_USER_ID, null)
        if (existing != null) return existing

        // Generate new UUID on first launch
        val newId = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_ANONYMOUS_USER_ID, newId).apply()
        return newId
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
     * Set snooze for a specific duration in minutes
     * @param durationMinutes Duration in minutes to snooze
     */
    fun setSnoozeDuration(durationMinutes: Int) {
        val durationMs = durationMinutes * 60 * 1000L
        val snoozeUntil = System.currentTimeMillis() + durationMs
        setSnoozeUntil(snoozeUntil)
        // Store the selected duration for overlay display
        setSelectedSnoozeDuration(durationMinutes)
    }

    /**
     * Get the user's selected snooze duration in minutes (for overlay display)
     * @return Duration in minutes, defaults to 10
     */
    fun getSelectedSnoozeDuration(): Int {
        return prefs.getInt(KEY_SELECTED_SNOOZE_DURATION, 10)
    }

    /**
     * Set the selected snooze duration in minutes (for overlay display)
     * @param durationMinutes Duration in minutes
     */
    private fun setSelectedSnoozeDuration(durationMinutes: Int) {
        prefs.edit().putInt(KEY_SELECTED_SNOOZE_DURATION, durationMinutes).apply()
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

    // ========== Phase 3: Rate Limiting Functionality ==========

    /**
     * Get timestamp of last intervention (any type)
     */
    fun getLastInterventionTime(): Long {
        return prefs.getLong(KEY_LAST_INTERVENTION_TIME, 0L)
    }

    /**
     * Set timestamp of last intervention
     */
    fun setLastInterventionTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_INTERVENTION_TIME, time).apply()
    }

    /**
     * Get timestamp of last reminder intervention
     */
    fun getLastReminderTime(): Long {
        return prefs.getLong(KEY_LAST_REMINDER_TIME, 0L)
    }

    /**
     * Set timestamp of last reminder intervention
     */
    fun setLastReminderTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_REMINDER_TIME, time).apply()
    }

    /**
     * Get timestamp of last timer intervention
     */
    fun getLastTimerTime(): Long {
        return prefs.getLong(KEY_LAST_TIMER_TIME, 0L)
    }

    /**
     * Set timestamp of last timer intervention
     */
    fun setLastTimerTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_TIMER_TIME, time).apply()
    }

    /**
     * Get intervention count for today
     * Automatically resets when day changes
     */
    fun getInterventionCountToday(): Int {
        val today = getCurrentDate()
        val lastResetDate = prefs.getString(KEY_LAST_COUNT_RESET_DATE, "")
        if (today != lastResetDate) {
            // Reset count for new day
            prefs.edit()
                .putInt(KEY_INTERVENTION_COUNT_TODAY, 0)
                .putString(KEY_LAST_COUNT_RESET_DATE, today)
                .apply()
            return 0
        }
        return prefs.getInt(KEY_INTERVENTION_COUNT_TODAY, 0)
    }

    /**
     * Get intervention count for the last hour
     * Automatically resets when hour changes
     */
    fun getInterventionCountLastHour(): Int {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val lastResetHour = prefs.getInt(KEY_LAST_COUNT_RESET_HOUR, -1)
        if (currentHour != lastResetHour) {
            // Reset hourly count
            prefs.edit()
                .putInt(KEY_INTERVENTION_COUNT_HOUR, 0)
                .putInt(KEY_LAST_COUNT_RESET_HOUR, currentHour)
                .apply()
            return 0
        }
        return prefs.getInt(KEY_INTERVENTION_COUNT_HOUR, 0)
    }

    /**
     * Increment intervention counters (both hourly and daily)
     */
    fun incrementInterventionCount() {
        val todayCount = getInterventionCountToday()
        val hourCount = getInterventionCountLastHour()
        prefs.edit()
            .putInt(KEY_INTERVENTION_COUNT_TODAY, todayCount + 1)
            .putInt(KEY_INTERVENTION_COUNT_HOUR, hourCount + 1)
            .apply()
    }

    /**
     * Get cooldown multiplier for adaptive cooldowns
     * Range: 1.0 (normal) to 3.0 (maximum)
     */
    fun getCooldownMultiplier(): Float {
        return prefs.getFloat(KEY_COOLDOWN_MULTIPLIER, 1.0f)
    }

    /**
     * Set cooldown multiplier
     */
    fun setCooldownMultiplier(multiplier: Float) {
        prefs.edit().putFloat(KEY_COOLDOWN_MULTIPLIER, multiplier).apply()
    }

    // ========== Phase 4: Snooze Abuse Tracking ==========

    /**
     * Snooze friction levels based on abuse patterns
     */
    enum class SnoozeFriction {
        NONE,           // 0 snoozes: No friction
        LIGHT,          // 1-2 snoozes: 3s countdown + confirmation
        MODERATE,       // 3-5 snoozes: 5s countdown + typing challenge
        HEAVY,          // 6-8 snoozes: 10s countdown + reflection question
        BLOCKED         // 9+ snoozes OR hit daily/hourly limit: Snooze disabled
    }

    /**
     * State of snooze availability
     */
    data class SnoozeState(
        val allowed: Boolean,
        val reason: String = "",
        val requiredFriction: SnoozeFriction,
        val snoozesUsedToday: Int,
        val snoozesUsedThisHour: Int,
        val maxDaily: Int,
        val maxHourly: Int
    )

    /**
     * Check if snooze is allowed and what friction level is required
     */
    fun canSnooze(): SnoozeState {
        val today = getCurrentDate()
        val lastResetDate = prefs.getString(KEY_LAST_SNOOZE_RESET_DATE, "")

        // Reset daily counter if new day
        if (today != lastResetDate) {
            prefs.edit()
                .putInt(KEY_SNOOZE_COUNT_TODAY, 0)
                .putString(KEY_LAST_SNOOZE_RESET_DATE, today)
                .putInt(KEY_CONSECUTIVE_SNOOZES, 0)
                .apply()
        }

        // Reset hourly counter if new hour
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val lastResetHour = prefs.getInt(KEY_LAST_SNOOZE_RESET_HOUR, -1)
        if (currentHour != lastResetHour) {
            prefs.edit()
                .putInt(KEY_SNOOZE_COUNT_HOUR, 0)
                .putInt(KEY_LAST_SNOOZE_RESET_HOUR, currentHour)
                .apply()
        }

        val snoozesToday = prefs.getInt(KEY_SNOOZE_COUNT_TODAY, 0)
        val snoozesThisHour = prefs.getInt(KEY_SNOOZE_COUNT_HOUR, 0)
        val consecutiveSnoozes = prefs.getInt(KEY_CONSECUTIVE_SNOOZES, 0)

        val maxHourly = 3
        val maxDaily = 10

        // Check limits
        if (snoozesThisHour >= maxHourly) {
            return SnoozeState(
                allowed = false,
                reason = "Hourly snooze limit reached ($maxHourly/hour). Snooze will be available next hour.",
                requiredFriction = SnoozeFriction.BLOCKED,
                snoozesUsedToday = snoozesToday,
                snoozesUsedThisHour = snoozesThisHour,
                maxDaily = maxDaily,
                maxHourly = maxHourly
            )
        }

        if (snoozesToday >= maxDaily) {
            return SnoozeState(
                allowed = false,
                reason = "Daily snooze limit reached ($maxDaily/day). Snooze will be available tomorrow.",
                requiredFriction = SnoozeFriction.BLOCKED,
                snoozesUsedToday = snoozesToday,
                snoozesUsedThisHour = snoozesThisHour,
                maxDaily = maxDaily,
                maxHourly = maxHourly
            )
        }

        // Determine friction level based on consecutive snoozes
        val friction = when (consecutiveSnoozes) {
            0 -> SnoozeFriction.NONE
            1, 2 -> SnoozeFriction.LIGHT
            3, 4, 5 -> SnoozeFriction.MODERATE
            6, 7, 8 -> SnoozeFriction.HEAVY
            else -> SnoozeFriction.BLOCKED
        }

        return SnoozeState(
            allowed = friction != SnoozeFriction.BLOCKED,
            reason = if (friction == SnoozeFriction.BLOCKED) "Too many consecutive snoozes" else "",
            requiredFriction = friction,
            snoozesUsedToday = snoozesToday,
            snoozesUsedThisHour = snoozesThisHour,
            maxDaily = maxDaily,
            maxHourly = maxHourly
        )
    }

    /**
     * Record a snooze action
     */
    fun recordSnooze() {
        val snoozesToday = getSnoozeCountToday()
        val snoozesHour = getSnoozeCountThisHour()
        val consecutive = prefs.getInt(KEY_CONSECUTIVE_SNOOZES, 0)

        prefs.edit()
            .putInt(KEY_SNOOZE_COUNT_TODAY, snoozesToday + 1)
            .putInt(KEY_SNOOZE_COUNT_HOUR, snoozesHour + 1)
            .putInt(KEY_CONSECUTIVE_SNOOZES, consecutive + 1)
            .apply()
    }

    /**
     * Reset consecutive snooze counter (called when user proceeds or goes back)
     */
    fun resetConsecutiveSnoozes() {
        prefs.edit().putInt(KEY_CONSECUTIVE_SNOOZES, 0).apply()
    }

    /**
     * Get snooze count for today
     */
    fun getSnoozeCountToday(): Int {
        val today = getCurrentDate()
        val lastResetDate = prefs.getString(KEY_LAST_SNOOZE_RESET_DATE, "")
        if (today != lastResetDate) return 0
        return prefs.getInt(KEY_SNOOZE_COUNT_TODAY, 0)
    }

    /**
     * Get snooze count for this hour
     */
    fun getSnoozeCountThisHour(): Int {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val lastResetHour = prefs.getInt(KEY_LAST_SNOOZE_RESET_HOUR, -1)
        if (currentHour != lastResetHour) return 0
        return prefs.getInt(KEY_SNOOZE_COUNT_HOUR, 0)
    }

    // ========== PHASE 2: GENERIC HELPERS ==========

    /**
     * Get string value from preferences
     * Phase 2: Used by BurdenTrendMonitor and other components
     */
    fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * Set string value in preferences
     * Phase 2: Used by BurdenTrendMonitor and other components
     */
    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "intervention_preferences"
        private const val KEY_FRICTION_OVERRIDE = "friction_level_override"
        private const val KEY_LOCKED_MODE = "locked_mode_enabled"
        private const val KEY_INSTALL_DATE = "install_date"
        private const val KEY_ANONYMOUS_USER_ID = "anonymous_user_id"

        // Phase 2: Snooze functionality keys
        private const val KEY_SNOOZE_UNTIL = "snooze_until"
        private const val KEY_SELECTED_SNOOZE_DURATION = "selected_snooze_duration"
        private const val KEY_WORKING_MODE = "working_mode_enabled"
        private const val KEY_DISMISSAL_COUNT = "dismissal_count_today"
        private const val KEY_LAST_DISMISSAL_DATE = "last_dismissal_date"

        // Phase 3: Rate limiting keys
        private const val KEY_LAST_INTERVENTION_TIME = "last_intervention_time"
        private const val KEY_LAST_REMINDER_TIME = "last_reminder_time"
        private const val KEY_LAST_TIMER_TIME = "last_timer_time"
        private const val KEY_INTERVENTION_COUNT_TODAY = "intervention_count_today"
        private const val KEY_INTERVENTION_COUNT_HOUR = "intervention_count_hour"
        private const val KEY_LAST_COUNT_RESET_DATE = "last_count_reset_date"
        private const val KEY_LAST_COUNT_RESET_HOUR = "last_count_reset_hour"
        private const val KEY_COOLDOWN_MULTIPLIER = "cooldown_multiplier"

        // Phase 4: Snooze abuse tracking keys
        private const val KEY_SNOOZE_COUNT_TODAY = "snooze_count_today"
        private const val KEY_SNOOZE_COUNT_HOUR = "snooze_count_hour"
        private const val KEY_LAST_SNOOZE_RESET_DATE = "last_snooze_reset_date"
        private const val KEY_LAST_SNOOZE_RESET_HOUR = "last_snooze_reset_hour"
        private const val KEY_CONSECUTIVE_SNOOZES = "consecutive_snoozes"

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
