package dev.sadakat.thinkfast.seed

import dev.sadakat.thinkfast.data.local.database.ThinkFastDatabase
import dev.sadakat.thinkfast.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfast.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfast.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfast.data.seed.model.SeedData
import dev.sadakat.thinkfast.data.seed.model.SeedMetadata

/**
 * Streak Achiever Persona (Days 45)
 *
 * Characteristics:
 * - FIRM friction (5s delay)
 * - 18-30 min/day, 4-6 sessions
 * - 25-35 day current streak (excellent!)
 * - 50-80% of goal (under goal, healthy usage)
 * - 65% GO_BACK rate (excellent self-control)
 * - High Gamification content (25% weight in interventions)
 * - Motivated by streaks and achievements
 *
 * Goal: Test gamification content effectiveness with engaged users.
 * This user is motivated by streak tracking and achievement badges.
 */
class StreakAchieverSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.STREAK_ACHIEVER
) {

    override suspend fun seedDatabase(database: ThinkFastDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = 45  // 45 days of data

        // Generate sessions
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (10% - very low, good self-control)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // Generate goals (set 45 days ago)
        // The generateGoals() method will create normal goals (60-120 min)
        // User stays under goal (50-80% compliance)
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = 45
        )

        // Calculate daily stats
        val dailyStats = calculateDailyStats(sessionsWithQuickReopens)

        // Generate events
        val events = generateEvents(sessionsWithQuickReopens)

        // Generate intervention results
        // With currentStreak >= 7, gamification content gets 25% weight
        val interventionGenerator = InterventionResultGenerator(config, random)
        val interventionResults = interventionGenerator.generateForSessions(
            sessions = sessionsWithQuickReopens,
            quickReopenMap = quickReopenMap,
            currentStreak = goals.firstOrNull()?.currentStreak ?: 0  // Will be 25-35
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
                description = "Streak achiever - 25-35 day streak, 18-30 min/day (50-80% of goal), FIRM friction, 65% GO_BACK, gamification content"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
