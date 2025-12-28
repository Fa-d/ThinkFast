package dev.sadakat.thinkfast.presentation.manageapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfast.data.local.CuratedApps
import dev.sadakat.thinkfast.data.repository.TrackedAppsRepositoryImpl
import dev.sadakat.thinkfast.domain.model.AppCategory
import dev.sadakat.thinkfast.domain.model.GoalProgress
import dev.sadakat.thinkfast.domain.model.InstalledAppInfo
import dev.sadakat.thinkfast.domain.model.TrackedApp
import dev.sadakat.thinkfast.domain.repository.TrackedAppsRepository
import dev.sadakat.thinkfast.domain.usecase.apps.AddTrackedAppUseCase
import dev.sadakat.thinkfast.domain.usecase.apps.GetInstalledAppsUseCase
import dev.sadakat.thinkfast.domain.usecase.apps.GetTrackedAppsWithDetailsUseCase
import dev.sadakat.thinkfast.domain.usecase.apps.RemoveTrackedAppUseCase
import dev.sadakat.thinkfast.domain.usecase.goals.GetGoalProgressUseCase
import dev.sadakat.thinkfast.domain.usecase.goals.SetGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Manage Apps screen
 * Handles app selection, tracking, goals, and limit enforcement
 */
class ManageAppsViewModel(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val addTrackedAppUseCase: AddTrackedAppUseCase,
    private val removeTrackedAppUseCase: RemoveTrackedAppUseCase,
    private val trackedAppsRepository: TrackedAppsRepository,
    private val getTrackedAppsWithDetailsUseCase: GetTrackedAppsWithDetailsUseCase,
    private val getGoalProgressUseCase: GetGoalProgressUseCase,
    private val setGoalUseCase: SetGoalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageAppsUiState())
    val uiState: StateFlow<ManageAppsUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeTrackedApps()
    }

    /**
     * Load installed apps and curated apps
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Load all installed apps
                val installedApps = getInstalledAppsUseCase()

                // Load curated apps (filter to only show installed ones)
                val curatedApps = CuratedApps.getCuratedByCategory()
                    .mapValues { (_, apps) ->
                        apps.filter { curatedApp ->
                            installedApps.any { it.packageName == curatedApp.packageName }
                        }
                    }
                    .filterValues { it.isNotEmpty() }

                _uiState.value = _uiState.value.copy(
                    installedApps = installedApps,
                    curatedApps = curatedApps,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load apps: ${e.message}"
                )
            }
        }
    }

    /**
     * Observe tracked apps for reactive UI updates
     */
    private fun observeTrackedApps() {
        viewModelScope.launch {
            trackedAppsRepository.observeTrackedApps().collect { tracked ->
                val isLimitReached = tracked.size >= TrackedAppsRepositoryImpl.MAX_TRACKED_APPS
                _uiState.value = _uiState.value.copy(
                    trackedApps = tracked,
                    isLimitReached = isLimitReached
                )
                // Load goal progress when tracked apps change
                loadGoalProgress()
            }
        }
    }

    /**
     * Load goal progress for all tracked apps
     */
    private fun loadGoalProgress() {
        viewModelScope.launch {
            try {
                val trackedApps = trackedAppsRepository.getTrackedApps()

                // Create map of packageName -> progress
                val goalProgressMap = mutableMapOf<String, GoalProgress?>()
                trackedApps.forEach { packageName ->
                    try {
                        val progress = getGoalProgressUseCase(packageName)
                        goalProgressMap[packageName] = progress
                    } catch (e: Exception) {
                        // No goal set for this app
                        goalProgressMap[packageName] = null
                    }
                }

                _uiState.value = _uiState.value.copy(
                    goalProgressMap = goalProgressMap
                )
            } catch (e: Exception) {
                // Silently fail - goals are optional
            }
        }
    }

    /**
     * Add an app to the tracked list
     */
    fun addApp(packageName: String) {
        viewModelScope.launch {
            val result = addTrackedAppUseCase(packageName)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Failed to add app"
                )
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "App added successfully"
                )
                // Clear success message after a delay
                kotlinx.coroutines.delay(2000)
                clearSuccessMessage()
            }
        }
    }

    /**
     * Remove an app from the tracked list
     */
    fun removeApp(packageName: String) {
        viewModelScope.launch {
            val result = removeTrackedAppUseCase(packageName)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Failed to remove app"
                )
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "App removed successfully"
                )
                // Clear success message after a delay
                kotlinx.coroutines.delay(2000)
                clearSuccessMessage()
            }
        }
    }

    /**
     * Set goal for an app
     */
    fun setGoal(packageName: String, dailyLimitMinutes: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingGoal = true, expandedAppForGoal = null)

            try {
                setGoalUseCase(packageName, dailyLimitMinutes)
                loadGoalProgress()

                _uiState.value = _uiState.value.copy(
                    isSavingGoal = false,
                    successMessage = "Goal updated successfully!"
                )

                // Clear success message after a delay
                kotlinx.coroutines.delay(2000)
                clearSuccessMessage()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSavingGoal = false,
                    error = e.message ?: "Failed to set goal"
                )
            }
        }
    }

    /**
     * Toggle goal expansion for an app
     */
    fun toggleGoalExpansion(packageName: String) {
        _uiState.value = _uiState.value.copy(
            expandedAppForGoal = if (_uiState.value.expandedAppForGoal == packageName) {
                null
            } else {
                packageName
            }
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    private fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

/**
 * UI state for Manage Apps screen
 */
data class ManageAppsUiState(
    val isLoading: Boolean = false,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val curatedApps: Map<AppCategory, List<TrackedApp>> = emptyMap(),
    val trackedApps: List<String> = emptyList(),
    val isLimitReached: Boolean = false,
    val goalProgressMap: Map<String, GoalProgress?> = emptyMap(),
    val expandedAppForGoal: String? = null,
    val isSavingGoal: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
