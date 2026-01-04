package dev.sadakat.thinkfaster.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.presentation.home.PerAppGoalUiModel
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.theme.AppColors
import dev.sadakat.thinkfaster.util.HapticFeedback

/**
 * Goal Card - Displays per-app goal with progress
 *
 * Features:
 * - Compact view: App icon, name, daily limit, streak, progress bar
 * - Color-coded streaks and progress bars
 * - Opens bottom sheet on click
 */
@Composable
fun GoalCard(
    app: PerAppGoalUiModel, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = Shapes.card
            )
            .clickable {
                HapticFeedback.light(context)
                onClick()
            }, shape = Shapes.card, colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            // App info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon + name + limit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementMD,
                    modifier = Modifier.weight(1f)
                ) {
                    // App icon using shared AppIconView component
                    AppIconView(
                        drawable = app.appIcon,
                        appName = app.appName,
                        size = 48.dp,
                        contentDescription = "${app.appName} icon"
                    )

                    Column(
                        verticalArrangement = Spacing.verticalArrangementXS
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (app.dailyLimitMinutes != null) {
                            Text(
                                text = "${app.dailyLimitMinutes} min/day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "No goal set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Streak badge
                if (app.currentStreak > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Spacing.horizontalArrangementXS,
                        modifier = Modifier
                            .clip(Shapes.button)
                            .background(app.streakColor.copy(alpha = 0.15f))
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    ) {
                        Text(text = "🔥", fontSize = 14.sp)
                        Text(
                            text = "${app.currentStreak}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = app.streakColor
                        )
                    }
                }
            }

            // Progress bar
            if (app.dailyLimitMinutes != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))

                Column(
                    verticalArrangement = Spacing.verticalArrangementXS
                ) {
                    // Usage text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${app.todayUsageMinutes} min used",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (app.remainingMinutes != null && !app.isOverLimit) {
                            Text(
                                text = "${app.remainingMinutes} min left",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (app.isOverLimit) {
                            Text(
                                text = "Over limit",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Progress indicator
                    val progress = (app.percentageUsed?.coerceAtMost(100) ?: 0) / 100f
                    val progressColor =
                        AppColors.Progress.getColorForPercentage(app.percentageUsed ?: 0)

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(Shapes.button),
                        color = progressColor,
                        trackColor = progressColor.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}
