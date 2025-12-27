package dev.sadakat.thinkfast.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfast.domain.intervention.FrictionLevel
import dev.sadakat.thinkfast.domain.model.AppSettings
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.GoalProgress
import dev.sadakat.thinkfast.domain.repository.SettingsRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.domain.usecase.goals.GetGoalProgressUseCase
import dev.sadakat.thinkfast.domain.usecase.goals.SetGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing goals and app settings in the settings screen
 */
class GoalViewModel(
    private val setGoalUseCase: SetGoalUseCase,
    private val getGoalProgressUseCase: GetGoalProgressUseCase,
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    init {
        loadGoalProgress()
        loadSettings()
        loadFrictionLevel()
    }

    /**
     * Load app settings
     */
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(appSettings = settings)
            }
        }
    }

    fun loadGoalProgress() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load progress for all supported apps
                val facebookProgress = getGoalProgressUseCase(AppTarget.FACEBOOK.packageName)
                val instagramProgress = getGoalProgressUseCase(AppTarget.INSTAGRAM.packageName)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    facebookProgress = facebookProgress,
                    instagramProgress = instagramProgress
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load goal progress"
                )
            }
        }
    }

    fun setGoal(targetApp: String, dailyLimitMinutes: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            try {
                setGoalUseCase(targetApp, dailyLimitMinutes)

                // Reload progress after setting goal
                loadGoalProgress()

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    successMessage = "Goal set successfully!"
                )

                // Clear success message after a delay
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } catch (e: IllegalArgumentException) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Invalid goal parameters"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to set goal"
                )
            }
        }
    }

    fun setFacebookGoal(dailyLimitMinutes: Int) {
        setGoal(AppTarget.FACEBOOK.packageName, dailyLimitMinutes)
    }

    fun setInstagramGoal(dailyLimitMinutes: Int) {
        setGoal(AppTarget.INSTAGRAM.packageName, dailyLimitMinutes)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        loadGoalProgress()
    }

    /**
     * Update timer alert duration
     */
    fun setTimerAlertDuration(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setTimerAlertMinutes(minutes)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Timer alert set to ${minutes} minutes"
                )

                // Clear success message after a delay
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update timer duration"
                )
            }
        }
    }

    /**
     * Toggle always show reminder setting
     */
    fun setAlwaysShowReminder(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setAlwaysShowReminder(enabled)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update reminder setting"
                )
            }
        }
    }

    /**
     * Phase F: Toggle locked mode (maximum friction)
     */
    fun setLockedMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setLockedMode(enabled)
                _uiState.value = _uiState.value.copy(
                    successMessage = if (enabled) "Locked Mode enabled - Maximum friction active" else "Locked Mode disabled"
                )

                // Clear success message after a delay
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update locked mode"
                )
            }
        }
    }

    /**
     * Load current friction level and override
     */
    private fun loadFrictionLevel() {
        viewModelScope.launch {
            try {
                val override = usageRepository.getFrictionLevelOverride()
                val effective = usageRepository.getEffectiveFrictionLevel()
                _uiState.value = _uiState.value.copy(
                    frictionLevelOverride = override,
                    currentFrictionLevel = effective
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load friction level"
                )
            }
        }
    }

    /**
     * Set friction level override
     * Pass null for automatic calculation based on tenure
     */
    fun setFrictionLevel(level: FrictionLevel?) {
        viewModelScope.launch {
            try {
                usageRepository.setFrictionLevelOverride(level)

                // Reload to get updated effective level
                loadFrictionLevel()

                val message = if (level == null) {
                    "Friction level set to Auto"
                } else {
                    "Friction level set to ${level.displayName}"
                }

                _uiState.value = _uiState.value.copy(successMessage = message)

                // Clear success message after a delay
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update friction level"
                )
            }
        }
    }
}

/**
 * UI state for goal management and app settings
 */
data class GoalUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val facebookProgress: GoalProgress? = null,
    val instagramProgress: GoalProgress? = null,
    val appSettings: AppSettings = AppSettings(),
    val frictionLevelOverride: FrictionLevel? = null,  // null = Auto mode
    val currentFrictionLevel: FrictionLevel = FrictionLevel.GENTLE,
    val error: String? = null,
    val successMessage: String? = null
)
