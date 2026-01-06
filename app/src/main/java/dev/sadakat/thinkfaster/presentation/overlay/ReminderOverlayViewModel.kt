package dev.sadakat.thinkfaster.presentation.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfaster.domain.intervention.ContentSelector
import dev.sadakat.thinkfaster.domain.intervention.InterventionContext
import dev.sadakat.thinkfaster.domain.intervention.InterventionType
import dev.sadakat.thinkfaster.domain.model.InterventionContent
import dev.sadakat.thinkfaster.domain.model.InterventionFeedback
import dev.sadakat.thinkfaster.domain.model.InterventionResult
import dev.sadakat.thinkfaster.domain.model.InterventionType as DomainInterventionType
import dev.sadakat.thinkfaster.domain.model.UsageEvent
import dev.sadakat.thinkfaster.domain.model.UserChoice
import dev.sadakat.thinkfaster.domain.repository.InterventionResultRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import dev.sadakat.thinkfaster.util.Constants
import dev.sadakat.thinkfaster.analytics.AnalyticsManager
import dev.sadakat.thinkfaster.service.InterventionRateLimiter
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
    private val analyticsManager: AnalyticsManager,
    private val interventionPreferences: dev.sadakat.thinkfaster.data.preferences.InterventionPreferences,
    private val rateLimiter: InterventionRateLimiter,
    private val settingsRepository: dev.sadakat.thinkfaster.domain.repository.SettingsRepository
) : ViewModel() {

    private val contentSelector = ContentSelector()

    private val _uiState = MutableStateFlow(ReminderOverlayState())
    val uiState: StateFlow<ReminderOverlayState> = _uiState.asStateFlow()

    // Phase G: Track intervention shown time for measuring decision time
    private var interventionShownTime: Long = 0L

    /**
     * Check if current session is a debug session (for testing)
     * Debug sessions have sessionId == -1
     */
    private fun isDebugSession(): Boolean {
        return _uiState.value.sessionId == -1L
    }

    /**
     * Called when the overlay is shown to the user
     * Generates context-aware content and logs the reminder shown event
     */
    fun onOverlayShown(sessionId: Long, targetApp: String) {
        viewModelScope.launch {
            interventionShownTime = System.currentTimeMillis()

            _uiState.value = _uiState.value.copy(
                sessionId = sessionId,
                targetApp = targetApp,
                isLoading = true
            )

            // Build context with current usage data
            val context = buildInterventionContext(targetApp, sessionId)

            // Check for debug override first
            val debugForceType: String? = settingsRepository.getDebugForceInterventionType()

            val content = if (debugForceType != null) {
                // Debug: Use forced content type
                contentSelector.generateContentByType(debugForceType, context)
            } else {
                // Phase G: Use effectiveness-based content selection if we have enough data
                val effectivenessData = resultRepository.getEffectivenessByContentType()
                val totalInterventions = effectivenessData.sumOf { it.total }

                if (totalInterventions >= 50) {
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
            }

            _uiState.value = _uiState.value.copy(
                sessionId = sessionId,
                targetApp = targetApp,
                interventionContent = content,
                interventionContext = context,
                isLoading = false
            )

            // Log reminder shown event (skip for debug sessions)
            if (sessionId != -1L) {
                usageRepository.insertEvent(
                    UsageEvent(
                        sessionId = sessionId,
                        eventType = Constants.EVENT_REMINDER_SHOWN,
                    timestamp = interventionShownTime,
                    metadata = "App: $targetApp | Content: ${content::class.simpleName}"
                )
            )
            }
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

            // Reset consecutive snoozes - user engaged with app instead of snoozing
            interventionPreferences.resetConsecutiveSnoozes()

            // Escalate cooldown - user dismissed intervention without positive outcome
            rateLimiter.escalateCooldown()

            // Phase G: Record intervention result
            recordInterventionResult(
                sessionId = currentState.sessionId!!,
                userChoice = UserChoice.PROCEED,
                decisionTimeMs = decisionTime
            )

            // Track to Firebase Analytics
            analyticsManager.trackInterventionProceeded(
                interventionType = "reminder",
                contentType = currentState.interventionContent?.javaClass?.simpleName ?: "Unknown"
            )

            // Log proceed clicked event (skip for debug sessions)
            if (!isDebugSession()) {
                usageRepository.insertEvent(
                    UsageEvent(
                        sessionId = currentState.sessionId ?: return@launch,
                        eventType = Constants.EVENT_PROCEED_CLICKED,
                        timestamp = System.currentTimeMillis(),
                        metadata = "User chose to proceed | Content: ${currentState.interventionContent?.javaClass?.simpleName}"
                    )
                )
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                userMadeChoice = true,
                showFeedbackPrompt = true  // Phase 1: Show feedback after choice
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

            // Reset consecutive snoozes - user chose to go back (positive engagement)
            interventionPreferences.resetConsecutiveSnoozes()

            // Reset cooldown - user engaged positively with intervention
            rateLimiter.resetCooldown()

            // Phase G: Record intervention result (success!)
            recordInterventionResult(
                sessionId = currentState.sessionId,
                userChoice = UserChoice.GO_BACK,
                decisionTimeMs = decisionTime
            )

            // Track to Firebase Analytics (SUCCESS!)
            analyticsManager.trackInterventionGoBack(
                interventionType = "reminder",
                contentType = currentState.interventionContent?.javaClass?.simpleName ?: "Unknown"
            )

            // Log go back event (successful intervention!) - skip for debug sessions
            if (!isDebugSession()) {
                usageRepository.insertEvent(
                    UsageEvent(
                        sessionId = currentState.sessionId ?: return@launch,
                        eventType = Constants.EVENT_GO_BACK_CLICKED,
                        timestamp = System.currentTimeMillis(),
                        metadata = "User chose to go back | Content: ${currentState.interventionContent?.javaClass?.simpleName}"
                    )
                )
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                userChoseGoBack = true,
                userMadeChoice = true,
                showFeedbackPrompt = true  // Phase 1: Show feedback after choice
            )
        }
    }

    /**
     * Phase G: Records the intervention result for effectiveness tracking
     * Skips database operations for debug sessions (sessionId == -1)
     */
    private suspend fun recordInterventionResult(
        sessionId: Long,
        userChoice: UserChoice,
        decisionTimeMs: Long
    ) {
        // Skip database operations for debug sessions
        if (sessionId == -1L) {
            dev.sadakat.thinkfaster.util.ErrorLogger.info(
                "Skipping recordInterventionResult for debug session",
                context = "ReminderOverlayViewModel.recordInterventionResult"
            )
            return
        }

        val currentState = _uiState.value
        val context = currentState.interventionContext ?: return
        val content = currentState.interventionContent
        val targetApp = currentState.targetApp ?: return

        // Phase 2 JITAI: Get JITAI context and clear it after use
        val jitaiContext = dev.sadakat.thinkfaster.domain.intervention.JitaiContextHolder.getAndClearContext()

        val calendar = Calendar.getInstance()

        val result = InterventionResult(
            sessionId = sessionId,
            targetApp = targetApp,  // Already a package name
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
            // Phase 2 JITAI: Include persona and opportunity tracking
            userPersona = jitaiContext?.persona?.name,
            personaConfidence = jitaiContext?.personaConfidence?.name,
            opportunityScore = jitaiContext?.opportunityScore,
            opportunityLevel = jitaiContext?.opportunityLevel?.name,
            decisionSource = jitaiContext?.decisionSource,
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

    // ========== Phase 1: Feedback System Methods ==========

    /**
     * Called when user provides feedback (thumbs up or thumbs down)
     * Records feedback to database and triggers overlay dismissal
     */
    fun onFeedbackReceived(feedback: InterventionFeedback) {
        val currentState = _uiState.value
        if (currentState.sessionId == null) return

        viewModelScope.launch {
            try {
                // Skip database operations for debug sessions
                if (!isDebugSession()) {
                    resultRepository.updateFeedback(
                        sessionId = currentState.sessionId ?: return@launch,
                        feedback = feedback,
                        timestamp = System.currentTimeMillis()
                    )

                    // Log feedback event for analytics
                    usageRepository.insertEvent(
                        UsageEvent(
                            sessionId = currentState.sessionId ?: return@launch,
                            eventType = Constants.EVENT_FEEDBACK_PROVIDED,
                            timestamp = System.currentTimeMillis(),
                            metadata = "Feedback: ${feedback.name} | Content: ${currentState.interventionContent?.javaClass?.simpleName}"
                        )
                    )
                }

                // Track to Firebase Analytics
                analyticsManager.trackInterventionFeedback(
                    feedback = feedback.name,
                    interventionType = "reminder",
                    contentType = currentState.interventionContent?.javaClass?.simpleName ?: "Unknown"
                )

                // Dismiss overlay after feedback with smooth transition
                _uiState.value = _uiState.value.copy(
                    shouldDismiss = true,
                    showFeedbackPrompt = false
                )

                dev.sadakat.thinkfaster.util.ErrorLogger.info(
                    "Feedback recorded: $feedback",
                    context = "ReminderOverlayViewModel.onFeedbackReceived"
                )
            } catch (e: Exception) {
                dev.sadakat.thinkfaster.util.ErrorLogger.error(
                    e,
                    message = "Failed to save feedback",
                    context = "ReminderOverlayViewModel.onFeedbackReceived"
                )
                // Still dismiss even if save failed - don't trap user
                _uiState.value = _uiState.value.copy(
                    shouldDismiss = true,
                    showFeedbackPrompt = false
                )
            }
        }
    }

    /**
     * Called when user skips feedback prompt
     * Dismisses overlay without recording feedback
     */
    fun onSkipFeedback() {
        _uiState.value = _uiState.value.copy(
            shouldDismiss = true,
            showFeedbackPrompt = false
        )

        dev.sadakat.thinkfaster.util.ErrorLogger.info(
            "User skipped feedback",
            context = "ReminderOverlayViewModel.onSkipFeedback"
        )
    }

    // ========== Phase 2: Snooze Functionality ==========

    /**
     * Called when user clicks snooze button
     * Sets snooze with user-selected duration and dismisses overlay
     */
    fun onSnoozeClicked() {
        val currentState = _uiState.value
        if (currentState.sessionId == null) return

        viewModelScope.launch {
            try {
                // Record snooze for abuse tracking
                interventionPreferences.recordSnooze()

                // Escalate cooldown - user avoided intervention
                rateLimiter.escalateCooldown()

                // Get user's selected snooze duration
                val snoozeDurationMinutes = interventionPreferences.getSelectedSnoozeDuration()
                val snoozeDurationMs = snoozeDurationMinutes * 60 * 1000L
                val snoozeUntil = System.currentTimeMillis() + snoozeDurationMs

                interventionPreferences.setSnoozeUntil(snoozeUntil)

                // Track dismissal for working mode suggestion
                interventionPreferences.incrementDismissalCount()

                // Track analytics - intervention snoozed
                analyticsManager.trackInterventionSnoozed(snoozeDurationMinutes)

                // Dismiss overlay
                _uiState.value = _uiState.value.copy(shouldDismiss = true)

                // Log snooze event (skip for debug sessions)
                if (!isDebugSession()) {
                    usageRepository.insertEvent(
                        UsageEvent(
                            sessionId = currentState.sessionId ?: return@launch,
                            eventType = Constants.EVENT_INTERVENTION_SNOOZED,
                            timestamp = System.currentTimeMillis(),
                            metadata = "Snoozed for $snoozeDurationMinutes minutes | Content: ${currentState.interventionContent?.javaClass?.simpleName}"
                        )
                    )
                }

                dev.sadakat.thinkfaster.util.ErrorLogger.info(
                    "Intervention snoozed for $snoozeDurationMinutes minutes",
                    context = "ReminderOverlayViewModel.onSnoozeClicked"
                )
            } catch (e: Exception) {
                dev.sadakat.thinkfaster.util.ErrorLogger.error(
                    e,
                    message = "Failed to set snooze",
                    context = "ReminderOverlayViewModel.onSnoozeClicked"
                )
                // Still dismiss even if snooze failed - don't trap user
                _uiState.value = _uiState.value.copy(shouldDismiss = true)
            }
        }
    }

    /**
     * Builds intervention context with current usage data
     * Phase F: Now uses effective friction level considering user preferences
     */
    private suspend fun buildInterventionContext(
        targetApp: String,
        sessionId: Long
    ): InterventionContext {
        // Get today's usage statistics (targetApp is already a package name)
        val todayUsage = usageRepository.getTodayUsageForApp(targetApp)
        val yesterdayUsage = usageRepository.getYesterdayUsageForApp(targetApp)
        val weeklyAverage = usageRepository.getWeeklyAverageForApp(targetApp)

        // Get session data
        val sessionCount = usageRepository.getTodaySessionCount(targetApp)
        val lastSessionEnd = usageRepository.getLastSessionEndTime(targetApp)
        val currentSessionDuration = usageRepository.getCurrentSessionDuration(sessionId)

        // Get user settings
        val goalMinutes = usageRepository.getDailyGoalForApp(targetApp)
        val streakDays = usageRepository.getCurrentStreak()
        val installDate = usageRepository.getInstallDate()
        val bestSessionMinutes = usageRepository.getBestSessionMinutes(targetApp)

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
 * Phase 1: Added feedback UI state
 */
data class ReminderOverlayState(
    val sessionId: Long? = null,
    val targetApp: String? = null,  // Package name
    val interventionContent: InterventionContent? = null,
    val interventionContext: InterventionContext? = null,
    val isLoading: Boolean = true,
    val shouldDismiss: Boolean = false,
    val userChoseGoBack: Boolean = false,
    // Phase 1: Feedback UI state
    val userMadeChoice: Boolean = false,  // User clicked Go Back or Proceed
    val showFeedbackPrompt: Boolean = false  // Show feedback prompt after choice
)
