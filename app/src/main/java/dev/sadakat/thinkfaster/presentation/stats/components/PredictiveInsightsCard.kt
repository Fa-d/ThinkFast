package dev.sadakat.thinkfaster.presentation.stats.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.domain.model.PredictiveInsights

/**
 * Card displaying predictive insights for anticipating usage patterns
 * Phase 4: Help users prepare for high-risk times
 */
@Composable
fun PredictiveInsightsCard(
    insights: PredictiveInsights,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Determine card color based on streak risk
    val containerColor = if (insights.streakAtRisk) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val onContainerColor = if (insights.streakAtRisk) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (insights.streakAtRisk) "⚡" else "🔮",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (insights.streakAtRisk) "Streak Alert" else "Predictions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = onContainerColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = insights.getPrimaryInsight(),
                            fontSize = 14.sp,
                            color = onContainerColor.copy(alpha = 0.8f)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = onContainerColor
                )
            }

            // Expanded Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current Usage Progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Usage",
                                fontSize = 14.sp,
                                color = onContainerColor.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${insights.currentUsagePercentage}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = onContainerColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (insights.currentUsagePercentage / 100f).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                            color = if (insights.streakAtRisk) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            trackColor = onContainerColor.copy(alpha = 0.2f)
                        )
                    }

                    // Streak Warning
                    insights.getStreakWarning()?.let { warning ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = warning,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = onContainerColor
                            )
                        }
                    }

                    // Projection
                    PredictionRow(
                        icon = "📊",
                        label = "Projected for Today",
                        value = "${insights.projectedEndOfDayUsageMinutes}m",
                        onContainerColor = onContainerColor
                    )

                    // Goal Probability
                    PredictionRow(
                        icon = if (insights.willMeetGoal) "✅" else "⚠️",
                        label = "Goal Achievement",
                        value = insights.formatGoalProbability(),
                        onContainerColor = onContainerColor
                    )

                    // High-Risk Time Alert
                    insights.getHighRiskAlert()?.let { alert ->
                        PredictionRow(
                            icon = "⏰",
                            label = "Upcoming High-Risk Time",
                            value = alert.substringAfter("around "),
                            onContainerColor = onContainerColor
                        )
                    }

                    // High-Risk Time Windows
                    if (insights.highRiskTimeWindows.isNotEmpty()) {
                        Text(
                            text = "High-Risk Times (based on your patterns):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = onContainerColor
                        )

                        insights.highRiskTimeWindows.take(3).forEach { window ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${window.timeLabel}",
                                    fontSize = 12.sp,
                                    color = onContainerColor.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${window.historicalFrequency} sessions, avg ${window.averageUsageMinutes}m",
                                    fontSize = 11.sp,
                                    color = onContainerColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Recommended Actions
                    if (insights.recommendedActions.isNotEmpty()) {
                        Text(
                            text = "Suggestions:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = onContainerColor
                        )

                        insights.recommendedActions.forEach { action ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "• ",
                                    fontSize = 12.sp,
                                    color = onContainerColor.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = action,
                                    fontSize = 12.sp,
                                    color = onContainerColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Row for displaying prediction details
 */
@Composable
private fun PredictionRow(
    icon: String,
    label: String,
    value: String,
    onContainerColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = onContainerColor.copy(alpha = 0.7f)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = onContainerColor
        )
    }
}
