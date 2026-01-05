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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.theme.AppColors

/**
 * Data class for overview statistics display
 */
data class OverviewStats(
    val totalMinutes: Int,
    val goalMinutes: Int?,
    val progressPercentage: Int,
    val sessionCount: Int,
    val avgSessionMinutes: Int
)

/**
 * Hero stats card with circular progress ring (matches Home page design)
 * Shows total usage, goal progress, and key metrics
 */
@Composable
fun OverviewStatsCard(
    stats: OverviewStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.card,
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
            // Left: Circular progress ring
            CircularProgressRing(
                progressPercentage = stats.progressPercentage,
                goalMinutes = stats.goalMinutes
            )

            // Right: Key metrics
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Total time metric
                MetricRow(
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    label = "Total Time",
                    value = formatMinutes(stats.totalMinutes)
                )

                // Sessions count metric
                MetricRow(
                    icon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
                    label = "Sessions",
                    value = stats.sessionCount.toString()
                )

                // Average session metric
                MetricRow(
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    label = "Avg Session",
                    value = formatMinutes(stats.avgSessionMinutes)
                )
            }
        }
    }
}

/**
 * Circular progress ring matching home page design
 * 140dp diameter, 12dp stroke, rounded caps
 */
@Composable
private fun CircularProgressRing(
    progressPercentage: Int,
    goalMinutes: Int?,
    modifier: Modifier = Modifier
) {
    val progressColor = AppColors.Progress.getColorForPercentage(progressPercentage)
    val backgroundArcColor = progressColor.copy(alpha = 0.2f)

    Box(
        modifier = modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw progress ring
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 12.dp.toPx()
            val diameter = size.minDimension
            val radius = (diameter - strokeWidth) / 2

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

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$progressPercentage%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
            if (goalMinutes != null) {
                Text(
                    text = "of goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "usage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Individual metric row with icon, label, and value
 */
@Composable
private fun MetricRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp)) {
            icon()
        }

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Format minutes to human-readable string
 */
private fun formatMinutes(minutes: Int): String {
    return when {
        minutes >= 60 -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
        }
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}
