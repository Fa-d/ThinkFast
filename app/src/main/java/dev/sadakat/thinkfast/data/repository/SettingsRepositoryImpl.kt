package dev.sadakat.thinkfast.data.repository

import android.content.Context
import android.content.SharedPreferences
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

    override suspend fun updateSettings(settings: AppSettings) {
        prefs.edit().apply {
            putInt(KEY_TIMER_ALERT_MINUTES, settings.timerAlertMinutes)
            putBoolean(KEY_ALWAYS_SHOW_REMINDER, settings.alwaysShowReminder)
        }.apply()
        _settingsFlow.value = settings
    }

    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings(): AppSettings {
        return AppSettings(
            timerAlertMinutes = prefs.getInt(KEY_TIMER_ALERT_MINUTES, DEFAULT_TIMER_MINUTES),
            alwaysShowReminder = prefs.getBoolean(KEY_ALWAYS_SHOW_REMINDER, DEFAULT_ALWAYS_SHOW_REMINDER)
        )
    }

    companion object {
        private const val PREFS_NAME = "thinkfast_settings"
        private const val KEY_TIMER_ALERT_MINUTES = "timer_alert_minutes"
        private const val KEY_ALWAYS_SHOW_REMINDER = "always_show_reminder"

        // Default values
        private const val DEFAULT_TIMER_MINUTES = 10
        private const val DEFAULT_ALWAYS_SHOW_REMINDER = true
    }
}
