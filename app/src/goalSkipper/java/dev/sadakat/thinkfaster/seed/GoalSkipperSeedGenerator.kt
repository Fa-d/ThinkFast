package dev.sadakat.thinkfast.seed

import dev.sadakat.thinkfaster.data.local.database.IntentlyDatabase
import dev.sadakat.thinkfaster.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfaster.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfaster.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfaster.data.seed.model.SeedData
import dev.sadakat.thinkfaster.data.seed.model.SeedMetadata

/**
 * Goal Skipper Persona (Days 22)
 *
 * Characteristics:
 * - MODERATE friction (3s delay)
 * - 70-100 min/day, 12-18 sessions
 * - NO goals set (hasGoals = false)
 * - Pure tracking mode - no goal compliance pressure
 * - No gamification content
 * - No streak tracking (currentStreak = 0)
 * - 40% PROCEED, 50% GO_BACK (decent self-control without goals)
 *
 * Goal: Test app behavior when user skips goal setting.
 * This user just wants to track usage without setting limits.
 */
class GoalSkipperSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.GOAL_SKIPPER
) {

    override suspend fun seedDatabase(database: IntentlyDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = 22  // 22 days of data

        // Generate sessions
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (20% - moderate)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // NO goals for this persona!
        // The generateGoals() method checks config.hasGoals and returns emptyList()
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = 22
        )

        // Calculate daily stats
        val dailyStats = calculateDailyStats(sessionsWithQuickReopens)

        // Generate events
        val events = generateEvents(sessionsWithQuickReopens)

        // Generate intervention results
        // No gamification content since no goals/streaks
        val interventionGenerator = InterventionResultGenerator(config, random)
        val interventionResults = interventionGenerator.generateForSessions(
            sessions = sessionsWithQuickReopens,
            quickReopenMap = quickReopenMap,
            currentStreak = 0  // No streak tracking
        )

        // Create seed data
        val seedData = SeedData(
            persona = config.personaType,
            goals = goals,  // Will be empty
            sessions = sessionsWithQuickReopens,
            dailyStats = dailyStats,
            events = events,
            interventionResults = interventionResults,
            metadata = SeedMetadata(
                daysOfData = days,
                targetApps = targetApps,
                description = "Goal skipper - NO goals set, pure tracking mode, 70-100 min/day, MODERATE friction, no streaks"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
