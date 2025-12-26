package dev.sadakat.thinkfast.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.sadakat.thinkfast.presentation.home.HomeScreen
import dev.sadakat.thinkfast.presentation.permission.PermissionRequestScreen
import dev.sadakat.thinkfast.presentation.settings.SettingsScreen
import dev.sadakat.thinkfast.presentation.stats.StatsScreen

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
        // Permission request screen - onboarding
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
    }
}
