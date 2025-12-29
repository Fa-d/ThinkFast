package dev.sadakat.thinkfast.presentation.overlay.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfast.ui.theme.InterventionColors
import kotlinx.coroutines.delay

/**
 * Enhanced gamification content with progress visualization
 * Phase 3.2: Visual progress indicators and milestone tracking
 *
 * Features:
 * - Circular progress ring showing current/target
 * - Animated trophy on achievement unlock
 * - Visual milestone indicators
 * - Color-coded progress (amber → gold gradient)
 */
@Composable
fun EnhancedGamificationContent(
    challenge: String,
    reward: String,
    currentProgress: Int,
    target: Int,
    textColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (currentProgress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val isComplete = currentProgress >= target

    // Progress color - amber to gold gradient based on completion
    val progressColor = if (isComplete) {
        Color(0xFFFFD700) // Gold
    } else {
        Color(0xFFFFB300) // Amber
    }

    // Animated trophy scale when complete
    var trophyScale by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isComplete) {
        if (isComplete) {
            trophyScale = 0f
            delay(300)
            // Spring bounce animation
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) { value, _ ->
                trophyScale = value
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Challenge title
        Text(
            text = challenge,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Circular progress indicator
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background circle
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 20f
                val radius = (size.minDimension - strokeWidth) / 2

                // Background track
                drawCircle(
                    color = progressColor.copy(alpha = 0.2f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                // Progress arc
                val sweepAngle = progress * 360f
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }

            // Center content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isComplete) {
                    // Animated trophy
                    Text(
                        text = "🏆",
                        fontSize = 56.sp,
                        modifier = Modifier.scale(trophyScale)
                    )
                } else {
                    // Progress text
                    Text(
                        text = "$currentProgress",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "/ $target days",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress status text
        if (isComplete) {
            Text(
                text = "🎉 Achievement Unlocked!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = progressColor,
                textAlign = TextAlign.Center
            )
        } else {
            val remaining = target - currentProgress
            Text(
                text = "$remaining more day${if (remaining > 1) "s" else ""} to unlock",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = secondaryTextColor,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reward description
        Text(
            text = reward,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = if (isComplete) progressColor else secondaryTextColor,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        // Milestone indicators (visual progress dots)
        if (!isComplete && target <= 10) {
            Spacer(modifier = Modifier.height(8.dp))
            MilestoneIndicators(
                current = currentProgress,
                target = target,
                activeColor = progressColor,
                inactiveColor = progressColor.copy(alpha = 0.2f)
            )
        }
    }
}

/**
 * Visual milestone dots showing progress through a streak
 * Only shown for short streaks (≤10 days) to avoid clutter
 */
@Composable
private fun MilestoneIndicators(
    current: Int,
    target: Int,
    activeColor: Color,
    inactiveColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(target) { index ->
            val isActive = index < current
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.7f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy
                ),
                label = "milestone_scale_$index"
            )

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(scale)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = if (isActive) activeColor else inactiveColor,
                        radius = size.minDimension / 2
                    )
                }
            }

            if (index < target - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}
