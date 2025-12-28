package dev.sadakat.thinkfast.di

import androidx.room.Room
import dev.sadakat.thinkfast.BuildConfig
import dev.sadakat.thinkfast.data.local.database.MIGRATION_1_2
import dev.sadakat.thinkfast.data.local.database.ThinkFastDatabase
import dev.sadakat.thinkfast.data.seed.callback.SeedDatabaseCallback
import dev.sadakat.thinkfast.util.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        val builder = Room.databaseBuilder(
            androidContext(), ThinkFastDatabase::class.java, Constants.DATABASE_NAME
        ).addMigrations(MIGRATION_1_2)  // Phase G: Add migration
            .fallbackToDestructiveMigration()  // Fallback for development

        // Only add seed callback for non-production builds
        if (BuildConfig.USER_PERSONA != "PRODUCTION") {
            builder.addCallback(SeedDatabaseCallback(androidContext()))
        }

        builder.build()
    }

    single { get<ThinkFastDatabase>().usageSessionDao() }
    single { get<ThinkFastDatabase>().usageEventDao() }
    single { get<ThinkFastDatabase>().dailyStatsDao() }
    single { get<ThinkFastDatabase>().goalDao() }
    single { get<ThinkFastDatabase>().interventionResultDao() }  // Phase G
}
