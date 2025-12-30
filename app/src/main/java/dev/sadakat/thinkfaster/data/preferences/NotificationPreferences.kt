package dev.sadakat.thinkfaster.data.preferences

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "notification_preferences"

        // Master toggle
        private const val KEY_MOTIVATIONAL_NOTIFICATIONS_ENABLED = "motivational_notifications_enabled"

        // Timing keys
        private const val KEY_MORNING_HOUR = "morning_notification_hour"
        private const val KEY_MORNING_MINUTE = "morning_notification_minute"
        private const val KEY_EVENING_HOUR = "evening_notification_hour"
        private const val KEY_EVENING_MINUTE = "evening_notification_minute"

        // Tracking keys (for 2-3 per day limit)
        private const val KEY_LAST_NOTIFICATION_DATE = "last_notification_date"
        private const val KEY_NOTIFICATION_COUNT_TODAY = "notification_count_today"

        // Defaults
        private const val DEFAULT_MORNING_HOUR = 8
        private const val DEFAULT_MORNING_MINUTE = 0
        private const val DEFAULT_EVENING_HOUR = 20
        private const val DEFAULT_EVENING_MINUTE = 0

        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    // Master toggle
    fun setMotivationalNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MOTIVATIONAL_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isMotivationalNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_MOTIVATIONAL_NOTIFICATIONS_ENABLED, true)
    }

    // Morning time
    fun setMorningTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_MORNING_HOUR, hour)
            .putInt(KEY_MORNING_MINUTE, minute)
            .apply()
    }

    fun getMorningHour(): Int {
        return prefs.getInt(KEY_MORNING_HOUR, DEFAULT_MORNING_HOUR)
    }

    fun getMorningMinute(): Int {
        return prefs.getInt(KEY_MORNING_MINUTE, DEFAULT_MORNING_MINUTE)
    }

    // Evening time
    fun setEveningTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_EVENING_HOUR, hour)
            .putInt(KEY_EVENING_MINUTE, minute)
            .apply()
    }

    fun getEveningHour(): Int {
        return prefs.getInt(KEY_EVENING_HOUR, DEFAULT_EVENING_HOUR)
    }

    fun getEveningMinute(): Int {
        return prefs.getInt(KEY_EVENING_MINUTE, DEFAULT_EVENING_MINUTE)
    }

    // Daily limit tracking
    fun incrementNotificationCount() {
        resetCountIfNewDay()
        val currentCount = getNotificationCountToday()
        prefs.edit()
            .putInt(KEY_NOTIFICATION_COUNT_TODAY, currentCount + 1)
            .apply()
    }

    fun getNotificationCountToday(): Int {
        resetCountIfNewDay()
        return prefs.getInt(KEY_NOTIFICATION_COUNT_TODAY, 0)
    }

    fun canSendNotificationToday(): Boolean {
        return getNotificationCountToday() < 3
    }

    private fun resetCountIfNewDay() {
        val today = DATE_FORMAT.format(Date())
        val lastDate = prefs.getString(KEY_LAST_NOTIFICATION_DATE, "")

        if (lastDate != today) {
            prefs.edit()
                .putString(KEY_LAST_NOTIFICATION_DATE, today)
                .putInt(KEY_NOTIFICATION_COUNT_TODAY, 0)
                .apply()
        }
    }
}
