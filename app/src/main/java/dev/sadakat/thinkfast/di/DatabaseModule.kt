package dev.sadakat.thinkfast.di

import androidx.room.Room
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
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<ThinkFastDatabase>().usageSessionDao() }
    single { get<ThinkFastDatabase>().usageEventDao() }
    single { get<ThinkFastDatabase>().dailyStatsDao() }
    single { get<ThinkFastDatabase>().goalDao() }
}
