package dev.sadakat.thinkfaster.data.seed.model

/**
 * Container for all seed data for a persona
 */
data class SeedData(
    val persona: PersonaType,
    val goals: List<GoalData>,
    val sessions: List<SessionData>,
    val dailyStats: List<DailyStatsData>,
    val events: List<EventData>,
    val interventionResults: List<InterventionResultData>,
    val metadata: SeedMetadata
)

/**
 * User persona types representing different usage patterns
 */
enum class PersonaType {
    FRESH_INSTALL,
    EARLY_ADOPTER,
    TRANSITIONING_USER,
    ESTABLISHED_USER,
    LOCKED_MODE_USER,
    LATE_NIGHT_SCROLLER,
    WEEKEND_WARRIOR,
    COMPULSIVE_REOPENER,
    GOAL_SKIPPER,
    OVER_LIMIT_STRUGGLER,
    STREAK_ACHIEVER,
    REALISTIC_MIXED
}

/**
 * Goal data for seeding
 */
data class GoalData(
    val targetApp: String,
    val dailyLimitMinutes: Int,
    val startDate: String,
    val currentStreak: Int,
    val longestStreak: Int
)

/**
 * Session data for seeding
 */
data class SessionData(
    var id: Long? = null,  // Set after insert
    val targetApp: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val duration: Long,
    val wasInterrupted: Boolean,
    val interruptionType: String?,
    val date: String  // YYYY-MM-DD format
)

/**
 * Daily statistics data for seeding
 */
data class DailyStatsData(
    val date: String,  // YYYY-MM-DD format
    val targetApp: String,
    val totalDuration: Long,
    val sessionCount: Int,
    val longestSession: Long,
    val averageSession: Long,
    val alertsShown: Int,
    val alertsProceeded: Int
)

/**
 * Event data for seeding
 */
data class EventData(
    val eventType: String,
    val timestamp: Long,
    val metadata: String?
)

/**
 * Intervention result data for seeding
 * Tracks how users respond to interventions
 */
data class InterventionResultData(
    val sessionId: Long,
    val targetApp: String,
    val interventionType: String,  // "REMINDER" or "TIMER"
    val contentType: String,  // "ReflectionQuestion", "TimeAlternative", etc.
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val isWeekend: Boolean,
    val isLateNight: Boolean,
    val sessionCount: Int,
    val quickReopen: Boolean,
    val currentSessionDurationMs: Long,
    val userChoice: String,  // "PROCEED", "GO_BACK", or "DISMISSED"
    val timeToShowDecisionMs: Long,
    val finalSessionDurationMs: Long?,
    val sessionEndedNormally: Boolean?,
    val timestamp: Long
)

/**
 * Metadata about the seeded data
 */
data class SeedMetadata(
    val generatedAt: Long = System.currentTimeMillis(),
    val daysOfData: Int,
    val targetApps: List<String>,
    val description: String
)
