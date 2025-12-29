package dev.sadakat.thinkfast

import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
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
    } else if (!PermissionHelper.hasAllRequiredPermissions(context)) {
        // Onboarding done, but need permissions
        Screen.PermissionRequest.route
    } else {
        // All setup complete - go to home
        Screen.Home.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom navigation on onboarding, permission request, analytics, and theme appearance screens
    val showBottomBar = currentRoute != Screen.Onboarding.route &&
                        currentRoute != Screen.PermissionRequest.route &&
                        currentRoute != Screen.Analytics.route &&
                        currentRoute != Screen.ThemeAppearance.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
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

/**
 * Enhanced bottom navigation bar with 2025 UX best practices
 * Features:
 * - Floating design with rounded corners
 * - Outlined/filled icon states (Material 3 Expressive)
 * - Smooth animations and transitions
 * - Haptic feedback on selection
 * - Scale animation on tap
 * - Centered items with compact spacing
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
            iconSelected = Icons.Filled.Star,
            iconUnselected = Icons.Outlined.Star,
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

    // Floating container with centered navigation bar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .width(280.dp)
                .shadow(8.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationItems.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any {
                    it.route == item.route
                } == true

                CustomNavigationBarItem(
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
 * Custom navigation bar item with background covering both icon and label
 * Maintains proper radius ratio: Container (28dp) -> Item (20dp)
 */
@Composable
fun CustomNavigationBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Animated scale for tap feedback
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.95f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "item_scale"
    )

    // Animated background color
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
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
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .scale(scale),
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
