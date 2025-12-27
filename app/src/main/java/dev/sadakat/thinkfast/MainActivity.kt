package dev.sadakat.thinkfast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.sadakat.thinkfast.presentation.navigation.NavGraph
import dev.sadakat.thinkfast.presentation.navigation.Screen
import dev.sadakat.thinkfast.ui.theme.ThinkFastTheme
import dev.sadakat.thinkfast.ui.theme.ThinkFastThemeWithMode
import dev.sadakat.thinkfast.util.PermissionHelper
import dev.sadakat.thinkfast.util.ThemePreferences

/**
 * Main activity for ThinkFast app
 * Contains bottom navigation and hosts all screens
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode = ThemePreferences.getThemeMode(this)
            val dynamicColor = ThemePreferences.getDynamicColor(this)
            val amoledDark = ThemePreferences.getAmoledDark(this)

            ThinkFastThemeWithMode(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                amoledDark = amoledDark
            ) {
                MainScreen()
            }
        }
    }
}

/**
 * Check if onboarding has been completed
 */
private fun isOnboardingCompleted(context: android.content.Context): Boolean {
    val prefs = context.getSharedPreferences("think_fast_onboarding", android.content.Context.MODE_PRIVATE)
    return prefs.getBoolean("onboarding_completed", false)
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Determine start destination based on onboarding completion and permissions
    val startDestination = if (!isOnboardingCompleted(context)) {
        // First time user - show onboarding
        Screen.Onboarding.route
    } else if (PermissionHelper.hasAllRequiredPermissions(context)) {
        // Onboarding done, permissions granted - go to home
        Screen.Home.route
    } else {
        // Onboarding done, but need permissions
        Screen.PermissionRequest.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom navigation on onboarding and permission request screens
    val showBottomBar = currentRoute != Screen.Onboarding.route &&
                        currentRoute != Screen.PermissionRequest.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination

                    // Home
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    // Statistics
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Star, contentDescription = "Statistics") },
                        label = { Text("Statistics") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Statistics.route } == true,
                        onClick = {
                            navController.navigate(Screen.Statistics.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    // Settings
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Settings.route } == true,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            startDestination = startDestination,
            contentPadding = innerPadding
        )
    }
}
