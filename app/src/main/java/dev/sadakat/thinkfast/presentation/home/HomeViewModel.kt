package dev.sadakat.thinkfast.presentation.home

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfast.domain.repository.GoalRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.service.UsageMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * ViewModel for Home Screen
 * Phase 1.2: Enhanced with "Today at a Glance" card showing usage, goals, and streaks
 */
class HomeViewModel(
    private val usageRepository: UsageRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Load today's usage summary with goals and streaks
     */
    fun loadTodaySummary() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

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
                    totalUsageMinutes = usageMinutes,
                    goalMinutes = combinedGoalMinutes,
                    remainingMinutes = remainingMinutes,
                    progressPercentage = progressPercentage,
                    currentStreak = currentStreak,
                    isOverLimit = isOverLimit,
                    celebrationMessage = celebrationMessage,
                    hasGoalsSet = combinedGoalMinutes != null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
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
    val isLoading: Boolean = true,
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
    val showGoalAchievedBadge: Boolean = false
)
