package dev.sadakat.thinkfast.data.local

import dev.sadakat.thinkfast.domain.model.AppCategory
import dev.sadakat.thinkfast.domain.model.TrackedApp

/**
 * Curated list of popular apps users might want to track
 * These are pre-defined suggestions shown in the "Popular" tab
 */
object CuratedApps {

    /**
     * Pre-defined list of popular apps organized by category
     */
    val CURATED_LIST = listOf(
        // Social Media
        TrackedApp("com.facebook.katana", "Facebook", AppCategory.SOCIAL_MEDIA, true),
        TrackedApp("com.instagram.android", "Instagram", AppCategory.SOCIAL_MEDIA, true),
        TrackedApp("com.twitter.android", "Twitter", AppCategory.SOCIAL_MEDIA, true),
        TrackedApp("com.snapchat.android", "Snapchat", AppCategory.SOCIAL_MEDIA, true),
        TrackedApp("com.zhiliaoapp.musically", "TikTok", AppCategory.SOCIAL_MEDIA, true),
        TrackedApp("com.reddit.frontpage", "Reddit", AppCategory.SOCIAL_MEDIA, true),
        TrackedApp("com.linkedin.android", "LinkedIn", AppCategory.SOCIAL_MEDIA, true),
        TrackedApp("com.pinterest", "Pinterest", AppCategory.SOCIAL_MEDIA, true),
        TrackedApp("com.tumblr", "Tumblr", AppCategory.SOCIAL_MEDIA, true),
        TrackedApp("com.discord", "Discord", AppCategory.SOCIAL_MEDIA, true),

        // Entertainment
        TrackedApp("com.google.android.youtube", "YouTube", AppCategory.ENTERTAINMENT, true),
        TrackedApp("com.netflix.mediaclient", "Netflix", AppCategory.ENTERTAINMENT, true),
        TrackedApp("com.spotify.music", "Spotify", AppCategory.ENTERTAINMENT, true),
        TrackedApp("com.amazon.avod.thirdpartyclient", "Prime Video", AppCategory.ENTERTAINMENT, true),
        TrackedApp("com.hulu.plus", "Hulu", AppCategory.ENTERTAINMENT, true),
        TrackedApp("com.disney.disneyplus", "Disney+", AppCategory.ENTERTAINMENT, true),
        TrackedApp("com.hbo.hbonow", "HBO Max", AppCategory.ENTERTAINMENT, true),
        TrackedApp("com.twitch.android.app", "Twitch", AppCategory.ENTERTAINMENT, true),

        // Games
        TrackedApp("com.supercell.clashofclans", "Clash of Clans", AppCategory.GAMES, true),
        TrackedApp("com.king.candycrushsaga", "Candy Crush", AppCategory.GAMES, true),
        TrackedApp("com.pubg.imobile", "PUBG Mobile", AppCategory.GAMES, true),
        TrackedApp("com.epicgames.fortnite", "Fortnite", AppCategory.GAMES, true),
        TrackedApp("com.roblox.client", "Roblox", AppCategory.GAMES, true),
        TrackedApp("com.mojang.minecraftpe", "Minecraft", AppCategory.GAMES, true),

        // News
        TrackedApp("com.google.android.apps.magazines", "Google News", AppCategory.NEWS, true),
        TrackedApp("flipboard.app", "Flipboard", AppCategory.NEWS, true),
        TrackedApp("com.cnn.mobile.android.phone", "CNN", AppCategory.NEWS, true),
        TrackedApp("com.nytimes.android", "NY Times", AppCategory.NEWS, true),
    )

    /**
     * Get curated apps grouped by category
     * Useful for displaying categorized sections in the UI
     */
    fun getCuratedByCategory(): Map<AppCategory, List<TrackedApp>> {
        return CURATED_LIST.groupBy { it.category }
    }

    /**
     * Get all curated app package names
     */
    fun getAllCuratedPackageNames(): Set<String> {
        return CURATED_LIST.map { it.packageName }.toSet()
    }

    /**
     * Check if a package name is in the curated list
     */
    fun isCurated(packageName: String): Boolean {
        return CURATED_LIST.any { it.packageName == packageName }
    }

    /**
     * Find a curated app by package name
     */
    fun findCuratedApp(packageName: String): TrackedApp? {
        return CURATED_LIST.firstOrNull { it.packageName == packageName }
    }
}
