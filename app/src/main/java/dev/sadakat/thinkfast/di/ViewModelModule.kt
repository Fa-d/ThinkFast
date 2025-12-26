package dev.sadakat.thinkfast.di

import dev.sadakat.thinkfast.presentation.overlay.ReminderOverlayViewModel
import dev.sadakat.thinkfast.presentation.overlay.TimerOverlayViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for ViewModels
 */
val viewModelModule = module {
    viewModel { ReminderOverlayViewModel(usageRepository = get()) }
    viewModel { TimerOverlayViewModel(usageRepository = get()) }
}
