package dev.sadakat.thinkfast.di

import dev.sadakat.thinkfast.presentation.analytics.AnalyticsViewModel
import dev.sadakat.thinkfast.presentation.home.HomeViewModel
import dev.sadakat.thinkfast.presentation.onboarding.OnboardingViewModel
import dev.sadakat.thinkfast.presentation.overlay.ReminderOverlayViewModel
import dev.sadakat.thinkfast.presentation.overlay.TimerOverlayViewModel
import dev.sadakat.thinkfast.presentation.settings.GoalViewModel
import dev.sadakat.thinkfast.presentation.stats.StatsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for ViewModels
 * Phase G: Added InterventionResultRepository and AnalyticsViewModel
 * Phase 1.1: Added OnboardingViewModel
 * Phase 1.2: Added HomeViewModel
 */
val viewModelModule = module {
    viewModel { HomeViewModel(usageRepository = get(), goalRepository = get()) }
    viewModel { OnboardingViewModel(goalRepository = get()) }
    viewModel { ReminderOverlayViewModel(usageRepository = get(), resultRepository = get(), analyticsManager = get()) }
    viewModel { TimerOverlayViewModel(usageRepository = get(), resultRepository = get(), analyticsManager = get()) }
    viewModel { AnalyticsViewModel(resultRepository = get()) }
    viewModel {
        StatsViewModel(
            getDailyStatisticsUseCase = get(),
            getWeeklyStatisticsUseCase = get(),
            getMonthlyStatisticsUseCase = get(),
            getSessionBreakdownUseCase = get(),
            calculateTrendsUseCase = get()
        )
    }
    viewModel {
        GoalViewModel(
            setGoalUseCase = get(),
            getGoalProgressUseCase = get(),
            settingsRepository = get(),
            usageRepository = get()
        )
    }
}
