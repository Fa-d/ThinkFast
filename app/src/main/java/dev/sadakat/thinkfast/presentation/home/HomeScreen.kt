package dev.sadakat.thinkfast.presentation.home

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import dev.sadakat.thinkfast.service.UsageMonitorService
import dev.sadakat.thinkfast.util.PermissionHelper
import kotlinx.coroutines.delay

/**
 * Home screen - main dashboard with service controls and status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    var isServiceRunning by remember { mutableStateOf(isMonitorServiceRunning(context)) }
    var hasAllPermissions by remember { mutableStateOf(PermissionHelper.hasAllRequiredPermissions(context)) }

    // Periodically check service status
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000) // Check every 2 seconds
            isServiceRunning = isMonitorServiceRunning(context)
            hasAllPermissions = PermissionHelper.hasAllRequiredPermissions(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ThinkFast") },
                actions = {
                    IconButton(onClick = {
                        isServiceRunning = isMonitorServiceRunning(context)
                        hasAllPermissions = PermissionHelper.hasAllRequiredPermissions(context)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏰",
                        fontSize = 64.sp
                    )
                    Text(
                        text = "ThinkFast",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Mindful Usage Tracker",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Service status card
            item {
                ServiceStatusCard(
                    isRunning = isServiceRunning,
                    hasPermissions = hasAllPermissions
                )
            }

            // Permission warning (if missing)
            if (!hasAllPermissions) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permissions Required",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Some permissions are missing. Grant them to enable monitoring.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                navController.navigate(Screen.PermissionRequest.route)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }

            // Control buttons
            item {
                if (hasAllPermissions) {
                    if (isServiceRunning) {
                        // Stop button
                        Button(
                            onClick = {
                                stopMonitoringService(context)
                                isServiceRunning = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⏸️ Stop Monitoring",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Start button
                        Button(
                            onClick = {
                                startMonitoringService(context)
                                isServiceRunning = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Monitoring",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Info card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ℹ️ How It Works",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Full-screen reminder when opening Facebook/Instagram",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "• Alert after 10 minutes of continuous use",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "• Track your usage patterns and set daily goals",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "• Build streaks by staying under your limits",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Navigation hints
            item {
                Text(
                    text = "View Statistics to see detailed analytics and set Goals in Settings.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ServiceStatusCard(
    isRunning: Boolean,
    hasPermissions: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning && hasPermissions) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status icon
            Icon(
                imageVector = if (isRunning && hasPermissions) {
                    Icons.Default.Check
                } else {
                    Icons.Default.Close
                },
                contentDescription = null,
                tint = if (isRunning && hasPermissions) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(40.dp)
            )

            // Status text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Monitoring Status",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isRunning && hasPermissions) {
                        "Active - Monitoring usage"
                    } else if (!hasPermissions) {
                        "Inactive - Permissions needed"
                    } else {
                        "Inactive - Not monitoring"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Status indicator
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isRunning && hasPermissions) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.size(12.dp)
            ) {}
        }
    }
}

/**
 * Check if UsageMonitorService is running
 */
private fun isMonitorServiceRunning(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return activityManager.getRunningServices(Int.MAX_VALUE).any {
        it.service.className == UsageMonitorService::class.java.name
    }
}

/**
 * Start the usage monitoring service
 */
private fun startMonitoringService(context: Context) {
    try {
        val intent = Intent(context, UsageMonitorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Stop the usage monitoring service
 */
private fun stopMonitoringService(context: Context) {
    try {
        val intent = Intent(context, UsageMonitorService::class.java)
        context.stopService(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
