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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import dev.sadakat.thinkfaster.domain.model.ComparativeAnalytics

/**
 * Card displaying comparative analytics - personal bests and improvements
 * Phase 4: Show progress over time
 */
@Composable
fun ComparativeAnalyticsCard(
    analytics: ComparativeAnalytics,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "📈", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Your Progress",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = analytics.getPrimaryInsight(),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Animated expand/collapse indicator
                if (isExpanded) {
                    Text(
                        text = "▲",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "▼",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
                    // Personal Bests Section
                    Text(
                        text = "🏆 Personal Bests",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Longest Streak
                    if (analytics.personalBests.longestStreak > 0) {
                        ComparisonRow(
                            icon = "🔥",
                            label = "Longest Streak",
                            value = analytics.personalBests.formatLongestStreak(),
                            isPositive = true
                        )
                    }

                    // Best Day
                    analytics.personalBests.lowestUsageDay?.let { bestDay ->
                        ComparisonRow(
                            icon = "⭐",
                            label = "Best Day",
                            value = "${bestDay.formatUsage()}${bestDay.percentBelowAverage?.let { " (${it}% below avg)" } ?: ""}",
                            isPositive = true
                        )
                    }

                    // Best Week
                    analytics.personalBests.lowestUsageWeek?.let { bestWeek ->
                        ComparisonRow(
                            icon = "🌟",
                            label = "Best Week",
                            value = "${bestWeek.formatUsage()}${bestWeek.percentBelowAverage?.let { " (${it}% below avg)" } ?: ""}",
                            isPositive = true
                        )
                    }

                    // Improvement Summary
                    analytics.getImprovementSummary()?.let { summary ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "📊", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = summary,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Comparisons Section
                    if (analytics.comparisons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📊 Comparisons",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        analytics.comparisons.forEach { comparison ->
                            ComparisonDetailRow(
                                label = comparison.label,
                                currentValue = comparison.formatValues(),
                                percentageDiff = comparison.percentageDiff.toInt(),
                                isImprovement = comparison.isImprovement
                            )
                        }
                    }

                    // Consistency Score
                    if (analytics.consistencyScore > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        ComparisonRow(
                            icon = "🎯",
                            label = "Consistency",
                            value = analytics.getConsistencyMessage(),
                            isPositive = analytics.consistencyScore > 0.6f
                        )
                    }
                }
            }
        }
    }
}

/**
 * Row for displaying comparison metric
 */
@Composable
private fun ComparisonRow(
    icon: String,
    label: String,
    value: String,
    isPositive: Boolean
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isPositive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Detailed row for week-to-week comparisons
 */
@Composable
private fun ComparisonDetailRow(
    label: String,
    currentValue: String,
    percentageDiff: Int,
    isImprovement: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isImprovement) "↓" else "↑",
                    fontSize = 16.sp,
                    color = if (isImprovement) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$percentageDiff%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isImprovement) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
        Text(
            text = currentValue,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
