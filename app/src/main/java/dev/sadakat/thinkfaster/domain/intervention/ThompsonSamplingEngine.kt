package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Phase 4: Thompson Sampling Engine
 *
 * Implements Thompson Sampling (TS) for multi-armed bandit content selection.
 * TS is a Bayesian reinforcement learning algorithm that balances exploration
 * and exploitation by sampling from Beta distributions.
 *
 * Key Features:
 * - No pre-training required (starts with zero data)
 * - Online learning from each interaction
 * - Automatic exploration vs exploitation balance
 * - Bayesian updates with Beta distributions
 * - Per-arm (content type) success tracking
 *
 * Algorithm:
 * 1. Each arm has a Beta(α, β) distribution representing belief about success rate
 * 2. Sample a value θ ~ Beta(α, β) for each arm
 * 3. Select the arm with highest sampled θ
 * 4. Observe reward r (1 for success, 0 for failure)
 * 5. Update: α = α + r, β = β + (1 - r)
 *
 * Usage:
 * ```
 * // Select best content type
 * val selection = engine.selectArm()
 *
 * // After intervention completes
 * engine.updateArm(armId, reward = 1.0f)  // 1.0 = success, 0.0 = failure
 * ```
 */
@Singleton
class ThompsonSamplingEngine @Inject constructor(
    private val preferences: InterventionPreferences
) {

    companion object {
        private const val PREF_TS_STATE = "thompson_sampling_state_v1"

        // Initial prior parameters (optimistic initialization)
        private const val INITIAL_ALPHA = 1.0  // Prior successes (optimistic start)
        private const val INITIAL_BETA = 1.0   // Prior failures (optimistic start)

        // Content type arm IDs
        const val ARM_REFLECTION = "ReflectionQuestion"
        const val ARM_TIME_ALTERNATIVE = "TimeAlternative"
        const val ARM_BREATHING = "BreathingExercise"
        const val ARM_USAGE_STATS = "UsageStats"
        const val ARM_EMOTIONAL = "EmotionalAppeal"
        const val ARM_QUOTE = "Quote"
        const val ARM_GAMIFICATION = "Gamification"
        const val ARM_ACTIVITY = "ActivitySuggestion"

        val ALL_ARMS = listOf(
            ARM_REFLECTION,
            ARM_TIME_ALTERNATIVE,
            ARM_BREATHING,
            ARM_USAGE_STATS,
            ARM_EMOTIONAL,
            ARM_QUOTE,
            ARM_GAMIFICATION,
            ARM_ACTIVITY
        )
    }

    /**
     * Select the best arm using Thompson Sampling
     * @param excludedArms Arms to exclude from selection (optional)
     * @return Selected arm ID and confidence
     */
    suspend fun selectArm(excludedArms: Set<String> = emptySet()): ArmSelection = withContext(Dispatchers.IO) {
        val state = loadState()
        val availableArms = ALL_ARMS.filter { it !in excludedArms }

        if (availableArms.isEmpty()) {
            // Fallback to reflection if all excluded
            return@withContext ArmSelection(ARM_REFLECTION, 0.5f, "fallback")
        }

        // Sample from Beta distribution for each arm
        val sampledValues = mutableMapOf<String, Double>()
        for (armId in availableArms) {
            val armState = state.arms[armId] ?: ArmState(INITIAL_ALPHA, INITIAL_BETA)
            val sampledTheta = sampleBeta(armState.alpha, armState.beta)
            sampledValues[armId] = sampledTheta
        }

        // Select arm with highest sampled value
        val selectedArm = sampledValues.maxByOrNull { it.value }!!.key
        val selectedValue = sampledValues[selectedArm]!!
        val armState = state.arms[selectedArm] ?: ArmState(INITIAL_ALPHA, INITIAL_BETA)

        // Calculate confidence (based on number of pulls)
        val totalPulls = armState.alpha + armState.beta - 2.0  // Subtract initial priors
        val confidence = calculateConfidence(totalPulls.toInt())

        ArmSelection(
            armId = selectedArm,
            confidence = confidence,
            strategy = "thompson_sampling",
            sampledValue = selectedValue.toFloat(),
            totalPulls = totalPulls.toInt()
        )
    }

    /**
     * Update arm state after observing reward
     * @param armId The arm that was pulled
     * @param reward Reward value (0.0 to 1.0, typically binary 0 or 1)
     */
    suspend fun updateArm(armId: String, reward: Float) = withContext(Dispatchers.IO) {
        require(reward in 0.0f..1.0f) { "Reward must be between 0 and 1" }
        require(armId in ALL_ARMS) { "Invalid arm ID: $armId" }

        val state = loadState()
        val armState = state.arms[armId] ?: ArmState(INITIAL_ALPHA, INITIAL_BETA)

        // Bayesian update: α += reward, β += (1 - reward)
        val updatedState = armState.copy(
            alpha = armState.alpha + reward.toDouble(),
            beta = armState.beta + (1.0 - reward.toDouble())
        )

        state.arms[armId] = updatedState
        saveState(state)
    }

    /**
     * Get current arm statistics
     */
    suspend fun getArmStats(armId: String): ArmStats? = withContext(Dispatchers.IO) {
        val state = loadState()
        val armState = state.arms[armId] ?: return@withContext null

        val totalPulls = (armState.alpha + armState.beta - 2.0).toInt()  // Subtract initial priors
        val estimatedSuccessRate = armState.alpha / (armState.alpha + armState.beta)
        val uncertainty = calculateUncertainty(armState.alpha, armState.beta)

        ArmStats(
            armId = armId,
            totalPulls = totalPulls,
            estimatedSuccessRate = estimatedSuccessRate.toFloat(),
            uncertainty = uncertainty.toFloat(),
            alpha = armState.alpha,
            beta = armState.beta
        )
    }

    /**
     * Get statistics for all arms
     */
    suspend fun getAllArmStats(): List<ArmStats> = withContext(Dispatchers.IO) {
        ALL_ARMS.mapNotNull { getArmStats(it) }
    }

    /**
     * Reset all arm states (for testing or reset)
     */
    suspend fun resetAllArms() = withContext(Dispatchers.IO) {
        preferences.setString(PREF_TS_STATE, "")
    }

    /**
     * Check if we have sufficient data for reliable recommendations
     */
    suspend fun hasSufficientData(): Boolean = withContext(Dispatchers.IO) {
        val state = loadState()
        val totalPulls = state.arms.values.sumOf { it.alpha + it.beta - 2.0 }
        totalPulls >= 30  // Minimum 30 interventions
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Sample from Beta(α, β) distribution
     * Uses inverse transform sampling with Gamma approximation
     */
    private fun sampleBeta(alpha: Double, beta: Double): Double {
        // For simplicity, use method of moments approximation
        // Beta(α, β) can be approximated by sampling two Gamma distributions
        val x = sampleGamma(alpha, 1.0)
        val y = sampleGamma(beta, 1.0)
        return x / (x + y)
    }

    /**
     * Sample from Gamma(α, β) distribution
     * Uses Marsaglia and Tsang's method
     */
    private fun sampleGamma(alpha: Double, beta: Double): Double {
        if (alpha < 1.0) {
            // Use rejection method for small alpha
            val u = Random.nextDouble()
            return sampleGamma(1.0 + alpha, beta) * u.pow(1.0 / alpha)
        }

        val d = alpha - 1.0 / 3.0
        val c = 1.0 / sqrt(9.0 * d)

        var x: Double
        var v: Double
        var u: Double

        do {
            do {
                // Generate standard normal (Box-Muller transform)
                val u1 = Random.nextDouble()
                val u2 = Random.nextDouble()
                x = sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
                v = (1.0 + c * x).pow(3.0)
            } while (v <= 0.0)

            u = Random.nextDouble()
        } while (u >= 1.0 - 0.0331 * x.pow(4.0) && ln(u) >= 0.5 * x * x + d * (1.0 - v + ln(v)))

        return d * v / beta
    }

    /**
     * Calculate confidence based on number of pulls
     */
    private fun calculateConfidence(pulls: Int): Float {
        return when {
            pulls >= 50 -> 0.95f   // High confidence
            pulls >= 20 -> 0.75f   // Medium confidence
            pulls >= 10 -> 0.50f   // Low confidence
            else -> 0.25f          // Very low confidence
        }
    }

    /**
     * Calculate uncertainty (standard deviation of Beta distribution)
     */
    private fun calculateUncertainty(alpha: Double, beta: Double): Double {
        val n = alpha + beta
        val mean = alpha / n
        val variance = (alpha * beta) / (n * n * (n + 1))
        return sqrt(variance)
    }


    /**
     * Load Thompson Sampling state from preferences
     */
    private fun loadState(): TSState {
        val json = preferences.getString(PREF_TS_STATE, "")
        if (json.isEmpty()) return TSState()

        return try {
            deserializeState(json)
        } catch (e: Exception) {
            TSState()  // Return default state on error
        }
    }

    /**
     * Save Thompson Sampling state to preferences
     */
    private fun saveState(state: TSState) {
        val json = serializeState(state)
        preferences.setString(PREF_TS_STATE, json)
    }

    /**
     * Serialize state to JSON-like string
     * Format: armId1:alpha1:beta1|armId2:alpha2:beta2|...
     */
    private fun serializeState(state: TSState): String {
        return state.arms.entries.joinToString("|") { (armId, armState) ->
            "$armId:${armState.alpha}:${armState.beta}"
        }
    }

    /**
     * Deserialize state from string
     */
    private fun deserializeState(json: String): TSState {
        val arms = mutableMapOf<String, ArmState>()

        json.split("|").forEach { entry ->
            if (entry.isNotEmpty()) {
                val parts = entry.split(":")
                if (parts.size == 3) {
                    val armId = parts[0]
                    val alpha = parts[1].toDoubleOrNull() ?: INITIAL_ALPHA
                    val beta = parts[2].toDoubleOrNull() ?: INITIAL_BETA
                    arms[armId] = ArmState(alpha, beta)
                }
            }
        }

        return TSState(arms)
    }
}

/**
 * Phase 4: Thompson Sampling state
 */
data class TSState(
    val arms: MutableMap<String, ArmState> = mutableMapOf()
)

/**
 * Phase 4: State of a single arm (Beta distribution parameters)
 */
data class ArmState(
    val alpha: Double,  // Successes + prior
    val beta: Double    // Failures + prior
)

/**
 * Phase 4: Arm selection result
 */
data class ArmSelection(
    val armId: String,
    val confidence: Float,  // 0.0 to 1.0
    val strategy: String,
    val sampledValue: Float = 0.5f,
    val totalPulls: Int = 0
)

/**
 * Phase 4: Arm statistics
 */
data class ArmStats(
    val armId: String,
    val totalPulls: Int,
    val estimatedSuccessRate: Float,  // Mean of Beta distribution
    val uncertainty: Float,             // Standard deviation
    val alpha: Double,
    val beta: Double
) {
    fun getConfidenceInterval(): Pair<Float, Float> {
        // Approximate 95% confidence interval
        val margin = 1.96f * uncertainty
        return Pair(
            (estimatedSuccessRate - margin).coerceIn(0f, 1f),
            (estimatedSuccessRate + margin).coerceIn(0f, 1f)
        )
    }
}
