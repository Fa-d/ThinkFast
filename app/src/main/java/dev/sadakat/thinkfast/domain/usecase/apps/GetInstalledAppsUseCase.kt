package dev.sadakat.thinkfast.domain.usecase.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dev.sadakat.thinkfast.domain.model.AppCategory
import dev.sadakat.thinkfast.domain.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case to get all installed apps on the device
 * Filters out system apps and ThinkFast itself
 */
class GetInstalledAppsUseCase(
    private val context: Context
) {
    suspend operator fun invoke(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager

            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    // Filter: Include non-system apps OR apps updated from system (like pre-installed apps user can disable)
                    // Exclude ThinkFast itself
                    val isSystemApp = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystemApp = (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    val isThinkFast = it.packageName == context.packageName

                    // Keep: non-system apps OR updated system apps (user-manageable pre-installed apps)
                    // Skip: pure system apps that weren't updated, and ThinkFast itself
                    (!isSystemApp || isUpdatedSystemApp) && !isThinkFast
                }
                .mapNotNull { appInfo ->
                    try {
                        InstalledAppInfo(
                            packageName = appInfo.packageName,
                            appName = pm.getApplicationLabel(appInfo).toString(),
                            icon = pm.getApplicationIcon(appInfo),
                            category = categorizeApp(appInfo.packageName),
                            isInstalled = true
                        )
                    } catch (e: Exception) {
                        // Skip apps we can't load info for
                        null
                    }
                }
                .sortedBy { it.appName.lowercase() }
        } catch (e: SecurityException) {
            // Permission denied - return empty list
            emptyList()
        } catch (e: Exception) {
            // Other errors - return empty list
            emptyList()
        }
    }

    /**
     * Categorize app based on package name heuristics
     */
    private fun categorizeApp(packageName: String): AppCategory {
        val lowerPackage = packageName.lowercase()

        return when {
            lowerPackage.contains("facebook") ||
            lowerPackage.contains("instagram") ||
            lowerPackage.contains("twitter") ||
            lowerPackage.contains("snapchat") ||
            lowerPackage.contains("tiktok") ||
            lowerPackage.contains("musically") ||
            lowerPackage.contains("linkedin") ||
            lowerPackage.contains("reddit") ||
            lowerPackage.contains("pinterest") ||
            lowerPackage.contains("tumblr") ||
            lowerPackage.contains("discord") ||
            lowerPackage.contains("whatsapp") ||
            lowerPackage.contains("telegram") ||
            lowerPackage.contains("messenger") -> AppCategory.SOCIAL_MEDIA

            lowerPackage.contains("youtube") ||
            lowerPackage.contains("netflix") ||
            lowerPackage.contains("spotify") ||
            lowerPackage.contains("amazon.avod") ||
            lowerPackage.contains("hulu") ||
            lowerPackage.contains("disney") ||
            lowerPackage.contains("hbo") ||
            lowerPackage.contains("twitch") ||
            lowerPackage.contains("music") ||
            lowerPackage.contains("video") -> AppCategory.ENTERTAINMENT

            lowerPackage.contains("game") ||
            lowerPackage.contains("supercell") ||
            lowerPackage.contains("king.candy") ||
            lowerPackage.contains("pubg") ||
            lowerPackage.contains("fortnite") ||
            lowerPackage.contains("roblox") ||
            lowerPackage.contains("minecraft") -> AppCategory.GAMES

            lowerPackage.contains("news") ||
            lowerPackage.contains("flipboard") ||
            lowerPackage.contains("cnn") ||
            lowerPackage.contains("nytimes") ||
            lowerPackage.contains("feedly") -> AppCategory.NEWS

            lowerPackage.contains("office") ||
            lowerPackage.contains("docs") ||
            lowerPackage.contains("sheets") ||
            lowerPackage.contains("notion") ||
            lowerPackage.contains("evernote") ||
            lowerPackage.contains("trello") -> AppCategory.PRODUCTIVITY

            else -> AppCategory.OTHER
        }
    }
}
