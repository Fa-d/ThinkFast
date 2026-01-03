package dev.sadakat.thinkfaster.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sadakat.thinkfaster.data.local.database.entities.UserBaselineEntity
import kotlinx.coroutines.flow.Flow

/**
 * UserBaselineDao - Database access for baseline calculation
 * First-Week Retention Feature - Phase 1.3: Data Layer
 *
 * Provides CRUD operations for user baseline data
 */
@Dao
interface UserBaselineDao {

    /**
     * Insert or replace baseline (upsert)
     * Always replaces since id = 1 for single row
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBaseline(baseline: UserBaselineEntity)

    /**
     * Get baseline for user
     * @return UserBaselineEntity or null if not calculated yet
     */
    @Query("SELECT * FROM user_baseline WHERE id = 1")
    suspend fun getBaseline(): UserBaselineEntity?

    /**
     * Observe baseline changes
     * @return Flow of UserBaselineEntity (reactive)
     */
    @Query("SELECT * FROM user_baseline WHERE id = 1")
    fun observeBaseline(): Flow<UserBaselineEntity?>

    /**
     * Delete baseline (for testing or reset)
     */
    @Query("DELETE FROM user_baseline")
    suspend fun deleteBaseline()

    // ========== Sync Methods ==========

    @Query("SELECT * FROM user_baseline WHERE user_id = :userId")
    suspend fun getBaselineByUserId(userId: String): UserBaselineEntity?

    @Query("SELECT * FROM user_baseline WHERE sync_status = :status")
    suspend fun getBaselinesBySyncStatus(status: String): List<UserBaselineEntity>

    @Query("SELECT * FROM user_baseline WHERE user_id = :userId AND sync_status = :status")
    suspend fun getBaselinesByUserAndSyncStatus(userId: String, status: String): List<UserBaselineEntity>

    @Query("UPDATE user_baseline SET sync_status = :status, cloud_id = :cloudId, last_modified = :lastModified WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, status: String, cloudId: String?, lastModified: Long)

    @Query("UPDATE user_baseline SET user_id = :userId WHERE id = :id")
    suspend fun updateUserId(id: Int, userId: String)

    @Query("UPDATE user_baseline SET last_modified = :lastModified WHERE id = :id")
    suspend fun updateLastModified(id: Int, lastModified: Long)
}
