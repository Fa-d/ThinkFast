package dev.sadakat.thinkfaster.domain.intervention

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4: Reward Calculator
 *
 * Calculates reward signals for Thompson Sampling based on intervention outcomes.
 * Converts user responses and behavior into reward values (0.0 to 1.0).
 *
 * Reward Structure:
 * - GO_BACK (user left app): 1.0 (full success)
 * - CONTINUE (stayed but engaged): 0.3 (partial success)
 * - DISMISS (actively dismissed): 0.0 (failure)
 * - TIMEOUT (ignored): 0.1 (minor failure)
 *
 * Bonus Rewards:
 * - HELPFUL feedback: +0.2
 * - Session ended within 5 min: +0.1
 * - No quick reopen (5+ min): +0.1
 *
 * Penalty Rewards:
 * - DISRUPTIVE feedback: -0.3
 * - Quick reopen (<2 min): -0.2
 * - Extended session (>15 min): -0.1
 *
 * Usage:
 * ```
 * val reward = calculator.calculateReward(
 *     userChoice = "GO_BACK",
 *     feedback = "HELPFUL",
 *     sessionContinued = false
 * )
 * ```
 */
@Singleton
class RewardCalculator @Inject constructor() {

    companion object {
        // Base rewards
        private const val REWARD_GO_BACK = 1.0f
        private const val REWARD_CONTINUE = 0.3f
        private const val REWARD_DISMISS = 0.0f
        private const val REWARD_TIMEOUT = 0.1f

        // Bonus/penalty amounts
        private const val BONUS_HELPFUL_FEEDBACK = 0.2f
        private const val BONUS_SESSION_ENDED = 0.1f
        private const val BONUS_NO_QUICK_REOPEN = 0.1f
        private const val PENALTY_DISRUPTIVE_FEEDBACK = -0.3f
        private const val PENALTY_QUICK_REOPEN = -0.2f
        private const val PENALTY_EXTENDED_SESSION = -0.1f

        // Thresholds
        private const val QUICK_REOPEN_THRESHOLD_MS = 2 * 60 * 1000L  // 2 minutes
        private const val NO_REOPEN_THRESHOLD_MS = 5 * 60 * 1000L     // 5 minutes
        private const val SHORT_SESSION_THRESHOLD_MS = 5 * 60 * 1000L  // 5 minutes
        private const val EXTENDED_SESSION_THRESHOLD_MS = 15 * 60 * 1000L  // 15 minutes
    }

    /**
     * Calculate reward from intervention result
     */
    fun calculateReward(
        userChoice: String,
        feedback: String? = null,
        sessionContinued: Boolean? = null,
        sessionDurationAfter: Long? = null,
        quickReopen: Boolean? = null,
        reopenDelayMs: Long? = null
    ): Float {
        // Base reward from user choice
        var reward = when (userChoice) {
            "GO_BACK" -> REWARD_GO_BACK
            "CONTINUE" -> REWARD_CONTINUE
            "DISMISS" -> REWARD_DISMISS
            "TIMEOUT" -> REWARD_TIMEOUT
            else -> 0.5f  // Unknown, neutral
        }

        // Apply feedback bonuses/penalties
        feedback?.let {
            reward += when (it) {
                "HELPFUL" -> BONUS_HELPFUL_FEEDBACK
                "DISRUPTIVE" -> PENALTY_DISRUPTIVE_FEEDBACK
                else -> 0.0f
            }
        }

        // Session continuation bonus
        sessionContinued?.let {
            if (!it) {
                reward += BONUS_SESSION_ENDED
            }
        }

        // Session duration penalties
        sessionDurationAfter?.let { duration ->
            when {
                duration > EXTENDED_SESSION_THRESHOLD_MS -> reward += PENALTY_EXTENDED_SESSION
                duration <= SHORT_SESSION_THRESHOLD_MS -> reward += BONUS_SESSION_ENDED
            }
        }

        // Reopen behavior
        quickReopen?.let {
            if (it) {
                reward += PENALTY_QUICK_REOPEN
            }
        }

        reopenDelayMs?.let { delay ->
            when {
                delay < QUICK_REOPEN_THRESHOLD_MS -> reward += PENALTY_QUICK_REOPEN
                delay > NO_REOPEN_THRESHOLD_MS -> reward += BONUS_NO_QUICK_REOPEN
            }
        }

        // Clamp reward to [0.0, 1.0]
        return reward.coerceIn(0.0f, 1.0f)
    }

    /**
     * Calculate simplified binary reward (for Thompson Sampling pure mode)
     * @return 1.0 if successful (GO_BACK), 0.0 otherwise
     */
    fun calculateBinaryReward(userChoice: String): Float {
        return if (userChoice == "GO_BACK") 1.0f else 0.0f
    }

    /**
     * Calculate reward from comprehensive outcome entity
     */
    fun calculateRewardFromOutcome(
        outcome: dev.sadakat.thinkfaster.data.local.database.entities.InterventionResultEntity
    ): Float {
        return calculateReward(
            userChoice = outcome.userChoice,
            feedback = outcome.userFeedback,
            sessionContinued = null,  // Not available in entity
            sessionDurationAfter = outcome.finalSessionDurationMs,
            quickReopen = null,  // Can derive from timing
            reopenDelayMs = null
        )
    }

    /**
     * Get reward explanation (for debugging/analytics)
     */
    fun explainReward(
        userChoice: String,
        feedback: String? = null,
        sessionContinued: Boolean? = null,
        quickReopen: Boolean? = null
    ): String {
        val parts = mutableListOf<String>()

        // Base reward
        val baseReward = when (userChoice) {
            "GO_BACK" -> "GO_BACK (+1.0)"
            "CONTINUE" -> "CONTINUE (+0.3)"
            "DISMISS" -> "DISMISS (0.0)"
            "TIMEOUT" -> "TIMEOUT (+0.1)"
            else -> "Unknown (+0.5)"
        }
        parts.add(baseReward)

        // Modifiers
        feedback?.let {
            when (it) {
                "HELPFUL" -> parts.add("HELPFUL (+0.2)")
                "DISRUPTIVE" -> parts.add("DISRUPTIVE (-0.3)")
            }
        }

        sessionContinued?.let {
            if (!it) parts.add("Session ended (+0.1)")
        }

        quickReopen?.let {
            if (it) parts.add("Quick reopen (-0.2)")
        }

        return parts.joinToString(", ")
    }

    /**
     * Check if outcome is considered successful
     */
    fun isSuccessfulOutcome(userChoice: String, feedback: String? = null): Boolean {
        return when {
            userChoice == "GO_BACK" -> true
            userChoice == "CONTINUE" && feedback == "HELPFUL" -> true
            else -> false
        }
    }

    /**
     * Calculate normalized reward (scaled by confidence)
     * Lower confidence = more reward variance for exploration
     */
    fun calculateNormalizedReward(
        baseReward: Float,
        confidence: Float
    ): Float {
        // Add exploration bonus for low confidence
        val explorationBonus = (1.0f - confidence) * 0.1f
        return (baseReward + explorationBonus).coerceIn(0.0f, 1.0f)
    }
}
