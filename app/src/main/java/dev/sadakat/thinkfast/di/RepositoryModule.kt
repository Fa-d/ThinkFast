package dev.sadakat.thinkfast.di

import dev.sadakat.thinkfast.data.repository.GoalRepositoryImpl
import dev.sadakat.thinkfast.data.repository.SettingsRepositoryImpl
import dev.sadakat.thinkfast.data.repository.StatsRepositoryImpl
import dev.sadakat.thinkfast.data.repository.UsageRepositoryImpl
import dev.sadakat.thinkfast.domain.repository.GoalRepository
import dev.sadakat.thinkfast.domain.repository.SettingsRepository
import dev.sadakat.thinkfast.domain.repository.StatsRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for repository dependencies
 */
val repositoryModule = module {

    // UsageRepository
    single<UsageRepository> {
        UsageRepositoryImpl(
            sessionDao = get(),
            eventDao = get()
        )
    }

    // GoalRepository
    single<GoalRepository> {
        GoalRepositoryImpl(
            goalDao = get()
        )
    }

    // StatsRepository
    single<StatsRepository> {
        StatsRepositoryImpl(
            dailyStatsDao = get(),
            usageSessionDao = get()
        )
    }

    // SettingsRepository
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            context = androidContext()
        )
    }
}
