package dev.sadakat.thinkfaster.presentation.home

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfaster.analytics.AnalyticsManager
import dev.sadakat.thinkfaster.data.preferences.OnboardingQuestPreferences
import dev.sadakat.thinkfaster.domain.model.OnboardingQuest
import dev.sadakat.thinkfaster.domain.model.QuickWinType
import dev.sadakat.thinkfaster.domain.model.StreakFreezeStatus
import dev.sadakat.thinkfaster.domain.model.StreakRecovery
import dev.sadakat.thinkfaster.domain.model.UserBaseline
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.repository.StreakRecoveryRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import dev.sadakat.thinkfaster.domain.repository.UserBaselineRepository
import dev.sadakat.thinkfaster.domain.usecase.quickwins.CheckQuickWinMilestonesUseCase
import dev.sadakat.thinkfaster.domain.usecase.quest.GetOnboardingQuestStatusUseCase
import dev.sadakat.thinkfaster.domain.usecase.streaks.ActivateStreakFreezeUseCase
import dev.sadakat.thinkfaster.domain.usecase.streaks.GetRecoveryProgressUseCase
import dev.sadakat.thinkfaster.domain.usecase.streaks.GetStreakFreezeStatusUseCase
import dev.sadakat.thinkfaster.service.UsageMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * ViewModel for Home Screen
 * Phase 1.2: Enhanced with "Today at a Glance" card showing usage, goals, and streaks
 * Broken Streak Recovery: Added freeze and recovery support
 * First-Week Retention: Added quest and baseline tracking
 */
class HomeViewModel(
    private val usageRepository: UsageRepository,
    private val goalRepository: GoalRepository,
    private val getStreakFreezeStatusUseCase: GetStreakFreezeStatusUseCase,
    private val activateStreakFreezeUseCase: ActivateStreakFreezeUseCase,
    private val getRecoveryProgressUseCase: GetRecoveryProgressUseCase,
    private val streakRecoveryRepository: StreakRecoveryRepository,
    private val checkQuickWinMilestonesUseCase: CheckQuickWinMilestonesUseCase,
    private val getOnboardingQuestStatusUseCase: GetOnboardingQuestStatusUseCase,
    private val baselineRepository: UserBaselineRepository,
    private val questPreferences: OnboardingQuestPreferences,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Load today's usage summary with goals and streaks
     * @param isRefresh true for periodic refresh, false for initial load
     */
    fun loadTodaySummary(isRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                // Only show loading spinner on initial load, not during refresh
                if (isRefresh) {
                    _uiState.value = _uiState.value.copy(isRefreshing = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }

                // Get usage for both apps
                val facebookUsageMs = usageRepository.getTodayUsageForApp("com.facebook.katana")
                val instagramUsageMs = usageRepository.getTodayUsageForApp("com.instagram.android")
                val totalUsageMs = facebookUsageMs + instagramUsageMs

                // Get goals
                val facebookGoal = goalRepository.getGoalByApp("com.facebook.katana")
                val instagramGoal = goalRepository.getGoalByApp("com.instagram.android")

                // Calculate combined goal (use max if both exist, otherwise single goal)
                val combinedGoalMinutes = when {
                    facebookGoal != null && instagramGoal != null -> {
                        // Both have goals - use combined limit
                        facebookGoal.dailyLimitMinutes + instagramGoal.dailyLimitMinutes
                    }
                    facebookGoal != null -> facebookGoal.dailyLimitMinutes
                    instagramGoal != null -> instagramGoal.dailyLimitMinutes
                    else -> null
                }

                // Get current streak (system-wide)
                val currentStreak = usageRepository.getCurrentStreak()

                // Calculate progress
                val usageMinutes = TimeUnit.MILLISECONDS.toMinutes(totalUsageMs).toInt()
                val remainingMinutes = if (combinedGoalMinutes != null) {
                    (combinedGoalMinutes - usageMinutes).coerceAtLeast(0)
                } else {
                    null
                }

                val progressPercentage = if (combinedGoalMinutes != null && combinedGoalMinutes > 0) {
                    ((usageMinutes.toFloat() / combinedGoalMinutes) * 100).toInt().coerceAtMost(100)
                } else {
                    null
                }

                val isOverLimit = combinedGoalMinutes != null && usageMinutes > combinedGoalMinutes

                // Determine celebration message
                val celebrationMessage = when {
                    combinedGoalMinutes == null -> null
                    isOverLimit -> null // No celebration if over limit
                    remainingMinutes != null && remainingMinutes > combinedGoalMinutes / 2 -> {
                        // More than 50% remaining
                        "Great start! 🌟"
                    }
                    remainingMinutes != null && remainingMinutes > 0 -> {
                        // Some remaining, but less than 50%
                        "$remainingMinutes min left today! 💪"
                    }
                    usageMinutes == combinedGoalMinutes -> {
                        // Exactly at goal
                        "Perfect! Right on target! 🎯"
                    }
                    else -> "You're doing great! 🎉"
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    totalUsageMinutes = usageMinutes,
                    goalMinutes = combinedGoalMinutes,
                    remainingMinutes = remainingMinutes,
                    progressPercentage = progressPercentage,
                    currentStreak = currentStreak,
                    isOverLimit = isOverLimit,
                    celebrationMessage = celebrationMessage,
                    hasGoalsSet = combinedGoalMinutes != null
                )

                // Update freeze button visibility based on new usage data
                updateFreezeButtonVisibility()

                // Load quest and baseline (First-Week Retention)
                loadQuestStatus()
                loadBaseline()
                checkQuickWins()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = "Failed to load summary: ${e.message}"
                )
            }
        }
    }

    /**
     * Check if the monitoring service is running
     */
    fun checkServiceStatus(context: Context) {
        viewModelScope.launch {
            val isRunning = isMonitorServiceRunning(context)
            _uiState.value = _uiState.value.copy(isServiceRunning = isRunning)
        }
    }

    /**
     * Update service running state
     */
    fun updateServiceState(isRunning: Boolean) {
        _uiState.value = _uiState.value.copy(isServiceRunning = isRunning)
    }

    /**
     * Check for streak milestones and trigger celebration if applicable
     * Phase 1.5: Quick Wins - Celebration logic
     */
    fun checkForStreakMilestone() {
        val streak = _uiState.value.currentStreak
        // Show celebration for milestones: 3, 7, 14, 30 days
        val isMilestone = streak in listOf(3, 7, 14, 30) || (streak > 30 && streak % 30 == 0)

        if (isMilestone) {
            // Track analytics
            val milestoneType = when (streak) {
                3 -> "three_day"
                7 -> "week"
                14 -> "two_week"
                30 -> "month"
                else -> "extended"
            }
            analyticsManager.trackStreakMilestone(streak, milestoneType)

            _uiState.value = _uiState.value.copy(showStreakCelebration = true)
        }
    }

    /**
     * Dismiss streak celebration
     */
    fun dismissStreakCelebration() {
        _uiState.value = _uiState.value.copy(showStreakCelebration = false)
    }

    /**
     * Load streak freeze status (Broken Streak Recovery feature)
     */
    fun loadFreezeStatus() {
        viewModelScope.launch {
            try {
                // Use first tracked app with a goal (simplified for combined tracking)
                val targetApp = "com.facebook.katana" // Default to Facebook
                val freezeStatus = getStreakFreezeStatusUseCase.invoke(targetApp)
                _uiState.value = _uiState.value.copy(freezeStatus = freezeStatus)

                // Determine if freeze button should be shown
                updateFreezeButtonVisibility()
            } catch (e: Exception) {
                // Silently fail - freeze feature is optional
            }
        }
    }

    /**
     * Load active recovery progress (Broken Streak Recovery feature)
     * Loads recovery for all tracked apps and shows the first active one
     */
    fun loadRecoveryStatus() {
        viewModelScope.launch {
            try {
                // Check both tracked apps for active recovery
                val facebookRecovery = getRecoveryProgressUseCase.invoke("com.facebook.katana")
                val instagramRecovery = getRecoveryProgressUseCase.invoke("com.instagram.android")

                // Use first non-null, non-complete recovery
                val activeRecovery = when {
                    facebookRecovery != null && !facebookRecovery.isRecoveryComplete -> facebookRecovery
                    instagramRecovery != null && !instagramRecovery.isRecoveryComplete -> instagramRecovery
                    else -> null
                }

                _uiState.value = _uiState.value.copy(activeRecovery = activeRecovery)

                // Check if we should show streak broken dialog
                if (activeRecovery != null && !activeRecovery.notificationShown) {
                    _uiState.value = _uiState.value.copy(showStreakBrokenDialog = true)
                }

                // Check if recovery was just completed (show celebration)
                val justCompleted = (facebookRecovery?.isRecoveryComplete == true &&
                                    facebookRecovery.recoveryCompletedDate == dateFormatter.format(Date())) ||
                                   (instagramRecovery?.isRecoveryComplete == true &&
                                    instagramRecovery.recoveryCompletedDate == dateFormatter.format(Date()))

                if (justCompleted) {
                    val completedRecovery = facebookRecovery ?: instagramRecovery
                    if (completedRecovery != null) {
                        _uiState.value = _uiState.value.copy(
                            showRecoveryCompleteDialog = true,
                            completedRecovery = completedRecovery
                        )
                    }
                }
            } catch (e: Exception) {
                // Silently fail - recovery feature is optional
            }
        }
    }

    /**
     * Activate streak freeze for today (Broken Streak Recovery feature)
     */
    fun activateStreakFreeze() {
        viewModelScope.launch {
            try {
                val currentDate = dateFormatter.format(Date())
                // Use first tracked app with a goal (simplified for combined tracking)
                val targetApp = "com.facebook.katana" // Default to Facebook

                val result = activateStreakFreezeUseCase.invoke(targetApp, currentDate)

                when (result) {
                    is ActivateStreakFreezeUseCase.Result.Success -> {
                        // Track analytics
                        analyticsManager.trackStreakFreezeActivated(_uiState.value.currentStreak)

                        // Reload freeze status to update UI
                        loadFreezeStatus()
                    }
                    is ActivateStreakFreezeUseCase.Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to activate freeze: ${e.message}"
                )
            }
        }
    }

    /**
     * Dismiss streak broken dialog
     */
    fun dismissStreakBrokenDialog() {
        viewModelScope.launch {
            try {
                // Mark notification as shown so dialog doesn't appear again
                _uiState.value.activeRecovery?.let { recovery ->
                    streakRecoveryRepository.markNotificationShown(recovery.targetApp)
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
        _uiState.value = _uiState.value.copy(showStreakBrokenDialog = false)
    }

    /**
     * Dismiss recovery complete dialog
     */
    fun dismissRecoveryCompleteDialog() {
        _uiState.value = _uiState.value.copy(showRecoveryCompleteDialog = false)
    }

    /**
     * Dismiss recovery progress card
     */
    fun dismissRecoveryCard() {
        _uiState.value = _uiState.value.copy(activeRecovery = null)
    }

    /**
     * Load quest status (First-Week Retention)
     */
    fun loadQuestStatus() {
        viewModelScope.launch {
            try {
                val quest = getOnboardingQuestStatusUseCase()
                _uiState.value = _uiState.value.copy(
                    questStatus = quest,
                    showQuestCard = quest.isActive && !quest.isCompleted
                )
            } catch (e: Exception) {
                // Silently fail - quest feature is optional
            }
        }
    }

    /**
     * Load baseline (First-Week Retention)
     */
    fun loadBaseline() {
        viewModelScope.launch {
            try {
                val baseline = baselineRepository.getBaseline()
                _uiState.value = _uiState.value.copy(
                    userBaseline = baseline,
                    // Only show baseline card after Day 3 and if baseline exists
                    showBaselineCard = baseline != null && _uiState.value.currentStreak >= 3
                )
            } catch (e: Exception) {
                // Silently fail - baseline feature is optional
            }
        }
    }

    /**
     * Check for quick win milestones (First-Week Retention)
     */
    fun checkQuickWins() {
        viewModelScope.launch {
            try {
                val quickWin = checkQuickWinMilestonesUseCase()
                if (quickWin != null) {
                    _uiState.value = _uiState.value.copy(quickWinToShow = quickWin)
                }
            } catch (e: Exception) {
                // Silently fail - quick wins are optional
            }
        }
    }

    /**
     * Dismiss quick win celebration
     */
    fun dismissQuickWin(type: QuickWinType) {
        when (type) {
            QuickWinType.FIRST_SESSION -> questPreferences.markFirstSessionCelebrated()
            QuickWinType.FIRST_UNDER_GOAL -> questPreferences.markFirstUnderGoalCelebrated()
            // Day 1/2 complete are handled by AchievementWorker
            else -> {}
        }
        _uiState.value = _uiState.value.copy(quickWinToShow = null)
    }

    /**
     * Dismiss quest card
     */
    fun dismissQuestCard() {
        _uiState.value = _uiState.value.copy(showQuestCard = false)
    }

    /**
     * Update freeze button visibility based on current state
     * Show when: percentageUsed >= 80 && !isOverLimit && has goals && has streak
     */
    private fun updateFreezeButtonVisibility() {
        val state = _uiState.value
        val shouldShow = state.hasGoalsSet &&
                        state.currentStreak > 0 &&
                        state.progressPercentage != null &&
                        state.progressPercentage >= 80 &&
                        !state.isOverLimit &&
                        state.freezeStatus != null

        _uiState.value = _uiState.value.copy(showFreezeButton = shouldShow)
    }

    /**
     * Show goal achieved badge when user is on track
     */
    fun updateGoalAchievedBadge() {
        val state = _uiState.value
        val shouldShowBadge = state.hasGoalsSet &&
                              !state.isOverLimit &&
                              state.progressPercentage != null &&
                              state.progressPercentage <= 100

        _uiState.value = _uiState.value.copy(showGoalAchievedBadge = shouldShowBadge)
    }

    /**
     * Check if UsageMonitorService is running
     */
    private fun isMonitorServiceRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return activityManager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == UsageMonitorService::class.java.name
        }
    }
}

/**
 * UI state for Home Screen
 */
data class HomeUiState(
    val isLoading: Boolean = true,       // Initial load only
    val isRefreshing: Boolean = false,   // Periodic refresh (prevents flicker)
    val totalUsageMinutes: Int = 0,
    val goalMinutes: Int? = null,
    val remainingMinutes: Int? = null,
    val progressPercentage: Int? = null,
    val currentStreak: Int = 0,
    val isOverLimit: Boolean = false,
    val celebrationMessage: String? = null,
    val hasGoalsSet: Boolean = false,
    val isServiceRunning: Boolean = false,
    val errorMessage: String? = null,
    // Celebration states (Phase 1.5)
    val showStreakCelebration: Boolean = false,
    val showGoalAchievedBadge: Boolean = false,
    // Broken Streak Recovery states
    val freezeStatus: StreakFreezeStatus? = null,
    val activeRecovery: StreakRecovery? = null,
    val showFreezeButton: Boolean = false,
    val showStreakBrokenDialog: Boolean = false,
    val showRecoveryCompleteDialog: Boolean = false,
    val completedRecovery: StreakRecovery? = null,
    // First-Week Retention states
    val quickWinToShow: QuickWinType? = null,
    val questStatus: OnboardingQuest? = null,
    val showQuestCard: Boolean = false,
    val userBaseline: UserBaseline? = null,
    val showBaselineCard: Boolean = false
)
