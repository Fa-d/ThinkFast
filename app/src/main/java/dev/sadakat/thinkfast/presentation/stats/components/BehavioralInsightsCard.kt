package dev.sadakat.thinkfast.presentation.stats.components

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
import dev.sadakat.thinkfast.domain.model.BehavioralInsights

/**
 * Card displaying behavioral pattern insights with progressive disclosure
 * Phase 2: Self-awareness & pattern recognition
 */
@Composable
fun BehavioralInsightsCard(
    insights: BehavioralInsights,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                    Text(text = "🧠", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Behavioral Patterns",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = insights.getPrimaryInsight(),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
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
                    // Weekend vs Weekday
                    if (insights.weekendVsWeekdayRatio > 0) {
                        InsightRow(
                            icon = "📅",
                            label = "Weekend vs Weekday",
                            value = insights.formatWeekendComparison()
                        )
                    }

                    // Late Night Pattern
                    insights.getLateNightInsight()?.let { lateNightInsight ->
                        InsightRow(
                            icon = "🌙",
                            label = "Late Night Usage",
                            value = lateNightInsight
                        )
                    }

                    // Quick Reopen Pattern
                    insights.getQuickReopenInsight()?.let { quickReopenInsight ->
                        InsightRow(
                            icon = "🔄",
                            label = "Quick Reopens",
                            value = quickReopenInsight
                        )
                    }

                    // Peak Time
                    if (insights.sessionsInPeakHour > 0) {
                        InsightRow(
                            icon = "📊",
                            label = "Peak Time",
                            value = "${insights.peakUsageContext} (${insights.sessionsInPeakHour} sessions, ${insights.peakHourUsageMinutes}m)"
                        )
                    }

                    // Binge Sessions
                    if (insights.bingeSessionCount > 0) {
                        InsightRow(
                            icon = "⏱️",
                            label = "Extended Sessions",
                            value = "${insights.bingeSessionCount} session${if (insights.bingeSessionCount > 1) "s" else ""} over 30 minutes"
                        )
                    }

                    // Average Session
                    if (insights.averageSessionMinutes > 0) {
                        InsightRow(
                            icon = "⏳",
                            label = "Average Session",
                            value = "${insights.averageSessionMinutes}m per session"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable row for displaying insight details
 */
@Composable
private fun InsightRow(
    icon: String,
    label: String,
    value: String
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
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
