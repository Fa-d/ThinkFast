package dev.sadakat.thinkfaster.data.sync.backend

import android.content.Context
import android.util.Log
import dev.sadakat.thinkfaster.data.local.database.entities.*
import dev.sadakat.thinkfaster.data.sync.model.AuthCredential
import dev.sadakat.thinkfaster.data.sync.model.AuthProvider
import dev.sadakat.thinkfaster.data.sync.model.SyncResult
import dev.sadakat.thinkfaster.data.sync.model.SyncUser
import dev.sadakat.thinkfaster.data.sync.supabase.*
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

/**
 * Supabase implementation of SyncBackend
 * Phase 7: Supabase Backend Implementation
 *
 * Implements authentication and data sync using:
 * - PostgreSQL database via PostgREST
 * - GoTrue for OAuth authentication (Google/Facebook)
 * - Realtime subscriptions for live updates
 */
class SupabaseSyncBackend(
    private val context: Context
) : SyncBackend {

    companion object {
        private const val TAG = "SupabaseSyncBackend"
    }

    private val supabase by lazy { SupabaseClientProvider.getInstance(context) }

    override suspend fun signIn(authCredential: AuthCredential): Result<SyncUser> {
        return try {
            when (authCredential) {
                is AuthCredential.Google -> {
                    // Sign in with Google ID token
                    supabase.auth.signInWith(IDToken) {
                        idToken = authCredential.idToken
                        provider = io.github.jan.supabase.gotrue.providers.Google
                    }
                }
                is AuthCredential.Facebook -> {
                    // Sign in with Facebook access token
                    supabase.auth.signInWith(IDToken) {
                        idToken = authCredential.accessToken
                        provider = io.github.jan.supabase.gotrue.providers.Facebook
                    }
                }
                is AuthCredential.Email -> {
                    // Email/password sign in
                    supabase.auth.signInWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                        email = authCredential.email
                        password = authCredential.password
                    }
                }
            }

            // Get current user after sign in
            val user = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Sign in succeeded but user is null"))

            Result.success(user.toSyncUser(authCredential))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): SyncUser? {
        return try {
            supabase.auth.currentUserOrNull()?.toSyncUser()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun linkAnonymousAccount(authCredential: AuthCredential): Result<SyncUser> {
        // Supabase doesn't have built-in anonymous auth like Firebase
        // Instead, we'll sign in normally and migrate data manually
        return signIn(authCredential)
    }

    /**
     * Convert Supabase UserInfo to SyncUser
     * @param authCredential The credential used for sign in (to determine provider)
     */
    private fun UserInfo.toSyncUser(authCredential: AuthCredential? = null): SyncUser {
        // Determine provider from the auth credential (most accurate)
        val provider = when (authCredential) {
            is AuthCredential.Google -> AuthProvider.GOOGLE
            is AuthCredential.Facebook -> AuthProvider.FACEBOOK
            is AuthCredential.Email -> AuthProvider.EMAIL
            null -> {
                // Fallback: try to determine from identities, app metadata, or user metadata
                val providerFromIdentities = identities?.firstOrNull()?.provider?.toString()
                val providerFromAppMetadata = appMetadata?.get("provider")?.toString()
                val providerFromUserMetadata = userMetadata?.get("provider")?.toString()

                when {
                    providerFromIdentities == "google" || providerFromAppMetadata == "google" || providerFromUserMetadata == "google" -> AuthProvider.GOOGLE
                    providerFromIdentities == "facebook" || providerFromAppMetadata == "facebook" || providerFromUserMetadata == "facebook" -> AuthProvider.FACEBOOK
                    email != null -> AuthProvider.EMAIL
                    else -> AuthProvider.ANONYMOUS
                }
            }
        }

        return SyncUser(
            userId = id,
            email = email,
            displayName = userMetadata?.get("full_name")?.toString()
                ?: userMetadata?.get("name")?.toString(),
            photoUrl = userMetadata?.get("avatar_url")?.toString()
                ?: userMetadata?.get("picture")?.toString(),
            provider = provider,
            isAnonymous = false
        )
    }

    override suspend fun syncGoals(
        localGoals: List<GoalEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<GoalEntity>> {
        return try {
            Log.d(TAG, "syncGoals: Starting with ${localGoals.size} local goals, userId=$userId, lastSyncTime=$lastSyncTime")

            // Upload pending local changes and get back the records with cloudIds
            val uploadedGoals = mutableListOf<GoalEntity>()
            val pendingGoals = localGoals.filter { it.syncStatus == "PENDING" }
            if (pendingGoals.isNotEmpty()) {
                Log.d(TAG, "syncGoals: Uploading ${pendingGoals.size} pending goals")
                val supabaseGoals = pendingGoals.map { it.toSupabase(userId) }
                val returnedGoals = supabase.from("goals")
                    .upsert(supabaseGoals, onConflict = "user_id,target_app") {
                        select()
                    }
                    .decodeList<SupabaseGoal>()
                uploadedGoals.addAll(returnedGoals.map { it.toEntity() })
                Log.d(TAG, "syncGoals: Successfully uploaded ${pendingGoals.size} pending goals, received ${returnedGoals.size} back with cloudIds")
            }

            // Download remote changes since last sync
            Log.d(TAG, "syncGoals: Downloading remote goals since lastSyncTime=$lastSyncTime")
            val remoteGoals = supabase.from("goals")
                .select {
                    filter {
                        SupabaseGoal::userId eq userId
                        SupabaseGoal::lastModified gt lastSyncTime
                    }
                }
                .decodeList<SupabaseGoal>()

            Log.d(TAG, "syncGoals: Downloaded ${remoteGoals.size} remote goals")

            // Merge local, uploaded (with cloudIds), and remote
            val mergedGoals = mergeGoals(
                localGoals.filterNot { it.syncStatus == "PENDING" },  // Exclude uploaded items from local
                uploadedGoals + remoteGoals.map { it.toEntity() }     // Include uploaded + remote
            )

            Log.d(TAG, "syncGoals: Merged to ${mergedGoals.size} goals")
            SyncResult.Success(mergedGoals)
        } catch (e: Exception) {
            Log.e(TAG, "syncGoals: Exception - ${e.message}", e)
            SyncResult.Error(e, e.message ?: "Goals sync failed")
        }
    }

    /**
     * Merge local and remote goals with conflict resolution
     * Strategy: Max streak wins (preserve user progress)
     */
    private fun mergeGoals(local: List<GoalEntity>, remote: List<GoalEntity>): List<GoalEntity> {
        val localMap = local.associateBy { it.targetApp }
        val remoteMap = remote.associateBy { it.targetApp }

        return (localMap.keys + remoteMap.keys).mapNotNull { targetApp ->
            val localGoal = localMap[targetApp]
            val remoteGoal = remoteMap[targetApp]

            when {
                localGoal == null -> remoteGoal
                remoteGoal == null -> localGoal
                // Conflict: Choose goal with better progress (max streak)
                localGoal.currentStreak > remoteGoal.currentStreak -> localGoal.copy(
                    syncStatus = "SYNCED",
                    cloudId = remoteGoal.cloudId
                )
                remoteGoal.currentStreak > localGoal.currentStreak -> remoteGoal
                // Same streak: Last-write-wins
                localGoal.lastModified > remoteGoal.lastModified -> localGoal.copy(
                    syncStatus = "SYNCED",
                    cloudId = remoteGoal.cloudId
                )
                else -> remoteGoal
            }
        }
    }

    override suspend fun syncUsageSessions(
        sessions: List<UsageSessionEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<UsageSessionEntity>> {
        return try {
            // Upload pending local sessions and get back records with cloudIds
            val uploadedSessions = mutableListOf<UsageSessionEntity>()
            val pendingSessions = sessions.filter { it.syncStatus == "PENDING" }
            if (pendingSessions.isNotEmpty()) {
                val supabaseSessions = pendingSessions.map { it.toSupabase(userId) }
                val returnedSessions = supabase.from("usage_sessions")
                    .upsert(supabaseSessions, onConflict = "id") {
                        select()
                    }
                    .decodeList<SupabaseUsageSession>()

                // Map returned sessions back to entities, preserving local IDs
                val localIdMap = pendingSessions.associateBy { it.startTimestamp to it.targetApp }
                returnedSessions.forEach { returned ->
                    val localId = localIdMap[returned.startTimestamp to returned.targetApp]?.id ?: 0
                    uploadedSessions.add(returned.toEntity(localId))
                }
            }

            // Download remote changes since last sync (paginated for large datasets)
            val remoteSessions = downloadSessionsPaginated(userId, lastSyncTime)

            // Merge: Sessions are append-only, merge by unique cloud ID
            val mergedSessions = mergeSessions(
                sessions.filterNot { it.syncStatus == "PENDING" },  // Exclude uploaded items
                uploadedSessions + remoteSessions                    // Include uploaded + remote
            )

            SyncResult.Success(mergedSessions)
        } catch (e: Exception) {
            SyncResult.Error(e, e.message ?: "Usage sessions sync failed")
        }
    }

    /**
     * Download sessions with pagination (handles large datasets)
     */
    private suspend fun downloadSessionsPaginated(
        userId: String,
        lastSyncTime: Long,
        pageSize: Int = 500
    ): List<UsageSessionEntity> {
        val allSessions = mutableListOf<UsageSessionEntity>()
        var offset = 0

        while (true) {
            val page = supabase.from("usage_sessions")
                .select {
                    filter {
                        SupabaseUsageSession::userId eq userId
                        SupabaseUsageSession::lastModified gt lastSyncTime
                    }
                    order(column = "last_modified", order = io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    range(offset.toLong()..(offset + pageSize - 1).toLong())
                }
                .decodeList<SupabaseUsageSession>()

            allSessions.addAll(page.map { (it as SupabaseUsageSession).toEntity() })

            if (page.size < pageSize) break // Last page
            offset += pageSize
        }

        return allSessions
    }

    /**
     * Merge sessions by unique cloud ID (append-only, no conflicts)
     */
    private fun mergeSessions(
        local: List<UsageSessionEntity>,
        remote: List<UsageSessionEntity>
    ): List<UsageSessionEntity> {
        val localMap = local.associateBy { it.cloudId }
        val remoteMap = remote.associateBy { it.cloudId }

        // Combine all sessions, preferring remote if cloudId exists in both
        val allIds = (localMap.keys + remoteMap.keys).filterNotNull()
        val localOnly = local.filter { it.cloudId == null }

        return allIds.map { cloudId ->
            remoteMap[cloudId] ?: localMap[cloudId]!!
        } + localOnly
    }

    override suspend fun syncUsageEvents(
        events: List<UsageEventEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<UsageEventEntity>> {
        return try {
            // Upload pending local events and get back records with cloudIds
            val uploadedEvents = mutableListOf<UsageEventEntity>()
            val pendingEvents = events.filter { it.syncStatus == "PENDING" }
            if (pendingEvents.isNotEmpty()) {
                val supabaseEvents = pendingEvents.map { it.toSupabase(userId) }
                val returnedEvents = supabase.from("usage_events")
                    .upsert(supabaseEvents, onConflict = "id") {
                        select()
                    }
                    .decodeList<SupabaseUsageEvent>()

                // Map returned events back to entities, preserving local IDs
                val localIdMap = pendingEvents.associateBy { it.sessionId to it.timestamp }
                returnedEvents.forEach { returned ->
                    val localId = localIdMap[returned.sessionId to returned.timestamp]?.id ?: 0
                    uploadedEvents.add(returned.toEntity(localId))
                }
            }

            // Download remote changes (append-only, similar to sessions)
            val remoteEvents = supabase.from("usage_events")
                .select {
                    filter {
                        SupabaseUsageEvent::userId eq userId
                        SupabaseUsageEvent::lastModified gt lastSyncTime
                    }
                }
                .decodeList<SupabaseUsageEvent>()

            // Merge by cloud ID (append-only)
            val mergedEvents = mergeEvents(
                events.filterNot { it.syncStatus == "PENDING" },
                uploadedEvents + remoteEvents.map { (it as SupabaseUsageEvent).toEntity() }
            )

            SyncResult.Success(mergedEvents)
        } catch (e: Exception) {
            SyncResult.Error(e, e.message ?: "Usage events sync failed")
        }
    }

    /**
     * Merge usage events (append-only)
     */
    private fun mergeEvents(
        local: List<UsageEventEntity>,
        remote: List<UsageEventEntity>
    ): List<UsageEventEntity> {
        val localMap = local.associateBy { it.cloudId }
        val remoteMap = remote.associateBy { it.cloudId }

        val allIds = (localMap.keys + remoteMap.keys).filterNotNull()
        val localOnly = local.filter { it.cloudId == null }

        return allIds.map { cloudId ->
            remoteMap[cloudId] ?: localMap[cloudId]!!
        } + localOnly
    }

    override suspend fun syncDailyStats(
        stats: List<DailyStatsEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<DailyStatsEntity>> {
        return try {
            // Upload pending local stats and get back records with cloudIds
            val uploadedStats = mutableListOf<DailyStatsEntity>()
            val pendingStats = stats.filter { it.syncStatus == "PENDING" }
            if (pendingStats.isNotEmpty()) {
                val supabaseStats = pendingStats.map { it.toSupabase(userId) }
                val returnedStats = supabase.from("daily_stats")
                    .upsert(supabaseStats, onConflict = "user_id,target_app,date") {
                        select()
                    }
                    .decodeList<SupabaseDailyStats>()
                uploadedStats.addAll(returnedStats.map { it.toEntity() })
            }

            // Download remote changes since last sync
            val remoteStats = supabase.from("daily_stats")
                .select {
                    filter {
                        SupabaseDailyStats::userId eq userId
                        SupabaseDailyStats::lastModified gt lastSyncTime
                    }
                }
                .decodeList<SupabaseDailyStats>()

            // Merge with last-write-wins strategy
            val mergedStats = mergeDailyStats(
                stats.filterNot { it.syncStatus == "PENDING" },
                uploadedStats + remoteStats.map { (it as SupabaseDailyStats).toEntity() }
            )

            SyncResult.Success(mergedStats)
        } catch (e: Exception) {
            SyncResult.Error(e, e.message ?: "Daily stats sync failed")
        }
    }

    /**
     * Merge daily stats with last-write-wins strategy
     */
    private fun mergeDailyStats(
        local: List<DailyStatsEntity>,
        remote: List<DailyStatsEntity>
    ): List<DailyStatsEntity> {
        val localMap = local.associateBy { "${it.date}_${it.targetApp}" }
        val remoteMap = remote.associateBy { "${it.date}_${it.targetApp}" }

        return (localMap.keys + remoteMap.keys).mapNotNull { key ->
            val localStat = localMap[key]
            val remoteStat = remoteMap[key]

            when {
                localStat == null -> remoteStat
                remoteStat == null -> localStat
                // Last-write-wins
                localStat.lastModified > remoteStat.lastModified -> localStat.copy(
                    syncStatus = "SYNCED",
                    cloudId = remoteStat.cloudId
                )
                else -> remoteStat
            }
        }
    }

    override suspend fun syncInterventionResults(
        results: List<InterventionResultEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<InterventionResultEntity>> {
        return try {
            // Upload pending results and get back records with cloudIds
            val uploadedResults = mutableListOf<InterventionResultEntity>()
            val pendingResults = results.filter { it.syncStatus == "PENDING" }
            if (pendingResults.isNotEmpty()) {
                val supabaseResults = pendingResults.map { it.toSupabase(userId) }
                val returnedResults = supabase.from("intervention_results")
                    .upsert(supabaseResults, onConflict = "id") {
                        select()
                    }
                    .decodeList<SupabaseInterventionResult>()

                // Map returned results back to entities, preserving local IDs
                val localIdMap = pendingResults.associateBy { it.sessionId to it.timestamp }
                returnedResults.forEach { returned ->
                    val localId = localIdMap[returned.sessionId to returned.timestamp]?.id ?: 0
                    uploadedResults.add(returned.toEntity(localId))
                }
            }

            // Download remote changes (append-only analytics data)
            val remoteResults = supabase.from("intervention_results")
                .select {
                    filter {
                        SupabaseInterventionResult::userId eq userId
                        SupabaseInterventionResult::lastModified gt lastSyncTime
                    }
                }
                .decodeList<SupabaseInterventionResult>()

            // Merge (append-only)
            val mergedResults = mergeInterventionResults(
                results.filterNot { it.syncStatus == "PENDING" },
                uploadedResults + remoteResults.map { (it as SupabaseInterventionResult).toEntity() }
            )

            SyncResult.Success(mergedResults)
        } catch (e: Exception) {
            SyncResult.Error(e, e.message ?: "Intervention results sync failed")
        }
    }

    /**
     * Merge intervention results (append-only analytics)
     */
    private fun mergeInterventionResults(
        local: List<InterventionResultEntity>,
        remote: List<InterventionResultEntity>
    ): List<InterventionResultEntity> {
        val localMap = local.associateBy { it.cloudId }
        val remoteMap = remote.associateBy { it.cloudId }

        val allIds = (localMap.keys + remoteMap.keys).filterNotNull()
        val localOnly = local.filter { it.cloudId == null }

        return allIds.map { cloudId ->
            remoteMap[cloudId] ?: localMap[cloudId]!!
        } + localOnly
    }

    override suspend fun syncStreakRecoveries(
        recoveries: List<StreakRecoveryEntity>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<List<StreakRecoveryEntity>> {
        return try {
            // Upload pending recoveries and get back records with cloudIds
            val uploadedRecoveries = mutableListOf<StreakRecoveryEntity>()
            val pendingRecoveries = recoveries.filter { it.syncStatus == "PENDING" }
            if (pendingRecoveries.isNotEmpty()) {
                val supabaseRecoveries = pendingRecoveries.map { it.toSupabase(userId) }
                val returnedRecoveries = supabase.from("streak_recoveries")
                    .upsert(supabaseRecoveries, onConflict = "user_id,target_app") {
                        select()
                    }
                    .decodeList<SupabaseStreakRecovery>()
                uploadedRecoveries.addAll(returnedRecoveries.map { it.toEntity() })
            }

            // Download remote changes
            val remoteRecoveries = supabase.from("streak_recoveries")
                .select {
                    filter {
                        SupabaseStreakRecovery::userId eq userId
                        SupabaseStreakRecovery::lastModified gt lastSyncTime
                    }
                }
                .decodeList<SupabaseStreakRecovery>()

            // Merge with last-write-wins
            val mergedRecoveries = mergeStreakRecoveries(
                recoveries.filterNot { it.syncStatus == "PENDING" },
                uploadedRecoveries + remoteRecoveries.map { (it as SupabaseStreakRecovery).toEntity() }
            )

            SyncResult.Success(mergedRecoveries)
        } catch (e: Exception) {
            SyncResult.Error(e, e.message ?: "Streak recoveries sync failed")
        }
    }

    /**
     * Merge streak recoveries by targetApp (one per app)
     */
    private fun mergeStreakRecoveries(
        local: List<StreakRecoveryEntity>,
        remote: List<StreakRecoveryEntity>
    ): List<StreakRecoveryEntity> {
        val localMap = local.associateBy { it.targetApp }
        val remoteMap = remote.associateBy { it.targetApp }

        return (localMap.keys + remoteMap.keys).mapNotNull { targetApp ->
            val localRecovery = localMap[targetApp]
            val remoteRecovery = remoteMap[targetApp]

            when {
                localRecovery == null -> remoteRecovery
                remoteRecovery == null -> localRecovery
                // Last-write-wins
                localRecovery.lastModified > remoteRecovery.lastModified -> localRecovery.copy(
                    syncStatus = "SYNCED",
                    cloudId = remoteRecovery.cloudId
                )
                else -> remoteRecovery
            }
        }
    }

    override suspend fun syncUserBaseline(
        baseline: UserBaselineEntity?,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<UserBaselineEntity?> {
        return try {
            // Upload local baseline if exists and pending, get back with cloudId
            var uploadedBaseline: UserBaselineEntity? = null
            if (baseline != null && baseline.syncStatus == "PENDING") {
                val supabaseBaseline = baseline.toSupabase(userId)
                val returnedBaseline = supabase.from("user_baseline")
                    .upsert(supabaseBaseline, onConflict = "user_id,target_app") {
                        select()
                    }
                    .decodeList<SupabaseUserBaseline>()
                uploadedBaseline = returnedBaseline.firstOrNull()?.toEntity()
            }

            // Download remote baseline (single row per user)
            val remoteBaseline: SupabaseUserBaseline? = try {
                val results = supabase.from("user_baseline")
                    .select {
                        filter {
                            SupabaseUserBaseline::userId eq userId
                        }
                    }
                    .decodeList<SupabaseUserBaseline>()
                results.firstOrNull()
            } catch (e: Exception) {
                // No remote baseline exists
                null
            }

            // Use the most recent: uploaded, remote, or local
            val mergedBaseline: UserBaselineEntity? = when {
                uploadedBaseline != null -> uploadedBaseline
                remoteBaseline != null && (baseline == null || remoteBaseline.lastModified > baseline.lastModified) -> remoteBaseline.toEntity()
                baseline != null -> baseline.copy(syncStatus = "SYNCED", cloudId = remoteBaseline?.id)
                else -> null
            }

            SyncResult.Success(mergedBaseline)
        } catch (e: Exception) {
            SyncResult.Error(e, e.message ?: "User baseline sync failed")
        }
    }

    override suspend fun syncSettings(
        settings: Map<String, Any>,
        userId: String,
        lastSyncTime: Long
    ): SyncResult<Map<String, Any>> {
        return try {

            // Serialize settings to JSON
            val settingsJson = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.serializer(),
                settings
            )

            // Upload to Supabase
            supabase.from("settings").upsert(SupabaseSettings(
                userId = userId,
                settingsJson = settingsJson,
                lastModified = System.currentTimeMillis()
            ), onConflict = "user_id")

            // Download latest settings (remote wins for settings)
            val remoteSettings: SupabaseSettings = try {
                val results = supabase.from("settings")
                    .select {
                        filter {
                            SupabaseSettings::userId eq userId
                        }
                    }
                    .decodeList<SupabaseSettings>()
                results.firstOrNull() ?: throw Exception("No remote settings found")
            } catch (e: Exception) {
                // If no remote settings exist, return local
                return SyncResult.Success(settings)
            }

            val remoteMap = kotlinx.serialization.json.Json.decodeFromString<Map<String, Any>>(
                remoteSettings.settingsJson
            )

            // Remote wins for settings
            SyncResult.Success(remoteMap)
        } catch (e: Exception) {
            SyncResult.Error(e, e.message ?: "Settings sync failed")
        }
    }

    override suspend fun performFullSync(userId: String): SyncResult<Unit> {
        return try {
            // Verify authentication
            getCurrentUser()?.userId
                ?: return SyncResult.Error(Exception("Not authenticated"), "User not authenticated")

            // Upload all pending changes first
            uploadPendingChanges(userId)

            // Then download all remote changes
            // Note: lastSyncTime would come from SyncPreferences in real implementation
            downloadRemoteChanges(userId, 0)

            SyncResult.Success(Unit)
        } catch (e: Exception) {
            SyncResult.Error(e, e.message ?: "Full sync failed")
        }
    }

    override suspend fun uploadPendingChanges(userId: String): SyncResult<Unit> {
        return try {
            // Verify authentication
            getCurrentUser()?.userId
                ?: return SyncResult.Error(Exception("Not authenticated"), "User not authenticated")

            // Upload pending changes for each entity type
            // Note: This method uploads entities that are already tracked locally with sync_status = 'PENDING'
            // The SyncCoordinator is responsible for querying the local DB and passing entities to individual sync methods
            // This is a high-level method that could be used for batch uploads if needed

            // For now, return success as the actual upload is handled by individual sync methods
            // (syncGoals, syncUsageSessions, etc.) which are called by SyncCoordinator
            SyncResult.Success(Unit)
        } catch (e: Exception) {
            SyncResult.Error(e, e.message ?: "Upload pending changes failed")
        }
    }

    override suspend fun downloadRemoteChanges(userId: String, lastSyncTime: Long): SyncResult<Unit> {
        return try {
            // Verify authentication
            getCurrentUser()?.userId
                ?: return SyncResult.Error(Exception("Not authenticated"), "User not authenticated")

            // Download all remote changes since lastSyncTime for each entity type
            // This fetches data from Supabase that needs to be merged with local data

            // Download goals changed since lastSyncTime
            val remoteGoals = supabase.from("goals")
                .select {
                    filter {
                        SupabaseGoal::userId eq userId
                        SupabaseGoal::lastModified gt lastSyncTime
                    }
                }
                .decodeList<SupabaseGoal>()

            // Download sessions changed since lastSyncTime (with pagination)
            val remoteSessions = mutableListOf<SupabaseUsageSession>()
            var offset = 0
            val pageSize = 500
            do {
                val page = supabase.from("usage_sessions")
                    .select {
                        filter {
                            SupabaseUsageSession::userId eq userId
                            SupabaseUsageSession::lastModified gt lastSyncTime
                        }
                        range(offset.toLong()..(offset + pageSize - 1).toLong())
                    }
                    .decodeList<SupabaseUsageSession>()
                remoteSessions.addAll(page)
                offset += pageSize
            } while (page.size == pageSize)

            // Download events changed since lastSyncTime (with pagination)
            val remoteEvents = mutableListOf<SupabaseUsageEvent>()
            offset = 0
            do {
                val page = supabase.from("usage_events")
                    .select {
                        filter {
                            SupabaseUsageEvent::userId eq userId
                            SupabaseUsageEvent::lastModified gt lastSyncTime
                        }
                        range(offset.toLong()..(offset + pageSize - 1).toLong())
                    }
                    .decodeList<SupabaseUsageEvent>()
                remoteEvents.addAll(page)
                offset += pageSize
            } while (page.size == pageSize)

            // Download stats changed since lastSyncTime
            val remoteStats = supabase.from("daily_stats")
                .select {
                    filter {
                        SupabaseDailyStats::userId eq userId
                        SupabaseDailyStats::lastModified gt lastSyncTime
                    }
                }
                .decodeList<SupabaseDailyStats>()

            // Download intervention results changed since lastSyncTime (with pagination)
            val remoteResults = mutableListOf<SupabaseInterventionResult>()
            offset = 0
            do {
                val page = supabase.from("intervention_results")
                    .select {
                        filter {
                            SupabaseInterventionResult::userId eq userId
                            SupabaseInterventionResult::lastModified gt lastSyncTime
                        }
                        range(offset.toLong()..(offset + pageSize - 1).toLong())
                    }
                    .decodeList<SupabaseInterventionResult>()
                remoteResults.addAll(page)
                offset += pageSize
            } while (page.size == pageSize)

            // Download streak recoveries changed since lastSyncTime
            val remoteRecoveries = supabase.from("streak_recoveries")
                .select {
                    filter {
                        SupabaseStreakRecovery::userId eq userId
                        SupabaseStreakRecovery::lastModified gt lastSyncTime
                    }
                }
                .decodeList<SupabaseStreakRecovery>()

            // Download user baseline
            val remoteBaseline: SupabaseUserBaseline? = try {
                supabase.from("user_baseline")
                    .select {
                        filter {
                            SupabaseUserBaseline::userId eq userId
                            SupabaseUserBaseline::lastModified gt lastSyncTime
                        }
                    }
                    .decodeSingle<SupabaseUserBaseline>()
            } catch (e: Exception) {
                null
            }

            // Note: The downloaded data is available here but not returned due to interface limitations
            // In the current architecture, SyncCoordinator handles the merging via individual sync methods
            // This method serves as a verification that data can be downloaded from Supabase

            android.util.Log.d("SupabaseSyncBackend", "Downloaded ${remoteGoals.size} goals, ${remoteSessions.size} sessions, ${remoteEvents.size} events, ${remoteStats.size} stats, ${remoteResults.size} results, ${remoteRecoveries.size} recoveries")

            SyncResult.Success(Unit)
        } catch (e: Exception) {
            SyncResult.Error(e, e.message ?: "Download remote changes failed")
        }
    }

    override suspend fun deleteAllUserData(userId: String): Result<Unit> {
        return try {
            // Delete user's data from all tables
            supabase.from("goals").delete {
                filter { eq("user_id", userId) }
            }
            supabase.from("usage_sessions").delete {
                filter { eq("user_id", userId) }
            }
            supabase.from("usage_events").delete {
                filter { eq("user_id", userId) }
            }
            supabase.from("daily_stats").delete {
                filter { eq("user_id", userId) }
            }
            supabase.from("intervention_results").delete {
                filter { eq("user_id", userId) }
            }
            supabase.from("streak_recoveries").delete {
                filter { eq("user_id", userId) }
            }
            supabase.from("user_baseline").delete {
                filter { eq("user_id", userId) }
            }
            supabase.from("settings").delete {
                filter { eq("user_id", userId) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * IMPLEMENTATION EXAMPLES
 * =======================
 *
 * Below are comprehensive examples of how to implement each sync method with Supabase.
 *
 * 1. GOALS SYNC EXAMPLE:
 * ```
 * override suspend fun syncGoals(
 *     localGoals: List<GoalEntity>,
 *     lastSyncTime: Long
 * ): SyncResult<List<GoalEntity>> {
 *     return try {
 *         val userId = getCurrentUser()?.userId
 *             ?: return SyncResult.Error(Exception("Not authenticated"), "User not authenticated")
 *
 *         // Upload pending local changes
 *         val pendingGoals = localGoals.filter { it.syncStatus == "PENDING" }
 *         if (pendingGoals.isNotEmpty()) {
 *             val supabaseGoals = pendingGoals.map { it.toSupabase(userId) }
 *             supabase.from("goals").upsert(supabaseGoals)
 *         }
 *
 *         // Download remote changes since last sync
 *         val remoteGoals = supabase.from("goals")
 *             .select()
 *             .eq("user_id", userId)
 *             .gt("last_modified", lastSyncTime)
 *             .decodeList<SupabaseGoal>()
 *
 *         // Merge local and remote with conflict resolution
 *         val mergedGoals = mergeGoals(localGoals, remoteGoals.map { it.toEntity() })
 *
 *         SyncResult.Success(mergedGoals)
 *     } catch (e: Exception) {
 *         SyncResult.Error(e, e.message ?: "Goals sync failed")
 *     }
 * }
 *
 * private fun mergeGoals(local: List<GoalEntity>, remote: List<GoalEntity>): List<GoalEntity> {
 *     val localMap = local.associateBy { it.targetApp }
 *     val remoteMap = remote.associateBy { it.targetApp }
 *
 *     return (localMap.keys + remoteMap.keys).mapNotNull { targetApp ->
 *         val localGoal = localMap[targetApp]
 *         val remoteGoal = remoteMap[targetApp]
 *
 *         when {
 *             localGoal == null -> remoteGoal
 *             remoteGoal == null -> localGoal
 *             // Conflict: Max streak wins (preserve progress)
 *             localGoal.currentStreak >= remoteGoal.currentStreak -> localGoal
 *             else -> remoteGoal
 *         }
 *     }
 * }
 * ```
 *
 * 2. AUTHENTICATION EXAMPLE (Facebook/Google):
 * ```
 * override suspend fun signIn(authCredential: AuthCredential): Result<SyncUser> {
 *     return try {
 *         val authResult = when (authCredential) {
 *             is AuthCredential.Facebook -> {
 *                 supabase.auth.signInWith(OAuthProvider.Facebook) {
 *                     accessToken = authCredential.accessToken
 *                 }
 *             }
 *             is AuthCredential.Google -> {
 *                 supabase.auth.signInWith(OAuthProvider.Google) {
 *                     idToken = authCredential.idToken
 *                 }
 *             }
 *             is AuthCredential.Email -> {
 *                 supabase.auth.signInWith(Email) {
 *                     email = authCredential.email
 *                     password = authCredential.password
 *                 }
 *             }
 *         }
 *
 *         val user = authResult.user ?: return Result.failure(Exception("No user returned"))
 *
 *         Result.success(SyncUser(
 *             userId = user.id,
 *             email = user.email,
 *             displayName = user.userMetadata?.get("full_name") as? String,
 *             photoUrl = user.userMetadata?.get("avatar_url") as? String,
 *             provider = when (authCredential) {
 *                 is AuthCredential.Facebook -> AuthProvider.FACEBOOK
 *                 is AuthCredential.Google -> AuthProvider.GOOGLE
 *                 is AuthCredential.Email -> AuthProvider.EMAIL
 *             },
 *             isAnonymous = false
 *         ))
 *     } catch (e: Exception) {
 *         Result.failure(e)
 *     }
 * }
 * ```
 *
 * 3. SETTINGS SYNC EXAMPLE:
 * ```
 * override suspend fun syncSettings(
 *     settings: Map<String, Any>,
 *     lastSyncTime: Long
 * ): SyncResult<Map<String, Any>> {
 *     return try {
 *         val userId = getCurrentUser()?.userId
 *             ?: return SyncResult.Error(Exception("Not authenticated"), "User not authenticated")
 *
 *         // Serialize settings to JSON
 *         val settingsJson = Json.encodeToString(settings)
 *
 *         // Upload to Supabase
 *         supabase.from("settings").upsert(SupabaseSettings(
 *             userId = userId,
 *             settingsJson = settingsJson,
 *             lastModified = System.currentTimeMillis()
 *         ))
 *
 *         // Download latest settings
 *         val remoteSettings = supabase.from("settings")
 *             .select()
 *             .eq("user_id", userId)
 *             .decodeSingle<SupabaseSettings>()
 *
 *         val remoteMap = Json.decodeFromString<Map<String, Any>>(remoteSettings.settingsJson)
 *
 *         // Remote wins for settings
 *         SyncResult.Success(remoteMap)
 *     } catch (e: Exception) {
 *         SyncResult.Error(e, e.message ?: "Settings sync failed")
 *     }
 * }
 * ```
 *
 * 4. REALTIME SUBSCRIPTIONS EXAMPLE:
 * ```
 * fun subscribeToGoalChanges(userId: String, onUpdate: (GoalEntity) -> Unit) {
 *     val channel = supabase.realtime.createChannel("goals-$userId") {
 *         postgresChangeFlow<SupabaseGoal>("public") {
 *             table = "goals"
 *             filter = "user_id=eq.$userId"
 *         }.collect { change ->
 *             when (change) {
 *                 is PostgresAction.Insert -> onUpdate(change.record.toEntity())
 *                 is PostgresAction.Update -> onUpdate(change.record.toEntity())
 *                 is PostgresAction.Delete -> {
 *                     // Handle deletion
 *                 }
 *             }
 *         }
 *     }
 *     channel.subscribe()
 * }
 * ```
 *
 * 5. BATCH OPERATIONS FOR PERFORMANCE:
 * ```
 * override suspend fun performFullSync(userId: String): SyncResult<Unit> {
 *     return try {
 *         // Batch upload all pending changes in parallel
 *         coroutineScope {
 *             listOf(
 *                 async { uploadPendingGoals(userId) },
 *                 async { uploadPendingSessions(userId) },
 *                 async { uploadPendingStats(userId) },
 *                 async { uploadPendingSettings(userId) }
 *             ).awaitAll()
 *         }
 *
 *         // Batch download all remote changes in parallel
 *         coroutineScope {
 *             listOf(
 *                 async { downloadGoals(userId) },
 *                 async { downloadSessions(userId) },
 *                 async { downloadStats(userId) },
 *                 async { downloadSettings(userId) }
 *             ).awaitAll()
 *         }
 *
 *         SyncResult.Success(Unit)
 *     } catch (e: Exception) {
 *         SyncResult.Error(e, e.message ?: "Full sync failed")
 *     }
 * }
 * ```
 *
 * 6. PAGINATION FOR LARGE DATASETS:
 * ```
 * private suspend fun downloadSessionsPaginated(
 *     userId: String,
 *     lastSyncTime: Long,
 *     pageSize: Int = 500
 * ): List<UsageSessionEntity> {
 *     val allSessions = mutableListOf<UsageSessionEntity>()
 *     var offset = 0
 *
 *     while (true) {
 *         val page = supabase.from("usage_sessions")
 *             .select()
 *             .eq("user_id", userId)
 *             .gt("last_modified", lastSyncTime)
 *             .order("last_modified", ascending = true)
 *             .range(offset, offset + pageSize - 1)
 *             .decodeList<SupabaseUsageSession>()
 *
 *         allSessions.addAll(page.map { it.toEntity() })
 *
 *         if (page.size < pageSize) break // Last page
 *         offset += pageSize
 *     }
 *
 *     return allSessions
 * }
 * ```
 */
