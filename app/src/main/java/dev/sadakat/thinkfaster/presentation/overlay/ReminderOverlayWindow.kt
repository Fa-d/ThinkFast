package dev.sadakat.thinkfaster.presentation.overlay

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
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
import dev.sadakat.thinkfaster.domain.model.InterventionContent
import dev.sadakat.thinkfaster.domain.model.InterventionFeedback
import dev.sadakat.thinkfaster.presentation.overlay.components.CelebrationScreen
import dev.sadakat.thinkfaster.presentation.overlay.components.InterventionContentRenderer
import dev.sadakat.thinkfaster.presentation.overlay.components.OverlayEntranceAnimation
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.theme.InterventionColors
import dev.sadakat.thinkfaster.ui.theme.InterventionGradients
import dev.sadakat.thinkfaster.ui.theme.InterventionTypography
import dev.sadakat.thinkfaster.ui.theme.IntentlyerTheme
import dev.sadakat.thinkfaster.ui.theme.ResponsivePadding
import dev.sadakat.thinkfaster.ui.theme.ResponsiveFontSize
import dev.sadakat.thinkfaster.ui.theme.rememberAccessibilityState
import dev.sadakat.thinkfaster.ui.theme.AccessibleColors
import dev.sadakat.thinkfaster.ui.theme.adaptiveAnimationSpec
import dev.sadakat.thinkfaster.ui.theme.isLandscape
import dev.sadakat.thinkfaster.ui.theme.adaptiveColumnCount
import dev.sadakat.thinkfaster.ui.theme.shouldUseTwoColumnLayout
import dev.sadakat.thinkfaster.ui.theme.rememberScreenSize
import dev.sadakat.thinkfaster.ui.theme.ScreenSize
import dev.sadakat.thinkfaster.presentation.overlay.components.SmallScreenInterventionOverlay
import dev.sadakat.thinkfaster.util.InterventionStyling
import dev.sadakat.thinkfaster.util.ErrorLogger
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Screen state enum for reminder overlay content
 */
private enum class OverlayScreenState {
    LOADING,
    CONTENT,
    CELEBRATION
}

/**
 * WindowManager-based overlay that appears on top of all apps
 * Uses Compose UI embedded in a system window
 * Now renders context-aware dynamic content based on time, usage patterns, and user behavior
 */
class ReminderOverlayWindow(
    private val context: Context,
    private val onDismissCallback: (() -> Unit)? = null
) : KoinComponent, LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val interventionPreferences: dev.sadakat.thinkfaster.data.preferences.InterventionPreferences by inject()
    private val viewModel: ReminderOverlayViewModel by inject()
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
     * Show the reminder overlay
     * This method can be called from any thread - it will post to main thread
     */
    fun show(sessionId: Long, targetApp: String) {
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show(sessionId, targetApp) }
            return
        }

        if (isShowing) {
            ErrorLogger.warning(
                "Overlay already showing, ignoring show() request",
                context = "ReminderOverlayWindow.show"
            )
            return
        }

        // Recreate lifecycle objects to avoid DESTROYED → CREATED transition issues
        // This ensures fresh lifecycle state on each show
        lifecycleRegistry = LifecycleRegistry(this)
        savedStateRegistryController = SavedStateRegistryController.create(this)
        store = ViewModelStore()

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        // Create the overlay view
        overlayView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@ReminderOverlayWindow)
            setViewTreeViewModelStoreOwner(this@ReminderOverlayWindow)
            setViewTreeSavedStateRegistryOwner(this@ReminderOverlayWindow)

            setContent {
                IntentlyerTheme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    var showCelebration by remember { mutableStateOf(false) }
                    val isDarkTheme = isSystemInDarkTheme()

                    // Handle dismiss with celebration flow
                    LaunchedEffect(uiState.shouldDismiss) {
                        if (uiState.shouldDismiss) {
                            // If user chose "Go Back", show celebration first
                            if (uiState.userChoseGoBack && !showCelebration) {
                                showCelebration = true
                            } else if (!uiState.userChoseGoBack) {
                                // User chose "Proceed" - dismiss immediately
                                dismiss()
                                viewModel.onDismissHandled()
                            }
                        }
                    }

                    // Get gradient background based on content type
                    val backgroundGradient = remember(uiState.interventionContent, isDarkTheme) {
                        dev.sadakat.thinkfaster.ui.theme.InterventionGradients.getGradientForContent(
                            uiState.interventionContent?.javaClass?.simpleName,
                            isDarkTheme
                        )
                    }

                    // Phase 1.3: Enhanced entrance animation wrapper
                    OverlayEntranceAnimation {
                        // Background gradient always visible
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(backgroundGradient)
                        ) {
                            // Crossfade between different states for smooth transitions
                            Crossfade(
                                targetState = when {
                                    showCelebration -> OverlayScreenState.CELEBRATION
                                    uiState.isLoading -> OverlayScreenState.LOADING
                                    else -> OverlayScreenState.CONTENT
                                },
                                label = "reminder_overlay_state_transition"
                            ) { state ->
                                when (state) {
                                    OverlayScreenState.CELEBRATION -> {
                                        CelebrationScreen(
                                            onComplete = {
                                                goToHomeScreen()
                                                dismiss()
                                                viewModel.onDismissHandled()
                                            }
                                        )
                                    }
                                    OverlayScreenState.LOADING -> {
                                        LoadingScreen()
                                    }
                                    OverlayScreenState.CONTENT -> {
                                        // Route to small screen optimized overlay for narrow devices
                                        val screenSize = rememberScreenSize()
                                        if (screenSize == ScreenSize.SMALL) {
                                            SmallScreenInterventionOverlay(
                                                targetApp = targetApp,
                                                context = context,
                                                interventionContent = uiState.interventionContent,
                                                frictionLevel = uiState.interventionContext?.userFrictionLevel
                                                    ?: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.GENTLE,
                                                showFeedbackPrompt = uiState.showFeedbackPrompt,
                                                snoozeDurationMinutes = interventionPreferences.getSelectedSnoozeDuration(),
                                                onGoBackClick = {
                                                    handleGoBackClick(sessionId)
                                                },
                                                onProceedClick = {
                                                    handleProceedClick(sessionId)
                                                },
                                                onSnoozeClick = {
                                                    overlayView?.performHapticFeedback(
                                                        android.view.HapticFeedbackConstants.VIRTUAL_KEY
                                                    )
                                                    viewModel.onSnoozeClicked()
                                                },
                                                onFeedbackReceived = { feedback ->
                                                    overlayView?.performHapticFeedback(
                                                        android.view.HapticFeedbackConstants.VIRTUAL_KEY
                                                    )
                                                    viewModel.onFeedbackReceived(feedback)
                                                },
                                                onSkipFeedback = {
                                                    viewModel.onSkipFeedback()
                                                }
                                            )
                                        } else {
                                            ReminderOverlayContent(
                                                targetApp = targetApp,
                                                context = context,
                                                interventionContent = uiState.interventionContent,
                                                frictionLevel = uiState.interventionContext?.userFrictionLevel
                                                    ?: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.GENTLE,
                                                showFeedbackPrompt = uiState.showFeedbackPrompt,
                                                snoozeDurationMinutes = interventionPreferences.getSelectedSnoozeDuration(),
                                                onGoBackClick = {
                                                    handleGoBackClick(sessionId)
                                                },
                                                onProceedClick = {
                                                    handleProceedClick(sessionId)
                                                },
                                                onSnoozeClick = {  // Phase 2: Snooze callback
                                                    overlayView?.performHapticFeedback(
                                                        android.view.HapticFeedbackConstants.VIRTUAL_KEY
                                                    )
                                                    viewModel.onSnoozeClicked()
                                                },
                                                onFeedbackReceived = { feedback ->
                                                    // Phase 1.2: Haptic feedback on thumbs up/down
                                                    overlayView?.performHapticFeedback(
                                                        android.view.HapticFeedbackConstants.VIRTUAL_KEY
                                                    )
                                                    viewModel.onFeedbackReceived(feedback)
                                                },
                                                onSkipFeedback = {
                                                    viewModel.onSkipFeedback()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
            viewModel.onOverlayShown(sessionId, targetApp)

            ErrorLogger.info(
                "Reminder overlay shown successfully for $targetApp",
                context = "ReminderOverlayWindow.show"
            )
        } catch (e: SecurityException) {
            ErrorLogger.error(
                e,
                message = "SecurityException when adding overlay - likely missing overlay permission",
                context = "ReminderOverlayWindow.show"
            )
            overlayView = null
            isShowing = false
        } catch (e: Exception) {
            ErrorLogger.error(
                e,
                message = "Unexpected error when showing reminder overlay",
                context = "ReminderOverlayWindow.show"
            )
            overlayView = null
            isShowing = false
        }
    }

    /**
     * Handle proceed button click
     */
    private fun handleProceedClick(sessionId: Long) {
        // Phase 1.2: Haptic feedback on button press
        overlayView?.performHapticFeedback(
            android.view.HapticFeedbackConstants.VIRTUAL_KEY
        )

        CoroutineScope(Dispatchers.IO).launch {
            viewModel.onProceedClicked()
            // Note: dismiss() is called via LaunchedEffect when shouldDismiss becomes true
        }
    }

    /**
     * Handle go back button click (user chose NOT to proceed)
     */
    private fun handleGoBackClick(sessionId: Long) {
        // Phase 1.2: Success haptic feedback pattern on "Go Back"
        overlayView?.performHapticFeedback(
            android.view.HapticFeedbackConstants.CONFIRM
        )

        CoroutineScope(Dispatchers.IO).launch {
            viewModel.onGoBackClicked()
            // Note: dismiss() is called via LaunchedEffect when shouldDismiss becomes true
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

            // Call the dismiss callback
            onDismissCallback?.invoke()
        } catch (e: Exception) {
            ErrorLogger.error(
                e,
                message = "Error dismissing overlay",
                context = "ReminderOverlayWindow.dismiss"
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

/**
 * Background colors for different intervention types
 * Dark theme aware
 */
private fun getTextColor(isDarkTheme: Boolean): Color {
    return if (isDarkTheme)
        InterventionColors.InterventionTextPrimaryDark
    else
        InterventionColors.InterventionTextPrimary
}

private fun getSecondaryTextColor(isDarkTheme: Boolean): Color {
    return if (isDarkTheme)
        InterventionColors.InterventionTextSecondaryDark
    else
        InterventionColors.InterventionTextSecondary
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Color.White
        )
    }
}

@Composable
private fun ReminderOverlayContent(
    targetApp: String,
    context: Context,
    interventionContent: InterventionContent?,
    frictionLevel: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel,
    showFeedbackPrompt: Boolean,
    snoozeDurationMinutes: Int = 10,  // User-selected snooze duration
    onGoBackClick: () -> Unit,
    onProceedClick: () -> Unit,
    onSnoozeClick: () -> Unit,  // Phase 2: Snooze functionality
    onFeedbackReceived: (InterventionFeedback) -> Unit,
    onSkipFeedback: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    // Phase 4: Accessibility state
    val a11yState = rememberAccessibilityState()
    val isLandscapeMode = isLandscape()

    // Get actual app name from package name using PackageManager
    val appName = remember(targetApp) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(targetApp, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            // Fallback to simple name extraction if package not found
            targetApp.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: "App"
        }
    }

    // Animation states
    var visible by remember { mutableStateOf(false) }

    // Phase 4: Reduce motion support
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = adaptiveAnimationSpec(
            normalSpec = tween(300),
            reducedSpec = tween(0)
        ),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = adaptiveAnimationSpec(
            normalSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            reducedSpec = tween(0)
        ),
        label = "scale"
    )

    // Phase 4: High-contrast text colors (accessibility aware)
    // IMPORTANT: All gradients use dark backgrounds, so we ALWAYS use light text
    // regardless of system theme to ensure readability
    // Use gradient-aware colors from InterventionStyling
    val style = interventionContent?.let {
        InterventionStyling.getStyleForContent(it, isDarkTheme)
    } ?: run {
        // Fallback style when no content
        dev.sadakat.thinkfaster.util.InterventionStyle(
            backgroundColor = Color.Transparent,
            textColor = AccessibleColors.text(
                normalColor = InterventionColors.InterventionTextPrimaryDark,
                highContrastColor = Color.White
            ),
            accentColor = InterventionColors.Success,
            primaryTextStyle = InterventionTypography.InterventionMessage,
            secondaryTextStyle = InterventionTypography.InterventionSubtext,
            secondaryTextColor = AccessibleColors.text(
                normalColor = InterventionColors.InterventionTextSecondaryDark,
                highContrastColor = Color.White
            ),
            borderColor = Color.White.copy(alpha = 0.5f),
            containerColor = Color.White.copy(alpha = 0.55f),
            iconColor = AccessibleColors.text(
                normalColor = InterventionColors.InterventionTextPrimaryDark,
                highContrastColor = Color.White
            )
        )
    }

    // Local aliases for compatibility with existing code
    val textColor = style.textColor
    val secondaryTextColor = style.secondaryTextColor

    LaunchedEffect(interventionContent) {
        visible = true
    }

    // Phase 4: Accessibility semantic description
    val contentDescription = remember(appName, interventionContent) {
        buildString {
            append("Intervention screen for $appName. ")
            when (interventionContent) {
                is InterventionContent.ReflectionQuestion -> append("Reflection question: ${interventionContent.question}")
                is InterventionContent.TimeAlternative -> append("Time alternatives suggested for your usage")
                is InterventionContent.BreathingExercise -> append("Breathing exercise to help you pause")
                is InterventionContent.UsageStats -> append("Your usage statistics")
                is InterventionContent.EmotionalAppeal -> append("Mindfulness message: ${interventionContent.message}")
                is InterventionContent.Quote -> append("Inspirational quote from ${interventionContent.author}")
                is InterventionContent.Gamification -> append("Challenge: ${interventionContent.challenge}")
                is InterventionContent.ActivitySuggestion -> append("Suggested activity: ${interventionContent.suggestion}")
                null -> append("Mindfulness reminder")
            }
            append(". Two options: Go Back to home screen, or Proceed to app.")
        }
    }

    // Phase 4: Landscape layout support - two-column on tablets in landscape
    // Using shouldUseTwoColumnLayout() for consistency with other overlays
    val useLandscapeLayout = shouldUseTwoColumnLayout()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing) // Avoid system bars, notches, camera holes
            .alpha(alpha)
            .scale(scale)
            .padding(ResponsivePadding.overlay()) // Phase 4: Responsive padding
            .semantics { this.contentDescription = contentDescription }, // Phase 4: TalkBack
        contentAlignment = Alignment.Center
    ) {
        if (useLandscapeLayout) {
            // Phase 4: Two-column landscape layout for tablets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left column: Content
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = appName,
                        fontSize = ResponsiveFontSize.appName(),
                        fontWeight = FontWeight.Medium,
                        color = style.secondaryTextColor,
                        textAlign = TextAlign.Center
                    )

                    interventionContent?.let { content ->
                        InterventionContentRenderer(
                            content = content,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor
                        )
                    }
                }

                // Right column: Actions / Feedback
                Column(
                    modifier = Modifier.weight(0.7f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Phase 1: Show feedback prompt OR action buttons
                    AnimatedVisibility(
                        visible = showFeedbackPrompt,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 }
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
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Phase 2: Snooze button
                            OutlinedButton(
                                onClick = onSnoozeClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White.copy(
                                        alpha = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.getOptimalContainerAlpha(
                                            isOnGradient = true,
                                            baseAlpha = 0.15f
                                        )
                                    ),
                                    contentColor = textColor.copy(
                                        alpha = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.getOptimalTextAlpha(
                                            isPrimary = false,
                                            isOnGradient = true
                                        )
                                    )
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    textColor.copy(
                                        alpha = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.getOptimalBorderAlpha(
                                            isOnGradient = true
                                        )
                                    )
                                ),
                                shape = Shapes.button
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = textColor.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Snooze for $snoozeDurationMinutes minutes",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            InterventionActionButtons(
                                contentType = interventionContent?.javaClass?.simpleName,
                                frictionLevel = frictionLevel,
                                onGoBackClick = onGoBackClick,
                                onProceedClick = onProceedClick,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                isDarkTheme = isDarkTheme
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "This overlay helps you build mindful usage habits",
                                fontSize = 14.sp,
                                color = secondaryTextColor.copy(
                        alpha = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.getOptimalTextAlpha(
                            isPrimary = false,
                            isOnGradient = true
                        )
                    ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            // Standard portrait/phone layout
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = appName,
                    fontSize = ResponsiveFontSize.appName(),
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(
                        alpha = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.getOptimalTextAlpha(
                            isPrimary = false,
                            isOnGradient = true
                        )
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(0.3f))

                interventionContent?.let { content ->
                    InterventionContentRenderer(
                        content = content,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }

                Spacer(modifier = Modifier.weight(0.2f))

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
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(
                                    alpha = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.getOptimalContainerAlpha(
                                        isOnGradient = true,
                                        baseAlpha = 0.15f
                                    )
                                ),
                                contentColor = textColor.copy(
                                    alpha = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.getOptimalTextAlpha(
                                        isPrimary = false,
                                        isOnGradient = true
                                    )
                                )
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                textColor.copy(
                                    alpha = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.getOptimalBorderAlpha(
                                        isOnGradient = true
                                    )
                                )
                            ),
                            shape = Shapes.button
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = textColor.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Snooze for $snoozeDurationMinutes minutes",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        InterventionActionButtons(
                            contentType = interventionContent?.javaClass?.simpleName,
                            frictionLevel = frictionLevel,
                            onGoBackClick = onGoBackClick,
                            onProceedClick = onProceedClick,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            isDarkTheme = isDarkTheme
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "This overlay helps you build mindful usage habits",
                            fontSize = 14.sp,
                            color = secondaryTextColor.copy(
                        alpha = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.getOptimalTextAlpha(
                            isPrimary = false,
                            isOnGradient = true
                        )
                    ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Action buttons for intervention with progressive friction
 * Phase F: Delayed button appearance based on user tenure
 */
@Composable
private fun InterventionActionButtons(
    contentType: String?,
    frictionLevel: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel,
    onGoBackClick: () -> Unit,
    onProceedClick: () -> Unit,
    textColor: Color,
    secondaryTextColor: Color,
    isDarkTheme: Boolean
) {
    // Button colors: Use contrast-checked colors for gradient backgrounds
    // Approximate gradient mid-point for contrast checking
    val backgroundAverage = Color(0xFF283593)  // Approximate mid-point of dark gradients

    val goBackColor = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.ensureButtonContrast(
        buttonColor = InterventionColors.GoBackButtonDark,  // Light green
        backgroundColor = backgroundAverage,
        minContrast = 3.0f
    )
    val proceedColor = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.ensureButtonContrast(
        buttonColor = InterventionColors.ProceedButtonDark,  // Light gray
        backgroundColor = backgroundAverage,
        minContrast = 3.0f
    )

    var showButtons by remember { mutableStateOf(frictionLevel.delayMs == 0L) }
    var countdown by remember { mutableStateOf((frictionLevel.delayMs / 1000).toInt()) }

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

    if (!showButtons) {
        // Show countdown during delay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (frictionLevel) {
                    dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.MODERATE ->
                        "Take a breath and consider..."
                    dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.FIRM ->
                        "Pause. Think about what matters."
                    dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.LOCKED ->
                        "Maximum focus mode active."
                    else -> "Please wait..."
                },
                fontSize = 16.sp,
                color = secondaryTextColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Circular countdown indicator
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = proceedColor,
                    strokeWidth = 6.dp
                )

                Text(
                    text = "${countdown}s",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (frictionLevel) {
                    dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.LOCKED ->
                        "You requested this extra time to reflect"
                    else ->
                        "This brief pause helps you make a conscious choice"
                },
                fontSize = 14.sp,
                color = secondaryTextColor.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        // Phase 2.2 & 3.5: Enhanced button design with micro-interactions
        // Micro-interaction: Scale down on press for tactile feedback
        val goBackInteractionSource = remember { MutableInteractionSource() }
        val proceedInteractionSource = remember { MutableInteractionSource() }

        val goBackPressed by goBackInteractionSource.collectIsPressedAsState()
        val proceedPressed by proceedInteractionSource.collectIsPressedAsState()

        // Phase 4: Reduce motion support for button animations
        val goBackScale by animateFloatAsState(
            targetValue = if (goBackPressed) 0.95f else 1f,
            animationSpec = adaptiveAnimationSpec(
                normalSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                reducedSpec = tween(0)
            ),
            label = "go_back_scale"
        )

        val proceedScale by animateFloatAsState(
            targetValue = if (proceedPressed) 0.95f else 1f,
            animationSpec = adaptiveAnimationSpec(
                normalSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                reducedSpec = tween(0)
            ),
            label = "proceed_scale"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Go Back button - 60% width, filled, with home icon
            // Phase 4: 64dp height meets minimum touch target (48dp)
            Button(
                onClick = onGoBackClick,
                modifier = Modifier
                    .weight(0.6f) // 60% width
                    .height(64.dp) // Phase 4: Meets accessibility touch target
                    .scale(goBackScale) // Phase 3.5: Scale on press
                    .semantics {
                        contentDescription = "Go Back button. Returns to home screen instead of opening the app."
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = goBackColor
                ),
                interactionSource = goBackInteractionSource
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null, // Described by button semantics
                    tint = Color.White
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Go Back",
                    fontSize = ResponsiveFontSize.button(), // Phase 4: Responsive text
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Proceed button - 40% width, outlined, less prominent
            // Phase 4: 64dp height meets minimum touch target (48dp)
            OutlinedButton(
                onClick = onProceedClick,
                modifier = Modifier
                    .weight(0.4f) // 40% width
                    .height(64.dp) // Phase 4: Meets accessibility touch target
                    .scale(proceedScale) // Phase 3.5: Scale on press
                    .semantics {
                        contentDescription = "Proceed button. Opens the app you were trying to access."
                    },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = proceedColor
                ),
                interactionSource = proceedInteractionSource
            ) {
                Text(
                    text = "Proceed",
                    fontSize = ResponsiveFontSize.button(), // Phase 4: Responsive text
                    fontWeight = FontWeight.Medium,
                    color = proceedColor
                )
            }
        }
    }
}

/**
 * Phase 1: Feedback prompt with excellent UX
 * Shows after user makes their choice (Go Back or Proceed)
 * Smooth slide-up animation with haptic feedback
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
                            imageVector = Icons.Outlined.ThumbDown,
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
