package dev.sadakat.thinkfaster.data.repository

import dev.sadakat.thinkfaster.data.local.database.dao.StreakRecoveryDao
import dev.sadakat.thinkfaster.data.local.database.entities.StreakRecoveryEntity
import dev.sadakat.thinkfaster.data.mapper.toDomain
import dev.sadakat.thinkfaster.domain.model.StreakRecovery
import dev.sadakat.thinkfaster.domain.repository.StreakRecoveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class StreakRecoveryRepositoryImpl(
    private val streakRecoveryDao: StreakRecoveryDao
) : StreakRecoveryRepository {

    override suspend fun startRecovery(targetApp: String, previousStreak: Int, startDate: String) {
        val recovery = StreakRecoveryEntity(
            targetApp = targetApp,
            previousStreak = previousStreak,
            recoveryStartDate = startDate,
            currentRecoveryDays = 0,
            isRecoveryComplete = false,
            recoveryCompletedDate = null,
            notificationShown = false,
            timestamp = System.currentTimeMillis()
        )
        streakRecoveryDao.upsertRecovery(recovery)
    }

    override suspend fun getRecoveryByApp(targetApp: String): StreakRecovery? {
        return streakRecoveryDao.getRecoveryByApp(targetApp)?.toDomain()
    }

    override fun observeRecoveryByApp(targetApp: String): Flow<StreakRecovery?> {
        return streakRecoveryDao.observeRecoveryByApp(targetApp).map { it?.toDomain() }
    }

    override suspend fun updateRecoveryProgress(targetApp: String, daysRecovered: Int) {
        streakRecoveryDao.updateRecoveryDays(targetApp, daysRecovered)
    }

    override suspend fun completeRecovery(targetApp: String, completedDate: String) {
        streakRecoveryDao.markRecoveryComplete(targetApp, completedDate)
    }

    override suspend fun deleteRecovery(targetApp: String) {
        streakRecoveryDao.deleteRecovery(targetApp)
    }

    override suspend fun getActiveRecoveries(): List<StreakRecovery> {
        return streakRecoveryDao.getActiveRecoveries().map { it.toDomain() }
    }

    override suspend fun markNotificationShown(targetApp: String) {
        streakRecoveryDao.markNotificationShown(targetApp)
    }

    override suspend fun cleanupOldRecoveries(daysOld: Int) {
        val cutoffTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysOld.toLong())
        streakRecoveryDao.deleteCompletedRecoveriesOlderThan(cutoffTimestamp)
    }

    // ========== Sync Methods ==========

    override suspend fun getRecoveriesByUserId(userId: String): List<StreakRecovery> {
        return streakRecoveryDao.getRecoveriesByUserId(userId).toDomain()
    }

    override suspend fun getUnsyncedRecoveries(userId: String): List<StreakRecovery> {
        return streakRecoveryDao.getRecoveriesByUserAndSyncStatus(userId, "PENDING").toDomain()
    }

    override suspend fun markRecoveryAsSynced(targetApp: String, cloudId: String) {
        streakRecoveryDao.updateSyncStatus(
            targetApp = targetApp,
            status = "SYNCED",
            cloudId = cloudId,
            lastModified = System.currentTimeMillis()
        )
    }

    override suspend fun upsertRecoveryFromRemote(recovery: StreakRecovery, cloudId: String) {
        val entity = StreakRecoveryEntity(
            targetApp = recovery.targetApp,
            previousStreak = recovery.previousStreak,
            recoveryStartDate = recovery.recoveryStartDate,
            currentRecoveryDays = recovery.currentRecoveryDays,
            isRecoveryComplete = recovery.isRecoveryComplete,
            recoveryCompletedDate = recovery.recoveryCompletedDate,
            notificationShown = recovery.notificationShown,
            timestamp = recovery.timestamp,
            syncStatus = "SYNCED",
            cloudId = cloudId,
            lastModified = System.currentTimeMillis(),
            userId = null  // Will be set by caller
        )
        streakRecoveryDao.upsertRecovery(entity)
    }

    override suspend fun updateRecoveryUserId(targetApp: String, userId: String) {
        streakRecoveryDao.updateUserId(targetApp, userId)
    }
}
