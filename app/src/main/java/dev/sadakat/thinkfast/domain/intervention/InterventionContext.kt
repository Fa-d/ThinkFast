package dev.sadakat.thinkfast.domain.intervention

import dev.sadakat.thinkfast.domain.model.AppTarget
import java.util.Calendar

/**
 * Context information used to select appropriate intervention content.
 * Captures all relevant data about current state, usage patterns, and user settings.
 */
data class InterventionContext(
    // Time context
    val timeOfDay: Int,                     // 0-23
    val dayOfWeek: Int,                     // 1=Sunday, 7=Saturday
    val isWeekend: Boolean,                 // Saturday or Sunday

    // Session context
    val targetApp: AppTarget,               // Which app is being opened
    val currentSessionMinutes: Int,         // Current session duration
    val sessionCount: Int,                  // Number of sessions today

    // Recent activity
    val lastSessionEndTime: Long,           // When last session ended (timestamp)
    val timeSinceLastSession: Long,         // Milliseconds since last session
    val quickReopenAttempt: Boolean,        // Opened within 2 min of close

    // Usage statistics
    val totalUsageToday: Long,              // Total minutes today
    val totalUsageYesterday: Long,          // Total minutes yesterday
    val weeklyAverage: Long,                // Average daily usage this week

    // Goals and progress
    val goalMinutes: Int?,                  // Daily goal if set
    val isOverGoal: Boolean,                // Currently over daily goal
    val streakDays: Int,                    // Current streak

    // User settings
    val userFrictionLevel: FrictionLevel,   // Gentle/Moderate/Firm/Locked
    val daysSinceInstall: Int,              // Days since app installed

    // Best records
    val bestSessionMinutes: Int             // Shortest session ever
) {
    companion object {
        /**
         * Creates an InterventionContext from current app state.
         */
        fun create(
            targetApp: AppTarget,
            currentSessionDuration: Long = 0,
            sessionCount: Int,
            lastSessionEndTime: Long,
            totalUsageToday: Long,
            totalUsageYesterday: Long,
            weeklyAverage: Long,
            goalMinutes: Int?,
            streakDays: Int,
            userFrictionLevel: FrictionLevel,
            installDate: Long,
            bestSessionMinutes: Int
        ): InterventionContext {
            val calendar = Calendar.getInstance()
            val timeOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

            val currentTime = System.currentTimeMillis()
            val timeSinceLastSession = if (lastSessionEndTime > 0) {
                currentTime - lastSessionEndTime
            } else {
                Long.MAX_VALUE  // First session ever
            }

            val quickReopenAttempt = timeSinceLastSession < 2 * 60 * 1000  // < 2 minutes

            val currentSessionMinutes = (currentSessionDuration / (1000 * 60)).toInt()

            val totalUsageTodayMinutes = totalUsageToday / (1000 * 60)
            val isOverGoal = goalMinutes?.let { totalUsageTodayMinutes > it } ?: false

            val daysSinceInstall = ((currentTime - installDate) / (1000 * 60 * 60 * 24)).toInt()

            return InterventionContext(
                timeOfDay = timeOfDay,
                dayOfWeek = dayOfWeek,
                isWeekend = isWeekend,
                targetApp = targetApp,
                currentSessionMinutes = currentSessionMinutes,
                sessionCount = sessionCount,
                lastSessionEndTime = lastSessionEndTime,
                timeSinceLastSession = timeSinceLastSession,
                quickReopenAttempt = quickReopenAttempt,
                totalUsageToday = totalUsageTodayMinutes,
                totalUsageYesterday = totalUsageYesterday / (1000 * 60),
                weeklyAverage = weeklyAverage / (1000 * 60),
                goalMinutes = goalMinutes,
                isOverGoal = isOverGoal,
                streakDays = streakDays,
                userFrictionLevel = userFrictionLevel,
                daysSinceInstall = daysSinceInstall,
                bestSessionMinutes = bestSessionMinutes
            )
        }
    }

    /**
     * Helper properties for easier context checks
     */
    val isLateNight: Boolean
        get() = timeOfDay >= 22 || timeOfDay <= 5

    val isWeekendMorning: Boolean
        get() = isWeekend && timeOfDay in 6..11

    val isExtendedSession: Boolean
        get() = currentSessionMinutes >= 15

    val isHighFrequencyDay: Boolean
        get() = sessionCount >= 10

    val isFirstSessionOfDay: Boolean
        get() = sessionCount == 1

    val usageVsYesterday: UsageComparison
        get() = when {
            totalUsageYesterday == 0L -> UsageComparison.NO_COMPARISON
            totalUsageToday < totalUsageYesterday -> UsageComparison.LESS
            totalUsageToday > totalUsageYesterday -> UsageComparison.MORE
            else -> UsageComparison.SAME
        }

    val usageVsAverage: UsageComparison
        get() = when {
            weeklyAverage == 0L -> UsageComparison.NO_COMPARISON
            totalUsageToday < weeklyAverage -> UsageComparison.LESS
            totalUsageToday > weeklyAverage -> UsageComparison.MORE
            else -> UsageComparison.SAME
        }
}

/**
 * Friction level determines how difficult it is to dismiss interventions.
 * Increases gradually based on user tenure and preferences.
 */
enum class FrictionLevel(
    val delayMs: Long,
    val requiresInteraction: Boolean,
    val showGoBackButton: Boolean,
    val allowBackButton: Boolean,
    val displayName: String,
    val description: String
) {
    /**
     * Week 1-2: No delay, easy to proceed.
     * Goal: Don't scare away new users.
     */
    GENTLE(
        delayMs = 0,
        requiresInteraction = false,
        showGoBackButton = true,
        allowBackButton = false,
        displayName = "Gentle",
        description = "Simple message, no delay"
    ),

    /**
     * Week 3-4: 3 second delay with countdown.
     * Goal: Create awareness without annoyance.
     */
    MODERATE(
        delayMs = 3000,
        requiresInteraction = true,
        showGoBackButton = true,
        allowBackButton = false,
        displayName = "Moderate",
        description = "3-second pause before proceeding"
    ),

    /**
     * Week 5+: 5 second delay with required interaction.
     * Goal: Strong friction for established users.
     */
    FIRM(
        delayMs = 5000,
        requiresInteraction = true,
        showGoBackButton = true,
        allowBackButton = false,
        displayName = "Firm",
        description = "5-second pause with reflection prompts"
    ),

    /**
     * User-requested mode: 10 second delay + multi-step.
     * Goal: Maximum friction for users who want strict control.
     */
    LOCKED(
        delayMs = 10000,
        requiresInteraction = true,
        showGoBackButton = true,
        allowBackButton = false,
        displayName = "Locked",
        description = "10-second pause, maximum friction"
    );

    companion object {
        /**
         * Determines appropriate friction level based on user tenure.
         */
        fun fromDaysSinceInstall(days: Int): FrictionLevel {
            return when {
                days < 14 -> GENTLE      // First 2 weeks
                days < 28 -> MODERATE    // Weeks 3-4
                else -> FIRM             // Week 5+
            }
        }
    }
}

/**
 * Usage comparison categories
 */
enum class UsageComparison {
    LESS,           // Using less than comparison
    MORE,           // Using more than comparison
    SAME,           // Same as comparison
    NO_COMPARISON   // No data to compare
}

/**
 * Intervention type for tracking purposes
 */
enum class InterventionType {
    REMINDER,       // Launch intervention (reminder overlay)
    TIMER,          // 10-minute alert (timer overlay)
    CUSTOM          // Custom intervention
}
