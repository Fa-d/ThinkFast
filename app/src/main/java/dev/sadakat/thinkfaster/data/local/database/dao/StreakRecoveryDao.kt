package dev.sadakat.thinkfaster.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.sadakat.thinkfaster.data.local.database.entities.StreakRecoveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakRecoveryDao {

    @Upsert
    suspend fun upsertRecovery(recovery: StreakRecoveryEntity)

    @Query("SELECT * FROM streak_recovery WHERE targetApp = :targetApp")
    suspend fun getRecoveryByApp(targetApp: String): StreakRecoveryEntity?

    @Query("SELECT * FROM streak_recovery WHERE targetApp = :targetApp")
    fun observeRecoveryByApp(targetApp: String): Flow<StreakRecoveryEntity?>

    @Query("SELECT * FROM streak_recovery WHERE isRecoveryComplete = 0")
    suspend fun getActiveRecoveries(): List<StreakRecoveryEntity>

    @Query("SELECT * FROM streak_recovery")
    fun observeAllRecoveries(): Flow<List<StreakRecoveryEntity>>

    @Query("DELETE FROM streak_recovery WHERE targetApp = :targetApp")
    suspend fun deleteRecovery(targetApp: String)

    @Query("DELETE FROM streak_recovery WHERE isRecoveryComplete = 1 AND timestamp < :beforeTimestamp")
    suspend fun deleteCompletedRecoveriesOlderThan(beforeTimestamp: Long)

    @Query("UPDATE streak_recovery SET currentRecoveryDays = :days WHERE targetApp = :targetApp")
    suspend fun updateRecoveryDays(targetApp: String, days: Int)

    @Query("UPDATE streak_recovery SET isRecoveryComplete = 1, recoveryCompletedDate = :date WHERE targetApp = :targetApp")
    suspend fun markRecoveryComplete(targetApp: String, date: String)

    @Query("UPDATE streak_recovery SET notificationShown = 1 WHERE targetApp = :targetApp")
    suspend fun markNotificationShown(targetApp: String)

    // ========== Sync Methods ==========

    @Query("SELECT * FROM streak_recovery WHERE user_id = :userId")
    suspend fun getRecoveriesByUserId(userId: String): List<StreakRecoveryEntity>

    @Query("SELECT * FROM streak_recovery WHERE sync_status = :status")
    suspend fun getRecoveriesBySyncStatus(status: String): List<StreakRecoveryEntity>

    @Query("SELECT * FROM streak_recovery WHERE user_id = :userId AND sync_status = :status")
    suspend fun getRecoveriesByUserAndSyncStatus(userId: String, status: String): List<StreakRecoveryEntity>

    @Query("UPDATE streak_recovery SET sync_status = :status, cloud_id = :cloudId, last_modified = :lastModified WHERE targetApp = :targetApp")
    suspend fun updateSyncStatus(targetApp: String, status: String, cloudId: String?, lastModified: Long)

    @Query("UPDATE streak_recovery SET user_id = :userId WHERE targetApp = :targetApp")
    suspend fun updateUserId(targetApp: String, userId: String)

    @Query("UPDATE streak_recovery SET last_modified = :lastModified WHERE targetApp = :targetApp")
    suspend fun updateLastModified(targetApp: String, lastModified: Long)

    @Query("SELECT * FROM streak_recovery")
    suspend fun getAllRecoveries(): List<StreakRecoveryEntity>
}
