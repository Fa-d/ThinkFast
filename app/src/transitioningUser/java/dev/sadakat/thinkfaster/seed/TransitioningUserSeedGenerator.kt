package dev.sadakat.thinkfast.seed

import dev.sadakat.thinkfaster.data.local.database.IntentlyDatabase
import dev.sadakat.thinkfaster.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfaster.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfaster.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfaster.data.seed.model.SeedData
import dev.sadakat.thinkfaster.data.seed.model.SeedMetadata

/**
 * Transitioning User Persona (Days 14-16)
 *
 * Characteristics:
 * - MODERATE friction (3s delay - just activated!)
 * - Balanced time distribution
 * - 30-45 min/day, 5-8 sessions
 * - 40% PROCEED rate (adapting to new friction)
 * - 7-10 day streak (good progress)
 * - 40% quick, 40% moderate decisions (more deliberate)
 *
 * Goal: User just hit the 2-week mark and friction increased.
 * They're adapting to the 3-second delay and becoming more mindful.
 */
class TransitioningUserSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.TRANSITIONING_USER
) {

    override suspend fun seedDatabase(database: IntentlyDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = random.nextInt(14, 17)  // 14-16 days of data

        // Generate sessions
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (15% quick reopen rate - improving)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // Generate goals (set 2 weeks ago)
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = 14
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
                description = "Transitioning user - Day 14-16, just hit MODERATE friction (3s delay), adapting well"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
