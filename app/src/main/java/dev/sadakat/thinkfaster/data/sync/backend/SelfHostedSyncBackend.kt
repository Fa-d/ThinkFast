package dev.sadakat.thinkfaster.data.sync.backend

import dev.sadakat.thinkfaster.data.local.database.entities.*
import dev.sadakat.thinkfaster.data.sync.model.AuthCredential
import dev.sadakat.thinkfaster.data.sync.model.SyncResult
import dev.sadakat.thinkfaster.data.sync.model.SyncUser

/**
 * Self-hosted backend implementation of SyncBackend
 * Uses custom REST API for data storage
 *
 * To be implemented later for self-hosted deployments
 */
class SelfHostedSyncBackend : SyncBackend {

    override suspend fun signIn(authCredential: AuthCredential): Result<SyncUser> {
        return Result.failure(NotImplementedError("Self-hosted backend not yet implemented"))
    }

    override suspend fun signOut(): Result<Unit> {
        return Result.failure(NotImplementedError("Self-hosted backend not yet implemented"))
    }

    override suspend fun getCurrentUser(): SyncUser? {
        return null
    }

    override suspend fun linkAnonymousAccount(authCredential: AuthCredential): Result<SyncUser> {
        return Result.failure(NotImplementedError("Self-hosted backend not yet implemented"))
    }

    override suspend fun syncGoals(
        localGoals: List<GoalEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<GoalEntity>> {
        return SyncResult.Error(NotImplementedError("Not implemented"), "Self-hosted backend not yet implemented")
    }

    override suspend fun syncUsageSessions(
        sessions: List<UsageSessionEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<UsageSessionEntity>> {
        return SyncResult.Error(NotImplementedError("Not implemented"), "Self-hosted backend not yet implemented")
    }

    override suspend fun syncUsageEvents(
        events: List<UsageEventEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<UsageEventEntity>> {
        return SyncResult.Error(NotImplementedError("Not implemented"), "Self-hosted backend not yet implemented")
    }

    override suspend fun syncDailyStats(
        stats: List<DailyStatsEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<DailyStatsEntity>> {
        return SyncResult.Error(NotImplementedError("Not implemented"), "Self-hosted backend not yet implemented")
    }

    override suspend fun syncInterventionResults(
        results: List<InterventionResultEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<InterventionResultEntity>> {
        return SyncResult.Error(NotImplementedError("Not implemented"), "Self-hosted backend not yet implemented")
    }

    override suspend fun syncStreakRecoveries(
        recoveries: List<StreakRecoveryEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<StreakRecoveryEntity>> {
        return SyncResult.Error(NotImplementedError("Not implemented"), "Self-hosted backend not yet implemented")
    }

    override suspend fun syncUserBaseline(
        baseline: UserBaselineEntity?,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<UserBaselineEntity?> {
        return SyncResult.Error(NotImplementedError("Not implemented"), "Self-hosted backend not yet implemented")
    }

    override suspend fun syncSettings(
        settings: Map<String, Any>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<Map<String, Any>> {
        return SyncResult.Error(NotImplementedError("Not implemented"), "Self-hosted backend not yet implemented")
    }

    override suspend fun performFullSync(userId: String): SyncResult<Unit> {
        return SyncResult.Error(NotImplementedError("Not implemented"), "Self-hosted backend not yet implemented")
    }

    override suspend fun uploadPendingChanges(userId: String): SyncResult<Unit> {
        return SyncResult.Error(NotImplementedError("Not implemented"), "Self-hosted backend not yet implemented")
    }

    override suspend fun downloadRemoteChanges(userId: String, lastSyncTime: Long): SyncResult<Unit> {
        return SyncResult.Error(NotImplementedError("Not implemented"), "Self-hosted backend not yet implemented")
    }

    override suspend fun deleteAllUserData(userId: String): Result<Unit> {
        return Result.failure(NotImplementedError("Self-hosted backend not yet implemented"))
    }
}
