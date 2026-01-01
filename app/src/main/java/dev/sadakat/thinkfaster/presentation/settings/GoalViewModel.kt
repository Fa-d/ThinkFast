package dev.sadakat.thinkfaster.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfaster.analytics.AnalyticsManager
import dev.sadakat.thinkfaster.domain.intervention.FrictionLevel
import dev.sadakat.thinkfaster.domain.model.AppSettings
import dev.sadakat.thinkfaster.domain.model.AppTarget
import dev.sadakat.thinkfaster.domain.model.GoalProgress
import dev.sadakat.thinkfaster.domain.repository.SettingsRepository
import dev.sadakat.thinkfaster.domain.repository.TrackedAppsRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import dev.sadakat.thinkfaster.domain.usecase.apps.GetTrackedAppsWithDetailsUseCase
import dev.sadakat.thinkfaster.domain.usecase.goals.GetGoalProgressUseCase
import dev.sadakat.thinkfaster.domain.usecase.goals.SetGoalUseCase
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
    private val usageRepository: UsageRepository,
    private val trackedAppsRepository: TrackedAppsRepository,
    private val getTrackedAppsWithDetailsUseCase: GetTrackedAppsWithDetailsUseCase,
    private val goalRepository: dev.sadakat.thinkfaster.domain.repository.GoalRepository,
    private val interventionPreferences: dev.sadakat.thinkfaster.data.preferences.InterventionPreferences,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    init {
        loadGoalProgress()
        loadSettings()
        loadFrictionLevel()
        loadWorkingMode()  // Phase 2: Load working mode state
        loadSnoozeState()  // Load snooze state
        observeTrackedApps()
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

    /**
     * Observe tracked apps for reactive updates
     */
    private fun observeTrackedApps() {
        viewModelScope.launch {
            trackedAppsRepository.observeTrackedApps().collect { trackedApps ->
                _uiState.value = _uiState.value.copy(
                    trackedAppsCount = trackedApps.size
                )
                // Reload progress when tracked apps change
                loadGoalProgress()
            }
        }
    }

    fun loadGoalProgress() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load progress for all tracked apps
                val trackedApps = trackedAppsRepository.getTrackedApps()
                val allProgress = trackedApps.mapNotNull { packageName ->
                    try {
                        getGoalProgressUseCase(packageName)
                    } catch (e: Exception) {
                        null // Skip apps without goals
                    }
                }

                // For backward compatibility, keep separate FB/IG fields
                val facebookProgress = allProgress.find {
                    it.goal.targetApp == AppTarget.FACEBOOK.packageName
                }
                val instagramProgress = allProgress.find {
                    it.goal.targetApp == AppTarget.INSTAGRAM.packageName
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    facebookProgress = facebookProgress,
                    instagramProgress = instagramProgress,
                    trackedAppsProgress = allProgress
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
                // Check if goal already exists (for update vs create tracking)
                val existingGoal = goalRepository.getGoalByApp(targetApp)

                setGoalUseCase(targetApp, dailyLimitMinutes)

                // Track analytics
                val daysSinceInstall = interventionPreferences.getDaysSinceInstall()
                if (existingGoal != null) {
                    analyticsManager.trackGoalUpdated(targetApp, dailyLimitMinutes, daysSinceInstall)
                } else {
                    analyticsManager.trackGoalCreated(targetApp, dailyLimitMinutes, daysSinceInstall)
                }

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
     * Update overlay display style (full-screen vs compact)
     */
    fun setOverlayStyle(style: dev.sadakat.thinkfaster.domain.model.OverlayStyle) {
        viewModelScope.launch {
            try {
                settingsRepository.setOverlayStyle(style)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Overlay style updated"
                )

                // Clear success message after a delay
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update overlay style"
                )
            }
        }
    }

    /**
     * Push Notification Strategy: Toggle daily reminder notifications
     */
    fun setMotivationalNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setMotivationalNotificationsEnabled(enabled)
                _uiState.value = _uiState.value.copy(
                    successMessage = if (enabled) "Daily reminders enabled" else "Daily reminders disabled"
                )

                // Clear success message after a delay
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update notification settings"
                )
            }
        }
    }

    /**
     * Push Notification Strategy: Update morning notification time
     */
    fun setMorningNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setMorningNotificationTime(hour, minute)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Morning notification time updated"
                )

                // Clear success message after a delay
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update morning notification time"
                )
            }
        }
    }

    /**
     * Push Notification Strategy: Update evening notification time
     */
    fun setEveningNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setEveningNotificationTime(hour, minute)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Evening notification time updated"
                )

                // Clear success message after a delay
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update evening notification time"
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

                // Track analytics
                val levelName = level?.name ?: "AUTO"
                analyticsManager.trackSettingChanged("friction_level", levelName)

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

    /**
     * Toggle expanded state for compact goal cards
     */
    fun toggleExpanded(packageName: String) {
        _uiState.value = _uiState.value.copy(
            expandedAppId = if (_uiState.value.expandedAppId == packageName) {
                null
            } else {
                packageName
            }
        )
    }

    // ========== Phase 2: Working Mode ==========

    /**
     * Load working mode state
     */
    private fun loadWorkingMode() {
        viewModelScope.launch {
            try {
                val isEnabled = interventionPreferences.isWorkingModeEnabled()
                val remainingMinutes = interventionPreferences.getSnoozeRemainingMinutes()

                _uiState.value = _uiState.value.copy(
                    workingModeEnabled = isEnabled,
                    workingModeRemainingMinutes = remainingMinutes
                )
            } catch (e: Exception) {
                // Silently fail - not critical
            }
        }
    }

    /**
     * Toggle "I'm Working" mode (2-hour snooze)
     */
    fun setWorkingMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                interventionPreferences.setWorkingMode(enabled)

                // Track analytics
                analyticsManager.trackSettingChanged("working_mode", enabled.toString())

                // Update UI state
                val remainingMinutes = if (enabled) {
                    interventionPreferences.getSnoozeRemainingMinutes()
                } else {
                    0
                }

                _uiState.value = _uiState.value.copy(
                    workingModeEnabled = enabled,
                    workingModeRemainingMinutes = remainingMinutes,
                    successMessage = if (enabled)
                        "Working Mode enabled - 2 hours of focus time"
                    else
                        "Working Mode disabled"
                )

                // Clear success message after a delay
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update working mode"
                )
            }
        }
    }

    // ========== Snooze Settings ==========

    /**
     * Load snooze state
     */
    private fun loadSnoozeState() {
        viewModelScope.launch {
            try {
                val isActive = interventionPreferences.isSnoozed()
                val remainingMinutes = interventionPreferences.getSnoozeRemainingMinutes()

                _uiState.value = _uiState.value.copy(
                    snoozeActive = isActive,
                    snoozeRemainingMinutes = remainingMinutes
                )
            } catch (e: Exception) {
                // Silently fail - not critical
            }
        }
    }

    /**
     * Toggle snooze on/off
     */
    fun toggleSnooze(enabled: Boolean, durationMinutes: Int = 10) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    interventionPreferences.setSnoozeDuration(durationMinutes)

                    // Track analytics
                    analyticsManager.trackSettingChanged("snooze_enabled", "$durationMinutes min")

                    _uiState.value = _uiState.value.copy(
                        snoozeActive = true,
                        snoozeRemainingMinutes = durationMinutes,
                        successMessage = "Snooze enabled for $durationMinutes minutes"
                    )
                } else {
                    interventionPreferences.clearSnooze()

                    // Track analytics
                    analyticsManager.trackSettingChanged("snooze_enabled", "false")

                    _uiState.value = _uiState.value.copy(
                        snoozeActive = false,
                        snoozeRemainingMinutes = 0,
                        successMessage = "Snooze disabled - overlays will show"
                    )
                }

                // Clear success message after a delay
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update snooze"
                )
            }
        }
    }

    /**
     * Set snooze duration
     */
    fun setSnoozeDuration(durationMinutes: Int) {
        viewModelScope.launch {
            try {
                interventionPreferences.setSnoozeDuration(durationMinutes)

                // Track analytics
                analyticsManager.trackSettingChanged("snooze_duration", "$durationMinutes min")

                _uiState.value = _uiState.value.copy(
                    snoozeActive = true,
                    snoozeRemainingMinutes = durationMinutes,
                    successMessage = "Snooze set for $durationMinutes minutes"
                )

                // Clear success message after a delay
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to set snooze duration"
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
    val trackedAppsProgress: List<GoalProgress> = emptyList(),
    val trackedAppsCount: Int = 0,
    val expandedAppId: String? = null,  // For compact expandable cards
    val appSettings: AppSettings = AppSettings(),
    val frictionLevelOverride: FrictionLevel? = null,  // null = Auto mode
    val currentFrictionLevel: FrictionLevel = FrictionLevel.GENTLE,
    val workingModeEnabled: Boolean = false,  // Phase 2: Working mode toggle
    val workingModeRemainingMinutes: Int = 0,  // Phase 2: Remaining minutes
    val snoozeActive: Boolean = false,  // Snooze toggle state
    val snoozeRemainingMinutes: Int = 0,  // Remaining snooze minutes
    val error: String? = null,
    val successMessage: String? = null
)
