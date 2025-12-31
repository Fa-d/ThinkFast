package dev.sadakat.thinkfaster.presentation.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfaster.analytics.AnalyticsManager
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.domain.model.Goal
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

/**
 * ViewModel for onboarding flow
 * Manages 3-screen onboarding state and completion tracking
 */
class OnboardingViewModel(
    private val goalRepository: GoalRepository,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingState())
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()

    companion object {
        private const val PREFS_NAME = "think_fast_onboarding"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val DEFAULT_GOAL_MINUTES = 60 // Research-based: 60 min/day for social media
    }

    /**
     * Check if onboarding has been completed
     */
    fun isOnboardingCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    /**
     * Initialize onboarding and track analytics
     */
    fun init(context: Context) {
        if (!isOnboardingCompleted(context)) {
            analyticsManager.trackOnboardingStarted()
        }
    }

    /**
     * Move to next page in onboarding
     */
    fun nextPage() {
        val currentPage = _uiState.value.currentPage
        if (currentPage < 2) { // 0, 1, 2 = 3 pages
            analyticsManager.trackOnboardingStepCompleted(currentPage + 1)
            _uiState.value = _uiState.value.copy(currentPage = currentPage + 1)
        }
    }

    /**
     * Move to previous page in onboarding
     */
    fun previousPage() {
        val currentPage = _uiState.value.currentPage
        if (currentPage > 0) {
            _uiState.value = _uiState.value.copy(currentPage = currentPage - 1)
        }
    }

    /**
     * Skip onboarding (mark as completed without setting goals)
     */
    fun skipOnboarding(context: Context) {
        // Track analytics
        val interventionPrefs = InterventionPreferences(context)
        val daysSinceInstall = interventionPrefs.getDaysSinceInstall()
        analyticsManager.trackOnboardingSkipped(daysSinceInstall)

        markOnboardingCompleted(context)
    }

    /**
     * Complete onboarding with goal setup
     */
    fun completeOnboarding(context: Context, goalMinutes: Int = DEFAULT_GOAL_MINUTES) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val today = dateFormat.format(Date())
            val now = System.currentTimeMillis()

            // Set default goals for Facebook and Instagram
            val facebookGoal = Goal(
                targetApp = "com.facebook.katana",
                dailyLimitMinutes = goalMinutes,
                startDate = today,
                currentStreak = 0,
                longestStreak = 0,
                lastUpdated = now
            )

            val instagramGoal = Goal(
                targetApp = "com.instagram.android",
                dailyLimitMinutes = goalMinutes,
                startDate = today,
                currentStreak = 0,
                longestStreak = 0,
                lastUpdated = now
            )

            goalRepository.upsertGoal(facebookGoal)
            goalRepository.upsertGoal(instagramGoal)

            // Track analytics
            val interventionPrefs = InterventionPreferences(context)
            val daysSinceInstall = interventionPrefs.getDaysSinceInstall()
            analyticsManager.trackOnboardingCompleted(daysSinceInstall)

            // Mark onboarding as completed
            markOnboardingCompleted(context)

            _uiState.value = _uiState.value.copy(isCompleted = true)
        }
    }

    /**
     * Update goal slider value
     */
    fun updateGoalMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(selectedGoalMinutes = minutes)
    }

    /**
     * Save goal selection (Step 2 of new onboarding flow)
     * Stores selected goal minutes without creating Goal entities yet
     * Goal entities are created during completeOnboarding()
     */
    fun saveGoalSelection(minutes: Int) {
        _uiState.value = _uiState.value.copy(selectedGoalMinutes = minutes)
        // Track analytics for goal selection step
        analyticsManager.trackOnboardingStepCompleted(2)
    }

    /**
     * Mark onboarding as completed in SharedPreferences
     */
    private fun markOnboardingCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, true) }
    }

    /**
     * Generate sample usage data for preview charts
     * Shows what the app will look like after a week of use
     */
    fun generateSampleData(): List<SampleUsageDay> {
        return listOf(
            SampleUsageDay(dayLabel = "Mon", facebookMinutes = 45, instagramMinutes = 38),
            SampleUsageDay(dayLabel = "Tue", facebookMinutes = 52, instagramMinutes = 41),
            SampleUsageDay(dayLabel = "Wed", facebookMinutes = 38, instagramMinutes = 29),
            SampleUsageDay(dayLabel = "Thu", facebookMinutes = 61, instagramMinutes = 47),
            SampleUsageDay(dayLabel = "Fri", facebookMinutes = 48, instagramMinutes = 35),
            SampleUsageDay(dayLabel = "Sat", facebookMinutes = 73, instagramMinutes = 58),
            SampleUsageDay(dayLabel = "Sun", facebookMinutes = 41, instagramMinutes = 32)
        )
    }
}

/**
 * UI state for onboarding screens
 * Used by both old 3-page flow and new 6-step flow
 */
data class OnboardingState(
    val currentPage: Int = 0, // For old flow: 0 = Welcome, 1 = Permissions, 2 = Goals
    val selectedGoalMinutes: Int = 60, // Default: 60 minutes/day (user-selectable: 15-180)
    val isCompleted: Boolean = false // Set to true when onboarding is fully complete
)

/**
 * Sample usage data for preview charts
 */
data class SampleUsageDay(
    val dayLabel: String,
    val facebookMinutes: Int,
    val instagramMinutes: Int
) {
    val totalMinutes: Int
        get() = facebookMinutes + instagramMinutes
}
