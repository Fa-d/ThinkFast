package dev.sadakat.thinkfaster.di

import dev.sadakat.thinkfaster.domain.intervention.AdaptiveContentSelector
import dev.sadakat.thinkfaster.domain.intervention.BurdenTrendMonitor
import dev.sadakat.thinkfaster.domain.intervention.ComprehensiveOutcomeTracker
import dev.sadakat.thinkfaster.domain.intervention.ContextualTimingOptimizer
import dev.sadakat.thinkfaster.domain.intervention.DecisionLogger
import dev.sadakat.thinkfaster.domain.intervention.FatigueRecoveryTracker
import dev.sadakat.thinkfaster.domain.intervention.InterventionBurdenTracker
import dev.sadakat.thinkfaster.domain.intervention.RewardCalculator
import dev.sadakat.thinkfaster.domain.intervention.ThompsonSamplingEngine
import dev.sadakat.thinkfaster.domain.intervention.TimingPatternLearner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Phase 1, 2, 3 & 4: Dependency injection module for intervention components
 *
 * Phase 1 Provides:
 * - ComprehensiveOutcomeTracker: Tracks intervention outcomes across time windows
 * - InterventionBurdenTracker: Monitors user fatigue from interventions
 * - DecisionLogger: Logs complete rationale for intervention decisions
 *
 * Phase 2 Provides:
 * - FatigueRecoveryTracker: Tracks fatigue recovery and grants burden relief
 * - BurdenTrendMonitor: Monitors burden trends and proactive warnings
 *
 * Phase 3 Provides:
 * - ContextualTimingOptimizer: Learns optimal intervention timing from historical data
 * - TimingPatternLearner: Continuously updates timing effectiveness patterns
 *
 * Phase 4 Provides:
 * - ThompsonSamplingEngine: Reinforcement learning for content selection
 * - RewardCalculator: Calculates rewards from intervention outcomes
 * - AdaptiveContentSelector: Intelligently selects intervention content types
 */
val interventionModule = module {
    // Provide a CoroutineScope for ComprehensiveOutcomeTracker
    single {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    // Comprehensive outcome tracker
    single {
        ComprehensiveOutcomeTracker(
            comprehensiveOutcomeDao = get(),
            usageSessionDao = get(),
            scope = get()
        )
    }

    // Phase 2: Fatigue recovery tracker
    single {
        FatigueRecoveryTracker(
            interventionResultDao = get()
        )
    }

    // Phase 2: Burden trend monitor
    single {
        BurdenTrendMonitor(
            preferences = get()
        )
    }

    // Intervention burden tracker (updated for Phase 2)
    single {
        InterventionBurdenTracker(
            interventionResultDao = get(),
            fatigueRecoveryTracker = get(),
            burdenTrendMonitor = get()
        )
    }

    // Decision logger
    single {
        DecisionLogger(
            decisionExplanationDao = get()
        )
    }

    // Phase 3: Contextual timing optimizer
    single {
        ContextualTimingOptimizer(
            interventionResultDao = get()
        )
    }

    // Phase 3: Timing pattern learner
    single {
        TimingPatternLearner(
            preferences = get()
        )
    }

    // Phase 4: Thompson Sampling engine
    single {
        ThompsonSamplingEngine(
            preferences = get()
        )
    }

    // Phase 4: Reward calculator
    single {
        RewardCalculator()
    }

    // Phase 4: Adaptive content selector
    single {
        AdaptiveContentSelector(
            thompsonSampling = get(),
            rewardCalculator = get(),
            interventionResultDao = get()
        )
    }
}
