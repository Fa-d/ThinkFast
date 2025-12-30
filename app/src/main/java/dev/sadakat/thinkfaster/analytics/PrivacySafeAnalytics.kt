package dev.sadakat.thinkfaster.analytics

import android.content.Context
import android.content.SharedPreferences
import dev.sadakat.thinkfaster.domain.model.InterventionResult
import dev.sadakat.thinkfaster.domain.model.InterventionType
import dev.sadakat.thinkfaster.domain.model.UserChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import androidx.core.content.edit

/**
 * Privacy-Safe Analytics for ThinkFast
 *
 * PRIVACY PROMISE:
 * - All raw data stays on device
 * - Only aggregated summaries are sent to analytics
 * - No personally identifiable information collected
 * - No device identifiers sent
 * - Data is grouped in time buckets (not exact timestamps)
 */
class PrivacySafeAnalytics(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "analytics_prefs",
        Context.MODE_PRIVATE
    )

    /**
     * Check if user has opted in to analytics
     */
    fun isAnalyticsEnabled(): Boolean {
        // Default to true (opt-out model instead of opt-in)
        return prefs.getBoolean(KEY_ANALYTICS_ENABLED, true)
    }

    /**
     * Enable or disable analytics
     * Call this when user changes their preference in settings
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        prefs.edit {putBoolean(KEY_ANALYTICS_ENABLED, enabled)}
    }

    /**
     * Process intervention result locally
     * Returns aggregated data ready to send (no raw user data)
     */
    fun aggregateIntervention(result: InterventionResult): AggregatedInterventionEvent {
        return AggregatedInterventionEvent(
            // Group content into broader categories (not specific content)
            contentCategory = categorizeContent(result.contentType),

            // Intervention type only (no specifics)
            interventionType = result.interventionType.name.lowercase(),

            // Time bucket (e.g., "morning", "afternoon") - not exact hour
            timeOfDay = getTimeOfDay(result.hourOfDay),

            // Day type only
            dayType = if (result.isWeekend) "weekend" else "weekday",

            // User's choice (needed for effectiveness measurement)
            userChoice = result.userChoice.name.lowercase(),

            // Decision time bucket (not exact milliseconds)
            decisionSpeed = getDecisionSpeedBucket(result.timeToShowDecisionMs),

            // Approximate session count bucket
            sessionCountBucket = getSessionCountBucket(result.sessionCount),

            // Day bucket (not exact date - prevents linking events)
            dayBucket = getDayBucket(result.timestamp)
        )
    }

    /**
     * Generate daily summary to send to analytics
     * This is what actually gets sent to Firebase
     */
    suspend fun generateDailySummary(
        allResults: List<InterventionResult>
    ): DailySummary? = withContext(Dispatchers.Default) {
        if (allResults.isEmpty() || !isAnalyticsEnabled()) {
            return@withContext null
        }

        // Group by intervention type
        val byType = allResults.groupBy { it.interventionType }

        // Average decision time (in seconds, rounded)
        val avgDecisionTimeSeconds = allResults
            .map { it.timeToShowDecisionMs }
            .average()
            .let { if (it.isNaN()) 0.0 else it / 1000.0 }
            .toInt()

        DailySummary(
            date = getDayBucket(allResults.first().timestamp),
            totalInterventions = allResults.size,

            // Success rate (GO_BACK = successful intervention)
            successRatePercent = calculateSuccessRate(allResults),

            avgDecisionTimeSeconds = avgDecisionTimeSeconds,

            // Breakdown by intervention type
            reminderStats = byType[InterventionType.REMINDER]?.let { stats ->
                TypeStats(
                    count = stats.size,
                    successRate = calculateSuccessRate(stats)
                )
            },
            timerStats = byType[InterventionType.TIMER]?.let { stats ->
                TypeStats(
                    count = stats.size,
                    successRate = calculateSuccessRate(stats)
                )
            }
        )
    }

    // ========== Helper Methods (Private) ==========

    private fun calculateSuccessRate(results: List<InterventionResult>): Int {
        val goBackCount = results.count { it.userChoice == UserChoice.GO_BACK }
        return if (results.isNotEmpty()) {
            ((goBackCount.toDouble() / results.size) * 100).toInt()
        } else 0
    }

    private fun categorizeContent(contentType: String): String {
        return when {
            contentType.contains("Reflection", ignoreCase = true) -> "reflection"
            contentType.contains("Question", ignoreCase = true) -> "question"
            contentType.contains("Time", ignoreCase = true) -> "time_alternative"
            contentType.contains("Breathing", ignoreCase = true) -> "breathing"
            contentType.contains("Fact", ignoreCase = true) -> "fact"
            else -> "other"
        }
    }

    private fun getTimeOfDay(hour: Int): String {
        return when (hour) {
            in 5..11 -> "morning"
            in 12..17 -> "afternoon"
            in 18..22 -> "evening"
            else -> "night"
        }
    }

    private fun getDecisionSpeedBucket(ms: Long): String {
        val seconds = ms / 1000.0
        return when {
            seconds < 2 -> "instant"
            seconds < 5 -> "quick"
            seconds < 15 -> "moderate"
            else -> "deliberate"
        }
    }

    private fun getSessionCountBucket(count: Int): String {
        return when {
            count == 1 -> "first_session"
            count <= 3 -> "few_sessions"
            count <= 10 -> "moderate_sessions"
            else -> "heavy_usage"
        }
    }

    private fun getDayBucket(timestamp: Long): String {
        // Returns "2025-01-15" format (day precision, not hour/minute)
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return formatter.format(date)
    }

    companion object {
        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
    }
}

/**
 * Aggregated event data (no raw user behavior)
 * This is what we send to analytics - completely anonymized
 */
@Serializable
data class AggregatedInterventionEvent(
    val contentCategory: String,
    val interventionType: String,
    val timeOfDay: String,
    val dayType: String,
    val userChoice: String,
    val decisionSpeed: String,
    val sessionCountBucket: String,
    val dayBucket: String
)

/**
 * Daily summary sent to analytics
 * Contains only aggregated statistics, no individual data
 */
@Serializable
data class DailySummary(
    val date: String,
    val totalInterventions: Int,
    val successRatePercent: Int,
    val avgDecisionTimeSeconds: Int,
    val reminderStats: TypeStats? = null,
    val timerStats: TypeStats? = null
)

@Serializable
data class TypeStats(
    val count: Int,
    val successRate: Int
)
