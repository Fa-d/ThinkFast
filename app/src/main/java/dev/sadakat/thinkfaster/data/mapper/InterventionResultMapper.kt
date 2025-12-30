package dev.sadakat.thinkfaster.data.mapper

import dev.sadakat.thinkfaster.data.local.database.dao.ContentEffectivenessStats
import dev.sadakat.thinkfaster.data.local.database.entities.InterventionResultEntity
import dev.sadakat.thinkfaster.domain.model.InterventionResult
import dev.sadakat.thinkfaster.domain.model.InterventionType
import dev.sadakat.thinkfaster.domain.model.UserChoice

/**
 * Mapper functions to convert between InterventionResultEntity and InterventionResult
 * Phase G: Effectiveness tracking
 */

/**
 * Convert InterventionResultEntity to InterventionResult (Entity -> Domain)
 */
fun InterventionResultEntity.toDomain(): InterventionResult {
    return InterventionResult(
        id = id,
        sessionId = sessionId,
        targetApp = targetApp,
        interventionType = InterventionType.valueOf(interventionType),
        contentType = contentType,
        hourOfDay = hourOfDay,
        dayOfWeek = dayOfWeek,
        isWeekend = isWeekend,
        isLateNight = isLateNight,
        sessionCount = sessionCount,
        quickReopen = quickReopen,
        currentSessionDurationMs = currentSessionDurationMs,
        userChoice = UserChoice.valueOf(userChoice),
        timeToShowDecisionMs = timeToShowDecisionMs,
        finalSessionDurationMs = finalSessionDurationMs,
        sessionEndedNormally = sessionEndedNormally,
        timestamp = timestamp
    )
}

/**
 * Convert InterventionResult to InterventionResultEntity (Domain -> Entity)
 */
fun InterventionResult.toEntity(): InterventionResultEntity {
    return InterventionResultEntity(
        id = id,
        sessionId = sessionId,
        targetApp = targetApp,
        interventionType = interventionType.name,
        contentType = contentType,
        hourOfDay = hourOfDay,
        dayOfWeek = dayOfWeek,
        isWeekend = isWeekend,
        isLateNight = isLateNight,
        sessionCount = sessionCount,
        quickReopen = quickReopen,
        currentSessionDurationMs = currentSessionDurationMs,
        userChoice = userChoice.name,
        timeToShowDecisionMs = timeToShowDecisionMs,
        finalSessionDurationMs = finalSessionDurationMs,
        sessionEndedNormally = sessionEndedNormally,
        timestamp = timestamp
    )
}

/**
 * Convert a list of InterventionResultEntity to a list of InterventionResult
 */
fun List<InterventionResultEntity>.toDomain(): List<InterventionResult> {
    return map { it.toDomain() }
}

/**
 * Convert a list of InterventionResult to a list of InterventionResultEntity
 */
fun List<InterventionResult>.toEntity(): List<InterventionResultEntity> {
    return map { it.toEntity() }
}

/**
 * Convert DAO stats to domain model
 */
fun ContentEffectivenessStats.toDomainContentStats(): dev.sadakat.thinkfaster.domain.model.ContentEffectivenessStats {
    return dev.sadakat.thinkfaster.domain.model.ContentEffectivenessStats(
        contentType = contentType,
        total = total,
        goBackCount = goBackCount,
        avgDecisionTimeMs = avgDecisionTimeMs,
        avgFinalDurationMs = avgFinalDurationMs
    )
}

fun List<ContentEffectivenessStats>.toDomainContentStats(): List<dev.sadakat.thinkfaster.domain.model.ContentEffectivenessStats> {
    return map { it.toDomainContentStats() }
}
