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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.sadakat.thinkfaster.domain.model.InsightPriority
import dev.sadakat.thinkfaster.domain.model.SmartInsight

/**
 * Featured smart insight card - displays the single most relevant insight
 * Phase 5: Intelligent insight selection with priority-based styling
 *
 * Visual design:
 * - URGENT: Red error container (streak at risk, goal exceeded)
 * - HIGH: Amber warning container (behavioral patterns)
 * - MEDIUM: Blue primary container (positive reinforcement)
 * - LOW: Green tertiary container (general awareness)
 */
@Composable
fun SmartInsightCard(
    insight: SmartInsight,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Determine card styling based on priority
    val containerColor = when (insight.priority) {
        InsightPriority.URGENT -> MaterialTheme.colorScheme.errorContainer
        InsightPriority.HIGH -> Color(0xFFFFF3E0) // Amber 50
        InsightPriority.MEDIUM -> MaterialTheme.colorScheme.primaryContainer
        InsightPriority.LOW -> Color(0xFFE8F5E9) // Green 50
    }

    val onContainerColor = when (insight.priority) {
        InsightPriority.URGENT -> MaterialTheme.colorScheme.onErrorContainer
        InsightPriority.HIGH -> Color(0xFFE65100) // Amber 900
        InsightPriority.MEDIUM -> MaterialTheme.colorScheme.onPrimaryContainer
        InsightPriority.LOW -> Color(0xFF1B5E20) // Green 900
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (insight.priority == InsightPriority.URGENT) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(20.dp)
        ) {
            // Header - Featured insight label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (insight.priority) {
                            InsightPriority.URGENT -> "⚠️ ATTENTION"
                            InsightPriority.HIGH -> "💡 INSIGHT"
                            InsightPriority.MEDIUM -> "✨ GOOD NEWS"
                            InsightPriority.LOW -> "📊 STATUS"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = onContainerColor.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Main insight message
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = insight.icon,
                            fontSize = 36.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = insight.message,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = onContainerColor,
                            lineHeight = 24.sp
                        )
                    }
                }

                // Expand/collapse icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = onContainerColor.copy(alpha = 0.6f)
                )
            }

            // Actionable badge (if applicable)
            if (insight.actionable && !isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "💡 Actionable",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = onContainerColor.copy(alpha = 0.8f)
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
                    // Divider
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(vertical = 8.dp)
                    )

                    // Detailed explanation
                    insight.details?.let { details ->
                        Text(
                            text = details,
                            fontSize = 14.sp,
                            color = onContainerColor.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                    }

                    // Actionable indicator (expanded view)
                    if (insight.actionable) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This is something you can act on right now",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = onContainerColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Context data (if relevant)
                    if (insight.contextData.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ContextDataSection(
                            contextData = insight.contextData,
                            onContainerColor = onContainerColor
                        )
                    }

                    // Priority indicator
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Priority: ${insight.priority.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = onContainerColor.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Display context data in a readable format
 */
@Composable
private fun ContextDataSection(
    contextData: Map<String, Any>,
    onContainerColor: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Details:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = onContainerColor.copy(alpha = 0.7f)
        )

        contextData.forEach { (key, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatContextKey(key),
                    fontSize = 12.sp,
                    color = onContainerColor.copy(alpha = 0.6f)
                )
                Text(
                    text = value.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = onContainerColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Format context data keys for display
 */
private fun formatContextKey(key: String): String {
    return when (key) {
        "percentage" -> "Current %"
        "remainingMinutes" -> "Remaining"
        "count" -> "Count"
        "sessions" -> "Sessions"
        "minutes" -> "Minutes"
        "percentageDiff" -> "Difference"
        "contentType" -> "Type"
        "successRate" -> "Success Rate"
        "days" -> "Days"
        "improvement" -> "Improvement"
        "projected" -> "Projected"
        "peakTime" -> "Peak Time"
        "hour" -> "Hour"
        "frequency" -> "Frequency"
        "avgMinutes" -> "Avg Minutes"
        "percentBelow" -> "Below Average"
        else -> key.replaceFirstChar { it.uppercase() }
    }
}
