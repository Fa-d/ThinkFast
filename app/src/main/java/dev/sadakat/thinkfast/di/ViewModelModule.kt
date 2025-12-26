package dev.sadakat.thinkfast.di

import dev.sadakat.thinkfast.presentation.overlay.ReminderOverlayViewModel
import dev.sadakat.thinkfast.presentation.overlay.TimerOverlayViewModel
import dev.sadakat.thinkfast.presentation.settings.GoalViewModel
import dev.sadakat.thinkfast.presentation.stats.StatsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for ViewModels
 */
val viewModelModule = module {
    viewModel { ReminderOverlayViewModel(usageRepository = get()) }
    viewModel { TimerOverlayViewModel(usageRepository = get()) }
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
            getGoalProgressUseCase = get()
        )
    }
}
