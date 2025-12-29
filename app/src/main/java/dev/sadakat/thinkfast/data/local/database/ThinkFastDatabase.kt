package dev.sadakat.thinkfast.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.sadakat.thinkfast.data.local.database.dao.DailyStatsDao
import dev.sadakat.thinkfast.data.local.database.dao.GoalDao
import dev.sadakat.thinkfast.data.local.database.dao.InterventionResultDao
import dev.sadakat.thinkfast.data.local.database.dao.StreakRecoveryDao
import dev.sadakat.thinkfast.data.local.database.dao.UsageEventDao
import dev.sadakat.thinkfast.data.local.database.dao.UsageSessionDao
import dev.sadakat.thinkfast.data.local.database.entities.DailyStatsEntity
import dev.sadakat.thinkfast.data.local.database.entities.GoalEntity
import dev.sadakat.thinkfast.data.local.database.entities.InterventionResultEntity
import dev.sadakat.thinkfast.data.local.database.entities.StreakRecoveryEntity
import dev.sadakat.thinkfast.data.local.database.entities.UsageEventEntity
import dev.sadakat.thinkfast.data.local.database.entities.UsageSessionEntity

@Database(
    entities = [
        UsageSessionEntity::class,
        UsageEventEntity::class,
        DailyStatsEntity::class,
        GoalEntity::class,
        InterventionResultEntity::class,  // Phase G: Added for effectiveness tracking
        StreakRecoveryEntity::class  // Broken Streak Recovery feature
    ],
    version = 3,
    exportSchema = true
)
abstract class ThinkFastDatabase : RoomDatabase() {
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun usageEventDao(): UsageEventDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun goalDao(): GoalDao
    abstract fun interventionResultDao(): InterventionResultDao  // Phase G
    abstract fun streakRecoveryDao(): StreakRecoveryDao  // Broken Streak Recovery
}

/**
 * Migration from version 1 to 2
 * Adds the intervention_results table for Phase G: Effectiveness Tracking
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create intervention_results table
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

