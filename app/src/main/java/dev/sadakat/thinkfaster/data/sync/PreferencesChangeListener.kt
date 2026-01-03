package dev.sadakat.thinkfaster.data.sync

import android.content.SharedPreferences
import android.util.Log

/**
 * PreferencesChangeListener - Triggers settings sync when preferences change
 * Phase 6: Settings Sync & Preferences
 *
 * Listens to all SharedPreferences changes and triggers debounced sync
 * to automatically sync settings across devices
 */
class PreferencesChangeListener(
    private val settingsSyncManager: SettingsSyncManager,
    private val syncPreferences: dev.sadakat.thinkfaster.data.preferences.SyncPreferences
) : SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val TAG = "PrefsChangeListener"

        // Preferences that should NOT trigger sync (to avoid recursion)
        private val EXCLUDED_KEYS = setOf(
            "sync_last_sync_time",
            "sync_sync_status",
            "sync_last_sync_error",
            "sync_pending_changes_count",
            "sync_user_id",
            "sync_authenticated",
            "sync_enabled",
            "anonymous_user_id" // Don't sync anonymous ID
        )
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // Skip if key is null or excluded
        if (key == null || EXCLUDED_KEYS.contains(key)) {
            return
        }

        // Only sync if user is authenticated and auto-sync is enabled
        if (!syncPreferences.isAuthenticated() || !syncPreferences.isAutoSyncEnabled()) {
            Log.d(TAG, "Skipping sync - user not authenticated or auto-sync disabled")
            return
        }

        Log.d(TAG, "Preference changed: $key - triggering debounced sync")

        // Trigger debounced sync (will batch rapid changes)
        settingsSyncManager.triggerDebouncedSync()
    }
}
