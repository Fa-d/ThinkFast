package dev.sadakat.thinkfast.presentation.navigation

/**
 * Sealed class representing all navigation destinations in the app
 */
sealed class Screen(val route: String) {
    /**
     * Permission request screen - onboarding for granting permissions
     */
    data object PermissionRequest : Screen("permission_request")

    /**
     * Home screen - main dashboard with quick stats and controls
     */
    data object Home : Screen("home")

    /**
     * Statistics screen - detailed analytics and trends
     */
    data object Statistics : Screen("statistics")

    /**
     * Settings screen - app configuration and preferences
     */
    data object Settings : Screen("settings")
}

/**
 * Bottom navigation items
 */
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Statistics,
    Screen.Settings
)
