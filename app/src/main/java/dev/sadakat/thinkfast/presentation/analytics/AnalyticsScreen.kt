package dev.sadakat.thinkfast.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfast.analytics.AnalyticsManager
import dev.sadakat.thinkfast.domain.model.ContentEffectivenessStats
import dev.sadakat.thinkfast.domain.model.OverallAnalytics
import dev.sadakat.thinkfast.ui.theme.ThinkFastTheme
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

    ThinkFastTheme {
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Stats
        item {
            OverallStatsCard(analytics = state.analytics)
        }

        // Anonymous Analytics toggle
        item {
            AnonymousAnalyticsCard()
        }

        // Success Metrics
        item {
            SuccessMetricsCard(analytics = state.analytics)
        }

        // Underperforming Content (Phase G)
        if (state.underperformingContent.isNotEmpty()) {
            item {
                UnderperformingContentCard(underperformingContent = state.underperformingContent)
            }
        }

        // Content Effectiveness
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

        // App Stats
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
private fun AppStatsCard(appName: String, stats: dev.sadakat.thinkfast.domain.repository.AppInterventionStats) {
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
                        "Help improve ThinkFast with anonymous data"
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
