package dev.sadakat.thinkfaster

import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationRailItem as MaterialNavigationRailItem
import androidx.compose.material3.NavigationRail
import dev.sadakat.thinkfaster.ui.theme.isLandscape
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.sadakat.thinkfaster.analytics.AnalyticsManager
import dev.sadakat.thinkfaster.presentation.navigation.NavGraph
import dev.sadakat.thinkfaster.presentation.navigation.Screen
import dev.sadakat.thinkfaster.presentation.widget.refreshWidgetData
import dev.sadakat.thinkfaster.ui.theme.ThinkFasterThemeWithMode
import dev.sadakat.thinkfaster.util.PermissionHelper
import dev.sadakat.thinkfaster.util.ThemePreferences
import org.koin.compose.koinInject

/**
 * Data class for bottom navigation items
 * 2025 UX Enhancement: Supports outlined/filled icon states
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector,
    val contentDescription: String
)

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

            ThinkFasterThemeWithMode(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                amoledDark = amoledDark
            ) {
                MainScreen()
            }
        }

        // Refresh widget data when app opens
        refreshWidgetData(this)
    }

    override fun onResume() {
        super.onResume()
        // Refresh widget when returning to the app
        refreshWidgetData(this)
    }
}

/**
 * Check if onboarding has been completed
 */
private fun isOnboardingCompleted(context: android.content.Context): Boolean {
    val prefs = context.getSharedPreferences("think_fast_onboarding", android.content.Context.MODE_PRIVATE)
    return prefs.getBoolean("onboarding_completed", false)
}

/**
 * Calculate days since app was first installed
 * Used for analytics events to track user journey
 */
private fun getDaysSinceInstall(context: android.content.Context): Int {
    val prefs = context.getSharedPreferences("think_fast_onboarding", android.content.Context.MODE_PRIVATE)
    val firstInstallTime = prefs.getLong("first_install_time", 0L)

    // If not set, use the app's install time from package manager
    val installTime = if (firstInstallTime > 0) {
        firstInstallTime
    } else {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val installTimeFromPm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.firstInstallTime
            } else {
                @Suppress("DEPRECATION")
                packageInfo.firstInstallTime
            }
            // Store it for future use
            prefs.edit().putLong("first_install_time", installTimeFromPm).apply()
            installTimeFromPm
        } catch (e: Exception) {
            // Fallback to current time if we can't get install time
            System.currentTimeMillis()
        }
    }

    val daysSinceInstall = (System.currentTimeMillis() - installTime) / (1000 * 60 * 60 * 24)
    return daysSinceInstall.toInt().coerceAtLeast(0)
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val analyticsManager: AnalyticsManager = koinInject()
    val landscapeMode = isLandscape()

    // Determine start destination based on onboarding completion and permissions
    val startDestination = if (!isOnboardingCompleted(context)) {
        // First time user - show new 6-step onboarding flow
        Screen.OnboardingWelcome.route
    } else if (!PermissionHelper.hasAllRequiredPermissions(context)) {
        // Onboarding done, but need permissions - show permission screen
        Screen.PermissionRequest.route
    } else {
        // All setup complete - go to home
        // Track user_ready event when user enters with all permissions (only tracked once)
        LaunchedEffect(Unit) {
            val daysSinceInstall = getDaysSinceInstall(context)
            analyticsManager.trackUserReady(daysSinceInstall)
        }
        Screen.Home.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide navigation on onboarding, permission request, analytics, theme appearance, login, manage apps, and account management screens
    val showNavigation = currentRoute != Screen.Onboarding.route &&
                        currentRoute != Screen.OnboardingWelcome.route &&
                        currentRoute != Screen.OnboardingGoals.route &&
                        currentRoute != Screen.OnboardingPermissionUsage.route &&
                        currentRoute != Screen.OnboardingPermissionOverlay.route &&
                        currentRoute != Screen.OnboardingPermissionNotification.route &&
                        currentRoute != Screen.OnboardingComplete.route &&
                        currentRoute != Screen.PermissionRequest.route &&
                        currentRoute != Screen.Analytics.route &&
                        currentRoute != Screen.ThemeAppearance.route &&
                        currentRoute != Screen.Login.route &&
                        currentRoute != Screen.ManageApps.route &&
                        currentRoute != Screen.AccountManagement.route

    if (landscapeMode && showNavigation) {
        // Landscape: Navigation rail on the side
        Row(modifier = Modifier.fillMaxSize()) {
            EnhancedNavigationRail(
                navController = navController,
                currentDestination = navBackStackEntry?.destination
            )
            Box(modifier = Modifier.weight(1f)) {
                NavGraph(
                    navController = navController,
                    startDestination = startDestination,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = 16.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        // Portrait: Bottom navigation bar
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showNavigation) {
                    EnhancedNavigationBar(
                        navController = navController,
                        currentDestination = navBackStackEntry?.destination
                    )
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
}

/**
 * Enhanced bottom navigation bar with consistent project design
 * Uses standard design tokens: shapes, spacing, and colors from theme
 */
@Composable
fun EnhancedNavigationBar(
    navController: androidx.navigation.NavController,
    currentDestination: androidx.navigation.NavDestination?
) {
    val view = LocalView.current

    // Define navigation items with outlined/filled variants
    val navigationItems = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            label = "Home",
            iconSelected = Icons.Filled.Home,
            iconUnselected = Icons.Outlined.Home,
            contentDescription = "Home"
        ),
        BottomNavItem(
            route = Screen.Statistics.route,
            label = "Statistics",
            iconSelected = Icons.Filled.BarChart,
            iconUnselected = Icons.Outlined.BarChart,
            contentDescription = "Statistics"
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            label = "Settings",
            iconSelected = Icons.Filled.Settings,
            iconUnselected = Icons.Outlined.Settings,
            contentDescription = "Settings"
        )
    )

    // Navigation container following project card design patterns
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(2.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationItems.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any {
                    it.route == item.route
                } == true

                NavigationBarItem(
                    icon = if (isSelected) item.iconSelected else item.iconUnselected,
                    label = item.label,
                    selected = isSelected,
                    onClick = {
                        // Haptic feedback on selection
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }

                        navController.navigate(item.route) {
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
}

/**
 * Navigation bar item following project card design patterns
 * Selected items are more pill-shaped (24dp) for emphasis
 * Unselected items use standard 12dp corner radius
 */
@Composable
fun NavigationBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Animated corner radius - selected items are rounder
    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 24.dp else 12.dp,
        animationSpec = tween(300),
        label = "corner_radius"
    )

    // Animated background color
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "background_color"
    )

    // Animated content color
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300),
        label = "content_color"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

/**
 * Enhanced navigation rail for landscape mode
 * Shows navigation items vertically on the side with consistent project design
 */
@Composable
fun EnhancedNavigationRail(
    navController: androidx.navigation.NavController,
    currentDestination: androidx.navigation.NavDestination?
) {
    val view = LocalView.current

    // Define navigation items with outlined/filled variants
    val navigationItems = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            label = "Home",
            iconSelected = Icons.Filled.Home,
            iconUnselected = Icons.Outlined.Home,
            contentDescription = "Home"
        ),
        BottomNavItem(
            route = Screen.Statistics.route,
            label = "Statistics",
            iconSelected = Icons.Filled.BarChart,
            iconUnselected = Icons.Outlined.BarChart,
            contentDescription = "Statistics"
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            label = "Settings",
            iconSelected = Icons.Filled.Settings,
            iconUnselected = Icons.Outlined.Settings,
            contentDescription = "Settings"
        )
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            navigationItems.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any {
                it.route == item.route
            } == true

            MaterialNavigationRailItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.iconSelected else item.iconUnselected,
                        contentDescription = item.contentDescription,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = isSelected,
                onClick = {
                    // Haptic feedback on selection
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }

                    navController.navigate(item.route) {
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
}
