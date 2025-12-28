package dev.sadakat.thinkfast.presentation.navigation

/**
 * Sealed class representing all navigation destinations in the app
 */
sealed class Screen(val route: String) {
    /**
     * Onboarding screen - first-time user onboarding with value prop and goal setup
     */
    data object Onboarding : Screen("onboarding")

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

    /**
     * Analytics screen - intervention effectiveness debug/analytics
     */
    data object Analytics : Screen("analytics")

    /**
     * Theme and Appearance screen - theme customization
     */
    data object ThemeAppearance : Screen("theme_appearance")

    /**
     * Manage Apps screen - select and manage tracked apps with goals
     */
    data object ManageApps : Screen("manage_apps")
}

/**
 * Bottom navigation items
 */
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Statistics,
    Screen.Settings
)
