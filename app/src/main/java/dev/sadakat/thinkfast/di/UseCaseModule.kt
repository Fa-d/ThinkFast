package dev.sadakat.thinkfast.di

import dev.sadakat.thinkfast.domain.usecase.apps.AddTrackedAppUseCase
import dev.sadakat.thinkfast.domain.usecase.apps.GetInstalledAppsUseCase
import dev.sadakat.thinkfast.domain.usecase.apps.GetTrackedAppsWithDetailsUseCase
import dev.sadakat.thinkfast.domain.usecase.apps.RemoveTrackedAppUseCase
import dev.sadakat.thinkfast.domain.usecase.goals.GetGoalProgressUseCase
import dev.sadakat.thinkfast.domain.usecase.goals.SetGoalUseCase
import dev.sadakat.thinkfast.domain.usecase.goals.UpdateStreakUseCase
import dev.sadakat.thinkfast.domain.usecase.insights.CalculateBehavioralInsightsUseCase
import dev.sadakat.thinkfast.domain.usecase.insights.CalculateComparativeAnalyticsUseCase
import dev.sadakat.thinkfast.domain.usecase.insights.CalculateInterventionInsightsUseCase
import dev.sadakat.thinkfast.domain.usecase.insights.GeneratePredictiveInsightsUseCase
import dev.sadakat.thinkfast.domain.usecase.insights.GenerateSmartInsightUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.CalculateTrendsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetDailyStatisticsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetMonthlyStatisticsUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetSessionBreakdownUseCase
import dev.sadakat.thinkfast.domain.usecase.stats.GetWeeklyStatisticsUseCase
import dev.sadakat.thinkfast.domain.usecase.streaks.ActivateStreakFreezeUseCase
import dev.sadakat.thinkfast.domain.usecase.streaks.GetRecoveryProgressUseCase
import dev.sadakat.thinkfast.domain.usecase.streaks.GetStreakFreezeStatusUseCase
import dev.sadakat.thinkfast.domain.usecase.streaks.ResetMonthlyFreezesUseCase
import dev.sadakat.thinkfast.domain.usecase.streaks.UpdateStreakWithRecoveryUseCase
import dev.sadakat.thinkfast.domain.usecase.baseline.CalculateUserBaselineUseCase
import dev.sadakat.thinkfast.domain.usecase.quickwins.CheckQuickWinMilestonesUseCase
import dev.sadakat.thinkfast.domain.usecase.quest.CompleteQuestDayUseCase
import dev.sadakat.thinkfast.domain.usecase.quest.GetOnboardingQuestStatusUseCase
import dev.sadakat.thinkfast.util.NotificationHelper
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

    // Phase 5: Smart Insights use cases
    factory { CalculateBehavioralInsightsUseCase(usageRepository = get()) }
    factory { CalculateInterventionInsightsUseCase(interventionResultRepository = get()) }
    factory { GeneratePredictiveInsightsUseCase(usageRepository = get(), goalRepository = get(), getGoalProgressUseCase = get()) }
    factory { CalculateComparativeAnalyticsUseCase(usageRepository = get(), goalRepository = get()) }
    factory {
        GenerateSmartInsightUseCase(
            calculateBehavioralInsightsUseCase = get(),
            calculateInterventionInsightsUseCase = get(),
            generatePredictiveInsightsUseCase = get(),
            calculateComparativeAnalyticsUseCase = get()
        )
    }

    // Broken Streak Recovery use cases
    factory { ActivateStreakFreezeUseCase(freezePreferences = get()) }
    factory { GetStreakFreezeStatusUseCase(freezePreferences = get()) }
    factory { ResetMonthlyFreezesUseCase(freezePreferences = get()) }
    factory { GetRecoveryProgressUseCase(streakRecoveryRepository = get()) }
    factory {
        UpdateStreakWithRecoveryUseCase(
            goalRepository = get(),
            usageRepository = get(),
            streakRecoveryRepository = get(),
            freezePreferences = get(),
            notificationHelper = NotificationHelper,
            context = androidContext()
        )
    }

    // First-Week Retention use cases
    factory {
        CalculateUserBaselineUseCase(
            usageRepository = get(),
            goalRepository = get(),
            baselineRepository = get()
        )
    }
    factory {
        CheckQuickWinMilestonesUseCase(
            usageRepository = get(),
            goalRepository = get(),
            questPreferences = get()
        )
    }
    factory {
        GetOnboardingQuestStatusUseCase(
            questPreferences = get(),
            goalRepository = get()
        )
    }
    factory {
        CompleteQuestDayUseCase(
            questPreferences = get(),
            notificationHelper = NotificationHelper,
            context = androidContext()
        )
    }
}
