package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 3: Timing Pattern Learner
 *
 * Continuously learns and updates timing effectiveness patterns as new
 * intervention results arrive. Works alongside ContextualTimingOptimizer
 * to provide real-time learning.
 *
 * Tracks:
 * - Per-hour success rates (rolling average)
 * - Time-window effectiveness trends
 * - Context-specific timing patterns
 *
 * Usage:
 * ```
 * // After intervention completes
 * learner.recordTimingOutcome(hour, wasSuccessful, targetApp)
 * ```
 */
@Singleton
class TimingPatternLearner @Inject constructor(
    private val preferences: InterventionPreferences
) {

    companion object {
        private const val PREF_TIMING_PATTERNS = "timing_patterns_v1"
        private const val LEARNING_RATE = 0.15f  // Weight for new observations
        private const val MIN_OBSERVATIONS = 3   // Minimum before trusting pattern
    }

    /**
     * Record the outcome of an intervention for timing learning
     */
    suspend fun recordTimingOutcome(
        hour: Int,
        wasSuccessful: Boolean,
        targetApp: String,
        isWeekend: Boolean
    ) = withContext(Dispatchers.IO) {
        val patterns = loadPatterns()

        // Update hourly pattern
        val hourKey = "hour_$hour"
        val currentPattern = patterns[hourKey] ?: TimingPattern()
        val updatedPattern = updatePattern(currentPattern, wasSuccessful)
        patterns[hourKey] = updatedPattern

        // Update app-specific pattern
        val appHourKey = "${targetApp}_hour_$hour"
        val appPattern = patterns[appHourKey] ?: TimingPattern()
        patterns[appHourKey] = updatePattern(appPattern, wasSuccessful)

        // Update weekend/weekday pattern
        val dayTypeKey = if (isWeekend) "weekend_hour_$hour" else "weekday_hour_$hour"
        val dayTypePattern = patterns[dayTypeKey] ?: TimingPattern()
        patterns[dayTypeKey] = updatePattern(dayTypePattern, wasSuccessful)

        savePatterns(patterns)
    }

    /**
     * Get learned timing pattern for a specific context
     */
    suspend fun getTimingPattern(
        hour: Int,
        targetApp: String? = null,
        isWeekend: Boolean? = null
    ): TimingPattern? = withContext(Dispatchers.IO) {
        val patterns = loadPatterns()

        // Try app-specific pattern first
        if (targetApp != null) {
            val appPattern = patterns["${targetApp}_hour_$hour"]
            if (appPattern != null && appPattern.observations >= MIN_OBSERVATIONS) {
                return@withContext appPattern
            }
        }

        // Try day-type specific pattern
        if (isWeekend != null) {
            val dayTypeKey = if (isWeekend) "weekend_hour_$hour" else "weekday_hour_$hour"
            val dayPattern = patterns[dayTypeKey]
            if (dayPattern != null && dayPattern.observations >= MIN_OBSERVATIONS) {
                return@withContext dayPattern
            }
        }

        // Fall back to general hourly pattern
        val hourPattern = patterns["hour_$hour"]
        if (hourPattern != null && hourPattern.observations >= MIN_OBSERVATIONS) {
            return@withContext hourPattern
        }

        null  // No reliable pattern yet
    }

    /**
     * Get timing effectiveness summary across all hours
     */
    suspend fun getEffectivenessSummary(): Map<Int, Float> = withContext(Dispatchers.IO) {
        val patterns = loadPatterns()
        val summary = mutableMapOf<Int, Float>()

        for (hour in 0..23) {
            val pattern = patterns["hour_$hour"]
            if (pattern != null && pattern.observations >= MIN_OBSERVATIONS) {
                summary[hour] = pattern.successRate
            }
        }

        summary
    }

    /**
     * Check if we have reliable timing data
     */
    suspend fun hasReliableData(): Boolean = withContext(Dispatchers.IO) {
        val patterns = loadPatterns()
        val hourlyPatterns = patterns.filterKeys { it.startsWith("hour_") }

        // Need at least 12 hours with reliable data
        hourlyPatterns.count { it.value.observations >= MIN_OBSERVATIONS } >= 12
    }

    /**
     * Clear all learned patterns (for testing or reset)
     */
    suspend fun clearPatterns() = withContext(Dispatchers.IO) {
        preferences.setString(PREF_TIMING_PATTERNS, "")
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Update timing pattern with new observation using exponential moving average
     */
    private fun updatePattern(
        pattern: TimingPattern,
        wasSuccessful: Boolean
    ): TimingPattern {
        val newObservation = if (wasSuccessful) 1.0f else 0.0f

        // Exponential moving average: new_rate = old_rate * (1 - α) + new_obs * α
        val updatedRate = if (pattern.observations == 0) {
            newObservation
        } else {
            pattern.successRate * (1 - LEARNING_RATE) + newObservation * LEARNING_RATE
        }

        return TimingPattern(
            successRate = updatedRate,
            observations = pattern.observations + 1,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Load timing patterns from preferences
     */
    private fun loadPatterns(): MutableMap<String, TimingPattern> {
        val json = preferences.getString(PREF_TIMING_PATTERNS, "")
        if (json.isEmpty()) return mutableMapOf()

        return try {
            deserializePatterns(json)
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    /**
     * Save timing patterns to preferences
     */
    private fun savePatterns(patterns: Map<String, TimingPattern>) {
        val json = serializePatterns(patterns)
        preferences.setString(PREF_TIMING_PATTERNS, json)
    }

    /**
     * Serialize patterns to JSON-like string format
     * Format: key1:rate1:obs1:time1|key2:rate2:obs2:time2|...
     */
    private fun serializePatterns(patterns: Map<String, TimingPattern>): String {
        return patterns.entries.joinToString("|") { (key, pattern) ->
            "$key:${pattern.successRate}:${pattern.observations}:${pattern.lastUpdated}"
        }
    }

    /**
     * Deserialize patterns from string format
     */
    private fun deserializePatterns(json: String): MutableMap<String, TimingPattern> {
        val patterns = mutableMapOf<String, TimingPattern>()

        json.split("|").forEach { entry ->
            if (entry.isNotEmpty()) {
                val parts = entry.split(":")
                if (parts.size == 4) {
                    val key = parts[0]
                    val rate = parts[1].toFloatOrNull() ?: 0.5f
                    val obs = parts[2].toIntOrNull() ?: 0
                    val time = parts[3].toLongOrNull() ?: 0L

                    patterns[key] = TimingPattern(rate, obs, time)
                }
            }
        }

        return patterns
    }
}

/**
 * Phase 3: Learned timing pattern
 */
data class TimingPattern(
    val successRate: Float = 0.5f,  // 0.0 to 1.0
    val observations: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val isReliable: Boolean
        get() = observations >= 3

    val effectivenessLevel: String
        get() = when {
            !isReliable -> "Insufficient Data"
            successRate >= 0.60f -> "High"
            successRate >= 0.40f -> "Moderate"
            else -> "Low"
        }
}
