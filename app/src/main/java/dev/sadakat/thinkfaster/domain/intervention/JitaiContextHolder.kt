package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.domain.model.ConfidenceLevel
import dev.sadakat.thinkfaster.domain.model.OpportunityLevel
import dev.sadakat.thinkfaster.domain.model.UserPersona
import dev.sadakat.thinkfaster.util.ErrorLogger

/**
 * JITAI context data for intervention tracking
 * Phase 2 JITAI: Contains persona and opportunity detection results
 *
 * This class holds the JITAI metadata for the current intervention,
 * which needs to be recorded in the InterventionResult for analytics.
 */
data class JitaiContext(
    val persona: UserPersona,
    val personaConfidence: ConfidenceLevel,
    val opportunityScore: Int,
    val opportunityLevel: OpportunityLevel,
    val decision: dev.sadakat.thinkfaster.domain.model.InterventionDecision,
    val decisionSource: String
) {
    /**
     * Convert to map for InterventionResult construction
     */
    fun toResultMap(): Map<String, Any?> = mapOf(
        "userPersona" to persona.name,
        "personaConfidence" to personaConfidence.name,
        "opportunityScore" to opportunityScore,
        "opportunityLevel" to opportunityLevel.name,
        "decisionSource" to decisionSource
    )
}

/**
 * Thread-safe holder for current JITAI context
 * Phase 2 JITAI: Stores JITAI metadata for the current intervention
 *
 * This singleton provides a way to pass JITAI context from UsageMonitorService
 * to the overlay ViewModels without modifying the entire overlay chain.
 *
 * Usage:
 * - UsageMonitorService sets context before showing overlay
 * - Overlay ViewModel retrieves context when recording result
 * - Context is cleared after recording
 */
object JitaiContextHolder {
    @Volatile
    private var currentContext: JitaiContext? = null

    /**
     * Set the current JITAI context
     * Call this before showing an intervention overlay
     */
    fun setContext(context: JitaiContext) {
        currentContext = context
        ErrorLogger.debug(
            message = "JITAI context set: ${context.persona.name}, opportunity=${context.opportunityLevel}(${context.opportunityScore}/100)",
            context = "JitaiContextHolder"
        )
    }

    /**
     * Get the current JITAI context
     * Call this when recording an intervention result
     */
    fun getContext(): JitaiContext? = currentContext

    /**
     * Clear the current JITAI context
     * Call this after recording the intervention result
     */
    fun clearContext() {
        currentContext = null
        ErrorLogger.debug(
            message = "JITAI context cleared",
            context = "JitaiContextHolder"
        )
    }

    /**
     * Get and clear the current JITAI context atomically
     * This ensures the context is only used once
     */
    fun getAndClearContext(): JitaiContext? {
        val context = currentContext
        currentContext = null
        if (context != null) {
            ErrorLogger.debug(
                message = "JITAI context retrieved and cleared: ${context.persona.name}",
                context = "JitaiContextHolder"
            )
        }
        return context
    }
}
