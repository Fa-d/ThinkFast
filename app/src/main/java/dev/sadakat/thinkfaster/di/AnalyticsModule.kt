package dev.sadakat.thinkfaster.di

import dev.sadakat.thinkfaster.analytics.AnalyticsManager
import dev.sadakat.thinkfaster.analytics.FirebaseAnalyticsReporter
import dev.sadakat.thinkfaster.analytics.PrivacySafeAnalytics
import dev.sadakat.thinkfaster.analytics.UserPropertiesManager
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for analytics dependencies
 * Provides privacy-safe analytics functionality
 */
val analyticsModule = module {
    // FirebaseAnalyticsReporter (singleton)
    single {
        FirebaseAnalyticsReporter()
    }

    // PrivacySafeAnalytics (singleton for shared preferences)
    single {
        PrivacySafeAnalytics(androidContext())
    }

    // UserPropertiesManager (singleton)
    single {
        UserPropertiesManager(
            firebaseReporter = get(),
            usageRepository = get(),
            goalRepository = get(),
            interventionPreferences = InterventionPreferences(androidContext())
        )
    }

    // AnalyticsManager (singleton, requires repository)
    single {
        AnalyticsManager(
            context = androidContext(),
            repository = get(), // InterventionResultRepository from repositoryModule
            userPropertiesManager = get()
        )
    }
}
