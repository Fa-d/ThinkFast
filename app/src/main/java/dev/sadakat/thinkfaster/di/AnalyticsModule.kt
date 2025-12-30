package dev.sadakat.thinkfaster.di

import dev.sadakat.thinkfaster.analytics.AnalyticsManager
import dev.sadakat.thinkfaster.analytics.PrivacySafeAnalytics
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for analytics dependencies
 * Provides privacy-safe analytics functionality
 */
val analyticsModule = module {
    // PrivacySafeAnalytics (singleton for shared preferences)
    single {
        PrivacySafeAnalytics(androidContext())
    }

    // AnalyticsManager (singleton, requires repository)
    single {
        AnalyticsManager(
            context = androidContext(),
            repository = get() // InterventionResultRepository from repositoryModule
        )
    }
}
