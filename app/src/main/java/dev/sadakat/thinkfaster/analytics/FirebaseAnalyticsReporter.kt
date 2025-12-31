package dev.sadakat.thinkfaster.analytics

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

/**
 * Firebase Analytics Reporter
 *
 * Sends ONLY aggregated, privacy-safe data to Firebase
 * No raw user behavior or personally identifiable information
 *
 * CRITICAL FIX: Firebase.analytics is now lazy-initialized to prevent
 * initialization timing issues where Firebase isn't ready during Koin startup.
 */
class FirebaseAnalyticsReporter {

    companion object {
        private const val TAG = "FirebaseAnalyticsReporter"
        private const val ANALYTICS_ENABLED_KEY = "firebase_analytics_enabled"
    }

    // Lazy initialization - only creates FirebaseAnalytics instance when first accessed
    // This prevents issues where Firebase isn't initialized yet during app startup
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        Log.d(TAG, "Initializing FirebaseAnalytics instance...")
        try {
            val instance = Firebase.analytics
            Log.d(TAG, "FirebaseAnalytics initialized successfully")
            instance
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseAnalytics", e)
            throw e
        }
    }

    // Track whether analytics collection is enabled
    @Volatile
    private var analyticsEnabled = true

    // Track if we've logged the initialization state
    @Volatile
    private var initLogged = false

    /**
     * Report aggregated intervention event
     */
    fun reportIntervention(event: AggregatedInterventionEvent) {
        if (!analyticsEnabled) {
            Log.w(TAG, "Analytics disabled - skipping intervention event")
            return
        }

        try {
            ensureInitialized()
            Log.d(TAG, "Logging intervention_aggregated: $event")
            firebaseAnalytics.logEvent("intervention_aggregated") {
                param("content_category", event.contentCategory)
                param("intervention_type", event.interventionType)
                param("time_of_day", event.timeOfDay)
                param("day_type", event.dayType)
                param("user_choice", event.userChoice)
                param("decision_speed", event.decisionSpeed)
                param("session_count_bucket", event.sessionCountBucket)
                param("day_bucket", event.dayBucket)
            }
            Log.d(TAG, "intervention_aggregated event logged successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log intervention_aggregated event", e)
        }
    }

    /**
     * Report daily summary
     */
    fun reportDailySummary(summary: DailySummary) {
        if (!analyticsEnabled) {
            Log.w(TAG, "Analytics disabled - skipping daily summary")
            return
        }

        try {
            ensureInitialized()
            Log.d(TAG, "Logging daily_summary for ${summary.date}")
            firebaseAnalytics.logEvent("daily_summary") {
                param("date", summary.date)
                param("total_interventions", summary.totalInterventions.toLong())
                param("success_rate_percent", summary.successRatePercent.toLong())
                param("avg_decision_time_seconds", summary.avgDecisionTimeSeconds.toLong())

                summary.reminderStats?.let {
                    param("reminder_count", it.count.toLong())
                    param("reminder_success_rate", it.successRate.toLong())
                }

                summary.timerStats?.let {
                    param("timer_count", it.count.toLong())
                    param("timer_success_rate", it.successRate.toLong())
                }
            }
            Log.d(TAG, "daily_summary event logged successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log daily_summary event", e)
        }
    }

    /**
     * Report app quality metrics
     * Helps track: app crashes, ANRs, performance issues
     */
    fun reportAppQuality(
        crashType: String? = null,
        anrOccurred: Boolean = false,
        slowRender: Boolean = false
    ) {
        if (!analyticsEnabled) {
            Log.w(TAG, "Analytics disabled - skipping app quality report")
            return
        }

        try {
            ensureInitialized()
            Log.d(TAG, "Logging app_quality: crash=$crashType, anr=$anrOccurred, slow=$slowRender")
            firebaseAnalytics.logEvent("app_quality") {
                crashType?.let { param("crash_type", it) }
                param("anr_occurred", if (anrOccurred) 1L else 0L)
                param("slow_render", if (slowRender) 1L else 0L)
            }
            Log.d(TAG, "app_quality event logged successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log app_quality event", e)
        }
    }

    /**
     * Report content performance
     * Helps understand which content types work better
     */
    fun reportContentPerformance(
        contentCategory: String,
        successRate: Int,
        avgDecisionTime: Int,
        sampleSize: Int
    ) {
        if (!analyticsEnabled) {
            Log.w(TAG, "Analytics disabled - skipping content performance report")
            return
        }

        try {
            ensureInitialized()
            Log.d(TAG, "Logging content_performance: category=$contentCategory, rate=$successRate%")
            firebaseAnalytics.logEvent("content_performance") {
                param("content_category", contentCategory)
                param("success_rate", successRate.toLong())
                param("avg_decision_time", avgDecisionTime.toLong())
                param("sample_size", sampleSize.toLong())
            }
            Log.d(TAG, "content_performance event logged successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log content_performance event", e)
        }
    }

    /**
     * Log analytics consent event
     */
    fun logConsentGiven() {
        if (!analyticsEnabled) {
            Log.w(TAG, "Analytics disabled - skipping consent event")
            return
        }

        try {
            ensureInitialized()
            Log.d(TAG, "Logging analytics_consent_given")
            firebaseAnalytics.logEvent("analytics_consent_given") {}
            Log.d(TAG, "analytics_consent_given event logged successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log analytics_consent_given event", e)
        }
    }

    /**
     * Set user ID for analytics (call once on app init)
     */
    fun setUserId(userId: String) {
        try {
            ensureInitialized()
            Log.d(TAG, "Setting user ID: ${userId.take(8)}***") // Only log first 8 chars for privacy
            firebaseAnalytics.setUserId(userId)
            Log.d(TAG, "User ID set successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user ID", e)
        }
    }

    /**
     * Set user property for app usage pattern
     */
    fun setUserProperty(usageLevel: String) {
        setUserProperty("usage_level", usageLevel)
    }

    /**
     * Set a single user property
     */
    fun setUserProperty(key: String, value: String) {
        if (!analyticsEnabled) {
            Log.w(TAG, "Analytics disabled - skipping user property: $key")
            return
        }

        try {
            ensureInitialized()
            Log.d(TAG, "Setting user property: $key = $value")
            firebaseAnalytics.setUserProperty(key, value)
            Log.d(TAG, "User property set successfully: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user property: $key", e)
        }
    }

    /**
     * Set multiple user properties at once
     */
    fun setUserProperties(properties: Map<String, String>) {
        if (!analyticsEnabled) {
            Log.w(TAG, "Analytics disabled - skipping user properties (count=${properties.size})")
            return
        }

        try {
            ensureInitialized()
            Log.d(TAG, "Setting ${properties.size} user properties")
            properties.forEach { (key, value) ->
                try {
                    firebaseAnalytics.setUserProperty(key, value)
                    Log.d(TAG, "  Set: $key = $value")
                } catch (e: Exception) {
                    Log.e(TAG, "  Failed to set: $key", e)
                }
            }
            Log.d(TAG, "User properties set successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user properties", e)
        }
    }

    /**
     * Log a simple event with no parameters
     */
    fun logEvent(eventName: String) {
        logEvent(eventName, emptyMap())
    }

    /**
     * Log event with parameters
     */
    fun logEvent(eventName: String, params: Map<String, Any>) {
        if (!analyticsEnabled) {
            Log.w(TAG, "Analytics disabled - skipping event: $eventName")
            return
        }

        try {
            ensureInitialized()
            Log.d(TAG, "Logging event: $eventName with ${params.size} params")
            firebaseAnalytics.logEvent(eventName) {
                params.forEach { (key, value) ->
                    when (value) {
                        is String -> param(key, value)
                        is Long -> param(key, value)
                        is Int -> param(key, value.toLong())
                        is Double -> param(key, value)
                        is Boolean -> param(key, if (value) 1L else 0L)
                        else -> {
                            Log.w(TAG, "Unsupported param type for $key: ${value::class.simpleName}")
                        }
                    }
                }
            }
            Log.d(TAG, "Event '$eventName' logged successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event: $eventName", e)
        }
    }

    /**
     * Log screen view event
     */
    fun logScreenView(screenName: String, screenClass: String) {
        if (!analyticsEnabled) {
            Log.w(TAG, "Analytics disabled - skipping screen view: $screenName")
            return
        }

        try {
            ensureInitialized()
            Log.d(TAG, "Logging screen view: $screenName ($screenClass)")
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
            Log.d(TAG, "Screen view logged successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log screen view", e)
        }
    }

    /**
     * Enable or disable analytics collection
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        analyticsEnabled = enabled
        try {
            ensureInitialized()
            Log.i(TAG, "Setting analytics collection enabled: $enabled")
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
            Log.i(TAG, "Analytics collection ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set analytics enabled", e)
        }
    }

    /**
     * Ensures Firebase Analytics is initialized before use.
     * This triggers lazy initialization if not already done.
     */
    private fun ensureInitialized() {
        if (!initLogged) {
            // Access the lazy property to trigger initialization
            val instance = firebaseAnalytics
            initLogged = true
            Log.i(TAG, "FirebaseAnalytics instance ready (app_id: ${instance.toString().take(50)}...)")
        }
    }

    /**
     * Check if analytics is currently enabled
     */
    fun isAnalyticsEnabled(): Boolean = analyticsEnabled
}
