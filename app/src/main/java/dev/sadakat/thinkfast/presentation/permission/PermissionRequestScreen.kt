package dev.sadakat.thinkfast.presentation.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import dev.sadakat.thinkfast.presentation.navigation.Screen
import dev.sadakat.thinkfast.util.PermissionHelper

/**
 * Permission request screen - onboarding flow for granting required permissions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    var permissionsState by remember { mutableStateOf(getPermissionsState(context)) }

    // Periodically refresh permissions state (check every 2 seconds)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            permissionsState = getPermissionsState(context)

            // Auto-navigate if all permissions granted
            if (permissionsState.all { it.isGranted }) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.PermissionRequest.route) { inclusive = true }
                }
                break
            }
        }
    }

    // Launcher for notification permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionsState = getPermissionsState(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Required") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 64.sp
                    )
                    Text(
                        text = "Permissions Required",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ThinkFast needs a few permissions to monitor your app usage and show helpful reminders.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            // Permission cards
            items(permissionsState) { permission ->
                PermissionCard(
                    permission = permission,
                    onRequestClick = {
                        when (permission.type) {
                            PermissionType.USAGE_STATS -> {
                                context.startActivity(PermissionHelper.getUsageStatsPermissionIntent())
                            }
                            PermissionType.OVERLAY -> {
                                context.startActivity(PermissionHelper.getOverlayPermissionIntent(context))
                            }
                            PermissionType.NOTIFICATION -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    }
                )
            }

            // Continue button (only enabled when all permissions granted)
            item {
                val allGranted = permissionsState.all { it.isGranted }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.PermissionRequest.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = allGranted,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (allGranted) "Continue to App" else "Grant All Permissions First",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!allGranted) {
                    Text(
                        text = "All permissions are required for the app to function",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    permission: PermissionState,
    onRequestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (permission.isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = permission.icon,
                        fontSize = 24.sp
                    )
                    Text(
                        text = permission.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = permission.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (permission.isGranted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Button(
                    onClick = onRequestClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Grant")
                }
            }
        }
    }
}

private data class PermissionState(
    val type: PermissionType,
    val name: String,
    val icon: String,
    val description: String,
    val isGranted: Boolean
)

private enum class PermissionType {
    USAGE_STATS,
    OVERLAY,
    NOTIFICATION
}

private fun getPermissionsState(context: android.content.Context): List<PermissionState> {
    return listOf(
        PermissionState(
            type = PermissionType.USAGE_STATS,
            name = "Usage Access",
            icon = "📊",
            description = "Required to detect when you open Facebook or Instagram and track your usage time accurately.",
            isGranted = PermissionHelper.hasUsageStatsPermission(context)
        ),
        PermissionState(
            type = PermissionType.OVERLAY,
            name = "Display Over Apps",
            icon = "🔔",
            description = "Required to show full-screen reminder overlays when you open social media apps.",
            isGranted = PermissionHelper.hasOverlayPermission(context)
        ),
        PermissionState(
            type = PermissionType.NOTIFICATION,
            name = "Notifications",
            icon = "🔕",
            description = "Required to keep the monitoring service running reliably in the background.",
            isGranted = PermissionHelper.hasNotificationPermission(context)
        )
    )
}
