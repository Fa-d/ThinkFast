package dev.sadakat.thinkfaster.data.mapper

import dev.sadakat.thinkfaster.data.local.database.entities.GoalEntity
import dev.sadakat.thinkfaster.domain.model.Goal

/**
 * Mapper functions to convert between GoalEntity and Goal
 */

/**
 * Convert GoalEntity to Goal (Entity -> Domain)
 */
fun GoalEntity.toDomain(): Goal {
    return Goal(
        targetApp = targetApp,
        dailyLimitMinutes = dailyLimitMinutes,
        startDate = startDate,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastUpdated = lastUpdated
    )
}

/**
 * Convert Goal to GoalEntity (Domain -> Entity)
 */
fun Goal.toEntity(): GoalEntity {
    return GoalEntity(
        targetApp = targetApp,
        dailyLimitMinutes = dailyLimitMinutes,
        startDate = startDate,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastUpdated = lastUpdated
    )
}

/**
 * Convert a list of GoalEntity to a list of Goal
 */
fun List<GoalEntity>.toDomain(): List<Goal> {
    return map { it.toDomain() }
}

/**
 * Convert a list of Goal to a list of GoalEntity
 */
fun List<Goal>.toEntity(): List<GoalEntity> {
    return map { it.toEntity() }
}
