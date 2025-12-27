package dev.sadakat.thinkfast.util

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Intervention overlay animation utilities
 *
 * Research shows micro-animations can boost engagement by up to 76%.
 * These animations are designed to:
 * 1. Grab attention (entrance)
 * 2. Provide feedback (button presses)
 * 3. Create emotional impact (content-specific animations)
 */
object InterventionAnimations {

    /**
     * Duration constants based on UX research
     */
    object Duration {
        const val ENTRANCE = 400       // Overlay entrance animation
        const val EXIT = 300          // Overlay exit animation
        const val BUTTON_PRESS = 100  // Button press feedback
        const val BREATHING = 4000    // Breathing cycle phases
        const val PULSE = 1500        // Pulsing alert animation
    }

    /**
     * Entrance animation specs
     */
    val EntranceSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val EntranceTween = tween<Float>(
        durationMillis = Duration.ENTRANCE,
        easing = FastOutSlowInEasing
    )

    /**
     * Exit animation specs
     */
    val ExitTween = tween<Float>(
        durationMillis = Duration.EXIT,
        easing = FastOutLinearInEasing
    )

    /**
     * Button press animation spec
     */
    val ButtonPressTween = tween<Float>(
        durationMillis = Duration.BUTTON_PRESS,
        easing = LinearEasing
    )

    /**
     * Entrance transition for intervention overlays
     * Combines fade-in and scale-up for impactful appearance
     */
    fun overlayEnterTransition(): EnterTransition {
        return fadeIn(
            animationSpec = tween(durationMillis = Duration.ENTRANCE)
        ) + scaleIn(
            initialScale = 0.8f,
            animationSpec = EntranceSpring
        )
    }

    /**
     * Exit transition for intervention overlays
     */
    fun overlayExitTransition(): ExitTransition {
        return fadeOut(
            animationSpec = ExitTween
        ) + scaleOut(
            targetScale = 0.9f,
            animationSpec = ExitTween
        )
    }

    /**
     * Content fade-in for text appearing after overlay entrance
     */
    fun contentFadeIn(delayMillis: Int = 100): EnterTransition {
        return fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = delayMillis,
                easing = LinearOutSlowInEasing
            )
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = 400,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        )
    }

    /**
     * Breathing circle animation states
     */
    enum class BreathPhase {
        INHALE,
        HOLD,
        EXHALE
    }

    /**
     * Pulsing animation for urgent alerts (15+ minute sessions)
     */
    @Composable
    fun rememberPulsingAnimation(
        enabled: Boolean = true,
        minScale: Float = 0.95f,
        maxScale: Float = 1.05f
    ): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")

        return if (enabled) {
            infiniteTransition.animateFloat(
                initialValue = minScale,
                targetValue = maxScale,
                animationSpec = infiniteRepeatable(
                    animation = tween(Duration.PULSE, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            ).value
        } else {
            1f
        }
    }

    /**
     * Breathing circle size animation
     */
    @Composable
    fun rememberBreathingAnimation(
        phase: BreathPhase,
        onPhaseComplete: (BreathPhase) -> Unit
    ): Float {
        val (targetSize, duration) = when (phase) {
            BreathPhase.INHALE -> 200f to 4000   // 4 seconds
            BreathPhase.HOLD -> 200f to 7000     // 7 seconds
            BreathPhase.EXHALE -> 120f to 8000   // 8 seconds
        }

        val size by animateFloatAsState(
            targetValue = targetSize,
            animationSpec = tween(
                durationMillis = duration,
                easing = LinearEasing
            ),
            finishedListener = {
                onPhaseComplete(phase)
            },
            label = "breathing_size"
        )

        return size
    }

    /**
     * Shake animation for "Go Back" button to draw attention
     */
    @Composable
    fun rememberShakeAnimation(trigger: Boolean): Float {
        val shake by animateFloatAsState(
            targetValue = if (trigger) 1f else 0f,
            animationSpec = repeatable(
                iterations = 3,
                animation = tween(100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shake"
        )
        return shake * 10f  // 10 pixels shake distance
    }

    /**
     * Countdown timer animation for delayed buttons (progressive friction)
     */
    @Composable
    fun rememberCountdownAnimation(
        totalDurationMs: Long,
        onComplete: () -> Unit
    ): Float {
        var progress by remember { mutableStateOf(0f) }

        LaunchedEffect(totalDurationMs) {
            val startTime = System.currentTimeMillis()
            while (progress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                kotlinx.coroutines.delay(16) // ~60fps
            }
            onComplete()
        }

        return progress
    }
}

/**
 * Modifier extension for button press animation
 * Provides tactile feedback by scaling down slightly when pressed
 */
fun Modifier.buttonPressAnimation(): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = InterventionAnimations.ButtonPressTween,
        label = "button_press"
    )

    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

/**
 * Modifier extension for pulsing animation
 * Used for urgent alerts or important elements
 */
fun Modifier.pulseAnimation(enabled: Boolean = true): Modifier = composed {
    val scale = InterventionAnimations.rememberPulsingAnimation(enabled)
    this.scale(scale)
}

/**
 * Modifier extension for attention-grabbing shake
 * Used to highlight the "Go Back" button
 */
fun Modifier.shakeAnimation(trigger: Boolean): Modifier = composed {
    val offsetX = InterventionAnimations.rememberShakeAnimation(trigger)
    this.graphicsLayer {
        translationX = offsetX
    }
}

/**
 * Fade and slide entrance for content elements
 */
@Composable
fun AnimatedContentAppearance(
    visible: Boolean,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = InterventionAnimations.contentFadeIn(delayMillis),
        exit = fadeOut()
    ) {
        content()
    }
}
