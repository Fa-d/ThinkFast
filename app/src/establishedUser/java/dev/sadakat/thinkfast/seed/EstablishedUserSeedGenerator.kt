package dev.sadakat.thinkfast.seed

import dev.sadakat.thinkfast.data.local.database.ThinkFastDatabase
import dev.sadakat.thinkfast.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfast.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfast.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfast.data.seed.model.SeedData
import dev.sadakat.thinkfast.data.seed.model.SeedMetadata

/**
 * Established User Persona (Days 30-35)
 *
 * Characteristics:
 * - FIRM friction (5s delay - strong friction)
 * - Healthy usage patterns
 * - 20-35 min/day, 4-7 sessions
 * - 30% PROCEED, 60% GO_BACK (very responsive to interventions)
 * - 15-21 day streak (consistent user)
 * - Mostly moderate/deliberate decisions (thoughtful)
 *
 * Goal: Mature user with healthy patterns. The app is working well for them.
 * Strong friction but they've adapted and use it mindfully.
 */
class EstablishedUserSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.ESTABLISHED_USER
) {

    override suspend fun seedDatabase(database: ThinkFastDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = random.nextInt(30, 36)  // 30-35 days of data

        // Generate sessions
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (only 10% - very good self-control)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // Generate goals (set a month ago)
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = 30
        )

        // Calculate daily stats
        val dailyStats = calculateDailyStats(sessionsWithQuickReopens)

        // Generate events
        val events = generateEvents(sessionsWithQuickReopens)

        // Generate intervention results with high streak
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
                description = "Established user - Day 30-35, FIRM friction (5s delay), healthy patterns, 15-21 day streak"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
