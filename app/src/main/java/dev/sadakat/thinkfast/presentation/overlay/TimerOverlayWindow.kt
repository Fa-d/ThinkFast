package dev.sadakat.thinkfast.presentation.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.presentation.overlay.components.BreathingExercise
import dev.sadakat.thinkfast.ui.theme.InterventionColors
import dev.sadakat.thinkfast.ui.theme.InterventionTypography
import dev.sadakat.thinkfast.ui.theme.ThinkFastTheme
import dev.sadakat.thinkfast.util.ErrorLogger
import dev.sadakat.thinkfast.util.InterventionAnimations
import dev.sadakat.thinkfast.util.InterventionStyling
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * WindowManager-based timer alert overlay with dynamic intervention content
 * Shown after configured duration of continuous usage
 * Phase D implementation with full dynamic content support
 */
class TimerOverlayWindow(
    private val context: Context
) : KoinComponent, LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val viewModel: TimerOverlayViewModel by inject()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: ComposeView? = null
    private var isShowing = false

    // For Compose lifecycle - recreated on each show to avoid DESTROYED state issues
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private var savedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)
    private var store: ViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    /**
     * Show the timer overlay
     * This method can be called from any thread - it will post to main thread
     */
    fun show(
        sessionId: Long,
        targetApp: AppTarget,
        sessionStartTime: Long,
        sessionDuration: Long
    ) {
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show(sessionId, targetApp, sessionStartTime, sessionDuration) }
            return
        }

        if (isShowing) {
            ErrorLogger.warning(
                "Timer overlay already showing, ignoring show() request",
                context = "TimerOverlayWindow.show"
            )
            return
        }

        // Recreate lifecycle objects to avoid DESTROYED → CREATED transition issues
        lifecycleRegistry = LifecycleRegistry(this)
        savedStateRegistryController = SavedStateRegistryController.create(this)
        store = ViewModelStore()

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        // Create the overlay view
        overlayView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@TimerOverlayWindow)
            setViewTreeViewModelStoreOwner(this@TimerOverlayWindow)
            setViewTreeSavedStateRegistryOwner(this@TimerOverlayWindow)

            // Make overlay cover system bars (status bar and navigation bar)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsetsController?.hide(android.view.WindowInsets.Type.systemBars())
                windowInsetsController?.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                @Suppress("DEPRECATION")
                systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }

            setContent {
                ThinkFastTheme {
                    TimerOverlayScreen(
                        viewModel = viewModel,
                        onDismiss = { dismiss() },
                        onGoBackToHome = { goToHomeScreen() }
                    )
                }
            }
        }

        // Setup window layout params
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // Add view to window manager
        try {
            windowManager.addView(overlayView, params)
            isShowing = true

            // Move lifecycle to RESUMED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED

            // Notify ViewModel
            viewModel.onOverlayShown(sessionId, targetApp, sessionStartTime, sessionDuration)

            ErrorLogger.info(
                "Timer overlay shown successfully for ${targetApp.displayName} (session duration: ${sessionDuration}ms)",
                context = "TimerOverlayWindow.show"
            )
        } catch (e: SecurityException) {
            ErrorLogger.error(
                e,
                message = "SecurityException when adding timer overlay - likely missing overlay permission",
                context = "TimerOverlayWindow.show"
            )
            overlayView = null
            isShowing = false
        } catch (e: Exception) {
            ErrorLogger.error(
                e,
                message = "Unexpected error when showing timer overlay",
                context = "TimerOverlayWindow.show"
            )
            overlayView = null
            isShowing = false
        }
    }

    /**
     * Dismiss the overlay
     * This method can be called from any thread - it will post to main thread
     */
    fun dismiss() {
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { dismiss() }
            return
        }

        if (!isShowing || overlayView == null) {
            return
        }

        try {
            // Move lifecycle to DESTROYED
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

            windowManager.removeView(overlayView)
            overlayView = null
            isShowing = false
        } catch (e: Exception) {
            ErrorLogger.error(
                e,
                message = "Error dismissing timer overlay",
                context = "TimerOverlayWindow.dismiss"
            )
        }
    }

    /**
     * Check if overlay is currently showing
     */
    fun isShowing(): Boolean = isShowing

    /**
     * Go back to home screen, effectively closing the target app
     */
    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)
    }
}

@Composable
fun TimerOverlayScreen(
    viewModel: TimerOverlayViewModel,
    onDismiss: () -> Unit,
    onGoBackToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldDismiss) {
        if (uiState.shouldDismiss) {
            viewModel.onDismissHandled()
            onDismiss()

            // If user chose "Go Back", also navigate to home screen
            if (uiState.userChoseGoBack) {
                onGoBackToHome()
            }
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
        when {
            // Show celebration when requested (takes priority over loading)
            uiState.showCelebration -> {
                CelebrationScreen(isDarkTheme = isDarkTheme)
            }
            // Show loading spinner during initial load
            uiState.isLoading -> {
                LoadingScreen()
            }
            // Show main content when ready
            else -> {
                AnimatedVisibility(
                    visible = true,
                    enter = InterventionAnimations.overlayEnterTransition(),
                    exit = InterventionAnimations.overlayExitTransition()
                ) {
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
    content: dev.sadakat.thinkfast.domain.model.InterventionContent,
    targetApp: dev.sadakat.thinkfast.domain.model.AppTarget?,
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
                is dev.sadakat.thinkfast.domain.model.InterventionContent.ReflectionQuestion -> ReflectionQuestionContent(content, style)
                is dev.sadakat.thinkfast.domain.model.InterventionContent.TimeAlternative -> TimeAlternativeContent(content, style, sessionDuration)
                is dev.sadakat.thinkfast.domain.model.InterventionContent.BreathingExercise -> BreathingExerciseContent(content, isDarkTheme)
                is dev.sadakat.thinkfast.domain.model.InterventionContent.UsageStats -> UsageStatsContent(content, style, todaysTotalUsage)
                is dev.sadakat.thinkfast.domain.model.InterventionContent.EmotionalAppeal -> EmotionalAppealContent(content, style)
                is dev.sadakat.thinkfast.domain.model.InterventionContent.Quote -> QuoteContent(content, style)
                is dev.sadakat.thinkfast.domain.model.InterventionContent.Gamification -> GamificationContent(content, style)
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
    content: dev.sadakat.thinkfast.domain.model.InterventionContent.ReflectionQuestion,
    style: dev.sadakat.thinkfast.util.InterventionStyle
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
    content: dev.sadakat.thinkfast.domain.model.InterventionContent.TimeAlternative,
    style: dev.sadakat.thinkfast.util.InterventionStyle,
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
    content: dev.sadakat.thinkfast.domain.model.InterventionContent.BreathingExercise,
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
    content: dev.sadakat.thinkfast.domain.model.InterventionContent.UsageStats,
    style: dev.sadakat.thinkfast.util.InterventionStyle,
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
    content: dev.sadakat.thinkfast.domain.model.InterventionContent.EmotionalAppeal,
    style: dev.sadakat.thinkfast.util.InterventionStyle
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
    content: dev.sadakat.thinkfast.domain.model.InterventionContent.Quote,
    style: dev.sadakat.thinkfast.util.InterventionStyle
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
    content: dev.sadakat.thinkfast.domain.model.InterventionContent.Gamification,
    style: dev.sadakat.thinkfast.util.InterventionStyle
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
                    color = if (isDarkTheme) InterventionColors.InterventionTextPrimaryDark.copy(alpha = 0.7f) else InterventionColors.InterventionTextPrimary.copy(alpha = 0.7f),
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
                    color = if (isDarkTheme) InterventionColors.InterventionTextPrimaryDark else InterventionColors.InterventionTextPrimary
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
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(proceedColor)
                )
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

// Extension function for button press animation
@Suppress("unused")
private fun Modifier.buttonPressAnimation(): Modifier = this

// Import LinearProgressIndicator with lambda progress
@Composable
private fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = Color.Unspecified,
    strokeCap: StrokeCap = StrokeCap.Round
) {
    androidx.compose.material3.LinearProgressIndicator(
        progress = { progress() },
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeCap = strokeCap,
        drawStopIndicator = {},
    )
}
