package dev.sadakat.thinkfaster.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 1: Entity for storing intervention decision explanations
 *
 * Stores complete rationale for each intervention decision (SHOW or SKIP)
 * to enable debugging, optimization, and transparency.
 *
 * This data is critical for:
 * - Understanding why interventions were shown/skipped
 * - Identifying algorithm bugs or suboptimal patterns
 * - A/B testing decision rule changes
 * - Building user trust through transparency
 *
 * Indexes optimized for:
 * - Finding decisions by target app
 * - Finding skipped interventions by blocking reason
 * - Analyzing decisions by opportunity level
 */
@Entity(
    tableName = "decision_explanations",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["target_app"]),
        Index(value = ["decision"]),
        Index(value = ["blocking_reason"]),
        Index(value = ["opportunity_level"]),
        Index(value = ["intervention_id"])
    ]
)
data class DecisionExplanationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val timestamp: Long,

    @ColumnInfo(name = "target_app")
    val targetApp: String,

    // ========== DECISION OUTCOME ==========

    val decision: String,  // "SHOW" or "SKIP"

    @ColumnInfo(name = "blocking_reason")
    val blockingReason: String?,  // Reason if SKIP

    @ColumnInfo(name = "intervention_id")
    val interventionId: Long?,  // Link to InterventionResultEntity if SHOW

    // ========== OPPORTUNITY SCORING ==========

    @ColumnInfo(name = "opportunity_score")
    val opportunityScore: Int,

    @ColumnInfo(name = "opportunity_level")
    val opportunityLevel: String,

    @ColumnInfo(name = "opportunity_breakdown")
    val opportunityBreakdown: String,  // JSON map of factor -> points

    // ========== PERSONA ==========

    @ColumnInfo(name = "persona_detected")
    val personaDetected: String,

    @ColumnInfo(name = "persona_confidence")
    val personaConfidence: String,

    // ========== RATE LIMITING ==========

    @ColumnInfo(name = "passed_basic_rate_limit")
    val passedBasicRateLimit: Boolean,

    @ColumnInfo(name = "time_since_last_intervention")
    val timeSinceLastIntervention: Long?,

    @ColumnInfo(name = "passed_persona_frequency")
    val passedPersonaFrequency: Boolean,

    @ColumnInfo(name = "persona_frequency_rule")
    val personaFrequencyRule: String?,

    @ColumnInfo(name = "passed_jitai_filter")
    val passedJitaiFilter: Boolean,

    @ColumnInfo(name = "jitai_decision")
    val jitaiDecision: String?,

    // ========== BURDEN ==========

    @ColumnInfo(name = "burden_level")
    val burdenLevel: String?,

    @ColumnInfo(name = "burden_score")
    val burdenScore: Int?,

    @ColumnInfo(name = "burden_mitigation_applied")
    val burdenMitigationApplied: Boolean,

    @ColumnInfo(name = "burden_cooldown_multiplier")
    val burdenCooldownMultiplier: Float?,

    // ========== CONTENT SELECTION ==========

    @ColumnInfo(name = "content_type_selected")
    val contentTypeSelected: String?,

    @ColumnInfo(name = "content_weights")
    val contentWeights: String?,  // JSON map of type -> weight

    @ColumnInfo(name = "content_selection_reason")
    val contentSelectionReason: String?,

    // ========== REINFORCEMENT LEARNING ==========

    @ColumnInfo(name = "rl_predicted_reward")
    val rlPredictedReward: Double?,

    @ColumnInfo(name = "rl_exploration_vs_exploitation")
    val rlExplorationVsExploitation: String?,

    // ========== CONTEXT ==========

    @ColumnInfo(name = "context_snapshot")
    val contextSnapshot: String,  // JSON of full context

    // ========== EXPLANATIONS ==========

    val explanation: String,  // Short human-readable explanation

    @ColumnInfo(name = "detailed_explanation")
    val detailedExplanation: String  // Full detailed explanation
)
