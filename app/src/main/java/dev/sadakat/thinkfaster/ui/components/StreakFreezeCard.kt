package dev.sadakat.thinkfaster.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.domain.model.StreakFreezeStatus
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.theme.AppColors
import dev.sadakat.thinkfaster.util.HapticFeedback

/**
 * Streak Freeze Card - Shows freeze button when approaching daily limit
 * Broken Streak Recovery feature - Phase 3: UI Components
 *
 * Display rules:
 * - Only show when percentageUsed >= 80 && !isOverLimit
 * - Button enabled when canUseFreeze && currentStreak > 0
 * - Button disabled when freeze already active or no freezes left
 */
@Composable
fun StreakFreezeCard(
    freezeStatus: StreakFreezeStatus,
    currentStreak: Int,
    percentageUsed: Int,
    onActivateFreeze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canActivate = freezeStatus.canUseFreeze && currentStreak > 0 && !freezeStatus.hasActiveFreeze

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
                containerColor = AppColors.Semantic.Info.Container
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                verticalArrangement = Spacing.verticalArrangementMD
            ) {
                // Header with emoji and freeze count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Spacing.horizontalArrangementMD,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Emoji in circle
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = AppColors.Semantic.Info.Container,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "❄️",
                                fontSize = 28.sp
                            )
                        }

                        // Title
                        Column(
                            verticalArrangement = Spacing.verticalArrangementXS
                        ) {
                            Text(
                                text = "Streak Freeze",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Protect your streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Freeze count badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (freezeStatus.freezesAvailable > 0)
                                    AppColors.Semantic.Info.Container
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = Shapes.button
                            )
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    ) {
                        Text(
                            text = "${freezeStatus.freezesAvailable}/${freezeStatus.maxFreezes}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (freezeStatus.freezesAvailable > 0)
                                AppColors.Semantic.Info.Default
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Description
                Text(
                    text = when {
                        freezeStatus.hasActiveFreeze -> "Freeze is active! Your streak is protected today."
                        freezeStatus.freezesAvailable == 0 -> "No freezes left this month. They'll refill on the 1st!"
                        currentStreak == 0 -> "Build a streak first, then you can protect it with a freeze."
                        percentageUsed >= 80 -> "Running low on time? Use a freeze to protect your ${currentStreak}-day streak!"
                        else -> "Save your freeze for when you really need it."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Action button
                Button(
                    onClick = {
                        HapticFeedback.success(context)
                        onActivateFreeze()
                    },
                    enabled = canActivate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Semantic.Info.Default,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Row(
                        horizontalArrangement = Spacing.horizontalArrangementSM,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = Spacing.xs)
                    ) {
                        Text(
                            text = "❄️",
                            fontSize = 20.sp
                        )
                        Text(
                            text = when {
                                freezeStatus.hasActiveFreeze -> "Freeze Active"
                                freezeStatus.freezesAvailable == 0 -> "No Freezes Left"
                                currentStreak == 0 -> "Build a Streak First"
                                else -> "Use Streak Freeze"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Info text
                if (freezeStatus.freezesAvailable > 0 && !freezeStatus.hasActiveFreeze) {
                    Text(
                        text = "💡 Freezes refill monthly. Use them wisely!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
