package dev.sadakat.thinkfaster.data.mapper

import dev.sadakat.thinkfaster.data.local.database.entities.StreakRecoveryEntity
import dev.sadakat.thinkfaster.domain.model.StreakRecovery

/**
 * Convert StreakRecoveryEntity to StreakRecovery domain model
 */
fun StreakRecoveryEntity.toDomain(): StreakRecovery {
    return StreakRecovery(
        targetApp = targetApp,
        previousStreak = previousStreak,
        recoveryStartDate = recoveryStartDate,
        currentRecoveryDays = currentRecoveryDays,
        isRecoveryComplete = isRecoveryComplete,
        recoveryCompletedDate = recoveryCompletedDate,
        notificationShown = notificationShown,
        timestamp = timestamp
    )
}

/**
 * Convert StreakRecovery domain model to StreakRecoveryEntity
 */
fun StreakRecovery.toEntity(): StreakRecoveryEntity {
    return StreakRecoveryEntity(
        targetApp = targetApp,
        previousStreak = previousStreak,
        recoveryStartDate = recoveryStartDate,
        currentRecoveryDays = currentRecoveryDays,
        isRecoveryComplete = isRecoveryComplete,
        recoveryCompletedDate = recoveryCompletedDate,
        notificationShown = notificationShown,
        timestamp = timestamp
    )
}
