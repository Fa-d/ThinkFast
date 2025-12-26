package dev.sadakat.thinkfast.di

import dev.sadakat.thinkfast.domain.usecase.stats.CalculateTrendsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetDailyStatisticsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetMonthlyStatisticsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetSessionBreakdownUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetWeeklyStatisticsUseCase
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
}
