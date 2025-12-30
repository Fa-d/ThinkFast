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
import dev.sadakat.thinkfaster.ui.theme.ProgressColors
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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = ProgressColors.Info.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with emoji and freeze count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Emoji in circle
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = ProgressColors.Info.copy(alpha = 0.2f),
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
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Streak Freeze",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Protect your streak",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Freeze count badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (freezeStatus.freezesAvailable > 0)
                                    ProgressColors.Info.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${freezeStatus.freezesAvailable}/${freezeStatus.maxFreezes}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (freezeStatus.freezesAvailable > 0)
                                ProgressColors.Info
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
                    fontSize = 14.sp,
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProgressColors.Info,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Info text
                if (freezeStatus.freezesAvailable > 0 && !freezeStatus.hasActiveFreeze) {
                    Text(
                        text = "💡 Freezes refill monthly. Use them wisely!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
