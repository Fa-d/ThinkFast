package dev.sadakat.thinkfaster.domain.repository

import dev.sadakat.thinkfaster.domain.model.Goal
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user goals and streaks
 */
interface GoalRepository {

    /**
     * Insert or update a goal
     */
    suspend fun upsertGoal(goal: Goal)

    /**
     * Get goal for a specific target app
     * @param targetApp the package name of the target app
     */
    suspend fun getGoalByApp(targetApp: String): Goal?

    /**
     * Observe goal for a specific target app (real-time updates)
     * @param targetApp the package name of the target app
     */
    fun observeGoalByApp(targetApp: String): Flow<Goal?>

    /**
     * Get all active goals
     */
    suspend fun getAllGoals(): List<Goal>

    /**
     * Observe all goals (real-time updates)
     */
    fun observeAllGoals(): Flow<List<Goal>>

    /**
     * Update the current streak for a goal
     * @param targetApp the package name of the target app
     * @param currentStreak the new current streak value
     */
    suspend fun updateCurrentStreak(targetApp: String, currentStreak: Int)

    /**
     * Update the best streak for a goal if the current streak is higher
     * @param targetApp the package name of the target app
     * @param currentStreak the current streak value
     */
    suspend fun updateBestStreakIfNeeded(targetApp: String, currentStreak: Int)

    /**
     * Reset the current streak to 0
     * @param targetApp the package name of the target app
     */
    suspend fun resetCurrentStreak(targetApp: String)

    /**
     * Delete a goal
     * @param targetApp the package name of the target app
     */
    suspend fun deleteGoal(targetApp: String)
}
