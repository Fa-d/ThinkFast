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
     * Set user property for app usage pattern
     */
    fun setUserProperty(usageLevel: String) {
        firebaseAnalytics.setUserProperty("usage_level", usageLevel)
    }

    /**
     * Enable or disable analytics collection
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
}
