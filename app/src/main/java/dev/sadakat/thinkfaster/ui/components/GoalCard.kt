package dev.sadakat.thinkfaster.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.presentation.home.PerAppGoalUiModel
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.theme.AppColors
import dev.sadakat.thinkfaster.util.HapticFeedback

/**
 * Goal Card - Displays per-app goal with inline editing
 *
 * Features:
 * - Compact view: App icon, name, daily limit, streak, progress bar
 * - Expanded view: Slider (15-180min), streak stats, save button
 * - Color-coded streaks and progress bars
 * - Smooth expand/collapse animation
 */
@Composable
fun GoalCard(
    app: PerAppGoalUiModel,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSaveGoal: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sliderValue by remember(app.dailyLimitMinutes) {
        mutableStateOf((app.dailyLimitMinutes ?: 60).toFloat())
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrow_rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    HapticFeedback.light(context)
                    onToggleExpanded()
                }
                .padding(Spacing.md)
        ) {
            // Compact view - always visible
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

                // Streak badge + expand arrow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
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

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier
                            .rotate(rotationAngle)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    val progressColor = AppColors.Progress.getColorForPercentage(app.percentageUsed ?: 0)

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

            // Expanded view - inline editor
            if (isExpanded) {
                Spacer(modifier = Modifier.height(Spacing.md))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Goal slider
                Column(
                    verticalArrangement = Spacing.verticalArrangementSM
                ) {
                    Text(
                        text = "Daily Goal: ${sliderValue.toInt()} minutes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 15f..180f,
                        steps = 32, // 5-minute increments
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "15 min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "180 min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Streak stats
                if (app.currentStreak > 0 || app.bestStreak > 0) {
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Spacing.horizontalArrangementMD
                    ) {
                        if (app.currentStreak > 0) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${app.currentStreak}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = app.streakColor
                                )
                                Text(
                                    text = "Current Streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (app.bestStreak > 0) {
                            Spacer(modifier = Modifier.width(Spacing.lg))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${app.bestStreak}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Best Streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Save button
                Spacer(modifier = Modifier.height(Spacing.md))

                Button(
                    onClick = {
                        HapticFeedback.success(context)
                        onSaveGoal(sliderValue.toInt())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Save Goal",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
