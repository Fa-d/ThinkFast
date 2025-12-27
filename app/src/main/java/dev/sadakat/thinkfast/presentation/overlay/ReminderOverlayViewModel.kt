package dev.sadakat.thinkfast.presentation.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfast.domain.intervention.ContentSelector
import dev.sadakat.thinkfast.domain.intervention.InterventionContext
import dev.sadakat.thinkfast.domain.intervention.InterventionType
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.InterventionContent
import dev.sadakat.thinkfast.domain.model.InterventionResult
import dev.sadakat.thinkfast.domain.model.InterventionType as DomainInterventionType
import dev.sadakat.thinkfast.domain.model.UsageEvent
import dev.sadakat.thinkfast.domain.model.UserChoice
import dev.sadakat.thinkfast.domain.repository.InterventionResultRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.util.Constants
import dev.sadakat.thinkfast.analytics.AnalyticsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for ReminderOverlayActivity
 * Manages the reminder overlay state and event logging
 * Phase G: Now tracks intervention effectiveness
 */
class ReminderOverlayViewModel(
    private val usageRepository: UsageRepository,
    private val resultRepository: InterventionResultRepository,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val contentSelector = ContentSelector()

    private val _uiState = MutableStateFlow(ReminderOverlayState())
    val uiState: StateFlow<ReminderOverlayState> = _uiState.asStateFlow()

    // Phase G: Track intervention shown time for measuring decision time
    private var interventionShownTime: Long = 0L

    /**
     * Called when the overlay is shown to the user
     * Generates context-aware content and logs the reminder shown event
     */
    fun onOverlayShown(sessionId: Long, targetApp: AppTarget) {
        viewModelScope.launch {
            interventionShownTime = System.currentTimeMillis()

            _uiState.value = _uiState.value.copy(
                sessionId = sessionId,
                targetApp = targetApp,
                isLoading = true
            )

            // Build context with current usage data
            val context = buildInterventionContext(targetApp, sessionId)

            // Phase G: Use effectiveness-based content selection if we have enough data
            val effectivenessData = resultRepository.getEffectivenessByContentType()
            val totalInterventions = effectivenessData.sumOf { it.total }

            val content = if (totalInterventions >= 50) {
                // Use effectiveness-weighted selection when we have sufficient data
                contentSelector.selectContentWithEffectiveness(
                    context = context,
                    interventionType = InterventionType.REMINDER,
                    effectivenessData = effectivenessData
                )
            } else {
                // Use basic selection for new users
                contentSelector.selectContent(
                    context = context,
                    interventionType = InterventionType.REMINDER
                )
            }

            _uiState.value = _uiState.value.copy(
                sessionId = sessionId,
                targetApp = targetApp,
                interventionContent = content,
                interventionContext = context,
                isLoading = false
            )

            // Log reminder shown event with content type
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = sessionId,
                    eventType = Constants.EVENT_REMINDER_SHOWN,
                    timestamp = interventionShownTime,
                    metadata = "App: ${targetApp.displayName} | Content: ${content::class.simpleName}"
                )
            )
        }
    }

    /**
     * Called when user clicks "Proceed" button
     * Phase G: Records intervention result for effectiveness tracking
     */
    fun onProceedClicked() {
        val currentState = _uiState.value
        if (currentState.sessionId == null) return

        viewModelScope.launch {
            val decisionTime = System.currentTimeMillis() - interventionShownTime

            // Phase G: Record intervention result
            recordInterventionResult(
                sessionId = currentState.sessionId!!,
                userChoice = UserChoice.PROCEED,
                decisionTimeMs = decisionTime
            )

            // Log proceed clicked event
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = currentState.sessionId,
                    eventType = Constants.EVENT_PROCEED_CLICKED,
                    timestamp = System.currentTimeMillis(),
                    metadata = "User chose to proceed | Content: ${currentState.interventionContent?.javaClass?.simpleName}"
                )
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                shouldDismiss = true
            )
        }
    }

    /**
     * Called when user clicks "Go Back" button
     * Phase G: Records successful intervention
     */
    fun onGoBackClicked() {
        val currentState = _uiState.value
        if (currentState.sessionId == null) return

        viewModelScope.launch {
            val decisionTime = System.currentTimeMillis() - interventionShownTime

            // Phase G: Record intervention result (success!)
            recordInterventionResult(
                sessionId = currentState.sessionId,
                userChoice = UserChoice.GO_BACK,
                decisionTimeMs = decisionTime
            )

            // Log go back event (successful intervention!)
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = currentState.sessionId,
                    eventType = "EVENT_GO_BACK_CLICKED",
                    timestamp = System.currentTimeMillis(),
                    metadata = "User chose to go back | Content: ${currentState.interventionContent?.javaClass?.simpleName}"
                )
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                shouldDismiss = true,
                userChoseGoBack = true
            )
        }
    }

    /**
     * Phase G: Records the intervention result for effectiveness tracking
     */
    private suspend fun recordInterventionResult(
        sessionId: Long,
        userChoice: UserChoice,
        decisionTimeMs: Long
    ) {
        val currentState = _uiState.value
        val context = currentState.interventionContext ?: return
        val content = currentState.interventionContent
        val targetApp = currentState.targetApp ?: return

        val calendar = Calendar.getInstance()

        val result = InterventionResult(
            sessionId = sessionId,
            targetApp = targetApp.packageName,
            interventionType = DomainInterventionType.REMINDER,
            contentType = content?.javaClass?.simpleName ?: "Unknown",
            hourOfDay = calendar.get(Calendar.HOUR_OF_DAY),
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            isWeekend = context.isWeekend,
            isLateNight = context.isLateNight,
            sessionCount = context.sessionCount,
            quickReopen = context.quickReopenAttempt,
            currentSessionDurationMs = context.currentSessionMinutes * 60 * 1000L,  // Convert minutes to ms
            userChoice = userChoice,
            timeToShowDecisionMs = decisionTimeMs,
            timestamp = interventionShownTime
        )

        resultRepository.recordResult(result)

        // Track to analytics (privacy-safe, aggregated only)
        analyticsManager.trackIntervention(result)
    }

    /**
     * Phase G: Call this when session ends to update the outcome
     */
    suspend fun updateSessionOutcome(sessionId: Long, finalDurationMs: Long, endedNormally: Boolean) {
        resultRepository.updateSessionOutcome(sessionId, finalDurationMs, endedNormally)
    }

    /**
     * Reset the dismiss flag after handling
     */
    fun onDismissHandled() {
        _uiState.value = _uiState.value.copy(shouldDismiss = false, userChoseGoBack = false)
    }

    /**
     * Builds intervention context with current usage data
     * Phase F: Now uses effective friction level considering user preferences
     */
    private suspend fun buildInterventionContext(
        targetApp: AppTarget,
        sessionId: Long
    ): InterventionContext {
        // Get today's usage statistics
        val todayUsage = usageRepository.getTodayUsageForApp(targetApp.packageName)
        val yesterdayUsage = usageRepository.getYesterdayUsageForApp(targetApp.packageName)
        val weeklyAverage = usageRepository.getWeeklyAverageForApp(targetApp.packageName)

        // Get session data
        val sessionCount = usageRepository.getTodaySessionCount(targetApp.packageName)
        val lastSessionEnd = usageRepository.getLastSessionEndTime(targetApp.packageName)
        val currentSessionDuration = usageRepository.getCurrentSessionDuration(sessionId)

        // Get user settings
        val goalMinutes = usageRepository.getDailyGoalForApp(targetApp.packageName)
        val streakDays = usageRepository.getCurrentStreak()
        val installDate = usageRepository.getInstallDate()
        val bestSessionMinutes = usageRepository.getBestSessionMinutes(targetApp.packageName)

        // Phase F: Get effective friction level (considers user override)
        val frictionLevel = usageRepository.getEffectiveFrictionLevel()

        return InterventionContext.create(
            targetApp = targetApp,
            currentSessionDuration = currentSessionDuration,
            sessionCount = sessionCount,
            lastSessionEndTime = lastSessionEnd,
            totalUsageToday = todayUsage,
            totalUsageYesterday = yesterdayUsage,
            weeklyAverage = weeklyAverage,
            goalMinutes = goalMinutes,
            streakDays = streakDays,
            userFrictionLevel = frictionLevel,
            installDate = installDate,
            bestSessionMinutes = bestSessionMinutes
        )
    }
}

/**
 * UI state for the reminder overlay screen
 * Phase G: Enhanced with intervention tracking
 */
data class ReminderOverlayState(
    val sessionId: Long? = null,
    val targetApp: AppTarget? = null,
    val interventionContent: InterventionContent? = null,
    val interventionContext: InterventionContext? = null,
    val isLoading: Boolean = true,
    val shouldDismiss: Boolean = false,
    val userChoseGoBack: Boolean = false
)
