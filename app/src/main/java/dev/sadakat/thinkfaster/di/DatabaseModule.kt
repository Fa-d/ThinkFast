package dev.sadakat.thinkfaster.di

import androidx.room.Room
import dev.sadakat.thinkfaster.BuildConfig
import dev.sadakat.thinkfaster.data.local.database.MIGRATION_1_2
import dev.sadakat.thinkfaster.data.local.database.MIGRATION_2_3
import dev.sadakat.thinkfaster.data.local.database.MIGRATION_3_4
import dev.sadakat.thinkfaster.data.local.database.MIGRATION_4_5
import dev.sadakat.thinkfaster.data.local.database.MIGRATION_5_6
import dev.sadakat.thinkfaster.data.local.database.MIGRATION_6_7
import dev.sadakat.thinkfaster.data.local.database.MIGRATION_7_8
import dev.sadakat.thinkfaster.data.local.database.MIGRATION_8_9
import dev.sadakat.thinkfaster.data.local.database.IntentlyDatabase
import dev.sadakat.thinkfaster.data.seed.callback.SeedDatabaseCallback
import dev.sadakat.thinkfaster.util.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        val builder = Room.databaseBuilder(
            androidContext(), IntentlyDatabase::class.java, Constants.DATABASE_NAME
        ).addMigrations(
            MIGRATION_1_2,  // Phase G: Add intervention results table
            MIGRATION_2_3,  // Broken Streak Recovery: Add streak recovery table
            MIGRATION_3_4,  // First-Week Retention: Add user baseline table
            MIGRATION_4_5,  // Phase 1: Add feedback and context fields for ML training
            MIGRATION_5_6,  // Phase 2: Add sync metadata columns for multi-device sync
            MIGRATION_6_7,  // Phase 2 JITAI: Add persona and opportunity tracking columns
            MIGRATION_7_8,  // Phase 2 JITAI: Add performance optimization indexes
            MIGRATION_8_9   // Phase 1: Add comprehensive outcomes and decision explanations
        ).fallbackToDestructiveMigrationOnDowngrade()  // Production-safe: only fallback on downgrade

        // Only add seed callback for non-production builds
        if (BuildConfig.USER_PERSONA != "PRODUCTION") {
            builder.addCallback(SeedDatabaseCallback(androidContext()))
        }

        builder.build()
    }

    single { get<IntentlyDatabase>().usageSessionDao() }
    single { get<IntentlyDatabase>().usageEventDao() }
    single { get<IntentlyDatabase>().dailyStatsDao() }
    single { get<IntentlyDatabase>().goalDao() }
    single { get<IntentlyDatabase>().interventionResultDao() }  // Phase G
    single { get<IntentlyDatabase>().streakRecoveryDao() }  // Broken Streak Recovery
    single { get<IntentlyDatabase>().userBaselineDao() }  // First-Week Retention
    single { get<IntentlyDatabase>().comprehensiveOutcomeDao() }  // Phase 1
    single { get<IntentlyDatabase>().decisionExplanationDao() }  // Phase 1
}
