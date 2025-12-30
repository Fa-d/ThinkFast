package dev.sadakat.thinkfaster.data.seed.generator

import android.util.Log
import dev.sadakat.thinkfaster.data.local.database.ThinkFastDatabase
import dev.sadakat.thinkfaster.data.local.database.entities.DailyStatsEntity
import dev.sadakat.thinkfaster.data.local.database.entities.GoalEntity
import dev.sadakat.thinkfaster.data.local.database.entities.InterventionResultEntity
import dev.sadakat.thinkfaster.data.local.database.entities.UsageSessionEntity
import dev.sadakat.thinkfaster.data.seed.config.PersonaConfig
import dev.sadakat.thinkfaster.data.seed.model.DailyStatsData
import dev.sadakat.thinkfaster.data.seed.model.EventData
import dev.sadakat.thinkfaster.data.seed.model.GoalData
import dev.sadakat.thinkfaster.data.seed.model.SeedData
import dev.sadakat.thinkfaster.data.seed.model.SessionData
import dev.sadakat.thinkfaster.data.seed.util.TimeDistributionHelper
import java.util.Calendar
import kotlin.random.Random

/**
 * Base class for all seed generators.
 * Provides common functionality for generating realistic seed data.
 */
abstract class BaseSeedGenerator(
    protected val config: PersonaConfig,
    protected val random: Random = Random.Default
) : SeedGenerator {

    companion object {
        const val QUICK_REOPEN_THRESHOLD_MS = 2 * 60 * 1000L  // 2 minutes
        const val FACEBOOK_PACKAGE = "com.facebook.katana"
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
    }

    override fun getPersonaConfig(): PersonaConfig = config

    /**
     * Generates sessions for the specified number of days.
     * Sessions are distributed across the day based on TimeDistribution.
     */
    protected fun generateSessions(
        config: PersonaConfig,
        days: Int,
        targetApps: List<String> = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
    ): List<SessionData> {
        val sessions = mutableListOf<SessionData>()
        val endDate = Calendar.getInstance()

        // Generate sessions for each day, going backwards from today
        for (dayOffset in days - 1 downTo 0) {
            val calendar = Calendar.getInstance().apply {
                time = endDate.time
                add(Calendar.DAY_OF_MONTH, -dayOffset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val date = TimeDistributionHelper.timestampToDateString(calendar.timeInMillis)
            val isWeekend = TimeDistributionHelper.isWeekend(calendar.timeInMillis)

            // Generate sessions for this day
            val dailySessions = generateSessionsForDay(
                date = date,
                isWeekend = isWeekend,
                targetApps = targetApps
            )

            sessions.addAll(dailySessions)
        }

        return sessions.sortedBy { it.startTimestamp }
    }

    /**
     * Generates sessions for a single day.
     */
    private fun generateSessionsForDay(
        date: String,
        isWeekend: Boolean,
        targetApps: List<String>
    ): List<SessionData> {
        val sessions = mutableListOf<SessionData>()

        // Determine session count for the day
        val baseSessionCount = random.nextInt(
            config.sessionsPerDay.first,
            config.sessionsPerDay.last + 1
        )

        // Apply weekend multiplier if applicable
        val sessionCount = if (isWeekend) {
            (baseSessionCount * config.weekendUsageMultiplier).toInt()
        } else {
            baseSessionCount
        }

        // Generate session times for the day
        val sessionTimes = mutableListOf<Pair<Int, Int>>()  // (hour, minute)

        for (i in 0 until sessionCount) {
            val hour = TimeDistributionHelper.sampleHourOfDay(config.timeDistribution, random)
            val minute = random.nextInt(0, 60)
            sessionTimes.add(hour to minute)
        }

        // Sort by time
        sessionTimes.sortBy { it.first * 60 + it.second }

        // Create sessions
        for ((hour, minute) in sessionTimes) {
            val targetApp = targetApps.random(random)
            val startTimestamp = TimeDistributionHelper.dateToTimestamp(date, hour, minute)

            // Generate session duration
            val baseDuration = random.nextInt(
                config.averageSessionMinutes.first,
                config.averageSessionMinutes.last + 1
            )

            // Occasionally create extended sessions
            val duration = if (random.nextDouble() < config.extendedSessionRate) {
                random.nextInt(
                    config.longestSessionMinutes.first,
                    config.longestSessionMinutes.last + 1
                )
            } else {
                baseDuration
            }

            val durationMs = duration * 60 * 1000L
            val endTimestamp = startTimestamp + durationMs

            sessions.add(
                SessionData(
                    targetApp = targetApp,
                    startTimestamp = startTimestamp,
                    endTimestamp = endTimestamp,
                    duration = durationMs,
                    wasInterrupted = false,
                    interruptionType = null,
                    date = date
                )
            )
        }

        return sessions
    }

    /**
     * Detects quick reopens (sessions starting within 2 minutes of previous session end).
     * Adjusts sessions to create quick reopen patterns based on persona config.
     */
    protected fun applyQuickReopenPattern(sessions: List<SessionData>): List<SessionData> {
        if (sessions.isEmpty()) return sessions

        val result = mutableListOf<SessionData>()
        val targetQuickReopenCount = (sessions.size * config.quickReopenRate).toInt()
        var quickReopenCount = 0

        result.add(sessions.first())

        for (i in 1 until sessions.size) {
            val previousSession = result.last()
            val currentSession = sessions[i]

            // Check if we should create a quick reopen
            val shouldCreateQuickReopen = quickReopenCount < targetQuickReopenCount &&
                    random.nextDouble() < config.quickReopenRate &&
                    currentSession.date == previousSession.date  // Same day

            if (shouldCreateQuickReopen) {
                // Adjust start time to be within 2 minutes of previous end
                val quickReopenDelay = random.nextLong(10_000, QUICK_REOPEN_THRESHOLD_MS)
                val newStart = previousSession.endTimestamp + quickReopenDelay
                val newEnd = newStart + currentSession.duration

                result.add(
                    currentSession.copy(
                        startTimestamp = newStart,
                        endTimestamp = newEnd
                    )
                )
                quickReopenCount++
            } else {
                result.add(currentSession)
            }
        }

        return result
    }

    /**
     * Calculates daily statistics from sessions.
     */
    protected fun calculateDailyStats(sessions: List<SessionData>): List<DailyStatsData> {
        return sessions
            .groupBy { it.date to it.targetApp }
            .map { (key, daySessions) ->
                val (date, targetApp) = key
                val totalDuration = daySessions.sumOf { it.duration }
                val sessionCount = daySessions.size
                val longestSession = daySessions.maxOf { it.duration }
                val averageSession = if (sessionCount > 0) totalDuration / sessionCount else 0L

                // For seed data, we don't have actual alert data, so use estimates
                val alertsShown = (sessionCount * 0.6).toInt()  // 60% of sessions show alerts
                val alertsProceeded = (alertsShown * 0.5).toInt()  // 50% proceed

                DailyStatsData(
                    date = date,
                    targetApp = targetApp,
                    totalDuration = totalDuration,
                    sessionCount = sessionCount,
                    longestSession = longestSession,
                    averageSession = averageSession,
                    alertsShown = alertsShown,
                    alertsProceeded = alertsProceeded
                )
            }
    }

    /**
     * Inserts all seed data into the database with proper foreign key handling.
     */
    protected suspend fun insertSeedData(database: ThinkFastDatabase, seedData: SeedData) {
        // 1. Insert goals first (no dependencies)
        for (goalData in seedData.goals) {
            val goalEntity = GoalEntity(
                targetApp = goalData.targetApp,
                dailyLimitMinutes = goalData.dailyLimitMinutes,
                startDate = goalData.startDate,
                currentStreak = goalData.currentStreak,
                longestStreak = goalData.longestStreak,
                lastUpdated = System.currentTimeMillis()
            )
            database.goalDao().insertGoal(goalEntity)
        }

        // 2. Insert sessions and capture IDs
        val sessionIdMap = mutableMapOf<SessionData, Long>()
        for (sessionData in seedData.sessions) {
            val sessionEntity = UsageSessionEntity(
                targetApp = sessionData.targetApp,
                startTimestamp = sessionData.startTimestamp,
                endTimestamp = sessionData.endTimestamp,
                duration = sessionData.duration,
                wasInterrupted = sessionData.wasInterrupted,
                interruptionType = sessionData.interruptionType,
                date = sessionData.date
            )
            val sessionId = database.usageSessionDao().insertSession(sessionEntity)
            sessionIdMap[sessionData] = sessionId
            sessionData.id = sessionId  // Update the SessionData with the ID
        }

        // 3. Insert daily stats (no dependencies)
        val dailyStatsEntities = seedData.dailyStats.map { stats ->
            DailyStatsEntity(
                date = stats.date,
                targetApp = stats.targetApp,
                totalDuration = stats.totalDuration,
                sessionCount = stats.sessionCount,
                longestSession = stats.longestSession,
                averageSession = stats.averageSession,
                alertsShown = stats.alertsShown,
                alertsProceeded = stats.alertsProceeded,
                lastUpdated = System.currentTimeMillis()
            )
        }
        database.dailyStatsDao().insertAll(dailyStatsEntities)

        // 4. Insert events (references sessionId)
        // Skip events for now - they require proper session ID mapping
        // Events are not critical for persona testing
        // TODO: Implement proper event insertion with session ID mapping if needed

        // 5. Insert intervention results (references sessionId)
        val interventionEntities = seedData.interventionResults.map { result ->
            InterventionResultEntity(
                sessionId = result.sessionId,
                targetApp = result.targetApp,
                interventionType = result.interventionType,
                contentType = result.contentType,
                hourOfDay = result.hourOfDay,
                dayOfWeek = result.dayOfWeek,
                isWeekend = result.isWeekend,
                isLateNight = result.isLateNight,
                sessionCount = result.sessionCount,
                quickReopen = result.quickReopen,
                currentSessionDurationMs = result.currentSessionDurationMs,
                userChoice = result.userChoice,
                timeToShowDecisionMs = result.timeToShowDecisionMs,
                finalSessionDurationMs = result.finalSessionDurationMs,
                sessionEndedNormally = result.sessionEndedNormally,
                timestamp = result.timestamp
            )
        }
        database.interventionResultDao().insertResults(interventionEntities)

        // Log seeding summary
        Log.d("BaseSeedGenerator", "=== Seed Data Summary ===")
        Log.d("BaseSeedGenerator", "Persona: ${seedData.persona}")
        Log.d("BaseSeedGenerator", "Goals: ${seedData.goals.size}")
        Log.d("BaseSeedGenerator", "Sessions: ${seedData.sessions.size}")
        Log.d("BaseSeedGenerator", "Daily Stats: ${seedData.dailyStats.size}")
        Log.d("BaseSeedGenerator", "Intervention Results: ${seedData.interventionResults.size}")
        Log.d("BaseSeedGenerator", "Days of data: ${seedData.metadata.daysOfData}")
        Log.d("BaseSeedGenerator", "========================")
    }

    /**
     * Detects which sessions are quick reopens.
     * Returns a map of session index to whether it's a quick reopen.
     */
    protected fun detectQuickReopens(sessions: List<SessionData>): Map<Int, Boolean> {
        val quickReopens = mutableMapOf<Int, Boolean>()

        quickReopens[0] = false  // First session can't be a quick reopen

        for (i in 1 until sessions.size) {
            val current = sessions[i]
            val previous = sessions[i - 1]

            val timeSinceLastSession = current.startTimestamp - previous.endTimestamp
            val isQuickReopen = timeSinceLastSession < QUICK_REOPEN_THRESHOLD_MS &&
                    current.date == previous.date  // Same day

            quickReopens[i] = isQuickReopen
        }

        return quickReopens
    }

    /**
     * Generates goals for the persona.
     */
    protected fun generateGoals(
        targetApps: List<String>,
        startDaysAgo: Int
    ): List<GoalData> {
        if (!config.hasGoals) return emptyList()

        return targetApps.map { app ->
            val dailyLimit = when {
                config.goalComplianceRate < 0.5 -> {
                    // Struggling users have lower goals they still can't meet
                    random.nextInt(45, 75)
                }
                config.goalComplianceRate > 1.5 -> {
                    // Over-limit users have goals but exceed them significantly
                    random.nextInt(30, 60)
                }
                else -> {
                    // Normal goals
                    random.nextInt(60, 120)
                }
            }

            val streakDays = random.nextInt(
                config.streakDays.first,
                config.streakDays.last + 1
            )

            GoalData(
                targetApp = app,
                dailyLimitMinutes = dailyLimit,
                startDate = TimeDistributionHelper.getDateStringDaysAgo(startDaysAgo),
                currentStreak = streakDays,
                longestStreak = (streakDays * random.nextDouble(1.0, 1.5)).toInt()
            )
        }
    }

    /**
     * Generates basic events for tracking.
     */
    protected fun generateEvents(sessions: List<SessionData>): List<EventData> {
        val events = mutableListOf<EventData>()

        // Generate app_opened events for a subset of sessions
        for ((index, session) in sessions.withIndex()) {
            if (random.nextDouble() < 0.3) {  // 30% of sessions have tracked events
                events.add(
                    EventData(
                        eventType = "app_opened",
                        timestamp = session.startTimestamp,
                        metadata = session.id?.toString()  // Reference to session
                    )
                )
            }
        }

        return events
    }
}
