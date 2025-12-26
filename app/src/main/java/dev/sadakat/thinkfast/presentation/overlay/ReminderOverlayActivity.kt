package dev.sadakat.thinkfast.presentation.overlay

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.ui.theme.ThinkFastTheme
import dev.sadakat.thinkfast.util.Constants
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Full-screen overlay activity shown when user launches a target app
 * Cannot be dismissed with back button - forces user to click "Proceed"
 */
class ReminderOverlayActivity : ComponentActivity() {

    private val viewModel: ReminderOverlayViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity full-screen and show over lockscreen
        setupFullScreen()

        // Extract session data from intent
        val sessionId = intent.getLongExtra(Constants.EXTRA_SESSION_ID, -1L)
        val targetAppPackage = intent.getStringExtra(Constants.EXTRA_TARGET_APP)

        if (sessionId == -1L || targetAppPackage == null) {
            finish()
            return
        }

        val targetApp = AppTarget.fromPackageName(targetAppPackage)
        if (targetApp == null) {
            finish()
            return
        }

        // Notify ViewModel that overlay is shown
        viewModel.onOverlayShown(sessionId, targetApp)

        setContent {
            ThinkFastTheme {
                ReminderOverlayScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }

    /**
     * Disable back button - user must click "Proceed"
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing - prevent dismissal via back button
    }

    /**
     * Setup full-screen flags and window properties
     */
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
fun ReminderOverlayScreen(
    viewModel: ReminderOverlayViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle dismissal
    LaunchedEffect(uiState.shouldDismiss) {
        if (uiState.shouldDismiss) {
            viewModel.onDismissHandled()
            onDismiss()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isLoading) {
            LoadingScreen()
        } else {
            ReminderContent(
                targetApp = uiState.targetApp,
                onProceedClick = { viewModel.onProceedClicked() }
            )
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
private fun ReminderContent(
    targetApp: AppTarget?,
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
            text = targetApp?.displayName ?: "Target App",
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
