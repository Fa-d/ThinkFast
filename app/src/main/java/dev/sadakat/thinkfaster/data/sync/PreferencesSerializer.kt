package dev.sadakat.thinkfaster.data.sync

import android.content.Context
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.data.preferences.NotificationPreferences
import dev.sadakat.thinkfaster.data.preferences.OnboardingQuestPreferences
import dev.sadakat.thinkfaster.data.preferences.StreakFreezePreferences
import dev.sadakat.thinkfaster.data.repository.SettingsRepositoryImpl
import dev.sadakat.thinkfaster.domain.model.OverlayStyle
import dev.sadakat.thinkfaster.ui.theme.ThemeMode
import dev.sadakat.thinkfaster.util.ThemePreferences

/**
 * PreferencesSerializer - Serializes all app preferences to/from JSON for cloud sync
 * Phase 6: Settings Sync & Preferences
 *
 * Syncs all preference types:
 * - App settings (timer, locked mode, overlay style, notifications)
 * - Theme settings (theme mode, dynamic colors, amoled dark)
 * - Intervention preferences (locked mode, snooze duration)
 * - Notification preferences (morning/evening times, enabled)
 * - Onboarding quest progress
 * - Streak freeze inventory
 */
class PreferencesSerializer(
    private val context: Context,
    private val interventionPrefs: InterventionPreferences,
    private val notificationPrefs: NotificationPreferences,
    private val questPrefs: OnboardingQuestPreferences,
    private val streakFreezePrefs: StreakFreezePreferences,
    private val settingsRepository: SettingsRepositoryImpl
) {
    /**
     * Serialize all preferences to a JSON map
     */
    suspend fun serializeAll(): Map<String, Any> {
        val appSettings = settingsRepository.getSettingsOnce()

        return mapOf(
            "appSettings" to serializeAppSettings(),
            "theme" to serializeThemeSettings(),
            "intervention" to serializeInterventionPrefs(),
            "notification" to serializeNotificationPrefs(),
            "quest" to serializeQuestPrefs(),
            "streakFreeze" to serializeStreakFreezePrefs(),
            "lastModified" to System.currentTimeMillis()
        )
    }

    /**
     * Deserialize JSON map to all preferences
     * Remote wins in case of conflicts
     */
    suspend fun deserializeAll(data: Map<String, Any>) {
        try {
            // App settings
            (data["appSettings"] as? Map<*, *>)?.let { map ->
                deserializeAppSettings(map as Map<String, Any>)
            }

            // Theme settings
            (data["theme"] as? Map<*, *>)?.let { map ->
                deserializeThemeSettings(map as Map<String, Any>)
            }

            // Intervention preferences
            (data["intervention"] as? Map<*, *>)?.let { map ->
                deserializeInterventionPrefs(map as Map<String, Any>)
            }

            // Notification preferences
            (data["notification"] as? Map<*, *>)?.let { map ->
                deserializeNotificationPrefs(map as Map<String, Any>)
            }

            // Quest preferences
            (data["quest"] as? Map<*, *>)?.let { map ->
                deserializeQuestPrefs(map as Map<String, Any>)
            }

            // Streak freeze preferences
            (data["streakFreeze"] as? Map<*, *>)?.let { map ->
                deserializeStreakFreezePrefs(map as Map<String, Any>)
            }
        } catch (e: Exception) {
            // Log error but don't crash - preferences remain unchanged
            println("Error deserializing preferences: ${e.message}")
        }
    }

    // ========== App Settings ==========

    private suspend fun serializeAppSettings(): Map<String, Any> {
        val settings = settingsRepository.getSettingsOnce()
        return mapOf(
            "timerAlertMinutes" to settings.timerAlertMinutes,
            "alwaysShowReminder" to settings.alwaysShowReminder,
            "lockedMode" to settings.lockedMode,
            "overlayStyle" to settings.overlayStyle.name,
            "motivationalNotificationsEnabled" to settings.motivationalNotificationsEnabled,
            "morningNotificationHour" to settings.morningNotificationHour,
            "morningNotificationMinute" to settings.morningNotificationMinute,
            "eveningNotificationHour" to settings.eveningNotificationHour,
            "eveningNotificationMinute" to settings.eveningNotificationMinute
        )
    }

    private suspend fun deserializeAppSettings(data: Map<String, Any>) {
        (data["timerAlertMinutes"] as? Double)?.toInt()?.let {
            settingsRepository.setTimerAlertMinutes(it)
        }
        (data["alwaysShowReminder"] as? Boolean)?.let {
            settingsRepository.setAlwaysShowReminder(it)
        }
        (data["lockedMode"] as? Boolean)?.let {
            settingsRepository.setLockedMode(it)
        }
        (data["overlayStyle"] as? String)?.let {
            try {
                settingsRepository.setOverlayStyle(OverlayStyle.valueOf(it))
            } catch (e: IllegalArgumentException) {
                // Ignore invalid overlay style
            }
        }
        (data["motivationalNotificationsEnabled"] as? Boolean)?.let {
            settingsRepository.setMotivationalNotificationsEnabled(it)
        }

        // Update morning time if both hour and minute are present
        val morningHour = (data["morningNotificationHour"] as? Double)?.toInt()
        val morningMinute = (data["morningNotificationMinute"] as? Double)?.toInt()
        if (morningHour != null && morningMinute != null) {
            settingsRepository.setMorningNotificationTime(morningHour, morningMinute)
        }

        // Update evening time if both hour and minute are present
        val eveningHour = (data["eveningNotificationHour"] as? Double)?.toInt()
        val eveningMinute = (data["eveningNotificationMinute"] as? Double)?.toInt()
        if (eveningHour != null && eveningMinute != null) {
            settingsRepository.setEveningNotificationTime(eveningHour, eveningMinute)
        }
    }

    // ========== Theme Settings ==========

    private fun serializeThemeSettings(): Map<String, Any> {
        return mapOf(
            "themeMode" to ThemePreferences.getThemeMode(context).name,
            "dynamicColors" to ThemePreferences.getDynamicColor(context),
            "amoledDark" to ThemePreferences.getAmoledDark(context)
        )
    }

    private fun deserializeThemeSettings(data: Map<String, Any>) {
        (data["themeMode"] as? String)?.let {
            try {
                ThemePreferences.saveThemeMode(context, ThemeMode.valueOf(it))
            } catch (e: IllegalArgumentException) {
                // Ignore invalid theme mode
            }
        }
        (data["dynamicColors"] as? Boolean)?.let {
            ThemePreferences.saveDynamicColor(context, it)
        }
        (data["amoledDark"] as? Boolean)?.let {
            ThemePreferences.saveAmoledDark(context, it)
        }
    }

    // ========== Intervention Preferences ==========

    private fun serializeInterventionPrefs(): Map<String, Any> {
        return mapOf(
            "lockedModeEnabled" to interventionPrefs.isLockedModeEnabled(),
            "snoozeDuration" to interventionPrefs.getSelectedSnoozeDuration(),
            "snoozeUntil" to interventionPrefs.getSnoozeUntil(),
            "workingModeEnabled" to interventionPrefs.isWorkingModeEnabled()
        )
    }

    private fun deserializeInterventionPrefs(data: Map<String, Any>) {
        (data["lockedModeEnabled"] as? Boolean)?.let {
            interventionPrefs.setLockedMode(it)
        }
        (data["snoozeDuration"] as? Double)?.toInt()?.let {
            interventionPrefs.setSnoozeDuration(it)
        }
        (data["snoozeUntil"] as? Double)?.toLong()?.let {
            interventionPrefs.setSnoozeUntil(it)
        }
        (data["workingModeEnabled"] as? Boolean)?.let {
            interventionPrefs.setWorkingMode(it)
        }
    }

    // ========== Notification Preferences ==========

    private fun serializeNotificationPrefs(): Map<String, Any> {
        return mapOf(
            "motivationalEnabled" to notificationPrefs.isMotivationalNotificationsEnabled(),
            "morningHour" to notificationPrefs.getMorningHour(),
            "morningMinute" to notificationPrefs.getMorningMinute(),
            "eveningHour" to notificationPrefs.getEveningHour(),
            "eveningMinute" to notificationPrefs.getEveningMinute()
        )
    }

    private fun deserializeNotificationPrefs(data: Map<String, Any>) {
        (data["motivationalEnabled"] as? Boolean)?.let {
            notificationPrefs.setMotivationalNotificationsEnabled(it)
        }

        // Update morning time if both hour and minute are present
        val morningHour = (data["morningHour"] as? Double)?.toInt()
        val morningMinute = (data["morningMinute"] as? Double)?.toInt()
        if (morningHour != null && morningMinute != null) {
            notificationPrefs.setMorningTime(morningHour, morningMinute)
        }

        // Update evening time if both hour and minute are present
        val eveningHour = (data["eveningHour"] as? Double)?.toInt()
        val eveningMinute = (data["eveningMinute"] as? Double)?.toInt()
        if (eveningHour != null && eveningMinute != null) {
            notificationPrefs.setEveningTime(eveningHour, eveningMinute)
        }
    }

    // ========== Quest Preferences ==========

    private fun serializeQuestPrefs(): Map<String, Any> {
        val questStartDate = questPrefs.getCurrentQuestDay()
        return mapOf(
            "questActive" to questPrefs.isQuestActive(),
            "questCompleted" to questPrefs.isQuestCompleted(),
            "currentQuestDay" to questStartDate,
            "firstSessionCelebrated" to questPrefs.isFirstSessionCelebrated(),
            "firstUnderGoalCelebrated" to questPrefs.isFirstUnderGoalCelebrated()
        )
    }

    private fun deserializeQuestPrefs(data: Map<String, Any>) {
        // Note: Quest preferences are mostly read-only after creation
        // We sync completion states but don't override quest start date
        // to avoid breaking user's quest progress

        (data["questCompleted"] as? Boolean)?.takeIf { it }?.let {
            // Only mark as complete if remote says complete
            questPrefs.markQuestComplete(getCurrentDate())
        }

        (data["firstSessionCelebrated"] as? Boolean)?.takeIf { it }?.let {
            questPrefs.markFirstSessionCelebrated()
        }

        (data["firstUnderGoalCelebrated"] as? Boolean)?.takeIf { it }?.let {
            questPrefs.markFirstUnderGoalCelebrated()
        }
    }

    // ========== Streak Freeze Preferences ==========

    private fun serializeStreakFreezePrefs(): Map<String, Any> {
        return mapOf(
            "freezesAvailable" to streakFreezePrefs.getFreezesAvailable(),
            "lastResetMonth" to streakFreezePrefs.getLastResetMonth(),
            "maxMonthlyFreezes" to streakFreezePrefs.getMaxMonthlyFreezes()
        )
    }

    private fun deserializeStreakFreezePrefs(data: Map<String, Any>) {
        (data["freezesAvailable"] as? Double)?.toInt()?.let {
            streakFreezePrefs.setFreezesAvailable(it)
        }
        (data["lastResetMonth"] as? String)?.let {
            streakFreezePrefs.setLastResetMonth(it)
        }
        (data["maxMonthlyFreezes"] as? Double)?.toInt()?.let {
            streakFreezePrefs.setMaxMonthlyFreezes(it)
        }
    }

    /**
     * Get last modified timestamp from serialized data
     */
    fun getLastModified(data: Map<String, Any>): Long {
        return (data["lastModified"] as? Double)?.toLong() ?: 0L
    }

    /**
     * Get current date string for quest tracking
     */
    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
    }
}
