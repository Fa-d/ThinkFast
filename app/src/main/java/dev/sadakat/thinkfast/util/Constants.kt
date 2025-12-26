package dev.sadakat.thinkfast.util

object Constants {
    // Target Apps
    const val FACEBOOK_PACKAGE = "com.facebook.katana"
    const val INSTAGRAM_PACKAGE = "com.instagram.android"

    // Timing
    // Adaptive polling intervals based on state
    const val POLLING_INTERVAL_ACTIVE = 1500L  // 1.5 seconds when target app is active
    const val POLLING_INTERVAL_IDLE = 5000L  // 5 seconds when no target app active (screen on)
    const val POLLING_INTERVAL_INACTIVE = 30000L  // 30 seconds when screen is off
    const val POLLING_INTERVAL_POWER_SAVE = 10000L  // 10 seconds in power save mode

    const val SESSION_GAP_THRESHOLD = 30000L  // 30 seconds

    // Time before switching to idle polling
    const val IDLE_THRESHOLD_MS = 60000L  // 1 minute of no target app before slower polling
    const val TEN_MINUTES_MILLIS = 600000L  // 10 minutes
    const val MIN_SESSION_DURATION = 5000L  // 5 seconds

    // Database
    const val DATABASE_NAME = "thinkfast_db"
    const val DATABASE_VERSION = 1
    const val EVENT_BATCH_SIZE = 10
    const val EVENT_FLUSH_INTERVAL = 5000L  // 5 seconds

    // Notifications
    const val CHANNEL_ID = "thinkfast_monitoring"
    const val CHANNEL_NAME = "Usage Monitoring"
    const val CHANNEL_DESCRIPTION = "Persistent notification for app usage monitoring"
    const val NOTIFICATION_ID = 1001

    // Service Actions
    const val ACTION_START_MONITORING = "dev.sadakat.thinkfast.START_MONITORING"
    const val ACTION_STOP_MONITORING = "dev.sadakat.thinkfast.STOP_MONITORING"
    const val ACTION_SESSION_RESUMED = "dev.sadakat.thinkfast.SESSION_RESUMED"

    // Intent Extras
    const val EXTRA_SESSION_ID = "extra_session_id"
    const val EXTRA_TARGET_APP = "extra_target_app"

    // WorkManager
    const val DAILY_AGGREGATION_WORK = "daily_stats_aggregation"
    const val DATA_CLEANUP_WORK = "data_cleanup"
    const val CLEANUP_AFTER_DAYS = 90

    // Preferences
    const val PREF_NAME = "thinkfast_prefs"
    const val PREF_MONITORING_ENABLED = "monitoring_enabled"
    const val PREF_DAILY_GOAL_MINUTES = "daily_goal_minutes"
    const val PREF_REMINDER_MESSAGE = "reminder_message"
    const val PREF_VIBRATE_ON_ALERT = "vibrate_on_alert"
    const val PREF_FIRST_LAUNCH = "first_launch"

    // Default Values
    const val DEFAULT_DAILY_GOAL_MINUTES = 30
    const val DEFAULT_REMINDER_MESSAGE = "Think Before You Scroll"

    // Event Types
    const val EVENT_APP_OPENED = "app_opened"
    const val EVENT_APP_CLOSED = "app_closed"
    const val EVENT_SESSION_ENDED = "session_ended"
    const val EVENT_SCREEN_ON = "screen_on"
    const val EVENT_SCREEN_OFF = "screen_off"
    const val EVENT_ALERT_SHOWN = "alert_shown"
    const val EVENT_REMINDER_SHOWN = "reminder_shown"
    const val EVENT_PROCEED_CLICKED = "proceed_clicked"
    const val EVENT_TEN_MIN_ALERT = "ten_minute_alert"
    const val EVENT_TIMER_ALERT_SHOWN = "timer_alert_shown"
    const val EVENT_TIMER_ACKNOWLEDGED = "timer_acknowledged"

    // Interruption Types
    const val INTERRUPTION_TEN_MINUTE_ALERT = "10_minute_alert"
    const val INTERRUPTION_MANUAL = "manual"
    const val INTERRUPTION_SCREEN_OFF = "screen_off"
    const val INTERRUPTION_APP_SWITCH = "app_switch"
}
