package dev.sadakat.thinkfaster.di

import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.data.preferences.OnboardingQuestPreferences
import dev.sadakat.thinkfaster.data.preferences.StreakFreezePreferences
import dev.sadakat.thinkfaster.data.repository.GoalRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.InterventionResultRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.SettingsRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.StatsRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.StreakRecoveryRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.TrackedAppsRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.UsageRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.UserBaselineRepositoryImpl
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.repository.InterventionResultRepository
import dev.sadakat.thinkfaster.domain.repository.SettingsRepository
import dev.sadakat.thinkfaster.domain.repository.StatsRepository
import dev.sadakat.thinkfaster.domain.repository.StreakRecoveryRepository
import dev.sadakat.thinkfaster.domain.repository.TrackedAppsRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import dev.sadakat.thinkfaster.domain.repository.UserBaselineRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for repository dependencies
 * Phase F: Added InterventionPreferences for friction level management
 * Phase G: Added InterventionResultRepository for effectiveness tracking
 * Broken Streak Recovery: Added StreakFreezePreferences and StreakRecoveryRepository
 * First-Week Retention: Added OnboardingQuestPreferences and UserBaselineRepository
 */
val repositoryModule = module {

    // InterventionPreferences (singleton)
    single {
        InterventionPreferences.getInstance(androidContext())
    }

    // StreakFreezePreferences (singleton) - Broken Streak Recovery
    single {
        StreakFreezePreferences(androidContext())
    }

    // OnboardingQuestPreferences (singleton) - First-Week Retention
    single {
        OnboardingQuestPreferences(androidContext())
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

    // StreakRecoveryRepository - Broken Streak Recovery
    single<StreakRecoveryRepository> {
        StreakRecoveryRepositoryImpl(
            streakRecoveryDao = get()
        )
    }

    // UserBaselineRepository - First-Week Retention
    single<UserBaselineRepository> {
        UserBaselineRepositoryImpl(
            baselineDao = get()
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

    // TrackedAppsRepository
    single<TrackedAppsRepository> {
        TrackedAppsRepositoryImpl(
            context = androidContext(),
            goalRepository = get()
        )
    }
}
