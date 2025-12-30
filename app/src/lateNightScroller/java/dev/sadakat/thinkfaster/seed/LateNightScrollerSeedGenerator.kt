package dev.sadakat.thinkfast.seed

import dev.sadakat.thinkfaster.data.local.database.ThinkFastDatabase
import dev.sadakat.thinkfaster.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfaster.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfaster.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfaster.data.seed.model.SeedData
import dev.sadakat.thinkfaster.data.seed.model.SeedMetadata

/**
 * Late Night Scroller Persona (Days 20)
 *
 * Characteristics:
 * - MODERATE friction (3s delay)
 * - 70% usage between 10pm-2am (classic late night scrolling pattern)
 * - 60-90 min/day, 10-15 sessions
 * - 45% PROCEED rate (struggle with late night willpower)
 * - Over goal (120-150% of 60 min goal)
 * - Many isLateNight intervention contexts
 * - 5-8 day streak (inconsistent due to late nights)
 *
 * Goal: Test late night intervention content (breathing exercises, activity suggestions).
 * This user scrolls when tired, has lower willpower.
 */
class LateNightScrollerSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.LATE_NIGHT_SCROLLER
) {

    override suspend fun seedDatabase(database: ThinkFastDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = 20  // 20 days of data

        // Generate sessions with late night time distribution
        // The PersonaConfigurations.LATE_NIGHT_SCROLLER already has the right TimeDistribution
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (25% - moderate quick reopening)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // Generate goals (set 20 days ago)
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = 20
        )

        // Calculate daily stats
        val dailyStats = calculateDailyStats(sessionsWithQuickReopens)

        // Generate events
        val events = generateEvents(sessionsWithQuickReopens)

        // Generate intervention results
        // Many will have isLateNight=true, triggering breathing/activity content
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
                description = "Late night scroller - 70% usage 10pm-2am, over goal, MODERATE friction, 5-8 day streak"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
