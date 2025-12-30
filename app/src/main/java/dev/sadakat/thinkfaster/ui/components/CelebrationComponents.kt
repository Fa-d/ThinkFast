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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.sadakat.thinkfaster.ui.theme.ProgressColors
import dev.sadakat.thinkfaster.util.HapticFeedback
import kotlinx.coroutines.delay

/**
 * Celebration components for achievements and milestones
 * Phase 1.5: Quick Wins - Celebration UI
 */

/**
 * Full-screen celebration dialog for major achievements
 */
@Composable
fun CelebrationDialog(
    show: Boolean,
    emoji: String,
    title: String,
    message: String,
    streakDays: Int? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(show) {
        if (show) {
            // Trigger haptic feedback
            if (streakDays != null && streakDays >= 7) {
                HapticFeedback.streakMilestone(context)
            } else {
                HapticFeedback.celebration(context)
            }

            // Auto-dismiss after 3 seconds
            delay(3000)
            onDismiss()
        }
    }

    if (show) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated emoji
                    AnimatedEmoji(emoji = emoji, size = 80.sp)

                    // Title
                    Text(
                        text = title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Message
                    Text(
                        text = message,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    // Streak indicator (if applicable)
                    if (streakDays != null) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            ProgressColors.Streak.getColorForStreak(streakDays),
                                            ProgressColors.Streak.getColorForStreak(streakDays).copy(alpha = 0.7f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔥",
                                    fontSize = 24.sp
                                )
                                Text(
                                    text = "$streakDays ${if (streakDays == 1) "Day" else "Days"} Streak!",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated emoji with bounce effect
 */
@Composable
private fun AnimatedEmoji(emoji: String, size: androidx.compose.ui.unit.TextUnit) {
    val scale = rememberBounceAnimation()

    Text(
        text = emoji,
        fontSize = size,
        modifier = Modifier.scale(scale)
    )
}

/**
 * Compact celebration card for inline achievements
 * Follows Material 3 color system: each container color has a matching "on" color
 */
@Composable
fun CompactCelebrationCard(
    show: Boolean,
    emoji: String,
    title: String,
    message: String,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current

    LaunchedEffect(show) {
        if (show) {
            HapticFeedback.success(context)
            // Auto-dismiss after 5 seconds if callback provided
            if (onDismiss != null) {
                delay(5000)
                onDismiss()
            }
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji in circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 32.sp
                    )
                }

                // Text content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Pulsing achievement badge for ongoing celebrations
 */
@Composable
fun AchievementBadge(
    emoji: String,
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ProgressColors.Achievement
) {
    val pulseScale = rememberPulseAnimation(
        minScale = 0.98f,
        maxScale = 1.02f,
        durationMillis = 1500
    )

    Box(
        modifier = modifier
            .scale(pulseScale)
            .background(
                color = backgroundColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 20.sp
            )
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = backgroundColor
            )
        }
    }
}

/**
 * Streak fire animation for streak counter
 */
@Composable
fun StreakFireBadge(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pulseScale = rememberPulseAnimation(
        minScale = 0.95f,
        maxScale = 1.05f,
        durationMillis = 1000
    )

    val backgroundColor = ProgressColors.Streak.getColorForStreak(streakDays)

    Box(
        modifier = modifier
            .scale(pulseScale)
            .background(
                color = backgroundColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥",
                fontSize = 20.sp
            )
            Text(
                text = "$streakDays",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = backgroundColor
            )
            Text(
                text = if (streakDays == 1) "day" else "days",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = backgroundColor.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * First-time achievement celebration (extra special)
 */
@Composable
fun FirstTimeAchievementCelebration(
    show: Boolean,
    onDismiss: () -> Unit
) {
    CelebrationDialog(
        show = show,
        emoji = "🌟",
        title = "First Goal Achieved!",
        message = "Congratulations on your first day meeting your goal! This is just the beginning of building better habits.",
        onDismiss = onDismiss
    )
}

/**
 * Streak milestone celebration
 */
@Composable
fun StreakMilestoneCelebration(
    show: Boolean,
    streakDays: Int,
    onDismiss: () -> Unit
) {
    val (emoji, title, message) = remember(streakDays) {
        when (streakDays) {
            3 -> Triple("🔥", "3-Day Streak!", "You're on fire! Three days in a row!")
            7 -> Triple("⭐", "Week-Long Streak!", "Incredible! A full week of meeting your goals!")
            14 -> Triple("💎", "2-Week Streak!", "You're a diamond! Two weeks of dedication!")
            30 -> Triple("👑", "30-Day Streak!", "You're royalty! A full month of success!")
            else -> Triple("🎉", "$streakDays-Day Streak!", "$streakDays days in a row! Amazing!")
        }
    }

    CelebrationDialog(
        show = show,
        emoji = emoji,
        title = title,
        message = message,
        streakDays = streakDays,
        onDismiss = onDismiss
    )
}

/**
 * Streak broken recovery dialog (Broken Streak Recovery feature)
 * Shown once when streak breaks (on next app open)
 */
@Composable
fun StreakBrokenRecoveryDialog(
    show: Boolean,
    previousStreak: Int,
    targetApp: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val appName = remember(targetApp) {
        when (targetApp) {
            "com.facebook.katana" -> "Facebook"
            "com.instagram.android" -> "Instagram"
            else -> "App"
        }
    }

    LaunchedEffect(show) {
        if (show) {
            // Trigger warning haptic feedback
            HapticFeedback.warning(context)

            // Auto-dismiss after 4 seconds
            delay(4000)
            onDismiss()
        }
    }

    if (show) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated emoji
                    AnimatedEmoji(emoji = "💔", size = 80.sp)

                    // Title
                    Text(
                        text = "Streak Ended",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    // Message
                    Text(
                        text = "Your $previousStreak-day $appName streak was amazing! Don't give up—you're just 1 day away from starting your comeback.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        lineHeight = 22.sp
                    )

                    // Recovery badge
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(
                                color = ProgressColors.Approaching.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔄",
                                fontSize = 24.sp
                            )
                            Text(
                                text = "Recovery Mode Active",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // Button
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = "Let's Get Back on Track",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recovery complete dialog (Broken Streak Recovery feature)
 * Shown when recovery target is reached
 */
@Composable
fun RecoveryCompleteDialog(
    show: Boolean,
    previousStreak: Int,
    daysToRecover: Int,
    targetApp: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val appName = remember(targetApp) {
        when (targetApp) {
            "com.facebook.katana" -> "Facebook"
            "com.instagram.android" -> "Instagram"
            else -> "App"
        }
    }

    LaunchedEffect(show) {
        if (show) {
            // Trigger success haptic feedback
            HapticFeedback.celebration(context)

            // Auto-dismiss after 4 seconds
            delay(4000)
            onDismiss()
        }
    }

    if (show) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated emoji
                    AnimatedEmoji(emoji = "🎉", size = 80.sp)

                    // Title
                    Text(
                        text = "You're Back on Track!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Message
                    Text(
                        text = "You recovered in just $daysToRecover ${if (daysToRecover == 1) "day" else "days"}! This shows incredible resilience.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        lineHeight = 22.sp
                    )

                    // Stats badge
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(
                                color = ProgressColors.OnTrack.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💪",
                                    fontSize = 24.sp
                                )
                                Text(
                                    text = "Comeback Complete",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProgressColors.OnTrack
                                )
                            }
                            Text(
                                text = "Previous streak: $previousStreak ${if (previousStreak == 1) "day" else "days"}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Button
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProgressColors.OnTrack
                        )
                    ) {
                        Text(
                            text = "Keep Going!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
