package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.local.database.dao.InterventionResultDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Phase 3: Contextual Timing Optimizer
 *
 * Learns from historical effectiveness data to optimize intervention timing.
 * Unlike OpportunityDetector which scores the current moment, this class
 * learns which times/contexts actually work and recommends delays if needed.
 *
 * Key Features:
 * - Time-of-day effectiveness learning
 * - Context-based delay recommendations
 * - Adaptive timing window identification
 * - Per-app timing pattern learning
 *
 * Usage:
 * ```
 * val timing = optimizer.getOptimalTiming(targetApp, currentHour, context)
 * if (timing.shouldDelay) {
 *     // Delay intervention by timing.recommendedDelayMs
 * }
 * ```
 */
@Singleton
class ContextualTimingOptimizer @Inject constructor(
    private val interventionResultDao: InterventionResultDao
) {

    companion object {
        private const val MIN_DATA_POINTS = 20  // Minimum interventions for reliable patterns
        private const val HIGH_SUCCESS_THRESHOLD = 0.60  // 60%+ success rate
        private const val LOW_SUCCESS_THRESHOLD = 0.30   // <30% success rate
        private const val CACHE_DURATION_MS = 15 * 60 * 1000L  // 15 minutes

        // Time windows (hours)
        private const val NIGHT_START = 22
        private const val NIGHT_END = 5
        private const val MORNING_START = 6
        private const val MORNING_END = 11
        private const val AFTERNOON_START = 12
        private const val AFTERNOON_END = 17
        private const val EVENING_START = 18
        private const val EVENING_END = 21
    }

    private data class CachedTimingAnalysis(
        val analysis: TimingAnalysis,
        val timestamp: Long,
        val appHash: String
    )

    private var cache: CachedTimingAnalysis? = null

    /**
     * Get optimal timing recommendation for an intervention
     */
    suspend fun getOptimalTiming(
        targetApp: String,
        currentHour: Int,
        isWeekend: Boolean,
        forceRefresh: Boolean = false
    ): TimingRecommendation = withContext(Dispatchers.IO) {
        // Check cache
        val cacheKey = "$targetApp-$currentHour-$isWeekend"
        cache?.let { cached ->
            if (!forceRefresh &&
                System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS &&
                cached.appHash == cacheKey) {
                return@withContext buildRecommendation(cached.analysis, currentHour, isWeekend)
            }
        }

        // Calculate fresh analysis
        val analysis = analyzeTimingPatterns(targetApp)
        cache = CachedTimingAnalysis(analysis, System.currentTimeMillis(), cacheKey)

        buildRecommendation(analysis, currentHour, isWeekend)
    }

    /**
     * Analyze historical timing patterns for effectiveness
     */
    private suspend fun analyzeTimingPatterns(targetApp: String): TimingAnalysis {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        val interventions = interventionResultDao.getResultsInRange(thirtyDaysAgo, System.currentTimeMillis())
            .filter { it.targetApp == targetApp }

        if (interventions.size < MIN_DATA_POINTS) {
            return TimingAnalysis.insufficient()
        }

        // Analyze by hour of day
        val hourlyStats = (0..23).map { hour ->
            val hourInterventions = interventions.filter { it.hourOfDay == hour }
            if (hourInterventions.isEmpty()) {
                HourlyEffectiveness(hour, 0, 0.5f, false)
            } else {
                val successCount = hourInterventions.count { it.userChoice == "GO_BACK" }
                val successRate = successCount.toFloat() / hourInterventions.size
                HourlyEffectiveness(
                    hour = hour,
                    sampleSize = hourInterventions.size,
                    successRate = successRate,
                    reliable = hourInterventions.size >= 5
                )
            }
        }

        // Analyze by time window
        val windowStats = listOf(
            analyzeWindow(interventions, NIGHT_START, NIGHT_END, "Night"),
            analyzeWindow(interventions, MORNING_START, MORNING_END, "Morning"),
            analyzeWindow(interventions, AFTERNOON_START, AFTERNOON_END, "Afternoon"),
            analyzeWindow(interventions, EVENING_START, EVENING_END, "Evening")
        )

        // Find best and worst hours
        val reliableHours = hourlyStats.filter { it.reliable }
        val bestHours = reliableHours
            .filter { it.successRate >= HIGH_SUCCESS_THRESHOLD }
            .sortedByDescending { it.successRate }
            .take(3)
            .map { it.hour }

        val worstHours = reliableHours
            .filter { it.successRate <= LOW_SUCCESS_THRESHOLD }
            .sortedBy { it.successRate }
            .take(3)
            .map { it.hour }

        // Analyze weekend vs weekday
        val weekendInterventions = interventions.filter { it.isWeekend }
        val weekdayInterventions = interventions.filterNot { it.isWeekend }

        val weekendSuccess = if (weekendInterventions.isNotEmpty()) {
            weekendInterventions.count { it.userChoice == "GO_BACK" }.toFloat() / weekendInterventions.size
        } else 0.5f

        val weekdaySuccess = if (weekdayInterventions.isNotEmpty()) {
            weekdayInterventions.count { it.userChoice == "GO_BACK" }.toFloat() / weekdayInterventions.size
        } else 0.5f

        return TimingAnalysis(
            hasSufficientData = true,
            totalSamples = interventions.size,
            hourlyStats = hourlyStats,
            windowStats = windowStats,
            bestHours = bestHours,
            worstHours = worstHours,
            weekendSuccessRate = weekendSuccess,
            weekdaySuccessRate = weekdaySuccess,
            overallSuccessRate = interventions.count { it.userChoice == "GO_BACK" }.toFloat() / interventions.size
        )
    }

    /**
     * Analyze effectiveness for a specific time window
     */
    private fun analyzeWindow(
        interventions: List<dev.sadakat.thinkfaster.data.local.database.entities.InterventionResultEntity>,
        startHour: Int,
        endHour: Int,
        label: String
    ): WindowEffectiveness {
        val windowInterventions = interventions.filter { intervention ->
            if (startHour > endHour) {
                // Wraps around midnight (e.g., 22-5)
                intervention.hourOfDay >= startHour || intervention.hourOfDay <= endHour
            } else {
                intervention.hourOfDay in startHour..endHour
            }
        }

        if (windowInterventions.isEmpty()) {
            return WindowEffectiveness(label, 0, 0.5f, false)
        }

        val successCount = windowInterventions.count { it.userChoice == "GO_BACK" }
        val successRate = successCount.toFloat() / windowInterventions.size

        return WindowEffectiveness(
            window = label,
            sampleSize = windowInterventions.size,
            successRate = successRate,
            reliable = windowInterventions.size >= 10
        )
    }

    /**
     * Build timing recommendation based on analysis
     */
    private fun buildRecommendation(
        analysis: TimingAnalysis,
        currentHour: Int,
        isWeekend: Boolean
    ): TimingRecommendation {
        // Insufficient data - allow intervention with no specific recommendation
        if (!analysis.hasSufficientData) {
            return TimingRecommendation(
                shouldInterveneNow = true,
                shouldDelay = false,
                recommendedDelayMs = 0L,
                reason = "Insufficient data for timing optimization",
                confidence = TimingConfidence.LOW,
                alternativeHours = emptyList()
            )
        }

        // Check if current hour is a worst performer
        val isWorstTime = currentHour in analysis.worstHours
        val isBestTime = currentHour in analysis.bestHours

        // Get current hour's effectiveness
        val currentHourStats = analysis.hourlyStats.find { it.hour == currentHour }
        val currentSuccessRate = currentHourStats?.successRate ?: 0.5f

        return when {
            // Excellent time - intervene now
            isBestTime && currentHourStats?.reliable == true -> {
                TimingRecommendation(
                    shouldInterveneNow = true,
                    shouldDelay = false,
                    recommendedDelayMs = 0L,
                    reason = "High success rate at this hour (${(currentSuccessRate * 100).toInt()}%)",
                    confidence = TimingConfidence.HIGH,
                    alternativeHours = emptyList()
                )
            }

            // Poor time - recommend delay to next best hour
            isWorstTime && currentHourStats?.reliable == true -> {
                val nextBestHour = findNextBestHour(currentHour, analysis.bestHours)
                val delayMs = calculateDelayToHour(currentHour, nextBestHour)

                TimingRecommendation(
                    shouldInterveneNow = false,
                    shouldDelay = true,
                    recommendedDelayMs = delayMs,
                    reason = "Low success rate at this hour (${(currentSuccessRate * 100).toInt()}%), better at ${nextBestHour}:00",
                    confidence = TimingConfidence.HIGH,
                    alternativeHours = analysis.bestHours
                )
            }

            // Moderate time - allow but suggest alternatives
            currentSuccessRate >= 0.40f -> {
                TimingRecommendation(
                    shouldInterveneNow = true,
                    shouldDelay = false,
                    recommendedDelayMs = 0L,
                    reason = "Moderate success rate at this hour (${(currentSuccessRate * 100).toInt()}%)",
                    confidence = TimingConfidence.MEDIUM,
                    alternativeHours = analysis.bestHours.take(2)
                )
            }

            // Below average - consider short delay
            else -> {
                val nextBestHour = findNextBestHour(currentHour, analysis.bestHours)
                val delayMs = calculateDelayToHour(currentHour, nextBestHour)

                TimingRecommendation(
                    shouldInterveneNow = true,  // Still allow but flag
                    shouldDelay = false,
                    recommendedDelayMs = delayMs,
                    reason = "Below average timing, better results at ${analysis.bestHours.joinToString()}:00",
                    confidence = TimingConfidence.MEDIUM,
                    alternativeHours = analysis.bestHours
                )
            }
        }
    }

    /**
     * Find the next best hour after current hour
     */
    private fun findNextBestHour(currentHour: Int, bestHours: List<Int>): Int {
        if (bestHours.isEmpty()) return (currentHour + 2) % 24

        // Find closest best hour in the future
        val sortedBest = bestHours.sorted()
        val nextBest = sortedBest.firstOrNull { it > currentHour }
        return nextBest ?: sortedBest.first()  // Wrap to first if none ahead
    }

    /**
     * Calculate delay in milliseconds to reach target hour
     */
    private fun calculateDelayToHour(currentHour: Int, targetHour: Int): Long {
        val hoursUntil = if (targetHour > currentHour) {
            targetHour - currentHour
        } else {
            24 - currentHour + targetHour  // Wrap around midnight
        }

        return hoursUntil * 60 * 60 * 1000L
    }

    /**
     * Invalidate cache to force recalculation
     */
    fun invalidateCache() {
        cache = null
    }
}

/**
 * Phase 3: Complete timing analysis
 */
data class TimingAnalysis(
    val hasSufficientData: Boolean,
    val totalSamples: Int,
    val hourlyStats: List<HourlyEffectiveness>,
    val windowStats: List<WindowEffectiveness>,
    val bestHours: List<Int>,
    val worstHours: List<Int>,
    val weekendSuccessRate: Float,
    val weekdaySuccessRate: Float,
    val overallSuccessRate: Float
) {
    companion object {
        fun insufficient() = TimingAnalysis(
            hasSufficientData = false,
            totalSamples = 0,
            hourlyStats = emptyList(),
            windowStats = emptyList(),
            bestHours = emptyList(),
            worstHours = emptyList(),
            weekendSuccessRate = 0.5f,
            weekdaySuccessRate = 0.5f,
            overallSuccessRate = 0.5f
        )
    }
}

/**
 * Phase 3: Effectiveness by hour of day
 */
data class HourlyEffectiveness(
    val hour: Int,
    val sampleSize: Int,
    val successRate: Float,
    val reliable: Boolean  // Has enough samples for reliable estimate
)

/**
 * Phase 3: Effectiveness by time window
 */
data class WindowEffectiveness(
    val window: String,
    val sampleSize: Int,
    val successRate: Float,
    val reliable: Boolean
)

/**
 * Phase 3: Timing recommendation
 */
data class TimingRecommendation(
    val shouldInterveneNow: Boolean,
    val shouldDelay: Boolean,
    val recommendedDelayMs: Long,
    val reason: String,
    val confidence: TimingConfidence,
    val alternativeHours: List<Int>
)

/**
 * Phase 3: Confidence in timing recommendation
 */
enum class TimingConfidence {
    LOW,      // Insufficient data
    MEDIUM,   // Some data, moderate confidence
    HIGH      // Strong data, high confidence
}
