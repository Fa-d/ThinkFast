package dev.sadakat.thinkfaster.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 1: Entity for storing comprehensive intervention outcomes
 *
 * This entity stores extended outcome data for interventions, tracking short-term,
 * medium-term, and long-term effects. It's linked to InterventionResultEntity via
 * foreign key.
 *
 * Collection happens in stages:
 * - Proximal: Immediately after intervention
 * - Short-term: 5-30 minutes after
 * - Medium-term: End of same day
 * - Long-term: 7-30 days after
 *
 * Indexes optimized for:
 * - Finding outcomes needing updates (by collection status + timestamp)
 * - RL training queries (by reward score)
 * - Analytics (by app, user choice)
 */
@Entity(
    tableName = "comprehensive_outcomes",
    foreignKeys = [
        ForeignKey(
            entity = InterventionResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["intervention_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["intervention_id"], unique = true),
        Index(value = ["target_app"]),
        Index(value = ["timestamp"]),
        Index(value = ["proximal_collected", "short_term_collected"]),
        Index(value = ["reward_score"]),
        Index(value = ["immediate_choice"])
    ]
)
data class ComprehensiveOutcomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Reference to parent intervention
    @ColumnInfo(name = "intervention_id")
    val interventionId: Long,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "target_app")
    val targetApp: String,

    val timestamp: Long,

    // ========== PROXIMAL OUTCOMES ==========

    @ColumnInfo(name = "immediate_choice")
    val immediateChoice: String,  // "GO_BACK", "CONTINUE", "SNOOZE", "DISMISS", "TIMEOUT"

    @ColumnInfo(name = "response_time")
    val responseTime: Long,

    @ColumnInfo(name = "interaction_depth")
    val interactionDepth: String,  // "DISMISSED", "VIEWED", "ENGAGED", "INTERACTED"

    // ========== SHORT-TERM OUTCOMES ==========

    @ColumnInfo(name = "session_continued")
    val sessionContinued: Boolean? = null,

    @ColumnInfo(name = "session_duration_after")
    val sessionDurationAfter: Long? = null,

    @ColumnInfo(name = "quick_reopen")
    val quickReopen: Boolean? = null,

    @ColumnInfo(name = "switched_to_productive_app")
    val switchedToProductiveApp: Boolean? = null,

    @ColumnInfo(name = "reopen_count_30min")
    val reopenCount30Min: Int? = null,

    // ========== MEDIUM-TERM OUTCOMES ==========

    @ColumnInfo(name = "total_usage_reduction_today")
    val totalUsageReductionToday: Long? = null,

    @ColumnInfo(name = "goal_met_today")
    val goalMetToday: Boolean? = null,

    @ColumnInfo(name = "additional_sessions_today")
    val additionalSessionsToday: Int? = null,

    @ColumnInfo(name = "total_screen_time_today")
    val totalScreenTimeToday: Long? = null,

    // ========== LONG-TERM OUTCOMES ==========

    @ColumnInfo(name = "weekly_usage_change")
    val weeklyUsageChange: String? = null,  // "DECREASED", "STABLE", "INCREASED"

    @ColumnInfo(name = "streak_maintained")
    val streakMaintained: Boolean? = null,

    @ColumnInfo(name = "app_uninstalled")
    val appUninstalled: Boolean? = null,

    @ColumnInfo(name = "user_retention")
    val userRetention: Boolean? = null,

    @ColumnInfo(name = "avg_daily_usage_next_7days")
    val avgDailyUsageNext7Days: Long? = null,

    // ========== METADATA ==========

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis(),

    // Collection status flags
    @ColumnInfo(name = "proximal_collected")
    val proximalCollected: Boolean = false,

    @ColumnInfo(name = "short_term_collected")
    val shortTermCollected: Boolean = false,

    @ColumnInfo(name = "medium_term_collected")
    val mediumTermCollected: Boolean = false,

    @ColumnInfo(name = "long_term_collected")
    val longTermCollected: Boolean = false,

    // Cached reward score for RL queries
    @ColumnInfo(name = "reward_score")
    val rewardScore: Double? = null
)
