package dev.sadakat.thinkfast.presentation.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.UsageEvent
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * ViewModel for TimerOverlayActivity
 * Manages the 10-minute alert overlay state and usage statistics
 */
class TimerOverlayViewModel(
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerOverlayState())
    val uiState: StateFlow<TimerOverlayState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)

    /**
     * Called when the overlay is shown to the user
     * Loads session statistics and logs the alert event
     */
    fun onOverlayShown(
        sessionId: Long,
        targetApp: AppTarget,
        sessionStartTime: Long,
        sessionDuration: Long
    ) {
        viewModelScope.launch {
            val date = dateFormatter.format(Date(sessionStartTime))

            // Get today's total usage for this app
            val todaysSessions = usageRepository.getSessionsByDate(date)
            val todaysUsageForApp = todaysSessions
                .filter { it.targetApp == targetApp.packageName }
                .sumOf { it.duration }

            // Format times
            val startTimeFormatted = timeFormatter.format(Date(sessionStartTime))
            val currentDurationFormatted = formatDuration(sessionDuration)
            val todaysTotalFormatted = formatDuration(todaysUsageForApp + sessionDuration)

            _uiState.value = TimerOverlayState(
                sessionId = sessionId,
                targetApp = targetApp,
                sessionStartTime = startTimeFormatted,
                currentSessionDuration = currentDurationFormatted,
                todaysTotalUsage = todaysTotalFormatted,
                isLoading = false
            )

            // Log timer alert shown event
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = sessionId,
                    eventType = Constants.EVENT_TIMER_ALERT_SHOWN,
                    timestamp = System.currentTimeMillis(),
                    metadata = "Session duration: ${sessionDuration}ms, App: ${targetApp.displayName}"
                )
            )
        }
    }

    /**
     * Called when user clicks "I Understand" button
     * Logs the acknowledgment event and signals completion
     */
    fun onAcknowledgeClicked() {
        val currentState = _uiState.value
        if (currentState.sessionId == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Log timer acknowledged event
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = currentState.sessionId,
                    eventType = Constants.EVENT_TIMER_ACKNOWLEDGED,
                    timestamp = System.currentTimeMillis(),
                    metadata = "User acknowledged 10-minute alert"
                )
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                shouldDismiss = true
            )
        }
    }

    /**
     * Reset the dismiss flag after handling
     */
    fun onDismissHandled() {
        _uiState.value = _uiState.value.copy(shouldDismiss = false)
    }

    /**
     * Format duration in milliseconds to human-readable format
     */
    private fun formatDuration(durationMillis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

        return when {
            minutes >= 60 -> {
                val hours = minutes / 60
                val mins = minutes % 60
                "${hours}h ${mins}m"
            }
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}

/**
 * UI state for the timer overlay screen
 */
data class TimerOverlayState(
    val sessionId: Long? = null,
    val targetApp: AppTarget? = null,
    val sessionStartTime: String = "",
    val currentSessionDuration: String = "",
    val todaysTotalUsage: String = "",
    val isLoading: Boolean = true,
    val shouldDismiss: Boolean = false
)
