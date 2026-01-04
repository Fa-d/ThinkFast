package dev.sadakat.thinkfaster.presentation.home

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
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
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.theme.AppColors
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

    // Bottom sheet state for goal management
    var showManageAppsSheet by remember { mutableStateOf(false) }
    var showGoalEditorSheet by remember { mutableStateOf(false) }
    var showAddAppsSheet by remember { mutableStateOf(false) }
    var selectedAppForGoalEdit by remember { mutableStateOf<PerAppGoalUiModel?>(null) }

    // Track newly added apps for sequential goal setting
    var appsNeedingGoals by remember { mutableStateOf<List<PerAppGoalUiModel>>(emptyList()) }
    var currentGoalEditorIndex by remember { mutableIntStateOf(0) }

    // Load data on first composition and periodically refresh
    LaunchedEffect(Unit) {
        // Initial load
        viewModel.loadTodaySummary(isRefresh = false)
        viewModel.checkServiceStatus(context)
        viewModel.loadFreezeStatus()      // Broken Streak Recovery
        viewModel.loadRecoveryStatus()    // Broken Streak Recovery
        viewModel.loadTrackedAppsGoals()  // Per-App Goals
        viewModel.loadCuratedApps()       // Load curated apps
        viewModel.loadTrackedAppsList()   // Load tracked apps list
        viewModel.loadInstalledApps()     // Load all installed apps

        // Refresh every 30 seconds without showing loading spinner
        while (true) {
            delay(30000)
            viewModel.loadTodaySummary(isRefresh = true)
            viewModel.checkServiceStatus(context)
            viewModel.loadFreezeStatus()      // Refresh freeze status
            viewModel.loadRecoveryStatus()    // Refresh recovery status
            viewModel.loadTrackedAppsGoals()  // Refresh tracked apps goals
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
                    Text(
                        "ThinkFast",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        HapticFeedback.light(context)
                        // Manual refresh - use isRefresh=true for smooth refresh
                        viewModel.loadTodaySummary(isRefresh = true)
                        viewModel.checkServiceStatus(context)
                        viewModel.loadTrackedAppsGoals()
                        hasAllPermissions = PermissionHelper.hasAllRequiredPermissions(context)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        val pullToRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                viewModel.loadTodaySummary(isRefresh = true)
                viewModel.checkServiceStatus(context)
                viewModel.loadTrackedAppsGoals()
            },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.sm
            ),
            verticalArrangement = Spacing.verticalArrangementMD
        ) {
            // Complete Setup Banner (for users who skipped onboarding)
            item {
                CompleteSetupBanner(navController = navController, context = context)
            }

            // Hero Card - Today's usage summary with quick actions
            item {
                TodayAtAGlanceCard(
                    uiState = uiState,
                    context = context,
                    hasPermissions = hasAllPermissions,
                    onSetGoalsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onManageAppsClick = {
                        navController.navigate(Screen.ManageApps.route)
                    },
                    onStartService = {
                        HapticFeedback.success(context)
                        startMonitoringService(context)
                        viewModel.updateServiceState(true)
                    },
                    onStopService = {
                        HapticFeedback.medium(context)
                        stopMonitoringService(context)
                        viewModel.updateServiceState(false)
                    }
                )
            }

            // Tracked Apps Section (Per-App Goals feature)
            item {
                TrackedAppsSection(
                    trackedAppsGoals = uiState.trackedAppsGoals,
                    expandedAppForGoalEdit = uiState.expandedAppForGoalEdit,
                    isUpdatingGoal = uiState.isUpdatingGoal,
                    onToggleExpanded = viewModel::toggleGoalEditor,
                    onUpdateGoal = viewModel::updateAppGoal,
                    onAddAppsClick = {
                        navController.navigate(Screen.ManageApps.route)
                    },
                    onManageClick = { showManageAppsSheet = true }
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

    // Manage Apps Bottom Sheet
    if (showManageAppsSheet) {
        dev.sadakat.thinkfaster.ui.components.ManageAppsBottomSheet(
            trackedApps = uiState.trackedAppsGoals,
            onAppClick = { app ->
                selectedAppForGoalEdit = app
                showManageAppsSheet = false
                showGoalEditorSheet = true
            },
            onDismiss = { showManageAppsSheet = false },
            onAddAppsClick = {
                showManageAppsSheet = false
                showAddAppsSheet = true
            },
            onRemoveApp = { packageName ->
                viewModel.removeTrackedApp(packageName)
            }
        )
    }

    // Add Apps Bottom Sheet
    if (showAddAppsSheet) {
        dev.sadakat.thinkfaster.ui.components.AddAppsBottomSheet(
            installedApps = uiState.installedApps,
            trackedApps = uiState.trackedApps ?: emptyList(),
            isLimitReached = (uiState.trackedApps?.size ?: 0) >= 10,
            onAddApp = { packageName ->
                viewModel.addTrackedApp(packageName)
            },
            onRemoveApp = { packageName ->
                viewModel.removeTrackedApp(packageName)
            },
            onDismiss = {
                showAddAppsSheet = false
                // Find all apps that were added but don't have goals yet
                val newAppsWithoutGoals = uiState.trackedAppsGoals.filter {
                    it.goal == null
                }
                if (newAppsWithoutGoals.isNotEmpty()) {
                    appsNeedingGoals = newAppsWithoutGoals
                    currentGoalEditorIndex = 0
                    selectedAppForGoalEdit = appsNeedingGoals.first()
                    showGoalEditorSheet = true
                } else {
                    showManageAppsSheet = true
                }
            }
        )
    }

    // Goal Editor Bottom Sheet
    if (showGoalEditorSheet && selectedAppForGoalEdit != null) {
        dev.sadakat.thinkfaster.ui.components.GoalEditorBottomSheet(
            app = selectedAppForGoalEdit!!,
            progressText = if (appsNeedingGoals.size > 1) {
                "${currentGoalEditorIndex + 1} of ${appsNeedingGoals.size}"
            } else null,
            onDismiss = {
                showGoalEditorSheet = false
                selectedAppForGoalEdit = null
                // Reset sequential flow if user cancels
                appsNeedingGoals = emptyList()
                currentGoalEditorIndex = 0
            },
            onSaveGoal = { minutes ->
                viewModel.updateAppGoal(selectedAppForGoalEdit!!.packageName, minutes)
                // Check if there are more apps needing goals
                if (currentGoalEditorIndex < appsNeedingGoals.size - 1) {
                    // Move to next app
                    currentGoalEditorIndex++
                    selectedAppForGoalEdit = appsNeedingGoals[currentGoalEditorIndex]
                } else {
                    // All done, close and reset
                    showGoalEditorSheet = false
                    selectedAppForGoalEdit = null
                    appsNeedingGoals = emptyList()
                    currentGoalEditorIndex = 0
                }
            }
        )
    }
}

/**
 * Today at a Glance card - Hero section with clear value proposition
 * Redesigned for clarity and simplicity
 */
@Composable
private fun TodayAtAGlanceCard(
    uiState: HomeUiState,
    context: Context,
    hasPermissions: Boolean,
    onSetGoalsClick: () -> Unit,
    onManageAppsClick: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val alpha = rememberFadeInAnimation(durationMillis = 600)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = alpha),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Spacing.verticalArrangementMD,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Value proposition
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Spacing.verticalArrangementSM
            ) {
                Text(
                    text = "Track & Reduce Distracting Apps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Take control of your screen time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }


            // Loading state
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // No goals set - Simple empty state
            else if (!uiState.hasGoalsSet) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Spacing.verticalArrangementMD
                ) {
                    Text(
                        text = "🎯",
                        fontSize = 64.sp
                    )
                    Text(
                        text = "Set your first goal to get started",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Track your usage and build healthy habits",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Button(
                        onClick = {
                            HapticFeedback.success(context)
                            onSetGoalsClick()
                        },
                        shape = Shapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Set Goals", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            // Goals set - Show progress in new layout
            else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Spacing.verticalArrangementMD
                ) {
                    // Progress section with percentage on left and circular ring on right
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shapes.button)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(Spacing.lg),
                        verticalArrangement = Spacing.verticalArrangementMD
                    ) {
                        // Header: Icon + "Your Progress"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Spacing.horizontalArrangementSM
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📊", fontSize = 20.sp)
                            }
                            Text(
                                text = "Your Progress",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Main content row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Percentage and info
                            Column(
                                verticalArrangement = Spacing.verticalArrangementSM
                            ) {
                                Text(
                                    text = "${uiState.progressPercentage ?: 0}%",
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 56.sp
                                )

                                // Info text below percentage (remaining minutes or streak)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Spacing.horizontalArrangementXS
                                ) {
                                    if (uiState.remainingMinutes != null && uiState.remainingMinutes > 0 && !uiState.isOverLimit) {
                                        Text(
                                            text = "${uiState.remainingMinutes} min left",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else if (uiState.isOverLimit) {
                                        Text(
                                            text = "Over limit",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Right side: Circular progress ring with flame icon
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(120.dp)
                            ) {
                                val progress = (uiState.progressPercentage ?: 0) / 100f
                                val progressColor = AppColors.Progress.getColorForPercentage(uiState.progressPercentage ?: 0)

                                // Background circle
                                Canvas(modifier = Modifier.size(120.dp)) {
                                    drawArc(
                                        color = progressColor.copy(alpha = 0.2f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 10.dp.toPx(),
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )

                                    // Progress arc
                                    drawArc(
                                        color = progressColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * progress.coerceIn(0f, 1f),
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 10.dp.toPx(),
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )
                                }

                                // Flame icon badge at top-right
                                if (uiState.currentStreak > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 8.dp, y = (-8).dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🔥", fontSize = 16.sp)
                                    }
                                }

                                // Center content
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Spacing.verticalArrangementXS
                                ) {
                                    Text(
                                        text = "${uiState.totalUsageMinutes}",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "minutes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Streak info below if exists
                        if (uiState.currentStreak > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Spacing.horizontalArrangementXS,
                                modifier = Modifier
                                    .clip(Shapes.button)
                                    .background(AppColors.Streak.getColorForStreak(uiState.currentStreak).copy(alpha = 0.15f))
                                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                            ) {
                                Text(text = "🔥", fontSize = 14.sp)
                                Text(
                                    text = "${uiState.currentStreak} day streak",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.Streak.getColorForStreak(uiState.currentStreak)
                                )
                            }
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
                            shape = Shapes.button,
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.isOverLimit) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Text(
                                text = uiState.celebrationMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.isOverLimit) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                },
                                modifier = Modifier.padding(Spacing.md),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.md),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Quick Actions - Only Set Goals and View Apps
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Spacing.verticalArrangementSM
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Spacing.horizontalArrangementSM
                        ) {
                            // Set Goals button
                            OutlinedButton(
                                onClick = {
                                    HapticFeedback.light(context)
                                    onSetGoalsClick()
                                },
                                modifier = Modifier.weight(1f),
                                shape = Shapes.button
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Spacing.verticalArrangementXS,
                                    modifier = Modifier.padding(vertical = Spacing.sm)
                                ) {
                                    Text(text = "⚙️", fontSize = 24.sp)
                                    Text(
                                        text = "Set Goals",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // View Apps button
                            OutlinedButton(
                                onClick = {
                                    HapticFeedback.light(context)
                                    onManageAppsClick()
                                },
                                modifier = Modifier.weight(1f),
                                shape = Shapes.button
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Spacing.verticalArrangementXS,
                                    modifier = Modifier.padding(vertical = Spacing.sm)
                                ) {
                                    Text(text = "📱", fontSize = 24.sp)
                                    Text(
                                        text = "View Apps",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.md),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Usage Monitoring Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shapes.button)
                            .background(
                                if (uiState.isServiceRunning && hasPermissions) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left side: Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Spacing.horizontalArrangementSM
                        ) {
                            Icon(
                                imageVector = if (uiState.isServiceRunning && hasPermissions) {
                                    Icons.Default.Check
                                } else {
                                    Icons.Default.Close
                                },
                                contentDescription = null,
                                tint = if (uiState.isServiceRunning && hasPermissions) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Usage Monitoring",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.isServiceRunning && hasPermissions) {
                                        "Active"
                                    } else if (!hasPermissions) {
                                        "Permissions required"
                                    } else {
                                        "Paused"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Right side: Toggle switch (only if has permissions)
                        if (hasPermissions) {
                            Switch(
                                checked = uiState.isServiceRunning,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        onStartService()
                                    } else {
                                        onStopService()
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
        shape = Shapes.button,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Spacing.verticalArrangementSM
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Spacing.horizontalArrangementMD
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
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
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Spacing.verticalArrangementMD
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
                    horizontalArrangement = Spacing.horizontalArrangementMD
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
            shape = Shapes.button
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(Spacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Complete Setup Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "ThinkFast isn't active yet. Complete the setup to start protecting your time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.sm))

                Button(
                    onClick = {
                        // Navigate to onboarding welcome screen (Step 1) for complete flow
                        navController.navigate(Screen.OnboardingWelcome.route)
                    },
                    shape = Shapes.button,
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
 * Tracked Apps Section - Displays per-app goal cards
 * Per-App Goals feature with bottom sheet integration
 */
@Composable
private fun TrackedAppsSection(
    trackedAppsGoals: List<PerAppGoalUiModel>,
    expandedAppForGoalEdit: String?,
    isUpdatingGoal: Boolean,
    onToggleExpanded: (String) -> Unit,
    onUpdateGoal: (String, Int) -> Unit,
    onAddAppsClick: () -> Unit,
    onManageClick: () -> Unit,  // NEW: Opens bottom sheet
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Spacing.verticalArrangementMD
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tracked Apps",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Show "Add Apps" button when empty, "Manage" button when has apps
            if (trackedAppsGoals.isEmpty()) {
                TextButton(
                    onClick = onAddAppsClick,
                    shape = Shapes.button
                ) {
                    Text(
                        text = "Add Apps",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                TextButton(
                    onClick = onManageClick,
                    shape = Shapes.button
                ) {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Empty state
        if (trackedAppsGoals.isEmpty()) {
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
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Spacing.verticalArrangementMD
                ) {
                    Text(
                        text = "📱",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "No Apps Tracked Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Start tracking apps to set goals and monitor your usage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Button(
                        onClick = onAddAppsClick,
                        shape = Shapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Add Apps to Track",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        } else {
            // Goal cards
            trackedAppsGoals.forEach { app ->
                dev.sadakat.thinkfaster.ui.components.GoalCard(
                    app = app,
                    isExpanded = expandedAppForGoalEdit == app.packageName,
                    onToggleExpanded = { onToggleExpanded(app.packageName) },
                    onSaveGoal = { dailyLimit -> onUpdateGoal(app.packageName, dailyLimit) }
                )

                Spacer(modifier = Modifier.height(Spacing.sm))
            }
        }
    }
}

