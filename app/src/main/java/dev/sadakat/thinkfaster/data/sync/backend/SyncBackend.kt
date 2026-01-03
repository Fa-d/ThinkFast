package dev.sadakat.thinkfaster.data.sync.backend

import dev.sadakat.thinkfaster.data.local.database.entities.*
import dev.sadakat.thinkfaster.data.sync.model.AuthCredential
import dev.sadakat.thinkfaster.data.sync.model.SyncResult
import dev.sadakat.thinkfaster.data.sync.model.SyncUser

/**
 * Backend abstraction layer for multi-device sync
 * Phase 3: Firebase Backend Implementation
 * 
 * This interface allows swapping backends (Firebase, Supabase, Self-Hosted)
 * via build flavors without changing sync logic
 */
interface SyncBackend {
    
    // ========== Authentication ==========
    
    /**
     * Sign in with provided credentials
     * @param authCredential Authentication credentials (Facebook, Google, Email)
     * @return Result containing SyncUser if successful
     */
    suspend fun signIn(authCredential: AuthCredential): Result<SyncUser>
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Get currently authenticated user
     * @return SyncUser if authenticated, null otherwise
     */
    suspend fun getCurrentUser(): SyncUser?
    
    /**
     * Link anonymous account to authenticated account
     * Migrates all local data under anonymous ID to authenticated user ID
     */
    suspend fun linkAnonymousAccount(authCredential: AuthCredential): Result<SyncUser>
    
    // ========== Data Sync Operations ==========

    /**
     * Sync goals between local and remote
     * @param localGoals Local goals to sync
     * @param userId User ID for Supabase queries
     * @param lastSyncTime Timestamp of last successful sync (0 if first sync)
     * @return SyncResult containing merged goals
     */
    suspend fun syncGoals(
        localGoals: List<GoalEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<GoalEntity>>

    /**
     * Sync usage sessions between local and remote
     */
    suspend fun syncUsageSessions(
        sessions: List<UsageSessionEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<UsageSessionEntity>>

    /**
     * Sync usage events between local and remote
     */
    suspend fun syncUsageEvents(
        events: List<UsageEventEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<UsageEventEntity>>

    /**
     * Sync daily stats between local and remote
     */
    suspend fun syncDailyStats(
        stats: List<DailyStatsEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<DailyStatsEntity>>

    /**
     * Sync intervention results between local and remote
     */
    suspend fun syncInterventionResults(
        results: List<InterventionResultEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<InterventionResultEntity>>

    /**
     * Sync streak recovery data between local and remote
     */
    suspend fun syncStreakRecoveries(
        recoveries: List<StreakRecoveryEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<StreakRecoveryEntity>>

    /**
     * Sync user baseline data between local and remote
     */
    suspend fun syncUserBaseline(
        baseline: UserBaselineEntity?,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<UserBaselineEntity?>

    /**
     * Sync app settings between local and remote
     * Settings are serialized as key-value map
     */
    suspend fun syncSettings(
        settings: Map<String, Any>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<Map<String, Any>>
    
    /**
     * Perform full sync of all data types
     * Used on initial login and manual sync trigger
     * @param userId User ID to sync for
     */
    suspend fun performFullSync(userId: String): SyncResult<Unit>
    
    /**
     * Upload local changes marked as PENDING
     * Used by background sync worker
     */
    suspend fun uploadPendingChanges(userId: String): SyncResult<Unit>
    
    /**
     * Download remote changes since last sync
     * Used by background sync worker
     */
    suspend fun downloadRemoteChanges(userId: String, lastSyncTime: Long): SyncResult<Unit>
    
    /**
     * Delete all user data from backend
     * Used for account deletion (GDPR compliance)
     */
    suspend fun deleteAllUserData(userId: String): Result<Unit>
}
