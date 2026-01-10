package dev.sadakat.thinkfast.seed

import dev.sadakat.thinkfaster.data.local.database.IntentlyDatabase
import dev.sadakat.thinkfaster.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfaster.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfaster.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfaster.data.seed.model.SeedData
import dev.sadakat.thinkfaster.data.seed.model.SeedMetadata

/**
 * Realistic Mixed Persona (Days 24)
 *
 * Characteristics:
 * - MODERATE friction (3s delay)
 * - 40-70 min/day, 7-12 sessions
 * - Mixed patterns: some late nights, weekend spikes, varied usage
 * - 50% PROCEED, 40% GO_BACK, 10% DISMISSED (realistic distribution)
 * - 25% instant, 35% quick, 30% moderate, 10% deliberate (realistic decisions)
 * - 10-15 day streak (decent but not exceptional)
 * - Weekend multiplier 1.3x (slight weekend increase)
 * - 20% quick reopen rate (moderate)
 * - 20% extended sessions (some long scrolls)
 *
 * Goal: Represents average user behavior for general testing.
 * This is the most realistic persona with varied patterns.
 */
class RealisticMixedSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.REALISTIC_MIXED
) {

    override suspend fun seedDatabase(database: IntentlyDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = 24  // 24 days of data

        // Generate sessions with realistic mixed patterns
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (20% - moderate)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // Generate goals (set 24 days ago)
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = 24
        )

        // Calculate daily stats
        val dailyStats = calculateDailyStats(sessionsWithQuickReopens)

        // Generate events
        val events = generateEvents(sessionsWithQuickReopens)

        // Generate intervention results with realistic mixed responses
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
                description = "Realistic mixed - Average user, 40-70 min/day, MODERATE friction, 50/40/10 PROCEED/GO_BACK/DISMISSED, varied patterns"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
