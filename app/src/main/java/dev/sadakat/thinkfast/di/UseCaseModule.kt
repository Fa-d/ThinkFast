package dev.sadakat.thinkfast.di

import dev.sadakat.thinkfast.domain.usecase.apps.AddTrackedAppUseCase
import dev.sadakat.thinkfast.domain.usecase.apps.GetInstalledAppsUseCase
import dev.sadakat.thinkfast.domain.usecase.apps.GetTrackedAppsWithDetailsUseCase
import dev.sadakat.thinkfast.domain.usecase.apps.RemoveTrackedAppUseCase
import dev.sadakat.thinkfast.domain.usecase.goals.GetGoalProgressUseCase
import dev.sadakat.thinkfast.domain.usecase.goals.SetGoalUseCase
import dev.sadakat.thinkfast.domain.usecase.goals.UpdateStreakUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.CalculateTrendsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetDailyStatisticsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetMonthlyStatisticsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetSessionBreakdownUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetWeeklyStatisticsUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for Use Cases
 */
val useCaseModule = module {
    // Statistics use cases
    factory { GetDailyStatisticsUseCase(usageRepository = get()) }
    factory { GetWeeklyStatisticsUseCase(usageRepository = get(), getDailyStatisticsUseCase = get()) }
    factory { GetMonthlyStatisticsUseCase(usageRepository = get(), getDailyStatisticsUseCase = get(), getWeeklyStatisticsUseCase = get()) }
    factory { GetSessionBreakdownUseCase(usageRepository = get()) }
    factory { CalculateTrendsUseCase(getDailyStatisticsUseCase = get(), getWeeklyStatisticsUseCase = get(), getMonthlyStatisticsUseCase = get()) }

    // Goal use cases
    factory { SetGoalUseCase(goalRepository = get()) }
    factory { GetGoalProgressUseCase(goalRepository = get(), usageRepository = get()) }
    factory { UpdateStreakUseCase(goalRepository = get(), usageRepository = get()) }

    // App management use cases
    factory { GetInstalledAppsUseCase(context = androidContext()) }
    factory { AddTrackedAppUseCase(trackedAppsRepository = get(), context = androidContext()) }
    factory { RemoveTrackedAppUseCase(trackedAppsRepository = get(), goalRepository = get()) }
    factory { GetTrackedAppsWithDetailsUseCase(trackedAppsRepository = get(), context = androidContext()) }
}
