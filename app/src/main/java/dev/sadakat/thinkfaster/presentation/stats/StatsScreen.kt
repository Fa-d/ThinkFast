package dev.sadakat.thinkfaster.presentation.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import dev.sadakat.thinkfaster.presentation.stats.charts.StackedBarUsageChart
import dev.sadakat.thinkfaster.presentation.stats.charts.HorizontalTimePatternChart
import dev.sadakat.thinkfaster.presentation.stats.charts.GoalProgressChart
import dev.sadakat.thinkfaster.presentation.stats.components.SmartInsightCard
import dev.sadakat.thinkfaster.presentation.stats.components.PredictiveInsightsCard
import dev.sadakat.thinkfaster.presentation.stats.components.BehavioralInsightsCard
import dev.sadakat.thinkfaster.presentation.stats.components.InterventionEffectivenessCard
import dev.sadakat.thinkfaster.presentation.stats.components.ComparativeAnalyticsCard
import dev.sadakat.thinkfaster.presentation.stats.components.GoalComplianceCalendar
import dev.sadakat.thinkfaster.presentation.stats.components.ErrorStateCard
import dev.sadakat.thinkfaster.presentation.stats.components.EmptyStatsCard
import dev.sadakat.thinkfaster.presentation.stats.components.InsightCardSkeleton
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
                title = { Text("Statistics") },
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Period selector tabs - directly below TopAppBar
                TabRow(
                    selectedTabIndex = uiState.selectedPeriod.ordinal,
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                ) {
                    Tab(
                        selected = uiState.selectedPeriod == StatsPeriod.DAILY,
                        onClick = { viewModel.selectPeriod(StatsPeriod.DAILY) },
                        text = { Text("Today") }
                    )
                    Tab(
                        selected = uiState.selectedPeriod == StatsPeriod.WEEKLY,
                        onClick = { viewModel.selectPeriod(StatsPeriod.WEEKLY) },
                        text = { Text("Week") }
                    )
                    Tab(
                        selected = uiState.selectedPeriod == StatsPeriod.MONTHLY,
                        onClick = { viewModel.selectPeriod(StatsPeriod.MONTHLY) },
                        text = { Text("Month") }
                    )
                }

                // Statistics content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = contentPadding.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Phase 1: Error state handling
                    uiState.error?.let { errorMessage ->
                        item {
                            ErrorStateCard(
                                errorMessage = errorMessage,
                                onRetry = { viewModel.loadStatistics() },
                                onDismiss = { viewModel.clearError() }
                            )
                        }
                    }

                    // Phase 1: Empty state handling - check if any stats data exists
                    val hasAnyData = uiState.dailyStats != null ||
                            uiState.weeklyStats != null ||
                            uiState.monthlyStats != null

                    if (!hasAnyData && uiState.error == null) {
                        item {
                            EmptyStatsCard(
                                onNavigateToManageApps = {
                                    navController.navigate("manage_apps")
                                }
                            )
                        }
                    }

                    // Phase 5: Smart Insight - Featured insight (shown on all tabs)
                    // Phase 2.2: Show skeleton while loading
                    // Phase 5.1: Added entrance animation
                    if (hasAnyData) {
                        val smartInsight = uiState.smartInsight
                        if (smartInsight != null) {
                            item {
                                AnimatedVisibility(
                                    visible = showContent,
                                    enter = fadeIn(animationSpec = tween(400)) +
                                            slideInVertically(
                                                initialOffsetY = { it / 4 },
                                                animationSpec = tween(400)
                                            )
                                ) {
                                    SmartInsightCard(insight = smartInsight)
                                }
                            }
                        } else if (uiState.isRefreshing) {
                            item {
                                InsightCardSkeleton()
                            }
                        }
                    }

                    when (uiState.selectedPeriod) {
                        StatsPeriod.DAILY -> {
                            uiState.dailyStats?.let { stats ->
                                item { DailyStatsContent(stats, uiState.dailyTrend) }

                                // Phase 5: Predictive Insights
                                uiState.predictiveInsights?.let { predictive ->
                                    item {
                                        PredictiveInsightsCard(insights = predictive)
                                    }
                                }

                                // Stacked Bar Chart - Primary usage visualization
                                item {
                                    ChartCard(
                                        title = "Usage Breakdown",
                                        description = "Your hourly usage today for Facebook and Instagram"
                                    ) {
                                        StackedBarUsageChart(
                                            sessions = uiState.dailySessions,
                                            period = ChartPeriod.DAILY
                                        )
                                    }
                                }

                                // Time Pattern Chart - Behavioral insights
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

                                // Goal Progress Chart - Motivational tracking
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
                            }
                        }
                        StatsPeriod.WEEKLY -> {
                            uiState.weeklyStats?.let { stats ->
                                item { WeeklyStatsContent(stats, uiState.weeklyTrend) }

                                // Phase 5: Behavioral Insights
                                uiState.behavioralInsights?.let { behavioral ->
                                    item {
                                        BehavioralInsightsCard(insights = behavioral)
                                    }
                                }

                                // Phase 5: Intervention Effectiveness
                                uiState.interventionInsights?.let { intervention ->
                                    item {
                                        InterventionEffectivenessCard(insights = intervention)
                                    }
                                }

                                // Phase 5: Comparative Analytics
                                uiState.comparativeAnalytics?.let { comparative ->
                                    item {
                                        ComparativeAnalyticsCard(analytics = comparative)
                                    }
                                }

                                // Stacked Bar Chart
                                item {
                                    ChartCard(
                                        title = "Usage Breakdown",
                                        description = "Daily usage this week"
                                    ) {
                                        StackedBarUsageChart(
                                            sessions = uiState.weeklySessions,
                                            period = ChartPeriod.WEEKLY
                                        )
                                    }
                                }

                                // Time Pattern Chart
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

                                // Goal Progress Chart
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
                            }
                        }
                        StatsPeriod.MONTHLY -> {
                            uiState.monthlyStats?.let { stats ->
                                item { MonthlyStatsContent(stats, uiState.monthlyTrend) }

                                // Phase 5: Goal Compliance Calendar
                                // Phase 4.3: Added month navigation
                                if (uiState.goalComplianceData.isNotEmpty()) {
                                    item {
                                        GoalComplianceCalendar(
                                            complianceData = uiState.goalComplianceData,
                                            monthOffset = uiState.calendarMonthOffset,
                                            onPreviousMonth = { viewModel.selectPreviousMonth() },
                                            onNextMonth = { viewModel.selectNextMonth() }
                                        )
                                    }
                                }

                                // Phase 5: Comparative Analytics (for monthly view)
                                uiState.comparativeAnalytics?.let { comparative ->
                                    item {
                                        ComparativeAnalyticsCard(analytics = comparative)
                                    }
                                }

                                // Stacked Bar Chart
                                item {
                                    ChartCard(
                                        title = "Usage Breakdown",
                                        description = "Daily usage this month"
                                    ) {
                                        StackedBarUsageChart(
                                            sessions = uiState.monthlySessions,
                                            period = ChartPeriod.MONTHLY
                                        )
                                    }
                                }

                                // Time Pattern Chart
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

                                // Goal Progress Chart
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
                            }
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
