package dev.sadakat.thinkfast.presentation.overlay.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Celebration screen shown when user chooses "Go Back"
 * Phase 1.1: Success celebration with confetti animation
 *
 * Shows for 1.5 seconds with:
 * - Confetti particle animation
 * - Encouraging success message
 * - Fade in animation
 */
@Composable
fun CelebrationScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Slight delay before showing celebration
        delay(100)
        visible = true

        // Show celebration for 1.5 seconds
        delay(1500)
        onComplete()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Confetti particles
        if (visible) {
            ConfettiAnimation()
        }

        // Success message with animation
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(400)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large celebration emoji
                Text(
                    text = "🎉",
                    fontSize = 72.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Encouraging message (randomized for variety)
                Text(
                    text = getRandomSuccessMessage(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary message
                Text(
                    text = "You're in control",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Confetti particle animation
 * Creates floating particles with random colors and positions
 */
@Composable
private fun ConfettiAnimation() {
    // Create 20 confetti particles
    repeat(20) { index ->
        ConfettiParticle(index)
    }
}

/**
 * Single confetti particle with random properties
 */
@Composable
private fun ConfettiParticle(seed: Int) {
    val random = remember { Random(seed) }

    // Random starting position (top of screen, random X)
    val startX = remember { random.nextFloat() * 1000f - 500f } // -500 to 500
    val startY = remember { -100f }

    // Random fall distance
    val endY = remember { random.nextFloat() * 800f + 400f } // 400 to 1200

    // Random horizontal drift
    val drift = remember { random.nextFloat() * 200f - 100f } // -100 to 100

    // Animated position
    val animatedY = remember { Animatable(startY) }
    val animatedX = remember { Animatable(startX) }
    val alpha = remember { Animatable(1f) }

    // Random delay for staggered animation
    val delay = remember { random.nextInt(200).toLong() }

    // Random color
    val color = remember {
        val colors = listOf(
            Color(0xFFFF6B6B), // Red
            Color(0xFF4ECDC4), // Teal
            Color(0xFFFFE66D), // Yellow
            Color(0xFF95E1D3), // Mint
            Color(0xFFF38181), // Pink
            Color(0xFFAA96DA), // Purple
            Color(0xFFFCBF49), // Orange
            Color(0xFF06FFA5)  // Green
        )
        colors[random.nextInt(colors.size)]
    }

    // Particle shape (emoji)
    val emoji = remember {
        val emojis = listOf("●", "★", "▲", "■")
        emojis[random.nextInt(emojis.size)]
    }

    LaunchedEffect(Unit) {
        delay(delay)

        launch {
            // Fall animation
            animatedY.animateTo(
                targetValue = endY,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = LinearEasing
                )
            )
        }

        launch {
            // Horizontal drift
            animatedX.animateTo(
                targetValue = startX + drift,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = LinearEasing
                )
            )
        }

        launch {
            // Fade out near the end
            delay(1000)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(500)
            )
        }
    }

    Text(
        text = emoji,
        fontSize = 24.sp,
        color = color,
        modifier = Modifier
            .offset(
                x = animatedX.value.dp,
                y = animatedY.value.dp
            )
            .alpha(alpha.value)
    )
}

/**
 * Returns a random success message for variety
 */
private fun getRandomSuccessMessage(): String {
    return listOf(
        "Great choice!",
        "You're in control!",
        "Way to go!",
        "Nice work!",
        "Well done!",
        "Awesome decision!",
        "You got this!"
    ).random()
}
