package dev.sadakat.thinkfaster.domain.usecase.insights

import dev.sadakat.thinkfaster.domain.model.ContentTypeEffectiveness
import dev.sadakat.thinkfaster.domain.model.InterventionInsights
import dev.sadakat.thinkfaster.domain.model.StatsPeriod
import dev.sadakat.thinkfaster.domain.model.TimeWindow
import dev.sadakat.thinkfaster.domain.repository.InterventionResultRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Use case for calculating intervention effectiveness insights
 * Phase 3: Understanding what works and when
 */
class CalculateInterventionInsightsUseCase(
    private val interventionResultRepository: InterventionResultRepository
) {
    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    /**
     * Calculate intervention effectiveness insights for a given period
     * @param period The time period to analyze (DAILY, WEEKLY, MONTHLY)
     * @param endDate The end date (defaults to today)
     */
    suspend operator fun invoke(
        period: StatsPeriod,
        endDate: String = DATE_FORMAT.format(Calendar.getInstance().time)
    ): InterventionInsights? {
        val calendar = Calendar.getInstance()
        calendar.time = DATE_FORMAT.parse(endDate) ?: return null

        // Calculate start date and timestamps based on period
        val endOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val startDate = when (period) {
            StatsPeriod.DAILY -> endDate
            StatsPeriod.WEEKLY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                DATE_FORMAT.format(calendar.time)
            }
            StatsPeriod.MONTHLY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                DATE_FORMAT.format(calendar.time)
            }
        }

        calendar.time = DATE_FORMAT.parse(startDate)!!
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfPeriod = calendar.timeInMillis

        // Get all results in the range
        val allResults = interventionResultRepository.getResultsInRange(
            startTimestamp = startOfPeriod,
            endTimestamp = endOfDay
        )

        if (allResults.isEmpty()) {
            return null // Not enough data
        }

        // Calculate overall metrics
        val totalInterventions = allResults.size
        val successCount = allResults.count { it.userChoice.name == "GO_BACK" }
        val proceedCount = allResults.count { it.userChoice.name == "PROCEED" }
        val overallSuccessRate = (successCount.toFloat() / totalInterventions) * 100

        // Get content type effectiveness
        val contentTypeStats = interventionResultRepository.getEffectivenessByContext(
            startTimestamp = startOfPeriod,
            endTimestamp = endOfDay,
            contextFilter = "ALL"
        )

        val contentTypeEffectiveness = contentTypeStats.map { stats ->
            ContentTypeEffectiveness(
                contentType = stats.contentType,
                displayName = formatContentTypeName(stats.contentType),
                totalShown = stats.total,
                successCount = stats.goBackCount,
                successRate = stats.successRate.toFloat()
            )
        }.sortedByDescending { it.successRate }

        val mostEffectiveType = contentTypeEffectiveness.firstOrNull()
        val leastEffectiveType = contentTypeEffectiveness.lastOrNull()

        // Get time-based effectiveness
        val timeWindowStats = interventionResultRepository.getEffectivenessByTimeWindow(
            startTimestamp = startOfPeriod,
            endTimestamp = endOfDay
        )

        val timeBasedEffectiveness = timeWindowStats.associate { stats ->
            stats.timeWindow to stats.successRate.toFloat()
        }

        val bestTimeForSuccess = timeWindowStats
            .filter { it.total >= 3 } // Require at least 3 interventions for statistical significance
            .maxByOrNull { it.successRate }
            ?.let { stats ->
                TimeWindow(
                    label = stats.timeWindow,
                    successRate = stats.successRate.toFloat(),
                    totalInterventions = stats.total
                )
            }

        // Get context-based effectiveness
        val lateNightStats = interventionResultRepository.getEffectivenessByContext(
            startTimestamp = startOfPeriod,
            endTimestamp = endOfDay,
            contextFilter = "LATE_NIGHT"
        )
        val weekendStats = interventionResultRepository.getEffectivenessByContext(
            startTimestamp = startOfPeriod,
            endTimestamp = endOfDay,
            contextFilter = "WEEKEND"
        )
        val quickReopenStats = interventionResultRepository.getEffectivenessByContext(
            startTimestamp = startOfPeriod,
            endTimestamp = endOfDay,
            contextFilter = "QUICK_REOPEN"
        )

        val contextBasedEffectiveness = mutableMapOf<String, Float>()

        lateNightStats.firstOrNull()?.let { stats ->
            if (stats.total > 0) {
                contextBasedEffectiveness["Late Night"] = stats.successRate.toFloat()
            }
        }

        weekendStats.firstOrNull()?.let { stats ->
            if (stats.total > 0) {
                contextBasedEffectiveness["Weekend"] = stats.successRate.toFloat()
            }
        }

        quickReopenStats.firstOrNull()?.let { stats ->
            if (stats.total > 0) {
                contextBasedEffectiveness["Quick Reopen"] = stats.successRate.toFloat()
            }
        }

        // Calculate average decision time
        val avgDecisionTimeMs = allResults
            .mapNotNull { it.timeToShowDecisionMs }
            .average()
            .takeIf { it.isNaN().not() } ?: 0.0
        val avgDecisionTimeSeconds = avgDecisionTimeMs / 1000.0

        // Calculate trend (compare with previous period if possible)
        val trendingUp = if (period != StatsPeriod.DAILY) {
            val previousPeriodStart = when (period) {
                StatsPeriod.WEEKLY -> {
                    calendar.add(Calendar.DAY_OF_YEAR, -7)
                    calendar.timeInMillis
                }
                StatsPeriod.MONTHLY -> {
                    calendar.add(Calendar.DAY_OF_YEAR, -30)
                    calendar.timeInMillis
                }
                else -> startOfPeriod
            }

            val previousResults = interventionResultRepository.getResultsInRange(
                startTimestamp = previousPeriodStart,
                endTimestamp = startOfPeriod - 1
            )

            if (previousResults.isNotEmpty()) {
                val previousSuccessRate = (previousResults.count { it.userChoice.name == "GO_BACK" }.toFloat() / previousResults.size) * 100
                overallSuccessRate > previousSuccessRate
            } else {
                false
            }
        } else {
            false
        }

        return InterventionInsights(
            period = formatPeriod(period),
            totalInterventions = totalInterventions,
            successCount = successCount,
            proceedCount = proceedCount,
            overallSuccessRate = overallSuccessRate,
            contentTypeEffectiveness = contentTypeEffectiveness,
            mostEffectiveType = mostEffectiveType,
            leastEffectiveType = leastEffectiveType,
            timeBasedEffectiveness = timeBasedEffectiveness,
            bestTimeForSuccess = bestTimeForSuccess,
            contextBasedEffectiveness = contextBasedEffectiveness,
            avgDecisionTimeSeconds = avgDecisionTimeSeconds,
            trendingUp = trendingUp
        )
    }

    /**
     * Format content type name for display
     */
    private fun formatContentTypeName(contentType: String): String {
        return when (contentType) {
            "QUESTION" -> "Reflection Questions"
            "QUOTE" -> "Inspirational Quotes"
            "STAT" -> "Usage Statistics"
            "TIP" -> "Mindfulness Tips"
            "BREATHING" -> "Breathing Exercise"
            else -> contentType.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Format period for display
     */
    private fun formatPeriod(period: StatsPeriod): String {
        return when (period) {
            StatsPeriod.DAILY -> "Today"
            StatsPeriod.WEEKLY -> "This Week"
            StatsPeriod.MONTHLY -> "This Month"
        }
    }
}
