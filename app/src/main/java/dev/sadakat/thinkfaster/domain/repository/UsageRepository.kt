package dev.sadakat.thinkfaster.domain.repository

import dev.sadakat.thinkfaster.domain.model.UsageEvent
import dev.sadakat.thinkfaster.domain.model.UsageSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing usage sessions and events
 */
interface UsageRepository {

    /**
     * Insert a new usage session
     * @return the ID of the inserted session
     */
    suspend fun insertSession(session: UsageSession): Long

    /**
     * Update an existing usage session
     */
    suspend fun updateSession(session: UsageSession)

    /**
     * Get the currently active session (endTimestamp is null)
     */
    suspend fun getActiveSession(): UsageSession?

    /**
     * End the active session by setting its end timestamp
     * @param sessionId the ID of the session to end
     * @param endTimestamp the timestamp when the session ended
     * @param wasInterrupted whether the session was interrupted by an alert
     * @param interruptionType the type of interruption (if any)
     */
    suspend fun endSession(
        sessionId: Long,
        endTimestamp: Long,
        wasInterrupted: Boolean = false,
        interruptionType: String? = null
    )

    /**
     * Get all sessions for a specific date
     * @param date the date in YYYY-MM-DD format
     */
    suspend fun getSessionsByDate(date: String): List<UsageSession>

    /**
     * Observe sessions for a specific date (real-time updates)
     * @param date the date in YYYY-MM-DD format
     */
    fun observeSessionsByDate(date: String): Flow<List<UsageSession>>

    /**
     * Get all sessions within a date range
     * @param startDate the start date in YYYY-MM-DD format
     * @param endDate the end date in YYYY-MM-DD format
     */
    suspend fun getSessionsInRange(startDate: String, endDate: String): List<UsageSession>

    /**
     * Get all sessions for a specific target app within a date range
     * @param targetApp the package name of the target app
     * @param startDate the start date in YYYY-MM-DD format
     * @param endDate the end date in YYYY-MM-DD format
     */
    suspend fun getSessionsByAppInRange(
        targetApp: String,
        startDate: String,
        endDate: String
    ): List<UsageSession>

    /**
     * Get the longest session in a date range
     * @param targetApp the package name of the target app
     * @param startDate the start date in YYYY-MM-DD format
     * @param endDate the end date in YYYY-MM-DD format
     */
    suspend fun getLongestSessionInRange(
        targetApp: String,
        startDate: String,
        endDate: String
    ): UsageSession?

    /**
     * Delete sessions older than a specific date
     * @param beforeDate the date before which sessions should be deleted (YYYY-MM-DD)
     */
    suspend fun deleteSessionsOlderThan(beforeDate: String)

    // ========== Usage Events ==========

    /**
     * Insert a new usage event
     */
    suspend fun insertEvent(event: UsageEvent)

    /**
     * Get all events for a specific session
     * @param sessionId the ID of the session
     */
    suspend fun getEventsBySession(sessionId: Long): List<UsageEvent>

    /**
     * Get events within a date range
     * @param startDate the start date in YYYY-MM-DD format
     * @param endDate the end date in YYYY-MM-DD format
     */
    suspend fun getEventsInRange(startDate: String, endDate: String): List<UsageEvent>

    /**
     * Count events of a specific type within a date range
     * @param eventType the type of event to count
     * @param startDate the start date in YYYY-MM-DD format
     * @param endDate the end date in YYYY-MM-DD format
     */
    suspend fun countEventsByTypeInRange(
        eventType: String,
        startDate: String,
        endDate: String
    ): Int

    // ========== Context-Aware Intervention Methods ==========

    /**
     * Get total usage time for a specific app today (in milliseconds)
     */
    suspend fun getTodayUsageForApp(packageName: String): Long

    /**
     * Get total usage time for a specific app yesterday (in milliseconds)
     */
    suspend fun getYesterdayUsageForApp(packageName: String): Long

    /**
     * Get weekly average usage time for a specific app (in milliseconds)
     */
    suspend fun getWeeklyAverageForApp(packageName: String): Long

    /**
     * Get the number of sessions today for a specific app
     */
    suspend fun getTodaySessionCount(packageName: String): Int

    /**
     * Get the end time of the last session for a specific app (timestamp)
     */
    suspend fun getLastSessionEndTime(packageName: String): Long

    /**
     * Get the current session duration for a specific session ID (in milliseconds)
     */
    suspend fun getCurrentSessionDuration(sessionId: Long): Long

    /**
     * Get the daily goal for a specific app (in minutes, null if not set)
     */
    suspend fun getDailyGoalForApp(packageName: String): Int?

    /**
     * Get the current streak of days under goal
     */
    suspend fun getCurrentStreak(): Int

    /**
     * Get the app installation date (timestamp)
     */
    suspend fun getInstallDate(): Long

    /**
     * Get the shortest (best) session duration for a specific app today (in minutes)
     */
    suspend fun getBestSessionMinutes(packageName: String): Int

    // ========== Phase F: Progressive Friction ==========

    /**
     * Get the effective friction level considering user preferences
     * Returns the user's override if set, otherwise calculates from install date
     */
    suspend fun getEffectiveFrictionLevel(): dev.sadakat.thinkfaster.domain.intervention.FrictionLevel

    /**
     * Set a friction level override
     * Pass null to clear the override and use automatic calculation
     */
    suspend fun setFrictionLevelOverride(level: dev.sadakat.thinkfaster.domain.intervention.FrictionLevel?)

    /**
     * Get the user's friction level override
     * Returns null if using automatic calculation
     */
    suspend fun getFrictionLevelOverride(): dev.sadakat.thinkfaster.domain.intervention.FrictionLevel?

    // ========== Phase 2: Behavioral Pattern Queries ==========

    /**
     * Get sessions that occurred during late night hours (22:00 - 05:00)
     */
    suspend fun getLateNightSessions(startDate: String, endDate: String): List<UsageSession>

    /**
     * Get sessions by day of week (0=Sunday, 1=Monday, etc.)
     * Useful for weekend vs weekday analysis
     */
    suspend fun getSessionsByDayOfWeek(
        startDate: String,
        endDate: String,
        daysOfWeek: List<Int>
    ): List<UsageSession>

    /**
     * Get sessions longer than specified duration (for binge detection)
     */
    suspend fun getLongSessions(
        startDate: String,
        endDate: String,
        minDurationMillis: Long
    ): List<UsageSession>

    /**
     * Get all sessions with their hour of day extracted
     */
    suspend fun getSessionsWithHourOfDay(startDate: String, endDate: String): List<UsageSession>

    // ========== Sync Methods for Sessions ==========

    /**
     * Get all sessions for a specific user
     */
    suspend fun getSessionsByUserId(userId: String): List<UsageSession>

    /**
     * Get unsynced sessions for a user (pending sync status)
     */
    suspend fun getUnsyncedSessions(userId: String): List<UsageSession>

    /**
     * Mark a session as synced
     */
    suspend fun markSessionAsSynced(sessionId: Long, cloudId: String)

    /**
     * Upsert a session from remote with sync metadata
     */
    suspend fun upsertSessionFromRemote(session: UsageSession, cloudId: String)

    /**
     * Update user ID for a session
     */
    suspend fun updateSessionUserId(sessionId: Long, userId: String)

    // ========== Sync Methods for Events ==========

    /**
     * Get all events for a specific user
     */
    suspend fun getEventsByUserId(userId: String): List<UsageEvent>

    /**
     * Get unsynced events for a user (pending sync status)
     */
    suspend fun getUnsyncedEvents(userId: String): List<UsageEvent>

    /**
     * Mark an event as synced
     */
    suspend fun markEventAsSynced(eventId: Long, cloudId: String)

    /**
     * Upsert an event from remote with sync metadata
     */
    suspend fun upsertEventFromRemote(event: UsageEvent, cloudId: String)

    /**
     * Update user ID for an event
     */
    suspend fun updateEventUserId(eventId: Long, userId: String)
}
