package dev.sadakat.thinkfast.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfast.domain.model.DailyStatistics
import dev.sadakat.thinkfast.domain.model.MonthlyStatistics
import dev.sadakat.thinkfast.domain.model.SessionBreakdown
import dev.sadakat.thinkfast.domain.model.UsageTrend
import dev.sadakat.thinkfast.domain.model.WeeklyStatistics
import dev.sadakat.thinkfast.domain.usecase.stats.CalculateTrendsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetDailyStatisticsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetMonthlyStatisticsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetSessionBreakdownUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetWeeklyStatisticsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for the Statistics screen
 * Manages statistics data and trends for different time periods
 */
class StatsViewModel(
    private val getDailyStatisticsUseCase: GetDailyStatisticsUseCase,
    private val getWeeklyStatisticsUseCase: GetWeeklyStatisticsUseCase,
    private val getMonthlyStatisticsUseCase: GetMonthlyStatisticsUseCase,
    private val getSessionBreakdownUseCase: GetSessionBreakdownUseCase,
    private val calculateTrendsUseCase: CalculateTrendsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadStatistics()
    }

    /**
     * Load all statistics data
     */
    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val today = dateFormatter.format(Date())

                // Load statistics for different periods
                val dailyStats = getDailyStatisticsUseCase(today)
                val weeklyStats = getWeeklyStatisticsUseCase(today)
                val monthlyStats = getMonthlyStatisticsUseCase(today)

                // Load session breakdown for current week
                val sessionBreakdown = getSessionBreakdownUseCase(
                    startDate = weeklyStats.weekStart,
                    endDate = weeklyStats.weekEnd
                )

                // Calculate trends
                val dailyTrend = calculateTrendsUseCase.calculateDailyTrend(today)
                val weeklyTrend = calculateTrendsUseCase.calculateWeeklyTrend(today)
                val monthlyTrend = calculateTrendsUseCase.calculateMonthlyTrend(today)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dailyStats = dailyStats,
                    weeklyStats = weeklyStats,
                    monthlyStats = monthlyStats,
                    sessionBreakdown = sessionBreakdown,
                    dailyTrend = dailyTrend,
                    weeklyTrend = weeklyTrend,
                    monthlyTrend = monthlyTrend,
                    selectedPeriod = StatsPeriod.DAILY
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load statistics"
                )
            }
        }
    }

    /**
     * Change the selected time period
     */
    fun selectPeriod(period: StatsPeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for the Statistics screen
 */
data class StatsUiState(
    val isLoading: Boolean = true,
    val dailyStats: DailyStatistics? = null,
    val weeklyStats: WeeklyStatistics? = null,
    val monthlyStats: MonthlyStatistics? = null,
    val sessionBreakdown: SessionBreakdown? = null,
    val dailyTrend: UsageTrend? = null,
    val weeklyTrend: UsageTrend? = null,
    val monthlyTrend: UsageTrend? = null,
    val selectedPeriod: StatsPeriod = StatsPeriod.DAILY,
    val error: String? = null
)

/**
 * Time period selection for statistics display
 */
enum class StatsPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}
