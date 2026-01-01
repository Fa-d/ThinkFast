package dev.sadakat.thinkfaster.presentation.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.sadakat.thinkfaster.domain.model.AppSettings
import dev.sadakat.thinkfaster.domain.model.GoalProgress
import dev.sadakat.thinkfaster.presentation.navigation.Screen
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * Settings screen - goal management and app configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: GoalViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Goals") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {


            // Success/Error messages
            item {
                uiState.successMessage?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "✓ $message",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                uiState.error?.let { error ->
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }


            // Empty state if no tracked apps (separate card for emphasis)
            if (uiState.trackedAppsCount == 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💡 Tip",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add apps to track usage and set daily time limits",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Timer alert duration setting
            item {
                TimerDurationCard(
                    currentDuration = uiState.appSettings.timerAlertMinutes,
                    onDurationChange = { viewModel.setTimerAlertDuration(it) }
                )
            }

            // Always show reminder toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🔔", fontSize = 24.sp)
                                Text(
                                    text = "Always Show Reminder",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Show reminder overlay every time you open Facebook or Instagram",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = uiState.appSettings.alwaysShowReminder,
                            onCheckedChange = { viewModel.setAlwaysShowReminder(it) }
                        )
                    }
                }
            }

            // Overlay Style toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "📐", fontSize = 24.sp)
                                    Text(
                                        text = "Overlay Style",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (uiState.appSettings.overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) {
                                        "Compact popup in center"
                                    } else {
                                        "Full-screen coverage"
                                    },
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Segmented control style toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Full-screen option
                            OutlinedButton(
                                onClick = { viewModel.setOverlayStyle(dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (uiState.appSettings.overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                    contentColor = if (uiState.appSettings.overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (uiState.appSettings.overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("Full-screen", fontWeight = FontWeight.Medium)
                            }

                            // Compact option
                            OutlinedButton(
                                onClick = { viewModel.setOverlayStyle(dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (uiState.appSettings.overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                    contentColor = if (uiState.appSettings.overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (uiState.appSettings.overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Text("Compact", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Locked Mode toggle (Phase F)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.appSettings.lockedMode) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = if (uiState.appSettings.lockedMode) {
                        BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.error
                        )
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (uiState.appSettings.lockedMode) "🔒" else "🔓",
                                    fontSize = 24.sp
                                )
                                Text(
                                    text = "Locked Mode",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.appSettings.lockedMode) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.appSettings.lockedMode) {
                                    "Maximum friction - 10 second delay with countdown. You requested this extra control."
                                } else {
                                    "Enable maximum friction for extra self-control (10s delay)"
                                },
                                fontSize = 14.sp,
                                color = if (uiState.appSettings.lockedMode) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                lineHeight = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = uiState.appSettings.lockedMode,
                            onCheckedChange = { viewModel.setLockedMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.error,
                                checkedTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }

            // Push Notification Strategy: Clickable card that opens bottom sheet
            item {
                NotificationSettingsCard(
                    appSettings = uiState.appSettings,
                    viewModel = viewModel
                )
            }

            // Friction Level selector
            item {
                FrictionLevelCard(
                    currentLevel = uiState.currentFrictionLevel,
                    selectedOverride = uiState.frictionLevelOverride,
                    onLevelSelected = { viewModel.setFrictionLevel(it) }
                )
            }

            // Snooze Settings
            item {
                SnoozeSettingsCard(
                    snoozeActive = uiState.snoozeActive,
                    remainingMinutes = uiState.snoozeRemainingMinutes,
                    viewModel = viewModel
                )
            }

            // Theme and Appearance card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("theme_appearance") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🎨", fontSize = 24.sp)
                                Text(
                                    text = "Theme & Appearance",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Customize app theme, colors, and appearance",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                lineHeight = 20.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Customize theme",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .size(32.dp)
                                .rotate(270f)
                        )
                    }
                }
            }

            // Intervention Analytics card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("analytics") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "📊", fontSize = 24.sp)
                                Text(
                                    text = "Intervention Analytics",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "View intervention effectiveness and content performance",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                lineHeight = 20.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "View analytics",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .size(32.dp)
                                .rotate(270f)
                        )
                    }
                }
            }

            // Info section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ℹ️ How it works",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Set a daily limit for each app",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Build streaks by staying under your limit",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Streaks update daily at midnight",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Track your progress in real-time",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(
    appName: String,
    appIcon: String,
    progress: GoalProgress?,
    isLoading: Boolean,
    isSaving: Boolean,
    onSetGoal: (Int) -> Unit
) {
    var sliderValue by remember {
        mutableStateOf(
            progress?.goal?.dailyLimitMinutes?.toFloat() ?: 30f
        )
    }
    val currentLimit = progress?.goal?.dailyLimitMinutes ?: 30

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = appIcon, fontSize = 32.sp)
                    Text(
                        text = appName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (progress?.goal != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = progress.goal.getStreakMessage(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = progress.goal.getLongestStreakMessage(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Current progress (if goal exists)
            if (progress != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (progress.getProgressColor()) {
                            dev.sadakat.thinkfaster.domain.model.ProgressColor.GREEN ->
                                MaterialTheme.colorScheme.primaryContainer

                            dev.sadakat.thinkfaster.domain.model.ProgressColor.YELLOW ->
                                MaterialTheme.colorScheme.tertiaryContainer

                            dev.sadakat.thinkfaster.domain.model.ProgressColor.ORANGE ->
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)

                            dev.sadakat.thinkfaster.domain.model.ProgressColor.RED ->
                                MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = progress.formatTodayUsage(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LinearProgressIndicator(
                            progress = (progress.percentageUsed / 100f).coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = progress.getStatusMessage(),
                                fontSize = 14.sp
                            )
                            Text(
                                text = progress.formatRemainingTime(),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Divider()

            // Goal setting slider
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Daily Limit: ${sliderValue.roundToInt()} minutes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 5f..180f,
                    steps = 34, // 5-minute increments
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "5 min",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "180 min",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Set goal button
            Button(
                onClick = { onSetGoal(sliderValue.roundToInt()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isSaving && sliderValue.roundToInt() != currentLimit,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (progress?.goal != null) "Update Goal" else "Set Goal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerDurationCard(
    currentDuration: Int,
    onDurationChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentDuration.toFloat()) }
    val hasChanged = sliderValue.roundToInt() != currentDuration

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "⏰", fontSize = 24.sp)
                Text(
                    text = "Timer Alert Duration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Get an alert after using the app continuously for this duration",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Current value display
            Text(
                text = "${sliderValue.roundToInt()} minutes",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            // Slider
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 1f..120f,
                steps = 118, // 1-minute increments
                modifier = Modifier.fillMaxWidth()
            )

            // Range labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1 min",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "120 min",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Apply button (only shown when value changed)
            if (hasChanged) {
                Button(
                    onClick = { onDurationChange(sliderValue.roundToInt()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Apply",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Friction Level selector card - shows summary and opens bottom sheet for selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrictionLevelCard(
    currentLevel: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel,
    selectedOverride: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel?,
    onLevelSelected: (dev.sadakat.thinkfaster.domain.intervention.FrictionLevel?) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Get display name for current selection
    val currentDisplayName = when {
        selectedOverride == null -> "Auto (Recommended)"
        else -> selectedOverride.displayName
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showBottomSheet = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⚙️", fontSize = 24.sp)
                    Text(
                        text = "Intervention Strength",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Control how much friction appears when opening apps",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current selection display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Current:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentDisplayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Show options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(32.dp)
                    .rotate(270f)
            )
        }
    }

    // Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .width(32.dp)
                            .height(4.dp),
                        shape = RoundedCornerShape(2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    ) {}
                }
            }
        ) {
            FrictionLevelBottomSheetContent(
                currentLevel = currentLevel,
                selectedOverride = selectedOverride,
                onLevelSelected = {
                    onLevelSelected(it)
                    showBottomSheet = false
                }
            )
        }
    }
}

/**
 * Bottom sheet content for friction level selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrictionLevelBottomSheetContent(
    currentLevel: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel,
    selectedOverride: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel?,
    onLevelSelected: (dev.sadakat.thinkfaster.domain.intervention.FrictionLevel?) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = screenHeight * 0.7f)
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Intervention Strength",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select the level of friction",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Auto option (null = automatic based on tenure)
        FrictionLevelOption(
            title = "Auto (Recommended)",
            description = "Gradually increases based on your app usage tenure",
            level = null,
            currentLevel = currentLevel,
            isSelected = selectedOverride == null,
            onSelect = { onLevelSelected(null) }
        )

        // Manual options for each friction level
        dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.values().forEach { level ->
            FrictionLevelOption(
                title = level.displayName,
                description = level.description,
                level = level,
                currentLevel = currentLevel,
                isSelected = selectedOverride == level,
                onSelect = { onLevelSelected(level) }
            )
        }
    }
}

/**
 * Individual friction level option row
 */
@Composable
private fun FrictionLevelOption(
    title: String,
    description: String,
    level: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel?,
    currentLevel: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    // Show "Current" badge if this is the active level
                    if (level == currentLevel && isSelected) {
                        Text(
                            text = "• Current",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Radio button indicator
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Notification settings card that opens a bottom sheet
 * Push Notification Strategy: Main entry point for notification settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsCard(
    appSettings: AppSettings,
    viewModel: GoalViewModel
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showBottomSheet = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🔔", fontSize = 24.sp)
                    Text(
                        text = "Daily Reminders",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (appSettings.motivationalNotificationsEnabled) {
                        "Enabled • ${appSettings.getMorningTimeFormatted()} & ${appSettings.getEveningTimeFormatted()}"
                    } else {
                        "Tap to configure notification times"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Open settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Bottom sheet with notification settings
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            NotificationSettingsBottomSheet(
                appSettings = appSettings,
                viewModel = viewModel,
                onDismiss = { showBottomSheet = false }
            )
        }
    }
}

/**
 * Bottom sheet content for notification settings
 */
@Composable
private fun NotificationSettingsBottomSheet(
    appSettings: AppSettings,
    viewModel: GoalViewModel,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🔔 Daily Reminders",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stay on track with daily notifications",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Enable/Disable toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Notifications",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "2-3 reminders per day max",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appSettings.motivationalNotificationsEnabled,
                    onCheckedChange = { viewModel.setMotivationalNotificationsEnabled(it) }
                )
            }
        }

        // Time pickers (only show if enabled)
        if (appSettings.motivationalNotificationsEnabled) {
            Text(
                text = "Notification Times",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            NotificationTimeRow(
                title = "Morning Intention",
                emoji = "🌅",
                description = "Start your day with intention",
                currentTime = appSettings.getMorningTimeFormatted(),
                onTimeSelected = { hour, minute ->
                    viewModel.setMorningNotificationTime(hour, minute)
                }
            )

            NotificationTimeRow(
                title = "Evening Review",
                emoji = "🌙",
                description = "Reflect on your progress",
                currentTime = appSettings.getEveningTimeFormatted(),
                onTimeSelected = { hour, minute ->
                    viewModel.setEveningNotificationTime(hour, minute)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = emoji, fontSize = 32.sp)
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currentTime,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap to change",
                    fontSize = 11.sp,
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
                context,
                { _, selectedHour, selectedMinute ->
                    onTimeSelected(selectedHour, selectedMinute)
                },
                hour,
                minute,
                false // 12-hour format
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

