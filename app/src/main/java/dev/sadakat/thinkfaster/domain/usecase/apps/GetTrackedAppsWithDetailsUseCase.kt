package dev.sadakat.thinkfaster.domain.usecase.apps

import android.content.Context
import android.content.pm.PackageManager
import dev.sadakat.thinkfaster.domain.model.AppCategory
import dev.sadakat.thinkfaster.domain.model.InstalledAppInfo
import dev.sadakat.thinkfaster.domain.repository.TrackedAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case to get details for all tracked apps
 * Handles uninstalled apps gracefully by marking them as not installed
 */
class GetTrackedAppsWithDetailsUseCase(
    private val trackedAppsRepository: TrackedAppsRepository,
    private val context: Context
) {
    suspend operator fun invoke(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val trackedPackages = trackedAppsRepository.getTrackedApps()
        val pm = context.packageManager

        trackedPackages.mapNotNull { packageName ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                InstalledAppInfo(
                    packageName = packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo),
                    category = AppCategory.OTHER, // Category not critical for tracked apps display
                    isInstalled = true
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // App was uninstalled - still show it but mark as uninstalled
                InstalledAppInfo(
                    packageName = packageName,
                    appName = packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() },
                    icon = null,
                    category = AppCategory.OTHER,
                    isInstalled = false
                )
            } catch (e: Exception) {
                // Other errors - skip this app
                null
            }
        }
    }
}
