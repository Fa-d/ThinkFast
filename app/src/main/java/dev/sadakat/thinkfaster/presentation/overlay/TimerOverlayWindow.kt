package dev.sadakat.thinkfaster.presentation.overlay

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontStyle
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
import dev.sadakat.thinkfaster.domain.model.InterventionFeedback
import dev.sadakat.thinkfaster.presentation.overlay.components.BreathingExercise
import dev.sadakat.thinkfaster.presentation.overlay.components.CelebrationScreen
import dev.sadakat.thinkfaster.presentation.overlay.components.InterventionContentRenderer
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.theme.InterventionColors
import dev.sadakat.thinkfaster.ui.theme.InterventionTypography
import dev.sadakat.thinkfaster.ui.theme.IntentlyerTheme
import dev.sadakat.thinkfaster.ui.theme.shouldUseTwoColumnLayout
import dev.sadakat.thinkfaster.util.ErrorLogger
import dev.sadakat.thinkfaster.util.InterventionAnimations
import dev.sadakat.thinkfaster.util.InterventionStyling
import android.os.Handler
import android.os.Looper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * WindowManager-based timer alert overlay with dynamic intervention content
 * Shown after configured duration of continuous usage
 * Phase D implementation with full dynamic content support
 */
class TimerOverlayWindow(
    private val context: Context,
    private val onDismissCallback: (() -> Unit)? = null
) : KoinComponent, LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val interventionPreferences: dev.sadakat.thinkfaster.data.preferences.InterventionPreferences by inject()
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
        targetApp: String,
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

            setContent {
                IntentlyerTheme {
                    TimerOverlayScreen(
                        viewModel = viewModel,
                        snoozeDurationMinutes = interventionPreferences.getSelectedSnoozeDuration(),
                        onDismiss = { dismiss() },
                        onGoBackToHome = { goToHomeScreen() }
                    )
                }
            }

            // Hide system bars after content is set
            post {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ - use WindowInsetsController on the window
                    windowInsetsController?.let { controller ->
                        controller.hide(android.view.WindowInsets.Type.systemBars())
                        controller.systemBarsBehavior =
                            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                }
            }
        }

        // Setup window layout params - fullscreen covering system bars
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - minimal flags, let WindowInsetsController handle system bars
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
        } else {
            // Older Android - use all flags
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Ensure window covers entire screen including system bars
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            // For Android 11+, set layoutInDisplayCutoutMode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        // Add view to window manager
        try {
            windowManager.addView(overlayView, params)
            isShowing = true

            // Phase 1.2: Haptic feedback on overlay appearance
            overlayView?.performHapticFeedback(
                android.view.HapticFeedbackConstants.LONG_PRESS
            )

            // Move lifecycle to RESUMED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED

            // Notify ViewModel
            viewModel.onOverlayShown(sessionId, targetApp, sessionStartTime, sessionDuration)

            ErrorLogger.info(
                "Timer overlay shown successfully for $targetApp (session duration: ${sessionDuration}ms)",
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

            // Invoke dismiss callback to notify service
            onDismissCallback?.invoke()
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
    snoozeDurationMinutes: Int,
    onDismiss: () -> Unit,
    onGoBackToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = androidx.compose.ui.platform.LocalView.current

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

    // Get gradient background based on content type (matching ReminderOverlay)
    val backgroundGradient = remember(uiState.interventionContent, isDarkTheme) {
        dev.sadakat.thinkfaster.ui.theme.InterventionGradients.getGradientForContent(
            uiState.interventionContent?.javaClass?.simpleName,
            isDarkTheme
        )
    }

    // Create a style object with gradient-compatible colors
    val style = object {
        val backgroundColor = Color(0xFF283593)  // Approximate gradient mid-point
        val textColor = dev.sadakat.thinkfaster.ui.theme.AccessibleColors.text(
            normalColor = dev.sadakat.thinkfaster.ui.theme.InterventionColors.InterventionTextPrimaryDark,
            highContrastColor = Color.White
        )
        val accentColor = Color(0xFF64B5F6)  // Light blue for dark gradients
        val primaryTextStyle = dev.sadakat.thinkfaster.ui.theme.InterventionTypography.InterventionMessage
        val secondaryTextStyle = dev.sadakat.thinkfaster.ui.theme.InterventionTypography.InterventionTitle
    }

    // Background gradient always visible
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        // Crossfade between different states to ensure smooth transitions
        Crossfade(
            targetState = when {
                uiState.showCelebration -> OverlayState.CELEBRATION
                uiState.isLoading -> OverlayState.LOADING
                else -> OverlayState.CONTENT
            },
            label = "overlay_state_transition"
        ) { state ->
            when (state) {
                OverlayState.CELEBRATION -> {
                    CelebrationScreen(
                        onComplete = {
                            // Celebration will show for 1.5s, then dismiss and go home
                            viewModel.onCelebrationComplete()
                        }
                    )
                }
                OverlayState.LOADING -> {
                    LoadingScreen()
                }
                OverlayState.CONTENT -> {
                    uiState.interventionContent?.let { content ->
                        DynamicInterventionContent(
                    content = content,
                    targetApp = uiState.targetApp,
                    sessionDuration = uiState.currentSessionDuration,
                    todaysTotalUsage = uiState.todaysTotalUsage,
                    timerAlertMinutes = uiState.timerAlertMinutes,
                    frictionLevel = uiState.frictionLevel,
                    showFeedbackPrompt = uiState.showFeedbackPrompt,
                    snoozeDurationMinutes = snoozeDurationMinutes,
                    onProceed = {
                        // Phase 1.2: Haptic feedback on button press
                        view.performHapticFeedback(
                            android.view.HapticFeedbackConstants.VIRTUAL_KEY
                        )
                        viewModel.onProceedClicked()
                    },
                    onGoBack = {
                        // Phase 1.2: Success haptic feedback on "Go Back"
                        view.performHapticFeedback(
                            android.view.HapticFeedbackConstants.CONFIRM
                        )
                        viewModel.onGoBackClicked()
                    },
                    onSnoozeClick = {  // Phase 2: Snooze callback
                        view.performHapticFeedback(
                            android.view.HapticFeedbackConstants.VIRTUAL_KEY
                        )
                        viewModel.onSnoozeClicked()
                    },
                    onFeedbackReceived = { feedback ->
                        // Phase 1.2: Haptic feedback on thumbs up/down
                        view.performHapticFeedback(
                            android.view.HapticFeedbackConstants.VIRTUAL_KEY
                        )
                        viewModel.onFeedbackReceived(feedback)
                    },
                    onSkipFeedback = {
                        viewModel.onSkipFeedback()
                    },
                    isDarkTheme = isDarkTheme
                )
                    }
                }
            }
        }
    }
}

// State enum for overlay content
private enum class OverlayState {
    LOADING,
    CONTENT,
    CELEBRATION
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
    content: dev.sadakat.thinkfaster.domain.model.InterventionContent,
    targetApp: String?,
    sessionDuration: String,
    todaysTotalUsage: String,
    timerAlertMinutes: Int,
    frictionLevel: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel,
    showFeedbackPrompt: Boolean,
    snoozeDurationMinutes: Int = 10,  // User-selected snooze duration
    onProceed: () -> Unit,
    onGoBack: () -> Unit,
    onSnoozeClick: () -> Unit,  // Phase 2: Snooze functionality
    onFeedbackReceived: (InterventionFeedback) -> Unit,
    onSkipFeedback: () -> Unit,
    isDarkTheme: Boolean
) {
    val style = InterventionStyling.getStyleForContent(content, isDarkTheme)
    val useTwoColumns = shouldUseTwoColumnLayout()

    // Get actual app name from package name using PackageManager
    val context = androidx.compose.ui.platform.LocalContext.current
    val appName = remember(targetApp) {
        targetApp?.let { packageName ->
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                // Fallback to simple name extraction if package not found
                packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: "App"
            }
        } ?: "App"
    }

    // Format timer alert duration dynamically
    val alertText = when {
        timerAlertMinutes >= 60 -> {
            val hours = timerAlertMinutes / 60
            val mins = timerAlertMinutes % 60
            if (mins > 0) "$hours-Hour $mins-Minute Alert" else "$hours-Hour Alert"
        }
        else -> "$timerAlertMinutes-Minute Alert"
    }

    if (useTwoColumns) {
        // Two-column landscape layout for tablets
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(Spacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left column: App name + content
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = appName,
                        style = InterventionTypography.AppName,
                        color = style.textColor,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = alertText,
                        style = InterventionTypography.InterventionSubtext,
                        color = style.secondaryTextColor,
                        textAlign = TextAlign.Center
                    )

                    InterventionContentRenderer(
                        content = content,
                        textColor = style.textColor,
                        secondaryTextColor = style.secondaryTextColor
                    )
                }

                // Right column: Actions
                Column(
                    modifier = Modifier.weight(0.7f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (showFeedbackPrompt) {
                        FeedbackPrompt(
                            onFeedback = onFeedbackReceived,
                            onDismiss = onSkipFeedback,
                            style = style
                        )
                    } else {
                        // Snooze + action buttons
                        OutlinedButton(
                            onClick = onSnoozeClick,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = style.containerColor,
                                contentColor = style.secondaryTextColor
                            ),
                            border = BorderStroke(1.5.dp, style.borderColor),
                            shape = Shapes.button
                        ) {
                            Icon(Icons.Default.Pause, null, Modifier.size(20.dp), style.iconColor)
                            Spacer(Modifier.width(8.dp))
                            Text("Snooze $snoozeDurationMinutes min", fontSize = 15.sp, color = style.textColor)
                        }

                        Spacer(Modifier.height(16.dp))

                        InterventionButtons(
                            frictionLevel = frictionLevel,
                            onProceed = onProceed,
                            onGoBack = onGoBack,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
    } else {
        // Original portrait layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section: App name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = appName,
                    style = InterventionTypography.AppName,
                    color = style.textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = alertText,
                    style = InterventionTypography.InterventionSubtext,
                    color = style.secondaryTextColor,
                    textAlign = TextAlign.Center
                )
            }

            // Middle section: Content-specific UI
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Use shared intervention content renderer
                InterventionContentRenderer(
                    content = content,
                    textColor = style.textColor,
                    secondaryTextColor = style.secondaryTextColor
                )
            }

            // Bottom section: Buttons / Feedback
            // Phase 1: Show feedback prompt OR action buttons
            AnimatedVisibility(
                visible = showFeedbackPrompt,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 }  // Slide up from 50% below
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it / 2 }
                ) + fadeOut()
            ) {
                FeedbackPrompt(
                    onFeedback = onFeedbackReceived,
                    onDismiss = onSkipFeedback,
                    style = style
                )
            }

            AnimatedVisibility(
                visible = !showFeedbackPrompt,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Phase 2: Snooze button
                    OutlinedButton(
                        onClick = onSnoozeClick,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = style.containerColor,
                            contentColor = style.secondaryTextColor
                        ),
                        border = BorderStroke(1.5.dp, style.borderColor),
                        shape = Shapes.button
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = style.iconColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Snooze for $snoozeDurationMinutes minutes",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = style.textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    InterventionButtons(
                        frictionLevel = frictionLevel,
                        onProceed = onProceed,
                        onGoBack = onGoBack,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun InterventionButtons(
    frictionLevel: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel,
    onProceed: () -> Unit,
    onGoBack: () -> Unit,
    isDarkTheme: Boolean
) {
    // Button colors: Use contrast-checked colors for gradient backgrounds
    val (baseGoBackColor, baseProceedColor) = InterventionStyling.getButtonColors(isDarkTheme)

    val backgroundAverage = Color(0xFF283593)  // Approximate mid-point of dark gradients

    val goBackColor = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.ensureButtonContrast(
        buttonColor = baseGoBackColor,
        backgroundColor = backgroundAverage,
        minContrast = 3.0f
    )
    val proceedColor = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.ensureButtonContrast(
        buttonColor = baseProceedColor,
        backgroundColor = backgroundAverage,
        minContrast = 3.0f
    )

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
                    color = if (isDarkTheme) InterventionColors.InterventionTextPrimaryDark.copy(alpha = 0.95f) else InterventionColors.InterventionTextPrimary.copy(alpha = 0.95f),
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

/**
 * Phase 1: Feedback prompt with excellent UX
 * Shows after user makes their choice (Go Back or Proceed)
 * Smooth slide-up animation with haptic feedback
 * Now uses gradient-aware colors for optimal contrast
 */
@Composable
private fun FeedbackPrompt(
    onFeedback: (InterventionFeedback) -> Unit,
    onDismiss: () -> Unit,
    style: dev.sadakat.thinkfaster.util.InterventionStyle
) {
    // Phase 1: Interaction sources for button press feedback
    val helpfulInteractionSource = remember { MutableInteractionSource() }
    val disruptiveInteractionSource = remember { MutableInteractionSource() }

    val helpfulPressed by helpfulInteractionSource.collectIsPressedAsState()
    val disruptivePressed by disruptiveInteractionSource.collectIsPressedAsState()

    // Scale animation on press for tactile feel
    val helpfulScale by animateFloatAsState(
        targetValue = if (helpfulPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "helpful_scale"
    )

    val disruptiveScale by animateFloatAsState(
        targetValue = if (disruptivePressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "disruptive_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = style.containerColor
        ),
        shape = Shapes.dialog
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Question text
            Text(
                text = "Was this intervention well-timed?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = style.textColor,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your feedback helps us show interventions at better times",
                fontSize = 14.sp,
                color = style.secondaryTextColor,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Thumbs up/down buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbs up button (Helpful)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedIconButton(
                        onClick = {
                            onFeedback(InterventionFeedback.HELPFUL)
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .scale(helpfulScale),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            containerColor = style.containerColor,
                            contentColor = style.iconColor
                        ),
                        border = BorderStroke(1.dp, style.borderColor),
                        interactionSource = helpfulInteractionSource
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Helpful - intervention was well-timed",
                            tint = style.iconColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Helpful",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = style.iconColor
                    )
                }

                // Thumbs down button (Disruptive)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedIconButton(
                        onClick = {
                            onFeedback(InterventionFeedback.DISRUPTIVE)
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .scale(disruptiveScale),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            containerColor = style.containerColor,
                            contentColor = style.iconColor
                        ),
                        border = BorderStroke(1.dp, style.borderColor),
                        interactionSource = disruptiveInteractionSource
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Disruptive - intervention was poorly-timed",
                            tint = style.iconColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Disruptive",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = style.iconColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Skip button - subtle, less prominent
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Skip feedback"
                }
            ) {
                Text(
                    text = "Skip",
                    fontSize = 14.sp,
                    color = style.secondaryTextColor,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
