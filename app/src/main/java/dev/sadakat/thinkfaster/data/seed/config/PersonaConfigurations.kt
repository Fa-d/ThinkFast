package dev.sadakat.thinkfaster.data.seed.config

import dev.sadakat.thinkfaster.data.seed.model.PersonaType
import dev.sadakat.thinkfaster.domain.intervention.FrictionLevel
import kotlin.math.absoluteValue

/**
 * Time distribution across different periods of the day
 * Must sum to 1.0
 */
data class TimeDistribution(
    val morning: Double,    // 6am-10am
    val midday: Double,     // 10am-3pm
    val evening: Double,    // 3pm-8pm
    val lateNight: Double,  // 8pm-12am
    val veryLate: Double    // 12am-6am
) {
    init {
        require((morning + midday + evening + lateNight + veryLate - 1.0).absoluteValue < 0.01) {
            "Time distribution must sum to 1.0, got ${morning + midday + evening + lateNight + veryLate}"
        }
    }

    companion object {
        val BALANCED = TimeDistribution(0.20, 0.25, 0.30, 0.20, 0.05)
        val EVENING_HEAVY = TimeDistribution(0.10, 0.15, 0.15, 0.50, 0.10)
        val LATE_NIGHT = TimeDistribution(0.05, 0.10, 0.15, 0.30, 0.40)
        val VERY_LATE_NIGHT = TimeDistribution(0.03, 0.07, 0.10, 0.20, 0.60)
        val MORNING_EVENING = TimeDistribution(0.30, 0.15, 0.30, 0.20, 0.05)
    }
}

/**
 * How users respond to interventions
 * Must sum to 1.0
 */
data class InterventionResponsePattern(
    val proceedRate: Double,      // User chooses to proceed into app
    val goBackRate: Double,       // User dismisses and goes back (successful intervention)
    val dismissedRate: Double     // Intervention dismissed without explicit choice
) {
    init {
        require((proceedRate + goBackRate + dismissedRate - 1.0).absoluteValue < 0.01) {
            "Response rates must sum to 1.0, got ${proceedRate + goBackRate + dismissedRate}"
        }
    }
}

/**
 * Decision time distribution buckets
 * Must sum to 1.0
 */
data class DecisionTimeDistribution(
    val instantRate: Double,    // <2s - instant dismissal
    val quickRate: Double,      // 2-5s - quick decision
    val moderateRate: Double,   // 5-15s - some thought
    val deliberateRate: Double  // 15s+ - careful consideration
) {
    init {
        require((instantRate + quickRate + moderateRate + deliberateRate - 1.0).absoluteValue < 0.01) {
            "Decision time rates must sum to 1.0, got ${instantRate + quickRate + moderateRate + deliberateRate}"
        }
    }
}

/**
 * Complete configuration for a user persona
 */
data class PersonaConfig(
    val personaType: PersonaType,
    val daysSinceInstall: IntRange,
    val frictionLevel: FrictionLevel,

    // Usage patterns
    val dailyUsageMinutes: IntRange,
    val sessionsPerDay: IntRange,
    val averageSessionMinutes: IntRange,
    val longestSessionMinutes: IntRange,
    val quickReopenRate: Double,  // 0.0-1.0

    // Goal behavior
    val hasGoals: Boolean,
    val goalComplianceRate: Double,  // 0.0-2.0 (>1.0 = over limit)
    val streakDays: IntRange,

    // Time patterns
    val timeDistribution: TimeDistribution,
    val weekendUsageMultiplier: Double,

    // Intervention response
    val interventionResponse: InterventionResponsePattern,
    val decisionTimeDistribution: DecisionTimeDistribution,

    // Session patterns
    val extendedSessionRate: Double,  // Rate of sessions >15 min
    val description: String
)

/**
 * All 12 persona configurations
 */
object PersonaConfigurations {

    val FRESH_INSTALL = PersonaConfig(
        personaType = PersonaType.FRESH_INSTALL,
        daysSinceInstall = 1..2,
        frictionLevel = FrictionLevel.GENTLE,

        dailyUsageMinutes = 45..75,
        sessionsPerDay = 8..12,
        averageSessionMinutes = 5..10,
        longestSessionMinutes = 15..25,
        quickReopenRate = 0.30,

        hasGoals = true,
        goalComplianceRate = 0.70,  // 70% of goal
        streakDays = 0..1,

        timeDistribution = TimeDistribution.EVENING_HEAVY,
        weekendUsageMultiplier = 1.0,

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.60,
            goBackRate = 0.30,
            dismissedRate = 0.10
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.70,
            quickRate = 0.20,
            moderateRate = 0.08,
            deliberateRate = 0.02
        ),

        extendedSessionRate = 0.15,
        description = "Brand new user, learning the app, high proceed rate, instant decisions"
    )

    val EARLY_ADOPTER = PersonaConfig(
        personaType = PersonaType.EARLY_ADOPTER,
        daysSinceInstall = 7..10,
        frictionLevel = FrictionLevel.GENTLE,

        dailyUsageMinutes = 35..55,
        sessionsPerDay = 6..10,
        averageSessionMinutes = 5..12,
        longestSessionMinutes = 12..22,
        quickReopenRate = 0.20,

        hasGoals = true,
        goalComplianceRate = 0.65,  // 50-75% range
        streakDays = 3..5,

        timeDistribution = TimeDistribution.MORNING_EVENING,
        weekendUsageMultiplier = 1.2,

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.50,
            goBackRate = 0.40,
            dismissedRate = 0.10
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.30,
            quickRate = 0.50,
            moderateRate = 0.15,
            deliberateRate = 0.05
        ),

        extendedSessionRate = 0.18,
        description = "Forming habits, learning to respond to interventions, improving compliance"
    )

    val TRANSITIONING_USER = PersonaConfig(
        personaType = PersonaType.TRANSITIONING_USER,
        daysSinceInstall = 14..16,
        frictionLevel = FrictionLevel.MODERATE,

        dailyUsageMinutes = 30..45,
        sessionsPerDay = 5..8,
        averageSessionMinutes = 6..12,
        longestSessionMinutes = 15..30,
        quickReopenRate = 0.15,

        hasGoals = true,
        goalComplianceRate = 0.70,  // 60-80% range
        streakDays = 7..10,

        timeDistribution = TimeDistribution.BALANCED,
        weekendUsageMultiplier = 1.1,

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.40,
            goBackRate = 0.50,
            dismissedRate = 0.10
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.10,
            quickRate = 0.35,
            moderateRate = 0.40,
            deliberateRate = 0.15
        ),

        extendedSessionRate = 0.20,
        description = "Just hit MODERATE friction (3s delay), adapting to new friction level"
    )

    val ESTABLISHED_USER = PersonaConfig(
        personaType = PersonaType.ESTABLISHED_USER,
        daysSinceInstall = 30..35,
        frictionLevel = FrictionLevel.FIRM,

        dailyUsageMinutes = 20..35,
        sessionsPerDay = 4..7,
        averageSessionMinutes = 5..10,
        longestSessionMinutes = 12..20,
        quickReopenRate = 0.10,

        hasGoals = true,
        goalComplianceRate = 0.60,  // Consistently under goal
        streakDays = 15..21,

        timeDistribution = TimeDistribution.BALANCED,
        weekendUsageMultiplier = 1.0,

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.30,
            goBackRate = 0.60,
            dismissedRate = 0.10
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.05,
            quickRate = 0.25,
            moderateRate = 0.35,
            deliberateRate = 0.35
        ),

        extendedSessionRate = 0.12,
        description = "Healthy patterns, FIRM friction (5s delay), long streaks, good compliance"
    )

    val LOCKED_MODE_USER = PersonaConfig(
        personaType = PersonaType.LOCKED_MODE_USER,
        daysSinceInstall = 50..60,
        frictionLevel = FrictionLevel.LOCKED,

        dailyUsageMinutes = 15..25,
        sessionsPerDay = 3..5,
        averageSessionMinutes = 5..8,
        longestSessionMinutes = 10..15,
        quickReopenRate = 0.05,

        hasGoals = true,
        goalComplianceRate = 0.45,  // Well under goal
        streakDays = 30..40,

        timeDistribution = TimeDistribution.BALANCED,
        weekendUsageMultiplier = 0.9,  // Better on weekends

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.20,
            goBackRate = 0.70,
            dismissedRate = 0.10
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.02,
            quickRate = 0.08,
            moderateRate = 0.30,
            deliberateRate = 0.60
        ),

        extendedSessionRate = 0.08,
        description = "Maximum friction mode, excellent compliance, very long streaks, deliberate decisions"
    )

    val LATE_NIGHT_SCROLLER = PersonaConfig(
        personaType = PersonaType.LATE_NIGHT_SCROLLER,
        daysSinceInstall = 18..22,
        frictionLevel = FrictionLevel.MODERATE,

        dailyUsageMinutes = 60..90,
        sessionsPerDay = 10..15,
        averageSessionMinutes = 6..12,
        longestSessionMinutes = 20..35,
        quickReopenRate = 0.25,

        hasGoals = true,
        goalComplianceRate = 1.35,  // 120-150% of goal (over limit)
        streakDays = 3..8,

        timeDistribution = TimeDistribution.VERY_LATE_NIGHT,  // 60% between 12am-6am
        weekendUsageMultiplier = 1.4,

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.55,
            goBackRate = 0.35,
            dismissedRate = 0.10
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.25,
            quickRate = 0.30,
            moderateRate = 0.30,
            deliberateRate = 0.15
        ),

        extendedSessionRate = 0.30,
        description = "70% usage 10pm-2am, over goal, struggling with late night habits"
    )

    val WEEKEND_WARRIOR = PersonaConfig(
        personaType = PersonaType.WEEKEND_WARRIOR,
        daysSinceInstall = 24..26,
        frictionLevel = FrictionLevel.MODERATE,

        dailyUsageMinutes = 30..40,  // Weekday average
        sessionsPerDay = 6..9,
        averageSessionMinutes = 5..10,
        longestSessionMinutes = 15..25,
        quickReopenRate = 0.18,

        hasGoals = true,
        goalComplianceRate = 0.75,  // Good weekdays, bad weekends
        streakDays = 3..5,  // Breaks streaks on weekends

        timeDistribution = TimeDistribution.MORNING_EVENING,
        weekendUsageMultiplier = 2.5,  // Much higher on weekends

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.45,
            goBackRate = 0.45,
            dismissedRate = 0.10
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.20,
            quickRate = 0.40,
            moderateRate = 0.30,
            deliberateRate = 0.10
        ),

        extendedSessionRate = 0.22,
        description = "2.5x usage on weekends, weekend mornings (8am-12pm) = 50% of weekend usage"
    )

    val COMPULSIVE_REOPENER = PersonaConfig(
        personaType = PersonaType.COMPULSIVE_REOPENER,
        daysSinceInstall = 17..19,
        frictionLevel = FrictionLevel.MODERATE,

        dailyUsageMinutes = 80..120,
        sessionsPerDay = 20..30,
        averageSessionMinutes = 4..6,  // Many short sessions
        longestSessionMinutes = 15..25,
        quickReopenRate = 0.60,  // Very high quick reopen rate

        hasGoals = true,
        goalComplianceRate = 1.75,  // 150-200% of goal (way over)
        streakDays = 1..2,

        timeDistribution = TimeDistribution.BALANCED,  // Throughout the day
        weekendUsageMultiplier = 1.3,

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.70,  // Habit override
            goBackRate = 0.25,
            dismissedRate = 0.05
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.80,  // Very quick dismissals
            quickRate = 0.15,
            moderateRate = 0.04,
            deliberateRate = 0.01
        ),

        extendedSessionRate = 0.10,
        description = "60% quick reopen rate, 20-30 sessions/day, compulsive checking, instant dismissals"
    )

    val GOAL_SKIPPER = PersonaConfig(
        personaType = PersonaType.GOAL_SKIPPER,
        daysSinceInstall = 20..24,
        frictionLevel = FrictionLevel.MODERATE,

        dailyUsageMinutes = 70..100,
        sessionsPerDay = 12..18,
        averageSessionMinutes = 5..10,
        longestSessionMinutes = 15..30,
        quickReopenRate = 0.20,

        hasGoals = false,  // No goals set
        goalComplianceRate = 0.0,  // N/A
        streakDays = 0..0,  // No streaks without goals

        timeDistribution = TimeDistribution.BALANCED,
        weekendUsageMultiplier = 1.4,

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.65,
            goBackRate = 0.25,
            dismissedRate = 0.10
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.40,
            quickRate = 0.35,
            moderateRate = 0.20,
            deliberateRate = 0.05
        ),

        extendedSessionRate = 0.25,
        description = "No goals set, pure usage tracking, no goal-based interventions or streaks"
    )

    val OVER_LIMIT_STRUGGLER = PersonaConfig(
        personaType = PersonaType.OVER_LIMIT_STRUGGLER,
        daysSinceInstall = 27..29,
        frictionLevel = FrictionLevel.FIRM,

        dailyUsageMinutes = 90..120,
        sessionsPerDay = 10..15,
        averageSessionMinutes = 8..15,
        longestSessionMinutes = 25..40,
        quickReopenRate = 0.30,

        hasGoals = true,
        goalComplianceRate = 1.75,  // 150-200% of 45-min goal
        streakDays = 0..2,

        timeDistribution = TimeDistribution.EVENING_HEAVY,
        weekendUsageMultiplier = 1.6,

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.60,  // Struggling to comply
            goBackRate = 0.35,
            dismissedRate = 0.05
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.40,
            quickRate = 0.30,
            moderateRate = 0.20,
            deliberateRate = 0.10
        ),

        extendedSessionRate = 0.35,  // 30-40% extended sessions
        description = "Consistently over goal (150-200%), many extended sessions, struggling with compliance"
    )

    val STREAK_ACHIEVER = PersonaConfig(
        personaType = PersonaType.STREAK_ACHIEVER,
        daysSinceInstall = 43..47,
        frictionLevel = FrictionLevel.FIRM,

        dailyUsageMinutes = 18..30,
        sessionsPerDay = 4..6,
        averageSessionMinutes = 4..8,
        longestSessionMinutes = 10..18,
        quickReopenRate = 0.08,

        hasGoals = true,
        goalComplianceRate = 0.55,  // 50-80% of goal (well under)
        streakDays = 25..35,

        timeDistribution = TimeDistribution.BALANCED,
        weekendUsageMultiplier = 0.9,

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.25,
            goBackRate = 0.65,
            dismissedRate = 0.10
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.05,
            quickRate = 0.20,
            moderateRate = 0.30,
            deliberateRate = 0.45
        ),

        extendedSessionRate = 0.08,
        description = "25-35 day streak, high gamification content, excellent compliance, deliberate decisions"
    )

    val REALISTIC_MIXED = PersonaConfig(
        personaType = PersonaType.REALISTIC_MIXED,
        daysSinceInstall = 22..26,
        frictionLevel = FrictionLevel.MODERATE,

        dailyUsageMinutes = 40..70,
        sessionsPerDay = 7..12,
        averageSessionMinutes = 5..12,
        longestSessionMinutes = 15..30,
        quickReopenRate = 0.18,

        hasGoals = true,
        goalComplianceRate = 0.85,  // Sometimes over (40%), sometimes under (60%)
        streakDays = 5..12,

        timeDistribution = TimeDistribution.BALANCED,
        weekendUsageMultiplier = 1.3,

        interventionResponse = InterventionResponsePattern(
            proceedRate = 0.50,
            goBackRate = 0.40,
            dismissedRate = 0.10
        ),
        decisionTimeDistribution = DecisionTimeDistribution(
            instantRate = 0.25,
            quickRate = 0.35,
            moderateRate = 0.30,
            deliberateRate = 0.10
        ),

        extendedSessionRate = 0.20,
        description = "Average realistic user, mixed patterns, balanced distributions, some late nights"
    )

    /**
     * Get configuration for a specific persona type
     */
    fun getConfig(personaType: PersonaType): PersonaConfig {
        return when (personaType) {
            PersonaType.FRESH_INSTALL -> FRESH_INSTALL
            PersonaType.EARLY_ADOPTER -> EARLY_ADOPTER
            PersonaType.TRANSITIONING_USER -> TRANSITIONING_USER
            PersonaType.ESTABLISHED_USER -> ESTABLISHED_USER
            PersonaType.LOCKED_MODE_USER -> LOCKED_MODE_USER
            PersonaType.LATE_NIGHT_SCROLLER -> LATE_NIGHT_SCROLLER
            PersonaType.WEEKEND_WARRIOR -> WEEKEND_WARRIOR
            PersonaType.COMPULSIVE_REOPENER -> COMPULSIVE_REOPENER
            PersonaType.GOAL_SKIPPER -> GOAL_SKIPPER
            PersonaType.OVER_LIMIT_STRUGGLER -> OVER_LIMIT_STRUGGLER
            PersonaType.STREAK_ACHIEVER -> STREAK_ACHIEVER
            PersonaType.REALISTIC_MIXED -> REALISTIC_MIXED
        }
    }
}
