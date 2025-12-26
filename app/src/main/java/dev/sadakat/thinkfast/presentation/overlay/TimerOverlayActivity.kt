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
 * Full-screen overlay activity shown after 10 minutes of continuous usage
 * Cannot be dismissed with back button - forces user to click "I Understand"
 */
class TimerOverlayActivity : ComponentActivity() {

    private val viewModel: TimerOverlayViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity full-screen and show over lockscreen
        setupFullScreen()

        // Extract session data from intent
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

        // Notify ViewModel that overlay is shown
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

    /**
     * Disable back button - user must click "I Understand"
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
fun TimerOverlayScreen(
    viewModel: TimerOverlayViewModel,
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
            TimerAlertContent(
                targetApp = uiState.targetApp,
                sessionStartTime = uiState.sessionStartTime,
                currentSessionDuration = uiState.currentSessionDuration,
                todaysTotalUsage = uiState.todaysTotalUsage,
                onAcknowledgeClick = { viewModel.onAcknowledgeClicked() }
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
private fun TimerAlertContent(
    targetApp: AppTarget?,
    sessionStartTime: String,
    currentSessionDuration: String,
    todaysTotalUsage: String,
    onAcknowledgeClick: () -> Unit
) {
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
            text = "10 Minutes Alert",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App name
        Text(
            text = targetApp?.displayName ?: "Target App",
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
                    value = sessionStartTime
                )

                // Current session
                StatRow(
                    label = "Current Session",
                    value = currentSessionDuration,
                    valueColor = MaterialTheme.colorScheme.error
                )

                // Today's total
                StatRow(
                    label = "Today's Total",
                    value = todaysTotalUsage,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Message
        Text(
            text = "You've been using this app for 10 minutes continuously.\n\nConsider taking a break or switching to a productive task.",
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
