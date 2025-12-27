package dev.sadakat.thinkfast.presentation.overlay

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.thinkfast.domain.model.*
import dev.sadakat.thinkfast.presentation.overlay.components.BreathingExercise
import dev.sadakat.thinkfast.ui.theme.*
import dev.sadakat.thinkfast.util.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Enhanced timer overlay with dynamic intervention content
 * Phase D implementation
 */
class TimerOverlayActivity : ComponentActivity() {

    private val viewModel: TimerOverlayViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupFullScreen()

        // Extract session data
        val sessionId = intent.getLongExtra(Constants.EXTRA_SESSION_ID, -1L)
        val targetAppPackage = intent.getStringExtra(Constants.EXTRA_TARGET_APP)
        val sessionStartTime = intent.getLongExtra("session_start_time", System.currentTimeMillis())
        val sessionDuration = intent.getLongExtra("session_duration", 0L)

        if (sessionId == -1L || targetAppPackage == null) {
            finish()
            return
        }

        val targetApp = AppTarget.fromPackageName(targetAppPackage)
        if (targetApp == null) {
            finish()
            return
        }

        viewModel.onOverlayShown(sessionId, targetApp, sessionStartTime, sessionDuration)

        setContent {
            ThinkFastTheme {
                TimerOverlayScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Disabled - user must choose button
    }

    private fun setupFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }
}

@Composable
fun TimerOverlayScreen(
    viewModel: TimerOverlayViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldDismiss) {
        if (uiState.shouldDismiss) {
            viewModel.onDismissHandled()
            onDismiss()
        }
    }

    val isDarkTheme = isSystemInDarkTheme()

    // Get styling based on content type
    val style = uiState.interventionContent?.let {
        InterventionStyling.getStyleForContent(it, isDarkTheme)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = style?.backgroundColor ?: InterventionStyling.getUrgentAlertBackground(isDarkTheme)
    ) {
        AnimatedVisibility(
            visible = !uiState.isLoading,
            enter = InterventionAnimations.overlayEnterTransition(),
            exit = InterventionAnimations.overlayExitTransition()
        ) {
            if (uiState.showCelebration) {
                CelebrationScreen(isDarkTheme = isDarkTheme)
            } else {
                uiState.interventionContent?.let { content ->
                    DynamicInterventionContent(
                        content = content,
                        targetApp = uiState.targetApp,
                        sessionDuration = uiState.currentSessionDuration,
                        todaysTotalUsage = uiState.todaysTotalUsage,
                        frictionLevel = uiState.frictionLevel,
                        onProceed = { viewModel.onProceedClicked() },
                        onGoBack = { viewModel.onGoBackClicked() },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }

        if (uiState.isLoading) {
            LoadingScreen()
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DynamicInterventionContent(
    content: InterventionContent,
    targetApp: AppTarget?,
    sessionDuration: String,
    todaysTotalUsage: String,
    frictionLevel: dev.sadakat.thinkfast.domain.intervention.FrictionLevel,
    onProceed: () -> Unit,
    onGoBack: () -> Unit,
    isDarkTheme: Boolean
) {
    val style = InterventionStyling.getStyleForContent(content, isDarkTheme)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(style.backgroundColor)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: App name
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = targetApp?.displayName ?: "Target App",
                style = InterventionTypography.AppName,
                color = style.textColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "10-Minute Alert",
                style = InterventionTypography.InterventionSubtext,
                color = style.textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        // Middle section: Content-specific UI
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (content) {
                is InterventionContent.ReflectionQuestion -> ReflectionQuestionContent(content, style)
                is InterventionContent.TimeAlternative -> TimeAlternativeContent(content, style, sessionDuration)
                is InterventionContent.BreathingExercise -> BreathingExerciseContent(content, isDarkTheme)
                is InterventionContent.UsageStats -> UsageStatsContent(content, style, todaysTotalUsage)
                is InterventionContent.EmotionalAppeal -> EmotionalAppealContent(content, style)
                is InterventionContent.Quote -> QuoteContent(content, style)
                is InterventionContent.Gamification -> GamificationContent(content, style)
            }
        }

        // Bottom section: Buttons
        InterventionButtons(
            frictionLevel = frictionLevel,
            onProceed = onProceed,
            onGoBack = onGoBack,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun ReflectionQuestionContent(
    content: InterventionContent.ReflectionQuestion,
    style: InterventionStyle
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = content.question,
            style = style.primaryTextStyle,
            color = style.textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = content.subtext,
            style = style.secondaryTextStyle,
            color = style.textColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimeAlternativeContent(
    content: InterventionContent.TimeAlternative,
    style: InterventionStyle,
    sessionDuration: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = content.prefix,
            style = InterventionTypography.TimeAlternativePrefix,
            color = style.textColor.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Alternative activity with emoji
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = content.alternative.emoji,
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = content.alternative.activity,
                style = style.primaryTextStyle,
                color = style.accentColor,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Show session stats
        Text(
            text = "Current session: $sessionDuration",
            style = InterventionTypography.StatsLabel,
            color = style.textColor.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BreathingExerciseContent(
    content: InterventionContent.BreathingExercise,
    isDarkTheme: Boolean
) {
    // Use full breathing exercise component
    BreathingExercise(
        variant = content.variant,
        instruction = content.instruction,
        onComplete = null,  // Don't auto-dismiss
        isDarkTheme = isDarkTheme,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun UsageStatsContent(
    content: InterventionContent.UsageStats,
    style: InterventionStyle,
    todaysTotalUsage: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = content.message,
            style = InterventionTypography.StatsMessage,
            color = style.textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = style.textColor.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Today's Total",
                    style = InterventionTypography.StatsLabel,
                    color = style.textColor.copy(alpha = 0.6f)
                )

                Text(
                    text = todaysTotalUsage,
                    style = InterventionTypography.StatsNumber,
                    color = style.accentColor
                )
            }
        }
    }
}

@Composable
private fun EmotionalAppealContent(
    content: InterventionContent.EmotionalAppeal,
    style: InterventionStyle
) {
    val pulseScale = InterventionAnimations.rememberPulsingAnimation(enabled = true, minScale = 0.98f, maxScale = 1.02f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.scale(pulseScale)
    ) {
        Text(
            text = content.message,
            style = style.primaryTextStyle,
            color = style.accentColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = content.subtext,
            style = style.secondaryTextStyle,
            color = style.textColor.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuoteContent(
    content: InterventionContent.Quote,
    style: InterventionStyle
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "\"",
            fontSize = 72.sp,
            color = style.accentColor.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = content.quote,
            style = style.primaryTextStyle,
            color = style.textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "— ${content.author}",
            style = style.secondaryTextStyle,
            color = style.textColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GamificationContent(
    content: InterventionContent.Gamification,
    style: InterventionStyle
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏆",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = content.challenge,
            style = style.primaryTextStyle,
            color = style.textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = content.reward,
            style = style.secondaryTextStyle,
            color = style.accentColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Progress indicator
        LinearProgressIndicator(
            progress = { content.currentProgress.toFloat() / content.target },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = style.accentColor,
            trackColor = style.accentColor.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun InterventionButtons(
    frictionLevel: dev.sadakat.thinkfast.domain.intervention.FrictionLevel,
    onProceed: () -> Unit,
    onGoBack: () -> Unit,
    isDarkTheme: Boolean
) {
    val (goBackColor, proceedColor) = InterventionStyling.getButtonColors(isDarkTheme)

    var showButtons by remember { mutableStateOf(frictionLevel.delayMs == 0L) }
    var countdown by remember { mutableStateOf(frictionLevel.delayMs / 1000) }

    // Countdown for delayed buttons
    LaunchedEffect(frictionLevel.delayMs) {
        if (frictionLevel.delayMs > 0) {
            while (countdown > 0) {
                kotlinx.coroutines.delay(1000)
                countdown--
            }
            showButtons = true
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!showButtons) {
            // Show countdown
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Take a moment to consider...",
                    style = InterventionTypography.ButtonTextSmall,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = proceedColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${countdown}s",
                    style = InterventionTypography.ButtonText,
                    color = if (isDarkTheme) Color.White else Color.Black
                )
            }
        } else {
            // "Go Back" button (positive action)
            Button(
                onClick = onGoBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .buttonPressAnimation(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goBackColor
                )
            ) {
                Text(
                    text = "Go Back",
                    style = InterventionTypography.ButtonText,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Proceed" button (neutral/discouraging)
            OutlinedButton(
                onClick = onProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .buttonPressAnimation(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = proceedColor
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(proceedColor))
            ) {
                Text(
                    text = "Continue Anyway",
                    style = InterventionTypography.ButtonTextSmall,
                    color = proceedColor
                )
            }
        }
    }
}

@Composable
private fun CelebrationScreen(isDarkTheme: Boolean) {
    val scale by rememberInfiniteTransition(label = "celebration").animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebration_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InterventionColors.Success),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎉",
                fontSize = 96.sp,
                modifier = Modifier.scale(scale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Great Choice!",
                style = InterventionTypography.InterventionTitle,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You chose to focus on what matters",
                style = InterventionTypography.InterventionSubtext,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}
