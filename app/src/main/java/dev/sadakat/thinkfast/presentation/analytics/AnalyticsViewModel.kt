package dev.sadakat.thinkfast.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfast.domain.intervention.ContentSelector
import dev.sadakat.thinkfast.domain.repository.AppInterventionStats
import dev.sadakat.thinkfast.domain.repository.InterventionResultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Analytics debug screen
 * Phase G: Effectiveness tracking
 */
class AnalyticsViewModel(
    private val resultRepository: InterventionResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val contentSelector = ContentSelector()

    /**
     * Load analytics data from the repository
     */
    fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.value = AnalyticsUiState.Loading
            try {
                val analytics = resultRepository.getOverallAnalytics()
                val contentEffectiveness = resultRepository.getEffectivenessByContentType()
                val appStats = resultRepository.getStatsByApp()

                // Phase G: Identify content types that need improvement
                val underperformingContent = contentSelector.getUnderperformingContentTypes(
                    effectivenessData = contentEffectiveness
                )

                _uiState.value = AnalyticsUiState.Success(
                    analytics = analytics,
                    contentEffectiveness = contentEffectiveness,
                    appStats = appStats,
                    underperformingContent = underperformingContent
                )
            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState.Error(
                    message = e.message ?: "Unknown error loading analytics"
                )
            }
        }
    }
}

/**
 * UI state for the analytics screen
 */
sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(
        val analytics: dev.sadakat.thinkfast.domain.model.OverallAnalytics,
        val contentEffectiveness: List<dev.sadakat.thinkfast.domain.model.ContentEffectivenessStats>,
        val appStats: Map<String, AppInterventionStats>,
        val underperformingContent: List<String> = emptyList()
    ) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}
