package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.domain.model.InterventionContent
import dev.sadakat.thinkfaster.domain.model.InterventionType
import dev.sadakat.thinkfaster.domain.model.UserPersona
import dev.sadakat.thinkfaster.util.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.forEach

/**
 * Content selection result with persona metadata
 * Phase 2 JITAI: Enhanced content selection with personalization
 */
data class PersonaAwareContentSelection(
    val content: InterventionContent,
    val persona: UserPersona,
    val contentType: ContentType,
    val selectionReason: String,  // Human-readable explanation
    val weights: Map<ContentType, Int>,  // Final weights used
    val detectedAt: Long = System.currentTimeMillis()
)

/**
 * Persona-Aware Content Selector
 * Phase 2 JITAI: Personalized intervention content based on behavioral persona
 *
 * This class enhances content selection by:
 * 1. Detecting user persona (cached if recent)
 * 2. Getting persona-specific base weights
 * 3. Applying context-aware adjustments (late night, quick reopen, etc.)
 * 4. Applying effectiveness adjustments (if 30+ interventions data)
 * 5. Selecting content type using weighted randomization
 * 6. Generating content with persona awareness
 * 7. Tracking selection for analytics
 *
 * Caching: Persona detection is cached for 6 hours in PersonaDetector
 */
class PersonaAwareContentSelector(
    private val personaDetector: PersonaDetector,
    private val baseContentSelector: ContentSelector
) {

    companion object {
        // Minimum interventions for effectiveness-based adjustments
        private const val MIN_EFFECTIVENESS_DATA = 30

        // Track recent selections to prevent repetition
        private val recentSelections = mutableListOf<ContentType>()
        private const val MAX_RECENT_SELECTIONS_SIZE = 10
    }

    /**
     * Select content with persona awareness
     * @param context Current intervention context
     * @param interventionType REMINDER or TIMER
     * @param effectivenessData Optional historical effectiveness data
     * @return Persona-aware content selection with metadata
     */
    suspend fun selectContent(
        context: InterventionContext,
        interventionType: InterventionType,
        effectivenessData: List<dev.sadakat.thinkfaster.domain.model.ContentEffectivenessStats> = emptyList()
    ): PersonaAwareContentSelection = withContext(Dispatchers.IO) {

        // Step 1: Detect user persona (cached if recent)
        val detectedPersona = personaDetector.detectPersona()
        val persona = detectedPersona.persona

        ErrorLogger.info(
            message = "Content selection for persona: ${persona.name}",
            context = "PersonaAwareContentSelector"
        )

        // Step 2: Get persona-specific base weights
        val baseWeights = getPersonaBaseWeights(persona)

        // Step 3: Apply context-aware adjustments
        val contextAdjustedWeights = applyContextAdjustments(
            baseWeights = baseWeights,
            context = context,
            persona = persona,
            interventionType = interventionType
        )

        // Step 4: Apply effectiveness adjustments (if sufficient data)
        val finalWeights = if (effectivenessData.sumOf { it.total } >= MIN_EFFECTIVENESS_DATA) {
            applyEffectivenessAdjustments(
                baseWeights = contextAdjustedWeights,
                effectivenessData = effectivenessData
            )
        } else {
            contextAdjustedWeights
        }

        // Step 5: Select content type using weighted randomization
        val contentType = selectContentType(finalWeights)

        // Step 6: Generate content using base selector
        val content = when (contentType) {
            ContentType.REFLECTION -> baseContentSelector.generateContentByType("REFLECTION", context)
            ContentType.TIME_ALTERNATIVE -> baseContentSelector.generateContentByType("TIME_ALTERNATIVE", context)
            ContentType.BREATHING -> baseContentSelector.generateContentByType("BREATHING", context)
            ContentType.STATS -> baseContentSelector.generateContentByType("STATS", context)
            ContentType.EMOTIONAL_APPEAL -> baseContentSelector.generateContentByType("EMOTIONAL_APPEAL", context)
            ContentType.QUOTE -> baseContentSelector.generateContentByType("QUOTE", context)
            ContentType.GAMIFICATION -> baseContentSelector.generateContentByType("GAMIFICATION", context)
            ContentType.ACTIVITY_SUGGESTION -> baseContentSelector.generateContentByType("ACTIVITY_SUGGESTION", context)
        }

        // Step 7: Generate selection reason
        val selectionReason = generateSelectionReason(persona, contentType, context)

        // Track selection to prevent repetition
        trackSelection(contentType)

        PersonaAwareContentSelection(
            content = content,
            persona = persona,
            contentType = contentType,
            selectionReason = selectionReason,
            weights = finalWeights
        )
    }

    /**
     * Get base weights for a persona
     * Maps String-based weights in UserPersona to ContentType enum
     */
    private fun getPersonaBaseWeights(persona: UserPersona): Map<ContentType, Int> {
        return persona.baseWeights.mapKeys { (contentTypeName, _) ->
            try {
                ContentType.valueOf(contentTypeName)
            } catch (e: IllegalArgumentException) {
                ErrorLogger.warning(
                    message = "Unknown content type: $contentTypeName",
                    context = "PersonaAwareContentSelector"
                )
                ContentType.REFLECTION // Fallback
            }
        }
    }

    /**
     * Apply context-aware adjustments to base weights
     * Combines persona-specific weights with contextual factors
     */
    private fun applyContextAdjustments(
        baseWeights: Map<ContentType, Int>,
        context: InterventionContext,
        persona: UserPersona,
        interventionType: InterventionType
    ): Map<ContentType, Int> {
        val adjustedWeights = baseWeights.toMutableMap()

        // Context adjustments by persona type
        when (persona) {
            // HEAVY_COMPULSIVE_USER: Focus on breaking compulsive patterns
            UserPersona.HEAVY_COMPULSIVE_USER -> {
                if (context.isLateNight) {
                    adjustedWeights[ContentType.REFLECTION] =
                        (adjustedWeights[ContentType.REFLECTION] ?: 0) + 15
                    adjustedWeights[ContentType.EMOTIONAL_APPEAL] =
                        (adjustedWeights[ContentType.EMOTIONAL_APPEAL] ?: 0) + 10
                }
                if (context.quickReopenAttempt) {
                    // Double down on reflection for compulsive reopens
                    adjustedWeights[ContentType.REFLECTION] =
                        (adjustedWeights[ContentType.REFLECTION] ?: 0) * 2
                    adjustedWeights[ContentType.EMOTIONAL_APPEAL] =
                        (adjustedWeights[ContentType.EMOTIONAL_APPEAL] ?: 0) + 15
                }
            }

            // HEAVY_BINGE_USER: Focus on providing activity alternatives
            UserPersona.HEAVY_BINGE_USER -> {
                if (context.isLateNight) {
                    adjustedWeights[ContentType.ACTIVITY_SUGGESTION] =
                        (adjustedWeights[ContentType.ACTIVITY_SUGGESTION] ?: 0) + 20
                    adjustedWeights[ContentType.BREATHING] =
                        (adjustedWeights[ContentType.BREATHING] ?: 0) + 15
                }
                if (context.isExtendedSession) {
                    // Extra emphasis on time alternatives for long binges
                    val currentWeight = adjustedWeights[ContentType.TIME_ALTERNATIVE] ?: 0
                    adjustedWeights[ContentType.TIME_ALTERNATIVE] = currentWeight +
                            (currentWeight * 0.5).toInt()  // Add 50% more weight
                }
            }

            // MODERATE_BALANCED_USER: Balanced approach
            UserPersona.MODERATE_BALANCED_USER -> {
                // Standard context adjustments from base selector
                if (context.quickReopenAttempt) {
                    adjustedWeights[ContentType.REFLECTION] =
                        (adjustedWeights[ContentType.REFLECTION] ?: 0) + 20
                }
                if (context.isExtendedSession) {
                    adjustedWeights[ContentType.TIME_ALTERNATIVE] =
                        (adjustedWeights[ContentType.TIME_ALTERNATIVE] ?: 0) + 15
                }
            }

            // CASUAL_USER: Light touch interventions
            UserPersona.CASUAL_USER -> {
                if (context.isLateNight) {
                    adjustedWeights[ContentType.BREATHING] =
                        (adjustedWeights[ContentType.BREATHING] ?: 0) + 15
                    adjustedWeights[ContentType.ACTIVITY_SUGGESTION] =
                        (adjustedWeights[ContentType.ACTIVITY_SUGGESTION] ?: 0) + 10
                }
                if (context.quickReopenAttempt) {
                    adjustedWeights[ContentType.REFLECTION] =
                        (adjustedWeights[ContentType.REFLECTION] ?: 0) + 10
                    adjustedWeights[ContentType.BREATHING] =
                        (adjustedWeights[ContentType.BREATHING] ?: 0) + 10
                }
            }

            // PROBLEMATIC_PATTERN_USER: Strong intervention needed
            UserPersona.PROBLEMATIC_PATTERN_USER -> {
                // Always emphasize reflection for problematic patterns
                adjustedWeights[ContentType.REFLECTION] =
                    (adjustedWeights[ContentType.REFLECTION] ?: 0) + 20

                if (context.quickReopenAttempt) {
                    // Maximum emphasis on breaking the pattern
                    adjustedWeights[ContentType.REFLECTION] =
                        (adjustedWeights[ContentType.REFLECTION] ?: 0) + 30
                    adjustedWeights[ContentType.EMOTIONAL_APPEAL] =
                        (adjustedWeights[ContentType.EMOTIONAL_APPEAL] ?: 0) + 20
                    // Remove activity suggestions - they need reflection first
                    adjustedWeights[ContentType.ACTIVITY_SUGGESTION] = 0
                }
            }

            // NEW_USER: Gentle onboarding approach
            UserPersona.NEW_USER -> {
                // Emphasize breathing and gentle content
                if (context.isLateNight) {
                    adjustedWeights[ContentType.BREATHING] =
                        (adjustedWeights[ContentType.BREATHING] ?: 0) + 15
                    adjustedWeights[ContentType.ACTIVITY_SUGGESTION] =
                        (adjustedWeights[ContentType.ACTIVITY_SUGGESTION] ?: 0) + 10
                }
                // Reduce emotional appeals for new users
                adjustedWeights[ContentType.EMOTIONAL_APPEAL] =
                    ((adjustedWeights[ContentType.EMOTIONAL_APPEAL] ?: 0) * 0.5).toInt()
            }
        }

        // Additional context-specific adjustments (apply to all personas)
        if (context.isWeekendMorning) {
            adjustedWeights[ContentType.ACTIVITY_SUGGESTION] =
                (adjustedWeights[ContentType.ACTIVITY_SUGGESTION] ?: 0) + 10
        }

        if (interventionType == InterventionType.TIMER) {
            // Timer interventions always emphasize time alternatives
            adjustedWeights[ContentType.TIME_ALTERNATIVE] =
                (adjustedWeights[ContentType.TIME_ALTERNATIVE] ?: 0) + 20
        }

        // Ensure no negative weights and minimum weight of 0 for all active content types
        adjustedWeights.keys.forEach { key ->
            adjustedWeights[key] = (adjustedWeights[key] ?: 0).coerceAtLeast(0)
        }

        return adjustedWeights.filterValues { it > 0 }
    }

    /**
     * Apply effectiveness-based adjustments to weights
     * Boosts content types that have historically worked well
     */
    private fun applyEffectivenessAdjustments(
        baseWeights: Map<ContentType, Int>,
        effectivenessData: List<dev.sadakat.thinkfaster.domain.model.ContentEffectivenessStats>
    ): Map<ContentType, Int> {
        val adjustedWeights = baseWeights.toMutableMap()

        // Calculate average success rate
        val avgSuccessRate = effectivenessData.map { it.dismissalRate }.average()

        effectivenessData.forEach { stats ->
            // Map content type string to ContentType enum
            val contentType = mapStringToContentType(stats.contentType) ?: return@forEach

            if (adjustedWeights.containsKey(contentType)) {
                val currentWeight = adjustedWeights[contentType] ?: 0

                // Boost weight if content performs above average
                when {
                    stats.dismissalRate >= avgSuccessRate + 15 -> {
                        // Significantly above average: boost by 25%
                        adjustedWeights[contentType] = (currentWeight * 1.25).toInt()
                    }
                    stats.dismissalRate >= avgSuccessRate + 5 -> {
                        // Moderately above average: boost by 15%
                        adjustedWeights[contentType] = (currentWeight * 1.15).toInt()
                    }
                    stats.dismissalRate >= avgSuccessRate -> {
                        // Slightly above average: boost by 5%
                        adjustedWeights[contentType] = (currentWeight * 1.05).toInt()
                    }
                    stats.dismissalRate < avgSuccessRate - 15 -> {
                        // Significantly below average: reduce by 20%
                        adjustedWeights[contentType] = (currentWeight * 0.8).toInt()
                    }
                }
            }
        }

        return adjustedWeights
    }

    /**
     * Select content type using weighted randomization
     * Ensures we don't select the same type too frequently
     */
    private fun selectContentType(weights: Map<ContentType, Int>): ContentType {
        // Filter out recently selected types to ensure variety
        val availableTypes = weights.filter { it.key !in recentSelections }

        // If we've used all types recently, reset the history
        val finalWeights = if (availableTypes.isEmpty()) {
            recentSelections.clear()
            weights
        } else {
            availableTypes
        }

        // Perform weighted random selection
        val totalWeight = finalWeights.values.sum()
        var randomValue = kotlin.random.Random.nextInt(totalWeight)

        for ((contentType, weight) in finalWeights) {
            randomValue -= weight
            if (randomValue < 0) {
                return contentType
            }
        }

        // Fallback
        return ContentType.REFLECTION
    }

    /**
     * Map content type string to ContentType enum
     */
    private fun mapStringToContentType(contentType: String): ContentType? {
        return when {
            contentType.contains("Reflection", ignoreCase = true) -> ContentType.REFLECTION
            contentType.contains("TimeAlternative", ignoreCase = true) -> ContentType.TIME_ALTERNATIVE
            contentType.contains("Breathing", ignoreCase = true) -> ContentType.BREATHING
            contentType.contains("Stats", ignoreCase = true) || contentType.contains("Usage", ignoreCase = true) -> ContentType.STATS
            contentType.contains("Emotional", ignoreCase = true) -> ContentType.EMOTIONAL_APPEAL
            contentType.contains("Quote", ignoreCase = true) -> ContentType.QUOTE
            contentType.contains("Gamification", ignoreCase = true) -> ContentType.GAMIFICATION
            contentType.contains("Activity", ignoreCase = true) -> ContentType.ACTIVITY_SUGGESTION
            else -> null
        }
    }

    /**
     * Generate human-readable explanation for content selection
     */
    private fun generateSelectionReason(
        persona: UserPersona,
        contentType: ContentType,
        context: InterventionContext
    ): String {
        val parts = mutableListOf<String>()

        // Add persona context
        parts.add("Detected as ${persona.displayName}")

        // Add content type rationale
        val rationale = when (persona) {
            UserPersona.HEAVY_COMPULSIVE_USER -> {
                when (contentType) {
                    ContentType.REFLECTION -> "Reflection helps break compulsive patterns"
                    ContentType.TIME_ALTERNATIVE -> "Time alternatives provide perspective"
                    ContentType.BREATHING -> "Breathing reduces immediate anxiety"
                    else -> "Selected for compulsive user pattern"
                }
            }
            UserPersona.HEAVY_BINGE_USER -> {
                when (contentType) {
                    ContentType.TIME_ALTERNATIVE -> "Time alternatives address extended sessions"
                    ContentType.ACTIVITY_SUGGESTION -> "Activity suggestions break binge cycles"
                    else -> "Selected for binge user pattern"
                }
            }
            UserPersona.MODERATE_BALANCED_USER -> {
                when (contentType) {
                    ContentType.REFLECTION -> "Self-awareness supports balanced usage"
                    ContentType.TIME_ALTERNATIVE -> "Time perspective aids moderation"
                    else -> "Selected for balanced user pattern"
                }
            }
            UserPersona.CASUAL_USER -> {
                "Light-touch support for casual usage"
            }
            UserPersona.PROBLEMATIC_PATTERN_USER -> {
                when (contentType) {
                    ContentType.REFLECTION -> "Strong reflection needed for escalating usage"
                    ContentType.EMOTIONAL_APPEAL -> "Direct appeal for problematic patterns"
                    else -> "Selected to address problematic pattern"
                }
            }
            UserPersona.NEW_USER -> {
                "Gentle introduction for new user"
            }
        }
        parts.add(rationale)

        // Add context factors
        val contextFactors = mutableListOf<String>()
        if (context.isLateNight) contextFactors.add("late night")
        if (context.quickReopenAttempt) contextFactors.add("quick reopen")
        if (context.isExtendedSession) contextFactors.add("extended session")
        if (context.isFirstSessionOfDay) contextFactors.add("first session")

        if (contextFactors.isNotEmpty()) {
            parts.add("Context: ${contextFactors.joinToString(", ")}")
        }

        return parts.joinToString(" | ")
    }

    /**
     * Track selection to prevent repetition
     */
    private fun trackSelection(contentType: ContentType) {
        recentSelections.add(contentType)

        // Keep only last N selections
        if (recentSelections.size > MAX_RECENT_SELECTIONS_SIZE) {
            recentSelections.removeAt(0)
        }
    }

    /**
     * Clear selection history (for testing)
     */
    fun clearHistory() {
        recentSelections.clear()
    }
}
