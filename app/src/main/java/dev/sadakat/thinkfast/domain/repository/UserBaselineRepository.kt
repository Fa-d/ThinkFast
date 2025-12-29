package dev.sadakat.thinkfast.domain.repository

import dev.sadakat.thinkfast.domain.model.UserBaseline
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
}
