package dev.sadakat.thinkfaster.service

import android.content.Context
import dev.sadakat.thinkfaster.util.ErrorLogger
import java.util.Calendar

/**
 * Detects contextual information to determine if interventions should be shown
 * Factors include:
 * - Time of day (work hours, sleep hours)
 * - Day of week (weekends vs weekdays)
 *
 * The app now uses time-based and usage-pattern-based context for intervention timing.
 */
class ContextDetector(
    private val context: Context
) {

    /**
     * Context check result
     */
    data class ContextResult(
        val shouldShowIntervention: Boolean,
        val reason: String? = null
    )

    /**
     * Check if interventions should be shown based on current context
     */
    fun shouldShowIntervention(): ContextResult {
        // Check time-based contexts
        return checkTimeContext()
    }

    /**
     * Check time-based context (work hours, sleep hours, etc.)
     */
    private fun checkTimeContext(): ContextResult {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Skip interventions during typical sleep hours (11 PM - 7 AM)
        if (hourOfDay !in 7..<23) {
            return ContextResult(
                shouldShowIntervention = false,
                reason = "Sleep hours (11 PM - 7 AM)"
            )
        }

        // Skip interventions during early morning hours on weekends (7 AM - 9 AM)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        if (isWeekend && (hourOfDay in 7..<9)) {
            return ContextResult(
                shouldShowIntervention = false,
                reason = "Weekend morning (7 AM - 9 AM)"
            )
        }

        return ContextResult(shouldShowIntervention = true)
    }

    /**
     * Check if a specific intervention type should be shown
     * This allows for more granular control per intervention type
     */
    fun shouldShowInterventionType(interventionType: InterventionType): ContextResult {
        val baseResult = shouldShowIntervention()

        if (!baseResult.shouldShowIntervention) {
            return baseResult
        }

        // Additional checks per intervention type can be added here
        return when (interventionType) {
            InterventionType.REMINDER -> {
                // Reminders can be shown more liberally
                ContextResult(shouldShowIntervention = true)
            }
            InterventionType.TIMER -> {
                // Timer alerts might be suppressed in certain contexts
                ContextResult(shouldShowIntervention = true)
            }
        }
    }

    /**
     * Intervention types
     */
    enum class InterventionType {
        REMINDER,
        TIMER
    }
}
