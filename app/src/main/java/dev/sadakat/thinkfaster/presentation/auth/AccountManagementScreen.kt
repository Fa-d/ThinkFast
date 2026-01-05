package dev.sadakat.thinkfaster.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.ui.theme.getMaxContentWidth
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * AccountManagementScreen - User account and sync management
 * Phase 5: UI Integration
 * 
 * Shows account info, sync status, and provides logout/delete options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    onNavigateBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: AccountManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account & Sync") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            val maxWidth = getMaxContentWidth()
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            // Account Info Card
            AccountInfoCard(uiState)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sync Status Card
            SyncStatusCard(
                uiState = uiState,
                onManualSync = { viewModel.triggerManualSync() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Settings Card
            SyncSettingsCard(
                uiState = uiState,
                onAutoSyncToggle = { viewModel.toggleAutoSync() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sign Out Button
            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Sign Out", fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Delete Account Button
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Delete Account Data", fontSize = 16.sp)
            }
            }
        }
    }

    // Sign out confirmation dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out?") },
            text = { Text("Your local data will remain on this device, but it won't sync anymore.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                        onSignedOut()
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete account confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account Data?") },
            text = { Text("This will permanently delete all your synced data from the cloud. Local data will remain.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccountData()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AccountInfoCard(uiState: AccountManagementUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Account Info",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Show email as primary identifier if available, otherwise show user ID
            uiState.email?.let { InfoRow("Email", it) } ?: InfoRow("User ID", uiState.userId)
            InfoRow("Provider", uiState.provider)
        }
    }
}

@Composable
private fun SyncStatusCard(
    uiState: AccountManagementUiState,
    onManualSync: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Sync Status",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                val statusEmoji = when (uiState.syncStatus) {
                    "SUCCESS" -> "✓"
                    "ERROR" -> "❌"
                    "SYNCING" -> "⏳"
                    else -> "○"
                }
                val statusText = when (uiState.syncStatus) {
                    "SUCCESS" -> "Synced"
                    "ERROR" -> "Error"
                    "SYNCING" -> "Syncing..."
                    else -> "Idle"
                }
                
                Text(
                    text = statusEmoji,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(text = statusText, fontSize = 16.sp)
            }
            
            InfoRow("Last Sync", uiState.lastSyncTime)
            
            if (uiState.pendingChanges > 0) {
                InfoRow("Pending Changes", "${uiState.pendingChanges} items")
            }
            
            uiState.syncError?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Manual sync button
            Button(
                onClick = onManualSync,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.syncStatus != "SYNCING",
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Sync Now")
            }
        }
    }
}

@Composable
private fun SyncSettingsCard(
    uiState: AccountManagementUiState,
    onAutoSyncToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Sync Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Sync",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Sync every 6 hours on WiFi",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.autoSyncEnabled,
                    onCheckedChange = { onAutoSyncToggle() }
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
