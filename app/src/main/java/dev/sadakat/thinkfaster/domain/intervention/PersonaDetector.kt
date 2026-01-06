package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.domain.repository.InterventionResultRepository
import dev.sadakat.thinkfaster.domain.repository.UsageRepository
import dev.sadakat.thinkfaster.domain.model.*
import dev.sadakat.thinkfaster.util.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Detected persona with confidence level
 * Phase 2 JITAI: Result of persona detection analysis
 */
data class DetectedPersona(
    val persona: UserPersona,
    val confidence: ConfidenceLevel,
    val analytics: PersonaAnalytics,
    val detectedAt: Long = System.currentTimeMillis()
)

/**
 * Analytics data for persona detection
 * Phase 2 JITAI: Behavioral metrics used for persona classification
 */
data class PersonaAnalytics(
    val daysSinceInstall: Int,
    val totalSessions: Int,
    val avgDailySessions: Double,
    val avgSessionLengthMin: Double,
    val quickReopenRate: Double,
    val usageTrend: UsageTrendType,
    val lastAnalysisDate: String
)

/**
 * Persona Detector
 * Phase 2 JITAI: Analyzes user behavior to detect behavioral persona
 *
 * Uses intervention and usage data to automatically segment users into personas
 * for personalized intervention strategies.
 *
 * Caching: Results are cached for 6 hours to avoid repeated analysis
 */
class PersonaDetector(
    private val interventionResultRepository: InterventionResultRepository,
    private val usageRepository: UsageRepository,
    private val preferences: InterventionPreferences
) {

    companion object {
        private const val CACHE_DURATION_MS = 6 * 60 * 60 * 1000L // 6 hours
        private const val MIN_DAYS_FOR_ANALYSIS = 3
        private const val OPTIMAL_DAYS_FOR_ANALYSIS = 14

        // Cached detection result
        private var cachedDetection: DetectedPersona? = null
        private var cacheTimestamp: Long = 0L
    }

    /**
     * Detect user persona with caching
     * @param forceRefresh Force re-analysis even if cache is valid
     * @return Detected persona with confidence level
     */
    suspend fun detectPersona(forceRefresh: Boolean = false): DetectedPersona = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // Return cached result if still valid
        if (!forceRefresh && cachedDetection != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
            ErrorLogger.debug(
                message = "Using cached persona: ${cachedDetection?.persona?.name}",
                context = "PersonaDetector"
            )
            return@withContext cachedDetection!!
        }

        // Perform fresh analysis
        val analytics = gatherAnalytics()
        val persona = UserPersona.detect(
            daysSinceInstall = analytics.daysSinceInstall,
            avgDailySessions = analytics.avgDailySessions,
            avgSessionLengthMin = analytics.avgSessionLengthMin,
            quickReopenRate = analytics.quickReopenRate,
            usageTrend = analytics.usageTrend
        )
        val confidence = calculateConfidence(analytics.daysSinceInstall)

        val detected = DetectedPersona(
            persona = persona,
            confidence = confidence,
            analytics = analytics,
            detectedAt = now
        )

        // Cache the result
        cachedDetection = detected
        cacheTimestamp = now

        ErrorLogger.info(
            message = "Detected persona: ${persona.name} ($confidence) - ${persona.displayName}",
            context = "PersonaDetector"
        )

        detected
    }

    /**
     * Gather behavioral analytics for persona detection
     */
    private suspend fun gatherAnalytics(): PersonaAnalytics {
        val installDate = preferences.getInstallDate()
        val daysSinceInstall = if (installDate > 0) {
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - installDate).toInt()
        } else {
            0
        }

        // Determine analysis date range
        val analysisDays = minOf(
            OPTIMAL_DAYS_FOR_ANALYSIS,
            maxOf(MIN_DAYS_FOR_ANALYSIS, daysSinceInstall)
        )

        val endDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val startDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(System.currentTimeMillis() - analysisDays * 24 * 60 * 60 * 1000L))

        // Convert dates to timestamps for getResultsInRange (which expects Long)
        val startDateMillis = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .parse(startDate)?.time ?: 0L
        val endDateMillis = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .parse(endDate)?.time ?: System.currentTimeMillis()
        // Set end date to end of day
        val endOfDayMillis = endDateMillis + 24 * 60 * 60 * 1000L - 1

        // Get intervention results for analysis
        val recentResults = interventionResultRepository.getResultsInRange(startDateMillis, endOfDayMillis)

        // Get usage sessions for analysis (uses String dates)
        val usageSessions = usageRepository.getSessionsInRange(startDate, endDate)

        // Calculate metrics
        val totalSessions = usageSessions.size
        val avgDailySessions = if (analysisDays > 0) totalSessions.toDouble() / analysisDays else 0.0

        val sessionDurations = usageSessions.mapNotNull { session ->
            val duration = (session.endTimestamp ?: session.startTimestamp) - session.startTimestamp
            if (duration > 0) duration / 60000.0 else null // Convert to minutes
        }
        val avgSessionLengthMin = if (sessionDurations.isNotEmpty()) {
            sessionDurations.average()
        } else {
            0.0
        }

        // Calculate quick reopen rate
        val quickReopens = recentResults.count { it.quickReopen }
        val quickReopenRate = if (recentResults.isNotEmpty()) {
            quickReopens.toDouble() / recentResults.size
        } else {
            0.0
        }

        // Detect usage trend
        val usageTrend = detectUsageTrend(usageSessions, analysisDays)

        return PersonaAnalytics(
            daysSinceInstall = daysSinceInstall,
            totalSessions = totalSessions,
            avgDailySessions = avgDailySessions,
            avgSessionLengthMin = avgSessionLengthMin,
            quickReopenRate = quickReopenRate,
            usageTrend = usageTrend,
            lastAnalysisDate = endDate
        )
    }

    /**
     * Detect usage trend over time
     * @param sessions Usage sessions to analyze
     * @param days Number of days in the analysis period
     * @return Detected usage trend
     */
    private fun detectUsageTrend(
        sessions: List<dev.sadakat.thinkfaster.domain.model.UsageSession>,
        days: Int
    ): UsageTrendType {
        if (sessions.size < 3 || days < 3) {
            return UsageTrendType.STABLE
        }

        // Group sessions by day and count
        val sessionsByDay = sessions.groupBy { session ->
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date(session.startTimestamp))
        }.mapValues { it.value.size }

        if (sessionsByDay.size < 3) {
            return UsageTrendType.STABLE
        }

        // Sort by date and get counts
        val sortedDays = sessionsByDay.keys.sorted()
        val counts = sortedDays.map { sessionsByDay[it] ?: 0 }

        // Calculate trend using linear regression
        val n = counts.size
        val sumX = counts.indices.sum()
        val sumY = counts.sum()
        val sumXY = counts.mapIndexed { index, count -> index * count }.sum()
        val sumXX = counts.indices.map { it * it }.sum()

        // Calculate slope
        val slope = if (n > 1) {
            (n * sumXY - sumX * sumY).toDouble() / (n * sumXX - sumX * sumX)
        } else {
            0.0
        }

        // Calculate average daily sessions
        val avgDaily = sumY.toDouble() / n

        // Classify trend based on slope
        return when {
            slope > 0.5 && avgDaily > 10 -> UsageTrendType.ESCALATING
            slope > 0.2 -> UsageTrendType.INCREASING
            slope < -0.5 -> UsageTrendType.DECLINING
            slope < -0.2 -> UsageTrendType.DECREASING
            else -> UsageTrendType.STABLE
        }
    }

    /**
     * Calculate confidence level in persona detection
     * Based on amount of data available
     */
    private fun calculateConfidence(daysSinceInstall: Int): ConfidenceLevel {
        return when {
            daysSinceInstall < 7 -> ConfidenceLevel.LOW
            daysSinceInstall < 14 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.HIGH
        }
    }

    /**
     * Clear cached persona detection
     * Call when significant user behavior changes are expected
     */
    fun clearCache() {
        cachedDetection = null
        cacheTimestamp = 0L
        ErrorLogger.debug(
            message = "Persona detection cache cleared",
            context = "PersonaDetector"
        )
    }
}
