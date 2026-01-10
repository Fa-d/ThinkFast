package dev.sadakat.thinkfast.seed

import android.content.Context
import dev.sadakat.thinkfaster.data.local.database.IntentlyDatabase
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.data.seed.config.PersonaConfigurations
import dev.sadakat.thinkfaster.data.seed.generator.BaseSeedGenerator
import dev.sadakat.thinkfaster.data.seed.generator.InterventionResultGenerator
import dev.sadakat.thinkfaster.data.seed.model.SeedData
import dev.sadakat.thinkfaster.data.seed.model.SeedMetadata
import dev.sadakat.thinkfaster.data.seed.util.TimeDistributionHelper
import org.koin.java.KoinJavaComponent.inject

/**
 * Locked Mode User Persona (Days 50+)
 *
 * Characteristics:
 * - LOCKED friction (10s delay - maximum friction)
 * - Very healthy patterns (app is working well)
 * - 15-25 min/day, 3-5 sessions
 * - 20% PROCEED, 70% GO_BACK (excellent self-control)
 * - 30+ day streak (long-term success)
 * - Mostly deliberate decisions (very thoughtful)
 *
 * Special: This user has explicitly enabled Locked Mode for maximum friction.
 * Install date is set to 50+ days ago, and locked mode preferences are enabled.
 *
 * Goal: Power user who wants strict control over their usage.
 */
class LockedModeUserSeedGenerator : BaseSeedGenerator(
    config = PersonaConfigurations.LOCKED_MODE_USER
) {

    override suspend fun seedDatabase(database: IntentlyDatabase) {
        val targetApps = listOf(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)
        val days = random.nextInt(50, 61)  // 50-60 days of data

        // Set install date to 50+ days ago
        val installDate = TimeDistributionHelper.getDaysAgo(days)

        // Get context from Koin (needed for preferences)
        val context: Context by inject(Context::class.java)
        val interventionPrefs = InterventionPreferences.getInstance(context)

        // Force set install date via direct SharedPreferences access
        // (since setInstallDate() only sets if not already set)
        context.getSharedPreferences("intervention_preferences", Context.MODE_PRIVATE)
            .edit()
            .putLong("install_date", installDate)
            .apply()

        // Enable locked mode
        interventionPrefs.setLockedMode(true)

        // Generate sessions
        val sessions = generateSessions(
            config = config,
            days = days,
            targetApps = targetApps
        )

        // Apply quick reopen pattern (only 5% - excellent self-control)
        val sessionsWithQuickReopens = applyQuickReopenPattern(sessions)

        // Detect quick reopens
        val quickReopenMap = detectQuickReopens(sessionsWithQuickReopens)

        // Generate goals (set 50+ days ago)
        val goals = generateGoals(
            targetApps = targetApps,
            startDaysAgo = days
        )

        // Calculate daily stats
        val dailyStats = calculateDailyStats(sessionsWithQuickReopens)

        // Generate events
        val events = generateEvents(sessionsWithQuickReopens)

        // Generate intervention results with very high streak
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
                description = "Locked mode user - Day 50+, LOCKED friction (10s delay), excellent patterns, 30+ day streak"
            )
        )

        // Insert into database
        insertSeedData(database, seedData)
    }
}
