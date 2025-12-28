package dev.sadakat.thinkfast.domain.usecase.apps

import android.content.Context
import android.content.pm.PackageManager
import dev.sadakat.thinkfast.domain.repository.TrackedAppsRepository

/**
 * Use case to add an app to the tracked list
 * Validates that the app is installed before adding
 */
class AddTrackedAppUseCase(
    private val trackedAppsRepository: TrackedAppsRepository,
    private val context: Context
) {
    suspend operator fun invoke(packageName: String): Result<Unit> {
        // Verify app is actually installed
        val isInstalled = try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        if (!isInstalled) {
            return Result.failure(Exception("App is not installed on this device"))
        }

        return trackedAppsRepository.addTrackedApp(packageName)
    }
}
