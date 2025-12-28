package dev.sadakat.thinkfast.seed

import dev.sadakat.thinkfast.data.local.database.ThinkFastDatabase
import dev.sadakat.thinkfast.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfast.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfast.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfast.data.seed.model.SeedData
import dev.sadakat.thinkfast.data.seed.model.SeedMetadata

/**
 * Fresh Install Persona (Days 1-2)
 *
 * Characteristics:
 * - GENTLE friction (no delay, easy to proceed)
 * - Evening-heavy usage (60% between 6pm-10pm)
 * - 45-75 min/day, 8-12 sessions
 * - 60% PROCEED rate (new users learning the app)
 * - 70% instant decisions (<2s)
 * - 0-1 day streak (just started)
 *
 * Goal: Don't scare away new users. Show them how the app works
 * without being too aggressive.
 */
class FreshInstallSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.FRESH_INSTALL
) {

    override suspend fun seedDatabase(database: ThinkFastDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = 2  // Just installed 1-2 days ago

        // Generate sessions
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (30% quick reopen rate for fresh users)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens for intervention context
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // Generate goals (fresh user just set them up)
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = 1  // Set goals yesterday
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
                description = "Fresh install user - Day 1-2, learning the app, GENTLE friction"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
