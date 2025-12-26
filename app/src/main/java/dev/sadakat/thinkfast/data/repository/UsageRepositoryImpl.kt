package dev.sadakat.thinkfast.data.repository

import dev.sadakat.thinkfast.data.local.database.dao.UsageEventDao
import dev.sadakat.thinkfast.data.local.database.dao.UsageSessionDao
import dev.sadakat.thinkfast.data.mapper.toDomain
import dev.sadakat.thinkfast.data.mapper.toEntity
import dev.sadakat.thinkfast.domain.model.UsageEvent
import dev.sadakat.thinkfast.domain.model.UsageSession
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of UsageRepository using Room database
 */
class UsageRepositoryImpl(
    private val sessionDao: UsageSessionDao,
    private val eventDao: UsageEventDao
) : UsageRepository {

    override suspend fun insertSession(session: UsageSession): Long {
        return sessionDao.insertSession(session.toEntity())
    }

    override suspend fun updateSession(session: UsageSession) {
        sessionDao.updateSession(session.toEntity())
    }

    override suspend fun getActiveSession(): UsageSession? {
        return sessionDao.getActiveSession()?.toDomain()
    }

    override suspend fun endSession(
        sessionId: Long,
        endTimestamp: Long,
        wasInterrupted: Boolean,
        interruptionType: String?
    ) {
        sessionDao.endSession(sessionId, endTimestamp, wasInterrupted, interruptionType)
    }

    override suspend fun getSessionsByDate(date: String): List<UsageSession> {
        return sessionDao.getSessionsByDate(date).toDomain()
    }

    override fun observeSessionsByDate(date: String): Flow<List<UsageSession>> {
        return sessionDao.observeSessionsByDate(date).map { it.toDomain() }
    }

    override suspend fun getSessionsInRange(startDate: String, endDate: String): List<UsageSession> {
        return sessionDao.getSessionsInRange(startDate, endDate).toDomain()
    }

    override suspend fun getSessionsByAppInRange(
        targetApp: String,
        startDate: String,
        endDate: String
    ): List<UsageSession> {
        return sessionDao.getSessionsByAppInRange(targetApp, startDate, endDate).toDomain()
    }

    override suspend fun getLongestSessionInRange(
        targetApp: String,
        startDate: String,
        endDate: String
    ): UsageSession? {
        return sessionDao.getLongestSessionInRange(targetApp, startDate, endDate)?.toDomain()
    }

    override suspend fun deleteSessionsOlderThan(beforeDate: String) {
        sessionDao.deleteSessionsOlderThan(beforeDate)
    }

    // ========== Usage Events ==========

    override suspend fun insertEvent(event: UsageEvent) {
        eventDao.insertEvent(event.toEntity())
    }

    override suspend fun getEventsBySession(sessionId: Long): List<UsageEvent> {
        return eventDao.getEventsBySession(sessionId).toDomain()
    }

    override suspend fun getEventsInRange(startDate: String, endDate: String): List<UsageEvent> {
        return eventDao.getEventsInRange(startDate, endDate).toDomain()
    }

    override suspend fun countEventsByTypeInRange(
        eventType: String,
        startDate: String,
        endDate: String
    ): Int {
        return eventDao.countEventsByTypeInRange(eventType, startDate, endDate)
    }
}
