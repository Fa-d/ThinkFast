package dev.sadakat.thinkfaster.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

/**
 * Firebase Analytics Reporter
 *
 * Sends ONLY aggregated, privacy-safe data to Firebase
 * No raw user behavior or personally identifiable information
 */
class FirebaseAnalyticsReporter {

    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    /**
     * Report aggregated intervention event
     */
    fun reportIntervention(event: AggregatedInterventionEvent) {
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
    }

    /**
     * Report daily summary
     */
    fun reportDailySummary(summary: DailySummary) {
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
        firebaseAnalytics.logEvent("app_quality") {
            crashType?.let { param("crash_type", it) }
            param("anr_occurred", if (anrOccurred) 1L else 0L)
            param("slow_render", if (slowRender) 1L else 0L)
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
        firebaseAnalytics.logEvent("content_performance") {
            param("content_category", contentCategory)
            param("success_rate", successRate.toLong())
            param("avg_decision_time", avgDecisionTime.toLong())
            param("sample_size", sampleSize.toLong())
        }
    }

    /**
     * Log analytics consent event
     */
    fun logConsentGiven() {
        firebaseAnalytics.logEvent("analytics_consent_given") {}
    }

    /**
     * Set user ID for analytics (call once on app init)
     */
    fun setUserId(userId: String) {
        firebaseAnalytics.setUserId(userId)
    }

    /**
     * Set user property for app usage pattern
     */
    fun setUserProperty(usageLevel: String) {
        firebaseAnalytics.setUserProperty("usage_level", usageLevel)
    }

    /**
     * Set a single user property
     */
    fun setUserProperty(key: String, value: String) {
        firebaseAnalytics.setUserProperty(key, value)
    }

    /**
     * Set multiple user properties at once
     */
    fun setUserProperties(properties: Map<String, String>) {
        properties.forEach { (key, value) ->
            firebaseAnalytics.setUserProperty(key, value)
        }
    }

    /**
     * Log a simple event with no parameters
     */
    fun logEvent(eventName: String) {
        firebaseAnalytics.logEvent(eventName) {}
    }

    /**
     * Log event with parameters
     */
    fun logEvent(eventName: String, params: Map<String, Any>) {
        firebaseAnalytics.logEvent(eventName) {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Double -> param(key, value)
                    is Boolean -> param(key, if (value) 1L else 0L)
                }
            }
        }
    }

    /**
     * Log screen view event
     */
    fun logScreenView(screenName: String, screenClass: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }

    /**
     * Enable or disable analytics collection
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
}
