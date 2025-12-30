package dev.sadakat.thinkfaster.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.sp

/**
 * Custom animations for ThinkFast
 * Phase 1.4: Visual Polish - Custom animations
 */

/**
 * Pulsing animation for emphasis
 */
@Composable
fun rememberPulseAnimation(
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    durationMillis: Int = 1000
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    return scale
}

/**
 * Bounce animation for achievements/celebrations
 */
@Composable
fun rememberBounceAnimation(
    initialScale: Float = 0.3f,
    targetScale: Float = 1f,
    durationMillis: Int = 600
): Float {
    var triggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        triggered = true
    }

    val scale by animateFloatAsState(
        targetValue = if (triggered) targetScale else initialScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce_scale"
    )

    return scale
}

/**
 * Fade in animation
 */
@Composable
fun rememberFadeInAnimation(
    durationMillis: Int = 400,
    delayMillis: Int = 0
): Float {
    var triggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        triggered = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = tween(durationMillis, easing = EaseOut),
        label = "fade_in"
    )

    return alpha
}

/**
 * Shimmer loading animation
 */
@Composable
fun rememberShimmerAnimation(durationMillis: Int = 1500): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    return offset
}

/**
 * Celebration animation with emoji
 */
@Composable
fun CelebrationAnimation(
    emoji: String = "🎉",
    show: Boolean = true,
    onComplete: () -> Unit = {}
) {
    if (!show) return

    val scale = rememberBounceAnimation()

    var completed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        if (!completed) {
            completed = true
            onComplete()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = (64 * scale).sp,
            modifier = Modifier.scale(scale)
        )
    }
}

/**
 * Progress animation easing
 */
object ProgressEasing {
    val smooth = tween<Float>(durationMillis = 500, easing = EaseInOutCubic)
    val quick = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
    val bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
