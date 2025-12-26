package dev.sadakat.thinkfast.domain.repository

import dev.sadakat.thinkfast.domain.model.UsageEvent
import dev.sadakat.thinkfast.domain.model.UsageSession
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
}
