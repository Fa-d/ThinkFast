package dev.sadakat.thinkfaster.domain.repository

import dev.sadakat.thinkfaster.domain.model.UserBaseline
import kotlinx.coroutines.flow.Flow

/**
 * UserBaselineRepository - Interface for baseline data operations
 * First-Week Retention Feature - Phase 1.5: Repository Interface
 *
 * Defines contract for accessing and storing user baseline
 */
interface UserBaselineRepository {
    /**
     * Save or update user baseline
     * @param baseline UserBaseline to save
     */
    suspend fun saveBaseline(baseline: UserBaseline)

    /**
     * Get current user baseline
     * @return UserBaseline or null if not calculated
     */
    suspend fun getBaseline(): UserBaseline?

    /**
     * Observe baseline changes (reactive)
     * @return Flow of UserBaseline updates
     */
    fun observeBaseline(): Flow<UserBaseline?>

    // ========== Sync Methods ==========

    /**
     * Get baseline for a specific user
     */
    suspend fun getBaselineByUserId(userId: String): UserBaseline?

    /**
     * Get unsynced baselines for a user (pending sync status)
     */
    suspend fun getUnsyncedBaselines(userId: String): List<UserBaseline>

    /**
     * Mark baseline as synced
     */
    suspend fun markBaselineAsSynced(baselineId: Int, cloudId: String)

    /**
     * Upsert baseline from remote with sync metadata
     */
    suspend fun upsertBaselineFromRemote(baseline: UserBaseline, cloudId: String)

    /**
     * Update user ID for baseline
     */
    suspend fun updateBaselineUserId(baselineId: Int, userId: String)
}
