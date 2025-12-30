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

    companion object {
        private const val PREFS_NAME = "intervention_preferences"
        private const val KEY_FRICTION_OVERRIDE = "friction_level_override"
        private const val KEY_LOCKED_MODE = "locked_mode_enabled"
        private const val KEY_INSTALL_DATE = "install_date"

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
