package dev.sadakat.thinkfaster.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.analytics.AnalyticsManager
import dev.sadakat.thinkfaster.domain.model.ContentEffectivenessStats
import dev.sadakat.thinkfaster.domain.model.OverallAnalytics
import dev.sadakat.thinkfaster.ui.theme.getMaxContentWidth
import dev.sadakat.thinkfaster.ui.theme.IntentlyerTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext

/**
 * Debug analytics screen for Phase G: Effectiveness Tracking
 * Shows intervention performance metrics and content effectiveness
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAnalytics()
    }

    IntentlyerTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Intervention Analytics") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadAnalytics() }) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    }
                )
            }
        ) { padding ->
            when (val state = uiState) {
                is AnalyticsUiState.Loading -> LoadingView(modifier = Modifier.padding(padding))
                is AnalyticsUiState.Success -> SuccessView(
                    state = state,
                    modifier = Modifier.padding(padding)
                )
                is AnalyticsUiState.Error -> ErrorView(
                    message = state.message,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading analytics...")
        }
    }
}

@Composable
private fun ErrorView(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Error loading analytics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Retry */ }) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun SuccessView(
    state: AnalyticsUiState.Success,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val maxWidth = getMaxContentWidth()
        LazyColumn(
            modifier = Modifier.widthIn(max = maxWidth),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Overall Stats
        item {
            OverallStatsCard(analytics = state.analytics)
        }

        // Anonymous Analytics toggle (hidden)
        // item {
        //     AnonymousAnalyticsCard()
        // }

        // Success Metrics
        item {
            SuccessMetricsCard(analytics = state.analytics)
        }

        // Phase 4: RL A/B Testing Metrics
        state.rlMetrics?.let { metrics ->
            item {
                RLMetricsCard(metrics = metrics)
            }
        }

        // Phase 4: RL Content Effectiveness (Thompson Sampling)
        state.rlContentEffectiveness?.let { contentEffectiveness ->
            if (contentEffectiveness.isNotEmpty()) {
                item {
                    RLContentEffectivenessCard(contentEffectiveness = contentEffectiveness)
                }
            }
        }

        // Phase 4: Timing Effectiveness (Personalized Timing)
        if (state.hasReliableTimingData && state.timingEffectiveness != null) {
            item {
                TimingEffectivenessCard(
                    timingData = state.timingEffectiveness,
                    hasReliableData = state.hasReliableTimingData
                )
            }
        }

        // Underperforming Content (Phase G)
        if (state.underperformingContent.isNotEmpty()) {
            item {
                UnderperformingContentCard(underperformingContent = state.underperformingContent)
            }
        }

        // Content Effectiveness (only show if data available)
        if (state.contentEffectiveness.isNotEmpty()) {
            item {
                Text(
                    text = "Content Effectiveness",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(state.contentEffectiveness) { content ->
                ContentEffectivenessCard(content)
            }
        }

        // Stats by App (only show if data available)
        if (state.appStats.isNotEmpty()) {
            item {
                Text(
                    text = "Stats by App",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(state.appStats.entries.toList()) { (app, stats) ->
                AppStatsCard(appName = app, stats = stats)
            }
        }
        }
    }
}

@Composable
private fun OverallStatsCard(analytics: OverallAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overall Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn("Total", analytics.totalInterventions.toString())
                StatColumn("Go Back", analytics.goBackCount.toString())
                StatColumn("Proceed", analytics.proceedCount.toString())
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SuccessMetricsCard(analytics: OverallAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Success Metrics (Target in Green)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            SuccessMetricRow(
                label = "Dismissal Rate",
                value = "${String.format("%.1f", analytics.dismissalRate)}%",
                target = "30%+",
                isTargetMet = analytics.dismissalRate >= 30.0
            )

            Spacer(modifier = Modifier.height(12.dp))

            SuccessMetricRow(
                label = "Avg Decision Time",
                value = "${String.format("%.1f", analytics.avgDecisionTimeSeconds)}s",
                target = "8s+",
                isTargetMet = analytics.avgDecisionTimeSeconds >= 8.0
            )

            Spacer(modifier = Modifier.height(12.dp))

            SuccessMetricRow(
                label = "Avg Session After",
                value = "${String.format("%.1f", analytics.avgSessionAfterInterventionMinutes)}m",
                target = "<15m",
                isTargetMet = analytics.avgSessionAfterInterventionMinutes < 15.0
            )
        }
    }
}

@Composable
private fun SuccessMetricRow(
    label: String,
    value: String,
    target: String,
    isTargetMet: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isTargetMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Target: $target",
            fontSize = 12.sp,
            color = if (isTargetMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ContentEffectivenessCard(content: ContentEffectivenessStats) {
    val dismissalColor = when {
        content.dismissalRate >= 40 -> Color(0xFF4CAF50)  // Green - Excellent
        content.dismissalRate >= 25 -> Color(0xFFFFC107)  // Amber - Good
        else -> Color(0xFFF44336)  // Red - Needs improvement
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with content type and dismissal rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = content.contentType,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${content.total} shown",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.1f", content.dismissalRate)}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = dismissalColor
                    )
                    Text(
                        text = "go back",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Additional statistics
            if (content.avgDecisionTimeMs != null || content.avgFinalDurationMs != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avg decision time
                    if (content.avgDecisionTimeMs != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Decision Time",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%.1f", content.avgDecisionTimeSeconds)}s",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Avg session duration
                    if (content.avgFinalDurationMs != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "After Session",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%.1f", content.avgFinalDurationMinutes)}m",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppStatsCard(appName: String, stats: dev.sadakat.thinkfaster.domain.repository.AppInterventionStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${stats.totalInterventions} interventions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats.dismissalRate.toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.dismissalRate >= 30) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "go back",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Phase G: Shows content types that need improvement
 * These are flagged based on low dismissal rates or insufficient data
 */
@Composable
private fun UnderperformingContentCard(underperformingContent: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF4E6) // Warm amber background for attention
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Content Improvement Needed",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100) // Dark amber text
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The following content types need attention:",
                fontSize = 14.sp,
                color = Color(0xFFBF360C)
            )
            Spacer(modifier = Modifier.height(12.dp))
            underperformingContent.forEach { contentType ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• ",
                        fontSize = 16.sp,
                        color = Color(0xFFBF360C)
                    )
                    Text(
                        text = contentType,
                        fontSize = 14.sp,
                        color = Color(0xFFBF360C)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Consider refreshing or improving these content types.",
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = Color(0xFFBF360C).copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Anonymous Analytics toggle card
 * Allows users to opt-out of privacy-safe analytics
 */
@Composable
private fun AnonymousAnalyticsCard() {
    val analyticsManager = remember { GlobalContext.get().get<AnalyticsManager>() }
    var isEnabled by remember { mutableStateOf(analyticsManager.isAnalyticsEnabled()) }

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
                    Text(text = "📊", fontSize = 24.sp)
                    Text(
                        text = "Anonymous Analytics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isEnabled)
                        "Help improve Intently with anonymous data"
                    else
                        "No analytics data is collected",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = { enabled ->
                    isEnabled = enabled
                    // Use AnalyticsManager to properly sync with Firebase
                    analyticsManager.setAnalyticsEnabled(enabled)
                }
            )
        }
    }
}

/**
 * Phase 4: RL A/B Testing Metrics Card
 * Shows performance comparison between Control (rule-based) and RL Treatment (Thompson Sampling)
 */
@Composable
private fun RLMetricsCard(metrics: dev.sadakat.thinkfaster.domain.intervention.RLEffectivenessMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (metrics.shouldRollback) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🧪 A/B Test: RL vs Control",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (metrics.shouldRollback) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                Text(
                    text = if (metrics.isEnabled) "Active" else "Disabled",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (metrics.isEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Effectiveness scores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "RL Treatment",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${(metrics.rlScore * 100).toInt()}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Control",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${(metrics.controlScore * 100).toInt()}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Rollout",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${metrics.rolloutPercentage}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Performance summary
            Text(
                text = metrics.rlPerformance,
                fontSize = 14.sp,
                color = when {
                    metrics.shouldRollback -> MaterialTheme.colorScheme.error
                    metrics.rlScore > metrics.controlScore -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                },
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (metrics.shouldRollback) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ RL underperforming - automatic rollback triggered",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * Phase 4: RL Content Effectiveness Card
 * Shows Thompson Sampling results - estimated success rate for each content type
 */
@Composable
private fun RLContentEffectivenessCard(
    contentEffectiveness: List<dev.sadakat.thinkfaster.domain.intervention.ContentEffectiveness>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎯 RL Content Performance (Thompson Sampling)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Learned success rates from reinforcement learning",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sort by estimated success rate (best first)
            val sortedContent = contentEffectiveness.sortedByDescending { it.estimatedSuccessRate }

            sortedContent.forEach { content ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = content.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "n=${content.totalShown}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }

                    // Success rate with color coding
                    val successRateColor = when {
                        content.estimatedSuccessRate >= 0.50f -> Color(0xFF4CAF50)  // Green - Good
                        content.estimatedSuccessRate >= 0.35f -> Color(0xFFFFC107)  // Amber - OK
                        else -> Color(0xFFF44336)  // Red - Needs improvement
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${(content.estimatedSuccessRate * 100).toInt()}%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = successRateColor
                        )
                        Text(
                            text = "success rate",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary text
            if (sortedContent.isNotEmpty()) {
                val bestContent = sortedContent.first()
                Text(
                    text = "Best performer: ${bestContent.displayName} (${(bestContent.estimatedSuccessRate * 100).toInt()}%)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

/**
 * Phase 4: Timing Effectiveness Card
 * Shows which hours of day are most effective for interventions (Personalized Timing learning)
 */
@Composable
private fun TimingEffectivenessCard(
    timingData: Map<Int, Float>,
    hasReliableData: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏰ Optimal Intervention Times",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                if (hasReliableData) {
                    Text(
                        text = "Reliable",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Based on your personal response patterns",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sort by hour and display top effective hours
            val sortedTiming = timingData.entries.sortedByDescending { it.value }.take(8)

            if (sortedTiming.isNotEmpty()) {
                // Show top 4 in grid
                val topTiming = sortedTiming.take(4)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    topTiming.forEach { (hour, effectiveness) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatHour(hour),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(effectiveness * 100).toInt()}%",
                                fontSize = 14.sp,
                                color = when {
                                    effectiveness >= 0.70f -> MaterialTheme.colorScheme.primary
                                    effectiveness >= 0.55f -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Summary text
                val bestHour = sortedTiming.first()
                val worstHour = timingData.entries.minByOrNull { it.value }
                Text(
                    text = "Best time: ${formatHour(bestHour.key)} (${(bestHour.value * 100).toInt()}% success rate)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                worstHour?.let {
                    if (it.value < 0.40f) {
                        Text(
                            text = "Avoid: ${formatHour(it.key)} (${(it.value * 100).toInt()}% success rate)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Text(
                    text = "No timing data available yet",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

/**
 * Format hour (0-23) to 12-hour format with AM/PM
 */
private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}
