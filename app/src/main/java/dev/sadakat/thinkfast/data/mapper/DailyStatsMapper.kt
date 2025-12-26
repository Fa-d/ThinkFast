package dev.sadakat.thinkfast.data.mapper

import dev.sadakat.thinkfast.data.local.database.entities.DailyStatsEntity
import dev.sadakat.thinkfast.domain.model.DailyStats

/**
 * Mapper functions to convert between DailyStatsEntity and DailyStats
 */

/**
 * Convert DailyStatsEntity to DailyStats (Entity -> Domain)
 */
fun DailyStatsEntity.toDomain(): DailyStats {
    return DailyStats(
        date = date,
        targetApp = targetApp,
        totalDuration = totalDuration,
        sessionCount = sessionCount,
        longestSession = longestSession,
        averageSession = averageSession,
        alertsShown = alertsShown,
        alertsProceeded = alertsProceeded,
        lastUpdated = lastUpdated
    )
}

/**
 * Convert DailyStats to DailyStatsEntity (Domain -> Entity)
 */
fun DailyStats.toEntity(): DailyStatsEntity {
    return DailyStatsEntity(
        date = date,
        targetApp = targetApp,
        totalDuration = totalDuration,
        sessionCount = sessionCount,
        longestSession = longestSession,
        averageSession = averageSession,
        alertsShown = alertsShown,
        alertsProceeded = alertsProceeded,
        lastUpdated = lastUpdated
    )
}

/**
 * Convert a list of DailyStatsEntity to a list of DailyStats
 */
fun List<DailyStatsEntity>.toDomain(): List<DailyStats> {
    return map { it.toDomain() }
}

/**
 * Convert a list of DailyStats to a list of DailyStatsEntity
 */
fun List<DailyStats>.toEntity(): List<DailyStatsEntity> {
    return map { it.toEntity() }
}
