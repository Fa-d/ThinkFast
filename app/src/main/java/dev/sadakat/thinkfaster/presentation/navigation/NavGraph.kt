package dev.sadakat.thinkfaster.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.sadakat.thinkfaster.analytics.FirebaseAnalyticsReporter
import dev.sadakat.thinkfaster.presentation.analytics.AnalyticsScreen
import dev.sadakat.thinkfaster.presentation.auth.AccountManagementScreen
import dev.sadakat.thinkfaster.presentation.auth.LoginScreen
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
import org.koin.compose.koinInject

/**
 * Main navigation graph for the app
 * Defines all navigation routes and their corresponding screens
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.PermissionRequest.route,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    analyticsReporter: FirebaseAnalyticsReporter = koinInject()
) {
    // Track previous destination to avoid duplicate screen_view events
    val previousDestination = rememberSaveable { mutableStateOf<String?>(null) }

    // Skip analytics tracking in preview mode
    val isPreviewMode = LocalInspectionMode.current

    // Log screen view events on destination change
    if (!isPreviewMode) {
        DisposableEffect(navController) {
            val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
                val route = destination.route
                val screenName = getScreenName(route)
                val screenClass = getScreenClass(route)

                if (route != previousDestination.value) {
                    analyticsReporter.logScreenView(screenName, screenClass)
                    previousDestination.value = route
                }
            }
            navController.addOnDestinationChangedListener(listener)

            onDispose {
                navController.removeOnDestinationChangedListener(listener)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
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

        // Login screen - social authentication (no bottom nav)
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onLoginSuccess = {
                    // Navigate to Settings after successful login
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Account Management screen - account info, sign out, delete (no bottom nav)
        composable(route = Screen.AccountManagement.route) {
            AccountManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignedOut = {
                    // Navigate to Settings after sign out
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.AccountManagement.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Maps a route to a human-readable screen name for analytics
 */
private fun getScreenName(route: String?): String {
    return when (route) {
        Screen.Onboarding.route -> "Onboarding"
        Screen.OnboardingWelcome.route -> "OnboardingWelcome"
        Screen.OnboardingGoals.route -> "OnboardingGoals"
        Screen.OnboardingPermissionUsage.route -> "OnboardingPermissionUsage"
        Screen.OnboardingPermissionOverlay.route -> "OnboardingPermissionOverlay"
        Screen.OnboardingPermissionNotification.route -> "OnboardingPermissionNotification"
        Screen.OnboardingComplete.route -> "OnboardingComplete"
        Screen.PermissionRequest.route -> "PermissionRequest"
        Screen.Home.route -> "Home"
        Screen.Statistics.route -> "Statistics"
        Screen.Settings.route -> "Settings"
        Screen.Analytics.route -> "Analytics"
        Screen.ThemeAppearance.route -> "ThemeAppearance"
        Screen.ManageApps.route -> "ManageApps"
        Screen.Login.route -> "Login"
        Screen.AccountManagement.route -> "AccountManagement"
        else -> route?.substringBeforeLast("/")?.substringAfterLast("/") ?: "Unknown"
    }
}

/**
 * Maps a route to its screen class name for analytics
 */
private fun getScreenClass(route: String?): String {
    return when (route) {
        Screen.Onboarding.route -> "OnboardingScreen"
        Screen.OnboardingWelcome.route -> "OnboardingWelcomeScreen"
        Screen.OnboardingGoals.route -> "OnboardingGoalsScreen"
        Screen.OnboardingPermissionUsage.route -> "OnboardingPermissionUsageScreen"
        Screen.OnboardingPermissionOverlay.route -> "OnboardingPermissionOverlayScreen"
        Screen.OnboardingPermissionNotification.route -> "OnboardingPermissionNotificationScreen"
        Screen.OnboardingComplete.route -> "OnboardingCompleteScreen"
        Screen.PermissionRequest.route -> "PermissionRequestScreen"
        Screen.Home.route -> "HomeScreen"
        Screen.Statistics.route -> "StatsScreen"
        Screen.Settings.route -> "SettingsScreen"
        Screen.Analytics.route -> "AnalyticsScreen"
        Screen.ThemeAppearance.route -> "ThemeAppearanceScreen"
        Screen.ManageApps.route -> "ManageAppsScreen"
        else -> route?.substringBeforeLast("/")?.substringAfterLast("/") ?: "UnknownScreen"
    }
}
