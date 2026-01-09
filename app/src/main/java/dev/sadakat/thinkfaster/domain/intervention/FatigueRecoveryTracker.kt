package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.local.database.dao.InterventionResultDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2: Fatigue Recovery Tracker
 *
 * Monitors when users take breaks from tracked apps and automatically
 * reduces burden scores to give users credit for healthy behavior.
 *
 * Research shows that intervention fatigue can recover when users
 * demonstrate positive behavior change.
 */
@Singleton
class FatigueRecoveryTracker @Inject constructor(
    private val interventionResultDao: InterventionResultDao
) {

    /**
     * Check if user has taken a recovery break
     * @return Recovery credit to apply (0.0 to 1.0)
     */
    suspend fun calculateRecoveryCredit(): Float = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val last24Hours = now - (24 * 60 * 60 * 1000)
        val last7Days = now - (7 * 24 * 60 * 60 * 1000)

        // Get recent intervention count
        val interventionsLast24h = interventionResultDao.getResultsInRange(last24Hours, now).size
        val interventionsLast7Days = interventionResultDao.getResultsInRange(last7Days, now).size
        val avgDaily = interventionsLast7Days / 7.0f

        // Calculate recovery based on reduced intervention frequency
        when {
            // Significant break (< 3 interventions in 24h when average is 10+)
            interventionsLast24h < 3 && avgDaily >= 10f -> 0.3f

            // Moderate break (< 5 interventions in 24h when average is 8+)
            interventionsLast24h < 5 && avgDaily >= 8f -> 0.2f

            // Small break (intervention frequency below average)
            interventionsLast24h.toFloat() < avgDaily * 0.5f -> 0.1f

            // No recovery
            else -> 0.0f
        }
    }

    /**
     * Check if user deserves burden relief based on positive behavior
     */
    suspend fun shouldGrantBurdenRelief(): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val last7Days = now - (7 * 24 * 60 * 60 * 1000)

        // Check last 10 interventions
        val recentResults = interventionResultDao.getRecentResults(10)

        if (recentResults.size < 10) return@withContext false

        // Count positive responses (GO_BACK)
        val goBackCount = recentResults.count { it.userChoice == "GO_BACK" }
        val goBackRate = goBackCount / recentResults.size.toFloat()

        // Grant relief if user is consistently responding positively
        goBackRate >= 0.7f  // 70%+ positive response rate
    }

    /**
     * Calculate adjusted burden score with recovery credit
     */
    fun applyRecoveryCredit(
        originalBurdenScore: Int,
        recoveryCredit: Float
    ): Int {
        val reduction = (originalBurdenScore * recoveryCredit).toInt()
        return maxOf(0, originalBurdenScore - reduction)
    }
}
