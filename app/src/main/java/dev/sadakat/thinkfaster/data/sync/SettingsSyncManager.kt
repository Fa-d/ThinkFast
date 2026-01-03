package dev.sadakat.thinkfaster.data.sync

import dev.sadakat.thinkfaster.data.preferences.SyncPreferences
import dev.sadakat.thinkfaster.data.sync.backend.SyncBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SettingsSyncManager - Manages synchronization of app preferences with cloud
 * Phase 6: Settings Sync & Preferences
 *
 * Features:
 * - Debounced sync (2-second delay to batch rapid changes)
 * - Conflict resolution (remote wins strategy)
 * - Background sync coordination
 * - Error handling and retry
 */
class SettingsSyncManager(
    private val syncBackend: SyncBackend,
    private val preferencesSerializer: PreferencesSerializer,
    private val syncPreferences: SyncPreferences
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var debouncedSyncJob: Job? = null

    companion object {
        private const val DEBOUNCE_DELAY_MS = 2000L // 2 seconds
    }

    /**
     * Trigger settings sync with 2-second debouncing
     * Multiple rapid calls will be batched into a single sync
     */
    fun triggerDebouncedSync() {
        // Cancel any pending sync
        debouncedSyncJob?.cancel()

        // Schedule new sync after debounce delay
        debouncedSyncJob = scope.launch {
            delay(DEBOUNCE_DELAY_MS)
            performSync()
        }
    }

    /**
     * Trigger immediate settings sync (no debouncing)
     * Use for manual sync or initial sync
     */
    suspend fun performSync(): Result<Unit> {
        return try {
            // Check if user is authenticated
            if (!syncPreferences.isAuthenticated()) {
                return Result.failure(Exception("User not authenticated"))
            }

            val userId = syncPreferences.getUserId() ?: return Result.failure(Exception("User ID not found"))

            // Serialize local preferences
            val localSettings = preferencesSerializer.serializeAll()
            val localTimestamp = preferencesSerializer.getLastModified(localSettings)

            // Sync with backend
            val result = syncBackend.syncSettings(localSettings, userId, syncPreferences.getLastSyncTime())

            when (result) {
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.Success -> {
                    // Check if remote has newer settings
                    val remoteSettings = result.data
                    val remoteTimestamp = preferencesSerializer.getLastModified(remoteSettings)

                    if (remoteTimestamp > localTimestamp) {
                        // Remote wins - apply remote settings
                        preferencesSerializer.deserializeAll(remoteSettings)
                    }

                    // Update last sync time
                    syncPreferences.setLastSyncTime(System.currentTimeMillis())
                    syncPreferences.setSyncStatus("SYNCED")
                    syncPreferences.setLastSyncError(null)

                    Result.success(Unit)
                }
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.Error -> {
                    syncPreferences.setSyncStatus("ERROR")
                    syncPreferences.setLastSyncError(result.message)
                    Result.failure(Exception(result.message))
                }
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.Conflict -> {
                    // Remote wins strategy - apply remote changes
                    preferencesSerializer.deserializeAll(result.remoteData)
                    syncPreferences.setLastSyncTime(System.currentTimeMillis())
                    syncPreferences.setSyncStatus("SYNCED")
                    Result.success(Unit)
                }
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.InProgress -> {
                    // Sync in progress - should not happen here
                    Result.failure(Exception("Sync in progress"))
                }
            }
        } catch (e: Exception) {
            syncPreferences.setSyncStatus("ERROR")
            syncPreferences.setLastSyncError(e.message)
            Result.failure(e)
        }
    }

    /**
     * Download settings from cloud and apply them locally
     * Used during initial sync or when restoring settings
     */
    suspend fun downloadSettings(): Result<Unit> {
        return try {
            if (!syncPreferences.isAuthenticated()) {
                return Result.failure(Exception("User not authenticated"))
            }

            val userId = syncPreferences.getUserId() ?: return Result.failure(Exception("User ID not found"))

            // Get settings from backend
            val result = syncBackend.syncSettings(emptyMap(), userId, 0L)

            when (result) {
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.Success -> {
                    // Apply remote settings
                    preferencesSerializer.deserializeAll(result.data)
                    syncPreferences.setLastSyncTime(System.currentTimeMillis())
                    syncPreferences.setSyncStatus("SYNCED")
                    Result.success(Unit)
                }
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.Error -> {
                    syncPreferences.setSyncStatus("ERROR")
                    syncPreferences.setLastSyncError(result.message)
                    Result.failure(Exception(result.message))
                }
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.Conflict -> {
                    // Apply remote changes (remote wins)
                    preferencesSerializer.deserializeAll(result.remoteData)
                    syncPreferences.setLastSyncTime(System.currentTimeMillis())
                    syncPreferences.setSyncStatus("SYNCED")
                    Result.success(Unit)
                }
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.InProgress -> {
                    Result.failure(Exception("Sync in progress"))
                }
            }
        } catch (e: Exception) {
            syncPreferences.setSyncStatus("ERROR")
            syncPreferences.setLastSyncError(e.message)
            Result.failure(e)
        }
    }

    /**
     * Upload current settings to cloud
     * Used during initial sync after login
     */
    suspend fun uploadSettings(): Result<Unit> {
        return try {
            if (!syncPreferences.isAuthenticated()) {
                return Result.failure(Exception("User not authenticated"))
            }

            // Serialize and upload
            val localSettings = preferencesSerializer.serializeAll()
            val userId = syncPreferences.getUserId() ?: return Result.failure(Exception("User ID not found"))
            val result = syncBackend.syncSettings(localSettings, userId, 0L) // Force upload with 0 lastSyncTime

            when (result) {
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.Success -> {
                    syncPreferences.setLastSyncTime(System.currentTimeMillis())
                    syncPreferences.setSyncStatus("SYNCED")
                    Result.success(Unit)
                }
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.Error -> {
                    syncPreferences.setSyncStatus("ERROR")
                    syncPreferences.setLastSyncError(result.message)
                    Result.failure(Exception(result.message))
                }
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.Conflict -> {
                    // Even on upload, remote wins
                    preferencesSerializer.deserializeAll(result.remoteData)
                    syncPreferences.setLastSyncTime(System.currentTimeMillis())
                    syncPreferences.setSyncStatus("SYNCED")
                    Result.success(Unit)
                }
                is dev.sadakat.thinkfaster.data.sync.model.SyncResult.InProgress -> {
                    Result.failure(Exception("Sync in progress"))
                }
            }
        } catch (e: Exception) {
            syncPreferences.setSyncStatus("ERROR")
            syncPreferences.setLastSyncError(e.message)
            Result.failure(e)
        }
    }

    /**
     * Cancel any pending debounced sync
     */
    fun cancelPendingSync() {
        debouncedSyncJob?.cancel()
        debouncedSyncJob = null
    }
}
