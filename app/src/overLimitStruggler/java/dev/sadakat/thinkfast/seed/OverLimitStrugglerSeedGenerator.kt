package dev.sadakat.thinkfaster.seed

import dev.sadakat.thinkfaster.data.local.database.IntentlyDatabase
import dev.sadakat.thinkfaster.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfaster.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfaster.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfaster.data.seed.model.SeedData
import dev.sadakat.thinkfaster.data.seed.model.SeedMetadata

/**
 * Over-Limit Struggler Persona (Days 28)
 *
 * Characteristics:
 * - FIRM friction (5s delay)
 * - 90-120 min/day, 10-15 sessions
 * - Goal: 45 min/day (but using 150-200% of goal)
 * - 30-40% extended sessions (15+ min)
 * - 60% PROCEED rate (struggling to comply)
 * - Many isOverGoal intervention contexts
 * - 5-10 day streak (struggling but trying)
 *
 * Goal: Test over-goal warnings and time alternative content effectiveness.
 * This user has set goals but consistently exceeds them.
 */
class OverLimitStrugglerSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.OVER_LIMIT_STRUGGLER
) {

    override suspend fun seedDatabase(database: IntentlyDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = 28  // 28 days of data

        // Generate sessions
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (25% - moderate)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // Generate goals (set 28 days ago)
        // The generateGoals() method will create lower goals (45 min)
        // because goalComplianceRate > 1.5 (user is over-limit)
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = 28
        )

        // Calculate daily stats
        val dailyStats = calculateDailyStats(sessionsWithQuickReopens)

        // Generate events
        val events = generateEvents(sessionsWithQuickReopens)

        // Generate intervention results
        // Many will have isOverGoal context, triggering time alternative content
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
                description = "Over-limit struggler - 90-120 min/day vs 45 min goal (150-200%), FIRM friction, 60% PROCEED, struggling"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
