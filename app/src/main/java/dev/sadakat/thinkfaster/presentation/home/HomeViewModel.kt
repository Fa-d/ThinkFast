package dev.sadakat.thinkfaster.presentation.home

import android.app.ActivityManager
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
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
 * Per-App Goals: Added tracked apps goal management
 */
class HomeViewModel(
    private val context: Context,
    private val usageRepository: UsageRepository,
    private val goalRepository: GoalRepository,
    private val trackedAppsRepository: dev.sadakat.thinkfaster.domain.repository.TrackedAppsRepository,
    private val getStreakFreezeStatusUseCase: GetStreakFreezeStatusUseCase,
    private val activateStreakFreezeUseCase: ActivateStreakFreezeUseCase,
    private val getRecoveryProgressUseCase: GetRecoveryProgressUseCase,
    private val streakRecoveryRepository: StreakRecoveryRepository,
    private val checkQuickWinMilestonesUseCase: CheckQuickWinMilestonesUseCase,
    private val getOnboardingQuestStatusUseCase: GetOnboardingQuestStatusUseCase,
    private val baselineRepository: UserBaselineRepository,
    private val questPreferences: OnboardingQuestPreferences,
    private val analyticsManager: AnalyticsManager,
    private val getInstalledAppsUseCase: dev.sadakat.thinkfaster.domain.usecase.apps.GetInstalledAppsUseCase
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Load today's usage summary with goals and streaks
     * Calculates aggregated data across ALL tracked apps
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

                // Get all tracked apps and calculate aggregated usage
                val trackedPackages = trackedAppsRepository.getTrackedApps()

                // Calculate total usage across all tracked apps
                var totalUsageMs = 0L
                var combinedGoalMinutes = 0
                var totalSessions = 0

                trackedPackages.forEach { packageName ->
                    totalUsageMs += usageRepository.getTodayUsageForApp(packageName)
                    totalSessions += usageRepository.getTodaySessionCount(packageName)
                    val goal = goalRepository.getGoalByApp(packageName)
                    if (goal != null) {
                        combinedGoalMinutes += goal.dailyLimitMinutes
                    }
                }

                // If no tracked apps have goals set, combinedGoalMinutes remains 0 (no goal)
                val finalGoalMinutes = if (combinedGoalMinutes > 0) combinedGoalMinutes else null

                // Get current streak (system-wide)
                val currentStreak = usageRepository.getCurrentStreak()

                // Calculate progress
                val usageMinutes = TimeUnit.MILLISECONDS.toMinutes(totalUsageMs).toInt()
                val remainingMinutes = if (finalGoalMinutes != null) {
                    (finalGoalMinutes - usageMinutes).coerceAtLeast(0)
                } else {
                    null
                }

                val progressPercentage = if (finalGoalMinutes != null && finalGoalMinutes > 0) {
                    ((usageMinutes.toFloat() / finalGoalMinutes) * 100).toInt().coerceAtMost(100)
                } else {
                    null
                }

                val isOverLimit = finalGoalMinutes != null && usageMinutes > finalGoalMinutes

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    totalUsageMinutes = usageMinutes,
                    todaySessionsCount = totalSessions,
                    goalMinutes = finalGoalMinutes,
                    remainingMinutes = remainingMinutes,
                    progressPercentage = progressPercentage,
                    currentStreak = currentStreak,
                    isOverLimit = isOverLimit,
                    hasGoalsSet = finalGoalMinutes != null
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
     * Load tracked apps with their goals and progress
     * Per-App Goals feature
     */
    fun loadTrackedAppsGoals() {
        viewModelScope.launch {
            try {
                val trackedPackages = trackedAppsRepository.getTrackedApps()
                val pm = context.packageManager

                val appsWithGoals = trackedPackages.mapNotNull { packageName ->
                    try {
                        // Get app info
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        val appName = pm.getApplicationLabel(appInfo).toString()
                        val appIcon = pm.getApplicationIcon(appInfo)

                        // Get goal (may be null if no goal set yet)
                        val goal = goalRepository.getGoalByApp(packageName)

                        // Get today's usage
                        val usageMs = usageRepository.getTodayUsageForApp(packageName)
                        val usageMinutes = TimeUnit.MILLISECONDS.toMinutes(usageMs).toInt()

                        // Calculate progress
                        val remainingMinutes = goal?.dailyLimitMinutes?.let { limit ->
                            (limit - usageMinutes).coerceAtLeast(0)
                        }

                        val percentageUsed = goal?.dailyLimitMinutes?.let { limit ->
                            if (limit > 0) {
                                ((usageMinutes.toFloat() / limit) * 100).toInt()
                            } else null
                        }

                        val isOverLimit = goal?.dailyLimitMinutes?.let { limit ->
                            usageMinutes > limit
                        } ?: false

                        // Determine streak color from goal
                        val streakColor = if (goal != null) {
                            dev.sadakat.thinkfaster.ui.theme.AppColors.Streak.getColorForStreak(goal.currentStreak)
                        } else {
                            androidx.compose.ui.graphics.Color.Gray
                        }

                        PerAppGoalUiModel(
                            packageName = packageName,
                            appName = appName,
                            appIcon = appIcon,
                            goal = goal,  // Goal object as per plan specification
                            todayUsageMinutes = usageMinutes,
                            remainingMinutes = remainingMinutes,
                            percentageUsed = percentageUsed,
                            isOverLimit = isOverLimit,
                            streakColor = streakColor
                        )
                    } catch (e: Exception) {
                        // Skip apps that can't be loaded
                        null
                    }
                }

                _uiState.value = _uiState.value.copy(trackedAppsGoals = appsWithGoals)
            } catch (e: Exception) {
                // Silently fail - tracked apps feature is optional
            }
        }
    }

    /**
     * Load curated apps for the "Add Apps" bottom sheet
     */
    fun loadCuratedApps() {
        viewModelScope.launch {
            try {
                val curated = dev.sadakat.thinkfaster.data.local.CuratedApps.getCuratedByCategory()
                _uiState.value = _uiState.value.copy(curatedApps = curated)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    /**
     * Load all installed apps for the "Add Apps" bottom sheet
     */
    fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                val installed = getInstalledAppsUseCase()
                _uiState.value = _uiState.value.copy(installedApps = installed)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    /**
     * Load tracked apps list (package names only)
     */
    fun loadTrackedAppsList() {
        viewModelScope.launch {
            try {
                val tracked = trackedAppsRepository.getTrackedApps()
                _uiState.value = _uiState.value.copy(trackedApps = tracked)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    /**
     * Add an app to the tracked list
     */
    fun addTrackedApp(packageName: String) {
        viewModelScope.launch {
            try {
                trackedAppsRepository.addTrackedApp(packageName)
                // Reload both the tracked apps list and the goals
                loadTrackedAppsList()
                loadTrackedAppsGoals()
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    /**
     * Remove an app from the tracked list
     */
    fun removeTrackedApp(packageName: String) {
        viewModelScope.launch {
            try {
                trackedAppsRepository.removeTrackedApp(packageName)
                // Reload both the tracked apps list and the goals
                loadTrackedAppsList()
                loadTrackedAppsGoals()
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    /**
     * Update goal for a specific app
     */
    fun updateAppGoal(packageName: String, dailyLimitMinutes: Int) {
        viewModelScope.launch {
            try {
                // Get existing goal or create new one
                val existingGoal = goalRepository.getGoalByApp(packageName)
                val currentDate = dateFormatter.format(Date())

                val newGoal = if (existingGoal != null) {
                    // Update existing goal
                    existingGoal.copy(
                        dailyLimitMinutes = dailyLimitMinutes,
                        lastUpdated = System.currentTimeMillis()
                    )
                } else {
                    // Create new goal
                    dev.sadakat.thinkfaster.domain.model.Goal(
                        targetApp = packageName,
                        dailyLimitMinutes = dailyLimitMinutes,
                        startDate = currentDate,
                        currentStreak = 0,
                        longestStreak = 0,
                        lastUpdated = System.currentTimeMillis()
                    )
                }

                // Save goal
                goalRepository.upsertGoal(newGoal)

                // Reload tracked apps goals
                loadTrackedAppsGoals()

                // Refresh hero card with updated aggregated percentage
                loadTodaySummary(isRefresh = true)

                // Track analytics
                val daysSinceInstall = TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - usageRepository.getInstallDate()
                ).toInt()

                if (existingGoal != null) {
                    analyticsManager.trackGoalUpdated(packageName, dailyLimitMinutes, daysSinceInstall)
                } else {
                    analyticsManager.trackGoalCreated(packageName, dailyLimitMinutes, daysSinceInstall)
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update goal: ${e.message}"
                )
            }
        }
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
 * UI Model for per-app goal display
 * Matches plan specification with goal: Goal? instead of flattened fields
 */
data class PerAppGoalUiModel(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable?,
    val goal: dev.sadakat.thinkfaster.domain.model.Goal?,
    val todayUsageMinutes: Int,
    val remainingMinutes: Int?,
    val percentageUsed: Int?,
    val isOverLimit: Boolean,
    val streakColor: Color
) {
    // Convenience properties for backward compatibility during transition
    val dailyLimitMinutes: Int? get() = goal?.dailyLimitMinutes
    val currentStreak: Int get() = goal?.currentStreak ?: 0
    val bestStreak: Int get() = goal?.longestStreak ?: 0
}

/**
 * UI state for Home Screen
 */
data class HomeUiState(
    val isLoading: Boolean = true,       // Initial load only
    val isRefreshing: Boolean = false,   // Periodic refresh (prevents flicker)
    val totalUsageMinutes: Int = 0,
    val todaySessionsCount: Int = 0,
    val goalMinutes: Int? = null,
    val remainingMinutes: Int? = null,
    val progressPercentage: Int? = null,
    val currentStreak: Int = 0,
    val isOverLimit: Boolean = false,
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
    val showBaselineCard: Boolean = false,
    // Per-app goal management
    val trackedAppsGoals: List<PerAppGoalUiModel> = emptyList(),
    // Add apps functionality
    val curatedApps: Map<dev.sadakat.thinkfaster.domain.model.AppCategory, List<dev.sadakat.thinkfaster.domain.model.TrackedApp>>? = null,
    val trackedApps: List<String>? = null,
    val installedApps: List<dev.sadakat.thinkfaster.domain.model.InstalledAppInfo> = emptyList()
)
