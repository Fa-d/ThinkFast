package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2: Burden Trend Monitor
 *
 * Tracks burden score changes over time to detect:
 * - Escalating fatigue (increasing burden)
 * - Recovery progress (decreasing burden)
 * - Stable burden levels
 *
 * Used for proactive warnings and intervention adjustments.
 */
@Singleton
class BurdenTrendMonitor @Inject constructor(
    private val preferences: InterventionPreferences
) {

    companion object {
        private const val PREF_BURDEN_HISTORY = "burden_score_history"
        private const val HISTORY_SIZE = 7 // Track last 7 burden scores
    }

    data class BurdenTrend(
        val currentScore: Int,
        val previousScore: Int,
        val trend: TrendDirection,
        val changePercentage: Float,
        val isEscalating: Boolean  // Rapid increase
    )

    enum class TrendDirection {
        INCREASING,    // Burden going up
        STABLE,        // Burden steady
        DECREASING     // Burden going down
    }

    /**
     * Record current burden score to history
     */
    suspend fun recordBurdenScore(score: Int) = withContext(Dispatchers.IO) {
        val history = getBurdenHistory().toMutableList()
        history.add(score)

        // Keep only last N scores
        if (history.size > HISTORY_SIZE) {
            history.removeAt(0)
        }

        saveBurdenHistory(history)
    }

    /**
     * Analyze burden trend
     */
    suspend fun analyzeTrend(currentScore: Int): BurdenTrend = withContext(Dispatchers.IO) {
        val history = getBurdenHistory()

        if (history.isEmpty()) {
            return@withContext BurdenTrend(
                currentScore = currentScore,
                previousScore = currentScore,
                trend = TrendDirection.STABLE,
                changePercentage = 0.0f,
                isEscalating = false
            )
        }

        val previousScore = history.last()
        val change = currentScore - previousScore
        val changePercentage = if (previousScore > 0) {
            (change / previousScore.toFloat()) * 100f
        } else {
            0.0f
        }

        val trend = when {
            change > 2 -> TrendDirection.INCREASING
            change < -2 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }

        // Detect escalation: 3+ consecutive increases
        val isEscalating = if (history.size >= 3) {
            val recent = history.takeLast(3)
            recent.zipWithNext().all { (a, b) -> b > a }
        } else {
            false
        }

        BurdenTrend(
            currentScore = currentScore,
            previousScore = previousScore,
            trend = trend,
            changePercentage = changePercentage,
            isEscalating = isEscalating
        )
    }

    /**
     * Check if user should receive burden warning
     */
    suspend fun shouldShowBurdenWarning(trend: BurdenTrend): Boolean = withContext(Dispatchers.IO) {
        // Show warning if burden is escalating
        if (trend.isEscalating && trend.currentScore >= 10) {
            return@withContext true
        }

        // Show warning if burden jumped significantly
        if (trend.changePercentage > 50f && trend.currentScore >= 8) {
            return@withContext true
        }

        false
    }

    private fun getBurdenHistory(): List<Int> {
        val historyString = preferences.getString(PREF_BURDEN_HISTORY, "")
        if (historyString.isEmpty()) return emptyList()

        return historyString.split(",")
            .mapNotNull { it.toIntOrNull() }
    }

    private fun saveBurdenHistory(history: List<Int>) {
        val historyString = history.joinToString(",")
        preferences.setString(PREF_BURDEN_HISTORY, historyString)
    }
}
