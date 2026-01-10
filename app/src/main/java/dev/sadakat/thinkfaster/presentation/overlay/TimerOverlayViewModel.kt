package dev.sadakat.thinkfaster.presentation.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfaster.domain.intervention.ComprehensiveOutcomeTracker
import dev.sadakat.thinkfaster.domain.intervention.UnifiedContentSelector
import dev.sadakat.thinkfaster.domain.intervention.FrictionLevel
import dev.sadakat.thinkfaster.domain.intervention.InteractionDepth
import dev.sadakat.thinkfaster.domain.intervention.InterventionContext
import dev.sadakat.thinkfaster.domain.intervention.InterventionType
import dev.sadakat.thinkfaster.domain.intervention.InterventionUserChoice
import dev.sadakat.thinkfaster.domain.intervention.RLVariant
import dev.sadakat.thinkfaster.domain.intervention.UnifiedContentSelection
import dev.sadakat.thinkfaster.domain.model.InterventionContent
import dev.sadakat.thinkfaster.domain.model.InterventionFeedback
import dev.sadakat.thinkfaster.domain.model.InterventionResult
import dev.sadakat.thinkfaster.domain.model.InterventionType as DomainInterventionType
import dev.sadakat.thinkfaster.domain.model.UsageEvent
import dev.sadakat.thinkfaster.domain.model.UserChoice
import dev.sadakat.thinkfaster.domain.repository.InterventionResultRepository
import dev.sadakat.thinkfaster.domain.repository.SettingsRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import dev.sadakat.thinkfaster.util.Constants
import dev.sadakat.thinkfaster.analytics.AnalyticsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * ViewModel for TimerOverlayWindow
 * Manages the timer alert overlay state and usage statistics (duration configurable in settings)
 * Phase G: Now tracks intervention effectiveness
 * Phase 4: Uses UnifiedContentSelector for A/B testing RL vs Control
 */
class TimerOverlayViewModel(
    private val usageRepository: UsageRepository,
    private val resultRepository: InterventionResultRepository,
    private val analyticsManager: AnalyticsManager,
    private val settingsRepository: SettingsRepository,
    private val interventionPreferences: dev.sadakat.thinkfaster.data.preferences.InterventionPreferences,
    private val rateLimiter: dev.sadakat.thinkfaster.service.InterventionRateLimiter,
    private val comprehensiveOutcomeTracker: ComprehensiveOutcomeTracker,
    private val unifiedContentSelector: UnifiedContentSelector  // Phase 4: A/B testing content selector
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerOverlayState())
    val uiState: StateFlow<TimerOverlayState> = _uiState.asStateFlow()

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)

    // Phase G: Track intervention shown time for measuring decision time
    private var interventionShownTime: Long = 0L

    /**
     * Called when the overlay is shown to the user
     * Phase G: Tracks intervention effectiveness
     */
    fun onOverlayShown(
        sessionId: Long,
        targetApp: String,
        sessionStartTime: Long,
        sessionDuration: Long
    ) {
        viewModelScope.launch {
            interventionShownTime = System.currentTimeMillis()

            // Get timer alert duration from settings
            val settings = settingsRepository.getSettingsOnce()
            val timerAlertMinutes = settings.timerAlertMinutes

            // Get context-aware data (targetApp is already a package name)
            val todayUsage = usageRepository.getTodayUsageForApp(targetApp)
            val yesterdayUsage = usageRepository.getYesterdayUsageForApp(targetApp)
            val weeklyAverage = usageRepository.getWeeklyAverageForApp(targetApp)
            val sessionCount = usageRepository.getTodaySessionCount(targetApp)
            val lastSessionEnd = usageRepository.getLastSessionEndTime(targetApp)
            val goalMinutes = usageRepository.getDailyGoalForApp(targetApp)
            val streakDays = usageRepository.getCurrentStreak()
            val installDate = usageRepository.getInstallDate()
            val bestSessionMinutes = usageRepository.getBestSessionMinutes(targetApp)

            // Calculate current session duration
            val currentDuration = if (sessionDuration > 0) {
                sessionDuration
            } else {
                usageRepository.getCurrentSessionDuration(sessionId)
            }

            // Get effective friction level
            val frictionLevel = usageRepository.getEffectiveFrictionLevel()

            // Create intervention context with real data
            val context = InterventionContext.create(
                targetApp = targetApp,
                currentSessionDuration = currentDuration,
                sessionCount = sessionCount + 1,
                lastSessionEndTime = lastSessionEnd,
                totalUsageToday = todayUsage + currentDuration,
                totalUsageYesterday = yesterdayUsage,
                weeklyAverage = weeklyAverage,
                goalMinutes = goalMinutes,
                streakDays = streakDays,
                userFrictionLevel = frictionLevel,
                installDate = installDate,
                bestSessionMinutes = bestSessionMinutes
            )

            // Check for debug override first
            val debugForceType = settingsRepository.getDebugForceInterventionType()

            val interventionContent: InterventionContent
            val rlVariant: RLVariant?
            val rlContentType: String?
            val rlSelectionReason: String?

            if (debugForceType != null) {
                // Debug: Use forced content type (bypass A/B testing)
                interventionContent = dev.sadakat.thinkfaster.domain.intervention.ContentSelector().generateContentByType(debugForceType, context)
                rlVariant = null
                rlContentType = null
                rlSelectionReason = "Debug override: $debugForceType"
            } else {
                // Phase 4: Use UnifiedContentSelector for A/B testing (Control vs RL Treatment)
                val effectivenessData = resultRepository.getEffectivenessByContentType()
                val unifiedSelection: UnifiedContentSelection = unifiedContentSelector.selectContent(
                    context = context,
                    interventionType = DomainInterventionType.TIMER,
                    effectivenessData = effectivenessData
                )
                interventionContent = unifiedSelection.content
                rlVariant = unifiedSelection.variant
                rlContentType = unifiedSelection.contentType
                rlSelectionReason = unifiedSelection.selectionReason

                // Log RL variant for analytics
                dev.sadakat.thinkfaster.util.ErrorLogger.info(
                    "Timer overlay shown - Variant: ${unifiedSelection.variant}, Content: ${unifiedSelection.contentType}, Reason: ${unifiedSelection.selectionReason}",
                    context = "TimerOverlayViewModel.onOverlayShown"
                )
            }

            // Format times for display
            val startTimeFormatted = timeFormatter.format(Date(sessionStartTime))
            val currentDurationFormatted = formatDuration(currentDuration)
            val todaysTotalFormatted = formatDuration(todayUsage + currentDuration)

            _uiState.value = TimerOverlayState(
                sessionId = sessionId,
                targetApp = targetApp,
                sessionStartTime = startTimeFormatted,
                currentSessionDuration = currentDurationFormatted,
                currentSessionDurationMs = currentDuration,
                todaysTotalUsage = todaysTotalFormatted,
                todaysTotalUsageMs = todayUsage + currentDuration,
                yesterdaysTotalUsageMs = yesterdayUsage,
                goalMinutes = goalMinutes,
                timerAlertMinutes = timerAlertMinutes,
                frictionLevel = context.userFrictionLevel,
                interventionContent = interventionContent,
                interventionContext = context,
                isLoading = false,
                rlVariant = rlVariant,
                rlContentType = rlContentType,
                rlSelectionReason = rlSelectionReason
            )

            // Log timer alert shown event
            val contentType = when (interventionContent) {
                is InterventionContent.ReflectionQuestion -> "reflection"
                is InterventionContent.TimeAlternative -> "time_alternative"
                is InterventionContent.BreathingExercise -> "breathing"
                is InterventionContent.UsageStats -> "stats"
                is InterventionContent.EmotionalAppeal -> "emotional_appeal"
                is InterventionContent.Quote -> "quote"
                is InterventionContent.Gamification -> "gamification"
                is InterventionContent.ActivitySuggestion -> "activity_suggestion"
            }

            val contextInfo = buildString {
                append("Session: ${currentDuration}ms, ")
                append("App: $targetApp, ")
                append("Content: $contentType, ")
                append("IsExtendedSession: ${context.isExtendedSession}, ")
                append("QuickReopen: ${context.quickReopenAttempt}, ")
                append("IsLateNight: ${context.isLateNight}")
            }

            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = sessionId,
                    eventType = Constants.EVENT_TIMER_ALERT_SHOWN,
                    timestamp = interventionShownTime,
                    metadata = contextInfo
                )
            )
        }
    }

    /**
     * Called when user clicks "Continue Anyway" / "Proceed" button
     * Phase G: Records intervention result
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
                interventionType = "timer",
                contentType = currentState.interventionContent?.javaClass?.simpleName ?: "Unknown"
            )

            // Log timer acknowledged event
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = currentState.sessionId,
                    eventType = Constants.EVENT_TIMER_ACKNOWLEDGED,
                    timestamp = System.currentTimeMillis(),
                    metadata = "User proceeded after 10-minute alert | " +
                            "Content: ${currentState.interventionContent?.javaClass?.simpleName}"
                )
            )

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
                sessionId = currentState.sessionId!!,
                userChoice = UserChoice.GO_BACK,
                decisionTimeMs = decisionTime
            )

            // Track to Firebase Analytics (SUCCESS!)
            analyticsManager.trackInterventionGoBack(
                interventionType = "timer",
                contentType = currentState.interventionContent?.javaClass?.simpleName ?: "Unknown"
            )

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                showCelebration = true,
                userChoseGoBack = true
            )

            // Log intervention success event
            usageRepository.insertEvent(
                UsageEvent(
                    sessionId = currentState.sessionId,
                    eventType = Constants.EVENT_INTERVENTION_SUCCESS,
                    timestamp = System.currentTimeMillis(),
                    metadata = "User chose to go back after timer alert (SUCCESS!) | " +
                            "Content: ${currentState.interventionContent?.javaClass?.simpleName}"
                )
            )

            // Note: Celebration screen will call onCelebrationComplete after 1.5s
        }
    }

    /**
     * Called when celebration animation completes
     * Phase 1.1: Triggered by CelebrationScreen onComplete callback
     * Phase 1: Now shows feedback prompt after celebration
     */
    fun onCelebrationComplete() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            showCelebration = false,
            userMadeChoice = true,
            showFeedbackPrompt = true  // Phase 1: Show feedback after celebration
        )
    }

    /**
     * Phase G: Records the intervention result for effectiveness tracking
     * Phase 1: Also records proximal outcome to ComprehensiveOutcomeTracker
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

        // Phase 2 JITAI: Get JITAI context and clear it after use
        val jitaiContext = dev.sadakat.thinkfaster.domain.intervention.JitaiContextHolder.getAndClearContext()

        val calendar = Calendar.getInstance()

        val result = InterventionResult(
            sessionId = sessionId,
            targetApp = targetApp,  // Already a package name
            interventionType = DomainInterventionType.TIMER,
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

        val resultId = resultRepository.recordResult(result)

        // Phase 1: Record proximal outcome to ComprehensiveOutcomeTracker
        try {
            val interventionUserChoice = when (userChoice) {
                UserChoice.GO_BACK -> InterventionUserChoice.GO_BACK
                UserChoice.PROCEED -> InterventionUserChoice.CONTINUE
                UserChoice.DISMISSED -> InterventionUserChoice.DISMISS
            }
            val interactionDepth = calculateInteractionDepth(decisionTimeMs, content)

            comprehensiveOutcomeTracker.recordProximalOutcome(
                interventionId = resultId,
                sessionId = sessionId,
                targetApp = targetApp,
                userChoice = interventionUserChoice,
                responseTime = decisionTimeMs,
                interactionDepth = interactionDepth
            )

            dev.sadakat.thinkfaster.util.ErrorLogger.info(
                "Proximal outcome recorded: interventionId=$resultId, choice=$interventionUserChoice, depth=$interactionDepth",
                context = "TimerOverlayViewModel.recordInterventionResult"
            )
        } catch (e: Exception) {
            dev.sadakat.thinkfaster.util.ErrorLogger.error(
                e,
                message = "Failed to record proximal outcome",
                context = "TimerOverlayViewModel.recordInterventionResult"
            )
        }

        // Track to analytics (privacy-safe, aggregated only)
        analyticsManager.trackIntervention(result)
    }

    /**
     * Calculate interaction depth based on response time and content type
     * Phase 1: Helps understand user engagement levels
     */
    private fun calculateInteractionDepth(
        responseTimeMs: Long,
        content: InterventionContent?
    ): InteractionDepth {
        val responseTimeSeconds = responseTimeMs / 1000.0

        return when {
            // User dismissed immediately without reading
            responseTimeSeconds < 2.0 -> InteractionDepth.DISMISSED

            // User viewed but minimal interaction
            responseTimeSeconds < 5.0 -> InteractionDepth.VIEWED

            // Check if content type indicates deeper interaction
            responseTimeSeconds >= 5.0 && isInteractiveContent(content) -> InteractionDepth.INTERACTED

            // User read and engaged with content
            responseTimeSeconds >= 5.0 -> InteractionDepth.ENGAGED

            // Default fallback
            else -> InteractionDepth.VIEWED
        }
    }

    /**
     * Determine if content type involves user interaction beyond just reading
     */
    private fun isInteractiveContent(content: InterventionContent?): Boolean {
        return when (content) {
            is dev.sadakat.thinkfaster.domain.model.InterventionContent.BreathingExercise -> true
            is dev.sadakat.thinkfaster.domain.model.InterventionContent.UsageStats -> true
            is dev.sadakat.thinkfaster.domain.model.InterventionContent.ActivitySuggestion -> true
            else -> false
        }
    }

    /**
     * Phase 4: Record RL outcome to close the Thompson Sampling feedback loop
     * Updates the UnifiedContentSelector with the intervention outcome
     * Only records for RL_TREATMENT variant (CONTROL doesn't use Thompson Sampling)
     */
    private suspend fun recordRLOutcome(
        userChoice: String,
        feedback: String?,
        sessionContinued: Boolean?
    ) {
        val currentState = _uiState.value
        val variant = currentState.rlVariant
        val sessionId = currentState.sessionId ?: return
        val contentType = currentState.rlContentType
        val context = currentState.interventionContext ?: return

        // Skip if debug session or not RL variant
        if (sessionId == -1L || variant != RLVariant.RL_TREATMENT || contentType == null) {
            dev.sadakat.thinkfaster.util.ErrorLogger.info(
                "Skipping RL outcome recording - sessionId=$sessionId, variant=$variant, contentType=$contentType",
                context = "TimerOverlayViewModel.recordRLOutcome"
            )
            return
        }

        try {
            // Get timing info from context
            val calendar = java.util.Calendar.getInstance()
            val hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val isWeekend = context.isWeekend

            // Record outcome to UnifiedContentSelector (updates Thompson Sampling)
            unifiedContentSelector.recordOutcome(
                interventionId = sessionId,
                variant = variant,
                userChoice = userChoice,
                feedback = feedback,
                sessionContinued = sessionContinued,
                sessionDurationAfter = null,  // Will be updated later when session ends
                quickReopen = context.quickReopenAttempt,
                targetApp = currentState.targetApp,
                hourOfDay = hourOfDay,
                isWeekend = isWeekend
            )

            dev.sadakat.thinkfaster.util.ErrorLogger.info(
                "RL outcome recorded: variant=$variant, contentType=$contentType, choice=$userChoice, feedback=$feedback",
                context = "TimerOverlayViewModel.recordRLOutcome"
            )
        } catch (e: Exception) {
            dev.sadakat.thinkfaster.util.ErrorLogger.error(
                e,
                message = "Failed to record RL outcome",
                context = "TimerOverlayViewModel.recordRLOutcome"
            )
        }
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
     * Phase 4: Records RL outcome with feedback (strong signal for Thompson Sampling)
     */
    fun onFeedbackReceived(feedback: InterventionFeedback) {
        val currentState = _uiState.value
        if (currentState.sessionId == null) return

        viewModelScope.launch {
            try {
                resultRepository.updateFeedback(
                    sessionId = currentState.sessionId,
                    feedback = feedback,
                    timestamp = System.currentTimeMillis()
                )

                // Phase 4: Record RL outcome with feedback (strong signal!)
                val previousChoice = if (currentState.userChoseGoBack) "GO_BACK" else "CONTINUE"
                recordRLOutcome(
                    userChoice = previousChoice,
                    feedback = feedback.name,
                    sessionContinued = if (previousChoice == "CONTINUE") true else false
                )

                // Track to Firebase Analytics
                analyticsManager.trackInterventionFeedback(
                    feedback = feedback.name,
                    interventionType = "timer",
                    contentType = currentState.interventionContent?.javaClass?.simpleName ?: "Unknown"
                )

                // Dismiss overlay after feedback with smooth transition
                _uiState.value = _uiState.value.copy(
                    shouldDismiss = true,
                    showFeedbackPrompt = false
                )

                // Log feedback event for analytics
                usageRepository.insertEvent(
                    UsageEvent(
                        sessionId = currentState.sessionId,
                        eventType = Constants.EVENT_FEEDBACK_PROVIDED,
                        timestamp = System.currentTimeMillis(),
                        metadata = "Feedback: ${feedback.name} | Content: ${currentState.interventionContent?.javaClass?.simpleName}"
                    )
                )

                dev.sadakat.thinkfaster.util.ErrorLogger.info(
                    "Feedback recorded: $feedback",
                    context = "TimerOverlayViewModel.onFeedbackReceived"
                )
            } catch (e: Exception) {
                dev.sadakat.thinkfaster.util.ErrorLogger.error(
                    e,
                    message = "Failed to save feedback",
                    context = "TimerOverlayViewModel.onFeedbackReceived"
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
     * Phase 4: Records RL outcome with NO feedback (completes the feedback loop)
     */
    fun onSkipFeedback() {
        val currentState = _uiState.value
        if (currentState.sessionId == null) {
            _uiState.value = _uiState.value.copy(
                shouldDismiss = true,
                showFeedbackPrompt = false
            )
            return
        }

        viewModelScope.launch {
            // Phase 4: Record RL outcome with no feedback
            val previousChoice = if (currentState.userChoseGoBack) "GO_BACK" else "CONTINUE"
            recordRLOutcome(
                userChoice = previousChoice,
                feedback = null,  // No feedback provided
                sessionContinued = if (previousChoice == "CONTINUE") true else false
            )

            _uiState.value = _uiState.value.copy(
                shouldDismiss = true,
                showFeedbackPrompt = false
            )

            dev.sadakat.thinkfaster.util.ErrorLogger.info(
                "User skipped feedback - RL outcome recorded with no feedback",
                context = "TimerOverlayViewModel.onSkipFeedback"
            )
        }
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

                // Log snooze event
                usageRepository.insertEvent(
                    UsageEvent(
                        sessionId = currentState.sessionId,
                        eventType = Constants.EVENT_INTERVENTION_SNOOZED,
                        timestamp = System.currentTimeMillis(),
                        metadata = "Snoozed for $snoozeDurationMinutes minutes | Content: ${currentState.interventionContent?.javaClass?.simpleName}"
                    )
                )

                dev.sadakat.thinkfaster.util.ErrorLogger.info(
                    "Intervention snoozed for $snoozeDurationMinutes minutes",
                    context = "TimerOverlayViewModel.onSnoozeClicked"
                )
            } catch (e: Exception) {
                dev.sadakat.thinkfaster.util.ErrorLogger.error(
                    e,
                    message = "Failed to set snooze",
                    context = "TimerOverlayViewModel.onSnoozeClicked"
                )
                // Still dismiss even if snooze failed - don't trap user
                _uiState.value = _uiState.value.copy(shouldDismiss = true)
            }
        }
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
 * Phase G: Enhanced with full context-aware intervention support
 * Phase 1: Added feedback UI state
 * Phase 4: Added RL variant tracking for Thompson Sampling
 */
data class TimerOverlayState(
    val sessionId: Long? = null,
    val targetApp: String? = null,  // Package name
    val sessionStartTime: String = "",
    val currentSessionDuration: String = "",
    val todaysTotalUsage: String = "",
    val currentSessionDurationMs: Long = 0,
    val todaysTotalUsageMs: Long = 0,
    val yesterdaysTotalUsageMs: Long = 0,
    val goalMinutes: Int? = null,
    val timerAlertMinutes: Int = 10,  // Duration before showing timer alert (from settings)
    val frictionLevel: FrictionLevel = FrictionLevel.GENTLE,
    val interventionContent: InterventionContent? = null,
    val interventionContext: InterventionContext? = null,
    val isLoading: Boolean = true,
    val shouldDismiss: Boolean = false,
    val showCelebration: Boolean = false,
    val userChoseGoBack: Boolean = false,
    // Phase 1: Feedback UI state
    val userMadeChoice: Boolean = false,  // User clicked Go Back or Proceed
    val showFeedbackPrompt: Boolean = false,  // Show feedback prompt after choice
    // Phase 4: RL tracking
    val rlVariant: RLVariant? = null,  // Which variant was used (CONTROL or RL_TREATMENT)
    val rlContentType: String? = null,  // Content type selected by RL
    val rlSelectionReason: String? = null  // Why this content was selected
)
