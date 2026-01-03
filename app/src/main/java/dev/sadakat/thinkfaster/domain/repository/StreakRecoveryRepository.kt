package dev.sadakat.thinkfaster.domain.repository

import dev.sadakat.thinkfaster.domain.model.StreakRecovery
import kotlinx.coroutines.flow.Flow

interface StreakRecoveryRepository {

    /**
     * Start a new recovery process for an app
     */
    suspend fun startRecovery(targetApp: String, previousStreak: Int, startDate: String)

    /**
     * Get recovery status for a specific app
     */
    suspend fun getRecoveryByApp(targetApp: String): StreakRecovery?

    /**
     * Observe recovery status for a specific app
     */
    fun observeRecoveryByApp(targetApp: String): Flow<StreakRecovery?>

    /**
     * Update recovery progress (increment days recovered)
     */
    suspend fun updateRecoveryProgress(targetApp: String, daysRecovered: Int)

    /**
     * Mark recovery as complete
     */
    suspend fun completeRecovery(targetApp: String, completedDate: String)

    /**
     * Delete recovery for a specific app
     */
    suspend fun deleteRecovery(targetApp: String)

    /**
     * Get all active (not completed) recoveries
     */
    suspend fun getActiveRecoveries(): List<StreakRecovery>

    /**
     * Mark notification as shown for a recovery
     */
    suspend fun markNotificationShown(targetApp: String)

    /**
     * Clean up old completed recoveries
     */
    suspend fun cleanupOldRecoveries(daysOld: Int)

    // ========== Sync Methods ==========

    /**
     * Get all recoveries for a specific user
     */
    suspend fun getRecoveriesByUserId(userId: String): List<StreakRecovery>

    /**
     * Get unsynced recoveries for a user (pending sync status)
     */
    suspend fun getUnsyncedRecoveries(userId: String): List<StreakRecovery>

    /**
     * Mark a recovery as synced
     */
    suspend fun markRecoveryAsSynced(targetApp: String, cloudId: String)

    /**
     * Upsert a recovery from remote with sync metadata
     */
    suspend fun upsertRecoveryFromRemote(recovery: StreakRecovery, cloudId: String)

    /**
     * Update user ID for a recovery
     */
    suspend fun updateRecoveryUserId(targetApp: String, userId: String)
}
