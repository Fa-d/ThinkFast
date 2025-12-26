package dev.sadakat.thinkfast.data.repository

import dev.sadakat.thinkfast.data.local.database.dao.GoalDao
import dev.sadakat.thinkfast.data.mapper.toDomain
import dev.sadakat.thinkfast.data.mapper.toEntity
import dev.sadakat.thinkfast.domain.model.Goal
import dev.sadakat.thinkfast.domain.repository.GoalRepository
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
}
