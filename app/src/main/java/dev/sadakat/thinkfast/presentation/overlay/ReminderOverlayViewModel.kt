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

/**
 * ViewModel for ReminderOverlayActivity
 * Manages the reminder overlay state and event logging
 */
class ReminderOverlayViewModel(
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderOverlayState())
    val uiState: StateFlow<ReminderOverlayState> = _uiState.asStateFlow()

    /**
     * Called when the overlay is shown to the user
     * Logs the reminder shown event
     */
    fun onOverlayShown(sessionId: Long, targetApp: AppTarget) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                sessionId = sessionId,
                targetApp = targetApp,
                isLoading = false
            )

            // Log reminder shown event
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = sessionId,
                    eventType = Constants.EVENT_REMINDER_SHOWN,
                    timestamp = System.currentTimeMillis(),
                    metadata = "App: ${targetApp.displayName}"
                )
            )
        }
    }

    /**
     * Called when user clicks "Proceed" button
     * Logs the proceed event and signals completion
     */
    fun onProceedClicked() {
        val currentState = _uiState.value
        if (currentState.sessionId == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Log proceed clicked event
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = currentState.sessionId,
                    eventType = Constants.EVENT_PROCEED_CLICKED,
                    timestamp = System.currentTimeMillis(),
                    metadata = "User acknowledged reminder"
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
}

/**
 * UI state for the reminder overlay screen
 */
data class ReminderOverlayState(
    val sessionId: Long? = null,
    val targetApp: AppTarget? = null,
    val isLoading: Boolean = true,
    val shouldDismiss: Boolean = false
)
