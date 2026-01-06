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
    version = 8,  // Phase 2 JITAI: Bumped for performance optimization indexes
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

/**
 * Migration from version 5 to 6
 * Phase 2: Adds sync metadata columns to all tables for multi-device sync support
 * Columns added: user_id, sync_status, last_modified, cloud_id
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
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

        // Helper to safely add a column if it doesn't already exist
        fun safeAddColumn(tableName: String, columnName: String, sql: String) {
            if (columnExists(tableName, columnName)) {
                dev.sadakat.thinkfaster.util.ErrorLogger.debug(
                    message = "Column $tableName.$columnName already exists, skipping",
                    context = "MIGRATION_5_6"
                )
                return
            }
            try {
                database.execSQL(sql)
                dev.sadakat.thinkfaster.util.ErrorLogger.debug(
                    message = "Added column $tableName.$columnName",
                    context = "MIGRATION_5_6"
                )
            } catch (e: Exception) {
                dev.sadakat.thinkfaster.util.ErrorLogger.warning(
                    message = "Failed to add column $tableName.$columnName: ${e.message}",
                    context = "MIGRATION_5_6"
                )
            }
        }

        val tables = listOf(
            "goals",
            "usage_sessions",
            "usage_events",
            "daily_stats",
            "intervention_results",
            "streak_recovery",
            "user_baseline"
        )

        tables.forEach { table ->
            // Add sync metadata columns (idempotent - checks if column exists first)
            safeAddColumn(
                table,
                "user_id",
                "ALTER TABLE $table ADD COLUMN user_id TEXT DEFAULT NULL"
            )
            safeAddColumn(
                table,
                "sync_status",
                "ALTER TABLE $table ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING'"
            )
            safeAddColumn(
                table,
                "last_modified",
                "ALTER TABLE $table ADD COLUMN last_modified INTEGER NOT NULL DEFAULT 0"
            )
            safeAddColumn(
                table,
                "cloud_id",
                "ALTER TABLE $table ADD COLUMN cloud_id TEXT DEFAULT NULL"
            )

            // Create indices for sync queries (idempotent with IF NOT EXISTS)
            try {
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_${table}_user_id ON $table(user_id)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_${table}_sync_status ON $table(sync_status)"
                )
            } catch (e: Exception) {
                dev.sadakat.thinkfaster.util.ErrorLogger.warning(
                    message = "Failed to create indices for $table: ${e.message}",
                    context = "MIGRATION_5_6"
                )
            }
        }
    }
}

/**
 * Migration from version 6 to 7
 * Phase 2 JITAI: Adds persona and opportunity tracking columns to intervention_results table
 * Enables JITAI-based smart timing and personalized interventions
 *
 * Columns added:
 * - user_persona: User persona at time of intervention (HEAVY_COMPULSIVE_USER, etc.)
 * - persona_confidence: Confidence level in persona detection (LOW, MEDIUM, HIGH)
 * - opportunity_score: Calculated opportunity score (0-100)
 * - opportunity_level: Opportunity level category (EXCELLENT, GOOD, MODERATE, POOR)
 * - decision_source: Source of intervention decision (OPPORTUNITY_BASED, BASIC_RATE_LIMIT, etc.)
 *
 * PRODUCTION-READY: Uses CREATE TABLE AS + data copy pattern for maximum reliability.
 * This ensures Room's schema validation passes correctly.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Clean up from any previous failed migration attempts
        // Drop the new table if it exists from a previous failed run
        try {
            database.execSQL("DROP TABLE IF EXISTS intervention_results_new")
        } catch (e: Exception) {
            // Ignore if table doesn't exist
        }

        // Step 1: Create the new table with all columns including the new ones
        database.execSQL(
            """
            CREATE TABLE intervention_results_new (
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
                timestamp INTEGER NOT NULL,
                user_id TEXT DEFAULT NULL,
                sync_status TEXT NOT NULL DEFAULT 'PENDING',
                last_modified INTEGER NOT NULL DEFAULT 0,
                cloud_id TEXT DEFAULT NULL,
                user_persona TEXT,
                persona_confidence TEXT,
                opportunity_score INTEGER,
                opportunity_level TEXT,
                decision_source TEXT
            )
            """.trimIndent()
        )

        // Step 2: Copy data from old table to new table
        database.execSQL(
            """
            INSERT INTO intervention_results_new (
                id, sessionId, targetApp, interventionType, contentType,
                hourOfDay, dayOfWeek, isWeekend, isLateNight, sessionCount,
                quickReopen, currentSessionDurationMs, userChoice, timeToShowDecisionMs,
                user_feedback, feedback_timestamp, audio_active, was_snoozed,
                snooze_duration_ms, finalSessionDurationMs, sessionEndedNormally,
                timestamp, user_id, sync_status, last_modified, cloud_id
            )
            SELECT
                id, sessionId, targetApp, interventionType, contentType,
                hourOfDay, dayOfWeek, isWeekend, isLateNight, sessionCount,
                quickReopen, currentSessionDurationMs, userChoice, timeToShowDecisionMs,
                user_feedback, feedback_timestamp, audio_active, was_snoozed,
                snooze_duration_ms, finalSessionDurationMs, sessionEndedNormally,
                timestamp, user_id, sync_status, last_modified, cloud_id
            FROM intervention_results
            """.trimIndent()
        )

        // Step 3: Drop the old table
        database.execSQL("DROP TABLE intervention_results")

        // Step 4: Rename the new table to the original name
        database.execSQL("ALTER TABLE intervention_results_new RENAME TO intervention_results")

        // Step 5: Recreate indices
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_intervention_results_sessionId ON intervention_results(sessionId)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_intervention_results_targetApp ON intervention_results(targetApp)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_intervention_results_contentType ON intervention_results(contentType)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_intervention_results_timestamp ON intervention_results(timestamp)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_intervention_results_user_feedback ON intervention_results(user_feedback)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_intervention_results_user_id ON intervention_results(user_id)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_intervention_results_sync_status ON intervention_results(sync_status)"
        )

        // Step 6: Create new indices for persona analytics
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_intervention_results_user_persona ON intervention_results(user_persona)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_intervention_results_opportunity_score ON intervention_results(opportunity_score)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_intervention_results_opportunity_level ON intervention_results(opportunity_level)"
        )
    }
}

/**
 * Migration from version 7 to 8
 * Phase 2 JITAI Performance Optimization: Reorders columns to match Room's expected schema
 * and adds performance indexes for JITAI analytics queries
 *
 * IMPORTANT: Room validates exact column order. MIGRATION_6_7 added JITAI columns in the
 * middle of the table, but Room expects them at the end to match the Entity field order.
 *
 * SQLite ALTER TABLE cannot reorder columns, so we must recreate the table.
 *
 * Reference: https://developer.android.com/training/data-storage/room/migrating-db-versions
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Disable foreign keys during migration
        database.execSQL("PRAGMA foreign_keys = OFF")

        try {
            // Step 1: Create new table with columns in EXACT order matching Entity
            database.execSQL(
                """
                CREATE TABLE intervention_results_new (
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
                    timestamp INTEGER NOT NULL,
                    user_id TEXT DEFAULT NULL,
                    sync_status TEXT NOT NULL DEFAULT 'PENDING',
                    last_modified INTEGER NOT NULL DEFAULT 0,
                    cloud_id TEXT DEFAULT NULL,
                    user_persona TEXT,
                    persona_confidence TEXT,
                    opportunity_score INTEGER,
                    opportunity_level TEXT,
                    decision_source TEXT
                )
                """.trimIndent()
            )

            // Step 2: Copy all data - must explicitly map columns due to different order
            database.execSQL(
                """
                INSERT INTO intervention_results_new (
                    id, sessionId, targetApp, interventionType, contentType,
                    hourOfDay, dayOfWeek, isWeekend, isLateNight, sessionCount,
                    quickReopen, currentSessionDurationMs, userChoice, timeToShowDecisionMs,
                    user_feedback, feedback_timestamp, audio_active, was_snoozed,
                    snooze_duration_ms, finalSessionDurationMs, sessionEndedNormally,
                    timestamp, user_id, sync_status, last_modified, cloud_id,
                    user_persona, persona_confidence, opportunity_score, opportunity_level, decision_source
                )
                SELECT
                    id, sessionId, targetApp, interventionType, contentType,
                    hourOfDay, dayOfWeek, isWeekend, isLateNight, sessionCount,
                    quickReopen, currentSessionDurationMs, userChoice, timeToShowDecisionMs,
                    user_feedback, feedback_timestamp, audio_active, was_snoozed,
                    snooze_duration_ms, finalSessionDurationMs, sessionEndedNormally,
                    timestamp, user_id, sync_status, last_modified, cloud_id,
                    user_persona, persona_confidence, opportunity_score, opportunity_level, decision_source
                FROM intervention_results
                """.trimIndent()
            )

            // Step 3: Drop old table
            database.execSQL("DROP TABLE intervention_results")

            // Step 4: Rename new table
            database.execSQL("ALTER TABLE intervention_results_new RENAME TO intervention_results")

            // Step 5: Create indexes - drop existing ones first to avoid conflicts
            val existingIndexes = listOf(
                "index_intervention_results_sessionId",
                "index_intervention_results_targetApp",
                "index_intervention_results_contentType",
                "index_intervention_results_timestamp",
                "index_intervention_results_user_feedback",
                "index_intervention_results_user_id",
                "index_intervention_results_sync_status",
                "index_intervention_results_user_persona",
                "index_intervention_results_opportunity_score",
                "index_intervention_results_opportunity_level",
                "index_intervention_results_userChoice",
                "index_intervention_results_targetApp_timestamp",
                "index_intervention_results_user_persona_userChoice"
            )

            for (indexName in existingIndexes) {
                try {
                    database.execSQL("DROP INDEX IF EXISTS $indexName")
                } catch (e: Exception) {
                    // Ignore - index might not exist
                }
            }

            // Recreate all indexes
            database.execSQL("CREATE INDEX index_intervention_results_sessionId ON intervention_results(sessionId)")
            database.execSQL("CREATE INDEX index_intervention_results_targetApp ON intervention_results(targetApp)")
            database.execSQL("CREATE INDEX index_intervention_results_contentType ON intervention_results(contentType)")
            database.execSQL("CREATE INDEX index_intervention_results_timestamp ON intervention_results(timestamp)")
            database.execSQL("CREATE INDEX index_intervention_results_user_feedback ON intervention_results(user_feedback)")
            database.execSQL("CREATE INDEX index_intervention_results_user_id ON intervention_results(user_id)")
            database.execSQL("CREATE INDEX index_intervention_results_sync_status ON intervention_results(sync_status)")
            database.execSQL("CREATE INDEX index_intervention_results_user_persona ON intervention_results(user_persona)")
            database.execSQL("CREATE INDEX index_intervention_results_opportunity_score ON intervention_results(opportunity_score)")
            database.execSQL("CREATE INDEX index_intervention_results_opportunity_level ON intervention_results(opportunity_level)")

            // New performance indexes
            database.execSQL("CREATE INDEX index_intervention_results_userChoice ON intervention_results(userChoice)")
            database.execSQL("CREATE INDEX index_intervention_results_targetApp_timestamp ON intervention_results(targetApp, timestamp)")
            database.execSQL("CREATE INDEX index_intervention_results_user_persona_userChoice ON intervention_results(user_persona, userChoice)")
        } finally {
            // Re-enable foreign keys
            database.execSQL("PRAGMA foreign_keys = ON")
        }
    }
}

