package dev.sadakat.thinkfaster.domain.intervention

/**
 * Phase 1: Comprehensive Intervention Outcome Tracking
 *
 * Tracks both proximal (immediate) and distal (long-term) outcomes of interventions
 * to enable reinforcement learning and effectiveness optimization.
 *
 * This extends the basic InterventionResult with additional outcome metrics collected
 * at different time intervals after the intervention.
 */
data class ComprehensiveInterventionOutcome(
    // Reference to the original intervention
    val interventionId: Long,
    val sessionId: Long,
    val targetApp: String,
    val timestamp: Long,

    // ========== PROXIMAL OUTCOMES (Immediate - 0-5 seconds) ==========

    /**
     * User's immediate choice when shown the intervention
     * "GO_BACK", "CONTINUE", "SNOOZE", "DISMISS", "TIMEOUT"
     */
    val immediateChoice: InterventionUserChoice,

    /**
     * How quickly the user responded to the intervention (milliseconds)
     * Fast response (<2s) might indicate decisiveness
     * Slow response (>10s) might indicate contemplation or confusion
     */
    val responseTime: Long,

    /**
     * Level of user interaction with the intervention
     */
    val interactionDepth: InteractionDepth,

    // ========== SHORT-TERM OUTCOMES (5-30 minutes after intervention) ==========

    /**
     * Did the user continue using the app after the intervention?
     * null if not yet determined (intervention just happened)
     */
    val sessionContinued: Boolean? = null,

    /**
     * How long did the user stay in the app after seeing the intervention?
     * null if session hasn't ended or was still in progress when tracked
     */
    val sessionDurationAfter: Long? = null,

    /**
     * Did the user reopen the app within 5 minutes of closing?
     * Indicates potential compulsive behavior or intervention ineffectiveness
     */
    val quickReopen: Boolean? = null,

    /**
     * Did the user switch to a "productive" app instead?
     * Productive apps are defined by the user or system defaults
     */
    val switchedToProductiveApp: Boolean? = null,

    /**
     * Number of times user reopened ANY tracked app in 30 min window
     * Helps identify intervention-resistant patterns
     */
    val reopenCount30Min: Int? = null,

    // ========== MEDIUM-TERM OUTCOMES (Same day as intervention) ==========

    /**
     * Total usage reduction compared to typical same-day usage at same time
     * Positive = reduction, Negative = increase
     * null if not enough historical data to compare
     */
    val totalUsageReductionToday: Long? = null,

    /**
     * Did the user meet their daily goal on this day?
     * null if no goal was set
     */
    val goalMetToday: Boolean? = null,

    /**
     * How many additional sessions occurred on this app after the intervention?
     */
    val additionalSessionsToday: Int? = null,

    /**
     * Total screen time across all apps for this day (minutes)
     */
    val totalScreenTimeToday: Long? = null,

    // ========== LONG-TERM OUTCOMES (7-30 days after intervention) ==========

    /**
     * Usage change in the week following the intervention
     * Compared to the week before
     */
    val weeklyUsageChange: UsageChange? = null,

    /**
     * Did the user maintain or improve their streak after this intervention?
     */
    val streakMaintained: Boolean? = null,

    /**
     * Was the app uninstalled within 30 days?
     * Strong signal of effectiveness (for behavior change app)
     */
    val appUninstalled: Boolean? = null,

    /**
     * Is the user still actively using Intently 30 days later?
     * Critical for measuring intervention burden and user retention
     */
    val userRetention: Boolean? = null,

    /**
     * Average daily usage in 7 days after intervention (minutes)
     * null if less than 7 days have passed
     */
    val avgDailyUsageNext7Days: Long? = null,

    // ========== METADATA ==========

    /**
     * When the outcome data was last updated
     */
    val lastUpdated: Long = System.currentTimeMillis(),

    /**
     * Which time windows have been collected
     * Useful for knowing when to update outcomes
     */
    val collectionStatus: OutcomeCollectionStatus = OutcomeCollectionStatus()
)

/**
 * User's immediate choice when shown an intervention (for outcome tracking)
 * More detailed than the simple UserChoice enum in InterventionResult
 */
enum class InterventionUserChoice {
    GO_BACK,        // User chose to close the app (desired outcome)
    CONTINUE,       // User chose to continue using app (undesired outcome)
    SNOOZE,         // User snoozed the intervention (neutral outcome)
    DISMISS,        // User dismissed without choosing (negative signal)
    TIMEOUT         // User ignored until timeout (negative signal)
}

/**
 * Level of user engagement with the intervention
 */
enum class InteractionDepth {
    DISMISSED,      // Dismissed immediately without reading (<2 seconds)
    VIEWED,         // Viewed but minimal interaction (2-5 seconds)
    ENGAGED,        // Read and engaged with content (>5 seconds)
    INTERACTED      // Took specific action (e.g., breathing exercise, viewed stats)
}

/**
 * Usage trend comparison
 */
enum class UsageChange {
    DECREASED,      // Usage went down (good)
    STABLE,         // Usage stayed roughly the same
    INCREASED       // Usage went up (bad)
}

/**
 * Tracks which outcome collection windows have been completed
 */
data class OutcomeCollectionStatus(
    val proximalCollected: Boolean = false,        // Immediate outcome (0-5s)
    val shortTermCollected: Boolean = false,       // 5-30 min
    val mediumTermCollected: Boolean = false,      // Same day
    val longTermCollected: Boolean = false         // 7-30 days
)

/**
 * Extension function to calculate a comprehensive reward score
 * Used for reinforcement learning algorithms
 */
fun ComprehensiveInterventionOutcome.calculateReward(): Double {
    var reward = 0.0

    // Immediate outcomes (weight: 30%)
    reward += when (immediateChoice) {
        InterventionUserChoice.GO_BACK -> 10.0
        InterventionUserChoice.CONTINUE -> -5.0
        InterventionUserChoice.SNOOZE -> 0.0
        InterventionUserChoice.DISMISS -> -3.0
        InterventionUserChoice.TIMEOUT -> -8.0
    }

    // Interaction depth bonus
    reward += when (interactionDepth) {
        InteractionDepth.INTERACTED -> 3.0
        InteractionDepth.ENGAGED -> 1.5
        InteractionDepth.VIEWED -> 0.0
        InteractionDepth.DISMISSED -> -2.0
    }

    // Short-term outcomes (weight: 40%)
    if (sessionContinued == false) reward += 15.0  // User closed app

    sessionDurationAfter?.let { duration ->
        val minutes = duration / (60 * 1000)
        if (minutes < 5) reward += 10.0
        else if (minutes < 15) reward += 5.0
        // No reward/penalty for longer sessions
    }

    if (switchedToProductiveApp == true) reward += 8.0
    if (quickReopen == true) reward -= 12.0  // Bad sign

    reopenCount30Min?.let { count ->
        if (count == 0) reward += 8.0
        else if (count >= 3) reward -= 6.0
    }

    // Medium-term outcomes (weight: 20%)
    if (goalMetToday == true) reward += 6.0

    totalUsageReductionToday?.let { reduction ->
        val reductionMinutes = reduction / (60 * 1000)
        reward += reductionMinutes * 0.5  // +0.5 per minute reduced
    }

    additionalSessionsToday?.let { sessions ->
        if (sessions == 0) reward += 5.0
        else if (sessions >= 3) reward -= 3.0
    }

    // Long-term outcomes (weight: 10%)
    when (weeklyUsageChange) {
        UsageChange.DECREASED -> reward += 5.0
        UsageChange.INCREASED -> reward -= 3.0
        else -> { /* no change */ }
    }

    if (streakMaintained == true) reward += 3.0
    if (appUninstalled == true) reward += 10.0  // Ultimate success for tracked app
    if (userRetention == false) reward -= 20.0  // Major penalty for Intently churn

    avgDailyUsageNext7Days?.let { avgUsage ->
        // Reward for sustained usage reduction
        if (avgUsage < 30) reward += 5.0  // Less than 30 min/day average
        else if (avgUsage < 60) reward += 2.0
    }

    return reward
}

/**
 * Extension function to check if outcome is complete
 */
fun ComprehensiveInterventionOutcome.isFullyCollected(): Boolean {
    return collectionStatus.proximalCollected &&
           collectionStatus.shortTermCollected &&
           collectionStatus.mediumTermCollected &&
           collectionStatus.longTermCollected
}

/**
 * Extension function to get collection age in days
 */
fun ComprehensiveInterventionOutcome.getAgeInDays(): Int {
    val ageMs = System.currentTimeMillis() - timestamp
    return (ageMs / (1000 * 60 * 60 * 24)).toInt()
}

/**
 * Extension function to determine which collection window to update next
 */
fun ComprehensiveInterventionOutcome.getNextCollectionWindow(): CollectionWindow? {
    val ageMinutes = (System.currentTimeMillis() - timestamp) / (1000 * 60)

    return when {
        !collectionStatus.proximalCollected -> CollectionWindow.PROXIMAL
        !collectionStatus.shortTermCollected && ageMinutes >= 5 -> CollectionWindow.SHORT_TERM
        !collectionStatus.mediumTermCollected && ageMinutes >= 60 -> CollectionWindow.MEDIUM_TERM
        !collectionStatus.longTermCollected && getAgeInDays() >= 7 -> CollectionWindow.LONG_TERM
        else -> null  // All windows collected or not yet time
    }
}

enum class CollectionWindow {
    PROXIMAL,       // 0-5 seconds
    SHORT_TERM,     // 5-30 minutes
    MEDIUM_TERM,    // Same day
    LONG_TERM       // 7-30 days
}
