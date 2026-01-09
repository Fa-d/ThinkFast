package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.local.database.dao.DecisionExplanationDao
import dev.sadakat.thinkfaster.data.local.database.entities.DecisionExplanationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 1: Decision Logger
 *
 * Records complete rationale for every intervention decision (SHOW or SKIP).
 * Provides transparency and enables optimization of decision rules.
 *
 * Usage:
 * ```
 * // When intervention is shown
 * decisionLogger.logDecision(explanation)
 *
 * // When intervention is skipped
 * decisionLogger.logSkippedDecision(explanation)
 * ```
 */
@Singleton
class DecisionLogger @Inject constructor(
    private val decisionExplanationDao: DecisionExplanationDao
) {

    /**
     * Log an intervention decision (SHOW or SKIP)
     */
    suspend fun logDecision(explanation: InterventionDecisionExplanation): Long = withContext(Dispatchers.IO) {
        val entity = explanation.toEntity()
        decisionExplanationDao.insert(entity)
    }

    /**
     * Log a SHOW decision with intervention ID
     */
    suspend fun logShowDecision(
        explanation: InterventionDecisionExplanation,
        interventionId: Long
    ): Long = withContext(Dispatchers.IO) {
        val entity = explanation.toEntity().copy(
            decision = "SHOW",
            interventionId = interventionId
        )
        decisionExplanationDao.insert(entity)
    }

    /**
     * Log a SKIP decision
     */
    suspend fun logSkipDecision(
        explanation: InterventionDecisionExplanation
    ): Long = withContext(Dispatchers.IO) {
        val entity = explanation.toEntity().copy(
            decision = "SKIP"
        )
        decisionExplanationDao.insert(entity)
    }

    /**
     * Get recent decisions for analysis
     */
    suspend fun getRecentDecisions(limit: Int = 100): List<DecisionExplanationEntity> = withContext(Dispatchers.IO) {
        decisionExplanationDao.getRecentDecisions(limit)
    }

    /**
     * Get skip statistics (why are interventions being skipped?)
     */
    suspend fun getSkipStatistics(daysBack: Int = 7): Map<String, Int> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000)
        val stats = decisionExplanationDao.getSkipReasonStats(startTime)

        stats.associate { (it.blocking_reason ?: "UNKNOWN") to it.count }
    }

    /**
     * Get decision breakdown by opportunity level
     */
    suspend fun getDecisionsByOpportunity(daysBack: Int = 7): Map<String, Map<String, Int>> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000)
        val stats = decisionExplanationDao.getDecisionsByOpportunityLevel(startTime)

        // Group by opportunity level, then by decision
        stats.groupBy { it.opportunity_level }
            .mapValues { (_, items) ->
                items.associate { it.decision to it.count }
            }
    }

    /**
     * Check if burden mitigation is being used frequently
     * (Indicates users are experiencing intervention fatigue)
     */
    suspend fun isBurdenMitigationActive(daysBack: Int = 1): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000)
        val decisionsWithMitigation = decisionExplanationDao.getDecisionsWithBurdenMitigation(startTime)

        // Consider active if >20% of recent decisions had burden mitigation
        val recentDecisions = decisionExplanationDao.getDecisionsInRange(startTime, System.currentTimeMillis())
        if (recentDecisions.isEmpty()) return@withContext false

        val mitigationRate = decisionsWithMitigation.size.toFloat() / recentDecisions.size
        mitigationRate > 0.20
    }

    /**
     * Get summary of decision patterns for debugging
     */
    suspend fun getDecisionSummary(daysBack: Int = 7): DecisionSummary = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000)
        val decisions = decisionExplanationDao.getDecisionsInRange(startTime, System.currentTimeMillis())

        val showCount = decisions.count { it.decision == "SHOW" }
        val skipCount = decisions.count { it.decision == "SKIP" }

        val skipReasons = decisions
            .filter { it.decision == "SKIP" }
            .groupBy { it.blockingReason }
            .mapValues { it.value.size }

        val avgOpportunityScore = decisions
            .map { it.opportunityScore }
            .average()

        val burdenMitigationCount = decisions.count { it.burdenMitigationApplied }

        DecisionSummary(
            totalDecisions = decisions.size,
            showCount = showCount,
            skipCount = skipCount,
            skipReasons = skipReasons,
            avgOpportunityScore = avgOpportunityScore,
            burdenMitigationCount = burdenMitigationCount,
            daysAnalyzed = daysBack
        )
    }

    /**
     * Clean up old decision logs (keep last 90 days)
     */
    suspend fun cleanupOldDecisions(daysToKeep: Int = 90): Int = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000)
        decisionExplanationDao.deleteOlderThan(cutoffTime)
    }
}

/**
 * Summary of decision patterns
 */
data class DecisionSummary(
    val totalDecisions: Int,
    val showCount: Int,
    val skipCount: Int,
    val skipReasons: Map<String?, Int>,
    val avgOpportunityScore: Double,
    val burdenMitigationCount: Int,
    val daysAnalyzed: Int
) {
    val showRate: Double
        get() = if (totalDecisions > 0) showCount.toDouble() / totalDecisions else 0.0

    val skipRate: Double
        get() = if (totalDecisions > 0) skipCount.toDouble() / totalDecisions else 0.0

    val burdenMitigationRate: Double
        get() = if (totalDecisions > 0) burdenMitigationCount.toDouble() / totalDecisions else 0.0

    override fun toString(): String {
        return buildString {
            appendLine("=== Decision Summary (Last $daysAnalyzed days) ===")
            appendLine("Total Decisions: $totalDecisions")
            appendLine("  - SHOW: $showCount (${(showRate * 100).toInt()}%)")
            appendLine("  - SKIP: $skipCount (${(skipRate * 100).toInt()}%)")
            appendLine()
            appendLine("Skip Reasons:")
            skipReasons.forEach { (reason, count) ->
                val percentage = if (skipCount > 0) (count.toDouble() / skipCount * 100).toInt() else 0
                appendLine("  - ${reason ?: "UNKNOWN"}: $count ($percentage%)")
            }
            appendLine()
            appendLine("Avg Opportunity Score: ${avgOpportunityScore.toInt()}")
            appendLine("Burden Mitigation: $burdenMitigationCount decisions (${(burdenMitigationRate * 100).toInt()}%)")
        }
    }
}

/**
 * Extension to convert InterventionDecisionExplanation to Entity
 */
private fun InterventionDecisionExplanation.toEntity(): DecisionExplanationEntity {
    return DecisionExplanationEntity(
        timestamp = timestamp,
        targetApp = targetApp,
        decision = decision.name,
        blockingReason = blockingReason?.name,
        interventionId = null,  // Set separately if SHOW
        opportunityScore = opportunityScore,
        opportunityLevel = opportunityLevel.name,
        opportunityBreakdown = opportunityBreakdown.toJson(),
        personaDetected = personaDetected.name,
        personaConfidence = personaConfidence.name,
        passedBasicRateLimit = passedBasicRateLimit,
        timeSinceLastIntervention = timeSinceLastIntervention,
        passedPersonaFrequency = passedPersonaFrequency,
        personaFrequencyRule = personaFrequencyRule,
        passedJitaiFilter = passedJitaiFilter,
        jitaiDecision = jitaiDecision,
        burdenLevel = burdenLevel?.name,
        burdenScore = burdenScore,
        burdenMitigationApplied = burdenMitigationApplied,
        burdenCooldownMultiplier = burdenCooldownMultiplier,
        contentTypeSelected = contentTypeSelected?.name,
        contentWeights = contentWeights?.toJson(),
        contentSelectionReason = contentSelectionReason,
        rlPredictedReward = rlPredictedReward,
        rlExplorationVsExploitation = rlExplorationVsExploitation,
        contextSnapshot = contextSnapshot.toJson(),
        explanation = explanation,
        detailedExplanation = detailedExplanation
    )
}

/**
 * Helper to convert map to JSON string
 */
private fun Map<String, Any>.toJson(): String {
    val json = JSONObject()
    forEach { (key, value) ->
        json.put(key, value)
    }
    return json.toString()
}
