package dev.sadakat.thinkfaster.di

import dev.sadakat.thinkfaster.domain.intervention.ComprehensiveOutcomeTracker
import dev.sadakat.thinkfaster.domain.intervention.DecisionLogger
import dev.sadakat.thinkfaster.domain.intervention.InterventionBurdenTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Phase 1: Dependency injection module for intervention components
 *
 * Provides:
 * - ComprehensiveOutcomeTracker: Tracks intervention outcomes across time windows
 * - InterventionBurdenTracker: Monitors user fatigue from interventions
 * - DecisionLogger: Logs complete rationale for intervention decisions
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

    // Intervention burden tracker
    single {
        InterventionBurdenTracker(
            interventionResultDao = get()
        )
    }

    // Decision logger
    single {
        DecisionLogger(
            decisionExplanationDao = get()
        )
    }
}
