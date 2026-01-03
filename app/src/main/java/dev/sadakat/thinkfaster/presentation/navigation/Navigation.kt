package dev.sadakat.thinkfaster.presentation.navigation

/**
 * Sealed class representing all navigation destinations in the app
 */
sealed class Screen(val route: String) {
    /**
     * Onboarding screen - first-time user onboarding with value prop and goal setup
     * @deprecated Use specific onboarding step screens instead
     */
    @Deprecated("Use OnboardingWelcome, OnboardingGoals, etc.")
    data object Onboarding : Screen("onboarding")

    /**
     * Onboarding Step 1 - Welcome and value proposition
     */
    data object OnboardingWelcome : Screen("onboarding/welcome")

    /**
     * Onboarding Step 2 - Goal setup (daily usage limit)
     */
    data object OnboardingGoals : Screen("onboarding/goals")

    /**
     * Onboarding Step 3 - Usage Stats permission primer
     */
    data object OnboardingPermissionUsage : Screen("onboarding/permission/usage")

    /**
     * Onboarding Step 4 - Display Over Apps permission primer
     */
    data object OnboardingPermissionOverlay : Screen("onboarding/permission/overlay")

    /**
     * Onboarding Step 5 - Notifications permission primer
     */
    data object OnboardingPermissionNotification : Screen("onboarding/permission/notification")

    /**
     * Onboarding Step 6 - Success celebration and activation
     */
    data object OnboardingComplete : Screen("onboarding/complete")

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

    /**
     * Login screen - social authentication (Facebook/Google)
     */
    data object Login : Screen("login")

    /**
     * Account Management screen - view account info, sign out, delete account
     */
    data object AccountManagement : Screen("account_management")
}

/**
 * Bottom navigation items
 */
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Statistics,
    Screen.Settings
)
