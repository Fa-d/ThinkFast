package dev.sadakat.thinkfast.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking intervention effectiveness
 * Phase G: Tracks how users respond to interventions and measures outcomes
 */
@Entity(tableName = "intervention_results")
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

    // Outcome (optional - filled after session ends)
    val finalSessionDurationMs: Long?,    // Total session duration
    val sessionEndedNormally: Boolean?,   // true if ended normally, false if force-closed
    val timestamp: Long                   // When intervention was shown
)
