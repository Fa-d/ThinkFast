package dev.sadakat.thinkfaster.service

import android.content.Context
import dev.sadakat.thinkfaster.util.ErrorLogger

/**
 * Detects contextual information to determine if interventions should be shown
 * No time-based restrictions - interventions can be shown whenever needed
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
     * Always returns true - no time restrictions
     */
    private fun checkTimeContext(): ContextResult {
        // No time-based restrictions - show interventions whenever needed
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
