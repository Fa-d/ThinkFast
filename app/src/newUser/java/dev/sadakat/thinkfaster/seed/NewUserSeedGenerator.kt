package dev.sadakat.thinkfaster.seed

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import dev.sadakat.thinkfaster.data.local.database.IntentlyDatabase
import dev.sadakat.thinkfaster.data.seed.config.PersonaConfig
import dev.sadakat.thinkfaster.data.seed.config.TimeDistribution
import dev.sadakat.thinkfaster.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfaster.data.seed.model.GoalData
import dev.sadakat.thinkfaster.data.seed.model.PersonaType
import dev.sadakat.thinkfaster.data.seed.model.SeedData
import dev.sadakat.thinkfaster.data.seed.model.SeedMetadata
import dev.sadakat.thinkfaster.data.seed.model.SessionData
import dev.sadakat.thinkfaster.data.seed.util.TimeDistributionHelper
import org.koin.java.KoinJavaComponent.inject
import java.util.Calendar
import kotlin.random.Random

/**
 * New User Persona with Real Usage Data Extraction
 *
 * This generator attempts to extract real usage data from the device using UsageStatsManager.
 * If insufficient real data is found (<10 min total), it falls back to a synthetic baseline.
 *
 * Synthetic Baseline Characteristics:
 * - 7 days of data
 * - 2-3 sessions per day
 * - 5-15 minute sessions
 * - Evening weighted (6pm-10pm = 60%)
 * - Weekend multiplier 1.3x
 * - Goals set to 60 min/day (OnboardingViewModel default)
 * - 3-day streaks
 * - No intervention results (new user hasn't seen interventions yet)
 *
 * Goal: Provide realistic onboarding experience for actual new users.
 */
class NewUserSeedGenerator : BaseSeedGenerator(
    config = PersonaConfig(
        personaType = PersonaType.FRESH_INSTALL,
        daysSinceInstall = 1..7,
        frictionLevel = dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.GENTLE,
        dailyUsageMinutes = 20..40,
        sessionsPerDay = 2..3,
        averageSessionMinutes = 5..15,
        longestSessionMinutes = 15..25,
        quickReopenRate = 0.20,
        hasGoals = true,
        goalComplianceRate = 0.80,
        streakDays = 0..3,
        timeDistribution = TimeDistribution(
            morning = 0.10,   // 6am-10am: 10%
            midday = 0.10,    // 10am-3pm: 10%
            evening = 0.60,   // 3pm-8pm: 60%
            lateNight = 0.15, // 8pm-12am: 15%
            veryLate = 0.05   // 12am-6am: 5%
        ),
        weekendUsageMultiplier = 1.3,
        interventionResponse = dev.sadakat.thinkfaster.data.seed.config.InterventionResponsePattern(
            proceedRate = 0.60,
            goBackRate = 0.30,
            dismissedRate = 0.10
        ),
        decisionTimeDistribution = dev.sadakat.thinkfaster.data.seed.config.DecisionTimeDistribution(
            instantRate = 0.70,
            quickRate = 0.20,
            moderateRate = 0.08,
            deliberateRate = 0.02
        ),
        extendedSessionRate = 0.10,
        description = "New user synthetic baseline - 7 days, evening-heavy, realistic patterns"
    ),
    random = Random.Default
) {

    companion object {
        private const val TAG = "NewUserSeedGenerator"
        private const val MIN_REAL_DATA_THRESHOLD_MS = 10 * 60 * 1000L  // 10 minutes
        private const val SESSION_MERGE_GAP_MS = 30 * 1000L  // 30 seconds
        private const val MIN_SESSION_DURATION_MS = 30 * 1000L  // 30 seconds
    }

    private val context: Context by inject(Context::class.java)

    override suspend fun seedDatabase(database: IntentlyDatabase) {
        Log.d(TAG, "Starting NewUser seeding...")

        // Attempt to extract real usage data
        val realUsageData = extractRealUsageData(days = 30)
        val totalRealUsage = realUsageData.sumOf { it.duration }

        Log.d(TAG, "Real usage data: ${realUsageData.size} sessions, ${totalRealUsage / 1000 / 60} minutes total")

        val seedData = if (totalRealUsage >= MIN_REAL_DATA_THRESHOLD_MS) {
            Log.d(TAG, "Using real usage data")
            generateFromRealUsage(realUsageData)
        } else {
            Log.d(TAG, "Insufficient real data, using synthetic baseline")
            generateSyntheticBaseline()
        }

        // Insert into database
        insertSeedData(database, seedData)
    }

    /**
     * Extracts real usage data from UsageStatsManager for the last N days.
     */
    private fun extractRealUsageData(days: Int): List<SessionData> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager == null) {
            Log.w(TAG, "UsageStatsManager not available")
            return emptyList()
        }

        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val sessions = mutableListOf<SessionData>()

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (days * 24 * 60 * 60 * 1000L)

        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()

            // Track session starts for each app
            val sessionStarts = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)

                if (event.packageName !in targetApps) continue

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        // Session start
                        sessionStarts[event.packageName] = event.timeStamp
                    }

                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        // Session end
                        val startTimestamp = sessionStarts[event.packageName]
                        if (startTimestamp != null) {
                            val duration = event.timeStamp - startTimestamp

                            // Only include sessions longer than 30 seconds
                            if (duration >= MIN_SESSION_DURATION_MS) {
                                val date = TimeDistributionHelper.timestampToDateString(startTimestamp)

                                sessions.add(
                                    SessionData(
                                        targetApp = event.packageName,
                                        startTimestamp = startTimestamp,
                                        endTimestamp = event.timeStamp,
                                        duration = duration,
                                        wasInterrupted = false,
                                        interruptionType = null,
                                        date = date
                                    )
                                )
                            }

                            sessionStarts.remove(event.packageName)
                        }
                    }
                }
            }

            // Merge sessions with <30s gap (same session)
            val mergedSessions = mergeSessions(sessions)

            Log.d(TAG, "Extracted ${mergedSessions.size} sessions from UsageStatsManager")
            return mergedSessions.sortedBy { it.startTimestamp }

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for UsageStatsManager", e)
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting real usage data", e)
            return emptyList()
        }
    }

    /**
     * Merges sessions that are less than 30 seconds apart (same session).
     */
    private fun mergeSessions(sessions: List<SessionData>): List<SessionData> {
        if (sessions.isEmpty()) return emptyList()

        val sorted = sessions.sortedBy { it.startTimestamp }
        val merged = mutableListOf<SessionData>()
        var current = sorted.first()

        for (i in 1 until sorted.size) {
            val next = sorted[i]

            // Same app and gap < 30 seconds
            if (next.targetApp == current.targetApp &&
                next.startTimestamp - current.endTimestamp < SESSION_MERGE_GAP_MS
            ) {
                // Merge sessions
                current = current.copy(
                    endTimestamp = next.endTimestamp,
                    duration = next.endTimestamp - current.startTimestamp
                )
            } else {
                // Different session
                merged.add(current)
                current = next
            }
        }
        merged.add(current)

        return merged
    }

    /**
     * Generates seed data from real usage data.
     */
    private fun generateFromRealUsage(sessions: List<SessionData>): SeedData {
        val targetApps = sessions.map { it.targetApp }.distinct()

        // Generate goals based on actual usage
        val avgDailyMinutes = sessions.groupBy { it.date }
            .map { (_, daySessions) -> daySessions.sumOf { it.duration } / 1000 / 60 }
            .average()
            .toInt()

        // Set goal slightly below average usage to encourage improvement
        val goalLimit = ((avgDailyMinutes * 0.8).toInt().coerceIn(30, 90))

        val goals = targetApps.map { app ->
            GoalData(
                targetApp = app,
                dailyLimitMinutes = goalLimit,
                startDate = sessions.firstOrNull()?.date ?: TimeDistributionHelper.getDateStringDaysAgo(0),
                currentStreak = calculateStreak(sessions, goalLimit),
                longestStreak = calculateStreak(sessions, goalLimit)
            )
        }

        // Calculate daily stats
        val dailyStats = calculateDailyStats(sessions)

        // No intervention results for real new users (haven't seen interventions yet)
        val interventionResults = emptyList<dev.sadakat.thinkfaster.data.seed.model.InterventionResultData>()

        // Events are optional
        val events = emptyList<dev.sadakat.thinkfaster.data.seed.model.EventData>()

        val daysOfData = sessions.map { it.date }.distinct().size

        return SeedData(
            persona = PersonaType.FRESH_INSTALL,
            goals = goals,
            sessions = sessions,
            dailyStats = dailyStats,
            events = events,
            interventionResults = interventionResults,
            metadata = SeedMetadata(
                daysOfData = daysOfData,
                targetApps = targetApps,
                description = "New user with real usage data - $daysOfData days, ${sessions.size} sessions, ${avgDailyMinutes}min/day avg"
            )
        )
    }

    /**
     * Calculates current streak based on sessions and goal limit.
     */
    private fun calculateStreak(sessions: List<SessionData>, goalLimitMinutes: Int): Int {
        val dailyUsage = sessions.groupBy { it.date }
            .mapValues { (_, daySessions) -> daySessions.sumOf { it.duration } / 1000 / 60 }

        var streak = 0
        val today = TimeDistributionHelper.timestampToDateString(System.currentTimeMillis())
        var checkDate = today

        // Count backwards from today
        for (i in 0 until 30) {
            val usage = dailyUsage[checkDate] ?: 0
            if (usage <= goalLimitMinutes) {
                streak++
            } else {
                break
            }

            // Move to previous day
            val calendar = Calendar.getInstance().apply {
                timeInMillis = TimeDistributionHelper.dateToTimestamp(checkDate, 0, 0)
                add(Calendar.DAY_OF_MONTH, -1)
            }
            checkDate = TimeDistributionHelper.timestampToDateString(calendar.timeInMillis)
        }

        return streak
    }

    /**
     * Generates synthetic baseline when no real usage data is available.
     */
    private fun generateSyntheticBaseline(): SeedData {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = 7

        // Generate sessions using base generator infrastructure
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (20%)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Generate goals (60 min/day - OnboardingViewModel default)
        val goals = targetApps.map { app ->
            GoalData(
                targetApp = app,
                dailyLimitMinutes = 60,
                startDate = TimeDistributionHelper.getDateStringDaysAgo(7),
                currentStreak = random.nextInt(0, 4),  // 0-3 day streak
                longestStreak = random.nextInt(0, 4)
            )
        }

        // Calculate daily stats
        val dailyStats = calculateDailyStats(sessionsWithQuickReopens)

        // No intervention results for synthetic new users
        val interventionResults = emptyList<dev.sadakat.thinkfaster.data.seed.model.InterventionResultData>()

        // No events
        val events = emptyList<dev.sadakat.thinkfaster.data.seed.model.EventData>()

        return SeedData(
            persona = PersonaType.FRESH_INSTALL,
            goals = goals,
            sessions = sessionsWithQuickReopens,
            dailyStats = dailyStats,
            events = events,
            interventionResults = interventionResults,
            metadata = SeedMetadata(
                daysOfData = days,
                targetApps = targetApps,
                description = "New user synthetic baseline - 7 days, 2-3 sessions/day, evening-heavy, 60 min/day goal"
            )
        )
    }
}
