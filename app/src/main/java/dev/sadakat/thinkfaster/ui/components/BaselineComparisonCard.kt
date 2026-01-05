package dev.sadakat.thinkfaster.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.domain.model.UserBaseline
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.theme.ProgressColors

/**
 * Baseline Comparison Card - Shows user performance vs baseline & population
 * First-Week Retention Feature - Phase 4: UI Components
 *
 * Display rules:
 * - Shows user's current usage vs their personal baseline
 * - Shows user's baseline vs population average
 * - Displays trend arrows and motivational messaging
 * - Only shown after baseline is calculated (Day 7+)
 */
@Composable
fun BaselineComparisonCard(
    baseline: UserBaseline,
    todayUsageMinutes: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.card,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📊", fontSize = 28.sp)
                    Text(
                        text = "Your Progress",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Today vs Your Baseline
                ComparisonRow(
                    label = "Today",
                    value = todayUsageMinutes,
                    comparison = baseline.averageDailyMinutes,
                    comparisonLabel = "Your average"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // Your Baseline vs Population
                ComparisonRow(
                    label = "Your average",
                    value = baseline.averageDailyMinutes,
                    comparison = UserBaseline.POPULATION_AVERAGE_MINUTES,
                    comparisonLabel = "Typical user"
                )

                // Motivational message
                Text(
                    text = baseline.getComparisonMessage(),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    value: Int,
    comparison: Int,
    comparisonLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "$value min",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Trend arrow
        val diff = comparison - value
        val emoji = when {
            diff > 5 -> "↓"  // Better (lower usage)
            diff < -5 -> "↑"  // Worse (higher usage)
            else -> "→"  // Stable
        }
        val color = when {
            diff > 5 -> ProgressColors.OnTrack
            diff < -5 -> ProgressColors.OverLimit
            else -> ProgressColors.Approaching
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = emoji,
                fontSize = 24.sp,
                color = color
            )
            Text(
                text = "vs $comparisonLabel",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
