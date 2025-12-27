package dev.sadakat.thinkfast.di

import dev.sadakat.thinkfast.data.preferences.InterventionPreferences
import dev.sadakat.thinkfast.data.repository.GoalRepositoryImpl
import dev.sadakat.thinkfast.data.repository.InterventionResultRepositoryImpl
import dev.sadakat.thinkfast.data.repository.SettingsRepositoryImpl
import dev.sadakat.thinkfast.data.repository.StatsRepositoryImpl
import dev.sadakat.thinkfast.data.repository.UsageRepositoryImpl
import dev.sadakat.thinkfast.domain.repository.GoalRepository
import dev.sadakat.thinkfast.domain.repository.InterventionResultRepository
import dev.sadakat.thinkfast.domain.repository.SettingsRepository
import dev.sadakat.thinkfast.domain.repository.StatsRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for repository dependencies
 * Phase F: Added InterventionPreferences for friction level management
 * Phase G: Added InterventionResultRepository for effectiveness tracking
 */
val repositoryModule = module {

    // InterventionPreferences (singleton)
    single {
        InterventionPreferences.getInstance(androidContext())
    }

    // UsageRepository
    single<UsageRepository> {
        UsageRepositoryImpl(
            sessionDao = get(),
            eventDao = get(),
            goalDao = get(),
            interventionPreferences = get()  // Phase F
        )
    }

    // InterventionResultRepository (Phase G)
    single<InterventionResultRepository> {
        InterventionResultRepositoryImpl(
            resultDao = get()
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
