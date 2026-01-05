package dev.sadakat.thinkfaster.presentation.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.sadakat.thinkfaster.domain.model.DailyStatistics
import dev.sadakat.thinkfaster.domain.model.MonthlyStatistics
import dev.sadakat.thinkfaster.domain.model.StatsPeriod
import dev.sadakat.thinkfaster.domain.model.UsageTrend
import dev.sadakat.thinkfaster.domain.model.WeeklyStatistics
import dev.sadakat.thinkfaster.presentation.stats.charts.HorizontalTimePatternChart
import dev.sadakat.thinkfaster.presentation.stats.charts.GoalProgressChart
import dev.sadakat.thinkfaster.presentation.stats.charts.GoalProgressTimeline
import dev.sadakat.thinkfaster.presentation.stats.components.GoalComplianceCalendar
import dev.sadakat.thinkfaster.presentation.stats.components.ErrorStateCard
import dev.sadakat.thinkfaster.presentation.stats.components.SmartInsightsCard
import dev.sadakat.thinkfaster.presentation.stats.components.EmptyStatsCard
import dev.sadakat.thinkfaster.presentation.stats.components.InsightCardSkeleton
import dev.sadakat.thinkfaster.presentation.stats.components.OverviewStatsCard
import dev.sadakat.thinkfaster.presentation.stats.components.StreakConsistencyCard
import dev.sadakat.thinkfaster.presentation.stats.charts.AppBreakdownChart
import dev.sadakat.thinkfaster.ui.components.rememberFadeInAnimation
import androidx.compose.ui.graphics.graphicsLayer
import org.koin.androidx.compose.koinViewModel

/**
 * Statistics screen displaying usage analytics and trends
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavHostController,
    viewModel: StatsViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Phase 5.1: Card entrance animation state
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            showContent = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Statistics",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Phase 2.1: Refresh button with loading indicator
                    IconButton(
                        onClick = { viewModel.loadStatistics() },
                        enabled = !uiState.isRefreshing
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Statistics content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 0.dp,
                    end = 0.dp,
                    top = paddingValues.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time range selector
                item {
                    TimeRangeSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.selectPeriod(it) }
                    )
                }

                // Phase 1: Error state handling
                uiState.error?.let { errorMessage ->
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ErrorStateCard(
                                errorMessage = errorMessage,
                                onRetry = { viewModel.loadStatistics() },
                                onDismiss = { viewModel.clearError() }
                            )
                        }
                    }
                }

                // Phase 1: Empty state handling - check if any stats data exists
                val hasAnyData = uiState.dailyStats != null ||
                        uiState.weeklyStats != null ||
                        uiState.monthlyStats != null

                if (!hasAnyData && uiState.error == null) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EmptyStatsCard(
                                onNavigateToManageApps = {
                                    navController.navigate("manage_apps")
                                }
                            )
                        }
                    }
                }

                // Goal Progress Timeline - Recent progress visualization
                if (uiState.selectedPeriod == StatsPeriod.WEEKLY) item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        GoalProgressTimeline(
                            sessions = uiState.weeklySessions,
                            dailyGoalMinutes = uiState.goalProgress
                                .firstOrNull()?.goal?.dailyLimitMinutes
                        )
                    }
                }

                // App Breakdown Donut Chart
                if (uiState.appBreakdown.isNotEmpty()) {
                    item {
                        val alpha = rememberFadeInAnimation(durationMillis = 600)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .graphicsLayer(alpha = alpha)
                        ) {
                            AppBreakdownChart(appUsageMap = uiState.appBreakdown)
                        }
                    }
                }

                when (uiState.selectedPeriod) {
                    StatsPeriod.DAILY -> {
                        uiState.dailyStats?.let { stats ->
                           /* item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    DailyStatsContent(stats, uiState.dailyTrend)
                                }
                            }*/

                            /* // Time Pattern Chart - Behavioral insights
                            item {
                                ChartCard(
                                    title = "Peak Usage Times",
                                    description = "Which parts of the day you use apps most"
                                ) {
                                    HorizontalTimePatternChart(
                                        sessions = uiState.dailySessions
                                    )
                                }
                            }
                            */

                            // Smart Insights Card - insights before goal progress
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    SmartInsightsCard(
                                        sessions = uiState.dailySessions,
                                        period = StatsPeriod.DAILY,
                                        trend = uiState.dailyTrend,
                                        streakConsistency = uiState.streakConsistency
                                    )
                                }
                            }

                            /* // Goal Progress Chart - Motivational tracking
                            item {
                                ChartCard(
                                    title = "Goal Progress",
                                    description = "Tracking your daily usage goal"
                                ) {
                                    GoalProgressChart(
                                        sessions = uiState.dailySessions,
                                        period = ChartPeriod.DAILY,
                                        dailyGoalMinutes = uiState.goalProgress
                                            .firstOrNull()?.goal?.dailyLimitMinutes
                                    )
                                }
                            }
                            */

                      /*      // Goal Progress Timeline - Recent progress visualization
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    GoalProgressTimeline(
                                        sessions = uiState.dailySessions,
                                        dailyGoalMinutes = uiState.goalProgress
                                            .firstOrNull()?.goal?.dailyLimitMinutes
                                    )
                                }
                            }*/
                        }
                    }

                    StatsPeriod.WEEKLY -> {
                        uiState.weeklyStats?.let { stats ->
                       /*     item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    WeeklyStatsContent(stats, uiState.weeklyTrend)
                                }
                            }*/

                            /* // Time Pattern Chart
                            item {
                                ChartCard(
                                    title = "Peak Usage Times",
                                    description = "Your average usage patterns this week"
                                ) {
                                    HorizontalTimePatternChart(
                                        sessions = uiState.weeklySessions
                                    )
                                }
                            }
                            */

                            // Smart Insights Card - insights before goal progress
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    SmartInsightsCard(
                                        sessions = uiState.weeklySessions,
                                        period = StatsPeriod.WEEKLY,
                                        trend = uiState.weeklyTrend,
                                        streakConsistency = uiState.streakConsistency
                                    )
                                }
                            }

                            /* // Goal Progress Chart
                            item {
                                ChartCard(
                                    title = "Goal Progress",
                                    description = "Daily progress against your goal"
                                ) {
                                    GoalProgressChart(
                                        sessions = uiState.weeklySessions,
                                        period = ChartPeriod.WEEKLY,
                                        dailyGoalMinutes = uiState.goalProgress
                                            .firstOrNull()?.goal?.dailyLimitMinutes
                                    )
                                }
                            }
                            */


                        }
                    }

                    StatsPeriod.MONTHLY -> {
                        uiState.monthlyStats?.let { stats ->
                   /*         item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    MonthlyStatsContent(stats, uiState.monthlyTrend)
                                }
                            }*/

                            // Goal Compliance Calendar - Monthly compliance visualization
                            if (uiState.goalComplianceData.isNotEmpty()) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        GoalComplianceCalendar(
                                            complianceData = uiState.goalComplianceData,
                                            monthOffset = uiState.calendarMonthOffset,
                                            onPreviousMonth = { viewModel.selectPreviousMonth() },
                                            onNextMonth = { viewModel.selectNextMonth() }
                                        )
                                    }
                                }
                            }

                            /* // Time Pattern Chart
                            item {
                                ChartCard(
                                    title = "Peak Usage Times",
                                    description = "Weekly usage patterns"
                                ) {
                                    HorizontalTimePatternChart(
                                        sessions = uiState.monthlySessions
                                    )
                                }
                            }
                            */

                            // Smart Insights Card - insights before goal progress
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    SmartInsightsCard(
                                        sessions = uiState.monthlySessions,
                                        period = StatsPeriod.MONTHLY,
                                        trend = uiState.monthlyTrend,
                                        streakConsistency = uiState.streakConsistency
                                    )
                                }
                            }

                            /* // Goal Progress Chart
                            item {
                                ChartCard(
                                    title = "Goal Progress",
                                    description = "Monthly progress tracking"
                                ) {
                                    GoalProgressChart(
                                        sessions = uiState.monthlySessions,
                                        period = ChartPeriod.MONTHLY,
                                        dailyGoalMinutes = uiState.goalProgress
                                            .firstOrNull()?.goal?.dailyLimitMinutes
                                    )
                                }
                            }
                            */
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyStatsContent(stats: DailyStatistics, trend: UsageTrend?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Phase 3: Enhanced header with divider
        Column {
            Text(
                text = "Today's Usage",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            )
        }

        // Phase 3: Enhanced trend card with prominence
        trend?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    TrendCard(it)
                }
            }
        }

        // Main stats card
        StatCard(
            title = "Total Time",
            value = stats.formatTotalUsage(),
            icon = "⏱️"
        )

        // Session breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Sessions",
                value = stats.sessionCount.toString(),
                icon = "📱"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Avg Session",
                value = stats.formatAverageSession(),
                icon = "⏳"
            )
        }

        // Interaction stats
        InteractionStatsCard(
            reminders = stats.reminderShownCount,
            alerts = stats.timerAlertsCount,
            proceeds = stats.proceedClickCount
        )
    }
}

@Composable
private fun WeeklyStatsContent(stats: WeeklyStatistics, trend: UsageTrend?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Phase 3: Enhanced header with divider
        Column {
            Text(
                text = "This Week",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stats.weekStart} to ${stats.weekEnd}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            )
        }

        // Phase 3: Enhanced trend card with prominence
        trend?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    TrendCard(it)
                }
            }
        }

        // Main stats card
        StatCard(
            title = "Total Time",
            value = stats.formatTotalUsage(),
            icon = "⏱️"
        )

        // Daily average
        StatCard(
            title = "Daily Average",
            value = stats.formatDailyAverage(),
            icon = "📊"
        )

        // Session breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Sessions",
                value = stats.sessionCount.toString(),
                icon = "📱"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Longest",
                value = stats.formatLongestSession(),
                icon = "⏰"
            )
        }
    }
}

@Composable
private fun MonthlyStatsContent(stats: MonthlyStatistics, trend: UsageTrend?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Phase 3: Enhanced header with divider
        Column {
            Text(
                text = stats.monthName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            )
        }

        // Phase 3: Enhanced trend card with prominence
        trend?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    TrendCard(it)
                }
            }
        }

        // Main stats card
        StatCard(
            title = "Total Time",
            value = stats.formatTotalUsage(),
            icon = "⏱️"
        )

        // Daily average
        StatCard(
            title = "Daily Average",
            value = stats.formatDailyAverage(),
            icon = "📊"
        )

        // Yearly projection
        StatCard(
            title = "Yearly Projection",
            value = stats.formatYearlyProjection(),
            icon = "📈",
            subtitle = "If usage continues at this rate"
        )

        // Session breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Sessions",
                value = stats.sessionCount.toString(),
                icon = "📱"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Avg Session",
                value = stats.formatDailyAverage(),
                icon = "⏳"
            )
        }
    }
}

@Composable
private fun TrendCard(trend: UsageTrend) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trend.getEmoji(),
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = trend.formatMessage(),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InteractionStatsCard(reminders: Int, alerts: Int, proceeds: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Interactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔔", fontSize = 24.sp)
                    Text(
                        text = reminders.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reminders",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⏰", fontSize = 24.sp)
                    Text(
                        text = alerts.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Alerts",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "✅", fontSize = 24.sp)
                    Text(
                        text = proceeds.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Proceeds",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDuration(durationMillis: Long): String {
    val hours = durationMillis / (1000 * 60 * 60)
    val minutes = (durationMillis / (1000 * 60)) % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}

/**
 * Reusable card for displaying charts
 */
@Composable
private fun ChartCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

/**
 * Time range selector - horizontal chip row for period selection
 * Replaces the tab-based navigation with a cleaner chip design
 */
@Composable
private fun TimeRangeSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Today chip
        TimeRangeChip(
            label = "Today",
            isSelected = selectedPeriod == StatsPeriod.DAILY,
            onClick = { onPeriodSelected(StatsPeriod.DAILY) },
            modifier = Modifier.weight(1f)
        )

        // Week chip
        TimeRangeChip(
            label = "Week",
            isSelected = selectedPeriod == StatsPeriod.WEEKLY,
            onClick = { onPeriodSelected(StatsPeriod.WEEKLY) },
            modifier = Modifier.weight(1f)
        )

        // Month chip
        TimeRangeChip(
            label = "Month",
            isSelected = selectedPeriod == StatsPeriod.MONTHLY,
            onClick = { onPeriodSelected(StatsPeriod.MONTHLY) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual time range chip component
 */
@Composable
private fun TimeRangeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
