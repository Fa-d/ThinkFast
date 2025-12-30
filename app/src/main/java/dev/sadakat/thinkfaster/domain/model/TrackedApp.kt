package dev.sadakat.thinkfaster.domain.model

/**
 * Represents an app that can be tracked by ThinkFast
 * Used for both curated suggestions and user-selected apps
 */
data class TrackedApp(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val isSystemCurated: Boolean = false  // True for apps in our suggestions list
)

/**
 * Categories for organizing apps in the selection UI
 */
enum class AppCategory(val displayName: String) {
    SOCIAL_MEDIA("Social Media"),
    ENTERTAINMENT("Entertainment"),
    GAMES("Games"),
    NEWS("News"),
    PRODUCTIVITY("Productivity"),
    OTHER("Other")
}
