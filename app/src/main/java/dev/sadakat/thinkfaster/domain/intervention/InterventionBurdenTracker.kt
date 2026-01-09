package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.local.database.dao.InterventionResultDao
import dev.sadakat.thinkfaster.data.local.database.entities.InterventionResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Phase 1: Intervention Burden Tracker
 * Phase 2: Enhanced with Fatigue Recovery and Trend Monitoring
 *
 * Calculates and monitors intervention burden metrics to detect user fatigue.
 * Provides methods to query burden levels and make adaptive frequency adjustments.
 *
 * Phase 2 Enhancements:
 * - Fatigue recovery tracking
 * - Burden trend analysis
 * - Proactive warnings
 *
 * Usage:
 * ```
 * val metrics = burdenTracker.calculateCurrentBurdenMetrics()
 * if (metrics.shouldReduceInterventions()) {
 *     // Apply burden reduction strategies
 * }
 * ```
 */
@Singleton
class InterventionBurdenTracker @Inject constructor(
    private val interventionResultDao: InterventionResultDao,
    private val fatigueRecoveryTracker: FatigueRecoveryTracker,
    private val burdenTrendMonitor: BurdenTrendMonitor
) {

    // Cache for burden metrics (invalidated every 10 minutes)
    private data class CachedMetrics(
        val metrics: InterventionBurdenMetrics,
        val timestamp: Long
    )

    private var cachedMetrics: CachedMetrics? = null
    private val CACHE_DURATION_MS = 10 * 60 * 1000  // 10 minutes

    /**
     * Calculate current burden metrics for the user
     * Results are cached for 10 minutes to reduce database load
     */
    suspend fun calculateCurrentBurdenMetrics(forceRefresh: Boolean = false): InterventionBurdenMetrics {
        // Check cache
        cachedMetrics?.let { cached ->
            if (!forceRefresh && System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS) {
                return cached.metrics
            }
        }

        val metrics = calculateBurdenMetricsInternal()
        cachedMetrics = CachedMetrics(metrics, System.currentTimeMillis())
        return metrics
    }

    /**
     * Calculate burden metrics for a specific time window
     */
    suspend fun calculateBurdenMetrics(
        startTime: Long,
        endTime: Long = System.currentTimeMillis()
    ): InterventionBurdenMetrics = withContext(Dispatchers.IO) {
        val interventions = interventionResultDao.getResultsInRange(startTime, endTime)
        calculateMetricsFromInterventions(interventions)
    }

    /**
     * Check if user is experiencing high burden
     */
    suspend fun isHighBurden(): Boolean {
        val metrics = calculateCurrentBurdenMetrics()
        return metrics.calculateBurdenLevel() in listOf(BurdenLevel.HIGH, BurdenLevel.CRITICAL)
    }

    /**
     * Get recommended cooldown adjustment based on current burden
     * Returns multiplier to apply to standard cooldown (1.0 = no change, 2.0 = double cooldown)
     */
    suspend fun getRecommendedCooldownAdjustment(): Float {
        val metrics = calculateCurrentBurdenMetrics()
        return if (metrics.isReliable()) {
            metrics.getRecommendedCooldownMultiplier()
        } else {
            1.0f  // No adjustment if insufficient data
        }
    }

    /**
     * Get burden summary for debugging/analytics
     */
    suspend fun getBurdenSummary(): String {
        val metrics = calculateCurrentBurdenMetrics()
        return metrics.getBurdenSummary()
    }

    /**
     * Invalidate cache to force recalculation
     */
    fun invalidateCache() {
        cachedMetrics = null
    }

    // ========== PHASE 2: ENHANCED BURDEN TRACKING ==========

    /**
     * Calculate burden with recovery credit applied
     * Phase 2: Gives users credit for healthy breaks
     */
    suspend fun calculateBurdenWithRecovery(): Int {
        val metrics = calculateCurrentBurdenMetrics()
        val baseScore = metrics.calculateBurdenScore()

        // Apply recovery credit if user took a break
        val recoveryCredit = fatigueRecoveryTracker.calculateRecoveryCredit()
        val adjustedScore = fatigueRecoveryTracker.applyRecoveryCredit(baseScore, recoveryCredit)

        // Record burden score for trend analysis
        burdenTrendMonitor.recordBurdenScore(adjustedScore)

        return adjustedScore
    }

    /**
     * Get burden trend analysis
     * Phase 2: Track if burden is increasing/decreasing
     */
    suspend fun getBurdenTrend(): BurdenTrendMonitor.BurdenTrend {
        val currentScore = calculateBurdenWithRecovery()
        return burdenTrendMonitor.analyzeTrend(currentScore)
    }

    /**
     * Check if user should see burden warning
     * Phase 2: Proactive warnings when burden escalates
     */
    suspend fun shouldShowBurdenWarning(): Boolean {
        val trend = getBurdenTrend()
        return burdenTrendMonitor.shouldShowBurdenWarning(trend)
    }

    /**
     * Check if user deserves burden relief
     * Phase 2: Positive reinforcement for good behavior
     */
    suspend fun shouldGrantBurdenRelief(): Boolean {
        return fatigueRecoveryTracker.shouldGrantBurdenRelief()
    }

    // ========== PRIVATE CALCULATION METHODS ==========

    private suspend fun calculateBurdenMetricsInternal(): InterventionBurdenMetrics = withContext(Dispatchers.IO) {
        // Get recent interventions (last 30 days)
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
        val allRecentInterventions = interventionResultDao.getResultsInRange(
            thirtyDaysAgo,
            System.currentTimeMillis()
        )

        calculateMetricsFromInterventions(allRecentInterventions)
    }

    private fun calculateMetricsFromInterventions(
        interventions: List<InterventionResultEntity>
    ): InterventionBurdenMetrics {
        if (interventions.isEmpty()) {
            return getDefaultMetrics(sampleSize = 0)
        }

        // Sort by timestamp (oldest first)
        val sortedInterventions = interventions.sortedBy { it.timestamp }

        // Calculate immediate burden indicators
        val avgResponseTime = sortedInterventions
            .map { it.timeToShowDecisionMs }
            .average()
            .toLong()

        val dismissCount = sortedInterventions.count { it.userChoice == "DISMISS" }
        val dismissRate = dismissCount.toFloat() / sortedInterventions.size

        val timeoutCount = sortedInterventions.count { it.userChoice == "TIMEOUT" }
        val timeoutRate = timeoutCount.toFloat() / sortedInterventions.size

        val snoozeFrequency = sortedInterventions.count { it.wasSnoozed }

        // Calculate engagement trends
        val now = System.currentTimeMillis()
        val twentyFourHoursAgo = now - (24 * 60 * 60 * 1000)
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000)

        val interventionsLast24h = sortedInterventions.count { it.timestamp >= twentyFourHoursAgo }
        val interventionsLast7d = sortedInterventions.count { it.timestamp >= sevenDaysAgo }

        val recentEngagementTrend = calculateEngagementTrend(sortedInterventions)

        // Calculate effectiveness metrics
        val effectivenessLast7d = sortedInterventions
            .filter { it.timestamp >= sevenDaysAgo }
            .let { recent ->
                if (recent.isEmpty()) 0f
                else recent.count { it.userChoice == "GO_BACK" }.toFloat() / recent.size
            }

        val effectivenessTrend = calculateEffectivenessTrend(sortedInterventions)

        val recentGoBackRate = sortedInterventions
            .takeLast(20)
            .let { recent ->
                if (recent.isEmpty()) 0f
                else recent.count { it.userChoice == "GO_BACK" }.toFloat() / recent.size
            }

        // Calculate explicit feedback metrics
        val helpfulCount = sortedInterventions.count { it.userFeedback == "HELPFUL" }
        val disruptiveCount = sortedInterventions.count { it.userFeedback == "DISRUPTIVE" }
        val totalExplicitFeedback = helpfulCount + disruptiveCount

        val helpfulnessRatio = if (totalExplicitFeedback > 0) {
            helpfulCount.toFloat() / totalExplicitFeedback
        } else {
            0.5f  // Neutral if no feedback
        }

        // Calculate intervention spacing
        val spacings = mutableListOf<Long>()
        for (i in 1 until sortedInterventions.size) {
            val spacing = (sortedInterventions[i].timestamp - sortedInterventions[i - 1].timestamp) / (60 * 1000)
            spacings.add(spacing)
        }

        val avgInterventionSpacing = if (spacings.isNotEmpty()) {
            spacings.average().toLong()
        } else 30L  // Default to 30 minutes if no spacing data

        val minInterventionSpacing = spacings.minOrNull() ?: 30L

        return InterventionBurdenMetrics(
            avgResponseTime = avgResponseTime,
            dismissRate = dismissRate,
            timeoutRate = timeoutRate,
            snoozeFrequency = snoozeFrequency,
            recentEngagementTrend = recentEngagementTrend,
            interventionsLast24h = interventionsLast24h,
            interventionsLast7d = interventionsLast7d,
            effectivenessRolling7d = effectivenessLast7d,
            effectivenessTrend = effectivenessTrend,
            recentGoBackRate = recentGoBackRate,
            helpfulFeedbackCount = helpfulCount,
            disruptiveFeedbackCount = disruptiveCount,
            helpfulnessRatio = helpfulnessRatio,
            avgInterventionSpacing = avgInterventionSpacing,
            minInterventionSpacing = minInterventionSpacing,
            sampleSize = sortedInterventions.size
        )
    }

    /**
     * Calculate engagement trend by comparing early vs. recent interventions
     * Uses "Go Back" rate as engagement proxy
     */
    private fun calculateEngagementTrend(
        interventions: List<InterventionResultEntity>
    ): Trend {
        if (interventions.size < 20) return Trend.STABLE  // Insufficient data

        // Compare first half to second half
        val midpoint = interventions.size / 2
        val earlyInterventions = interventions.subList(0, midpoint)
        val recentInterventions = interventions.subList(midpoint, interventions.size)

        val earlyEngagement = earlyInterventions.count { it.userChoice == "GO_BACK" }.toFloat() / earlyInterventions.size
        val recentEngagement = recentInterventions.count { it.userChoice == "GO_BACK" }.toFloat() / recentInterventions.size

        val difference = recentEngagement - earlyEngagement

        return when {
            difference > 0.10 -> Trend.INCREASING
            difference < -0.10 -> Trend.DECLINING
            else -> Trend.STABLE
        }
    }

    /**
     * Calculate effectiveness trend using linear regression over time
     * Positive slope = INCREASING, negative = DECLINING
     */
    private fun calculateEffectivenessTrend(
        interventions: List<InterventionResultEntity>
    ): Trend {
        if (interventions.size < 10) return Trend.STABLE  // Insufficient data

        // Take last 30 interventions for trend analysis
        val recentInterventions = interventions.takeLast(30)

        // Calculate effectiveness for each intervention (1.0 if GO_BACK, 0.0 otherwise)
        val dataPoints = recentInterventions.mapIndexed { index, intervention ->
            index.toDouble() to if (intervention.userChoice == "GO_BACK") 1.0 else 0.0
        }

        // Simple linear regression
        val n = dataPoints.size.toDouble()
        val sumX = dataPoints.sumOf { it.first }
        val sumY = dataPoints.sumOf { it.second }
        val sumXY = dataPoints.sumOf { it.first * it.second }
        val sumX2 = dataPoints.sumOf { it.first * it.first }

        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)

        return when {
            slope > 0.02 -> Trend.INCREASING   // Effectiveness improving
            slope < -0.02 -> Trend.DECLINING   // Effectiveness declining
            else -> Trend.STABLE
        }
    }

    private fun getDefaultMetrics(sampleSize: Int): InterventionBurdenMetrics {
        return InterventionBurdenMetrics(
            avgResponseTime = 5000L,  // 5 seconds default
            dismissRate = 0.0f,
            timeoutRate = 0.0f,
            snoozeFrequency = 0,
            recentEngagementTrend = Trend.STABLE,
            interventionsLast24h = 0,
            interventionsLast7d = 0,
            effectivenessRolling7d = 0.5f,
            effectivenessTrend = Trend.STABLE,
            recentGoBackRate = 0.5f,
            helpfulFeedbackCount = 0,
            disruptiveFeedbackCount = 0,
            helpfulnessRatio = 0.5f,
            avgInterventionSpacing = 30L,
            minInterventionSpacing = 30L,
            sampleSize = sampleSize
        )
    }
}
