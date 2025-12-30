package dev.sadakat.thinkfaster.presentation.overlay.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

/**
 * Enhanced overlay entrance animation wrapper
 * Phase 1.3: Attention-grabbing entrance with spring bounce
 *
 * Features:
 * - Slide from bottom with overshoot spring
 * - Smooth fade-in
 * - Bounce on arrival for impact
 * - No blur on content (keeps text sharp and readable)
 */
@Composable
fun OverlayEntranceAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Animation state
    val translationY = remember { Animatable(1000f) } // Start below screen
    val alpha = remember { Animatable(0f) }

    // Start animations on first composition
    LaunchedEffect(Unit) {
        // Small delay before starting (for overlay to attach to window)
        delay(50)

        // Start all animations concurrently using async
        val slideJob = async {
            // Slide from bottom with spring bounce
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.6f, // Lower = more overshoot
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        val fadeJob = async {
            // Fade in (faster than slide)
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300
                )
            )
        }

        // Wait for all animations to complete
        slideJob.await()
        fadeJob.await()
    }

    // Content with slide and fade animations (no blur on content!)
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.translationY = translationY.value
                this.alpha = alpha.value
            }
    ) {
        content()
    }
}

/**
 * Exit animation for overlay dismissal
 * Phase 1.4: Smooth exit based on user choice
 *
 * @param exitType Direction/style of exit animation
 * @param onComplete Callback when animation completes
 */
@Composable
fun OverlayExitAnimation(
    exitType: ExitAnimationType,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    onComplete: () -> Unit
) {
    val translationY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(exitType) {
        when (exitType) {
            ExitAnimationType.SLIDE_DOWN -> {
                // User chose "Proceed" - slide down and fade
                val slideJob = async {
                    translationY.animateTo(
                        targetValue = 1000f, // Slide below screen
                        animationSpec = tween(200)
                    )
                }
                val fadeJob = async {
                    alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(200)
                    )
                }
                slideJob.await()
                fadeJob.await()
                onComplete()
            }

            ExitAnimationType.SLIDE_UP -> {
                // User chose "Go Back" - slide up (before celebration)
                val slideJob = async {
                    translationY.animateTo(
                        targetValue = -1000f, // Slide above screen
                        animationSpec = tween(200)
                    )
                }
                val fadeJob = async {
                    alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(200)
                    )
                }
                slideJob.await()
                fadeJob.await()
                onComplete()
            }

            ExitAnimationType.FADE -> {
                // Simple fade out
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(200)
                )
                onComplete()
            }

            ExitAnimationType.SCALE_DOWN -> {
                // Scale down to center point
                val scaleJob = async {
                    scale.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(250)
                    )
                }
                val fadeJob = async {
                    alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(250)
                    )
                }
                scaleJob.await()
                fadeJob.await()
                onComplete()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.translationY = translationY.value
                this.alpha = alpha.value
                this.scaleX = scale.value
                this.scaleY = scale.value
            }
    ) {
        content()
    }
}

/**
 * Exit animation types
 */
enum class ExitAnimationType {
    SLIDE_DOWN,  // Dismissed by "Proceed"
    SLIDE_UP,    // Dismissed by "Go Back"
    FADE,        // Generic fade out
    SCALE_DOWN   // Scale to point
}

/**
 * Pulsing glow effect for emphasis
 * Can be used to draw attention to important content
 */
@Composable
fun PulsingGlow(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0.3f) }

    LaunchedEffect(Unit) {
        // Infinite pulse animation
        alpha.animateTo(
            targetValue = 0.8f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = tween(1000),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            )
        )
    }

    Box(modifier = modifier) {
        // Glow background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(color.copy(alpha = alpha.value))
                .blur(16.dp)
        )

        // Content on top
        content()
    }
}
