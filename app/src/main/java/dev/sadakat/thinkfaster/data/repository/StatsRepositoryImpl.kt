package dev.sadakat.thinkfaster.data.repository

import dev.sadakat.thinkfaster.data.local.database.dao.DailyStatsDao
import dev.sadakat.thinkfaster.data.local.database.dao.UsageSessionDao
import dev.sadakat.thinkfaster.data.mapper.toDomain
import dev.sadakat.thinkfaster.data.mapper.toEntity
import dev.sadakat.thinkfaster.domain.model.DailyStats
import dev.sadakat.thinkfaster.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of StatsRepository using Room database
 */
class StatsRepositoryImpl(
    private val dailyStatsDao: DailyStatsDao,
    private val usageSessionDao: UsageSessionDao
) : StatsRepository {

    override suspend fun upsertDailyStats(stats: DailyStats) {
        dailyStatsDao.upsertDailyStats(stats.toEntity())
    }

    override suspend fun getDailyStats(date: String, targetApp: String): DailyStats? {
        return dailyStatsDao.getDailyStats(date, targetApp)?.toDomain()
    }

    override fun observeDailyStats(date: String, targetApp: String): Flow<DailyStats?> {
        return dailyStatsDao.observeDailyStats(date, targetApp).map { it?.toDomain() }
    }

    override suspend fun getAllDailyStatsForDate(date: String): List<DailyStats> {
        return dailyStatsDao.getAllDailyStatsForDate(date).toDomain()
    }

    override suspend fun getDailyStatsInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): List<DailyStats> {
        return dailyStatsDao.getDailyStatsInRange(startDate, endDate, targetApp).toDomain()
    }

    override fun observeDailyStatsInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): Flow<List<DailyStats>> {
        return dailyStatsDao.observeDailyStatsInRange(startDate, endDate, targetApp)
            .map { it.toDomain() }
    }

    override suspend fun getTotalDurationInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): Long {
        return dailyStatsDao.getTotalDurationInRange(startDate, endDate, targetApp) ?: 0L
    }

    override suspend fun getAverageDurationInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): Long {
        return dailyStatsDao.getAverageDurationInRange(startDate, endDate, targetApp) ?: 0L
    }

    override suspend fun getMaxDurationInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): Long {
        return dailyStatsDao.getMaxDurationInRange(startDate, endDate, targetApp) ?: 0L
    }

    override suspend fun getTotalSessionCountInRange(
        startDate: String,
        endDate: String,
        targetApp: String
    ): Int {
        return dailyStatsDao.getTotalSessionCountInRange(startDate, endDate, targetApp) ?: 0
    }

    override suspend fun deleteStatsOlderThan(beforeDate: String) {
        dailyStatsDao.deleteStatsOlderThan(beforeDate)
    }

    override suspend fun aggregateDailyStats(date: String) {
        // Get all sessions for this date and aggregate them per app
        val sessions = usageSessionDao.getSessionsByDate(date)

        // Group by target app
        val sessionsByApp = sessions.groupBy { it.targetApp }

        sessionsByApp.forEach { (targetApp, appSessions) ->
            val totalDuration = appSessions.sumOf { it.duration }
            val sessionCount = appSessions.size
            val longestSession = appSessions.maxOfOrNull { it.duration } ?: 0L
            val averageSession = if (sessionCount > 0) totalDuration / sessionCount else 0L

            // Count alerts from interrupted sessions
            val alertsShown = appSessions.count { it.wasInterrupted }
            val alertsProceeded = alertsShown // All interruptions are proceeded (for now)

            val stats = DailyStats(
                date = date,
                targetApp = targetApp,
                totalDuration = totalDuration,
                sessionCount = sessionCount,
                longestSession = longestSession,
                averageSession = averageSession,
                alertsShown = alertsShown,
                alertsProceeded = alertsProceeded,
                lastUpdated = System.currentTimeMillis()
            )

            upsertDailyStats(stats)
        }
    }
}
