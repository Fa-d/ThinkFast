package dev.sadakat.thinkfaster.ui.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing

/**
 * Standard card with consistent styling
 *
 * Features:
 * - Uses design tokens for all dimensions (Spacing.Card.padding, Shapes.card)
 * - Optional onClick for interactive cards
 * - Consistent elevation (2dp)
 * - ColumnScope content for easy vertical layout
 *
 * Usage:
 * ```
 * StandardCard {
 *     Text("Title", style = MaterialTheme.typography.titleMedium)
 *     Text("Description", style = MaterialTheme.typography.bodyMedium)
 * }
 * ```
 *
 * @param modifier Modifier for the card
 * @param onClick Optional click handler (makes card interactive)
 * @param elevation Card elevation (default: 2dp)
 * @param content Card content in ColumnScope
 */
@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        },
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Card.padding),
            verticalArrangement = Spacing.verticalArrangementMD,
            content = content
        )
    }
}

/**
 * Elevated card with higher elevation for prominence
 *
 * Features:
 * - Higher elevation (8dp) for featured content
 * - Same consistent styling as StandardCard
 * - Uses design tokens
 *
 * Usage:
 * ```
 * ElevatedCard {
 *     Text("Featured Content")
 * }
 * ```
 *
 * @param modifier Modifier for the card
 * @param onClick Optional click handler
 * @param content Card content in ColumnScope
 */
@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    StandardCard(
        modifier = modifier,
        onClick = onClick,
        elevation = 8.dp,
        content = content
    )
}

/**
 * Outlined card with border and no elevation
 *
 * Features:
 * - 1dp border, no shadow
 * - Configurable border color
 * - Flat design for less visual weight
 *
 * Usage:
 * ```
 * OutlinedCard {
 *     Text("Outlined Content")
 * }
 * ```
 *
 * @param modifier Modifier for the card
 * @param onClick Optional click handler
 * @param borderColor Border color (default: MaterialTheme.colorScheme.outline)
 * @param content Card content in ColumnScope
 */
@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        },
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Card.padding),
            verticalArrangement = Spacing.verticalArrangementMD,
            content = content
        )
    }
}

/**
 * Stat card with icon, title, value, and optional subtitle
 *
 * Pattern from iOS reference project's StatCard component.
 *
 * Features:
 * - Icon + title header
 * - Large bold value display
 * - Optional subtitle
 * - Color-coded icon for visual hierarchy
 *
 * Usage:
 * ```
 * StatCard(
 *     title = "Total Usage",
 *     value = "2h 15m",
 *     subtitle = "25% less than yesterday",
 *     icon = Icons.Default.TrendingUp,
 *     color = AppColors.Semantic.Success.Default
 * )
 * ```
 *
 * @param title Stat label (e.g., "Total Usage")
 * @param value Main stat value (e.g., "2h 15m")
 * @param modifier Modifier for the card
 * @param subtitle Optional subtitle (e.g., "25% less than yesterday")
 * @param icon Optional icon for visual context
 * @param color Icon and accent color (default: primary)
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    StandardCard(modifier = modifier) {
        // Header with icon and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Spacing.horizontalArrangementSM,
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Value (large, bold)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Subtitle (optional)
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Info card with optional icon and colored container
 *
 * Features:
 * - Colored container background for visual distinction
 * - Optional icon
 * - Title and description layout
 * - Useful for callouts, tips, warnings
 *
 * Usage:
 * ```
 * InfoCard(
 *     title = "Tip",
 *     description = "Set realistic goals to build sustainable habits.",
 *     icon = Icons.Default.Lightbulb,
 *     containerColor = AppColors.Semantic.Info.Container
 * )
 * ```
 *
 * @param title Card title
 * @param description Card description text
 * @param modifier Modifier for the card
 * @param icon Optional leading icon
 * @param containerColor Background color (default: info container)
 * @param contentColor Text color (default: on surface)
 */
@Composable
fun InfoCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Card.padding),
            horizontalArrangement = Spacing.horizontalArrangementMD,
            verticalAlignment = Alignment.Top
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = contentColor
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Spacing.verticalArrangementSM
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
