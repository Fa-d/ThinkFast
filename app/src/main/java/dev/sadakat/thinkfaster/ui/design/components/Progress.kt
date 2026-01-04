package dev.sadakat.thinkfaster.ui.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sadakat.thinkfaster.ui.design.tokens.Animation
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.theme.AppColors

/**
 * Linear progress bar with semantic colors based on percentage
 *
 * Features:
 * - Semantic colors (green/orange/red) based on progress percentage
 * - Optional percentage label display
 * - Smooth animation on progress changes
 * - Rounded corners using design tokens
 *
 * Usage:
 * ```
 * AppLinearProgressBar(
 *     progress = 0.85f,
 *     percentageUsed = 85,
 *     showLabel = true
 * )
 * ```
 *
 * @param progress Progress value (0.0 - 1.0)
 * @param modifier Modifier for the progress bar
 * @param percentageUsed Percentage for color calculation (0-100+)
 * @param showLabel Whether to show percentage label above progress bar
 */
@Composable
fun AppLinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    percentageUsed: Int = (progress * 100).toInt(),
    showLabel: Boolean = true
) {
    val color = AppColors.Progress.getColorForPercentage(percentageUsed)

    // Animate progress changes
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = Animation.ProgressUpdateSpec,
        label = "linear_progress"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Spacing.verticalArrangementSM
    ) {
        if (showLabel) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$percentageUsed%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
        }

        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(Shapes.progressBar),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Circular progress indicator with percentage in center
 *
 * Pattern from iOS reference project's CircularProgress component.
 *
 * Features:
 * - Circular progress with percentage display in center
 * - Semantic colors based on progress percentage
 * - Spring animation for smooth progress updates
 * - Round stroke caps for polished look
 * - Configurable size and stroke width
 *
 * Usage:
 * ```
 * AppCircularProgress(
 *     progress = 0.75f,
 *     percentageUsed = 75,
 *     size = 100.dp
 * )
 * ```
 *
 * @param progress Progress value (0.0 - 1.0)
 * @param modifier Modifier for the circular progress
 * @param size Diameter of the circle (default: 80dp)
 * @param strokeWidth Width of the progress stroke (default: 8dp)
 * @param percentageUsed Percentage for color and display (0-100+)
 * @param showPercentage Whether to show percentage text in center
 */
@Composable
fun AppCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    percentageUsed: Int = (progress * 100).toInt(),
    showPercentage: Boolean = true
) {
    val color = AppColors.Progress.getColorForPercentage(percentageUsed)

    // Animate progress with spring physics (iOS-style)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = Animation.Spring.Default,
        label = "circular_progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Canvas(modifier = Modifier.size(size)) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        // Progress arc
        Canvas(modifier = Modifier.size(size)) {
            drawArc(
                color = color,
                startAngle = -90f,  // Start at top
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        // Percentage text in center
        if (showPercentage) {
            Text(
                text = "$percentageUsed%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Indeterminate circular progress indicator for loading states
 *
 * Usage:
 * ```
 * IndeterminateProgress(
 *     modifier = Modifier.size(48.dp)
 * )
 * ```
 *
 * @param modifier Modifier for the progress indicator
 * @param color Progress indicator color
 * @param strokeWidth Stroke width (default: 4dp)
 */
@Composable
fun IndeterminateProgress(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp
) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth
    )
}
