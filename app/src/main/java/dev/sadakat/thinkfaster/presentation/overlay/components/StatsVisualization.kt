package dev.sadakat.thinkfaster.presentation.overlay.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes

/**
 * Enhanced usage stats visualization
 * Phase 2.5: Visual stats with progress indicators and color coding
 *
 * Features:
 * - Goal progress circle showing current vs target
 * - Color coding (green = under goal, red = over)
 * - Comparison bars for multi-day view
 * - Trend indicators (↑ worse, ↓ better)
 */
@Composable
fun EnhancedUsageStatsContent(
    todayMinutes: Int,
    yesterdayMinutes: Int,
    weekAvgMinutes: Int,
    goalMinutes: Int?,
    textColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    val overGoal = goalMinutes != null && todayMinutes > goalMinutes
    val progressColor = if (overGoal) Color(0xFFF44336) else Color(0xFF4CAF50) // Red or Green

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Goal Progress Circle (if goal is set)
        if (goalMinutes != null) {
            GoalProgressCircle(
                current = todayMinutes,
                goal = goalMinutes,
                progressColor = progressColor,
                textColor = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Goal status text
            Text(
                text = if (overGoal) {
                    "Over goal by ${todayMinutes - goalMinutes} min"
                } else {
                    "${goalMinutes - todayMinutes} min remaining"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = progressColor
            )
        } else {
            // Show large today's usage if no goal
            Text(
                text = "$todayMinutes",
                fontFamily = FontFamily.Monospace,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = "minutes today",
                fontSize = 18.sp,
                color = secondaryTextColor.copy(alpha = 0.95f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Comparison bars
        ComparisonBars(
            today = todayMinutes,
            yesterday = yesterdayMinutes,
            weekAvg = weekAvgMinutes,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor
        )
    }
}

/**
 * Circular progress indicator showing goal progress
 */
@Composable
private fun GoalProgressCircle(
    current: Int,
    goal: Int,
    progressColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(160.dp)
        ) {
            val strokeWidth = 24f
            val radius = (size.minDimension - strokeWidth) / 2

            // Background track
            drawCircle(
                color = progressColor.copy(alpha = 0.2f),
                radius = radius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )

            // Progress arc
            val sweepAngle = (current.toFloat() / goal.toFloat() * 360f).coerceAtMost(360f)
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }

        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$current",
                fontFamily = FontFamily.Monospace,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = "/ $goal min",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.95f)
            )
        }
    }
}

/**
 * Horizontal comparison bars showing today vs yesterday vs week average
 */
@Composable
private fun ComparisonBars(
    today: Int,
    yesterday: Int,
    weekAvg: Int,
    textColor: Color,
    secondaryTextColor: Color
) {
    val maxValue = maxOf(today, yesterday, weekAvg, 1) // Avoid division by zero

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Today
        ComparisonBarItem(
            label = "Today",
            value = today,
            maxValue = maxValue,
            barColor = Color(0xFF2196F3), // Blue
            trend = when {
                yesterday == 0 -> null
                today > yesterday -> "↑" // Worse
                today < yesterday -> "↓" // Better
                else -> null
            },
            textColor = textColor,
            secondaryTextColor = secondaryTextColor
        )

        // Yesterday
        ComparisonBarItem(
            label = "Yesterday",
            value = yesterday,
            maxValue = maxValue,
            barColor = Color(0xFF9E9E9E), // Gray
            trend = null,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor
        )

        // Week Average
        ComparisonBarItem(
            label = "Week Avg",
            value = weekAvg,
            maxValue = maxValue,
            barColor = Color(0xFF757575), // Darker gray
            trend = null,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor
        )
    }
}

/**
 * Single comparison bar item
 */
@Composable
private fun ComparisonBarItem(
    label: String,
    value: Int,
    maxValue: Int,
    barColor: Color,
    trend: String?,
    textColor: Color,
    secondaryTextColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Label
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = secondaryTextColor,
            modifier = Modifier.width(80.dp)
        )

        // Progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .clip(Shapes.card)
                .background(barColor.copy(alpha = 0.2f))
        ) {
            // Filled portion
            val progress = (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(32.dp)
                    .clip(Shapes.card)
                    .background(barColor)
            )
        }

        // Value and trend
        Row(
            modifier = Modifier.width(70.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (trend != null) {
                Text(
                    text = trend,
                    fontSize = 16.sp,
                    color = if (trend == "↓") Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = "${value}m",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = textColor
            )
        }
    }
}
