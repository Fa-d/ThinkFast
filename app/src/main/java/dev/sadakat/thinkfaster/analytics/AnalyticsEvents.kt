package dev.sadakat.thinkfaster.analytics

/**
 * Analytics event names and parameter keys
 * Follows Firebase Analytics naming conventions
 */
object AnalyticsEvents {
    // App Lifecycle Events
    const val APP_LAUNCHED = "app_launched"
    const val SESSION_START = "session_start"
    const val SESSION_END = "session_end"

    // Onboarding Events
    const val ONBOARDING_STARTED = "onboarding_started"
    const val ONBOARDING_STEP_COMPLETED = "onboarding_step_completed"
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val ONBOARDING_SKIPPED = "onboarding_skipped"
    const val QUEST_STEP_COMPLETED = "quest_step_completed"
    const val QUEST_COMPLETED = "quest_completed"

    // Goal Events
    const val GOAL_CREATED = "goal_created"
    const val GOAL_UPDATED = "goal_updated"
    const val GOAL_ACHIEVED = "goal_achieved"
    const val GOAL_EXCEEDED = "goal_exceeded"

    // Streak Events
    const val STREAK_STARTED = "streak_started"
    const val STREAK_MILESTONE = "streak_milestone"
    const val STREAK_BROKEN = "streak_broken"
    const val STREAK_FREEZE_ACTIVATED = "streak_freeze_activated"
    const val STREAK_RECOVERY_STARTED = "streak_recovery_started"
    const val STREAK_RECOVERY_COMPLETED = "streak_recovery_completed"

    // Intervention Events (already tracked, but adding for completeness)
    const val INTERVENTION_SHOWN = "intervention_shown"
    const val INTERVENTION_DISMISSED = "intervention_dismissed"
    const val INTERVENTION_SUCCEEDED = "intervention_succeeded"
    const val INTERVENTION_SNOOZED = "intervention_snoozed"
    const val FEEDBACK_PROVIDED = "feedback_provided"

    // Settings Events
    const val SETTINGS_CHANGED = "settings_changed"
    const val FRICTION_LEVEL_CHANGED = "friction_level_changed"
    const val LOCKED_MODE_TOGGLED = "locked_mode_toggled"
    const val WORKING_MODE_TOGGLED = "working_mode_toggled"
    const val TIMER_DURATION_CHANGED = "timer_duration_changed"
    const val NOTIFICATIONS_TOGGLED = "notifications_toggled"

    // Feature Adoption Events
    const val FIRST_GOAL_SET = "first_goal_set"
    const val FIRST_INTERVENTION = "first_intervention"
    const val FIRST_FEEDBACK = "first_feedback"
    const val FIRST_STREAK_FREEZE = "first_streak_freeze"
    const val BASELINE_CALCULATED = "baseline_calculated"

    // User Setup Completion Event
    // Tracked when user completes onboarding AND grants all 3 permissions
    const val USER_READY = "user_ready"

    // Parameter Keys
    object Params {
        const val SESSION_DURATION_MS = "session_duration_ms"
        const val ONBOARDING_STEP = "onboarding_step"
        const val GOAL_MINUTES = "goal_minutes"
        const val TARGET_APP = "target_app"
        const val APP_CATEGORY = "app_category"
        const val STREAK_DAYS = "streak_days"
        const val MILESTONE_TYPE = "milestone_type"
        const val SETTING_NAME = "setting_name"
        const val SETTING_VALUE = "setting_value"
        const val FRICTION_LEVEL = "friction_level"
        const val INTERVENTION_TYPE = "intervention_type"
        const val USER_CHOICE = "user_choice"
        const val FEEDBACK_TYPE = "feedback_type"
        const val DAYS_SINCE_INSTALL = "days_since_install"
        const val STEP_NAME = "step_name"
        const val PREVIOUS_STREAK = "previous_streak"
        const val RECOVERY_STARTED = "recovery_started"
    }
}
