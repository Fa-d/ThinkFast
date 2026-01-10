package dev.sadakat.thinkfaster.seed

import dev.sadakat.thinkfaster.data.local.database.IntentlyDatabase
import dev.sadakat.thinkfaster.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfaster.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfaster.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfaster.data.seed.model.SeedData
import dev.sadakat.thinkfaster.data.seed.model.SeedMetadata

/**
 * Compulsive Reopener Persona (Days 18)
 *
 * Characteristics:
 * - MODERATE friction (3s delay)
 * - 80-120 min/day, 20-30 sessions
 * - 60% quick reopen rate (sessions <2 min apart)
 * - Many short sessions (habit override)
 * - 70% PROCEED rate (can't resist reopening)
 * - 80% instant decisions (<2s - habitual)
 * - 8-12 day streak (decent but not great)
 *
 * Goal: Test quick reopen detection and intervention content effectiveness.
 * This user compulsively reopens apps immediately after closing.
 */
class CompulsiveReopenerSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.COMPULSIVE_REOPENER
) {

    override suspend fun seedDatabase(database: IntentlyDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = 18  // 18 days of data

        // Generate sessions
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (60% - very high!)
        // This will create many sessions <2 min apart
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // Generate goals (set 18 days ago)
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = 18
        )

        // Calculate daily stats
        val dailyStats = calculateDailyStats(sessionsWithQuickReopens)

        // Generate events
        val events = generateEvents(sessionsWithQuickReopens)

        // Generate intervention results
        // Many will have quickReopen=true, triggering reflection/emotional content
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
                description = "Compulsive reopener - 60% quick reopen rate, 20-30 sessions/day, MODERATE friction, 80% instant decisions"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
