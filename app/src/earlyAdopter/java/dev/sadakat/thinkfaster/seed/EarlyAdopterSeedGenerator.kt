package dev.sadakat.thinkfast.seed

import dev.sadakat.thinkfaster.data.local.database.ThinkFastDatabase
import dev.sadakat.thinkfaster.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfaster.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfaster.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfaster.data.seed.model.SeedData
import dev.sadakat.thinkfaster.data.seed.model.SeedMetadata

/**
 * Early Adopter Persona (Days 7-10)
 *
 * Characteristics:
 * - GENTLE friction (still no delay)
 * - Morning + evening peaks (balanced usage)
 * - 35-55 min/day, 6-10 sessions
 * - 50% PROCEED rate (starting to respond to interventions)
 * - 3-5 day streak (forming habits)
 * - 50% instant, 30% quick decisions
 *
 * Goal: User is forming habits, starting to see value in the app.
 * Still gentle to encourage continued use.
 */
class EarlyAdopterSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.EARLY_ADOPTER
) {

    override suspend fun seedDatabase(database: ThinkFastDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = random.nextInt(7, 11)  // 7-10 days of data

        // Generate sessions
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (20% quick reopen rate)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // Generate goals (set about a week ago)
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = 7
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
                description = "Early adopter - Day 7-10, forming habits, 3-5 day streak, GENTLE friction"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
