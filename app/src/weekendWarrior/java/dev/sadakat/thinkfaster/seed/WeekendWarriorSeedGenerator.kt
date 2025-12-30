package dev.sadakat.thinkfast.seed

import dev.sadakat.thinkfaster.data.local.database.ThinkFastDatabase
import dev.sadakat.thinkfaster.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfaster.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfaster.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfaster.data.seed.model.SeedData
import dev.sadakat.thinkfaster.data.seed.model.SeedMetadata

/**
 * Weekend Warrior Persona (Days 25)
 *
 * Characteristics:
 * - MODERATE→FIRM friction (3s delay, progressing to 5s)
 * - Weekday: 25-40 min/day, 5-8 sessions
 * - Weekend: 90-120 min/day, 15-20 sessions (2.5x multiplier)
 * - Weekend morning heavy (8am-12pm = 50% of weekend usage)
 * - 35% PROCEED rate (moderate self-control)
 * - Streaks break every weekend (max 5 days)
 *
 * Goal: Test weekend usage spikes and streak breaking patterns.
 * This user has good weekday control but loses it on weekends.
 */
class WeekendWarriorSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.WEEKEND_WARRIOR
) {

    override suspend fun seedDatabase(database: ThinkFastDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = 25  // 25 days of data

        // Generate sessions with weekend multiplier
        // The PersonaConfigurations.WEEKEND_WARRIOR has weekendUsageMultiplier = 2.5
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (15% - moderate)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // Generate goals (set 25 days ago)
        // Streak will be limited to 5 days max due to weekend breaks
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = 25
        )

        // Calculate daily stats
        val dailyStats = calculateDailyStats(sessionsWithQuickReopens)

        // Generate events
        val events = generateEvents(sessionsWithQuickReopens)

        // Generate intervention results
        val interventionGenerator = InterventionResultGenerator(config, random)
        val interventionResults = interventionGenerator.generateForSessions(
            sessions = sessionsWithQuickReopens,
            quickReopenMap = quickReopenMap,
            currentStreak = goals.firstOrNull()?.currentStreak ?: 0
        )

        // Create seed data
        val seedData = SeedData(
            persona = config.personaType,
            goals = goals,
            sessions = sessionsWithQuickReopens,
            dailyStats = dailyStats,
            events = events,
            interventionResults = interventionResults,
            metadata = SeedMetadata(
                daysOfData = days,
                targetApps = targetApps,
                description = "Weekend warrior - Weekday control (25-40 min), weekend spikes (90-120 min), 2.5x multiplier, max 5-day streaks"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
