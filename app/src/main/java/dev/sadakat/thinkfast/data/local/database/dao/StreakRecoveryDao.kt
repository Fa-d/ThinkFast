package dev.sadakat.thinkfast.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.sadakat.thinkfast.data.local.database.entities.StreakRecoveryEntity
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
}
