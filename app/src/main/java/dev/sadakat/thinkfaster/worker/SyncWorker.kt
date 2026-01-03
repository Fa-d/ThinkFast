package dev.sadakat.thinkfaster.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sadakat.thinkfaster.data.preferences.SyncPreferences
import dev.sadakat.thinkfaster.data.sync.SyncCoordinator
import dev.sadakat.thinkfaster.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * SyncWorker - Background worker for periodic data sync
 * Phase 4: Sync Worker & Background Sync
 *
 * Runs every 6 hours to sync data with cloud backend
 * Only runs on WiFi to save battery and data
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {
    
    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_worker"
        
        // Input data keys
        const val KEY_SYNC_TYPE = "sync_type"
        const val SYNC_TYPE_FULL = "full"
        const val SYNC_TYPE_INCREMENTAL = "incremental"
    }
    
    private val syncPreferences = SyncPreferences(context)
    private val networkMonitor = NetworkMonitor(context)
    private val syncCoordinator: SyncCoordinator by inject()
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "SyncWorker started")
        
        try {
            // Check if sync is enabled and user is authenticated
            if (!syncPreferences.isSyncEnabled() || !syncPreferences.isAuthenticated()) {
                Log.d(TAG, "Sync not enabled or user not authenticated. Skipping sync.")
                return@withContext Result.success()
            }
            
            // Check network connectivity
            if (!networkMonitor.isNetworkAvailable()) {
                Log.d(TAG, "No network available. Skipping sync.")
                return@withContext Result.retry()
            }
            
            // Prefer WiFi for sync (respect user's data usage)
            if (!networkMonitor.isWiFiConnected()) {
                Log.d(TAG, "Not on WiFi. Waiting for WiFi connection.")
                return@withContext Result.retry()
            }
            
            val userId = syncPreferences.getUserId()
            if (userId == null) {
                Log.e(TAG, "No user ID found. Cannot sync.")
                syncPreferences.setSyncStatus("ERROR")
                syncPreferences.setLastSyncError("No user ID found")
                return@withContext Result.failure()
            }
            
            // Update sync status
            syncPreferences.setSyncStatus("SYNCING")
            
            // Determine sync type
            val syncType = inputData.getString(KEY_SYNC_TYPE) ?: SYNC_TYPE_INCREMENTAL
            val lastSyncTime = syncPreferences.getLastSyncTime()
            
            Log.d(TAG, "Starting $syncType sync for user: $userId")
            
            // Perform sync (stub for now - will be implemented when SyncCoordinator is wired up)
            val result = performSync(userId, lastSyncTime, syncType)
            
            if (result.isSuccess) {
                // Update last sync time
                syncPreferences.setLastSyncTime(System.currentTimeMillis())
                syncPreferences.setSyncStatus("SUCCESS")
                syncPreferences.setLastSyncError(null)
                syncPreferences.setPendingChangesCount(0)
                
                Log.d(TAG, "Sync completed successfully")
                return@withContext Result.success()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "Sync failed: $error")
                
                syncPreferences.setSyncStatus("ERROR")
                syncPreferences.setLastSyncError(error)
                
                return@withContext Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker failed with exception", e)
            
            syncPreferences.setSyncStatus("ERROR")
            syncPreferences.setLastSyncError(e.message)
            
            return@withContext Result.retry()
        }
    }
    
    /**
     * Perform the actual sync operation
     */
    private suspend fun performSync(
        userId: String,
        lastSyncTime: Long,
        syncType: String
    ): kotlin.Result<Unit> {
        return when (syncType) {
            SYNC_TYPE_FULL -> {
                Log.d(TAG, "Performing full sync")
                syncCoordinator.performFullSync(userId, lastSyncTime)
            }
            SYNC_TYPE_INCREMENTAL -> {
                Log.d(TAG, "Performing incremental sync (upload pending + download remote)")
                // First upload pending changes, then download remote changes
                val uploadResult = syncCoordinator.uploadPendingChanges(userId)
                if (uploadResult.isFailure) {
                    return uploadResult
                }
                syncCoordinator.downloadRemoteChanges(userId, lastSyncTime)
            }
            else -> {
                Log.w(TAG, "Unknown sync type: $syncType, defaulting to incremental")
                syncCoordinator.performFullSync(userId, lastSyncTime)
            }
        }
    }
}
