package dev.sadakat.thinkfaster.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfaster.domain.intervention.ContentSelector
import dev.sadakat.thinkfaster.domain.repository.AppInterventionStats
import dev.sadakat.thinkfaster.domain.repository.InterventionResultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Analytics debug screen
 * Phase G: Effectiveness tracking
 * Phase 4: RL A/B testing metrics, content effectiveness & timing optimization
 */
class AnalyticsViewModel(
    private val resultRepository: InterventionResultRepository,
    private val unifiedContentSelector: dev.sadakat.thinkfaster.domain.intervention.UnifiedContentSelector? = null  // Phase 4: Unified content selector (wraps all RL features)
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

                // Phase 4: Load RL metrics from UnifiedContentSelector
                val rlMetrics = unifiedContentSelector?.getRolloutMetrics()
                val rlContentEffectiveness = unifiedContentSelector?.getContentEffectiveness()

                _uiState.value = AnalyticsUiState.Success(
                    analytics = analytics,
                    contentEffectiveness = contentEffectiveness,
                    appStats = appStats,
                    underperformingContent = underperformingContent,
                    rlMetrics = rlMetrics,  // Phase 4: RL A/B testing metrics
                    rlContentEffectiveness = rlContentEffectiveness  // Phase 4: RL content type effectiveness
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
        val analytics: dev.sadakat.thinkfaster.domain.model.OverallAnalytics,
        val contentEffectiveness: List<dev.sadakat.thinkfaster.domain.model.ContentEffectivenessStats>,
        val appStats: Map<String, AppInterventionStats>,
        val underperformingContent: List<String> = emptyList(),
        val rlMetrics: dev.sadakat.thinkfaster.domain.intervention.RLEffectivenessMetrics? = null,  // Phase 4: RL A/B testing metrics
        val rlContentEffectiveness: List<dev.sadakat.thinkfaster.domain.intervention.ContentEffectiveness>? = null,  // Phase 4: RL content type effectiveness (Thompson Sampling)
        val timingEffectiveness: Map<Int, Float>? = null,  // Phase 4: Hour -> effectiveness score (0.0-1.0)
        val hasReliableTimingData: Boolean = false  // Phase 4: Whether timing data is reliable enough
    ) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}
