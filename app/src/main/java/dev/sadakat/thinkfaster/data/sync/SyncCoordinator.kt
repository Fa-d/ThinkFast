package dev.sadakat.thinkfaster.data.sync

import android.util.Log
import dev.sadakat.thinkfaster.data.local.database.dao.*
import dev.sadakat.thinkfaster.data.sync.backend.SyncBackend
import dev.sadakat.thinkfaster.data.sync.model.SyncResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Coordinates sync operations across all entity types
 * Phase 3: Firebase Backend Implementation
 * 
 * Orchestrates syncing of all 7 entity types + settings
 * Handles initial sync, periodic sync, and conflict resolution
 */
class SyncCoordinator(
    private val syncBackend: SyncBackend,
    private val goalDao: GoalDao,
    private val usageSessionDao: UsageSessionDao,
    private val usageEventDao: UsageEventDao,
    private val dailyStatsDao: DailyStatsDao,
    private val interventionResultDao: InterventionResultDao,
    private val streakRecoveryDao: StreakRecoveryDao,
    private val userBaselineDao: UserBaselineDao
) {
    
    companion object {
        private const val TAG = "SyncCoordinator"
    }
    
    /**
     * Perform full sync of all data types
     * Called on initial login and manual sync trigger
     */
    suspend fun performFullSync(userId: String, lastSyncTime: Long = 0): Result<Unit> {
        return try {
            Log.d(TAG, "Starting full sync for user: $userId")
            
            // Run all syncs in parallel for better performance
            coroutineScope {
                val goalsSync = async { syncGoals(userId, lastSyncTime) }
                val sessionsSync = async { syncUsageSessions(userId, lastSyncTime) }
                val eventsSync = async { syncUsageEvents(userId, lastSyncTime) }
                val statsSync = async { syncDailyStats(userId, lastSyncTime) }
                val interventionsSync = async { syncInterventionResults(userId, lastSyncTime) }
                val recoveriesSync = async { syncStreakRecoveries(userId, lastSyncTime) }
                val baselineSync = async { syncUserBaseline(userId, lastSyncTime) }
                val settingsSync = async { syncSettings(userId, lastSyncTime) }
                
                // Wait for all to complete
                val results = listOf(
                    goalsSync.await(),
                    sessionsSync.await(),
                    eventsSync.await(),
                    statsSync.await(),
                    interventionsSync.await(),
                    recoveriesSync.await(),
                    baselineSync.await(),
                    settingsSync.await()
                )
                
                // Check if any failed
                val failures = results.filter { it.isFailure }
                if (failures.isNotEmpty()) {
                    Log.e(TAG, "Some syncs failed: ${failures.size} failures")
                    failures.forEachIndexed { index, result ->
                        result.exceptionOrNull()?.let { e ->
                            Log.e(TAG, "Sync failure #$index: ${e.message}", e)
                        }
                    }
                    return@coroutineScope Result.failure<Unit>(Exception("Partial sync failure"))
                }
            }

            Log.d(TAG, "Full sync completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Perform initial sync after first login
     * Uploads all local data to cloud
     */
    suspend fun performInitialSync(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Starting initial sync for user: $userId")
            
            // Update all local entities with userId
            updateAllEntitiesWithUserId(userId)
            
            // Mark all as pending for upload
            markAllAsPending()
            
            // Perform full sync to upload everything
            performFullSync(userId, lastSyncTime = 0)
        } catch (e: Exception) {
            Log.e(TAG, "Initial sync failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Upload pending changes only
     * Called by background sync worker
     */
    suspend fun uploadPendingChanges(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Uploading pending changes for user: $userId")
            
            // Get pending entities
            val pendingGoals = goalDao.getGoalsBySyncStatus("PENDING")
            val pendingSessions = usageSessionDao.getSessionsBySyncStatus("PENDING")
            val pendingEvents = usageEventDao.getEventsBySyncStatus("PENDING")
            val pendingStats = dailyStatsDao.getStatsBySyncStatus("PENDING")
            val pendingInterventions = interventionResultDao.getResultsBySyncStatus("PENDING")
            val pendingRecoveries = streakRecoveryDao.getRecoveriesBySyncStatus("PENDING")
            
            // Sync pending data
            coroutineScope {
                if (pendingGoals.isNotEmpty()) syncGoals(userId, 0)
                if (pendingSessions.isNotEmpty()) syncUsageSessions(userId, 0)
                if (pendingEvents.isNotEmpty()) syncUsageEvents(userId, 0)
                if (pendingStats.isNotEmpty()) syncDailyStats(userId, 0)
                if (pendingInterventions.isNotEmpty()) syncInterventionResults(userId, 0)
                if (pendingRecoveries.isNotEmpty()) syncStreakRecoveries(userId, 0)
            }
            
            Log.d(TAG, "Pending changes uploaded")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Upload pending changes failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download remote changes since last sync
     * Called by background sync worker
     */
    suspend fun downloadRemoteChanges(userId: String, lastSyncTime: Long): Result<Unit> {
        return performFullSync(userId, lastSyncTime)
    }
    
    // ========== Individual Entity Sync Methods ==========
    
    private suspend fun syncGoals(userId: String, lastSyncTime: Long): Result<Unit> {
        return try {
            Log.d(TAG, "syncGoals: Starting for userId=$userId, lastSyncTime=$lastSyncTime")
            val localGoals = goalDao.getGoalsByUserId(userId)
            Log.d(TAG, "syncGoals: Found ${localGoals.size} local goals")
            val syncResult = syncBackend.syncGoals(localGoals, userId, lastSyncTime)

            when (syncResult) {
                is SyncResult.Success -> {
                    Log.d(TAG, "syncGoals: Sync successful, got ${syncResult.data.size} goals")
                    // Update local database with merged goals
                    syncResult.data.forEach { goal ->
                        goalDao.upsertGoal(goal)
                        if (goal.cloudId != null) {
                            goalDao.updateSyncStatus(
                                goal.targetApp,
                                "SYNCED",
                                goal.cloudId,
                                System.currentTimeMillis()
                            )
                        }
                    }
                    Result.success(Unit)
                }
                is SyncResult.Error -> {
                    Log.e(TAG, "syncGoals: Sync error - ${syncResult.exception.message}", syncResult.exception)
                    Result.failure(syncResult.exception)
                }
                is SyncResult.Conflict -> {
                    // Handle conflicts (for now, just log)
                    Log.w(TAG, "Goal sync conflicts detected")
                    Result.success(Unit)
                }
                is SyncResult.InProgress -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync goals failed", e)
            Result.failure(e)
        }
    }

    private suspend fun syncUsageSessions(userId: String, lastSyncTime: Long): Result<Unit> {
        return try {
            val localSessions = usageSessionDao.getSessionsByUserId(userId)
            val syncResult = syncBackend.syncUsageSessions(localSessions, userId, lastSyncTime)
            
            when (syncResult) {
                is SyncResult.Success -> {
                    syncResult.data.forEach { session ->
                        usageSessionDao.insertSession(session)
                        if (session.cloudId != null) {
                            usageSessionDao.updateSyncStatus(
                                session.id,
                                "SYNCED",
                                session.cloudId,
                                System.currentTimeMillis()
                            )
                        }
                    }
                    Result.success(Unit)
                }
                is SyncResult.Error -> Result.failure(syncResult.exception)
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync sessions failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun syncUsageEvents(userId: String, lastSyncTime: Long): Result<Unit> {
        return try {
            val localEvents = usageEventDao.getEventsByUserId(userId)
            val syncResult = syncBackend.syncUsageEvents(localEvents, userId, lastSyncTime)
            
            when (syncResult) {
                is SyncResult.Success -> {
                    syncResult.data.forEach { event ->
                        usageEventDao.insertEvent(event)
                        if (event.cloudId != null) {
                            usageEventDao.updateSyncStatus(
                                event.id,
                                "SYNCED",
                                event.cloudId,
                                System.currentTimeMillis()
                            )
                        }
                    }
                    Result.success(Unit)
                }
                is SyncResult.Error -> Result.failure(syncResult.exception)
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync events failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun syncDailyStats(userId: String, lastSyncTime: Long): Result<Unit> {
        return try {
            val localStats = dailyStatsDao.getStatsByUserId(userId)
            val syncResult = syncBackend.syncDailyStats(localStats, userId, lastSyncTime)
            
            when (syncResult) {
                is SyncResult.Success -> {
                    syncResult.data.forEach { stat ->
                        dailyStatsDao.upsertDailyStats(stat)
                        if (stat.cloudId != null) {
                            dailyStatsDao.updateSyncStatus(
                                stat.date,
                                stat.targetApp,
                                "SYNCED",
                                stat.cloudId,
                                System.currentTimeMillis()
                            )
                        }
                    }
                    Result.success(Unit)
                }
                is SyncResult.Error -> Result.failure(syncResult.exception)
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync stats failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun syncInterventionResults(userId: String, lastSyncTime: Long): Result<Unit> {
        return try {
            val localResults = interventionResultDao.getResultsByUserId(userId)
            val syncResult = syncBackend.syncInterventionResults(localResults, userId, lastSyncTime)
            
            when (syncResult) {
                is SyncResult.Success -> {
                    syncResult.data.forEach { result ->
                        interventionResultDao.insertResult(result)
                        if (result.cloudId != null) {
                            interventionResultDao.updateSyncStatus(
                                result.id,
                                "SYNCED",
                                result.cloudId,
                                System.currentTimeMillis()
                            )
                        }
                    }
                    Result.success(Unit)
                }
                is SyncResult.Error -> Result.failure(syncResult.exception)
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync intervention results failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun syncStreakRecoveries(userId: String, lastSyncTime: Long): Result<Unit> {
        return try {
            val localRecoveries = streakRecoveryDao.getRecoveriesByUserId(userId)
            val syncResult = syncBackend.syncStreakRecoveries(localRecoveries, userId, lastSyncTime)
            
            when (syncResult) {
                is SyncResult.Success -> {
                    syncResult.data.forEach { recovery ->
                        streakRecoveryDao.upsertRecovery(recovery)
                        if (recovery.cloudId != null) {
                            streakRecoveryDao.updateSyncStatus(
                                recovery.targetApp,
                                "SYNCED",
                                recovery.cloudId,
                                System.currentTimeMillis()
                            )
                        }
                    }
                    Result.success(Unit)
                }
                is SyncResult.Error -> Result.failure(syncResult.exception)
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync recoveries failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun syncUserBaseline(userId: String, lastSyncTime: Long): Result<Unit> {
        return try {
            val localBaseline = userBaselineDao.getBaselineByUserId(userId)
            val syncResult = syncBackend.syncUserBaseline(localBaseline, userId, lastSyncTime)
            
            when (syncResult) {
                is SyncResult.Success -> {
                    syncResult.data?.let { baseline ->
                        userBaselineDao.upsertBaseline(baseline)
                        if (baseline.cloudId != null) {
                            userBaselineDao.updateSyncStatus(
                                baseline.id,
                                "SYNCED",
                                baseline.cloudId,
                                System.currentTimeMillis()
                            )
                        }
                    }
                    Result.success(Unit)
                }
                is SyncResult.Error -> Result.failure(syncResult.exception)
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync baseline failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun syncSettings(userId: String, lastSyncTime: Long): Result<Unit> {
        return try {
            // Settings sync will be handled separately via SettingsRepository
            // For now, just return success
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync settings failed", e)
            Result.failure(e)
        }
    }
    
    // ========== Helper Methods ==========
    
    private suspend fun updateAllEntitiesWithUserId(userId: String) {
        Log.d(TAG, "Updating all entities with userId: $userId")

        // Update all goals
        val goals = goalDao.getAllGoals()
        goals.forEach { goal ->
            goalDao.updateUserId(goal.targetApp, userId)
        }

        // Update all sessions
        val sessions = usageSessionDao.getAllSessions()
        sessions.forEach { session ->
            usageSessionDao.updateUserId(session.id, userId)
        }

        // Update all events
        val events = usageEventDao.getAllEvents()
        events.forEach { event ->
            usageEventDao.updateUserId(event.id, userId)
        }

        // Update all stats
        val stats = dailyStatsDao.getAllStats()
        stats.forEach { stat ->
            dailyStatsDao.updateUserId(stat.date, stat.targetApp, userId)
        }

        // Update all intervention results
        val results = interventionResultDao.getAllResults()
        results.forEach { result ->
            interventionResultDao.updateUserId(result.id, userId)
        }

        // Update all recoveries
        val recoveries = streakRecoveryDao.getAllRecoveries()
        recoveries.forEach { recovery ->
            streakRecoveryDao.updateUserId(recovery.targetApp, userId)
        }

        // Update baseline
        userBaselineDao.getBaseline()?.let { baseline ->
            userBaselineDao.updateUserId(baseline.id, userId)
        }
    }

    private suspend fun markAllAsPending() {
        Log.d(TAG, "Marking all entities as pending")

        // Mark all entities with userId as PENDING for sync
        // This is done automatically when userId is set, as entities default to PENDING status
        // No explicit action needed since entities are created with syncStatus = "PENDING"
    }
}
