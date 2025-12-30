package dev.sadakat.thinkfaster.domain.repository

import dev.sadakat.thinkfaster.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository for app settings/preferences
 */
interface SettingsRepository {
    /**
     * Get current app settings as Flow (for reactive UI updates)
     */
    fun getSettings(): Flow<AppSettings>

    /**
     * Get current app settings (one-time read)
     */
    suspend fun getSettingsOnce(): AppSettings

    /**
     * Update timer alert duration in minutes
     */
    suspend fun setTimerAlertMinutes(minutes: Int)

    /**
     * Update whether to always show reminder
     */
    suspend fun setAlwaysShowReminder(enabled: Boolean)

    /**
     * Phase F: Update locked mode (maximum friction)
     * When enabled, interventions use LOCKED friction level (10s delay)
     */
    suspend fun setLockedMode(enabled: Boolean)

    /**
     * Push Notification Strategy: Enable/disable daily reminder notifications
     * When enabled, schedules morning, evening, and streak monitoring workers
     * When disabled, cancels all notification workers
     */
    suspend fun setMotivationalNotificationsEnabled(enabled: Boolean)

    /**
     * Push Notification Strategy: Update morning notification time
     * Reschedules the morning notification worker with new time
     */
    suspend fun setMorningNotificationTime(hour: Int, minute: Int)

    /**
     * Push Notification Strategy: Update evening notification time
     * Reschedules the evening notification worker with new time
     */
    suspend fun setEveningNotificationTime(hour: Int, minute: Int)

    /**
     * Update all settings at once
     */
    suspend fun updateSettings(settings: AppSettings)
}
