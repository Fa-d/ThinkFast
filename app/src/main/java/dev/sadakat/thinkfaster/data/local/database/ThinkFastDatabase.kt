package dev.sadakat.thinkfaster.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.sadakat.thinkfaster.data.local.database.dao.DailyStatsDao
import dev.sadakat.thinkfaster.data.local.database.dao.GoalDao
import dev.sadakat.thinkfaster.data.local.database.dao.InterventionResultDao
import dev.sadakat.thinkfaster.data.local.database.dao.StreakRecoveryDao
import dev.sadakat.thinkfaster.data.local.database.dao.UserBaselineDao
import dev.sadakat.thinkfaster.data.local.database.dao.UsageEventDao
import dev.sadakat.thinkfaster.data.local.database.dao.UsageSessionDao
import dev.sadakat.thinkfaster.data.local.database.entities.DailyStatsEntity
import dev.sadakat.thinkfaster.data.local.database.entities.GoalEntity
import dev.sadakat.thinkfaster.data.local.database.entities.InterventionResultEntity
import dev.sadakat.thinkfaster.data.local.database.entities.StreakRecoveryEntity
import dev.sadakat.thinkfaster.data.local.database.entities.UserBaselineEntity
import dev.sadakat.thinkfaster.data.local.database.entities.UsageEventEntity
import dev.sadakat.thinkfaster.data.local.database.entities.UsageSessionEntity

@Database(
    entities = [
        UsageSessionEntity::class,
        UsageEventEntity::class,
        DailyStatsEntity::class,
        GoalEntity::class,
        InterventionResultEntity::class,  // Phase G: Added for effectiveness tracking
        StreakRecoveryEntity::class,  // Broken Streak Recovery feature
        UserBaselineEntity::class  // First-Week Retention feature
    ],
    version = 5,  // Phase 1: Bumped for feedback system
    exportSchema = true
)
abstract class ThinkFastDatabase : RoomDatabase() {
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun usageEventDao(): UsageEventDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun goalDao(): GoalDao
    abstract fun interventionResultDao(): InterventionResultDao  // Phase G
    abstract fun streakRecoveryDao(): StreakRecoveryDao  // Broken Streak Recovery
    abstract fun userBaselineDao(): UserBaselineDao  // First-Week Retention
}

/**
 * Migration from version 1 to 2
 * Adds the intervention_results table for Phase G: Effectiveness Tracking
 *
 * NOTE: This creates the table with ALL columns including those added in later migrations.
 * This ensures the schema matches InterventionResultEntity exactly, allowing Room to validate properly.
 *
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create intervention_results table with complete schema (includes Phase 1 columns)
        // Using TEXT for nullable Booleans to match Room's handling of Boolean? columns
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS intervention_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                targetApp TEXT NOT NULL,
                interventionType TEXT NOT NULL,
                contentType TEXT NOT NULL,
                hourOfDay INTEGER NOT NULL,
                dayOfWeek INTEGER NOT NULL,
                isWeekend INTEGER NOT NULL,
                isLateNight INTEGER NOT NULL,
                sessionCount INTEGER NOT NULL,
                quickReopen INTEGER NOT NULL,
                currentSessionDurationMs INTEGER NOT NULL,
                userChoice TEXT NOT NULL,
                timeToShowDecisionMs INTEGER NOT NULL,
                user_feedback TEXT NOT NULL DEFAULT 'NONE',
                feedback_timestamp INTEGER,
                audio_active INTEGER NOT NULL DEFAULT 0,
                was_snoozed INTEGER NOT NULL DEFAULT 0,
                snooze_duration_ms INTEGER,
                finalSessionDurationMs INTEGER,
                sessionEndedNormally INTEGER,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Create index for faster queries
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_intervention_results_sessionId
            ON intervention_results(sessionId)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_intervention_results_targetApp
            ON intervention_results(targetApp)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_intervention_results_contentType
            ON intervention_results(contentType)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_intervention_results_timestamp
            ON intervention_results(timestamp)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_intervention_results_user_feedback
            ON intervention_results(user_feedback)
            """.trimIndent()
        )
    }
}

/**
 * Migration from version 2 to 3
 * Adds the streak_recovery table for Broken Streak Recovery feature
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create streak_recovery table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS streak_recovery (
                targetApp TEXT PRIMARY KEY NOT NULL,
                previousStreak INTEGER NOT NULL,
                recoveryStartDate TEXT NOT NULL,
                currentRecoveryDays INTEGER NOT NULL,
                isRecoveryComplete INTEGER NOT NULL,
                recoveryCompletedDate TEXT,
                notificationShown INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Create index for faster queries on recovery status
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_streak_recovery_isRecoveryComplete
            ON streak_recovery(isRecoveryComplete)
            """.trimIndent()
        )
    }
}

/**
 * Migration from version 3 to 4
 * Adds the user_baseline table for First-Week Retention feature
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create user_baseline table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_baseline (
                id INTEGER PRIMARY KEY NOT NULL,
                firstWeekStartDate TEXT NOT NULL,
                firstWeekEndDate TEXT NOT NULL,
                totalUsageMinutes INTEGER NOT NULL,
                averageDailyMinutes INTEGER NOT NULL,
                facebookAverageMinutes INTEGER NOT NULL,
                instagramAverageMinutes INTEGER NOT NULL,
                calculatedDate TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * Migration from version 4 to 5
 * Phase 1: Adds feedback and context fields to intervention_results table
 * Enables ML-based timing optimization by collecting user feedback
 *
 * NOTE: This migration is now idempotent - it checks if columns exist before adding them.
 * This handles cases where the table was created with MIGRATION_1_2 (which already includes these columns).
 *
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Helper function to check if column exists
        fun columnExists(tableName: String, columnName: String): Boolean {
            val cursor = database.query("PRAGMA table_info($tableName)")
            cursor.use {
                while (it.moveToNext()) {
                    if (it.getString(it.getColumnIndexOrThrow("name")) == columnName) {
                        return true
                    }
                }
            }
            return false
        }

        // Add columns only if they don't already exist (idempotent)
        if (!columnExists("intervention_results", "user_feedback")) {
            database.execSQL(
                "ALTER TABLE intervention_results ADD COLUMN user_feedback TEXT NOT NULL DEFAULT 'NONE'"
            )
        }

        if (!columnExists("intervention_results", "feedback_timestamp")) {
            database.execSQL(
                "ALTER TABLE intervention_results ADD COLUMN feedback_timestamp INTEGER"
            )
        }

        if (!columnExists("intervention_results", "audio_active")) {
            database.execSQL(
                "ALTER TABLE intervention_results ADD COLUMN audio_active INTEGER NOT NULL DEFAULT 0"
            )
        }

        if (!columnExists("intervention_results", "was_snoozed")) {
            database.execSQL(
                "ALTER TABLE intervention_results ADD COLUMN was_snoozed INTEGER NOT NULL DEFAULT 0"
            )
        }

        if (!columnExists("intervention_results", "snooze_duration_ms")) {
            database.execSQL(
                "ALTER TABLE intervention_results ADD COLUMN snooze_duration_ms INTEGER"
            )
        }

        // Create index on user_feedback for analytics queries (idempotent with IF NOT EXISTS)
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_intervention_results_user_feedback
            ON intervention_results(user_feedback)
            """.trimIndent()
        )
    }
}

