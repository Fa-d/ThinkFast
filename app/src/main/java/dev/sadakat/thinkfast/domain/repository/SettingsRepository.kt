package dev.sadakat.thinkfast.domain.repository

import dev.sadakat.thinkfast.domain.model.AppSettings
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
     * Update all settings at once
     */
    suspend fun updateSettings(settings: AppSettings)
}
