//package dev.sadakat.thinkfaster.data.sync.backend
//
//import android.content.Context
//import dev.sadakat.thinkfaster.data.local.database.entities.*
//import dev.sadakat.thinkfaster.data.sync.model.AuthCredential
//import dev.sadakat.thinkfaster.data.sync.model.SyncResult
//import dev.sadakat.thinkfaster.data.sync.model.SyncUser
//
///**
// * Stub Supabase implementation for SelfHosted builds
// * This class should never be instantiated in SelfHosted builds
// */
//class SupabaseSyncBackend(private val context: Context) : SyncBackend {
//    override suspend fun signIn(authCredential: AuthCredential): Result<SyncUser> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun signOut(): Result<Unit> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun getCurrentUser(): SyncUser? {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun linkAnonymousAccount(authCredential: AuthCredential): Result<SyncUser> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun syncGoals(
//        localGoals: List<GoalEntity>,
//        userId: String,
//        lastSyncTime: Long
//    ): SyncResult<List<GoalEntity>> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun syncUsageSessions(
//        sessions: List<UsageSessionEntity>,
//        userId: String,
//        lastSyncTime: Long
//    ): SyncResult<List<UsageSessionEntity>> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun syncUsageEvents(
//        events: List<UsageEventEntity>,
//        userId: String,
//        lastSyncTime: Long
//    ): SyncResult<List<UsageEventEntity>> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun syncDailyStats(
//        stats: List<DailyStatsEntity>,
//        userId: String,
//        lastSyncTime: Long
//    ): SyncResult<List<DailyStatsEntity>> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun syncInterventionResults(
//        results: List<InterventionResultEntity>,
//        userId: String,
//        lastSyncTime: Long
//    ): SyncResult<List<InterventionResultEntity>> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun syncStreakRecoveries(
//        recoveries: List<StreakRecoveryEntity>,
//        userId: String,
//        lastSyncTime: Long
//    ): SyncResult<List<StreakRecoveryEntity>> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun syncUserBaseline(
//        baseline: UserBaselineEntity?,
//        userId: String,
//        lastSyncTime: Long
//    ): SyncResult<UserBaselineEntity?> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun syncSettings(
//        settings: Map<String, Any>,
//        userId: String,
//        lastSyncTime: Long
//    ): SyncResult<Map<String, Any>> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun performFullSync(userId: String): SyncResult<Unit> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun uploadPendingChanges(userId: String): SyncResult<Unit> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun downloadRemoteChanges(userId: String, lastSyncTime: Long): SyncResult<Unit> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//
//    override suspend fun deleteAllUserData(userId: String): Result<Unit> {
//        throw UnsupportedOperationException("Supabase backend not available in SelfHosted build")
//    }
//}
