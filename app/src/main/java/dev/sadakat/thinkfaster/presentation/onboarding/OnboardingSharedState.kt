package dev.sadakat.thinkfaster.presentation.onboarding

import android.content.Context
import dev.sadakat.thinkfaster.domain.model.InstalledAppInfo
import dev.sadakat.thinkfaster.domain.usecase.apps.GetInstalledAppsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared state for onboarding flow
 * Persists across all onboarding screens to cache data like installed apps
 * This improves UX by loading apps once on Welcome screen and reusing on Goals screen
 *
 * Scope: Singleton (shared across all onboarding screens)
 * Lifecycle: Created when first onboarding screen appears, cleared when onboarding completes
 */
class OnboardingSharedState {

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _appsLoadError = MutableStateFlow<String?>(null)
    val appsLoadError: StateFlow<String?> = _appsLoadError.asStateFlow()

    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()

    private val _selectedGoalMinutes = MutableStateFlow(60)
    val selectedGoalMinutes: StateFlow<Int> = _selectedGoalMinutes.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        @Volatile
        private var INSTANCE: OnboardingSharedState? = null

        /**
         * Get or create the singleton instance
         */
        fun getInstance(): OnboardingSharedState {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnboardingSharedState().also { INSTANCE = it }
            }
        }

        /**
         * Clear the singleton instance (call when onboarding completes or is skipped)
         */
        fun clearInstance() {
            INSTANCE?.clear()
            INSTANCE = null
        }
    }

    /**
     * Load installed apps in the background
     * Can be called on Welcome screen to preload data for Goals screen
     * @param forceReload If true, reloads even if already loaded
     */
    fun loadInstalledApps(context: Context, forceReload: Boolean = false) {
        if (!forceReload && _installedApps.value.isNotEmpty()) {
            android.util.Log.d("OnboardingSharedState", "Apps already loaded, skipping")
            return
        }

        android.util.Log.d("OnboardingSharedState", "Loading installed apps...")
        _isLoadingApps.value = true
        _appsLoadError.value = null

        scope.launch {
            try {
                val getAppsUseCase = GetInstalledAppsUseCase(context)
                val apps = getAppsUseCase()

                android.util.Log.d("OnboardingSharedState", "Loaded ${apps.size} apps")

                _installedApps.value = apps
                _selectedApps.value = getPreselectedApps(apps)
                _isLoadingApps.value = false
            } catch (e: Exception) {
                android.util.Log.e("OnboardingSharedState", "Failed to load apps", e)
                _isLoadingApps.value = false
                _appsLoadError.value = e.message ?: "Failed to load apps: ${e.javaClass.simpleName}"
            }
        }
    }

    /**
     * Toggle app selection (add/remove from selected set)
     */
    fun toggleAppSelection(packageName: String) {
        val currentSelected = _selectedApps.value
        val newSelected = if (packageName in currentSelected) {
            currentSelected - packageName
        } else {
            currentSelected + packageName
        }
        _selectedApps.value = newSelected
    }

    /**
     * Update goal slider value
     */
    fun updateGoalMinutes(minutes: Int) {
        _selectedGoalMinutes.value = minutes
    }

    /**
     * Check if at least 1 app is selected
     */
    fun hasSelectedApps(): Boolean {
        return _selectedApps.value.isNotEmpty()
    }

    /**
     * Get pre-selected apps based on popular social media apps
     */
    private fun getPreselectedApps(apps: List<InstalledAppInfo>): Set<String> {
        val popularPackages = setOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.zhiliaoapp.musically",
            "com.twitter.android"
        )

        val installedPopular = apps
            .map { it.packageName }
            .filter { it in popularPackages }

        return installedPopular.take(3).toSet()
    }

    /**
     * Clear all state (call when onboarding completes or user goes back to home)
     */
    fun clear() {
        _installedApps.value = emptyList()
        _isLoadingApps.value = false
        _appsLoadError.value = null
        _selectedApps.value = emptySet()
        _selectedGoalMinutes.value = 60
    }
}
