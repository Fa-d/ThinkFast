package dev.sadakat.thinkfaster.di

import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.data.preferences.NotificationPreferences
import dev.sadakat.thinkfaster.data.preferences.OnboardingQuestPreferences
import dev.sadakat.thinkfaster.data.preferences.StreakFreezePreferences
import dev.sadakat.thinkfaster.data.preferences.SyncPreferences
import dev.sadakat.thinkfaster.service.InterventionRateLimiter
import dev.sadakat.thinkfaster.data.repository.GoalRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.InterventionResultRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.SettingsRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.StatsRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.StreakRecoveryRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.TrackedAppsRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.UsageRepositoryImpl
import dev.sadakat.thinkfaster.data.repository.UserBaselineRepositoryImpl
import dev.sadakat.thinkfaster.data.sync.PreferencesChangeListener
import dev.sadakat.thinkfaster.data.sync.PreferencesSerializer
import dev.sadakat.thinkfaster.data.sync.SettingsSyncManager
import dev.sadakat.thinkfaster.data.sync.SyncCoordinator
import dev.sadakat.thinkfaster.data.sync.backend.SupabaseSyncBackend
import dev.sadakat.thinkfaster.data.sync.backend.SyncBackend
import dev.sadakat.thinkfaster.domain.intervention.ContentSelector
import dev.sadakat.thinkfaster.domain.intervention.OpportunityDetector
import dev.sadakat.thinkfaster.domain.intervention.PersonaAwareContentSelector
import dev.sadakat.thinkfaster.domain.intervention.PersonaDetector
import dev.sadakat.thinkfaster.domain.repository.GoalRepository
import dev.sadakat.thinkfaster.domain.repository.InterventionResultRepository
import dev.sadakat.thinkfaster.domain.repository.SettingsRepository
import dev.sadakat.thinkfaster.domain.repository.StatsRepository
import dev.sadakat.thinkfaster.domain.repository.StreakRecoveryRepository
import dev.sadakat.thinkfaster.domain.repository.TrackedAppsRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import dev.sadakat.thinkfaster.domain.repository.UserBaselineRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for repository dependencies
 * Phase F: Added InterventionPreferences for friction level management
 * Phase G: Added InterventionResultRepository for effectiveness tracking
 * Broken Streak Recovery: Added StreakFreezePreferences and StreakRecoveryRepository
 * First-Week Retention: Added OnboardingQuestPreferences and UserBaselineRepository
 */
val repositoryModule = module {

    // InterventionPreferences (singleton)
    single {
        InterventionPreferences.getInstance(androidContext())
    }

    // InterventionRateLimiter (singleton) - Rate limiting for interventions
    single {
        InterventionRateLimiter(
            context = androidContext(),
            interventionPreferences = get()
        )
    }

    // PersonaDetector (singleton) - Phase 2 JITAI: Behavioral user segmentation
    single {
        PersonaDetector(
            interventionResultRepository = get(),
            usageRepository = get(),
            preferences = get()
        )
    }

    // OpportunityDetector (singleton) - Phase 2 JITAI: Smart intervention timing
    single {
        OpportunityDetector(
            interventionRepository = get(),
            preferences = get()
        )
    }

    // PersonaAwareContentSelector (singleton) - Phase 2 JITAI: Personalized content selection
    single {
        PersonaAwareContentSelector(
            personaDetector = get(),
            baseContentSelector = ContentSelector()
        )
    }

    // StreakFreezePreferences (singleton) - Broken Streak Recovery
    single {
        StreakFreezePreferences(androidContext())
    }

    // OnboardingQuestPreferences (singleton) - First-Week Retention
    single {
        OnboardingQuestPreferences(androidContext())
    }

    // NotificationPreferences (singleton) - Push Notification Strategy
    single {
        NotificationPreferences(androidContext())
    }

    // SyncPreferences (singleton) - Phase 5: Multi-device sync
    single {
        SyncPreferences(androidContext())
    }

    // UsageRepository
    single<UsageRepository> {
        UsageRepositoryImpl(
            sessionDao = get(),
            eventDao = get(),
            goalDao = get(),
            interventionPreferences = get()  // Phase F
        )
    }

    // InterventionResultRepository (Phase G)
    single<InterventionResultRepository> {
        InterventionResultRepositoryImpl(
            resultDao = get()
        )
    }

    // StreakRecoveryRepository - Broken Streak Recovery
    single<StreakRecoveryRepository> {
        StreakRecoveryRepositoryImpl(
            streakRecoveryDao = get()
        )
    }

    // UserBaselineRepository - First-Week Retention
    single<UserBaselineRepository> {
        UserBaselineRepositoryImpl(
            baselineDao = get()
        )
    }

    // GoalRepository
    single<GoalRepository> {
        GoalRepositoryImpl(
            goalDao = get()
        )
    }

    // StatsRepository
    single<StatsRepository> {
        StatsRepositoryImpl(
            dailyStatsDao = get(),
            usageSessionDao = get()
        )
    }

    // SettingsRepository
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            context = androidContext()
        )
    }

    // TrackedAppsRepository
    single<TrackedAppsRepository> {
        TrackedAppsRepositoryImpl(
            context = androidContext(),
            goalRepository = get()
        )
    }

    // Phase 6 & 7: Settings Sync Components

    // SyncBackend - Supabase (always available)
    single<SyncBackend> {
        SupabaseSyncBackend(androidContext())
    }

    // PreferencesSerializer - Serializes app preferences for sync
    single {
        PreferencesSerializer(
            context = androidContext(),
            interventionPrefs = get(),
            notificationPrefs = get(),
            questPrefs = get(),
            streakFreezePrefs = get(),
            settingsRepository = get<SettingsRepository>() as SettingsRepositoryImpl
        )
    }

    // SettingsSyncManager - Manages settings sync with debouncing
    single {
        SettingsSyncManager(
            syncBackend = get(),
            preferencesSerializer = get(),
            syncPreferences = get()
        )
    }

    // PreferencesChangeListener - Auto-triggers sync on preference changes
    single {
        PreferencesChangeListener(
            settingsSyncManager = get(),
            syncPreferences = get()
        )
    }

    // SyncCoordinator - Orchestrates sync operations across all entity types
    single {
        SyncCoordinator(
            syncBackend = get(),
            goalDao = get(),
            usageSessionDao = get(),
            usageEventDao = get(),
            dailyStatsDao = get(),
            interventionResultDao = get(),
            streakRecoveryDao = get(),
            userBaselineDao = get()
        )
    }
}
