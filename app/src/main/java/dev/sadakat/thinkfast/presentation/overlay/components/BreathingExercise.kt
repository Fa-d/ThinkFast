package dev.sadakat.thinkfast.presentation.overlay.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfast.domain.model.BreathingVariant
import dev.sadakat.thinkfast.ui.theme.InterventionColors
import dev.sadakat.thinkfast.ui.theme.InterventionTypography
import kotlinx.coroutines.delay

/**
 * Breathing phase states for the exercise
 */
enum class BreathPhase {
    INHALE,
    HOLD_AFTER_INHALE,
    EXHALE,
    HOLD_AFTER_EXHALE;  // For box breathing

    fun getDisplayText(): String = when (this) {
        INHALE -> "Breathe In"
        HOLD_AFTER_INHALE -> "Hold"
        EXHALE -> "Breathe Out"
        HOLD_AFTER_EXHALE -> "Hold"
    }

    fun getInstructionEmoji(): String = when (this) {
        INHALE -> "↑"
        HOLD_AFTER_INHALE -> "⏸"
        EXHALE -> "↓"
        HOLD_AFTER_EXHALE -> "⏸"
    }
}

/**
 * Breathing exercise configuration based on variant
 */
data class BreathingConfig(
    val variant: BreathingVariant,
    val phases: List<BreathPhaseConfig>
) {
    companion object {
        fun from(variant: BreathingVariant): BreathingConfig {
            return when (variant) {
                BreathingVariant.FOUR_SEVEN_EIGHT -> BreathingConfig(
                    variant = variant,
                    phases = listOf(
                        BreathPhaseConfig(BreathPhase.INHALE, durationMs = 4000, targetScale = 1.4f),
                        BreathPhaseConfig(BreathPhase.HOLD_AFTER_INHALE, durationMs = 7000, targetScale = 1.4f),
                        BreathPhaseConfig(BreathPhase.EXHALE, durationMs = 8000, targetScale = 1.0f)
                    )
                )
                BreathingVariant.BOX_BREATHING -> BreathingConfig(
                    variant = variant,
                    phases = listOf(
                        BreathPhaseConfig(BreathPhase.INHALE, durationMs = 4000, targetScale = 1.4f),
                        BreathPhaseConfig(BreathPhase.HOLD_AFTER_INHALE, durationMs = 4000, targetScale = 1.4f),
                        BreathPhaseConfig(BreathPhase.EXHALE, durationMs = 4000, targetScale = 1.0f),
                        BreathPhaseConfig(BreathPhase.HOLD_AFTER_EXHALE, durationMs = 4000, targetScale = 1.0f)
                    )
                )
                BreathingVariant.CALM_BREATHING -> BreathingConfig(
                    variant = variant,
                    phases = listOf(
                        BreathPhaseConfig(BreathPhase.INHALE, durationMs = 5000, targetScale = 1.3f),
                        BreathPhaseConfig(BreathPhase.EXHALE, durationMs = 5000, targetScale = 1.0f)
                    )
                )
            }
        }
    }
}

/**
 * Configuration for a single breathing phase
 */
data class BreathPhaseConfig(
    val phase: BreathPhase,
    val durationMs: Long,
    val targetScale: Float
)

/**
 * Interactive breathing exercise component
 *
 * Provides visual and textual guidance for breathing exercises.
 * Research shows breathing exercises create temporal friction and
 * provide immediate mindfulness benefits.
 *
 * @param variant The breathing pattern to use
 * @param instruction Initial instruction text
 * @param onComplete Callback when exercise completes (after 3 full cycles)
 * @param isDarkTheme Whether dark theme is active
 */
@Composable
fun BreathingExercise(
    variant: BreathingVariant = BreathingVariant.FOUR_SEVEN_EIGHT,
    instruction: String = "Let's take a moment to breathe together",
    onComplete: (() -> Unit)? = null,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val config = remember(variant) { BreathingConfig.from(variant) }

    var currentPhaseIndex by remember { mutableIntStateOf(0) }
    var cycleCount by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }

    val currentPhaseConfig = config.phases[currentPhaseIndex]
    val totalCycles = 3  // Complete 3 full breathing cycles

    // Calculate total progress
    val totalPhases = config.phases.size * totalCycles
    val currentPhaseNumber = cycleCount * config.phases.size + currentPhaseIndex + 1
    val progress = currentPhaseNumber.toFloat() / totalPhases

    // Animated scale for the breathing circle
    val circleScale by animateFloatAsState(
        targetValue = currentPhaseConfig.targetScale,
        animationSpec = tween(
            durationMillis = currentPhaseConfig.durationMs.toInt(),
            easing = LinearEasing
        ),
        label = "circle_scale"
    )

    // Animated opacity for pulsing effect
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Phase progression logic
    LaunchedEffect(currentPhaseIndex, cycleCount) {
        if (!isCompleted) {
            delay(currentPhaseConfig.durationMs)

            // Move to next phase
            val nextPhaseIndex = (currentPhaseIndex + 1) % config.phases.size

            if (nextPhaseIndex == 0) {
                // Completed one full cycle
                val nextCycle = cycleCount + 1
                if (nextCycle >= totalCycles) {
                    // Exercise complete
                    isCompleted = true
                    onComplete?.invoke()
                } else {
                    cycleCount = nextCycle
                    currentPhaseIndex = nextPhaseIndex
                }
            } else {
                currentPhaseIndex = nextPhaseIndex
            }
        }
    }

    // Colors based on theme
    val backgroundColor = if (isDarkTheme)
        InterventionColors.BreathingBackgroundDark
    else
        InterventionColors.BreathingBackground

    val circleColor = if (isDarkTheme)
        InterventionColors.Success.copy(alpha = 0.4f)
    else
        InterventionColors.Success.copy(alpha = 0.3f)

    val textColor = if (isDarkTheme)
        InterventionColors.InterventionTextPrimaryDark
    else
        InterventionColors.InterventionTextPrimary

    val accentColor = InterventionColors.Success

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Instruction text
        Text(
            text = instruction,
            style = InterventionTypography.BreathingInstruction,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Breathing circle container
        Box(
            modifier = Modifier
                .size(300.dp)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer pulsing circle (subtle guide)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.0f)
                    .alpha(pulseAlpha * 0.3f)
                    .background(
                        color = circleColor,
                        shape = CircleShape
                    )
            )

            // Main breathing circle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(circleScale)
                    .background(
                        color = accentColor.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            )

            // Inner circle (visual center)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            )

            // Phase emoji in center
            AnimatedContent(
                targetState = currentPhaseConfig.phase,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "phase_emoji"
            ) { phase ->
                Text(
                    text = phase.getInstructionEmoji(),
                    style = InterventionTypography.InterventionTitle.copy(fontSize = 48.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Phase label with smooth transition
        AnimatedContent(
            targetState = currentPhaseConfig.phase,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            },
            label = "phase_label"
        ) { phase ->
            Text(
                text = phase.getDisplayText(),
                style = InterventionTypography.BreathingPhase,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Breathing pattern name
        Text(
            text = when (variant) {
                BreathingVariant.FOUR_SEVEN_EIGHT -> "4-7-8 Breathing"
                BreathingVariant.BOX_BREATHING -> "Box Breathing"
                BreathingVariant.CALM_BREATHING -> "Calm Breathing"
            },
            style = InterventionTypography.InterventionSubtext,
            color = textColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Progress indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Cycle ${cycleCount + 1} of $totalCycles",
                style = InterventionTypography.ButtonTextSmall,
                color = textColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f),
            )
        }

        // Completion message
        if (isCompleted) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Well done! Take a moment to notice how you feel.",
                style = InterventionTypography.InterventionSubtext,
                color = accentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Compact breathing exercise for smaller spaces
 * (e.g., when shown as one option among multiple interventions)
 */
@Composable
fun CompactBreathingExercise(
    variant: BreathingVariant = BreathingVariant.FOUR_SEVEN_EIGHT,
    onComplete: (() -> Unit)? = null,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val config = remember(variant) { BreathingConfig.from(variant) }

    var currentPhaseIndex by remember { mutableIntStateOf(0) }
    val currentPhaseConfig = config.phases[currentPhaseIndex]

    val circleScale by animateFloatAsState(
        targetValue = currentPhaseConfig.targetScale,
        animationSpec = tween(
            durationMillis = currentPhaseConfig.durationMs.toInt(),
            easing = LinearEasing
        ),
        label = "compact_circle_scale"
    )

    LaunchedEffect(currentPhaseIndex) {
        delay(currentPhaseConfig.durationMs)
        val nextIndex = (currentPhaseIndex + 1) % config.phases.size
        if (nextIndex == 0) {
            onComplete?.invoke()
        }
        currentPhaseIndex = nextIndex
    }

    val accentColor = InterventionColors.Success
    val textColor = if (isDarkTheme)
        InterventionColors.InterventionTextPrimaryDark
    else
        InterventionColors.InterventionTextPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact breathing circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(circleScale)
                .background(
                    color = accentColor.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentPhaseConfig.phase.getInstructionEmoji(),
                style = InterventionTypography.InterventionMessage,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Phase text
        Column {
            AnimatedContent(
                targetState = currentPhaseConfig.phase,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "compact_phase"
            ) { phase ->
                Text(
                    text = phase.getDisplayText(),
                    style = InterventionTypography.BreathingPhase,
                    color = textColor
                )
            }

            Text(
                text = when (variant) {
                    BreathingVariant.FOUR_SEVEN_EIGHT -> "4-7-8"
                    BreathingVariant.BOX_BREATHING -> "Box"
                    BreathingVariant.CALM_BREATHING -> "Calm"
                },
                style = InterventionTypography.ButtonTextSmall,
                color = textColor.copy(alpha = 0.6f)
            )
        }
    }
}
