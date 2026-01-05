package dev.sadakat.thinkfaster.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sadakat.thinkfaster.domain.model.BehavioralInsights
import dev.sadakat.thinkfaster.domain.model.ComparativeAnalytics
import dev.sadakat.thinkfaster.domain.model.DailyStatistics
import dev.sadakat.thinkfaster.domain.model.GoalProgress
import dev.sadakat.thinkfaster.domain.model.InterventionInsights
import dev.sadakat.thinkfaster.domain.model.MonthlyStatistics
import dev.sadakat.thinkfaster.domain.model.PredictiveInsights
import dev.sadakat.thinkfaster.domain.model.SessionBreakdown
import dev.sadakat.thinkfaster.domain.model.SmartInsight
import dev.sadakat.thinkfaster.domain.model.StatsPeriod
import dev.sadakat.thinkfaster.domain.model.UsageSession
import dev.sadakat.thinkfaster.domain.model.UsageTrend
import dev.sadakat.thinkfaster.domain.model.WeeklyStatistics
import dev.sadakat.thinkfaster.domain.usecase.goals.GetGoalProgressUseCase
import dev.sadakat.thinkfaster.domain.usecase.insights.CalculateBehavioralInsightsUseCase
import dev.sadakat.thinkfaster.domain.usecase.insights.CalculateComparativeAnalyticsUseCase
import dev.sadakat.thinkfaster.domain.usecase.insights.CalculateInterventionInsightsUseCase
import dev.sadakat.thinkfaster.domain.usecase.insights.GeneratePredictiveInsightsUseCase
import dev.sadakat.thinkfaster.domain.usecase.insights.GenerateSmartInsightUseCase
import dev.sadakat.thinkfaster.domain.usecase.stats.CalculateTrendsUseCase
import dev.sadakat.thinkfaster.domain.usecase.stats.GetDailyStatisticsUseCase
import dev.sadakat.thinkfaster.domain.usecase.stats.GetMonthlyStatisticsUseCase
import dev.sadakat.thinkfaster.domain.usecase.stats.GetSessionBreakdownUseCase
import dev.sadakat.thinkfaster.domain.usecase.stats.GetWeeklyStatisticsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ViewModel for the Statistics screen
 * Manages statistics data and trends for different time periods
 * Phase 5: Enhanced with smart insights and behavioral analytics
 */
class StatsViewModel(
    private val getDailyStatisticsUseCase: GetDailyStatisticsUseCase,
    private val getWeeklyStatisticsUseCase: GetWeeklyStatisticsUseCase,
    private val getMonthlyStatisticsUseCase: GetMonthlyStatisticsUseCase,
    private val getSessionBreakdownUseCase: GetSessionBreakdownUseCase,
    private val calculateTrendsUseCase: CalculateTrendsUseCase,
    private val getGoalProgressUseCase: GetGoalProgressUseCase,
    private val generateSmartInsightUseCase: GenerateSmartInsightUseCase,
    private val calculateBehavioralInsightsUseCase: CalculateBehavioralInsightsUseCase,
    private val calculateInterventionInsightsUseCase: CalculateInterventionInsightsUseCase,
    private val generatePredictiveInsightsUseCase: GeneratePredictiveInsightsUseCase,
    private val calculateComparativeAnalyticsUseCase: CalculateComparativeAnalyticsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadStatistics()
    }

    /**
     * Load all statistics data
     * Phase 2.1: Added isRefreshing state for better loading feedback
     */
    fun loadStatistics() {
        viewModelScope.launch {
            // If already has data, show refreshing indicator instead of full loading
            val hasExistingData = _uiState.value.dailyStats != null ||
                    _uiState.value.weeklyStats != null ||
                    _uiState.value.monthlyStats != null

            _uiState.value = _uiState.value.copy(
                isLoading = !hasExistingData,
                isRefreshing = hasExistingData
            )

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

                // Load goal progress for all active goals
                val goalProgress = getGoalProgressUseCase.getAllProgress()

                // Phase 5: Load all enhanced insights
                val smartInsight = generateSmartInsightUseCase()
                val behavioralInsights = calculateBehavioralInsightsUseCase(StatsPeriod.WEEKLY)
                val interventionInsights = calculateInterventionInsightsUseCase(StatsPeriod.WEEKLY)
                val predictiveInsights = generatePredictiveInsightsUseCase()
                val comparativeAnalytics = calculateComparativeAnalyticsUseCase()

                // Build goal compliance data for calendar
                val complianceData = buildGoalComplianceData(goalProgress, monthStart, monthEnd)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
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
                    goalProgress = goalProgress,
                    smartInsight = smartInsight,
                    behavioralInsights = behavioralInsights,
                    interventionInsights = interventionInsights,
                    predictiveInsights = predictiveInsights,
                    comparativeAnalytics = comparativeAnalytics,
                    goalComplianceData = complianceData,
                    selectedPeriod = StatsPeriod.WEEKLY
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
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
     * Navigate to previous month in calendar
     * Phase 4.3: Month navigation
     */
    fun selectPreviousMonth() {
        val newOffset = _uiState.value.calendarMonthOffset - 1
        _uiState.value = _uiState.value.copy(calendarMonthOffset = newOffset)
        // TODO: Reload compliance data for the selected month
    }

    /**
     * Navigate to next month in calendar
     * Phase 4.3: Month navigation
     */
    fun selectNextMonth() {
        val newOffset = _uiState.value.calendarMonthOffset + 1
        // Don't allow future months
        if (newOffset <= 0) {
            _uiState.value = _uiState.value.copy(calendarMonthOffset = newOffset)
            // TODO: Reload compliance data for the selected month
        }
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
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        return String.format("%04d-%02d-%02d", year, month, lastDay)
    }

    /**
     * Build goal compliance data for calendar visualization
     * Maps each date to whether the goal was met (true/false)
     */
    private fun buildGoalComplianceData(
        goalProgress: List<GoalProgress>,
        monthStart: String,
        monthEnd: String
    ): Map<String, Boolean> {
        val complianceMap = mutableMapOf<String, Boolean>()

        // For now, use the first goal if available
        val mainGoal = goalProgress.firstOrNull() ?: return emptyMap()

        // Parse dates and iterate through the month
        val calendar = Calendar.getInstance()
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(monthStart)
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(monthEnd)

        if (startDate == null || endDate == null) return emptyMap()

        calendar.time = startDate

        while (calendar.time <= endDate) {
            val dateStr = dateFormatter.format(calendar.time)

            // Check if goal was met on this date
            // For now, we'll mark as true if the goal exists and it's not over limit
            // This is a simplified version - in a real implementation, you'd query daily compliance
            complianceMap[dateStr] = !mainGoal.isOverLimit

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return complianceMap
    }
}

/**
 * UI state for the Statistics screen
 * Phase 5: Enhanced with smart insights and behavioral analytics
 * Phase 2.1: Added isRefreshing for loading feedback
 * Phase 4.3: Added calendar month navigation
 */
data class StatsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val calendarMonthOffset: Int = 0,  // Phase 4.3: 0 = current, -1 = previous, etc.
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
    val goalProgress: List<GoalProgress> = emptyList(),

    // Phase 5: Smart Insights
    val smartInsight: SmartInsight? = null,
    val behavioralInsights: BehavioralInsights? = null,
    val interventionInsights: InterventionInsights? = null,
    val predictiveInsights: PredictiveInsights? = null,
    val comparativeAnalytics: ComparativeAnalytics? = null,
    val goalComplianceData: Map<String, Boolean> = emptyMap(),

    val selectedPeriod: StatsPeriod = StatsPeriod.WEEKLY,
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

    /**
     * Get overview stats for the currently selected period
     * Used by OverviewStatsCard
     */
    val overviewStats: dev.sadakat.thinkfaster.presentation.stats.components.OverviewStats?
        get() {
            val goalMinutes = goalProgress.firstOrNull()?.goal?.dailyLimitMinutes

            return when (selectedPeriod) {
                StatsPeriod.DAILY -> dailyStats?.let { stats ->
                    val totalMinutes = (stats.totalUsageMillis / (1000 * 60)).toInt()
                    val progressPercentage = if (goalMinutes != null && goalMinutes > 0) {
                        ((totalMinutes.toFloat() / goalMinutes.toFloat()) * 100).toInt()
                    } else {
                        0
                    }
                    dev.sadakat.thinkfaster.presentation.stats.components.OverviewStats(
                        totalMinutes = totalMinutes,
                        goalMinutes = goalMinutes,
                        progressPercentage = progressPercentage,
                        sessionCount = stats.sessionCount,
                        avgSessionMinutes = if (stats.sessionCount > 0) {
                            (stats.totalUsageMillis / (1000 * 60 * stats.sessionCount)).toInt()
                        } else 0
                    )
                }
                StatsPeriod.WEEKLY -> weeklyStats?.let { stats ->
                    val totalMinutes = (stats.totalUsageMillis / (1000 * 60)).toInt()
                    val avgDailyMinutes = totalMinutes / 7
                    val progressPercentage = if (goalMinutes != null && goalMinutes > 0) {
                        ((avgDailyMinutes.toFloat() / goalMinutes.toFloat()) * 100).toInt()
                    } else {
                        0
                    }
                    dev.sadakat.thinkfaster.presentation.stats.components.OverviewStats(
                        totalMinutes = totalMinutes,
                        goalMinutes = goalMinutes?.let { it * 7 }, // Weekly goal
                        progressPercentage = progressPercentage,
                        sessionCount = stats.sessionCount,
                        avgSessionMinutes = if (stats.sessionCount > 0) {
                            (stats.totalUsageMillis / (1000 * 60 * stats.sessionCount)).toInt()
                        } else 0
                    )
                }
                StatsPeriod.MONTHLY -> monthlyStats?.let { stats ->
                    val totalMinutes = (stats.totalUsageMillis / (1000 * 60)).toInt()
                    val daysInMonth = 30 // Approximate
                    val avgDailyMinutes = totalMinutes / daysInMonth
                    val progressPercentage = if (goalMinutes != null && goalMinutes > 0) {
                        ((avgDailyMinutes.toFloat() / goalMinutes.toFloat()) * 100).toInt()
                    } else {
                        0
                    }
                    dev.sadakat.thinkfaster.presentation.stats.components.OverviewStats(
                        totalMinutes = totalMinutes,
                        goalMinutes = goalMinutes?.let { it * daysInMonth }, // Monthly goal
                        progressPercentage = progressPercentage,
                        sessionCount = stats.sessionCount,
                        avgSessionMinutes = if (stats.sessionCount > 0) {
                            (stats.totalUsageMillis / (1000 * 60 * stats.sessionCount)).toInt()
                        } else 0
                    )
                }
            }
        }

    /**
     * Get streak and consistency data
     * Used by StreakConsistencyCard
     */
    val streakConsistency: dev.sadakat.thinkfaster.presentation.stats.components.StreakConsistency?
        get() {
            val mainGoal = goalProgress.firstOrNull() ?: return null

            // Count days met goal from goal compliance data
            val daysMetGoal = goalComplianceData.count { it.value }
            val totalDays = goalComplianceData.size.coerceAtLeast(1)

            return dev.sadakat.thinkfaster.presentation.stats.components.StreakConsistency(
                currentStreak = mainGoal.goal.currentStreak,
                daysMetGoal = daysMetGoal,
                totalDays = totalDays
            )
        }

    /**
     * Get app usage breakdown by package name
     * Used by AppBreakdownChart
     */
    val appBreakdown: Map<String, Int>
        get() = currentPeriodSessions
            .groupBy { session -> session.targetApp }
            .mapValues { (_, sessions) ->
                sessions.sumOf { session -> (session.duration / (1000 * 60)).toInt() }
            }
}
