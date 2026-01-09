package dev.sadakat.thinkfaster.domain.usecase.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dev.sadakat.thinkfaster.domain.model.AppCategory
import dev.sadakat.thinkfaster.domain.model.InstalledAppInfo
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

            // Popular apps that should appear first (in order of priority)
            val popularApps = listOf(
                "com.instagram.android",         // Instagram
                "com.zhiliaoapp.musically.go",   // TikTok
                "com.zhiliaoapp.musically",      // TikTok (old)
                "com.facebook.katana",           // Facebook
                "com.twitter.android",           // Twitter/X
                "com.snapchat.android",          // Snapchat
                "com.reddit.frontpage",          // Reddit
                "com.linkedin.android",          // LinkedIn
                "com.whatsapp",                  // WhatsApp
                "com.google.android.youtube",    // YouTube
                "com.pinterest",                 // Pinterest
                "com.discord",                   // Discord
                "org.telegram.messenger",        // Telegram
                "com.tiktok.users",              // TikTok (variant)
            )

            // Get all installed apps
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            android.util.Log.d("GetInstalledAppsUseCase", "Total apps found: ${allApps.size}")

            val filteredApps = allApps
                .filter { appInfo ->
                    // Exclude ThinkFast itself
                    if (appInfo.packageName == context.packageName) {
                        return@filter false
                    }

                    // Only include apps that have a launcher intent (appear in app drawer)
                    val hasLauncher = pm.getLaunchIntentForPackage(appInfo.packageName) != null
                    if (!hasLauncher) {
                        return@filter false
                    }

                    true
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
                        android.util.Log.e("GetInstalledAppsUseCase",
                            "Error loading app ${appInfo.packageName}: ${e.message}")
                        // Skip apps we can't load info for (e.g., no icon, no label)
                        null
                    }
                }
                .sortedWith(compareBy(
                    // First: Popular apps get priority (lower index = higher priority)
                    { app ->
                        val index = popularApps.indexOf(app.packageName)
                        if (index >= 0) index else Int.MAX_VALUE
                    },
                    // Second: Sort by category (Social Media first, then Entertainment, etc.)
                    { app -> app.category.ordinal },
                    // Third: Alphabetically within same priority
                    { app -> app.appName.lowercase() }
                ))

            android.util.Log.d("GetInstalledAppsUseCase", "Filtered apps count: ${filteredApps.size}")
            filteredApps
        } catch (e: Exception) {
            android.util.Log.e("GetInstalledAppsUseCase", "Error getting apps: ${e.message}", e)
            throw e
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
