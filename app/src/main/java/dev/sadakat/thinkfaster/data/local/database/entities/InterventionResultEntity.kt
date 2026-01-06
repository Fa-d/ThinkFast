package dev.sadakat.thinkfaster.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for tracking intervention effectiveness
 * Phase G: Tracks how users respond to interventions and measures outcomes
 * Phase 1: Added user feedback and context for ML training
 * Phase 2: Added persona and opportunity tracking for JITAI-based interventions
 *
 * The app now uses time-based and usage-pattern-based context for intervention timing.
 *
 * Performance Optimization: Added indexes for JITAI analytics queries
 * - targetApp: For app-specific analytics
 * - timestamp: For date range queries
 * - userPersona: For persona-based analytics
 * - userChoice: For effectiveness calculations
 */
@Entity(
    tableName = "intervention_results",
    indices = [
        Index(value = ["targetApp"]),
        Index(value = ["timestamp"]),
        Index(value = ["user_persona"]),
        Index(value = ["userChoice"]),
        Index(value = ["targetApp", "timestamp"]),
        Index(value = ["user_persona", "userChoice"])
    ]
)
data class InterventionResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Session info
    val sessionId: Long,

    // App info
    val targetApp: String,

    // Intervention details
    val interventionType: String,        // "REMINDER" or "TIMER"
    val contentType: String,             // "ReflectionQuestion", "TimeAlternative", etc.

    // Context at time of intervention
    val hourOfDay: Int,                   // 0-23
    val dayOfWeek: Int,                  // 1-7 (1=Monday)
    val isWeekend: Boolean,
    val isLateNight: Boolean,             // 22:00-05:00
    val sessionCount: Int,                // Number of sessions today before this
    val quickReopen: Boolean,             // Opened within 2 minutes of last close
    val currentSessionDurationMs: Long,   // Duration when intervention shown

    // User behavior
    val userChoice: String,               // "PROCEED" or "GO_BACK"
    val timeToShowDecisionMs: Long,       // How long user took to decide

    // Phase 1: User feedback on intervention quality (for ML training)
    @ColumnInfo(name = "user_feedback", defaultValue = "NONE")
    val userFeedback: String = "NONE",    // "HELPFUL" | "DISRUPTIVE" | "NONE"

    @ColumnInfo(name = "feedback_timestamp")
    val feedbackTimestamp: Long? = null,

    // Phase 1: Environmental context for ML training (privacy-safe)
    @ColumnInfo(name = "audio_active", defaultValue = "0")
    val audioActive: Boolean = false,     // Was phone call or media playing?

    @ColumnInfo(name = "was_snoozed", defaultValue = "0")
    val wasSnoozed: Boolean = false,      // Did user snooze this intervention?

    @ColumnInfo(name = "snooze_duration_ms")
    val snoozeDurationMs: Long? = null,   // How long was the snooze?

    // Phase 2: Persona and opportunity tracking
    @ColumnInfo(name = "user_persona")
    val userPersona: String? = null,       // "HEAVY_COMPULSIVE_USER", etc.

    @ColumnInfo(name = "persona_confidence")
    val personaConfidence: String? = null, // "LOW", "MEDIUM", "HIGH"

    @ColumnInfo(name = "opportunity_score")
    val opportunityScore: Int? = null,     // 0-100

    @ColumnInfo(name = "opportunity_level")
    val opportunityLevel: String? = null,  // "EXCELLENT", "GOOD", "MODERATE", "POOR"

    @ColumnInfo(name = "decision_source")
    val decisionSource: String? = null,    // "OPPORTUNITY_BASED", "BASIC_RATE_LIMIT", etc.

    // Outcome (optional - filled after session ends)
    val finalSessionDurationMs: Long?,    // Total session duration
    val sessionEndedNormally: Boolean?,   // true if ended normally, false if force-closed
    val timestamp: Long,                  // When intervention was shown

    // Sync metadata for multi-device sync
    @ColumnInfo(name = "user_id", defaultValue = "NULL")
    val userId: String? = null,

    @ColumnInfo(name = "sync_status", defaultValue = "PENDING")
    val syncStatus: String = "PENDING",

    @ColumnInfo(name = "last_modified", defaultValue = "0")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "cloud_id", defaultValue = "NULL")
    val cloudId: String? = null
)
