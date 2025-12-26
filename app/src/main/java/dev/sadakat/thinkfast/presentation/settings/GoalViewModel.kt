package dev.sadakat.thinkfast.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.GoalProgress
import dev.sadakat.thinkfast.domain.usecase.goals.GetGoalProgressUseCase
import dev.sadakat.thinkfast.domain.usecase.goals.SetGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing goals in the settings screen
 */
class GoalViewModel(
    private val setGoalUseCase: SetGoalUseCase,
    private val getGoalProgressUseCase: GetGoalProgressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    init {
        loadGoalProgress()
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
}

/**
 * UI state for goal management
 */
data class GoalUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val facebookProgress: GoalProgress? = null,
    val instagramProgress: GoalProgress? = null,
    val error: String? = null,
    val successMessage: String? = null
)
