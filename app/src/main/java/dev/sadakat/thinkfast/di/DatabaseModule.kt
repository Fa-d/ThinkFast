package dev.sadakat.thinkfast.di

import androidx.room.Room
import dev.sadakat.thinkfast.data.local.database.MIGRATION_1_2
import dev.sadakat.thinkfast.data.local.database.ThinkFastDatabase
import dev.sadakat.thinkfast.util.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            ThinkFastDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)  // Phase G: Add migration
            .fallbackToDestructiveMigration()  // Fallback for development
            .build()
    }

    single { get<ThinkFastDatabase>().usageSessionDao() }
    single { get<ThinkFastDatabase>().usageEventDao() }
    single { get<ThinkFastDatabase>().dailyStatsDao() }
    single { get<ThinkFastDatabase>().goalDao() }
    single { get<ThinkFastDatabase>().interventionResultDao() }  // Phase G
}
