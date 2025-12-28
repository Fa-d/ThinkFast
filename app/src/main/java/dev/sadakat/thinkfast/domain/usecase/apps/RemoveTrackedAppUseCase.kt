package dev.sadakat.thinkfast.domain.usecase.apps

import dev.sadakat.thinkfast.domain.repository.GoalRepository
import dev.sadakat.thinkfast.domain.repository.TrackedAppsRepository

/**
 * Use case to remove an app from the tracked list
 * Also deletes the associated goal if it exists
 */
class RemoveTrackedAppUseCase(
    private val trackedAppsRepository: TrackedAppsRepository,
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(packageName: String): Result<Unit> {
        // Remove from tracked apps
        val result = trackedAppsRepository.removeTrackedApp(packageName)

        // Also delete associated goal if exists
        if (result.isSuccess) {
            try {
                goalRepository.deleteGoal(packageName)
            } catch (e: Exception) {
                // Ignore if goal doesn't exist
            }
        }

        return result
    }
}
