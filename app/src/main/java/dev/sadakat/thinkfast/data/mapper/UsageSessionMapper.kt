package dev.sadakat.thinkfast.data.mapper

import dev.sadakat.thinkfast.data.local.database.entities.UsageSessionEntity
import dev.sadakat.thinkfast.domain.model.UsageSession

/**
 * Mapper functions to convert between UsageSessionEntity and UsageSession
 */

/**
 * Convert UsageSessionEntity to UsageSession (Entity -> Domain)
 */
fun UsageSessionEntity.toDomain(): UsageSession {
    return UsageSession(
        id = id,
        targetApp = targetApp,
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        duration = duration,
        wasInterrupted = wasInterrupted,
        interruptionType = interruptionType,
        date = date
    )
}

/**
 * Convert UsageSession to UsageSessionEntity (Domain -> Entity)
 */
fun UsageSession.toEntity(): UsageSessionEntity {
    return UsageSessionEntity(
        id = id,
        targetApp = targetApp,
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        duration = duration,
        wasInterrupted = wasInterrupted,
        interruptionType = interruptionType,
        date = date
    )
}

/**
 * Convert a list of UsageSessionEntity to a list of UsageSession
 */
fun List<UsageSessionEntity>.toDomain(): List<UsageSession> {
    return map { it.toDomain() }
}

/**
 * Convert a list of UsageSession to a list of UsageSessionEntity
 */
fun List<UsageSession>.toEntity(): List<UsageSessionEntity> {
    return map { it.toEntity() }
}
