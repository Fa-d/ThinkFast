package dev.sadakat.thinkfaster.data.sync.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data models for sync
 * Phase 7: Supabase Backend Implementation
 *
 * These models represent PostgreSQL table rows and use kotlinx.serialization
 * for JSON serialization/deserialization
 *
 * Note: @SerialName annotations map Kotlin camelCase to PostgreSQL snake_case columns
 */

// ========== Goal ==========
@Serializable
data class SupabaseGoal(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("target_app")
    val targetApp: String,
    @SerialName("daily_limit_minutes")
    val dailyLimitMinutes: Int,
    @SerialName("current_streak")
    val currentStreak: Int,
    @SerialName("longest_streak")
    val longestStreak: Int,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("last_updated")
    val lastUpdated: Long,
    @SerialName("sync_status")
    val syncStatus: String = "SYNCED",
    @SerialName("last_modified")
    val lastModified: Long = System.currentTimeMillis()
)

// ========== Usage Session ==========
@Serializable
data class SupabaseUsageSession(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("target_app")
    val targetApp: String,
    @SerialName("start_timestamp")
    val startTimestamp: Long,
    @SerialName("end_timestamp")
    val endTimestamp: Long,
    val duration: Long,
    val date: String,
    @SerialName("sync_status")
    val syncStatus: String = "SYNCED",
    @SerialName("last_modified")
    val lastModified: Long = System.currentTimeMillis()
)

// ========== Usage Event ==========
@Serializable
data class SupabaseUsageEvent(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("session_id")
    val sessionId: Long,
    val timestamp: Long,
    @SerialName("event_type")
    val eventType: String,
    val metadata: String?,
    @SerialName("sync_status")
    val syncStatus: String = "SYNCED",
    @SerialName("last_modified")
    val lastModified: Long = System.currentTimeMillis()
)

// ========== Daily Stats ==========
@Serializable
data class SupabaseDailyStats(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("target_app")
    val targetApp: String,
    val date: String,
    @SerialName("total_duration")
    val totalDuration: Long,
    @SerialName("session_count")
    val sessionCount: Int,
    @SerialName("alerts_shown")
    val alertsShown: Int,
    @SerialName("longest_session")
    val longestSession: Long,
    @SerialName("sync_status")
    val syncStatus: String = "SYNCED",
    @SerialName("last_modified")
    val lastModified: Long = System.currentTimeMillis()
)

// ========== Intervention Result ==========
@Serializable
data class SupabaseInterventionResult(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("session_id")
    val sessionId: Long,
    @SerialName("target_app")
    val targetApp: String,
    @SerialName("intervention_type")
    val interventionType: String,
    @SerialName("friction_level")
    val frictionLevel: String,
    @SerialName("user_choice")
    val userChoice: String,
    val timestamp: Long,
    @SerialName("delay_duration")
    val delayDuration: Long?,
    @SerialName("snooze_minutes")
    val snoozeMinutes: Int?,
    val context: String?,
    @SerialName("sync_status")
    val syncStatus: String = "SYNCED",
    @SerialName("last_modified")
    val lastModified: Long = System.currentTimeMillis()
)

// ========== Streak Recovery ==========
@Serializable
data class SupabaseStreakRecovery(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("target_app")
    val targetApp: String,
    val date: String,
    @SerialName("activation_timestamp")
    val activationTimestamp: Long,
    @SerialName("expiration_timestamp")
    val expirationTimestamp: Long,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("sync_status")
    val syncStatus: String = "SYNCED",
    @SerialName("last_modified")
    val lastModified: Long = System.currentTimeMillis()
)

// ========== User Baseline ==========
@Serializable
data class SupabaseUserBaseline(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("target_app")
    val targetApp: String,
    @SerialName("first_week_start_date")
    val firstWeekStartDate: String,
    @SerialName("first_week_end_date")
    val firstWeekEndDate: String,
    @SerialName("total_sessions")
    val totalSessions: Int,
    @SerialName("total_duration")
    val totalDuration: Long,
    @SerialName("average_daily_minutes")
    val averageDailyMinutes: Int,
    @SerialName("peak_usage_hour")
    val peakUsageHour: Int,
    @SerialName("is_calculated")
    val isCalculated: Boolean,
    @SerialName("sync_status")
    val syncStatus: String = "SYNCED",
    @SerialName("last_modified")
    val lastModified: Long = System.currentTimeMillis()
)

// ========== User Profile ==========
@Serializable
data class SupabaseUserProfile(
    val id: String,
    val email: String?,
    @SerialName("display_name")
    val displayName: String?,
    @SerialName("photo_url")
    val photoUrl: String?,
    val provider: String,
    @SerialName("last_sync_time")
    val lastSyncTime: Long,
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis()
)

// ========== Settings ==========
@Serializable
data class SupabaseSettings(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("settings_json")
    val settingsJson: String,  // Serialized JSON of all settings
    @SerialName("last_modified")
    val lastModified: Long = System.currentTimeMillis()
)
