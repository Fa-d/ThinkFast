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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.ui.theme.ThinkFastTheme
import dev.sadakat.thinkfast.util.Constants
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
        ErrorLogger.debug(
            "show() called for ${targetApp.displayName}, isShowing=$isShowing, thread=${Thread.currentThread().name}",
            context = "ReminderOverlayWindow.show"
        )

        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ErrorLogger.debug(
                "Not on main thread, posting to main handler",
                context = "ReminderOverlayWindow.show"
            )
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
                    ReminderOverlayContent(
                        targetApp = targetApp,
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
            ErrorLogger.debug(
                "Adding overlay view to WindowManager for ${targetApp.displayName}",
                context = "ReminderOverlayWindow.show"
            )

            windowManager.addView(overlayView, params)
            isShowing = true

            ErrorLogger.debug(
                "Overlay view added successfully, transitioning to RESUMED state",
                context = "ReminderOverlayWindow.show"
            )

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
            context = "ReminderOverlayWindow.dismiss"
        )

        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ErrorLogger.debug(
                "Not on main thread, posting to main handler",
                context = "ReminderOverlayWindow.dismiss"
            )
            mainHandler.post { dismiss() }
            return
        }

        if (!isShowing || overlayView == null) {
            ErrorLogger.debug(
                "Overlay not showing or view is null, skipping dismiss",
                context = "ReminderOverlayWindow.dismiss"
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
                "Overlay dismissed successfully",
                context = "ReminderOverlayWindow.dismiss"
            )

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
}

@Composable
private fun ReminderOverlayContent(
    targetApp: AppTarget,
    onProceedClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App name
        Text(
            text = targetApp.displayName,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Reminder message
        Text(
            text = Constants.DEFAULT_REMINDER_MESSAGE,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Subtext
        Text(
            text = "Take a moment to consider if this is the best use of your time right now.",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Proceed button
        Button(
            onClick = onProceedClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Proceed",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Small notice text
        Text(
            text = "This overlay helps you build mindful usage habits",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
