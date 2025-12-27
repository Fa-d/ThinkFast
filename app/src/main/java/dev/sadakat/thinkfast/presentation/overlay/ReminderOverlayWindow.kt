package dev.sadakat.thinkfast.presentation.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.InterventionContent
import dev.sadakat.thinkfast.presentation.overlay.components.CompactBreathingExercise
import dev.sadakat.thinkfast.ui.theme.InterventionColors
import dev.sadakat.thinkfast.ui.theme.ThinkFastTheme
import dev.sadakat.thinkfast.util.ErrorLogger
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
    fun show(sessionId: Long, targetApp: AppTarget) {
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

            // Make overlay cover system bars (status bar and navigation bar)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ - use WindowInsetsController
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
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    // Handle dismiss
                    LaunchedEffect(uiState.shouldDismiss) {
                        if (uiState.shouldDismiss) {
                            // If user chose "Go Back", navigate to home screen
                            if (uiState.userChoseGoBack) {
                                goToHomeScreen()
                            }
                            dismiss()
                            viewModel.onDismissHandled()
                        }
                    }

                    ReminderOverlayContent(
                        targetApp = targetApp,
                        interventionContent = uiState.interventionContent,
                        frictionLevel = uiState.interventionContext?.userFrictionLevel
                            ?: dev.sadakat.thinkfast.domain.intervention.FrictionLevel.GENTLE,
                        onGoBackClick = {
                            handleGoBackClick(sessionId)
                        },
                        onProceedClick = {
                            handleProceedClick(sessionId)
                        }
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
            // Focusable + touch modal to intercept all user interaction
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
            viewModel.onOverlayShown(sessionId, targetApp)

            ErrorLogger.info(
                "Reminder overlay shown successfully for ${targetApp.displayName}",
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
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.onProceedClicked()
            // Note: dismiss() is called via LaunchedEffect when shouldDismiss becomes true
        }
    }

    /**
     * Handle go back button click (user chose NOT to proceed)
     */
    private fun handleGoBackClick(sessionId: Long) {
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
private fun getBackgroundColor(content: InterventionContent?, isDarkTheme: Boolean): Color {
    return when (content) {
        is InterventionContent.ReflectionQuestion -> if (isDarkTheme)
            InterventionColors.ReflectionBackgroundDark
        else
            InterventionColors.ReflectionBackground
        is InterventionContent.TimeAlternative -> if (isDarkTheme)
            InterventionColors.TimerAlertBackgroundDark
        else
            InterventionColors.TimerAlertBackground
        is InterventionContent.BreathingExercise -> if (isDarkTheme)
            InterventionColors.BreathingBackgroundDark
        else
            InterventionColors.BreathingBackground
        is InterventionContent.UsageStats -> if (isDarkTheme)
            InterventionColors.StatsBackgroundDark
        else
            InterventionColors.StatsBackground
        is InterventionContent.EmotionalAppeal -> if (isDarkTheme)
            InterventionColors.EmotionalAppealBackgroundDark
        else
            InterventionColors.EmotionalAppealBackground
        is InterventionContent.Quote -> if (isDarkTheme)
            InterventionColors.QuoteBackgroundDark
        else
            InterventionColors.QuoteBackground
        is InterventionContent.Gamification -> if (isDarkTheme)
            InterventionColors.GamificationBackgroundDark
        else
            InterventionColors.GamificationBackground
        null -> if (isDarkTheme)
            InterventionColors.GentleReminderBackgroundDark
        else
            InterventionColors.GentleReminderBackground
    }
}

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
private fun ReminderOverlayContent(
    targetApp: AppTarget,
    interventionContent: InterventionContent?,
    frictionLevel: dev.sadakat.thinkfast.domain.intervention.FrictionLevel,
    onGoBackClick: () -> Unit,
    onProceedClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    // Animation states
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Background color based on content type (dark theme aware)
    val backgroundColor by animateColorAsState(
        targetValue = getBackgroundColor(interventionContent, isDarkTheme),
        animationSpec = tween(300),
        label = "backgroundColor"
    )

    // Text colors (dark theme aware)
    val textColor = getTextColor(isDarkTheme)
    val secondaryTextColor = getSecondaryTextColor(isDarkTheme)

    LaunchedEffect(interventionContent) {
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .alpha(alpha)
            .scale(scale)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App name (always shown)
        Text(
            text = targetApp.displayName,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Dynamic content based on intervention type
        interventionContent?.let { content ->
            InterventionContentRenderer(
                content = content,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor
            )
        }

        Spacer(modifier = Modifier.height(64.dp))

        // Action buttons with progressive friction
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

        // Small notice text
        Text(
            text = "This overlay helps you build mindful usage habits",
            fontSize = 14.sp,
            color = secondaryTextColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Renders different types of intervention content
 */
@Composable
private fun InterventionContentRenderer(
    content: InterventionContent,
    textColor: Color,
    secondaryTextColor: Color
) {
    when (content) {
        is InterventionContent.ReflectionQuestion -> ReflectionQuestionContent(content, textColor, secondaryTextColor)
        is InterventionContent.TimeAlternative -> TimeAlternativeContent(content, textColor, secondaryTextColor)
        is InterventionContent.BreathingExercise -> BreathingExerciseContent(content, textColor)
        is InterventionContent.UsageStats -> UsageStatsContent(content, textColor, secondaryTextColor)
        is InterventionContent.EmotionalAppeal -> EmotionalAppealContent(content, textColor, secondaryTextColor)
        is InterventionContent.Quote -> QuoteContent(content, textColor, secondaryTextColor)
        is InterventionContent.Gamification -> GamificationContent(content, textColor, secondaryTextColor)
    }
}

/**
 * Reflection question content
 */
@Composable
private fun ReflectionQuestionContent(
    content: InterventionContent.ReflectionQuestion,
    textColor: Color,
    secondaryTextColor: Color
) {
    Text(
        text = content.question,
        fontSize = 26.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Serif,
        color = textColor,
        textAlign = TextAlign.Center,
        lineHeight = 36.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = content.subtext,
        fontSize = 16.sp,
        color = secondaryTextColor,
        textAlign = TextAlign.Center,
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
    )
}

/**
 * Time alternative content (loss framing)
 */
@Composable
private fun TimeAlternativeContent(
    content: InterventionContent.TimeAlternative,
    textColor: Color,
    secondaryTextColor: Color
) {
    Text(
        text = "${content.prefix}...",
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "${content.alternative.emoji} ${content.alternative.activity}",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        textAlign = TextAlign.Center,
        lineHeight = 36.sp
    )
}

/**
 * Breathing exercise content
 * Uses the shared CompactBreathingExercise component with improved animation
 */
@Composable
private fun BreathingExerciseContent(
    content: InterventionContent.BreathingExercise,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Instruction text above the breathing exercise
        Text(
            text = content.instruction,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Use the shared CompactBreathingExercise component with improved animation
        // It uses the same natural EaseInOutCubic easing and smooth transitions
        CompactBreathingExercise(
            variant = content.variant,
            isDarkTheme = isSystemInDarkTheme(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Usage stats content
 */
@Composable
private fun UsageStatsContent(
    content: InterventionContent.UsageStats,
    textColor: Color,
    secondaryTextColor: Color
) {
    Text(
        text = content.message,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = textColor,
        textAlign = TextAlign.Center,
        lineHeight = 26.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        StatItem("Today", "${content.todayMinutes}m", textColor, secondaryTextColor)
        content.yesterdayMinutes.let { StatItem("Yesterday", "${it}m", textColor, secondaryTextColor) }
        content.weekAverage.let { StatItem("Week Avg", "${it}m", textColor, secondaryTextColor) }
    }
}

@Composable
private fun StatItem(label: String, value: String, textColor: Color, secondaryTextColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = secondaryTextColor
        )
    }
}

/**
 * Emotional appeal content
 */
@Composable
private fun EmotionalAppealContent(
    content: InterventionContent.EmotionalAppeal,
    textColor: Color,
    secondaryTextColor: Color
) {
    Text(
        text = content.message,
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        color = textColor,
        textAlign = TextAlign.Center,
        lineHeight = 32.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = content.subtext,
        fontSize = 16.sp,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )
}

/**
 * Quote content
 */
@Composable
private fun QuoteContent(
    content: InterventionContent.Quote,
    textColor: Color,
    secondaryTextColor: Color
) {
    Text(
        text = """"${content.quote}"""",
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Serif,
        color = textColor,
        textAlign = TextAlign.Center,
        lineHeight = 32.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "— ${content.author}",
        fontSize = 16.sp,
        color = secondaryTextColor,
        textAlign = TextAlign.Center,
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
    )
}

/**
 * Gamification content
 */
@Composable
private fun GamificationContent(
    content: InterventionContent.Gamification,
    textColor: Color,
    secondaryTextColor: Color
) {
    Text(
        text = "🏆 ${content.challenge}",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = content.reward,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        color = secondaryTextColor,
        textAlign = TextAlign.Center
    )
}

/**
 * Action buttons for intervention with progressive friction
 * Phase F: Delayed button appearance based on user tenure
 */
@Composable
private fun InterventionActionButtons(
    contentType: String?,
    frictionLevel: dev.sadakat.thinkfast.domain.intervention.FrictionLevel,
    onGoBackClick: () -> Unit,
    onProceedClick: () -> Unit,
    textColor: Color,
    secondaryTextColor: Color,
    isDarkTheme: Boolean
) {
    // Get theme-aware button colors
    val (goBackColor, proceedColor) = if (isDarkTheme) {
        Pair(InterventionColors.GoBackButtonDark, InterventionColors.ProceedButtonDark)
    } else {
        Pair(InterventionColors.GoBackButton, InterventionColors.ProceedButton)
    }

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
                    dev.sadakat.thinkfast.domain.intervention.FrictionLevel.MODERATE ->
                        "Take a breath and consider..."
                    dev.sadakat.thinkfast.domain.intervention.FrictionLevel.FIRM ->
                        "Pause. Think about what matters."
                    dev.sadakat.thinkfast.domain.intervention.FrictionLevel.LOCKED ->
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
                    dev.sadakat.thinkfast.domain.intervention.FrictionLevel.LOCKED ->
                        "You requested this extra time to reflect"
                    else ->
                        "This brief pause helps you make a conscious choice"
                },
                fontSize = 14.sp,
                color = secondaryTextColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        // Show buttons after delay (or immediately for GENTLE)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Go Back button (prominent green)
            Button(
                onClick = onGoBackClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goBackColor
                )
            ) {
                Text(
                    text = "Go Back",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Proceed button (neutral gray)
            Button(
                onClick = onProceedClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = proceedColor
                )
            ) {
                Text(
                    text = "Proceed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
