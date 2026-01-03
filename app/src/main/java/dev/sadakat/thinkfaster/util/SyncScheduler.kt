package dev.sadakat.thinkfaster.util

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.sadakat.thinkfaster.worker.SyncWorker
import java.util.concurrent.TimeUnit

/**
 * SyncScheduler - Schedules periodic background sync
 * Phase 4: Sync Worker & Background Sync
 * 
 * Manages WorkManager scheduling for data synchronization
 */
object SyncScheduler {
    
    private const val TAG = "SyncScheduler"
    
    // Sync interval: 6 hours
    private const val SYNC_INTERVAL_HOURS = 6L
    
    // Flex interval: 1 hour window for sync
    private const val SYNC_FLEX_INTERVAL_HOURS = 1L
    
    /**
     * Schedule periodic sync worker
     * Runs every 6 hours on WiFi only
     */
    fun schedulePeriodicSync(context: Context) {
        Log.d(TAG, "Scheduling periodic sync worker")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)  // WiFi only
            .setRequiresBatteryNotLow(true)  // Don't run when battery is low
            .setRequiresStorageNotLow(true)  // Don't run when storage is low
            .build()
        
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_HOURS,
            TimeUnit.HOURS,
            SYNC_FLEX_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    SyncWorker.KEY_SYNC_TYPE to SyncWorker.SYNC_TYPE_INCREMENTAL
                )
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15,  // Initial backoff: 15 minutes
                TimeUnit.MINUTES
            )
            .addTag("sync")
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // Keep existing work if already scheduled
            syncWorkRequest
        )
        
        Log.d(TAG, "Periodic sync worker scheduled successfully")
    }
    
    /**
     * Cancel periodic sync worker
     */
    fun cancelPeriodicSync(context: Context) {
        Log.d(TAG, "Cancelling periodic sync worker")
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
    }
    
    /**
     * Trigger immediate one-time sync
     * Used for manual sync trigger
     */
    fun triggerImmediateSync(context: Context, syncType: String = SyncWorker.SYNC_TYPE_INCREMENTAL) {
        Log.d(TAG, "Triggering immediate sync (type: $syncType)")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)  // Any network is fine for manual sync
            .build()
        
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    SyncWorker.KEY_SYNC_TYPE to syncType
                )
            )
            .addTag("sync_immediate")
            .build()
        
        WorkManager.getInstance(context).enqueue(syncWorkRequest)
        
        Log.d(TAG, "Immediate sync triggered")
    }
    
    /**
     * Check if sync worker is scheduled
     */
    fun isSyncScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncWorker.WORK_NAME)
            .get()
        
        return workInfos.isNotEmpty() && !workInfos[0].state.isFinished
    }
}
