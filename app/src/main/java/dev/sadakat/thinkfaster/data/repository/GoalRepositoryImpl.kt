package dev.sadakat.thinkfaster.data.repository

import dev.sadakat.thinkfaster.data.local.database.dao.GoalDao
import dev.sadakat.thinkfaster.data.mapper.toDomain
import dev.sadakat.thinkfaster.data.mapper.toEntity
import dev.sadakat.thinkfaster.domain.model.Goal
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of GoalRepository using Room database
 */
class GoalRepositoryImpl(
    private val goalDao: GoalDao
) : GoalRepository {

    override suspend fun upsertGoal(goal: Goal) {
        goalDao.upsertGoal(goal.toEntity())
    }

    override suspend fun getGoalByApp(targetApp: String): Goal? {
        return goalDao.getGoalByApp(targetApp)?.toDomain()
    }

    override fun observeGoalByApp(targetApp: String): Flow<Goal?> {
        return goalDao.observeGoalByApp(targetApp).map { it?.toDomain() }
    }

    override suspend fun getAllGoals(): List<Goal> {
        return goalDao.getAllGoals().toDomain()
    }

    override fun observeAllGoals(): Flow<List<Goal>> {
        return goalDao.observeAllGoals().map { it.toDomain() }
    }

    override suspend fun updateCurrentStreak(targetApp: String, currentStreak: Int) {
        goalDao.updateCurrentStreak(targetApp, currentStreak)
    }

    override suspend fun updateBestStreakIfNeeded(targetApp: String, currentStreak: Int) {
        val currentGoal = goalDao.getGoalByApp(targetApp)
        if (currentGoal != null && currentStreak > currentGoal.longestStreak) {
            goalDao.updateBestStreak(targetApp, currentStreak)
        }
    }

    override suspend fun resetCurrentStreak(targetApp: String) {
        goalDao.updateCurrentStreak(targetApp, 0)
    }

    override suspend fun deleteGoal(targetApp: String) {
        goalDao.deleteGoal(targetApp)
    }

    // ========== Sync Methods ==========

    override suspend fun getGoalsByUserId(userId: String): List<Goal> {
        return goalDao.getGoalsByUserId(userId).toDomain()
    }

    override suspend fun getUnsyncedGoals(userId: String): List<Goal> {
        return goalDao.getGoalsByUserAndSyncStatus(userId, "PENDING").toDomain()
    }

    override suspend fun markGoalAsSynced(targetApp: String, cloudId: String) {
        goalDao.updateSyncStatus(
            targetApp = targetApp,
            status = "SYNCED",
            cloudId = cloudId,
            lastModified = System.currentTimeMillis()
        )
    }

    override suspend fun upsertGoalFromRemote(goal: Goal, cloudId: String) {
        val entity = goal.toEntity().copy(
            syncStatus = "SYNCED",
            cloudId = cloudId,
            lastModified = System.currentTimeMillis()
        )
        goalDao.upsertGoal(entity)
    }

    override suspend fun updateGoalUserId(targetApp: String, userId: String) {
        goalDao.updateUserId(targetApp, userId)
    }
}
