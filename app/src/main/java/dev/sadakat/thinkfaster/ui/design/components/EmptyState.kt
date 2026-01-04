package dev.sadakat.thinkfaster.ui.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing

/**
 * Empty state view with icon, title, subtitle, and optional CTA
 *
 * Pattern from iOS reference project's EmptyState component.
 *
 * Features:
 * - Large icon for visual context
 * - Headline title
 * - Body subtitle with explanation
 * - Optional action button
 * - Centered layout with generous spacing
 *
 * Usage:
 * ```
 * EmptyStateView(
 *     icon = Icons.Default.CheckCircle,
 *     title = "No Goals Yet",
 *     subtitle = "Set your first goal to start building better habits.",
 *     actionText = "Add Goal",
 *     onAction = { navigateToAddGoal() }
 * )
 * ```
 *
 * @param icon Large icon representing the empty state
 * @param title Headline text describing the empty state
 * @param subtitle Supporting text with more context or instructions
 * @param modifier Modifier for the empty state container
 * @param actionText Optional text for action button
 * @param onAction Optional callback when action button is clicked
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Spacing.verticalArrangementLG
    ) {
        // Large icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        // Title and subtitle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Spacing.verticalArrangementXS
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Optional action button
        if (actionText != null && onAction != null) {
            PrimaryButton(
                text = actionText,
                onClick = onAction
            )
        }
    }
}

/**
 * Compact empty state for smaller spaces (e.g., inside cards)
 *
 * Usage:
 * ```
 * CompactEmptyState(
 *     icon = Icons.Default.Inbox,
 *     message = "No data available"
 * )
 * ```
 *
 * @param icon Icon representing the empty state
 * @param message Brief message describing the empty state
 * @param modifier Modifier for the empty state container
 */
@Composable
fun CompactEmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Spacing.verticalArrangementMD
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
