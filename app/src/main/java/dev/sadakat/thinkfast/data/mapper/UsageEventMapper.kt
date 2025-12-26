package dev.sadakat.thinkfast.data.mapper

import dev.sadakat.thinkfast.data.local.database.entities.UsageEventEntity
import dev.sadakat.thinkfast.domain.model.UsageEvent

/**
 * Mapper functions to convert between UsageEventEntity and UsageEvent
 */

/**
 * Convert UsageEventEntity to UsageEvent (Entity -> Domain)
 */
fun UsageEventEntity.toDomain(): UsageEvent {
    return UsageEvent(
        id = id,
        sessionId = sessionId,
        eventType = eventType,
        timestamp = timestamp,
        metadata = metadata
    )
}

/**
 * Convert UsageEvent to UsageEventEntity (Domain -> Entity)
 */
fun UsageEvent.toEntity(): UsageEventEntity {
    return UsageEventEntity(
        id = id,
        sessionId = sessionId,
        eventType = eventType,
        timestamp = timestamp,
        metadata = metadata
    )
}

/**
 * Convert a list of UsageEventEntity to a list of UsageEvent
 */
fun List<UsageEventEntity>.toDomain(): List<UsageEvent> {
    return map { it.toDomain() }
}

/**
 * Convert a list of UsageEvent to a list of UsageEventEntity
 */
fun List<UsageEvent>.toEntity(): List<UsageEventEntity> {
    return map { it.toEntity() }
}
