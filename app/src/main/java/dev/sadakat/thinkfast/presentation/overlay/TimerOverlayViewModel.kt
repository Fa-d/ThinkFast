package dev.sadakat.thinkfast.presentation.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfast.domain.intervention.ContentSelector
import dev.sadakat.thinkfast.domain.intervention.FrictionLevel
import dev.sadakat.thinkfast.domain.intervention.InterventionContext
import dev.sadakat.thinkfast.domain.intervention.InterventionType
import dev.sadakat.thinkfast.domain.model.InterventionContent
import dev.sadakat.thinkfast.domain.model.InterventionResult
import dev.sadakat.thinkfast.domain.model.InterventionType as DomainInterventionType
import dev.sadakat.thinkfast.domain.model.UsageEvent
import dev.sadakat.thinkfast.domain.model.UserChoice
import dev.sadakat.thinkfast.domain.repository.InterventionResultRepository
import dev.sadakat.thinkfast.domain.repository.SettingsRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.util.Constants
import dev.sadakat.thinkfast.analytics.AnalyticsManager
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
 */
class TimerOverlayViewModel(
    private val usageRepository: UsageRepository,
    private val resultRepository: InterventionResultRepository,
    private val analyticsManager: AnalyticsManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerOverlayState())
    val uiState: StateFlow<TimerOverlayState> = _uiState.asStateFlow()

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)

    // Content selector for dynamic interventions
    private val contentSelector = ContentSelector()

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

            // Phase G: Use effectiveness-based content selection if we have enough data
            val effectivenessData = resultRepository.getEffectivenessByContentType()
            val totalInterventions = effectivenessData.sumOf { it.total }

            val interventionContent = if (totalInterventions >= 50) {
                // Use effectiveness-weighted selection when we have sufficient data
                contentSelector.selectContentWithEffectiveness(
                    context = context,
                    interventionType = InterventionType.TIMER,
                    effectivenessData = effectivenessData
                )
            } else {
                // Use basic selection for new users
                contentSelector.selectContent(
                    context = context,
                    interventionType = InterventionType.TIMER
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
                isLoading = false
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

            // Phase G: Record intervention result
            recordInterventionResult(
                sessionId = currentState.sessionId!!,
                userChoice = UserChoice.PROCEED,
                decisionTimeMs = decisionTime
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
                sessionId = currentState.sessionId!!,
                userChoice = UserChoice.GO_BACK,
                decisionTimeMs = decisionTime
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
                    eventType = "EVENT_INTERVENTION_SUCCESS",
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
     */
    fun onCelebrationComplete() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            showCelebration = false,
            shouldDismiss = true
        )
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
    val userChoseGoBack: Boolean = false
)
