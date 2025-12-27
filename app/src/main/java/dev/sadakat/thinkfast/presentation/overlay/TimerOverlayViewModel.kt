package dev.sadakat.thinkfast.presentation.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfast.domain.intervention.ContentSelector
import dev.sadakat.thinkfast.domain.intervention.FrictionLevel
import dev.sadakat.thinkfast.domain.intervention.InterventionContext
import dev.sadakat.thinkfast.domain.intervention.InterventionType
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.InterventionContent
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
 * Now with dynamic content selection from Phase A
 */
class TimerOverlayViewModel(
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerOverlayState())
    val uiState: StateFlow<TimerOverlayState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)

    // Content selector for dynamic interventions
    private val contentSelector = ContentSelector()

    /**
     * Called when the overlay is shown to the user
     * Loads session statistics, selects intervention content, and logs the alert event
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

            // Get yesterday's usage for comparison
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val yesterdayDate = dateFormatter.format(calendar.time)
            val yesterdaysSessions = usageRepository.getSessionsByDate(yesterdayDate)
            val yesterdaysUsageForApp = yesterdaysSessions
                .filter { it.targetApp == targetApp.packageName }
                .sumOf { it.duration }

            // Calculate session count today
            val sessionCountToday = todaysSessions.count { it.targetApp == targetApp.packageName }

            // Get last session end time
            val lastSessionEndTime = todaysSessions
                .filter { it.targetApp == targetApp.packageName }
                .maxOfOrNull { it.startTimestamp + it.duration } ?: 0L

            // Placeholder values (TODO: Get from actual user data storage)
            val goalMinutes: Int? = null  // TODO: Get from settings/goals
            val streakDays = 0  // TODO: Calculate from usage history
            val installDate = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)  // TODO: Get from SharedPreferences
            val bestSessionMinutes = 5  // TODO: Calculate from session history

            // Create intervention context
            val context = InterventionContext.create(
                targetApp = targetApp,
                currentSessionDuration = sessionDuration,
                sessionCount = sessionCountToday + 1,  // +1 for current session
                lastSessionEndTime = lastSessionEndTime,
                totalUsageToday = todaysUsageForApp + sessionDuration,
                totalUsageYesterday = yesterdaysUsageForApp,
                weeklyAverage = todaysUsageForApp,  // TODO: Calculate actual weekly average
                goalMinutes = goalMinutes,
                streakDays = streakDays,
                userFrictionLevel = FrictionLevel.fromDaysSinceInstall(
                    ((System.currentTimeMillis() - installDate) / (1000 * 60 * 60 * 24)).toInt()
                ),
                installDate = installDate,
                bestSessionMinutes = bestSessionMinutes
            )

            // Select intervention content dynamically
            val interventionContent = contentSelector.selectContent(
                context = context,
                interventionType = InterventionType.TIMER
            )

            // Format times
            val startTimeFormatted = timeFormatter.format(Date(sessionStartTime))
            val currentDurationFormatted = formatDuration(sessionDuration)
            val todaysTotalFormatted = formatDuration(todaysUsageForApp + sessionDuration)

            _uiState.value = TimerOverlayState(
                sessionId = sessionId,
                targetApp = targetApp,
                sessionStartTime = startTimeFormatted,
                currentSessionDuration = currentDurationFormatted,
                currentSessionDurationMs = sessionDuration,
                todaysTotalUsage = todaysTotalFormatted,
                todaysTotalUsageMs = todaysUsageForApp + sessionDuration,
                yesterdaysTotalUsageMs = yesterdaysUsageForApp,
                goalMinutes = goalMinutes,
                frictionLevel = context.userFrictionLevel,
                interventionContent = interventionContent,
                isLoading = false
            )

            // Log timer alert shown event with content type
            val contentType = when (interventionContent) {
                is InterventionContent.ReflectionQuestion -> "reflection"
                is InterventionContent.TimeAlternative -> "time_alternative"
                is InterventionContent.BreathingExercise -> "breathing"
                is InterventionContent.UsageStats -> "stats"
                is InterventionContent.EmotionalAppeal -> "emotional_appeal"
                is InterventionContent.Quote -> "quote"
                is InterventionContent.Gamification -> "gamification"
            }

            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = sessionId,
                    eventType = Constants.EVENT_TIMER_ALERT_SHOWN,
                    timestamp = System.currentTimeMillis(),
                    metadata = "Session: ${sessionDuration}ms, App: ${targetApp.displayName}, Content: $contentType"
                )
            )
        }
    }

    /**
     * Called when user clicks "I Understand" / "Proceed" button
     * Logs the acknowledgment event and signals completion
     */
    fun onProceedClicked() {
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
                    metadata = "User proceeded after 10-minute alert (chose to continue)"
                )
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                shouldDismiss = true
            )
        }
    }

    /**
     * Called when user clicks "Go Back" button (chooses NOT to proceed)
     * This is a positive action - user heeded the intervention
     */
    fun onGoBackClicked() {
        val currentState = _uiState.value
        if (currentState.sessionId == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                showCelebration = true  // Celebrate the healthy choice!
            )

            // Log intervention success event
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = currentState.sessionId,
                    eventType = "intervention_successful",
                    timestamp = System.currentTimeMillis(),
                    metadata = "User chose to go back after intervention (success!)"
                )
            )

            // Brief delay to show celebration, then dismiss
            kotlinx.coroutines.delay(1500)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showCelebration = false,
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
 * Enhanced with dynamic intervention content
 */
data class TimerOverlayState(
    val sessionId: Long? = null,
    val targetApp: AppTarget? = null,

    // Formatted display strings
    val sessionStartTime: String = "",
    val currentSessionDuration: String = "",
    val todaysTotalUsage: String = "",

    // Raw values for intervention logic
    val currentSessionDurationMs: Long = 0,
    val todaysTotalUsageMs: Long = 0,
    val yesterdaysTotalUsageMs: Long = 0,
    val goalMinutes: Int? = null,

    // Intervention system
    val frictionLevel: FrictionLevel = FrictionLevel.GENTLE,
    val interventionContent: InterventionContent? = null,

    // UI flags
    val isLoading: Boolean = true,
    val shouldDismiss: Boolean = false,
    val showCelebration: Boolean = false
)
