package dev.sadakat.thinkfast.presentation.overlay.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import kotlinx.coroutines.delay
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfast.ui.theme.InterventionColors

/**
 * Enhanced activity suggestion content with interactivity
 * Phase 3.3: Interactive elements for better engagement
 *
 * Features:
 * - Tap emoji to see time estimate
 * - "I did this" completion button (positive reinforcement)
 * - Visual feedback on interactions
 * - Animated emoji with bounce effect
 */
@Composable
fun EnhancedActivitySuggestionContent(
    suggestion: String,
    emoji: String,
    timeEstimate: String = "5-10 min",
    textColor: Color,
    secondaryTextColor: Color,
    onActivityCompleted: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showTimeEstimate by remember { mutableStateOf(false) }
    var activityCompleted by remember { mutableStateOf(false) }
    var emojiScale by remember { mutableFloatStateOf(1f) }

    // Emoji bounce animation on first appearance
    LaunchedEffect(Unit) {
        delay(300) // After overlay entrance
        // Quick bounce
        animate(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) { value, _ ->
            emojiScale = value
        }
        delay(200)
        animate(
            initialValue = 1.2f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy
            )
        ) { value, _ ->
            emojiScale = value
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Large emoji with tap interaction
        Box(
            modifier = Modifier
                .scale(emojiScale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showTimeEstimate = !showTimeEstimate
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = emoji,
                    fontSize = 72.sp,
                    textAlign = TextAlign.Center
                )

                // Time estimate tooltip
                AnimatedVisibility(
                    visible = showTimeEstimate,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(
                                color = InterventionColors.Success.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "⏱",
                                fontSize = 16.sp
                            )
                            Text(
                                text = timeEstimate,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = InterventionColors.Success
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Header text
        if (!activityCompleted) {
            Text(
                text = "Instead, try this:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = secondaryTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Activity suggestion
            Text(
                text = suggestion,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive buttons
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // "I did this" button
                OutlinedButton(
                    onClick = {
                        activityCompleted = true
                        onActivityCompleted?.invoke()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Complete",
                        tint = InterventionColors.Success,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I did this!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                }
            }

            // Tap hint for emoji
            if (!showTimeEstimate) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap emoji for time estimate",
                    fontSize = 12.sp,
                    color = secondaryTextColor.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Completion celebration
            Spacer(modifier = Modifier.height(16.dp))

            val celebrationScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "celebration_scale"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(celebrationScale)
            ) {
                Text(
                    text = "✓",
                    fontSize = 64.sp,
                    color = InterventionColors.Success
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Awesome! Great choice",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = InterventionColors.Success,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Taking a break is always a good idea",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

/**
 * Get estimated time for an activity based on suggestion text
 */
fun getActivityTimeEstimate(suggestion: String): String {
    return when {
        suggestion.contains("walk", ignoreCase = true) -> "10-15 min"
        suggestion.contains("stretch", ignoreCase = true) -> "5-10 min"
        suggestion.contains("water", ignoreCase = true) -> "2 min"
        suggestion.contains("breathe", ignoreCase = true) -> "3-5 min"
        suggestion.contains("read", ignoreCase = true) -> "15-20 min"
        suggestion.contains("call", ignoreCase = true) -> "10-15 min"
        suggestion.contains("meditation", ignoreCase = true) -> "5-10 min"
        suggestion.contains("exercise", ignoreCase = true) -> "15-30 min"
        suggestion.contains("snack", ignoreCase = true) -> "5 min"
        suggestion.contains("music", ignoreCase = true) -> "10-15 min"
        else -> "5-10 min"
    }
}
