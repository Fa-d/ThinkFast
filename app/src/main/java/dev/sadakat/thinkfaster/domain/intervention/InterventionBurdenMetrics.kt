package dev.sadakat.thinkfaster.domain.intervention

/**
 * Phase 1: Intervention Burden Metrics
 *
 * Tracks the cumulative cognitive and emotional cost of being interrupted by interventions.
 * High burden leads to user fatigue, decreased engagement, and potential app abandonment.
 *
 * Research shows that intervention fatigue is a major factor in long-term engagement,
 * and optimal intervention frequency differs across individuals.
 *
 * These metrics help identify when users are experiencing intervention overload so the
 * system can adaptively reduce frequency.
 */
data class InterventionBurdenMetrics(
    // ========== IMMEDIATE BURDEN INDICATORS ==========

    /**
     * Average time to respond to interventions (milliseconds)
     * Slow response (>10s) may indicate burden or confusion
     * Fast response (<2s) may indicate dismissal without reading
     */
    val avgResponseTime: Long,

    /**
     * Percentage of interventions dismissed without meaningful interaction
     * High dismissal rate (>40%) indicates intervention fatigue
     */
    val dismissRate: Float,  // 0.0 - 1.0

    /**
     * Percentage of interventions that timed out (user ignored)
     * High timeout rate (>30%) indicates disengagement
     */
    val timeoutRate: Float,  // 0.0 - 1.0

    /**
     * Number of times user snoozed interventions recently
     * Frequent snoozing indicates user wants less frequent interruptions
     */
    val snoozeFrequency: Int,

    // ========== ENGAGEMENT TRENDS ==========

    /**
     * Trend in user engagement with interventions
     * DECLINING trend is a major warning sign of burden
     */
    val recentEngagementTrend: Trend,

    /**
     * Total number of interventions in last 24 hours
     * Used to detect overload
     */
    val interventionsLast24h: Int,

    /**
     * Total number of interventions in last 7 days
     */
    val interventionsLast7d: Int,

    // ========== EFFECTIVENESS DECAY ==========

    /**
     * Effectiveness ("Go Back" rate) over last 7 days
     * Range: 0.0 - 1.0
     */
    val effectivenessRolling7d: Float,

    /**
     * Trend in intervention effectiveness
     * DECLINING effectiveness suggests habituation or fatigue
     */
    val effectivenessTrend: Trend,

    /**
     * Percentage of recent interventions resulting in "GO_BACK"
     * Calculated over last 20 interventions
     */
    val recentGoBackRate: Float,  // 0.0 - 1.0

    // ========== EXPLICIT FEEDBACK ==========

    /**
     * Count of "HELPFUL" feedback in recent interventions
     */
    val helpfulFeedbackCount: Int,

    /**
     * Count of "DISRUPTIVE" feedback in recent interventions
     */
    val disruptiveFeedbackCount: Int,

    /**
     * Ratio of helpful to total explicit feedback
     * Low ratio (<0.3) indicates high burden
     */
    val helpfulnessRatio: Float,  // 0.0 - 1.0

    // ========== INTERVENTION SPACING ==========

    /**
     * Average time between interventions (minutes)
     * Too short (<5 min) may feel overwhelming
     */
    val avgInterventionSpacing: Long,

    /**
     * Minimum spacing seen in recent interventions (minutes)
     * Identifies if interventions are "bunching up"
     */
    val minInterventionSpacing: Long,

    // ========== METADATA ==========

    /**
     * When these metrics were calculated
     */
    val calculatedAt: Long = System.currentTimeMillis(),

    /**
     * Number of interventions analyzed for these metrics
     * Higher sample size = more reliable metrics
     */
    val sampleSize: Int
)

/**
 * Trend direction for metrics
 */
enum class Trend {
    INCREASING,   // Metric is going up
    STABLE,       // Metric is relatively flat
    DECLINING     // Metric is going down
}

/**
 * Burden level classification
 */
enum class BurdenLevel {
    LOW,          // User handling interventions well
    MODERATE,     // Some signs of burden, monitor closely
    HIGH,         // Clear burden signals, reduce frequency
    CRITICAL      // Severe burden, minimize interventions
}

/**
 * Extension function to calculate burden score
 * Returns raw numeric score for more granular analysis
 */
fun InterventionBurdenMetrics.calculateBurdenScore(): Int {
    var burdenScore = 0

    // Dismissal indicators (+3 points each)
    if (dismissRate > 0.40) burdenScore += 3
    if (timeoutRate > 0.30) burdenScore += 3

    // Trend indicators (+4 points each)
    if (recentEngagementTrend == Trend.DECLINING) burdenScore += 4
    if (effectivenessTrend == Trend.DECLINING) burdenScore += 4

    // Frequency indicators (+2 points each)
    if (interventionsLast24h > 15) burdenScore += 2
    if (avgInterventionSpacing < 10) burdenScore += 2  // Less than 10 minutes
    if (minInterventionSpacing < 3) burdenScore += 3   // Less than 3 minutes

    // Effectiveness indicator (+3 points)
    if (effectivenessRolling7d < 0.35) burdenScore += 3

    // Feedback indicator (+5 points - explicit negative feedback is serious)
    if (helpfulnessRatio < 0.30 && (helpfulFeedbackCount + disruptiveFeedbackCount) >= 5) {
        burdenScore += 5
    }

    // Snooze indicator (+2 points)
    if (snoozeFrequency > 5) burdenScore += 2

    return burdenScore
}

/**
 * Extension function to calculate overall burden level
 */
fun InterventionBurdenMetrics.calculateBurdenLevel(): BurdenLevel {
    val burdenScore = calculateBurdenScore()

    return when {
        burdenScore >= 15 -> BurdenLevel.CRITICAL
        burdenScore >= 10 -> BurdenLevel.HIGH
        burdenScore >= 5 -> BurdenLevel.MODERATE
        else -> BurdenLevel.LOW
    }
}

/**
 * Extension function to check if intervention reduction is needed
 */
fun InterventionBurdenMetrics.shouldReduceInterventions(): Boolean {
    return when {
        // High dismiss/timeout rates
        dismissRate > 0.40 || timeoutRate > 0.30 -> true

        // Declining engagement
        recentEngagementTrend == Trend.DECLINING &&
                effectivenessTrend == Trend.DECLINING -> true

        // Intervention overload
        interventionsLast24h > 20 -> true

        // Explicit negative feedback
        helpfulnessRatio < 0.25 &&
                (helpfulFeedbackCount + disruptiveFeedbackCount) >= 5 -> true

        // Too frequent interventions
        avgInterventionSpacing < 8 -> true  // Less than 8 minutes

        else -> false
    }
}

/**
 * Extension function to get recommended cooldown multiplier
 * Based on burden level, returns multiplier for standard cooldown
 */
fun InterventionBurdenMetrics.getRecommendedCooldownMultiplier(): Float {
    return when (calculateBurdenLevel()) {
        BurdenLevel.CRITICAL -> 4.0f     // 4x longer cooldowns
        BurdenLevel.HIGH -> 2.5f         // 2.5x longer cooldowns
        BurdenLevel.MODERATE -> 1.5f     // 1.5x longer cooldowns
        BurdenLevel.LOW -> 1.0f          // Normal cooldowns
    }
}

/**
 * Extension function to get human-readable burden summary
 */
fun InterventionBurdenMetrics.getBurdenSummary(): String {
    val level = calculateBurdenLevel()
    return when (level) {
        BurdenLevel.CRITICAL -> "CRITICAL: User showing severe intervention fatigue. " +
                "Dismiss rate: ${(dismissRate * 100).toInt()}%, Effectiveness: ${(effectivenessRolling7d * 100).toInt()}%"

        BurdenLevel.HIGH -> "HIGH: Clear signs of burden. " +
                "Recent engagement: $recentEngagementTrend, " +
                "${interventionsLast24h} interventions in 24h"

        BurdenLevel.MODERATE -> "MODERATE: Some burden indicators. " +
                "Dismiss rate: ${(dismissRate * 100).toInt()}%, " +
                "Avg spacing: ${avgInterventionSpacing} min"

        BurdenLevel.LOW -> "LOW: User handling interventions well. " +
                "Effectiveness: ${(effectivenessRolling7d * 100).toInt()}%, " +
                "Go back rate: ${(recentGoBackRate * 100).toInt()}%"
    }
}

/**
 * Extension function to check if metrics are reliable
 * Low sample size means metrics may not be trustworthy
 */
fun InterventionBurdenMetrics.isReliable(): Boolean {
    return sampleSize >= 10  // Need at least 10 interventions for reliable metrics
}

/**
 * Extension function to identify primary burden factors
 * Returns list of factors contributing to burden
 */
fun InterventionBurdenMetrics.identifyBurdenFactors(): List<String> {
    val factors = mutableListOf<String>()

    if (dismissRate > 0.40) factors.add("High dismissal rate (${(dismissRate * 100).toInt()}%)")
    if (timeoutRate > 0.30) factors.add("High timeout rate (${(timeoutRate * 100).toInt()}%)")
    if (recentEngagementTrend == Trend.DECLINING) factors.add("Declining engagement trend")
    if (effectivenessTrend == Trend.DECLINING) factors.add("Declining effectiveness trend")
    if (interventionsLast24h > 15) factors.add("Too many interventions (${interventionsLast24h} in 24h)")
    if (avgInterventionSpacing < 10) factors.add("Interventions too frequent (avg ${avgInterventionSpacing} min)")
    if (effectivenessRolling7d < 0.35) factors.add("Low effectiveness (${(effectivenessRolling7d * 100).toInt()}%)")
    if (helpfulnessRatio < 0.30) factors.add("Negative explicit feedback (${(helpfulnessRatio * 100).toInt()}% helpful)")

    return factors
}
