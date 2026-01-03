package dev.sadakat.thinkfaster.data.sync.supabase

import dev.sadakat.thinkfaster.data.local.database.entities.*

/**
 * Extension functions to convert between Room entities and Supabase models
 * Phase 7: Supabase Backend Implementation
 */

// ========== GoalEntity ==========

fun GoalEntity.toSupabase(userId: String): SupabaseGoal {
    return SupabaseGoal(
        id = cloudId,  // UUID from Supabase, null for new records
        userId = userId,
        targetApp = targetApp,
        dailyLimitMinutes = dailyLimitMinutes,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        startDate = startDate,
        lastUpdated = lastUpdated,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

fun SupabaseGoal.toEntity(): GoalEntity {
    return GoalEntity(
        targetApp = targetApp,
        dailyLimitMinutes = dailyLimitMinutes,
        startDate = startDate,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastUpdated = lastUpdated,
        userId = userId,
        syncStatus = "SYNCED",  // Mark as synced when coming from cloud
        lastModified = lastModified,
        cloudId = id
    )
}

// ========== UsageSessionEntity ==========

fun UsageSessionEntity.toSupabase(userId: String): SupabaseUsageSession {
    return SupabaseUsageSession(
        id = cloudId,
        userId = userId,
        targetApp = targetApp,
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp ?: startTimestamp + duration,  // Use calculated if null
        duration = duration,
        date = date,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

fun SupabaseUsageSession.toEntity(localId: Long = 0): UsageSessionEntity {
    return UsageSessionEntity(
        id = localId,  // Room auto-generates, keep existing if updating
        targetApp = targetApp,
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        duration = duration,
        date = date,
        userId = userId,
        syncStatus = "SYNCED",
        lastModified = lastModified,
        cloudId = id
    )
}

// ========== DailyStatsEntity ==========

fun DailyStatsEntity.toSupabase(userId: String): SupabaseDailyStats {
    return SupabaseDailyStats(
        id = cloudId,
        userId = userId,
        targetApp = targetApp,
        date = date,
        totalDuration = totalDuration,
        sessionCount = sessionCount,
        alertsShown = alertsShown,
        longestSession = longestSession,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

fun SupabaseDailyStats.toEntity(): DailyStatsEntity {
    return DailyStatsEntity(
        date = date,
        targetApp = targetApp,
        totalDuration = totalDuration,
        sessionCount = sessionCount,
        longestSession = longestSession,
        averageSession = if (sessionCount > 0) totalDuration / sessionCount else 0,
        alertsShown = alertsShown,
        alertsProceeded = 0,  // Not synced - local only
        lastUpdated = lastModified,
        userId = userId,
        syncStatus = "SYNCED",
        lastModified = lastModified,
        cloudId = id
    )
}

// ========== UsageEventEntity ==========

fun UsageEventEntity.toSupabase(userId: String): SupabaseUsageEvent {
    return SupabaseUsageEvent(
        id = cloudId,
        userId = userId,
        sessionId = sessionId,
        timestamp = timestamp,
        eventType = eventType,
        metadata = metadata,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

fun SupabaseUsageEvent.toEntity(localId: Long = 0): UsageEventEntity {
    return UsageEventEntity(
        id = localId,
        sessionId = sessionId,
        eventType = eventType,
        timestamp = timestamp,
        metadata = metadata,
        userId = userId,
        syncStatus = "SYNCED",
        lastModified = lastModified,
        cloudId = id
    )
}

// ========== InterventionResultEntity ==========

fun InterventionResultEntity.toSupabase(userId: String): SupabaseInterventionResult {
    // Serialize extended context as JSON for detailed analytics
    val contextJson = buildString {
        append("{")
        append("\"contentType\":\"$contentType\",")
        append("\"hourOfDay\":$hourOfDay,")
        append("\"dayOfWeek\":$dayOfWeek,")
        append("\"isWeekend\":$isWeekend,")
        append("\"isLateNight\":$isLateNight,")
        append("\"sessionCount\":$sessionCount,")
        append("\"quickReopen\":$quickReopen,")
        append("\"currentSessionDurationMs\":$currentSessionDurationMs,")
        append("\"timeToShowDecisionMs\":$timeToShowDecisionMs,")
        append("\"userFeedback\":\"$userFeedback\",")
        append("\"audioActive\":$audioActive,")
        append("\"wasSnoozed\":$wasSnoozed,")
        append("\"finalSessionDurationMs\":${finalSessionDurationMs ?: "null"},")
        append("\"sessionEndedNormally\":${sessionEndedNormally ?: "null"}")
        append("}")
    }

    return SupabaseInterventionResult(
        id = cloudId,
        userId = userId,
        sessionId = sessionId,
        targetApp = targetApp,
        interventionType = interventionType,
        frictionLevel = contentType,  // Map contentType to frictionLevel
        userChoice = userChoice,
        timestamp = timestamp,
        delayDuration = timeToShowDecisionMs,
        snoozeMinutes = if (wasSnoozed && snoozeDurationMs != null) (snoozeDurationMs / 60000).toInt() else null,
        context = contextJson,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

fun SupabaseInterventionResult.toEntity(localId: Long = 0): InterventionResultEntity {
    // Parse context JSON to restore detailed fields
    // For simplicity, using defaults if context is null
    val contextMap = try {
        context?.let { parseSimpleJson(it) } ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }

    return InterventionResultEntity(
        id = localId,
        sessionId = sessionId,
        targetApp = targetApp,
        interventionType = interventionType,
        contentType = frictionLevel,
        hourOfDay = contextMap["hourOfDay"]?.toIntOrNull() ?: 0,
        dayOfWeek = contextMap["dayOfWeek"]?.toIntOrNull() ?: 1,
        isWeekend = contextMap["isWeekend"]?.toBoolean() ?: false,
        isLateNight = contextMap["isLateNight"]?.toBoolean() ?: false,
        sessionCount = contextMap["sessionCount"]?.toIntOrNull() ?: 0,
        quickReopen = contextMap["quickReopen"]?.toBoolean() ?: false,
        currentSessionDurationMs = contextMap["currentSessionDurationMs"]?.toLongOrNull() ?: 0,
        userChoice = userChoice,
        timeToShowDecisionMs = delayDuration ?: 0,
        userFeedback = contextMap["userFeedback"] ?: "NONE",
        feedbackTimestamp = null,
        audioActive = contextMap["audioActive"]?.toBoolean() ?: false,
        wasSnoozed = snoozeMinutes != null && snoozeMinutes > 0,
        snoozeDurationMs = snoozeMinutes?.let { it * 60000L },
        finalSessionDurationMs = contextMap["finalSessionDurationMs"]?.toLongOrNull(),
        sessionEndedNormally = contextMap["sessionEndedNormally"]?.toBoolean(),
        timestamp = timestamp,
        userId = userId,
        syncStatus = "SYNCED",
        lastModified = lastModified,
        cloudId = id
    )
}

// ========== StreakRecoveryEntity ==========

fun StreakRecoveryEntity.toSupabase(userId: String): SupabaseStreakRecovery {
    return SupabaseStreakRecovery(
        id = cloudId,
        userId = userId,
        targetApp = targetApp,
        date = recoveryStartDate,
        activationTimestamp = timestamp,
        expirationTimestamp = if (isRecoveryComplete && recoveryCompletedDate != null) {
            // Parse date and calculate expiration
            timestamp + (currentRecoveryDays * 86400000L)
        } else {
            timestamp + (7 * 86400000L)  // Default 7 days
        },
        isActive = !isRecoveryComplete,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

fun SupabaseStreakRecovery.toEntity(): StreakRecoveryEntity {
    val recoveryDays = ((expirationTimestamp - activationTimestamp) / 86400000L).toInt()

    return StreakRecoveryEntity(
        targetApp = targetApp,
        previousStreak = 0,  // Not stored in Supabase model - local only
        recoveryStartDate = date,
        currentRecoveryDays = if (isActive) recoveryDays else 0,
        isRecoveryComplete = !isActive,
        recoveryCompletedDate = if (!isActive) date else null,
        notificationShown = true,  // Assume shown if synced
        timestamp = activationTimestamp,
        userId = userId,
        syncStatus = "SYNCED",
        lastModified = lastModified,
        cloudId = id
    )
}

// ========== UserBaselineEntity ==========

fun UserBaselineEntity.toSupabase(userId: String): SupabaseUserBaseline {
    return SupabaseUserBaseline(
        id = cloudId,
        userId = userId,
        targetApp = "all",  // Baseline is for all apps
        firstWeekStartDate = firstWeekStartDate,
        firstWeekEndDate = firstWeekEndDate,
        totalSessions = 0,  // Not tracked in entity - would need to calculate
        totalDuration = totalUsageMinutes * 60000L,  // Convert minutes to milliseconds
        averageDailyMinutes = averageDailyMinutes,
        peakUsageHour = 12,  // Not tracked in entity - default to noon
        isCalculated = true,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

fun SupabaseUserBaseline.toEntity(): UserBaselineEntity {
    return UserBaselineEntity(
        id = 1,  // Single baseline per user
        firstWeekStartDate = firstWeekStartDate,
        firstWeekEndDate = firstWeekEndDate,
        totalUsageMinutes = (totalDuration / 60000).toInt(),  // Convert milliseconds to minutes
        averageDailyMinutes = averageDailyMinutes,
        facebookAverageMinutes = 0,  // Not synced - local calculation
        instagramAverageMinutes = 0,  // Not synced - local calculation
        calculatedDate = firstWeekEndDate,
        timestamp = lastModified,
        userId = userId,
        syncStatus = "SYNCED",
        lastModified = lastModified,
        cloudId = id
    )
}

// ========== SettingsEntity (JSON Map) ==========

/**
 * Settings are synced as a JSON blob, no conversion needed
 */
fun Map<String, Any>.toSupabaseSettings(userId: String): SupabaseSettings {
    val settingsJson = kotlinx.serialization.json.Json.encodeToString(
        kotlinx.serialization.serializer(),
        this
    )
    return SupabaseSettings(
        userId = userId,
        settingsJson = settingsJson,
        lastModified = System.currentTimeMillis()
    )
}

// ========== Helper Functions ==========

/**
 * Simple JSON parser for context field (basic key-value extraction)
 */
private fun parseSimpleJson(json: String): Map<String, String> {
    val map = mutableMapOf<String, String>()

    // Remove braces and split by comma
    val content = json.trim().removeSurrounding("{", "}")
    val pairs = content.split(",")

    pairs.forEach { pair ->
        val keyValue = pair.split(":")
        if (keyValue.size == 2) {
            val key = keyValue[0].trim().removeSurrounding("\"")
            val value = keyValue[1].trim().removeSurrounding("\"")
            map[key] = value
        }
    }

    return map
}
