package dev.sadakat.thinkfaster.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * SyncPreferences - Manages sync-related preferences
 * Phase 4: Sync Worker & Background Sync
 * 
 * Stores sync state like last sync time, user ID, sync enabled status
 */
class SyncPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "sync_preferences"

        // Keys
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PROVIDER = "user_provider"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
        private const val KEY_SYNC_STATUS = "sync_status"
        private const val KEY_LAST_SYNC_ERROR = "last_sync_error"
        private const val KEY_PENDING_CHANGES_COUNT = "pending_changes_count"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
    }
    
    // Last successful sync timestamp
    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }
    
    fun setLastSyncTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
    }
    
    // User ID (authenticated user)
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    fun setUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }
    
    fun clearUserId() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }

    // User email
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun setUserEmail(email: String?) {
        if (email != null) {
            prefs.edit().putString(KEY_USER_EMAIL, email).apply()
        } else {
            prefs.edit().remove(KEY_USER_EMAIL).apply()
        }
    }

    // User provider (GOOGLE, FACEBOOK, EMAIL, ANONYMOUS)
    fun getUserProvider(): String? {
        return prefs.getString(KEY_USER_PROVIDER, null)
    }

    fun setUserProvider(provider: String?) {
        if (provider != null) {
            prefs.edit().putString(KEY_USER_PROVIDER, provider).apply()
        } else {
            prefs.edit().remove(KEY_USER_PROVIDER).apply()
        }
    }

    // Sync enabled flag
    fun isSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_SYNC_ENABLED, false)
    }
    
    fun setSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()
    }
    
    // Authentication status
    fun isAuthenticated(): Boolean {
        return prefs.getBoolean(KEY_IS_AUTHENTICATED, false)
    }
    
    fun setAuthenticated(authenticated: Boolean) {
        prefs.edit().putBoolean(KEY_IS_AUTHENTICATED, authenticated).apply()
    }
    
    // Sync status (IDLE, SYNCING, SUCCESS, ERROR)
    fun getSyncStatus(): String {
        return prefs.getString(KEY_SYNC_STATUS, "IDLE") ?: "IDLE"
    }
    
    fun setSyncStatus(status: String) {
        prefs.edit().putString(KEY_SYNC_STATUS, status).apply()
    }
    
    // Last sync error message
    fun getLastSyncError(): String? {
        return prefs.getString(KEY_LAST_SYNC_ERROR, null)
    }
    
    fun setLastSyncError(error: String?) {
        prefs.edit().putString(KEY_LAST_SYNC_ERROR, error).apply()
    }
    
    // Pending changes count (for UI display)
    fun getPendingChangesCount(): Int {
        return prefs.getInt(KEY_PENDING_CHANGES_COUNT, 0)
    }
    
    fun setPendingChangesCount(count: Int) {
        prefs.edit().putInt(KEY_PENDING_CHANGES_COUNT, count).apply()
    }
    
    // Auto-sync enabled (background sync)
    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
    }
    
    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
    }
    
    // Clear all sync data (on logout)
    fun clearSyncData() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PROVIDER)
            .remove(KEY_IS_AUTHENTICATED)
            .remove(KEY_SYNC_STATUS)
            .remove(KEY_LAST_SYNC_ERROR)
            .putBoolean(KEY_SYNC_ENABLED, false)
            .apply()
    }
}
