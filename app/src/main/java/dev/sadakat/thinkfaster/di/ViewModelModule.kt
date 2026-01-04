package dev.sadakat.thinkfaster.di

import dev.sadakat.thinkfaster.presentation.analytics.AnalyticsViewModel
import dev.sadakat.thinkfaster.presentation.auth.AccountManagementViewModel
import dev.sadakat.thinkfaster.presentation.auth.LoginViewModel
import dev.sadakat.thinkfaster.presentation.home.HomeViewModel
import dev.sadakat.thinkfaster.presentation.manageapps.ManageAppsViewModel
import dev.sadakat.thinkfaster.presentation.onboarding.OnboardingViewModel
import dev.sadakat.thinkfaster.presentation.overlay.ReminderOverlayViewModel
import dev.sadakat.thinkfaster.presentation.overlay.TimerOverlayViewModel
import dev.sadakat.thinkfaster.presentation.settings.GoalViewModel
import dev.sadakat.thinkfaster.presentation.stats.StatsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for ViewModels
 * Phase G: Added InterventionResultRepository and AnalyticsViewModel
 * Phase 1.1: Added OnboardingViewModel
 * Phase 1.2: Added HomeViewModel
 * Broken Streak Recovery: Added freeze and recovery use cases to HomeViewModel
 * First-Week Retention: Added quest and baseline use cases to HomeViewModel
 * Per-App Goals: Added tracked apps and context to HomeViewModel
 */
val viewModelModule = module {
    viewModel {
        HomeViewModel(
            context = androidContext(),
            usageRepository = get(),
            goalRepository = get(),
            trackedAppsRepository = get(),
            getStreakFreezeStatusUseCase = get(),
            activateStreakFreezeUseCase = get(),
            getRecoveryProgressUseCase = get(),
            streakRecoveryRepository = get(),
            checkQuickWinMilestonesUseCase = get(),
            getOnboardingQuestStatusUseCase = get(),
            baselineRepository = get(),
            questPreferences = get(),
            analyticsManager = get(),
            getInstalledAppsUseCase = get()
        )
    }
    viewModel { OnboardingViewModel(goalRepository = get(), analyticsManager = get()) }
    viewModel { ReminderOverlayViewModel(usageRepository = get(), resultRepository = get(), analyticsManager = get(), interventionPreferences = get(), rateLimiter = get()) }
    viewModel { TimerOverlayViewModel(usageRepository = get(), resultRepository = get(), analyticsManager = get(), settingsRepository = get(), interventionPreferences = get(), rateLimiter = get()) }
    viewModel { AnalyticsViewModel(resultRepository = get()) }
    viewModel {
        StatsViewModel(
            getDailyStatisticsUseCase = get(),
            getWeeklyStatisticsUseCase = get(),
            getMonthlyStatisticsUseCase = get(),
            getSessionBreakdownUseCase = get(),
            calculateTrendsUseCase = get(),
            getGoalProgressUseCase = get(),
            generateSmartInsightUseCase = get(),
            calculateBehavioralInsightsUseCase = get(),
            calculateInterventionInsightsUseCase = get(),
            generatePredictiveInsightsUseCase = get(),
            calculateComparativeAnalyticsUseCase = get()
        )
    }
    viewModel {
        GoalViewModel(
            setGoalUseCase = get(),
            getGoalProgressUseCase = get(),
            settingsRepository = get(),
            usageRepository = get(),
            trackedAppsRepository = get(),
            getTrackedAppsWithDetailsUseCase = get(),
            goalRepository = get(),
            interventionPreferences = get(),
            analyticsManager = get()
        )
    }
    viewModel {
        ManageAppsViewModel(
            getInstalledAppsUseCase = get(),
            addTrackedAppUseCase = get(),
            removeTrackedAppUseCase = get(),
            trackedAppsRepository = get(),
            getTrackedAppsWithDetailsUseCase = get(),
            getGoalProgressUseCase = get(),
            setGoalUseCase = get()
        )
    }

    // Phase 5: Auth ViewModels
    viewModel {
        LoginViewModel(
            syncPreferences = get(),
            syncBackend = get(),
            syncCoordinator = get(),
            context = androidContext()
        )
    }
    viewModel {
        AccountManagementViewModel(
            syncPreferences = get(),
            syncBackend = get(),
            context = androidContext()
        )
    }
}
