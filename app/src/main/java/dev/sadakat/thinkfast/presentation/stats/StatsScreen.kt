package dev.sadakat.thinkfast.presentation.stats

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.sadakat.thinkfast.domain.model.DailyStatistics
import dev.sadakat.thinkfast.domain.model.MonthlyStatistics
import dev.sadakat.thinkfast.domain.model.UsageTrend
import dev.sadakat.thinkfast.domain.model.WeeklyStatistics
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.graphics.Color as ComposeColor

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                actions = {
                    IconButton(onClick = { viewModel.loadStatistics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(contentPadding)
            ) {
                // Period selector tabs
                TabRow(selectedTabIndex = uiState.selectedPeriod.ordinal) {
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (uiState.selectedPeriod) {
                        StatsPeriod.DAILY -> {
                            uiState.dailyStats?.let { stats ->
                                item { DailyStatsContent(stats, uiState.dailyTrend) }
                                item {
                                    SessionDistributionCard(
                                        sessions = uiState.dailySessions,
                                        periodLabel = "Today",
                                        chartPeriod = ChartPeriod.DAILY
                                    )
                                }
                            }
                        }
                        StatsPeriod.WEEKLY -> {
                            uiState.weeklyStats?.let { stats ->
                                item { WeeklyStatsContent(stats, uiState.weeklyTrend) }
                                item {
                                    SessionDistributionCard(
                                        sessions = uiState.weeklySessions,
                                        periodLabel = "This Week",
                                        chartPeriod = ChartPeriod.WEEKLY
                                    )
                                }
                            }
                        }
                        StatsPeriod.MONTHLY -> {
                            uiState.monthlyStats?.let { stats ->
                                item { MonthlyStatsContent(stats, uiState.monthlyTrend) }
                                item {
                                    SessionDistributionCard(
                                        sessions = uiState.monthlySessions,
                                        periodLabel = stats.monthName,
                                        chartPeriod = ChartPeriod.MONTHLY
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

@Composable
private fun DailyStatsContent(stats: DailyStatistics, trend: UsageTrend?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Text(
            text = "Today's Usage",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Trend card
        trend?.let { TrendCard(it) }

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

        // App breakdown
        AppBreakdownCard(
            facebookUsage = stats.facebookUsageMillis,
            instagramUsage = stats.instagramUsageMillis
        )

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
        // Header
        Text(
            text = "This Week",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "${stats.weekStart} to ${stats.weekEnd}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Trend card
        trend?.let { TrendCard(it) }

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

        // App breakdown
        AppBreakdownCard(
            facebookUsage = stats.facebookUsageMillis,
            instagramUsage = stats.instagramUsageMillis
        )
    }
}

@Composable
private fun MonthlyStatsContent(stats: MonthlyStatistics, trend: UsageTrend?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Text(
            text = stats.monthName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Trend card
        trend?.let { TrendCard(it) }

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

        // App breakdown
        AppBreakdownCard(
            facebookUsage = stats.facebookUsageMillis,
            instagramUsage = stats.instagramUsageMillis
        )
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
private fun AppBreakdownCard(facebookUsage: Long, instagramUsage: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "App Breakdown",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Facebook
            AppUsageRow(
                appName = "Facebook",
                usageMillis = facebookUsage,
                icon = "📘"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Instagram
            AppUsageRow(
                appName = "Instagram",
                usageMillis = instagramUsage,
                icon = "📸"
            )
        }
    }
}

@Composable
private fun AppUsageRow(appName: String, usageMillis: Long, icon: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = appName,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = formatDuration(usageMillis),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
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
 * Card displaying the session duration over time for both apps
 */
@Composable
private fun SessionDistributionCard(
    sessions: List<dev.sadakat.thinkfast.domain.model.UsageSession>,
    periodLabel: String,
    chartPeriod: ChartPeriod
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
                text = "Session Duration Over Time",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Facebook and Instagram session durations for $periodLabel",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sessions recorded yet",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                SessionDurationTimeChart(
                    sessions = sessions,
                    period = chartPeriod,
                    modifier = Modifier.fillMaxWidth(),
                    facebookColor = ComposeColor(0xFF1877F2),  // Facebook blue
                    instagramColor = ComposeColor(0xFFE4405F) // Instagram pink
                )
            }
        }
    }
}
