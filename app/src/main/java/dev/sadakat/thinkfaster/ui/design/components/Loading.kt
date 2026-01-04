package dev.sadakat.thinkfaster.ui.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing

/**
 * Centered loading indicator with optional message
 *
 * Features:
 * - Centered circular progress indicator
 * - Optional loading message below spinner
 * - Uses design tokens for spacing
 *
 * Usage:
 * ```
 * LoadingIndicator(
 *     message = "Loading your goals..."
 * )
 * ```
 *
 * @param modifier Modifier for the loading container
 * @param message Optional message to display below the spinner
 * @param color Color of the progress indicator (default: primary)
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Spacing.verticalArrangementMD
    ) {
        IndeterminateProgress(
            modifier = Modifier.size(48.dp),
            color = color,
            strokeWidth = 4.dp
        )

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Compact loading indicator for inline use (e.g., buttons, cards)
 *
 * Usage:
 * ```
 * CompactLoadingIndicator()
 * ```
 *
 * @param modifier Modifier for the loading indicator
 * @param size Size of the circular progress (default: 24dp)
 * @param color Color of the progress indicator (default: primary)
 */
@Composable
fun CompactLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Int = 24,
    color: Color = MaterialTheme.colorScheme.primary
) {
    IndeterminateProgress(
        modifier = modifier.size(size.dp),
        color = color,
        strokeWidth = 2.dp
    )
}

/**
 * Full-screen loading overlay with message
 *
 * Usage:
 * ```
 * if (isLoading) {
 *     FullScreenLoading(
 *         message = "Syncing your data..."
 *     )
 * }
 * ```
 *
 * @param message Loading message to display
 * @param modifier Modifier for the loading container
 */
@Composable
fun FullScreenLoading(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Spacing.verticalArrangementXL
    ) {
        IndeterminateProgress(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 6.dp
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Spacing.verticalArrangementSM
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Please wait...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
