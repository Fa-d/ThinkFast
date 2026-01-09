package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4: RL Rollout Controller
 *
 * Manages gradual rollout of Reinforcement Learning (Thompson Sampling) content selection.
 * Implements A/B testing with automatic rollback if effectiveness drops.
 *
 * Features:
 * - Stable user assignment (hash-based)
 * - Configurable rollout percentage
 * - Effectiveness monitoring
 * - Automatic rollback on performance degradation
 *
 * Usage:
 * ```
 * val variant = controller.getUserVariant()
 * when (variant) {
 *     RLVariant.CONTROL -> useRuleBasedSelector()
 *     RLVariant.RL_TREATMENT -> useRLSelector()
 * }
 * ```
 */
@Singleton
class RLRolloutController @Inject constructor(
    private val preferences: InterventionPreferences
) {

    companion object {
        private const val PREF_ROLLOUT_PERCENTAGE = "rl_rollout_percentage"
        private const val PREF_ROLLOUT_ENABLED = "rl_rollout_enabled"
        private const val PREF_USER_VARIANT = "rl_user_variant"
        private const val PREF_RL_EFFECTIVENESS = "rl_effectiveness_score"
        private const val PREF_CONTROL_EFFECTIVENESS = "control_effectiveness_score"
        private const val PREF_LAST_EFFECTIVENESS_CHECK = "rl_last_effectiveness_check"
        private const val PREF_ROLLOUT_PERCENTAGE_INT = "rl_rollout_percentage_int"
        private const val PREF_ROLLOUT_ENABLED_STR = "rl_rollout_enabled_str"

        private const val DEFAULT_ROLLOUT_PERCENTAGE = 50  // 50% split
        private const val EFFECTIVENESS_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L  // 24 hours
        private const val ROLLBACK_THRESHOLD = 0.10f  // Rollback if RL is 10% worse
    }

    /**
     * Get user's assigned variant (stable assignment)
     */
    suspend fun getUserVariant(): RLVariant = withContext(Dispatchers.IO) {
        // Check if rollout is enabled
        if (!isRolloutEnabled()) {
            return@withContext RLVariant.CONTROL
        }

        // Get or assign variant
        val cachedVariant = preferences.getString(PREF_USER_VARIANT, "")
        if (cachedVariant.isNotEmpty()) {
            return@withContext try {
                RLVariant.valueOf(cachedVariant)
            } catch (e: Exception) {
                assignVariant()
            }
        }

        assignVariant()
    }

    /**
     * Assign variant based on user ID hash and rollout percentage
     */
    private suspend fun assignVariant(): RLVariant = withContext(Dispatchers.IO) {
        val rolloutPercentage = getRolloutPercentage()

        // Use anonymous user ID as stable identifier
        val userId = preferences.getAnonymousUserId()

        // Hash-based assignment (stable across app restarts)
        val hash = userId.hashCode().toLong().let { if (it < 0) -it else it }
        val bucket = (hash % 100).toInt()

        val variant = if (bucket < rolloutPercentage) {
            RLVariant.RL_TREATMENT
        } else {
            RLVariant.CONTROL
        }

        // Cache the assignment
        preferences.setString(PREF_USER_VARIANT, variant.name)
        variant
    }

    /**
     * Record intervention effectiveness for monitoring
     */
    suspend fun recordEffectiveness(
        variant: RLVariant,
        wasSuccessful: Boolean
    ) = withContext(Dispatchers.IO) {
        val key = when (variant) {
            RLVariant.RL_TREATMENT -> PREF_RL_EFFECTIVENESS
            RLVariant.CONTROL -> PREF_CONTROL_EFFECTIVENESS
        }

        // Update running average using exponential moving average
        val currentScore = preferences.getCooldownMultiplier()  // Reuse float storage
        val currentScoreStr = preferences.getString(key, "0.5")
        val currentScoreFloat = currentScoreStr.toFloatOrNull() ?: 0.5f

        val newObservation = if (wasSuccessful) 1.0f else 0.0f
        val alpha = 0.1f  // Learning rate
        val updatedScore = currentScoreFloat * (1 - alpha) + newObservation * alpha

        preferences.setString(key, updatedScore.toString())

        // Check for rollback condition
        checkForRollback()
    }

    /**
     * Check if RL effectiveness has degraded and rollback if needed
     */
    private suspend fun checkForRollback() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastCheckStr = preferences.getString(PREF_LAST_EFFECTIVENESS_CHECK, "0")
        val lastCheck = lastCheckStr.toLongOrNull() ?: 0L

        // Only check once per day
        if (now - lastCheck < EFFECTIVENESS_CHECK_INTERVAL_MS) {
            return@withContext
        }

        preferences.setString(PREF_LAST_EFFECTIVENESS_CHECK, now.toString())

        val rlScore = preferences.getString(PREF_RL_EFFECTIVENESS, "0.5").toFloatOrNull() ?: 0.5f
        val controlScore = preferences.getString(PREF_CONTROL_EFFECTIVENESS, "0.5").toFloatOrNull() ?: 0.5f

        // Rollback if RL is significantly worse than control
        if (controlScore > 0.0f && rlScore < controlScore * (1.0f - ROLLBACK_THRESHOLD)) {
            // Disable RL rollout
            setRolloutEnabled(false)

            android.util.Log.w(
                "RLRolloutController",
                "Automatic rollback triggered: RL=$rlScore, Control=$controlScore"
            )
        }
    }

    /**
     * Get current rollout percentage
     */
    fun getRolloutPercentage(): Int {
        return preferences.getString(PREF_ROLLOUT_PERCENTAGE_INT, DEFAULT_ROLLOUT_PERCENTAGE.toString()).toIntOrNull() ?: DEFAULT_ROLLOUT_PERCENTAGE
    }

    /**
     * Set rollout percentage (0-100)
     */
    suspend fun setRolloutPercentage(percentage: Int) = withContext(Dispatchers.IO) {
        require(percentage in 0..100) { "Percentage must be 0-100" }
        preferences.setString(PREF_ROLLOUT_PERCENTAGE_INT, percentage.toString())

        // Clear cached variant to force reassignment
        preferences.setString(PREF_USER_VARIANT, "")
    }

    /**
     * Check if rollout is enabled
     */
    fun isRolloutEnabled(): Boolean {
        return preferences.getString(PREF_ROLLOUT_ENABLED_STR, "true") == "true"
    }

    /**
     * Enable or disable rollout
     */
    suspend fun setRolloutEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferences.setString(PREF_ROLLOUT_ENABLED_STR, enabled.toString())
    }

    /**
     * Get effectiveness metrics
     */
    suspend fun getEffectivenessMetrics(): RLEffectivenessMetrics = withContext(Dispatchers.IO) {
        val rlScore = preferences.getString(PREF_RL_EFFECTIVENESS, "0.5").toFloatOrNull() ?: 0.5f
        val controlScore = preferences.getString(PREF_CONTROL_EFFECTIVENESS, "0.5").toFloatOrNull() ?: 0.5f
        val lastCheck = preferences.getString(PREF_LAST_EFFECTIVENESS_CHECK, "0").toLongOrNull() ?: 0L

        RLEffectivenessMetrics(
            rlScore = rlScore,
            controlScore = controlScore,
            rolloutPercentage = getRolloutPercentage(),
            isEnabled = isRolloutEnabled(),
            lastCheck = lastCheck
        )
    }

    /**
     * Reset all metrics (for testing)
     */
    suspend fun resetMetrics() = withContext(Dispatchers.IO) {
        preferences.setString(PREF_RL_EFFECTIVENESS, "0.5")
        preferences.setString(PREF_CONTROL_EFFECTIVENESS, "0.5")
        preferences.setString(PREF_LAST_EFFECTIVENESS_CHECK, "0")
        preferences.setString(PREF_USER_VARIANT, "")
    }

    /**
     * Force user into specific variant (for testing)
     */
    suspend fun forceVariant(variant: RLVariant) = withContext(Dispatchers.IO) {
        preferences.setString(PREF_USER_VARIANT, variant.name)
    }
}

/**
 * Phase 4: RL Rollout Variant
 */
enum class RLVariant {
    CONTROL,        // Rule-based persona selector
    RL_TREATMENT    // Thompson Sampling selector
}

/**
 * Phase 4: RL Effectiveness Metrics
 */
data class RLEffectivenessMetrics(
    val rlScore: Float,              // 0.0 to 1.0
    val controlScore: Float,         // 0.0 to 1.0
    val rolloutPercentage: Int,      // 0 to 100
    val isEnabled: Boolean,
    val lastCheck: Long
) {
    val rlPerformance: String
        get() = when {
            rlScore > controlScore + 0.05f -> "RL outperforming (+${((rlScore - controlScore) * 100).toInt()}%)"
            rlScore < controlScore - 0.05f -> "RL underperforming (${((rlScore - controlScore) * 100).toInt()}%)"
            else -> "RL and Control similar"
        }

    val shouldRollback: Boolean
        get() = controlScore > 0.0f && rlScore < controlScore * 0.9f
}
