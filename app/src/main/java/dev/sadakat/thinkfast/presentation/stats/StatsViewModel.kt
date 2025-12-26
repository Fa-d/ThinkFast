package dev.sadakat.thinkfast.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfast.domain.model.DailyStatistics
import dev.sadakat.thinkfast.domain.model.MonthlyStatistics
import dev.sadakat.thinkfast.domain.model.SessionBreakdown
import dev.sadakat.thinkfast.domain.model.UsageSession
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

                // Load session breakdown for each period
                val dailySessions = getSessionBreakdownUseCase(today, today).sessions
                val weeklySessionBreakdown = getSessionBreakdownUseCase(
                    startDate = weeklyStats.weekStart,
                    endDate = weeklyStats.weekEnd
                )
                val weeklySessions = weeklySessionBreakdown.sessions

                // Calculate month start and end from month string (yyyy-MM)
                val monthYear = monthlyStats.month // Format: yyyy-MM
                val monthStart = getMonthStart(monthYear)
                val monthEnd = getMonthEnd(monthYear)
                val monthlySessions = getSessionBreakdownUseCase(
                    startDate = monthStart,
                    endDate = monthEnd
                ).sessions

                // Calculate trends
                val dailyTrend = calculateTrendsUseCase.calculateDailyTrend(today)
                val weeklyTrend = calculateTrendsUseCase.calculateWeeklyTrend(today)
                val monthlyTrend = calculateTrendsUseCase.calculateMonthlyTrend(today)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dailyStats = dailyStats,
                    weeklyStats = weeklyStats,
                    monthlyStats = monthlyStats,
                    sessionBreakdown = weeklySessionBreakdown,
                    dailySessions = dailySessions,
                    weeklySessions = weeklySessions,
                    monthlySessions = monthlySessions,
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

    /**
     * Get the first day of the month in yyyy-MM-dd format
     */
    private fun getMonthStart(monthYear: String): String {
        return "${monthYear}-01"
    }

    /**
     * Get the last day of the month in yyyy-MM-dd format
     */
    private fun getMonthEnd(monthYear: String): String {
        val parts = monthYear.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()

        // Get the last day of the month
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        val lastDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        return String.format("%04d-%02d-%02d", year, month, lastDay)
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
    val dailySessions: List<UsageSession> = emptyList(),
    val weeklySessions: List<UsageSession> = emptyList(),
    val monthlySessions: List<UsageSession> = emptyList(),
    val dailyTrend: UsageTrend? = null,
    val weeklyTrend: UsageTrend? = null,
    val monthlyTrend: UsageTrend? = null,
    val selectedPeriod: StatsPeriod = StatsPeriod.DAILY,
    val error: String? = null
) {
    /**
     * Get sessions for the currently selected period
     */
    val currentPeriodSessions: List<UsageSession>
        get() = when (selectedPeriod) {
            StatsPeriod.DAILY -> dailySessions
            StatsPeriod.WEEKLY -> weeklySessions
            StatsPeriod.MONTHLY -> monthlySessions
        }
}

/**
 * Time period selection for statistics display
 */
enum class StatsPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}
