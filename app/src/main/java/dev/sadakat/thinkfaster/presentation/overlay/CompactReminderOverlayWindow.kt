package dev.sadakat.thinkfaster.presentation.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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
import dev.sadakat.thinkfaster.presentation.overlay.components.CelebrationScreen
import dev.sadakat.thinkfaster.presentation.overlay.components.InterventionContentRenderer
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.theme.InterventionColors
import dev.sadakat.thinkfaster.ui.theme.InterventionGradients
import dev.sadakat.thinkfaster.ui.theme.IntentlyerTheme
import dev.sadakat.thinkfaster.ui.theme.shouldUseTwoColumnLayout
import dev.sadakat.thinkfaster.ui.theme.rememberScreenSize
import dev.sadakat.thinkfaster.ui.theme.ScreenSize
import dev.sadakat.thinkfaster.presentation.overlay.components.SmallScreenInterventionOverlay
import dev.sadakat.thinkfaster.util.ErrorLogger
import dev.sadakat.thinkfaster.util.InterventionStyling
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.roundToInt

/**
 * Screen state enum for compact overlay content
 */
private enum class CompactOverlayScreenState {
    CONTENT,
    CELEBRATION
}

/**
 * Compact reminder overlay - center card style (~70% screen) with dimmed background
 * Less intrusive alternative to full-screen overlay
 */
class CompactReminderOverlayWindow(
    private val context: Context,
    private val onDismissCallback: (() -> Unit)? = null
) : KoinComponent, LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val interventionPreferences: dev.sadakat.thinkfaster.data.preferences.InterventionPreferences by inject()
    private val viewModel: ReminderOverlayViewModel by inject()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: ComposeView? = null
    private var isShowing = false

    // Lifecycle management
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private var savedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)
    private var store: ViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    /**
     * Show the compact reminder overlay
     */
    fun show(sessionId: Long, targetApp: String) {
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show(sessionId, targetApp) }
            return
        }

        if (isShowing) {
            ErrorLogger.warning(
                "Compact overlay already showing, ignoring show() request",
                context = "CompactReminderOverlayWindow.show"
            )
            return
        }

        // Recreate lifecycle objects
        lifecycleRegistry = LifecycleRegistry(this)
        savedStateRegistryController = SavedStateRegistryController.create(this)
        store = ViewModelStore()

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        // Create the overlay view
        overlayView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@CompactReminderOverlayWindow)
            setViewTreeViewModelStoreOwner(this@CompactReminderOverlayWindow)
            setViewTreeSavedStateRegistryOwner(this@CompactReminderOverlayWindow)

            setContent {
                IntentlyerTheme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    var showCelebration by remember { mutableStateOf(false) }

                    // Handle dismiss with celebration flow
                    LaunchedEffect(uiState.shouldDismiss) {
                        if (uiState.shouldDismiss) {
                            if (uiState.userChoseGoBack && !showCelebration) {
                                showCelebration = true
                            } else if (!uiState.userChoseGoBack) {
                                dismiss()
                                viewModel.onDismissHandled()
                            }
                        }
                    }

                    if (showCelebration) {
                        CelebrationScreen(
                            onComplete = {
                                goToHomeScreen()
                                dismiss()
                                viewModel.onDismissHandled()
                            }
                        )
                    } else {
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
                            CompactOverlayContent(
                                targetApp = targetApp,
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
                                },
                                onDismiss = {
                                    dismiss()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Calculate responsive dimensions based on orientation
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val isLandscape = screenWidth > screenHeight

        // Adaptive sizing: landscape gets wider but shorter, portrait stays same
        val widthFraction = if (isLandscape) 0.70f else 0.85f
        val heightFraction = if (isLandscape) 0.60f else 0.75f

        val cardWidth = (screenWidth * widthFraction).toInt()
        val cardHeight = (screenHeight * heightFraction).toInt()

        // Setup window layout params - centered card with tap-outside enabled
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,  // Full screen for scrim
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or  // Allow outside touches
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        // Add view to window manager
        try {
            windowManager.addView(overlayView, params)
            isShowing = true

            // Haptic feedback
            overlayView?.performHapticFeedback(
                android.view.HapticFeedbackConstants.LONG_PRESS
            )

            // Move lifecycle to RESUMED
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED

            // Notify ViewModel
            viewModel.onOverlayShown(sessionId, targetApp)

            ErrorLogger.info(
                "Compact reminder overlay shown successfully for $targetApp",
                context = "CompactReminderOverlayWindow.show"
            )
        } catch (e: SecurityException) {
            ErrorLogger.error(
                e,
                message = "SecurityException when adding compact overlay",
                context = "CompactReminderOverlayWindow.show"
            )
            overlayView = null
            isShowing = false
        } catch (e: Exception) {
            ErrorLogger.error(
                e,
                message = "Unexpected error when showing compact reminder overlay",
                context = "CompactReminderOverlayWindow.show"
            )
            overlayView = null
            isShowing = false
        }
    }

    private fun handleProceedClick(sessionId: Long) {
        overlayView?.performHapticFeedback(
            android.view.HapticFeedbackConstants.VIRTUAL_KEY
        )
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.onProceedClicked()
        }
    }

    private fun handleGoBackClick(sessionId: Long) {
        overlayView?.performHapticFeedback(
            android.view.HapticFeedbackConstants.CONFIRM
        )
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.onGoBackClicked()
        }
    }

    fun dismiss() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { dismiss() }
            return
        }

        if (!isShowing || overlayView == null) {
            return
        }

        try {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            windowManager.removeView(overlayView)
            overlayView = null
            isShowing = false
            onDismissCallback?.invoke()
        } catch (e: Exception) {
            ErrorLogger.error(
                e,
                message = "Error dismissing compact overlay",
                context = "CompactReminderOverlayWindow.dismiss"
            )
        }
    }

    fun isShowing(): Boolean = isShowing

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)
    }
}

/**
 * Compact overlay content with center card, dimmed background, and gestures
 */
@Composable
private fun CompactOverlayContent(
    targetApp: String,
    interventionContent: dev.sadakat.thinkfaster.domain.model.InterventionContent?,
    frictionLevel: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel,
    showFeedbackPrompt: Boolean,
    snoozeDurationMinutes: Int,
    onGoBackClick: () -> Unit,
    onProceedClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onFeedbackReceived: (InterventionFeedback) -> Unit,
    onSkipFeedback: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    // Entrance animation state
    var visible by remember { mutableStateOf(false) }
    var offsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    // Scale + fade entrance animation
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "compact_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "compact_alpha"
    )

    // Get gradient background
    val backgroundGradient = remember(interventionContent, isDarkTheme) {
        InterventionGradients.getGradientForContent(
            interventionContent?.javaClass?.simpleName,
            isDarkTheme
        )
    }

    // Get styling
    val style = interventionContent?.let {
        InterventionStyling.getStyleForContent(it, isDarkTheme)
    } ?: run {
        dev.sadakat.thinkfaster.util.InterventionStyle(
            backgroundColor = Color.Transparent,
            textColor = Color.White,
            accentColor = InterventionColors.Success,
            primaryTextStyle = dev.sadakat.thinkfaster.ui.theme.InterventionTypography.InterventionMessage,
            secondaryTextStyle = dev.sadakat.thinkfaster.ui.theme.InterventionTypography.InterventionSubtext,
            secondaryTextColor = Color.White.copy(alpha = 0.7f),
            borderColor = Color.White.copy(alpha = 0.5f),
            containerColor = Color.White.copy(alpha = 0.15f),
            iconColor = Color.White
        )
    }

    val useTwoColumns = shouldUseTwoColumnLayout()

    // Full-screen container with tap-outside detection
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    // Tap outside to dismiss
                    onDismiss()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Dimmed scrim background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // Center card with swipe-to-dismiss
        Card(
            modifier = Modifier
                .fillMaxWidth(if (useTwoColumns) 0.70f else 0.85f)
                .fillMaxHeight(if (useTwoColumns) 0.60f else 0.75f)
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .scale(scale)
                .alpha(alpha)
                .pointerInput(Unit) {
                    // Prevent tap-through to background
                    detectTapGestures { }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (offsetY > 200) {
                                // Swipe down threshold exceeded - dismiss
                                onDismiss()
                            } else {
                                // Snap back with animation
                                coroutineScope.launch {
                                    animate(
                                        initialValue = offsetY,
                                        targetValue = 0f,
                                        animationSpec = spring()
                                    ) { value, _ ->
                                        offsetY = value
                                    }
                                }
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // Only allow downward swipe
                            offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                        }
                    )
                },
            shape = Shapes.dialog,
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundGradient)
            ) {
                // Swipe indicator at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                Color.White.copy(alpha = 0.4f),
                                shape = Shapes.progressIndicator
                            )
                    )
                }

                if (useTwoColumns) {
                    // Two-column layout for landscape tablets
                    Row(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left: Content
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // App name
                            Text(
                                text = "Intentlyer",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = style.textColor.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Intervention content
                            interventionContent?.let { content ->
                                InterventionContentRenderer(
                                    content = content,
                                    textColor = style.textColor,
                                    secondaryTextColor = style.secondaryTextColor
                                )
                            }
                        }

                        // Right: Actions
                        Column(
                            modifier = Modifier.weight(0.6f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (showFeedbackPrompt) {
                                CompactFeedbackPrompt(
                                    onFeedback = onFeedbackReceived,
                                    onDismiss = onSkipFeedback,
                                    style = style
                                )
                            } else {
                                // Snooze button
                                OutlinedButton(
                                    onClick = onSnoozeClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.15f),
                                        contentColor = style.textColor
                                    ),
                                    border = BorderStroke(1.5.dp, style.textColor.copy(alpha = 0.3f)),
                                    shape = Shapes.button
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = style.textColor.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Snooze $snoozeDurationMinutes min",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = style.textColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                CompactActionButtons(
                                    frictionLevel = frictionLevel,
                                    onGoBackClick = onGoBackClick,
                                    onProceedClick = onProceedClick,
                                    textColor = style.textColor,
                                    isDarkTheme = isDarkTheme
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Building mindful usage habits",
                                    fontSize = 13.sp,
                                    color = style.secondaryTextColor.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Single-column layout with buttons at bottom
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Scrollable content area
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // App name
                            Text(
                                text = "Intentlyer",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = style.textColor.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Intervention content (shared composable)
                            interventionContent?.let { content ->
                                InterventionContentRenderer(
                                    content = content,
                                    textColor = style.textColor,
                                    secondaryTextColor = style.secondaryTextColor
                                )
                            }

                            // Extra padding at bottom for button area
                            Spacer(modifier = Modifier.height(250.dp))
                        }

                        // Fixed buttons at bottom with gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.3f)
                                        )
                                    )
                                )
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (showFeedbackPrompt) {
                                    CompactFeedbackPrompt(
                                        onFeedback = onFeedbackReceived,
                                        onDismiss = onSkipFeedback,
                                        style = style
                                    )
                                } else {
                                    // Snooze button
                                    OutlinedButton(
                                        onClick = onSnoozeClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.White.copy(alpha = 0.15f),
                                            contentColor = style.textColor
                                        ),
                                        border = BorderStroke(1.5.dp, style.textColor.copy(alpha = 0.3f)),
                                        shape = Shapes.button
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Pause,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = style.textColor.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Snooze $snoozeDurationMinutes min",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = style.textColor
                                        )
                                    }

                                    CompactActionButtons(
                                        frictionLevel = frictionLevel,
                                        onGoBackClick = onGoBackClick,
                                        onProceedClick = onProceedClick,
                                        textColor = style.textColor,
                                        isDarkTheme = isDarkTheme
                                    )

                                    Text(
                                        text = "Building mindful usage habits",
                                        fontSize = 13.sp,
                                        color = style.secondaryTextColor.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact feedback prompt
 */
@Composable
private fun CompactFeedbackPrompt(
    onFeedback: (InterventionFeedback) -> Unit,
    onDismiss: () -> Unit,
    style: dev.sadakat.thinkfaster.util.InterventionStyle
) {
    val helpfulInteractionSource = remember { MutableInteractionSource() }
    val disruptiveInteractionSource = remember { MutableInteractionSource() }

    val helpfulPressed by helpfulInteractionSource.collectIsPressedAsState()
    val disruptivePressed by disruptiveInteractionSource.collectIsPressedAsState()

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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Was this well-timed?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = style.textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbs up
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedIconButton(
                    onClick = { onFeedback(InterventionFeedback.HELPFUL) },
                    modifier = Modifier
                        .size(64.dp)
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
                        contentDescription = "Helpful",
                        tint = style.iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Helpful",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = style.iconColor
                )
            }

            // Thumbs down
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedIconButton(
                    onClick = { onFeedback(InterventionFeedback.DISRUPTIVE) },
                    modifier = Modifier
                        .size(64.dp)
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
                        contentDescription = "Disruptive",
                        tint = style.iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Disruptive",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = style.iconColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.semantics {
                contentDescription = "Skip feedback"
            }
        ) {
            Text(
                text = "Skip",
                fontSize = 13.sp,
                color = style.secondaryTextColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * Compact action buttons with friction support
 */
@Composable
private fun CompactActionButtons(
    frictionLevel: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel,
    onGoBackClick: () -> Unit,
    onProceedClick: () -> Unit,
    textColor: Color,
    isDarkTheme: Boolean
) {
    val goBackInteractionSource = remember { MutableInteractionSource() }
    val proceedInteractionSource = remember { MutableInteractionSource() }

    val goBackPressed by goBackInteractionSource.collectIsPressedAsState()
    val proceedPressed by proceedInteractionSource.collectIsPressedAsState()

    val goBackScale by animateFloatAsState(
        targetValue = if (goBackPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "go_back_scale"
    )

    val proceedScale by animateFloatAsState(
        targetValue = if (proceedPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "proceed_scale"
    )

    val backgroundAverage = Color(0xFF283593)
    val goBackColor = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.ensureButtonContrast(
        buttonColor = InterventionColors.GoBackButtonDark,
        backgroundColor = backgroundAverage,
        minContrast = 3.0f
    )
    val proceedColor = dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils.ensureButtonContrast(
        buttonColor = InterventionColors.ProceedButtonDark,
        backgroundColor = backgroundAverage,
        minContrast = 3.0f
    )

    var showButtons by remember { mutableStateOf(frictionLevel.delayMs == 0L) }
    var countdown by remember { mutableStateOf((frictionLevel.delayMs / 1000).toInt()) }

    LaunchedEffect(frictionLevel.delayMs) {
        if (frictionLevel.delayMs > 0) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            showButtons = true
        }
    }

    if (!showButtons) {
        // Countdown display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Take a moment to reflect...",
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp),
                    color = proceedColor,
                    strokeWidth = 4.dp
                )
                Text(
                    text = "${countdown}s",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Go Back button
            Button(
                onClick = onGoBackClick,
                modifier = Modifier
                    .weight(0.6f)
                    .height(56.dp)
                    .scale(goBackScale),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goBackColor
                ),
                interactionSource = goBackInteractionSource
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Go Back",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Proceed button
            OutlinedButton(
                onClick = onProceedClick,
                modifier = Modifier
                    .weight(0.4f)
                    .height(56.dp)
                    .scale(proceedScale),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = proceedColor
                ),
                interactionSource = proceedInteractionSource
            ) {
                Text(
                    text = "Proceed",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = proceedColor
                )
            }
        }
    }
}
