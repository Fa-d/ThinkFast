package dev.sadakat.thinkfaster.presentation.overlay.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.domain.model.BreathingVariant
import dev.sadakat.thinkfaster.ui.theme.InterventionColors
import dev.sadakat.thinkfaster.ui.theme.InterventionTypography
import kotlinx.coroutines.delay
import kotlin.math.abs

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

    /**
     * Short text to display in the center of the breathing circle
     * Clean, minimal text without emoji
     */
    fun getCenterText(): String = when (this) {
        INHALE -> "In"
        HOLD_AFTER_INHALE -> "Hold"
        EXHALE -> "Out"
        HOLD_AFTER_EXHALE -> "Hold"
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
 * Features improved animation with:
 * - Single continuous animation for smooth breathing motion
 * - Natural easing curves (EaseInOutCubic) for realistic breathing feel
 * - Countdown timer showing time remaining in current phase
 * - Accurate time representation users can follow naturally
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
    textColor: Color = InterventionColors.InterventionTextPrimaryDark,
    secondaryTextColor: Color = InterventionColors.InterventionTextSecondaryDark,
    modifier: Modifier = Modifier
) {
    val config = remember(variant) { BreathingConfig.from(variant) }

    var currentPhaseIndex by remember { mutableIntStateOf(0) }
    var cycleCount by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }
    var phaseStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var phaseProgress by remember { mutableFloatStateOf(0f) }

    val currentPhaseConfig = config.phases[currentPhaseIndex]
    val totalCycles = 3  // Complete 3 full breathing cycles

    // Calculate total progress
    val totalPhases = config.phases.size * totalCycles
    val currentPhaseNumber = cycleCount * config.phases.size + currentPhaseIndex + 1
    val overallProgress = currentPhaseNumber.toFloat() / totalPhases

    // Calculate time remaining in current phase for countdown display
    val timeElapsedInPhase = System.currentTimeMillis() - phaseStartTime
    val timeRemainingInPhase = maxOf(0, currentPhaseConfig.durationMs - timeElapsedInPhase)
    val secondsRemaining = (timeRemainingInPhase / 1000f).toInt()

    // Update phase progress continuously
    LaunchedEffect(currentPhaseIndex, cycleCount) {
        phaseStartTime = System.currentTimeMillis()
        while (!isCompleted && currentPhaseIndex == config.phases.indexOf(currentPhaseConfig)) {
            val elapsed = System.currentTimeMillis() - phaseStartTime
            phaseProgress = (elapsed.toFloat() / currentPhaseConfig.durationMs).coerceIn(0f, 1f)
            delay(16) // Update at ~60fps
        }
    }

    // Animated scale for the breathing circle - uses smooth, continuous animation
    val circleScale by animateFloatAsState(
        targetValue = currentPhaseConfig.targetScale,
        animationSpec = tween(
            durationMillis = currentPhaseConfig.durationMs.toInt(),
            easing = EaseInOutCubic // Natural breathing curve
        ),
        label = "circle_scale"
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
                    phaseProgress = 0f
                }
            } else {
                currentPhaseIndex = nextPhaseIndex
                phaseProgress = 0f
            }
        }
    }

    // Colors based on theme
    val backgroundColor = if (isDarkTheme)
        InterventionColors.BreathingBackgroundDark
    else
        InterventionColors.BreathingBackground

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
        AnimatedContent(
            targetState = if (isCompleted) "completed" else "instruction",
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            },
            label = "instruction_content"
        ) { state ->
            Text(
                text = if (state == "completed") {
                    "Well done! Take a moment to notice how you feel."
                } else {
                    instruction
                },
                style = InterventionTypography.BreathingInstruction,
                color = if (state == "completed") accentColor else textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = if (state == "completed") 0.dp else 48.dp)
            )
        }

        if (!isCompleted) {
            Spacer(modifier = Modifier.height(24.dp))

            // Breathing circle container (larger size for better visibility)
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer guide rings (subtle visual cue)
                config.phases.forEachIndexed { index, phase ->
                    val targetScale = phase.targetScale
                    val isCurrentPhase = index == currentPhaseIndex
                    val isPreviousPhase = index == (currentPhaseIndex - 1 + config.phases.size) % config.phases.size

                    // Show subtle guide for target scales
                    if (targetScale > 1.0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(targetScale)
                                .alpha(
                                    when {
                                        isCurrentPhase -> 0.15f
                                        isPreviousPhase -> 0.1f
                                        else -> 0.05f
                                    }
                                )
                                .background(
                                    color = accentColor.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Breathing ripple effect during phase transitions
                if (phaseProgress < 0.1f) {
                    val rippleScale = 1f + (phaseProgress * 0.3f)
                    val rippleAlpha = 1f - (phaseProgress * 10f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(rippleScale * currentPhaseConfig.targetScale)
                            .alpha(rippleAlpha * 0.3f)
                            .background(
                                color = accentColor,
                                shape = CircleShape
                            )
                    )
                }

                // Main breathing circle - smooth animation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(circleScale)
                        .background(
                            color = accentColor.copy(
                                alpha = 0.5f + (phaseProgress * 0.1f)
                            ),
                            shape = CircleShape
                        )
                )

                // Inner glow circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(1f + (abs(phaseProgress - 0.5f) * 0.1f))
                        .background(
                            color = accentColor.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )

                // Center circle with phase text (clean, no emoji)
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            color = accentColor.copy(alpha = 0.95f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = currentPhaseConfig.phase,
                        transitionSpec = {
                            fadeIn(tween(300, easing = FastOutSlowInEasing)) togetherWith
                                    fadeOut(tween(300, easing = FastOutSlowInEasing))
                        },
                        label = "phase_text"
                    ) { phase ->
                        Text(
                            text = phase.getCenterText(),
                            style = InterventionTypography.InterventionTitle.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Phase label with countdown
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Spacer(modifier = Modifier.width(12.dp))

                // Countdown timer
                Text(
                    text = "(${secondsRemaining + 1}s)",
                    style = InterventionTypography.InterventionSubtext.copy(fontSize = 18.sp),
                    color = textColor.copy(alpha = 0.95f),
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
                color = textColor.copy(alpha = 0.95f),
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
                    color = textColor.copy(alpha = 0.95f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.2f),
                )
            }
        }
    }
}

/**
 * Compact breathing exercise for smaller spaces
 * (e.g., when shown in reminder overlay)
 *
 * Phase 3.1 Enhanced with:
 * - Tap to pause/resume control
 * - Pause indicator overlay
 * - Completion feedback with animation
 * - Synchronized instruction text
 */
@Composable
fun CompactBreathingExercise(
    variant: BreathingVariant = BreathingVariant.FOUR_SEVEN_EIGHT,
    onComplete: (() -> Unit)? = null,
    isDarkTheme: Boolean = false,
    textColor: Color = InterventionColors.InterventionTextPrimaryDark,
    secondaryTextColor: Color = InterventionColors.InterventionTextSecondaryDark,
    modifier: Modifier = Modifier
) {
    val config = remember(variant) { BreathingConfig.from(variant) }

    var currentPhaseIndex by remember { mutableIntStateOf(0) }
    var cycleCount by remember { mutableIntStateOf(0) }
    var phaseStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pausedTime by remember { mutableLongStateOf(0L) }
    var isPaused by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    val currentPhaseConfig = config.phases[currentPhaseIndex]
    val totalCycles = 3

    // Calculate time remaining for countdown
    val timeElapsedInPhase = if (isPaused) {
        pausedTime - phaseStartTime
    } else {
        System.currentTimeMillis() - phaseStartTime
    }
    val timeRemainingInPhase = maxOf(0, currentPhaseConfig.durationMs - timeElapsedInPhase)
    val secondsRemaining = (timeRemainingInPhase / 1000f).toInt()

    // Update phase start time when phase changes
    LaunchedEffect(currentPhaseIndex) {
        phaseStartTime = System.currentTimeMillis()
        pausedTime = 0L
    }

    // Smooth animation with natural easing
    val circleScale by animateFloatAsState(
        targetValue = if (isPaused) currentPhaseConfig.targetScale else currentPhaseConfig.targetScale,
        animationSpec = if (isPaused) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        } else {
            tween(
                durationMillis = currentPhaseConfig.durationMs.toInt(),
                easing = EaseInOutCubic
            )
        },
        label = "compact_circle_scale"
    )

    // Phase progression logic
    LaunchedEffect(currentPhaseIndex, isPaused) {
        if (!isPaused && !isCompleted) {
            delay(currentPhaseConfig.durationMs - timeElapsedInPhase)

            val nextIndex = (currentPhaseIndex + 1) % config.phases.size
            if (nextIndex == 0) {
                // Completed one cycle
                val nextCycle = cycleCount + 1
                if (nextCycle >= totalCycles) {
                    // Exercise complete
                    isCompleted = true
                    delay(1500) // Show completion message for 1.5s
                    onComplete?.invoke()
                } else {
                    cycleCount = nextCycle
                    currentPhaseIndex = nextIndex
                }
            } else {
                currentPhaseIndex = nextIndex
            }
        }
    }

    val accentColor = InterventionColors.Success

    // Column layout for better vertical presentation
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Synchronized instruction text
        AnimatedContent(
            targetState = if (isCompleted) "completed" else currentPhaseConfig.phase.getDisplayText(),
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            },
            label = "instruction_text"
        ) { text ->
            Text(
                text = if (text == "completed") {
                    "Great job! You completed 3 cycles"
                } else {
                    text
                },
                style = InterventionTypography.BreathingPhase.copy(
                    fontSize = if (text == "completed") 20.sp else 18.sp,
                    fontWeight = if (text == "completed") FontWeight.Bold else FontWeight.Medium
                ),
                color = if (text == "completed") accentColor else textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (!isCompleted) {
            // Breathing circle container (larger for better visibility)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isPaused = !isPaused
                        if (isPaused) {
                            pausedTime = System.currentTimeMillis()
                        } else {
                            // Resume: adjust start time to account for pause duration
                            val pauseDuration = System.currentTimeMillis() - pausedTime
                            phaseStartTime += pauseDuration
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer guide rings (subtle visual cue for target sizes)
                config.phases.forEachIndexed { index, phase ->
                    val targetScale = phase.targetScale
                    val isCurrentPhase = index == currentPhaseIndex

                    // Show subtle guide for target scales
                    if (targetScale > 1.0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(targetScale)
                                .alpha(if (isCurrentPhase) 0.12f else 0.06f)
                                .background(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Main breathing circle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(circleScale)
                        .background(
                            color = accentColor.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner glow circle
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                color = accentColor.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )

                    // Center circle with phase text or pause icon
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                color = accentColor.copy(alpha = 0.95f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPaused) {
                            // Show pause icon
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Resume",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            // Show phase text
                            AnimatedContent(
                                targetState = currentPhaseConfig.phase,
                                transitionSpec = {
                                    fadeIn(tween(200, easing = FastOutSlowInEasing)) togetherWith
                                            fadeOut(tween(200, easing = FastOutSlowInEasing))
                                },
                                label = "compact_phase_text"
                            ) { phase ->
                                Text(
                                    text = phase.getCenterText(),
                                    style = InterventionTypography.InterventionMessage.copy(
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Pause indicator overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = isPaused,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Countdown timer and cycle counter
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Countdown timer
                Text(
                    text = if (isPaused) "PAUSED" else "${secondsRemaining + 1}s",
                    style = InterventionTypography.InterventionSubtext.copy(fontSize = 16.sp),
                    color = if (isPaused) accentColor else textColor.copy(alpha = 0.95f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Cycle counter
                Text(
                    text = "Cycle ${cycleCount + 1}/$totalCycles",
                    style = InterventionTypography.ButtonTextSmall.copy(fontSize = 14.sp),
                    color = textColor.copy(alpha = 0.95f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Breathing pattern name
            Text(
                text = when (variant) {
                    BreathingVariant.FOUR_SEVEN_EIGHT -> "4-7-8 Breathing"
                    BreathingVariant.BOX_BREATHING -> "Box Breathing"
                    BreathingVariant.CALM_BREATHING -> "Calm Breathing"
                },
                style = InterventionTypography.ButtonTextSmall.copy(fontSize = 13.sp),
                color = textColor.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tap hint
            Text(
                text = "Tap circle to ${if (isPaused) "resume" else "pause"}",
                style = InterventionTypography.ButtonTextSmall.copy(fontSize = 12.sp),
                color = textColor.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        } else {
            // Completion celebration
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "✓",
                fontSize = 80.sp,
                color = accentColor,
                modifier = Modifier
                    .scale(
                        animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "completion_checkmark"
                        ).value
                    )
            )
        }
    }
}
