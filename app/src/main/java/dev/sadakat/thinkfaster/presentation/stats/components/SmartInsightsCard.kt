package dev.sadakat.thinkfaster.presentation.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sadakat.thinkfaster.domain.model.StatsPeriod
import dev.sadakat.thinkfaster.domain.model.TrendDirection
import dev.sadakat.thinkfaster.domain.model.UsageTrend
import dev.sadakat.thinkfaster.domain.model.UsageSession
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing

/**
 * Smart insights card showing actionable insights
 * Uses the same design as OverviewStatsCard - circular progress ring + metrics
 */
@Composable
fun SmartInsightsCard(
    sessions: List<UsageSession>,
    period: StatsPeriod,
    trend: UsageTrend?,
    streakConsistency: StreakConsistency?,
    modifier: Modifier = Modifier
) {
    val insights = rememberInsights(sessions, period, trend, streakConsistency)

    if (insights.isEmpty()) {
        // Empty state - match OverviewStatsCard style
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = dev.sadakat.thinkfaster.ui.design.tokens.Shapes.card,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Empty state circle
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📊",
                        style = MaterialTheme.typography.displayMedium
                    )
                }

                // Right: Empty state message
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = "Not enough data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Keep tracking to unlock insights",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = dev.sadakat.thinkfaster.ui.design.tokens.Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Circular progress ring with primary insight
            insights.firstOrNull()?.let { primaryInsight ->
                InsightProgressRing(
                    insight = primaryInsight,
                    streakConsistency = streakConsistency
                )
            }

            // Right: Key insight metrics
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                insights.drop(1).take(3).forEach { insight ->
                    InsightMetricRow(insight = insight)
                }
            }
        }
    }
}

/**
 * Circular progress ring showing primary insight
 * Matches OverviewStatsCard design style exactly
 */
@Composable
private fun InsightProgressRing(
    insight: Insight,
    streakConsistency: StreakConsistency?,
    modifier: Modifier = Modifier
) {
    val progressColor = insight.color
    val backgroundArcColor = progressColor.copy(alpha = 0.2f)

    // Calculate progress percentage from streak if available
    val progressPercentage = streakConsistency?.let { streak ->
        if (streak.totalDays > 0) {
            (streak.daysMetGoal.toFloat() / streak.totalDays * 100).toInt()
        } else 0
    } ?: 75 // Default progress

    Box(
        modifier = modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw progress ring
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 12.dp.toPx()
            val diameter = size.minDimension

            // Background arc (full circle)
            drawArc(
                color = backgroundArcColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                ),
                size = androidx.compose.ui.geometry.Size(diameter, diameter)
            )

            // Progress arc
            val sweepAngle = (progressPercentage.toFloat() / 100f) * 360f
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle.coerceIn(0f, 360f),
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                ),
                size = androidx.compose.ui.geometry.Size(diameter, diameter)
            )
        }

        // Center content - match OverviewStatsCard style
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = insight.centerValue,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
            Text(
                text = insight.centerLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Individual insight metric row with icon and text
 * Matches OverviewStatsCard MetricRow style
 */
@Composable
private fun InsightMetricRow(
    insight: Insight,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color dot
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                drawCircle(color = insight.color)
            }
        }

        Column {
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/**
 * Data class for insights
 */
data class Insight(
    val centerValue: String,      // Big text in center (e.g., "75%", "3")
    val centerLabel: String,      // Small label below (e.g., "of goal", "streak")
    val title: String,            // For metric row - value (e.g., "+25%")
    val description: String,      // For metric row - label (e.g., "vs last week")
    val color: Color
)

/**
 * Generate insights from data
 */
@Composable
private fun rememberInsights(
    sessions: List<UsageSession>,
    period: StatsPeriod,
    trend: UsageTrend?,
    streakConsistency: StreakConsistency?
): List<Insight> {
    return remember(sessions, period, trend, streakConsistency) {
        generateInsights(sessions, period, trend, streakConsistency)
    }
}

/**
 * Generate actionable insights from usage data
 */
private fun generateInsights(
    sessions: List<UsageSession>,
    period: StatsPeriod,
    trend: UsageTrend?,
    streakConsistency: StreakConsistency?
): List<Insight> {
    val insights = mutableListOf<Insight>()

    // 1. Streak insight (if available) - primary insight
    streakConsistency?.let { streak ->
        if (streak.currentStreak > 0) {
            insights.add(
                Insight(
                    centerValue = "${streak.currentStreak}",
                    centerLabel = "streak",
                    title = "${streak.currentStreak} days",
                    description = "Current streak",
                    color = Color(0xFFFF6B35) // Orange
                )
            )
        }

        // Compliance rate insight
        val complianceRate = if (streak.totalDays > 0) {
            (streak.daysMetGoal.toFloat() / streak.totalDays * 100).toInt()
        } else 0

        if (complianceRate > 0) {
            val color = when {
                complianceRate >= 80 -> Color(0xFF4CAF50) // Green
                complianceRate >= 50 -> Color(0xFFFFC107) // Yellow
                else -> Color(0xFFF44336) // Red
            }
            insights.add(
                Insight(
                    centerValue = "$complianceRate%",
                    centerLabel = "goal",
                    title = "$complianceRate%",
                    description = "Goal compliance",
                    color = color
                )
            )
        }
    }

    // 2. Trend insight
    trend?.let { usageTrend ->
        val percentageChange = kotlin.math.abs(usageTrend.percentageChange)

        if (percentageChange > 0) {
            when (usageTrend.direction) {
                TrendDirection.INCREASING -> {
                    insights.add(
                        Insight(
                            centerValue = "+$percentageChange%",
                            centerLabel = "change",
                            title = "+$percentageChange%",
                            description = "Vs last ${period.name.lowercase()}",
                            color = Color(0xFFF44336) // Red
                        )
                    )
                }
                TrendDirection.DECREASING -> {
                    insights.add(
                        Insight(
                            centerValue = "-$percentageChange%",
                            centerLabel = "change",
                            title = "-$percentageChange%",
                            description = "Vs last ${period.name.lowercase()}",
                            color = Color(0xFF4CAF50) // Green
                        )
                    )
                }
                TrendDirection.STABLE -> {
                    insights.add(
                        Insight(
                            centerValue = "0%",
                            centerLabel = "change",
                            title = "Stable",
                            description = "No change",
                            color = Color(0xFF9C27B0) // Purple
                        )
                    )
                }
            }
        }
    }

    // 3. Peak time insight
    if (sessions.isNotEmpty()) {
        val timePattern = analyzePeakTime(sessions)
        insights.add(
            Insight(
                centerValue = timePattern,
                centerLabel = "peak",
                title = timePattern,
                description = "Peak usage time",
                color = Color(0xFF2196F3) // Blue
            )
        )
    }

    // 4. Session pattern insight
    if (sessions.isNotEmpty()) {
        val avgSessionDuration = sessions.map { it.duration }.average() / 60000 // to minutes
        val sessionCount = sessions.size

        insights.add(
            Insight(
                centerValue = "${avgSessionDuration.toInt()}m",
                centerLabel = "avg",
                title = "${avgSessionDuration.toInt()}m",
                description = "Avg of $sessionCount sessions",
                color = Color(0xFF3F51B5) // Indigo
            )
        )
    }

    return insights.take(4) // Show top 4 insights (1 in ring, 3 in metrics)
}

/**
 * Analyze peak usage time from sessions
 */
private fun analyzePeakTime(sessions: List<UsageSession>): String {
    val hourCounts = sessions.groupingBy {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = it.startTimestamp
        calendar.get(java.util.Calendar.HOUR_OF_DAY)
    }.eachCount()

    val peakHour = hourCounts.maxByOrNull { it.value }?.key ?: return "All day"

    return when (peakHour) {
        in 6..11 -> "Morning"
        in 12..17 -> "Afternoon"
        in 18..22 -> "Evening"
        else -> "Night"
    }
}
