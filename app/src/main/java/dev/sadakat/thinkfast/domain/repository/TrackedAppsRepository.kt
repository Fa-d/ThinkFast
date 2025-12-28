package dev.sadakat.thinkfast.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing the list of apps that user wants to track
 * Stores app package names and provides reactive updates
 */
interface TrackedAppsRepository {

    /**
     * Get all tracked app package names
     */
    suspend fun getTrackedApps(): List<String>

    /**
     * Add an app to the tracked list
     * Returns failure if limit reached or app already tracked
     */
    suspend fun addTrackedApp(packageName: String): Result<Unit>

    /**
     * Remove an app from the tracked list
     * Returns failure if app not found
     */
    suspend fun removeTrackedApp(packageName: String): Result<Unit>

    /**
     * Check if the maximum limit of tracked apps has been reached
     */
    suspend fun isLimitReached(): Boolean

    /**
     * Get the current number of tracked apps
     */
    suspend fun getTrackedAppCount(): Int

    /**
     * Observe tracked apps for reactive UI updates
     */
    fun observeTrackedApps(): Flow<List<String>>

    /**
     * Check if a specific app is being tracked
     */
    suspend fun isAppTracked(packageName: String): Boolean
}
