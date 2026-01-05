package dev.sadakat.thinkfaster.data.repository

import android.content.Context
import android.content.SharedPreferences
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.data.preferences.NotificationPreferences
import dev.sadakat.thinkfaster.domain.model.AppSettings
import dev.sadakat.thinkfaster.domain.repository.SettingsRepository
import dev.sadakat.thinkfaster.util.WorkManagerHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementation of SettingsRepository using SharedPreferences
 */
class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val interventionPreferences: InterventionPreferences = InterventionPreferences.getInstance(context)
    private val notificationPreferences: NotificationPreferences by lazy { NotificationPreferences(context) }

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

    override suspend fun setOverlayStyle(style: dev.sadakat.thinkfaster.domain.model.OverlayStyle) {
        prefs.edit().putString(KEY_OVERLAY_STYLE, style.name).apply()
        _settingsFlow.value = loadSettings()
    }

    override suspend fun setMotivationalNotificationsEnabled(enabled: Boolean) {
        // Update notification preferences
        notificationPreferences.setMotivationalNotificationsEnabled(enabled)

        // Schedule or cancel workers based on enabled state
        if (enabled) {
            WorkManagerHelper.scheduleMorningNotification(context)
            WorkManagerHelper.scheduleEveningNotification(context)
            WorkManagerHelper.scheduleStreakMonitor(context)
        } else {
            WorkManagerHelper.cancelMotivationalNotifications(context)
        }

        // Refresh settings flow
        _settingsFlow.value = loadSettings()
    }

    override suspend fun setMorningNotificationTime(hour: Int, minute: Int) {
        // Update notification preferences
        notificationPreferences.setMorningTime(hour, minute)

        // Reschedule morning notification worker with new time
        WorkManagerHelper.scheduleMorningNotification(context)

        // Refresh settings flow
        _settingsFlow.value = loadSettings()
    }

    override suspend fun setEveningNotificationTime(hour: Int, minute: Int) {
        // Update notification preferences
        notificationPreferences.setEveningTime(hour, minute)

        // Reschedule evening notification worker with new time
        WorkManagerHelper.scheduleEveningNotification(context)

        // Refresh settings flow
        _settingsFlow.value = loadSettings()
    }

    override suspend fun updateSettings(settings: AppSettings) {
        prefs.edit().apply {
            putInt(KEY_TIMER_ALERT_MINUTES, settings.timerAlertMinutes)
            putBoolean(KEY_ALWAYS_SHOW_REMINDER, settings.alwaysShowReminder)
            putBoolean(KEY_LOCKED_MODE, settings.lockedMode)
            putString(KEY_OVERLAY_STYLE, settings.overlayStyle.name)
            putString(KEY_DEBUG_FORCE_INTERVENTION_TYPE, settings.debugForceInterventionType)
        }.apply()

        // Also sync locked mode to InterventionPreferences
        interventionPreferences.setLockedMode(settings.lockedMode)

        _settingsFlow.value = settings
    }

    /**
     * Debug: Set forced intervention type for UI testing
     */
    override suspend fun setDebugForceInterventionType(typeName: String?) {
        prefs.edit().putString(KEY_DEBUG_FORCE_INTERVENTION_TYPE, typeName).apply()
        _settingsFlow.value = loadSettings()
    }

    /**
     * Debug: Get forced intervention type
     */
    override suspend fun getDebugForceInterventionType(): String? {
        return prefs.getString(KEY_DEBUG_FORCE_INTERVENTION_TYPE, null)
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

        // Load overlay style preference
        val overlayStyleStr = prefs.getString(KEY_OVERLAY_STYLE, DEFAULT_OVERLAY_STYLE) ?: DEFAULT_OVERLAY_STYLE
        val overlayStyle = try {
            dev.sadakat.thinkfaster.domain.model.OverlayStyle.valueOf(overlayStyleStr)
        } catch (e: IllegalArgumentException) {
            dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN
        }

        return AppSettings(
            timerAlertMinutes = prefs.getInt(KEY_TIMER_ALERT_MINUTES, DEFAULT_TIMER_MINUTES),
            alwaysShowReminder = prefs.getBoolean(KEY_ALWAYS_SHOW_REMINDER, DEFAULT_ALWAYS_SHOW_REMINDER),
            lockedMode = actualLockedMode,
            overlayStyle = overlayStyle,
            // Push Notification Strategy: Load notification settings
            motivationalNotificationsEnabled = notificationPreferences.isMotivationalNotificationsEnabled(),
            morningNotificationHour = notificationPreferences.getMorningHour(),
            morningNotificationMinute = notificationPreferences.getMorningMinute(),
            eveningNotificationHour = notificationPreferences.getEveningHour(),
            eveningNotificationMinute = notificationPreferences.getEveningMinute(),
            // Debug: Load forced intervention type
            debugForceInterventionType = prefs.getString(KEY_DEBUG_FORCE_INTERVENTION_TYPE, null)
        )
    }

    companion object {
        private const val PREFS_NAME = "thinkfast_settings"
        private const val KEY_TIMER_ALERT_MINUTES = "timer_alert_minutes"
        private const val KEY_ALWAYS_SHOW_REMINDER = "always_show_reminder"
        private const val KEY_LOCKED_MODE = "locked_mode"
        private const val KEY_OVERLAY_STYLE = "overlay_style"
        private const val KEY_DEBUG_FORCE_INTERVENTION_TYPE = "debug_force_intervention_type"

        // Default values
        private const val DEFAULT_TIMER_MINUTES = 10
        private const val DEFAULT_ALWAYS_SHOW_REMINDER = true
        private const val DEFAULT_LOCKED_MODE = false
        private const val DEFAULT_OVERLAY_STYLE = "FULLSCREEN"
    }
}
