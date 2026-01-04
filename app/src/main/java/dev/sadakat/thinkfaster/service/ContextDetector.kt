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
     *
     * @param interventionType Type of intervention (REMINDER or TIMER)
     * @param sessionDurationMs Duration of current session in milliseconds (optional)
     */
    fun shouldShowInterventionType(
        interventionType: InterventionType,
        sessionDurationMs: Long = 0
    ): ContextResult {
        // Skip interventions for very short sessions (< 2 minutes)
        // This prevents annoying users who are just quickly checking something
        if (sessionDurationMs > 0 && sessionDurationMs < 2 * 60 * 1000) {
            return ContextResult(
                shouldShowIntervention = false,
                reason = "Session too short (${sessionDurationMs / 1000}s < 2min)"
            )
        }

        val baseResult = shouldShowIntervention()

        if (!baseResult.shouldShowIntervention) {
            return baseResult
        }

        // Additional checks per intervention type
        return when (interventionType) {
            InterventionType.REMINDER -> {
                // Reminders can be shown for sessions >= 2 minutes
                ContextResult(shouldShowIntervention = true)
            }
            InterventionType.TIMER -> {
                // Timer requires longer session (>= 5 min) to show
                if (sessionDurationMs > 0 && sessionDurationMs < 5 * 60 * 1000) {
                    ContextResult(
                        shouldShowIntervention = false,
                        reason = "Timer requires longer session (${sessionDurationMs / 1000}s < 5min)"
                    )
                } else {
                    ContextResult(shouldShowIntervention = true)
                }
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
