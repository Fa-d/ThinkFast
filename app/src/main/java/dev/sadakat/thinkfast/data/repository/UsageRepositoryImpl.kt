package dev.sadakat.thinkfast.data.repository

import dev.sadakat.thinkfast.data.local.database.dao.GoalDao
import dev.sadakat.thinkfast.data.local.database.dao.UsageEventDao
import dev.sadakat.thinkfast.data.local.database.dao.UsageSessionDao
import dev.sadakat.thinkfast.data.mapper.toDomain
import dev.sadakat.thinkfast.data.mapper.toEntity
import dev.sadakat.thinkfast.data.preferences.InterventionPreferences
import dev.sadakat.thinkfast.domain.intervention.FrictionLevel
import dev.sadakat.thinkfast.domain.model.UsageEvent
import dev.sadakat.thinkfast.domain.model.UsageSession
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Implementation of UsageRepository using Room database
 * Phase F: Added InterventionPreferences for friction level management
 */
class UsageRepositoryImpl(
    private val sessionDao: UsageSessionDao,
    private val eventDao: UsageEventDao,
    private val goalDao: GoalDao,
    private val interventionPreferences: InterventionPreferences  // Phase F
) : UsageRepository {

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

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

    // ========== Context-Aware Intervention Methods ==========

    override suspend fun getTodayUsageForApp(packageName: String): Long {
        val today = DATE_FORMAT.format(Calendar.getInstance().time)
        return sessionDao.getTotalDurationForDate(today, packageName) ?: 0L
    }

    override suspend fun getYesterdayUsageForApp(packageName: String): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = DATE_FORMAT.format(calendar.time)
        return sessionDao.getTotalDurationForDate(yesterday, packageName) ?: 0L
    }

    override suspend fun getWeeklyAverageForApp(packageName: String): Long {
        val calendar = Calendar.getInstance()
        val endDate = DATE_FORMAT.format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = DATE_FORMAT.format(calendar.time)

        val sessions = sessionDao.getSessionsByAppInRange(packageName, startDate, endDate)
        val totalDuration = sessions.sumOf { it.duration ?: 0L }
        return totalDuration / 7  // Average over 7 days
    }

    override suspend fun getTodaySessionCount(packageName: String): Int {
        val today = DATE_FORMAT.format(Calendar.getInstance().time)
        return sessionDao.getSessionsByDateAndApp(today, packageName).size
    }

    override suspend fun getLastSessionEndTime(packageName: String): Long {
        val today = DATE_FORMAT.format(Calendar.getInstance().time)
        val sessions = sessionDao.getSessionsByDateAndApp(today, packageName)
        return sessions.firstOrNull()?.endTimestamp ?: 0L
    }

    override suspend fun getCurrentSessionDuration(sessionId: Long): Long {
        val session = sessionDao.getSessionById(sessionId)
        val currentTime = System.currentTimeMillis()

        return if (session?.endTimestamp == null) {
            // Session is still active - return current duration
            currentTime - (session?.startTimestamp ?: currentTime)
        } else {
            // Session has ended - return actual duration
            session.duration
        }
    }

    override suspend fun getDailyGoalForApp(packageName: String): Int? {
        val goal = goalDao.getGoalByApp(packageName)
        return goal?.dailyLimitMinutes
    }

    override suspend fun getCurrentStreak(): Int {
        // Get streaks from both Facebook and Instagram goals
        val facebookGoal = goalDao.getGoalByApp("com.facebook.katana")
        val instagramGoal = goalDao.getGoalByApp("com.instagram.android")

        // Return the maximum streak between the two apps
        // This shows the user their best current achievement
        return maxOf(
            facebookGoal?.currentStreak ?: 0,
            instagramGoal?.currentStreak ?: 0
        )
    }

    override suspend fun getInstallDate(): Long {
        // Phase F: Use InterventionPreferences for install date
        var installDate = interventionPreferences.getInstallDate()

        // Set install date if not already set
        if (installDate == 0L) {
            installDate = System.currentTimeMillis()
            interventionPreferences.setInstallDate(installDate)
        }

        return installDate
    }

    /**
     * Phase F: Get the effective friction level considering user preferences
     */
    override suspend fun getEffectiveFrictionLevel(): FrictionLevel {
        val daysSinceInstall = interventionPreferences.getDaysSinceInstall()
        val calculatedLevel = FrictionLevel.fromDaysSinceInstall(daysSinceInstall)
        return interventionPreferences.getEffectiveFrictionLevel(calculatedLevel)
    }

    override suspend fun setFrictionLevelOverride(level: FrictionLevel?) {
        interventionPreferences.setFrictionLevelOverride(level)
    }

    override suspend fun getFrictionLevelOverride(): FrictionLevel? {
        return interventionPreferences.getFrictionLevelOverride()
    }

    override suspend fun getBestSessionMinutes(packageName: String): Int {
        val today = DATE_FORMAT.format(Calendar.getInstance().time)
        val sessions = sessionDao.getSessionsByDateAndApp(today, packageName)

        // Find shortest session with a duration > 1 minute (filter out accidental opens)
        val shortestSession = sessions
            .filter { it.duration > 60_000 }  // At least 1 minute
            .minByOrNull { it.duration ?: Long.MAX_VALUE }

        return ((shortestSession?.duration ?: 300_000) / 1000 / 60).toInt()  // Default 5 minutes
    }

    // ========== Phase 2: Behavioral Pattern Queries ==========

    override suspend fun getLateNightSessions(startDate: String, endDate: String): List<UsageSession> {
        return sessionDao.getLateNightSessions(startDate, endDate).toDomain()
    }

    override suspend fun getSessionsByDayOfWeek(
        startDate: String,
        endDate: String,
        daysOfWeek: List<Int>
    ): List<UsageSession> {
        return sessionDao.getSessionsByDayOfWeek(startDate, endDate, daysOfWeek).toDomain()
    }

    override suspend fun getLongSessions(
        startDate: String,
        endDate: String,
        minDurationMillis: Long
    ): List<UsageSession> {
        return sessionDao.getLongSessions(startDate, endDate, minDurationMillis).toDomain()
    }

    override suspend fun getSessionsWithHourOfDay(startDate: String, endDate: String): List<UsageSession> {
        return sessionDao.getSessionsWithHourOfDay(startDate, endDate).toDomain()
    }
}
