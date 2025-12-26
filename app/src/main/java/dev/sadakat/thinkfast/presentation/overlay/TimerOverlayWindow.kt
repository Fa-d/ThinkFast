package dev.sadakat.thinkfast.presentation.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * WindowManager-based timer alert overlay
 * Shown after configured duration of continuous usage
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
        ErrorLogger.debug(
            "show() called for ${targetApp.displayName}, isShowing=$isShowing, thread=${Thread.currentThread().name}, duration=${sessionDuration}ms",
            context = "TimerOverlayWindow.show"
        )

        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ErrorLogger.debug(
                "Not on main thread, posting to main handler",
                context = "TimerOverlayWindow.show"
            )
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
        // This ensures fresh lifecycle state on each show
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
                    TimerOverlayContent(
                        viewModel = viewModel,
                        sessionId = sessionId,
                        targetApp = targetApp,
                        sessionStartTime = sessionStartTime,
                        sessionDuration = sessionDuration,
                        onAcknowledgeClick = {
                            handleAcknowledgeClick()
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
            ErrorLogger.debug(
                "Adding timer overlay view to WindowManager for ${targetApp.displayName}",
                context = "TimerOverlayWindow.show"
            )

            windowManager.addView(overlayView, params)
            isShowing = true

            ErrorLogger.debug(
                "Timer overlay view added successfully, transitioning to RESUMED state",
                context = "TimerOverlayWindow.show"
            )

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
     * Handle acknowledge button click
     */
    private fun handleAcknowledgeClick() {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.onAcknowledgeClicked()
            // Dismiss overlay
            dismiss()
        }
    }

    /**
     * Dismiss the overlay
     * This method can be called from any thread - it will post to main thread
     */
    fun dismiss() {
        ErrorLogger.debug(
            "dismiss() called, isShowing=$isShowing, thread=${Thread.currentThread().name}",
            context = "TimerOverlayWindow.dismiss"
        )

        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ErrorLogger.debug(
                "Not on main thread, posting to main handler",
                context = "TimerOverlayWindow.dismiss"
            )
            mainHandler.post { dismiss() }
            return
        }

        if (!isShowing || overlayView == null) {
            ErrorLogger.debug(
                "Timer overlay not showing or view is null, skipping dismiss",
                context = "TimerOverlayWindow.dismiss"
            )
            return
        }

        try {
            // Move lifecycle to DESTROYED
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

            windowManager.removeView(overlayView)
            overlayView = null
            isShowing = false

            ErrorLogger.debug(
                "Timer overlay dismissed successfully",
                context = "TimerOverlayWindow.dismiss"
            )
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
}

@Composable
private fun TimerOverlayContent(
    viewModel: TimerOverlayViewModel,
    sessionId: Long,
    targetApp: AppTarget,
    sessionStartTime: Long,
    sessionDuration: Long,
    onAcknowledgeClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Alert icon and title
        Text(
            text = "⏰",
            fontSize = 72.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Timer Alert",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App name
        Text(
            text = targetApp.displayName,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Usage statistics card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Session started
                StatRow(
                    label = "Session Started",
                    value = uiState.sessionStartTime
                )

                // Current session
                StatRow(
                    label = "Current Session",
                    value = uiState.currentSessionDuration,
                    valueColor = MaterialTheme.colorScheme.error
                )

                // Today's total
                StatRow(
                    label = "Today's Total",
                    value = uiState.todaysTotalUsage,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Message
        Text(
            text = "You've been using this app continuously for a while.\n\nConsider taking a break or switching to a productive task.",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Acknowledge button
        Button(
            onClick = onAcknowledgeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "I Understand",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Small notice text
        Text(
            text = "The session will end when you acknowledge this alert",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
