package dev.sadakat.thinkfaster.presentation.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.sadakat.thinkfaster.data.preferences.SyncPreferences
import dev.sadakat.thinkfaster.domain.model.AppSettings
import dev.sadakat.thinkfaster.domain.model.GoalProgress
import dev.sadakat.thinkfaster.presentation.navigation.Screen
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.theme.AppColors
import dev.sadakat.thinkfaster.ui.theme.shouldUseTwoColumnLayout
import dev.sadakat.thinkfaster.BuildConfig
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * Settings screen - iOS-style grouped menu with bottom sheets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: GoalViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
    syncPreferences: SyncPreferences = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useTwoColumns = shouldUseTwoColumnLayout()

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showInterventionSheet by remember { mutableStateOf(false) }
    var showFrictionSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                })
        }) { paddingValues ->
        if (useTwoColumns) {
            // Two-column grid layout for landscape tablets
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Row with two columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Group 1: Account
                        SettingsGroupCard {
                            SettingsMenuItem(
                                icon = "👤",
                                iconBackgroundColor = Color.Transparent,
                                title = "Account",
                                onClick = {
                                    val isAuthenticated = syncPreferences.isAuthenticated()
                                    if (isAuthenticated) {
                                        navController.navigate(Screen.AccountManagement.route)
                                    } else {
                                        navController.navigate(Screen.Login.route)
                                    }
                                })
                        }
                    }

                    // Right column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Group 3: Notifications, Appearance, Intervention
                        SettingsGroupCard {
                            SettingsMenuItem(
                                icon = "🔔",
                                iconBackgroundColor = Color.Transparent,
                                title = "Notifications",
                                onClick = { showNotificationsSheet = true })
                            SettingsDivider()
                            SettingsMenuItem(
                                icon = "🌙",
                                iconBackgroundColor = Color.Transparent,
                                title = "Appearance",
                                onClick = { navController.navigate("theme_appearance") })
                            SettingsDivider()
                            SettingsMenuItem(
                                icon = "✋",
                                iconBackgroundColor = Color.Transparent,
                                title = "Intervention",
                                onClick = { showInterventionSheet = true })
                        }
                    }
                }

                // Row with two columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Group 5: Help & Support, About
                        SettingsGroupCard {
                            SettingsMenuItem(
                                icon = "❓",
                                iconBackgroundColor = Color.Transparent,
                                title = "Help & Support",
                                onClick = { /* TODO */ })
                            SettingsDivider()
                            SettingsMenuItem(
                                icon = "ℹ️",
                                iconBackgroundColor = Color.Transparent,
                                title = "About",
                                onClick = { /* TODO */ })
                        }
                    }

                    // Right column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Group 6: App Version
                        val context = LocalContext.current
                        val versionName = remember {
                            try {
                                context.packageManager.getPackageInfo(context.packageName, 0).versionName
                            } catch (e: Exception) {
                                null
                            }
                        }

                        SettingsGroupCard {
                            SettingsVersionItem(
                                icon = "📱",
                                title = "App Version",
                                version = versionName ?: "Unknown"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            // Original single-column layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(
                    top = 16.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Group 1: Account (standalone)
            item {
                SettingsGroupCard {
                    SettingsMenuItem(
                        icon = "👤",
                        iconBackgroundColor = Color.Transparent,
                        title = "Account",
                        onClick = {
                            val isAuthenticated = syncPreferences.isAuthenticated()
                            if (isAuthenticated) {
                                navController.navigate(Screen.AccountManagement.route)
                            } else {
                                navController.navigate(Screen.Login.route)
                            }
                        })
                }
            }


            // Group 3: Notifications, Appearance, Intervention Settings
            item {
                SettingsGroupCard {
                    SettingsMenuItem(
                        icon = "🔔",
                        iconBackgroundColor = Color.Transparent,
                        title = "Notifications",
                        onClick = { showNotificationsSheet = true })
                    SettingsDivider()
                    SettingsMenuItem(
                        icon = "🌙",
                        iconBackgroundColor = Color.Transparent,
                        title = "Appearance",
                        onClick = { navController.navigate("theme_appearance") })
                    SettingsDivider()
                    SettingsMenuItem(
                        icon = "✋",
                        iconBackgroundColor = Color.Transparent,
                        title = "Intervention Settings",
                        onClick = { showInterventionSheet = true })
                }
            }


            // Group 5: Help & Support, About
            item {
                SettingsGroupCard {
                    SettingsMenuItem(
                        icon = "❓",
                        iconBackgroundColor = Color.Transparent,
                        title = "Help & Support",
                        onClick = { /* TODO: Navigate to Help & Support */ })
                    SettingsDivider()
                    SettingsMenuItem(
                        icon = "ℹ️",
                        iconBackgroundColor = Color.Transparent,
                        title = "About",
                        onClick = { /* TODO: Navigate to About */ })
                }
            }

            // Group 6: App Version
            item {
                val context = LocalContext.current
                val versionName = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    } catch (e: Exception) {
                        null
                    }
                }

                SettingsGroupCard {
                    SettingsVersionItem(
                        icon = "📱",
                        title = "App Version",
                        version = versionName ?: "Unknown"
                    )
                }
            }
        }
        } // End of else block

        // Bottom Sheets
        if (showNotificationsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNotificationsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                NotificationSettingsBottomSheet(
                    appSettings = uiState.appSettings,
                    viewModel = viewModel,
                    onDismiss = { showNotificationsSheet = false })
            }
        }

        if (showInterventionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInterventionSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                InterventionSettingsBottomSheet(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDismiss = { showInterventionSheet = false })
            }
        }

        // Friction Level Bottom Sheet
        if (showFrictionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFrictionSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                FrictionLevelBottomSheet(
                    currentLevel = uiState.currentFrictionLevel,
                    selectedOverride = uiState.frictionLevelOverride,
                    onLevelSelected = {
                        viewModel.setFrictionLevel(it)
                        showFrictionSheet = false
                    })
            }
        }
    }
}

/**
 * Settings group card - rounded container for grouped settings items (iOS-style)
 */
@Composable
private fun SettingsGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val cardBackgroundColor = if (isDarkTheme) {
        Color(0xFF1C1C1E) // iOS dark mode card color
    } else {
        Color(0xFFFFFFFF) // iOS light mode card color (white)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = cardBackgroundColor,
        shadowElevation = 0.dp
    ) {
        Column {
            content()
        }
    }
}

/**
 * iOS-style divider for settings items
 */
@Composable
private fun SettingsDivider() {
    val isDarkTheme = isSystemInDarkTheme()
    val dividerColor = if (isDarkTheme) {
        Color(0xFF38383A) // iOS dark mode separator color
    } else {
        Color(0xFFC6C6C8) // iOS light mode separator color
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp), color = dividerColor, thickness = 0.5.dp
    )
}

/**
 * Settings menu item - individual row in settings card (iOS-style)
 */
@Composable
private fun SettingsMenuItem(
    icon: String,
    iconBackgroundColor: Color,
    title: String,
    titleColor: Color? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val defaultTextColor = if (isDarkTheme) Color.White else Color.Black
    val finalTitleColor = titleColor ?: defaultTextColor
    val chevronColor = if (isDarkTheme) {
        Color(0xFF3C3C43).copy(alpha = 0.6f)
    } else {
        Color(0xFF8E8E93)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Icon with circular background
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon, fontSize = 18.sp
                )
            }

            Text(
                text = title,
                fontSize = 17.sp,
                color = finalTitleColor,
                fontWeight = FontWeight.Normal
            )
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = chevronColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Bottom sheet content for notification settings
 */
@Composable
private fun NotificationSettingsBottomSheet(
    appSettings: AppSettings, viewModel: GoalViewModel, onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Text(
            text = "🔔 Notifications",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Stay on track with daily notifications",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Enable/Disable toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.button,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "2-3 reminders per day max",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appSettings.motivationalNotificationsEnabled,
                    onCheckedChange = { viewModel.setMotivationalNotificationsEnabled(it) })
            }
        }

        // Time pickers (only show if enabled)
        if (appSettings.motivationalNotificationsEnabled) {
            Text(
                text = "Notification Times",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            NotificationTimeRow(
                title = "Morning Intention",
                emoji = "🌅",
                description = "Start your day with intention",
                currentTime = appSettings.getMorningTimeFormatted(),
                onTimeSelected = { hour, minute ->
                    viewModel.setMorningNotificationTime(hour, minute)
                })

            NotificationTimeRow(
                title = "Evening Review",
                emoji = "🌙",
                description = "Reflect on your progress",
                currentTime = appSettings.getEveningTimeFormatted(),
                onTimeSelected = { hour, minute ->
                    viewModel.setEveningNotificationTime(hour, minute)
                })
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Intervention Settings Bottom Sheet - Timer, Alerts, Friction Level, Snooze
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterventionSettingsBottomSheet(
    uiState: GoalUiState, viewModel: GoalViewModel, onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableStateOf(uiState.appSettings.timerAlertMinutes.toFloat()) }
    var showFrictionSheet by remember { mutableStateOf(false) }
    var showSnoozeBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Text(
            text = "✋ Intervention Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Timer Alert Duration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.button,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏰ Timer Alert",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${sliderValue.roundToInt()} min",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..120f,
                    steps = 118,
                    modifier = Modifier.fillMaxWidth()
                )
                if (sliderValue.roundToInt() != uiState.appSettings.timerAlertMinutes) {
                    Button(
                        onClick = { viewModel.setTimerAlertDuration(sliderValue.roundToInt()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = Shapes.button
                    ) {
                        Text("Apply")
                    }
                }
            }
        }

        // Overlay Style
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.button,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = "📐 Overlay Style",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setOverlayStyle(dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (uiState.appSettings.overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                    ) {
                        Text("Full-screen")
                    }
                    OutlinedButton(
                        onClick = { viewModel.setOverlayStyle(dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (uiState.appSettings.overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                    ) {
                        Text("Compact")
                    }
                }
            }
        }
        // Friction Level
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showFrictionSheet = true },
            shape = Shapes.button,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚙️ Intervention Strength",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val currentDisplayName = when {
                        uiState.frictionLevelOverride == null -> "Auto (Recommended)"
                        else -> uiState.frictionLevelOverride.displayName
                    }

                    Spacer(Modifier.height(Spacing.xs))

                    Text(
                        text = "Current: $currentDisplayName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Snooze Settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSnoozeBottomSheet = true },
            shape = Shapes.button,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Spacing.horizontalArrangementSM
                    ) {
                        Text(text = "⏸️", fontSize = 24.sp)
                        Text(
                            text = "Snooze Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (uiState.snoozeActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (uiState.snoozeActive && uiState.snoozeRemainingMinutes > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.snoozeRemainingMinutes} min remaining",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Surface(
                    shape = Shapes.button,
                    color = if (uiState.snoozeActive) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = if (uiState.snoozeActive) "Active" else "Off",
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Debug Section - Only visible in debug builds
        if (BuildConfig.DEBUG) {
            var expanded by remember { mutableStateOf(false) }
            val availableTypes = remember { viewModel.getAvailableDebugContentTypes() }
            val currentType = uiState.appSettings.debugForceInterventionType

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.button,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔧",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Debug: Test Interventions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Text(
                        text = "Force specific intervention types for UI testing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Spacing.md))

                    // Dropdown for content type selection
                    var selectedText by remember {
                        mutableStateOf(
                            if (currentType != null) {
                                val contentSelector = dev.sadakat.thinkfaster.domain.intervention.ContentSelector()
                                contentSelector.getContentTypeDisplayName(currentType)
                            } else {
                                "Normal (Random)"
                            }
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Intervention Type") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.error,
                                unfocusedBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            // Normal option
                            DropdownMenuItem(
                                text = { Text("Normal (Random)") },
                                onClick = {
                                    viewModel.setDebugForceInterventionType(null)
                                    selectedText = "Normal (Random)"
                                    expanded = false
                                }
                            )

                            HorizontalDivider()

                            // Content type options
                            availableTypes.forEach { (typeName, displayName) ->
                                DropdownMenuItem(
                                    text = { Text(displayName) },
                                    onClick = {
                                        viewModel.setDebugForceInterventionType(typeName)
                                        selectedText = displayName
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Test Now button
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            viewModel.launchDebugOverlay(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = Shapes.button
                    ) {
                        Text("Test Now", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Friction Level Bottom Sheet
    if (showFrictionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFrictionSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            FrictionLevelBottomSheet(
                currentLevel = uiState.currentFrictionLevel,
                selectedOverride = uiState.frictionLevelOverride,
                onLevelSelected = {
                    viewModel.setFrictionLevel(it)
                    showFrictionSheet = false
                })
        }
    }
    if (showSnoozeBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSnoozeBottomSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            SnoozeBottomSheetContent(
                snoozeActive = uiState.snoozeActive,
                remainingMinutes = uiState.snoozeRemainingMinutes,
                onToggleSnooze = { enabled, duration ->
                    viewModel.toggleSnooze(enabled, duration)
                },
                onSetDuration = { duration ->
                    viewModel.setSnoozeDuration(duration)
                    showSnoozeBottomSheet = false
                })
        }
    }
}

/**
 * Friction Level Bottom Sheet Content
 */
@Composable
private fun FrictionLevelBottomSheet(
    currentLevel: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel,
    selectedOverride: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel?,
    onLevelSelected: (dev.sadakat.thinkfaster.domain.intervention.FrictionLevel?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Intervention Strength",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select the level of friction",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Auto option
        FrictionLevelOption(
            title = "Auto (Recommended)",
            description = "Gradually increases based on your app usage tenure",
            isSelected = selectedOverride == null,
            onSelect = { onLevelSelected(null) })

        // Manual options
        dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.values().forEach { level ->
            FrictionLevelOption(
                title = level.displayName,
                description = level.description,
                isSelected = selectedOverride == level,
                onSelect = { onLevelSelected(level) })
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Individual friction level option
 */
@Composable
private fun FrictionLevelOption(
    title: String, description: String, isSelected: Boolean, onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = Shapes.button, colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ), onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = isSelected, onClick = onSelect
            )
        }
    }
}

/**
 * Compact time picker row for bottom sheet
 */
@Composable
private fun NotificationTimeRow(
    title: String,
    emoji: String,
    description: String,
    currentTime: String,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showTimePicker = true },
        shape = Shapes.button,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Spacing.horizontalArrangementMD,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = emoji, fontSize = 32.sp)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap to change",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Show Android time picker dialog
    if (showTimePicker) {
        DisposableEffect(Unit) {
            val (hour, minute) = parseTime(currentTime)
            val dialog = TimePickerDialog(
                context, { _, selectedHour, selectedMinute ->
                    onTimeSelected(selectedHour, selectedMinute)
                }, hour, minute, false // 12-hour format
            )
            dialog.setOnDismissListener { showTimePicker = false }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }
}

/**
 * Parse time string in format "HH:MM AM/PM" to hour and minute
 */
private fun parseTime(timeString: String): Pair<Int, Int> {
    val parts = timeString.split(" ")
    val timeParts = parts[0].split(":")
    var hour = timeParts[0].toInt()
    val minute = timeParts[1].toInt()
    val amPm = parts[1]

    // Convert to 24-hour format
    if (amPm == "PM" && hour != 12) {
        hour += 12
    } else if (amPm == "AM" && hour == 12) {
        hour = 0
    }

    return Pair(hour, minute)
}

/**
 * Settings version item - displays app version (non-clickable)
 */
@Composable
private fun SettingsVersionItem(
    icon: String,
    title: String,
    version: String
) {
    val isDarkTheme = isSystemInDarkTheme()
    val defaultTextColor = if (isDarkTheme) Color.White else Color.Black
    val versionColor = if (isDarkTheme) {
        Color(0xFF8E8E93)
    } else {
        Color(0xFF8E8E93)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Icon with circular background
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon, fontSize = 18.sp
                )
            }

            Text(
                text = title,
                fontSize = 17.sp,
                color = defaultTextColor,
                fontWeight = FontWeight.Normal
            )
        }

        Text(
            text = version,
            fontSize = 15.sp,
            color = versionColor
        )
    }
}
