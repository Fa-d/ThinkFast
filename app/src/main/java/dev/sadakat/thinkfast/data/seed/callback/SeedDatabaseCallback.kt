package dev.sadakat.thinkfast.data.seed.callback

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.sadakat.thinkfast.BuildConfig
import dev.sadakat.thinkfast.data.local.database.ThinkFastDatabase
import dev.sadakat.thinkfast.data.seed.generator.SeedGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

/**
 * Database callback that seeds the database on creation based on build flavor.
 */
class SeedDatabaseCallback(
    private val context: Context
) : RoomDatabase.Callback() {

    companion object {
        private const val TAG = "SeedDatabaseCallback"
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        Log.d(TAG, "Database created, seeding for persona: ${BuildConfig.USER_PERSONA}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database: ThinkFastDatabase by inject(ThinkFastDatabase::class.java)
                val generator = getSeedGenerator()

                Log.d(TAG, "Starting database seeding...")
                generator.seedDatabase(database)
                Log.d(TAG, "Database seeding completed successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "Database seeding failed", e)
                // Don't crash - app can work without seed data
            }
        }
    }

    private fun getSeedGenerator(): SeedGenerator {
        // Use reflection to dynamically load the flavor-specific generator
        // This avoids having direct references to flavor classes in main code
        val className = when (BuildConfig.USER_PERSONA) {
            "PRODUCTION" -> throw IllegalStateException("Production build should not use seed data")
            "FRESH_INSTALL" -> "dev.sadakat.thinkfast.seed.FreshInstallSeedGenerator"
            "EARLY_ADOPTER" -> "dev.sadakat.thinkfast.seed.EarlyAdopterSeedGenerator"
            "TRANSITIONING_USER" -> "dev.sadakat.thinkfast.seed.TransitioningUserSeedGenerator"
            "ESTABLISHED_USER" -> "dev.sadakat.thinkfast.seed.EstablishedUserSeedGenerator"
            "LOCKED_MODE_USER" -> "dev.sadakat.thinkfast.seed.LockedModeUserSeedGenerator"
            "LATE_NIGHT_SCROLLER" -> "dev.sadakat.thinkfast.seed.LateNightScrollerSeedGenerator"
            "WEEKEND_WARRIOR" -> "dev.sadakat.thinkfast.seed.WeekendWarriorSeedGenerator"
            "COMPULSIVE_REOPENER" -> "dev.sadakat.thinkfast.seed.CompulsiveReopenerSeedGenerator"
            "GOAL_SKIPPER" -> "dev.sadakat.thinkfast.seed.GoalSkipperSeedGenerator"
            "OVER_LIMIT_STRUGGLER" -> "dev.sadakat.thinkfast.seed.OverLimitStrugglerSeedGenerator"
            "STREAK_ACHIEVER" -> "dev.sadakat.thinkfast.seed.StreakAchieverSeedGenerator"
            "REALISTIC_MIXED" -> "dev.sadakat.thinkfast.seed.RealisticMixedSeedGenerator"
            "NEW_USER" -> "dev.sadakat.thinkfast.seed.NewUserSeedGenerator"
            else -> throw IllegalStateException("Unknown persona: ${BuildConfig.USER_PERSONA}")
        }

        return try {
            Class.forName(className).newInstance() as SeedGenerator
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "Generator class not found: $className", e)
            throw IllegalStateException("Failed to load seed generator for ${BuildConfig.USER_PERSONA}", e)
        }
    }
}
