package dev.sadakat.thinkfaster.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.sadakat.thinkfaster.presentation.analytics.AnalyticsScreen
import dev.sadakat.thinkfaster.presentation.home.HomeScreen
import dev.sadakat.thinkfaster.presentation.manageapps.ManageAppsScreen
import dev.sadakat.thinkfaster.presentation.onboarding.OnboardingScreen
import dev.sadakat.thinkfaster.presentation.onboarding.OnboardingWelcomeScreen
import dev.sadakat.thinkfaster.presentation.onboarding.OnboardingGoalsScreen
import dev.sadakat.thinkfaster.presentation.onboarding.OnboardingPermissionUsageScreen
import dev.sadakat.thinkfaster.presentation.onboarding.OnboardingPermissionOverlayScreen
import dev.sadakat.thinkfaster.presentation.onboarding.OnboardingPermissionNotificationScreen
import dev.sadakat.thinkfaster.presentation.onboarding.OnboardingCompleteScreen
import dev.sadakat.thinkfaster.presentation.permission.PermissionRequestScreen
import dev.sadakat.thinkfaster.presentation.settings.SettingsScreen
import dev.sadakat.thinkfaster.presentation.stats.StatsScreen
import dev.sadakat.thinkfaster.presentation.themeappearance.ThemeAppearanceScreen

/**
 * Main navigation graph for the app
 * Defines all navigation routes and their corresponding screens
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.PermissionRequest.route,
    contentPadding: PaddingValues = PaddingValues()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding screen - first-time user experience (DEPRECATED - use new 6-step flow)
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                navController = navController,
                contentPadding = contentPadding
            )
        }

        // New 6-step onboarding flow
        composable(route = Screen.OnboardingWelcome.route) {
            OnboardingWelcomeScreen(
                navController = navController,
                contentPadding = contentPadding
            )
        }

        composable(route = Screen.OnboardingGoals.route) {
            OnboardingGoalsScreen(
                navController = navController,
                contentPadding = contentPadding
            )
        }

        composable(route = Screen.OnboardingPermissionUsage.route) {
            OnboardingPermissionUsageScreen(
                navController = navController,
                contentPadding = contentPadding
            )
        }

        composable(route = Screen.OnboardingPermissionOverlay.route) {
            OnboardingPermissionOverlayScreen(
                navController = navController,
                contentPadding = contentPadding
            )
        }

        composable(route = Screen.OnboardingPermissionNotification.route) {
            OnboardingPermissionNotificationScreen(
                navController = navController,
                contentPadding = contentPadding
            )
        }

        composable(route = Screen.OnboardingComplete.route) {
            OnboardingCompleteScreen(
                navController = navController,
                contentPadding = contentPadding
            )
        }

        // Permission request screen - for re-granting permissions
        composable(route = Screen.PermissionRequest.route) {
            PermissionRequestScreen(
                navController = navController,
                contentPadding = contentPadding
            )
        }

        // Home screen - main dashboard
        composable(route = Screen.Home.route) {
            HomeScreen(
                navController = navController,
                contentPadding = contentPadding
            )
        }

        // Statistics screen - detailed analytics
        composable(route = Screen.Statistics.route) {
            StatsScreen(
                navController = navController,
                contentPadding = contentPadding
            )
        }

        // Settings screen - app configuration
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                contentPadding = contentPadding
            )
        }

        // Analytics screen - intervention effectiveness (debug)
        composable(route = Screen.Analytics.route) {
            AnalyticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Theme and Appearance screen - theme customization (no bottom nav)
        composable(route = Screen.ThemeAppearance.route) {
            ThemeAppearanceScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Manage Apps screen - select and manage tracked apps (no bottom nav)
        composable(route = Screen.ManageApps.route) {
            ManageAppsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
