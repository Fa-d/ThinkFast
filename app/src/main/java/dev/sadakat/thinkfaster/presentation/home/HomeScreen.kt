package dev.sadakat.thinkfaster.presentation.home

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import dev.sadakat.thinkfaster.presentation.navigation.Screen
import dev.sadakat.thinkfaster.service.UsageMonitorService
import dev.sadakat.thinkfaster.domain.model.QuickWinType
import dev.sadakat.thinkfaster.ui.components.BaselineComparisonCard
import dev.sadakat.thinkfaster.ui.components.CompactCelebrationCard
import dev.sadakat.thinkfaster.ui.components.DayOneCelebration
import dev.sadakat.thinkfaster.ui.components.DayTwoCelebration
import dev.sadakat.thinkfaster.ui.components.FirstSessionCelebration
import dev.sadakat.thinkfaster.ui.components.FirstUnderGoalCelebration
import dev.sadakat.thinkfaster.ui.components.QuestProgressCard
import dev.sadakat.thinkfaster.ui.components.RecoveryCompleteDialog
import dev.sadakat.thinkfaster.ui.components.RecoveryProgressCard
import dev.sadakat.thinkfaster.ui.components.StreakBrokenRecoveryDialog
import dev.sadakat.thinkfaster.ui.components.StreakFreezeCard
import dev.sadakat.thinkfaster.ui.components.StreakMilestoneCelebration
import dev.sadakat.thinkfaster.ui.components.rememberFadeInAnimation
import dev.sadakat.thinkfaster.ui.theme.ProgressColors
import dev.sadakat.thinkfaster.util.HapticFeedback
import dev.sadakat.thinkfaster.util.PermissionHelper
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

/**
 * Home screen - main dashboard with "Today at a Glance" summary
 * Phase 1.2: Enhanced with usage stats, streaks, and celebration messages
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var hasAllPermissions by remember { mutableStateOf(PermissionHelper.hasAllRequiredPermissions(context)) }

    // Load data on first composition and periodically refresh
    LaunchedEffect(Unit) {
        // Initial load
        viewModel.loadTodaySummary(isRefresh = false)
        viewModel.checkServiceStatus(context)
        viewModel.loadFreezeStatus()      // Broken Streak Recovery
        viewModel.loadRecoveryStatus()    // Broken Streak Recovery

        // Refresh every 30 seconds without showing loading spinner
        while (true) {
            delay(30000)
            viewModel.loadTodaySummary(isRefresh = true)
            viewModel.checkServiceStatus(context)
            viewModel.loadFreezeStatus()      // Refresh freeze status
            viewModel.loadRecoveryStatus()    // Refresh recovery status
            hasAllPermissions = PermissionHelper.hasAllRequiredPermissions(context)
        }
    }

    // Check for streak milestones when streak changes (Phase 1.5)
    LaunchedEffect(uiState.currentStreak) {
        if (uiState.currentStreak > 0) {
            viewModel.checkForStreakMilestone()
            viewModel.updateGoalAchievedBadge()
        }
    }

    // Streak Milestone Celebration Dialog (Phase 1.5)
    StreakMilestoneCelebration(
        show = uiState.showStreakCelebration,
        streakDays = uiState.currentStreak,
        onDismiss = { viewModel.dismissStreakCelebration() }
    )

    // Streak Broken Recovery Dialog (Broken Streak Recovery feature)
    uiState.activeRecovery?.let { recovery ->
        StreakBrokenRecoveryDialog(
            show = uiState.showStreakBrokenDialog,
            previousStreak = recovery.previousStreak,
            targetApp = recovery.targetApp,
            onDismiss = { viewModel.dismissStreakBrokenDialog() }
        )
    }

    // Recovery Complete Dialog (Broken Streak Recovery feature)
    uiState.completedRecovery?.let { completed ->
        RecoveryCompleteDialog(
            show = uiState.showRecoveryCompleteDialog,
            previousStreak = completed.previousStreak,
            daysToRecover = completed.currentRecoveryDays,
            targetApp = completed.targetApp,
            onDismiss = { viewModel.dismissRecoveryCompleteDialog() }
        )
    }

    // Quick Win Celebrations (First-Week Retention feature)
    when (uiState.quickWinToShow) {
        QuickWinType.FIRST_SESSION -> {
            FirstSessionCelebration(
                show = true,
                onDismiss = { viewModel.dismissQuickWin(QuickWinType.FIRST_SESSION) }
            )
        }
        QuickWinType.FIRST_UNDER_GOAL -> {
            FirstUnderGoalCelebration(
                show = true,
                goalMinutes = uiState.goalMinutes ?: 60,
                onDismiss = { viewModel.dismissQuickWin(QuickWinType.FIRST_UNDER_GOAL) }
            )
        }
        QuickWinType.DAY_ONE_COMPLETE -> {
            DayOneCelebration(
                show = true,
                usageMinutes = uiState.totalUsageMinutes,
                goalMinutes = uiState.goalMinutes ?: 60,
                onDismiss = { viewModel.dismissQuickWin(QuickWinType.DAY_ONE_COMPLETE) }
            )
        }
        QuickWinType.DAY_TWO_COMPLETE -> {
            DayTwoCelebration(
                show = true,
                onDismiss = { viewModel.dismissQuickWin(QuickWinType.DAY_TWO_COMPLETE) }
            )
        }
        null -> { /* No quick win to show */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ThinkFast", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Mindful Usage Tracker",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        HapticFeedback.light(context)
                        // Manual refresh - user expects to see loading state
                        viewModel.loadTodaySummary(isRefresh = false)
                        viewModel.checkServiceStatus(context)
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
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Complete Setup Banner (for users who skipped onboarding)
            item {
                CompleteSetupBanner(navController = navController, context = context)
            }

            // Today at a Glance Card
            item {
                TodayAtAGlanceCard(
                    uiState = uiState,
                    context = context,
                    onSetGoalsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onViewStatsClick = {
                        navController.navigate(Screen.Statistics.route)
                    },
                    onAdjustGoalsClick = {
                        navController.navigate(Screen.ManageApps.route)
                    }
                )
            }

            // Manage Apps & Goals Card
            item {
                ManageAppsAndGoalsCard(
                    onClick = { navController.navigate(Screen.ManageApps.route) }
                )
            }

            // Service Status Card (compact)
            item {
                CompactServiceStatusCard(
                    isRunning = uiState.isServiceRunning,
                    hasPermissions = hasAllPermissions,
                    onStartClick = {
                        HapticFeedback.success(context)
                        startMonitoringService(context)
                        viewModel.updateServiceState(true)
                    },
                    onStopClick = {
                        HapticFeedback.medium(context)
                        stopMonitoringService(context)
                        viewModel.updateServiceState(false)
                    }
                )
            }

            // Quest Progress Card (First-Week Retention: Days 1-7)
            if (uiState.showQuestCard) {
                uiState.questStatus?.let { quest ->
                    item {
                        QuestProgressCard(
                            quest = quest,
                            onDismiss = { viewModel.dismissQuestCard() }
                        )
                    }
                }
            }

            // Baseline Comparison Card (First-Week Retention: Day 3+)
            if (uiState.showBaselineCard) {
                uiState.userBaseline?.let { baseline ->
                    item {
                        BaselineComparisonCard(
                            baseline = baseline,
                            todayUsageMinutes = uiState.totalUsageMinutes
                        )
                    }
                }
            }

            // Goal Achievement Badge (Phase 1.5)
            if (uiState.showGoalAchievedBadge && !uiState.isOverLimit) {
                item {
                    CompactCelebrationCard(
                        show = true,
                        emoji = "🎯",
                        title = "On Track!",
                        message = "You're meeting your daily goal. Keep it up!",
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        textColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Recovery Progress Card (Broken Streak Recovery feature)
            uiState.activeRecovery?.let { recovery ->
                if (!recovery.isRecoveryComplete) {
                    item {
                        RecoveryProgressCard(
                            recovery = recovery,
                            onDismiss = { viewModel.dismissRecoveryCard() }
                        )
                    }
                }
            }

            // Streak Freeze Card (Broken Streak Recovery feature)
            if (uiState.showFreezeButton) {
                uiState.freezeStatus?.let { freezeStatus ->
                    uiState.progressPercentage?.let { percentage ->
                        item {
                            StreakFreezeCard(
                                freezeStatus = freezeStatus,
                                currentStreak = uiState.currentStreak,
                                percentageUsed = percentage,
                                onActivateFreeze = { viewModel.activateStreakFreeze() }
                            )
                        }
                    }
                }
            }

            // Permission warning (if missing)
            if (!hasAllPermissions) {
                item {
                    PermissionWarningCard(
                        onGrantClick = {
                            navController.navigate(Screen.PermissionRequest.route)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Today at a Glance card - Main summary card
 */
@Composable
private fun TodayAtAGlanceCard(
    uiState: HomeUiState,
    context: Context,
    onSetGoalsClick: () -> Unit,
    onViewStatsClick: () -> Unit,
    onAdjustGoalsClick: () -> Unit
) {
    val alpha = rememberFadeInAnimation(durationMillis = 600)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = alpha),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today at a Glance",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Streak counter
                if (uiState.currentStreak > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${uiState.currentStreak}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (uiState.currentStreak == 1) "day" else "days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Loading state
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // No goals set
            else if (!uiState.hasGoalsSet) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🎯",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "Set a daily goal to get started!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            HapticFeedback.light(context)
                            onSetGoalsClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Set Goals")
                    }
                }
            }
            // Goals set - show progress
            else {
                // Usage vs Goal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Usage Today",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${uiState.totalUsageMinutes} min",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (uiState.isOverLimit) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Daily Goal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${uiState.goalMinutes} min",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Progress bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val progress = (uiState.progressPercentage ?: 0) / 100f

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = ProgressColors.getColorForProgress(uiState.progressPercentage ?: 0),
                        trackColor = ProgressColors.getLightColorForProgress(uiState.progressPercentage ?: 0).copy(alpha = 0.2f)
                    )

                    // Progress percentage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!uiState.isOverLimit && uiState.remainingMinutes != null) {
                            Text(
                                text = "${uiState.remainingMinutes} min remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        } else if (uiState.isOverLimit) {
                            Text(
                                text = "Over limit",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Text(
                            text = "${uiState.progressPercentage}%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Celebration message
                AnimatedVisibility(
                    visible = uiState.celebrationMessage != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isOverLimit) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Text(
                            text = uiState.celebrationMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (uiState.isOverLimit) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Permission warning card
 */
@Composable
private fun PermissionWarningCard(
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Permissions Required",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Grant permissions to enable monitoring",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            TextButton(
                onClick = onGrantClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Grant Permissions")
            }
        }
    }
}

/**
 * Enhanced service status card with toggle switch
 */
@Composable
private fun CompactServiceStatusCard(
    isRunning: Boolean,
    hasPermissions: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with icon and toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Icon + Status text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRunning && hasPermissions) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRunning && hasPermissions) {
                                Icons.Default.Check
                            } else {
                                Icons.Default.Close
                            },
                            contentDescription = null,
                            tint = if (isRunning && hasPermissions) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Status text
                    Column {
                        Text(
                            text = "Usage Monitoring",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isRunning && hasPermissions) {
                                "Active and tracking"
                            } else if (!hasPermissions) {
                                "Permissions required"
                            } else {
                                "Currently paused"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right side: Toggle switch (only if has permissions)
                if (hasPermissions) {
                    Switch(
                        checked = isRunning,
                        onCheckedChange = { checked ->
                            if (checked) {
                                onStartClick()
                            } else {
                                onStopClick()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // Helper text
            if (hasPermissions) {
                Text(
                    text = if (isRunning) {
                        "ThinkFast is monitoring Facebook and Instagram usage"
                    } else {
                        "Turn on to start tracking your social media usage"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
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

/**
 * Complete Setup Banner - shown when user skips onboarding
 * Prompts user to complete setup to activate ThinkFast
 */
@Composable
private fun CompleteSetupBanner(
    navController: NavHostController,
    context: Context
) {
    // Check if onboarding was skipped (not completed)
    val prefs = context.getSharedPreferences("think_fast_onboarding", Context.MODE_PRIVATE)
    val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
    val hasAllPermissions = PermissionHelper.hasAllRequiredPermissions(context)

    // Show banner if onboarding was skipped OR if permissions are missing
    if (!onboardingCompleted || !hasAllPermissions) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Complete Setup Required",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ThinkFast isn't active yet. Complete the setup to start protecting your time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        // Navigate to onboarding welcome screen (Step 1) for complete flow
                        navController.navigate(Screen.OnboardingWelcome.route)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Get Started")
                }
            }
        }
    }
}

/**
 * Manage Apps & Goals Card - Quick access to manage tracked apps and goals
 */
@Composable
private fun ManageAppsAndGoalsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⚙️", fontSize = 24.sp)
                    Text(
                        text = "Manage Apps & Goals",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Manage apps",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(32.dp)
                        .rotate(270f)
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // Info sections
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "📱",
                        fontSize = 20.sp
                    )
                    Column {
                        Text(
                            text = "Tracked apps",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "View & edit",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🎯",
                        fontSize = 20.sp
                    )
                    Column {
                        Text(
                            text = "Daily goals",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Set limits",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
