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
import dev.sadakat.thinkfaster.data.preferences.SyncPreferences
import dev.sadakat.thinkfaster.domain.model.AppSettings
import dev.sadakat.thinkfaster.domain.model.GoalProgress
import dev.sadakat.thinkfaster.presentation.navigation.Screen
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.theme.AppColors
import org.koin.compose.koinInject
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
            TopAppBar(title = {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineLarge
                )
            }, actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            })
        }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.md,
                bottom = contentPadding.calculateBottomPadding() + Spacing.md
            ),
            verticalArrangement = Spacing.verticalArrangementSM
        ) {


            // Success/Error messages
            item {
                uiState.successMessage?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.button,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "✓ $message",
                            modifier = Modifier.padding(Spacing.md),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.button,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
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
                        shape = Shapes.card,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Spacing.verticalArrangementSM
                        ) {
                            Text(
                                text = "💡 Tip",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add apps to track usage and set daily time limits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ========== ACCOUNT SECTION ==========
            item {
                SectionHeader(title = "Account & Data")
            }
            item {
                AccountAndDataSection(
                    navController = navController
                )
            }

            // ========== SECTION 1: TIMER & ALERTS ==========
            item {
                SectionHeader(title = "Timer & Alerts")
            }
            item {
                TimerAndAlertsSection(
                    timerDuration = uiState.appSettings.timerAlertMinutes,
                    onTimerDurationChange = { viewModel.setTimerAlertDuration(it) },
                    alwaysShowReminder = uiState.appSettings.alwaysShowReminder,
                    onAlwaysShowReminderChange = { viewModel.setAlwaysShowReminder(it) },
                    overlayStyle = uiState.appSettings.overlayStyle,
                    onOverlayStyleChange = { viewModel.setOverlayStyle(it) },
                    lockedMode = uiState.appSettings.lockedMode,
                    onLockedModeChange = { viewModel.setLockedMode(it) })
            }

            // ========== SECTION 2: INTERVENTIONS ==========
            item {
                SectionHeader(title = "Interventions")
            }
            item {
                InterventionsSection(
                    appSettings = uiState.appSettings,
                    viewModel = viewModel,
                    currentFrictionLevel = uiState.currentFrictionLevel,
                    frictionLevelOverride = uiState.frictionLevelOverride,
                    onFrictionLevelSelected = { viewModel.setFrictionLevel(it) },
                    snoozeActive = uiState.snoozeActive,
                    snoozeRemainingMinutes = uiState.snoozeRemainingMinutes
                )
            }

            // ========== SECTION 3: APPEARANCE ==========
            item {
                SectionHeader(title = "Appearance")
            }
            item {
                AppearanceSection(
                    navController = navController
                )
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
        modifier = Modifier.fillMaxWidth(), shape = Shapes.card, colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Spacing.verticalArrangementMD
        ) {
            // App header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(text = appIcon, fontSize = 32.sp)
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (progress?.goal != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = progress.goal.getStreakMessage(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = progress.goal.getLongestStreakMessage(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Current progress (if goal exists)
            if (progress != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.button,
                    colors = CardDefaults.cardColors(
                        containerColor = when (progress.getProgressColor()) {
                            dev.sadakat.thinkfaster.domain.model.ProgressColor.GREEN -> MaterialTheme.colorScheme.primaryContainer

                            dev.sadakat.thinkfaster.domain.model.ProgressColor.YELLOW -> MaterialTheme.colorScheme.tertiaryContainer

                            dev.sadakat.thinkfaster.domain.model.ProgressColor.ORANGE -> MaterialTheme.colorScheme.errorContainer.copy(
                                alpha = 0.6f
                            )

                            dev.sadakat.thinkfaster.domain.model.ProgressColor.RED -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                        verticalArrangement = Spacing.verticalArrangementSM
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = progress.formatTodayUsage(),
                                style = MaterialTheme.typography.titleMedium,
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
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = progress.formatRemainingTime(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Divider()

            // Goal setting slider
            Column(
                verticalArrangement = Spacing.verticalArrangementSM
            ) {
                Text(
                    text = "Daily Limit: ${sliderValue.roundToInt()} minutes",
                    style = MaterialTheme.typography.titleMedium,
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "180 min",
                        style = MaterialTheme.typography.bodySmall,
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
                shape = Shapes.button
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (progress?.goal != null) "Update Goal" else "Set Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerDurationCard(
    currentDuration: Int, onDurationChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentDuration.toFloat()) }
    val hasChanged = sliderValue.roundToInt() != currentDuration

    Card(
        modifier = Modifier.fillMaxWidth(), shape = Shapes.card, colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Spacing.verticalArrangementMD
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Spacing.horizontalArrangementSM
            ) {
                Text(text = "⏰", fontSize = 24.sp)
                Text(
                    text = "Timer Alert Duration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Get an alert after using the app continuously for this duration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1 min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "120 min",
                    style = MaterialTheme.typography.bodySmall,
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
                    shape = Shapes.button
                ) {
                    Text(
                        text = "Apply",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Section Header - Consistent header for settings sections
 */
@Composable
private fun SectionHeader(
    title: String, modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier.padding(
            start = Spacing.md, top = Spacing.lg, bottom = Spacing.sm
        ),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Always Show Reminder Card - Toggle for reminder overlay
 */
@Composable
private fun AlwaysShowReminderCard(
    enabled: Boolean, onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = Shapes.card, colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(text = "🔔", fontSize = 24.sp)
                    Text(
                        text = "Always Show Reminder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Show reminder overlay every time you open Facebook or Instagram",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = enabled, onCheckedChange = onToggle
            )
        }
    }
}

/**
 * Overlay Style Card - Segmented control for overlay style selection
 */
@Composable
private fun OverlayStyleCard(
    currentStyle: dev.sadakat.thinkfaster.domain.model.OverlayStyle,
    onStyleChange: (dev.sadakat.thinkfaster.domain.model.OverlayStyle) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Spacing.horizontalArrangementSM
            ) {
                Text(text = "📐", fontSize = 24.sp)
                Text(
                    text = "Overlay Style",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Segmented control style toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Spacing.horizontalArrangementSM
            ) {
                // Full-screen option
                OutlinedButton(
                    onClick = { onStyleChange(dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (currentStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
                        contentColor = if (currentStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (currentStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text("Full-screen", fontWeight = FontWeight.Medium)
                }

                // Compact option
                OutlinedButton(
                    onClick = { onStyleChange(dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (currentStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
                        contentColor = if (currentStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (currentStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text("Compact", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Locked Mode Card - Toggle for maximum friction mode
 */
@Composable
private fun LockedModeCard(
    enabled: Boolean, onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = Shapes.card, colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), border = if (enabled) {
            BorderStroke(
                2.dp, MaterialTheme.colorScheme.error
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(
                        text = if (enabled) "🔒" else "🔓", fontSize = 24.sp
                    )
                    Text(
                        text = "Locked Mode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (enabled) {
                        "Maximum friction - 10 second delay with countdown. You requested this extra control."
                    } else {
                        "Enable maximum friction for extra self-control (10s delay)"
                    }, style = MaterialTheme.typography.bodyMedium, color = if (enabled) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }, lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = enabled, onCheckedChange = onToggle, colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.error,
                    checkedTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

/**
 * Theme & Appearance Card - Navigation link to theme settings
 */
@Composable
private fun ThemeAppearanceCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(text = "🎨", fontSize = 24.sp)
                    Text(
                        text = "Theme & Appearance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Customize app theme, colors, and appearance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
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

/**
 * Intervention Analytics Card - Navigation link to analytics
 */
@Composable
private fun InterventionAnalyticsCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(text = "📊", fontSize = 24.sp)
                    Text(
                        text = "Intervention Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "View intervention effectiveness and content performance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
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
        shape = Shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(text = "⚙️", fontSize = 24.sp)
                    Text(
                        text = "Intervention Strength",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Control how much friction appears when opening apps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current selection display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(
                        text = "Current:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentDisplayName,
                        style = MaterialTheme.typography.labelLarge,
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
            }) {
            FrictionLevelBottomSheetContent(
                currentLevel = currentLevel,
                selectedOverride = selectedOverride,
                onLevelSelected = {
                    onLevelSelected(it)
                    showBottomSheet = false
                })
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
        verticalArrangement = Spacing.verticalArrangementMD
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select the level of friction",
                    style = MaterialTheme.typography.bodyMedium,
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
            onSelect = { onLevelSelected(null) })

        // Manual options for each friction level
        dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.values().forEach { level ->
            FrictionLevelOption(
                title = level.displayName,
                description = level.description,
                level = level,
                currentLevel = currentLevel,
                isSelected = selectedOverride == level,
                onSelect = { onLevelSelected(level) })
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
        modifier = Modifier.fillMaxWidth(), shape = Shapes.button, colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ), border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null, onClick = onSelect
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
                    Text(
                        text = title,
                        style = if (isSelected) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium,
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
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description, style = MaterialTheme.typography.bodySmall, color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }, lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Radio button indicator
            RadioButton(
                selected = isSelected, onClick = onSelect, colors = RadioButtonDefaults.colors(
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
    appSettings: AppSettings, viewModel: GoalViewModel
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showBottomSheet = true },
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(text = "🔔", fontSize = 24.sp)
                    Text(
                        text = "Daily Reminders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }/*   Spacer(modifier = Modifier.height(4.dp))
                   Text(
                       text = if (appSettings.motivationalNotificationsEnabled) {
                           "Enabled • ${appSettings.getMorningTimeFormatted()} & ${appSettings.getEveningTimeFormatted()}"
                       } else {
                           "Tap to configure notification times"
                       },
                       fontSize = 14.sp,
                       color = MaterialTheme.colorScheme.onSurfaceVariant,
                       lineHeight = 20.sp
                   )*/
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
                onDismiss = { showBottomSheet = false })
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🔔 Daily Reminders",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stay on track with daily notifications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
 * Account & Sync card - shows authentication status and provides access to sync settings
 */
@Composable
private fun AccountAndSyncCard(
    navController: NavHostController, syncPreferences: SyncPreferences = koinInject()
) {
    val isAuthenticated = syncPreferences.isAuthenticated()
    val userEmail = syncPreferences.getUserEmail()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isAuthenticated) {
                    navController.navigate(Screen.AccountManagement.route)
                } else {
                    navController.navigate(Screen.Login.route)
                }
            }, shape = Shapes.card, colors = CardDefaults.cardColors(
            containerColor = if (isAuthenticated) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(
                        text = if (isAuthenticated) "☁️" else "🔐", fontSize = 24.sp
                    )
                    Text(
                        text = "Account & Sync",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isAuthenticated) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAuthenticated) {
                        "Signed in • Tap to manage"
                    } else {
                        "Sign in to sync your data across devices"
                    }, style = MaterialTheme.typography.bodyMedium, color = if (isAuthenticated) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                // Show email below if authenticated
                if (isAuthenticated && userEmail != null) {
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isAuthenticated) "Manage account" else "Sign in",
                tint = if (isAuthenticated) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .size(32.dp)
                    .rotate(270f)
            )
        }
    }
}

/**
 * Custom slider with circular thumb for better visual appearance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoundedThumbSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        thumb = {
            // Custom circular thumb using Surface
            Surface(
                shape = Shapes.badge, // Fully rounded (circular)
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
                shadowElevation = 4.dp,
                tonalElevation = 4.dp
            ) {}
        })
}

/**
 * Timer & Alerts Section - Grouped card for timer and alert settings
 */
@Composable
private fun TimerAndAlertsSection(
    timerDuration: Int,
    onTimerDurationChange: (Int) -> Unit,
    alwaysShowReminder: Boolean,
    onAlwaysShowReminderChange: (Boolean) -> Unit,
    overlayStyle: dev.sadakat.thinkfaster.domain.model.OverlayStyle,
    onOverlayStyleChange: (dev.sadakat.thinkfaster.domain.model.OverlayStyle) -> Unit,
    lockedMode: Boolean,
    onLockedModeChange: (Boolean) -> Unit
) {
    var sliderValue by remember { mutableStateOf(timerDuration.toFloat()) }
    val hasChanged = sliderValue.roundToInt() != timerDuration

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Spacing.verticalArrangementMD
        ) {
            // Overlay Style
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Spacing.horizontalArrangementSM
            ) {
                Text(text = "📐", fontSize = 20.sp)
                Text(
                    text = "Overlay Style",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Spacing.horizontalArrangementSM
            ) {
                OutlinedButton(
                    onClick = { onOverlayStyleChange(dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
                        contentColor = if (overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.FULLSCREEN) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text("Full-screen", fontWeight = FontWeight.Medium)
                }
                OutlinedButton(
                    onClick = { onOverlayStyleChange(dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
                        contentColor = if (overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text("Compact", fontWeight = FontWeight.Medium)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Timer Alert Duration
            Column(verticalArrangement = Spacing.verticalArrangementSM) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(text = "⏰", fontSize = 20.sp)
                    Text(
                        text = "Timer Alert",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = Shapes.button,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "${sliderValue.roundToInt()} min",
                            modifier = Modifier.padding(
                                horizontal = Spacing.md, vertical = Spacing.xs
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                RoundedThumbSlider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..120f,
                    steps = 118,
                    modifier = Modifier.fillMaxWidth()
                )
                if (hasChanged) {
                    Button(
                        onClick = { onTimerDurationChange(sliderValue.roundToInt()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = Shapes.button
                    ) {
                        Text(
                            "Apply",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Always Show Reminder
            SettingRow(
                icon = "🔔",
                title = "Reminder on App start",
                description = "",
                toggle = true,
                checked = alwaysShowReminder,
                onCheckedChange = onAlwaysShowReminderChange
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Locked Mode
            SettingRow(
                icon = if (lockedMode) "🔒" else "🔓",
                title = "Locked Mode",
                description = "Maximum friction with 10s delay",
                toggle = true,
                checked = lockedMode,
                onCheckedChange = onLockedModeChange,
                titleColor = if (lockedMode) MaterialTheme.colorScheme.error else null
            )
        }
    }
}

/**
 * Reusable setting row with toggle switch
 */
@Composable
private fun SettingRow(
    icon: String,
    title: String,
    description: String,
    toggle: Boolean = false,
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    titleColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Spacing.horizontalArrangementSM,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor ?: MaterialTheme.colorScheme.onBackground
                )
                if (description.isNotEmpty()) Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (toggle && onCheckedChange != null) {
            Switch(
                checked = checked, onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * Interventions Section - Grouped card for intervention settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterventionsSection(
    appSettings: AppSettings,
    viewModel: GoalViewModel,
    currentFrictionLevel: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel,
    frictionLevelOverride: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel?,
    onFrictionLevelSelected: (dev.sadakat.thinkfaster.domain.intervention.FrictionLevel?) -> Unit,
    snoozeActive: Boolean,
    snoozeRemainingMinutes: Int
) {
    var showFrictionBottomSheet by remember { mutableStateOf(false) }
    var showNotificationBottomSheet by remember { mutableStateOf(false) }
    var showSnoozeBottomSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // Daily Reminders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNotificationBottomSheet = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Spacing.horizontalArrangementSM
                    ) {
                        Text(text = "🔔", fontSize = 24.sp)
                        Text(
                            text = "Daily Reminders",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }/*    Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (appSettings.motivationalNotificationsEnabled) {
                                "Enabled • ${appSettings.getMorningTimeFormatted()} & ${appSettings.getEveningTimeFormatted()}"
                            } else {
                                "Tap to configure notification times"
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )*/
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Open settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(270f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.lg),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Intervention Strength
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFrictionBottomSheet = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Spacing.horizontalArrangementSM
                    ) {
                        Text(text = "⚙️", fontSize = 24.sp)
                        Text(
                            text = "Intervention Strength",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))/*      Text(
                              text = "Control how much friction appears when opening apps",
                              fontSize = 13.sp,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              lineHeight = 18.sp
                          )
                          Spacer(modifier = Modifier.height(12.dp))*/
                    val currentDisplayName = when {
                        frictionLevelOverride == null -> "Auto (Recommended)"
                        else -> frictionLevelOverride.displayName
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Spacing.horizontalArrangementSM
                    ) {
                        Text(
                            text = "Current:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentDisplayName,
                            style = MaterialTheme.typography.labelLarge,
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
                        .size(24.dp)
                        .rotate(270f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.lg),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Snooze Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSnoozeBottomSheet = true },
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (snoozeActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                    }/*   Spacer(modifier = Modifier.height(8.dp))
                       Text(
                           text = "Temporarily pause interventions",
                           fontSize = 13.sp,
                           color = if (snoozeActive) MaterialTheme.colorScheme.onTertiaryContainer.copy(
                               alpha = 0.8f
                           )
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                           lineHeight = 18.sp
                       )*/
                    if (snoozeActive && snoozeRemainingMinutes > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$snoozeRemainingMinutes min remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Surface(
                    shape = Shapes.button,
                    color = if (snoozeActive) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (snoozeActive) "Active" else "Off",
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Bottom sheets
    if (showNotificationBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            NotificationSettingsBottomSheet(
                appSettings = appSettings,
                viewModel = viewModel,
                onDismiss = { showNotificationBottomSheet = false })
        }
    }

    if (showFrictionBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFrictionBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            FrictionLevelBottomSheetContent(
                currentLevel = currentFrictionLevel,
                selectedOverride = frictionLevelOverride,
                onLevelSelected = {
                    onFrictionLevelSelected(it)
                    showFrictionBottomSheet = false
                })
        }
    }

    if (showSnoozeBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSnoozeBottomSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            SnoozeBottomSheetContent(
                snoozeActive = snoozeActive,
                remainingMinutes = snoozeRemainingMinutes,
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
 * Account & Data Section - Grouped card for account and data settings
 */
@Composable
private fun AccountAndDataSection(
    navController: NavHostController, syncPreferences: SyncPreferences = koinInject()
) {
    val isAuthenticated = syncPreferences.isAuthenticated()

    Card(
        modifier = Modifier.fillMaxWidth(), shape = Shapes.card, colors = CardDefaults.cardColors(
            containerColor = if (isAuthenticated) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // Intervention Analytics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("analytics") },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Spacing.horizontalArrangementSM
                    ) {
                        Text(text = "📊", fontSize = 24.sp)
                        Text(
                            text = "Intervention Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "View intervention effectiveness and content performance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
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

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.lg),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Account & Sync
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isAuthenticated) {
                            navController.navigate(Screen.AccountManagement.route)
                        } else {
                            navController.navigate(Screen.Login.route)
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Spacing.horizontalArrangementSM
                    ) {
                        Text(
                            text = if (isAuthenticated) "☁️" else "🔐", fontSize = 24.sp
                        )
                        Text(
                            text = "Account & Sync",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isAuthenticated) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val userEmail = syncPreferences.getUserEmail()
                    Text(
                        text = if (isAuthenticated) {
                            "Signed in • Tap to manage"
                        } else {
                            "Sign in to sync your data across devices"
                        }, style = MaterialTheme.typography.bodyMedium, color = if (isAuthenticated) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (isAuthenticated && userEmail != null) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isAuthenticated) "Manage account" else "Sign in",
                    tint = if (isAuthenticated) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .rotate(270f)
                )
            }
        }
    }
}

/**
 * Appearance Section - Theme & Appearance settings
 */
@Composable
private fun AppearanceSection(
    navController: NavHostController
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("theme_appearance") },
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(text = "🎨", fontSize = 24.sp)
                    Text(
                        text = "Theme & Appearance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Customize app theme, colors, and appearance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
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