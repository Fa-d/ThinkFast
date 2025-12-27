package dev.sadakat.thinkfast.data.repository

import android.content.Context
import android.content.SharedPreferences
import dev.sadakat.thinkfast.data.preferences.InterventionPreferences
import dev.sadakat.thinkfast.domain.model.AppSettings
import dev.sadakat.thinkfast.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementation of SettingsRepository using SharedPreferences
 */
class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val interventionPreferences: InterventionPreferences = InterventionPreferences.getInstance(context)

    // State flow for reactive settings updates
    private val _settingsFlow = MutableStateFlow(loadSettings())

    override fun getSettings(): Flow<AppSettings> = _settingsFlow.asStateFlow()

    override suspend fun getSettingsOnce(): AppSettings {
        return loadSettings()
    }

    override suspend fun setTimerAlertMinutes(minutes: Int) {
        require(minutes in 1..120) { "Timer alert must be between 1 and 120 minutes" }

        prefs.edit().putInt(KEY_TIMER_ALERT_MINUTES, minutes).apply()
        _settingsFlow.value = loadSettings()
    }

    override suspend fun setAlwaysShowReminder(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALWAYS_SHOW_REMINDER, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    override suspend fun setLockedMode(enabled: Boolean) {
        // Phase F: Update locked mode setting in both stores
        prefs.edit().putBoolean(KEY_LOCKED_MODE, enabled).apply()

        // CRITICAL: Also update InterventionPreferences so overlays reflect the change
        // InterventionPreferences.setLockedMode() automatically sets friction level to LOCKED
        interventionPreferences.setLockedMode(enabled)

        _settingsFlow.value = loadSettings()
    }

    override suspend fun updateSettings(settings: AppSettings) {
        prefs.edit().apply {
            putInt(KEY_TIMER_ALERT_MINUTES, settings.timerAlertMinutes)
            putBoolean(KEY_ALWAYS_SHOW_REMINDER, settings.alwaysShowReminder)
            putBoolean(KEY_LOCKED_MODE, settings.lockedMode)
        }.apply()

        // Also sync locked mode to InterventionPreferences
        interventionPreferences.setLockedMode(settings.lockedMode)

        _settingsFlow.value = settings
    }

    /**
     * Load settings from SharedPreferences
     * Also syncs with InterventionPreferences to ensure consistency
     */
    private fun loadSettings(): AppSettings {
        // Check if there's a mismatch between the two stores and sync them
        val appSettingsLockedMode = prefs.getBoolean(KEY_LOCKED_MODE, DEFAULT_LOCKED_MODE)
        val interventionLockedMode = interventionPreferences.isLockedModeEnabled()

        // If they differ, trust InterventionPreferences (the source of truth for interventions)
        val actualLockedMode = if (appSettingsLockedMode != interventionLockedMode) {
            // Sync AppSettings to match InterventionPreferences
            prefs.edit().putBoolean(KEY_LOCKED_MODE, interventionLockedMode).apply()
            interventionLockedMode
        } else {
            appSettingsLockedMode
        }

        return AppSettings(
            timerAlertMinutes = prefs.getInt(KEY_TIMER_ALERT_MINUTES, DEFAULT_TIMER_MINUTES),
            alwaysShowReminder = prefs.getBoolean(KEY_ALWAYS_SHOW_REMINDER, DEFAULT_ALWAYS_SHOW_REMINDER),
            lockedMode = actualLockedMode
        )
    }

    companion object {
        private const val PREFS_NAME = "thinkfast_settings"
        private const val KEY_TIMER_ALERT_MINUTES = "timer_alert_minutes"
        private const val KEY_ALWAYS_SHOW_REMINDER = "always_show_reminder"
        private const val KEY_LOCKED_MODE = "locked_mode"

        // Default values
        private const val DEFAULT_TIMER_MINUTES = 10
        private const val DEFAULT_ALWAYS_SHOW_REMINDER = true
        private const val DEFAULT_LOCKED_MODE = false
    }
}
