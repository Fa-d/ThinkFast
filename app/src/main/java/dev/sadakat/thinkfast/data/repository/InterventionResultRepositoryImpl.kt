package dev.sadakat.thinkfast.data.repository

import dev.sadakat.thinkfast.data.local.database.dao.AppStats
import dev.sadakat.thinkfast.data.local.database.dao.InterventionResultDao
import dev.sadakat.thinkfast.data.mapper.toDomain
import dev.sadakat.thinkfast.data.mapper.toDomainContentStats
import dev.sadakat.thinkfast.data.mapper.toEntity
import dev.sadakat.thinkfast.domain.model.ContentEffectivenessStats
import dev.sadakat.thinkfast.domain.model.InterventionResult
import dev.sadakat.thinkfast.domain.model.InterventionType
import dev.sadakat.thinkfast.domain.model.OverallAnalytics
import dev.sadakat.thinkfast.domain.model.UserChoice
import dev.sadakat.thinkfast.domain.repository.AppInterventionStats
import dev.sadakat.thinkfast.domain.repository.InterventionResultRepository

/**
 * Implementation of InterventionResultRepository using Room database
 * Phase G: Effectiveness tracking
 */
class InterventionResultRepositoryImpl(
    private val resultDao: InterventionResultDao
) : InterventionResultRepository {

    override suspend fun recordResult(result: InterventionResult): Long {
        return resultDao.insertResult(result.toEntity())
    }

    override suspend fun updateSessionOutcome(
        sessionId: Long,
        finalDurationMs: Long,
        endedNormally: Boolean
    ) {
        resultDao.updateSessionOutcome(sessionId, finalDurationMs, endedNormally)
    }

    override suspend fun getResultBySession(sessionId: Long): InterventionResult? {
        return resultDao.getResultBySessionId(sessionId)?.toDomain()
    }

    override suspend fun getRecentResultsForApp(
        targetApp: String,
        limit: Int
    ): List<InterventionResult> {
        return resultDao.getRecentResultsForApp(targetApp).take(limit).toDomain()
    }

    override suspend fun getRecentResults(limit: Int): List<InterventionResult> {
        return resultDao.getRecentResults(limit).toDomain()
    }

    override suspend fun getEffectivenessByContentType(): List<ContentEffectivenessStats> {
        return resultDao.getEffectivenessByContentType().toDomainContentStats()
    }

    override suspend fun getOverallAnalytics(): OverallAnalytics {
        val allStats = resultDao.getUserChoiceBreakdown()
        val total = allStats.sumOf { it.count }
        val goBackCount = allStats.find { it.userChoice == "GO_BACK" }?.count ?: 0
        val proceedCount = allStats.find { it.userChoice == "PROCEED" }?.count ?: 0
        val dismissalRate = resultDao.getOverallDismissalRate() ?: 0.0
        val avgDecisionTime = resultDao.getAverageDecisionTime()?.toLong()

        // Get average session duration after intervention
        val contentStats = resultDao.getEffectivenessByContentType()
        val avgSessionMs = contentStats.mapNotNull { it.avgFinalDurationMs }.average().takeIf { it.isNaN().not() }

        return OverallAnalytics(
            totalInterventions = total,
            proceedCount = proceedCount,
            goBackCount = goBackCount,
            dismissalRate = dismissalRate,
            avgDecisionTimeMs = avgDecisionTime,
            avgSessionAfterInterventionMs = avgSessionMs
        )
    }

    override suspend fun getDismissalRateForContentType(contentType: String): Double {
        val stats = resultDao.getEffectivenessByContentType()
        return stats.find { it.contentType == contentType }?.dismissalRate ?: 0.0
    }

    override suspend fun getOverallDismissalRate(): Double {
        return resultDao.getOverallDismissalRate() ?: 0.0
    }

    override suspend fun getAverageDecisionTime(): Long {
        return resultDao.getAverageDecisionTime() ?: 0L
    }

    override suspend fun getMostEffectiveContentTypes(limit: Int): List<ContentEffectivenessStats> {
        return resultDao.getEffectivenessByContentType()
            .toDomainContentStats()
            .sortedByDescending { it.dismissalRate }
            .take(limit)
    }

    override suspend fun getStatsByApp(): Map<String, AppInterventionStats> {
        return resultDao.getStatsByApp().associate { appStats ->
            appStats.targetApp to AppInterventionStats(
                targetApp = appStats.targetApp,
                totalInterventions = appStats.interventions,
                goBackCount = appStats.goBackCount,
                dismissalRate = appStats.dismissalRate
            )
        }
    }

    override suspend fun deleteResultsOlderThan(timestampMs: Long): Int {
        return resultDao.deleteResultsOlderThan(timestampMs)
    }

    override suspend fun getTotalResultCount(): Int {
        return resultDao.getTotalResultCount()
    }
}
