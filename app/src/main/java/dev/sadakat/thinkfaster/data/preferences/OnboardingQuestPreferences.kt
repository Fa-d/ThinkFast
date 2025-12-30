package dev.sadakat.thinkfaster.data.preferences

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * OnboardingQuestPreferences - Manages 7-day quest state
 * First-Week Retention Feature - Phase 1.1: Data Layer
 *
 * Tracks quest progress, completion flags, and celebration states
 * Uses SharedPreferences for lightweight, fast access
 */
class OnboardingQuestPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private const val PREFS_NAME = "onboarding_quest_preferences"

        // Quest state keys
        private const val KEY_QUEST_STARTED = "quest_started"
        private const val KEY_QUEST_START_DATE = "quest_start_date"
        private const val KEY_QUEST_COMPLETED = "quest_completed"
        private const val KEY_QUEST_COMPLETION_DATE = "quest_completion_date"

        // Milestone keys (day 1-7)
        private fun getDayMilestoneKey(day: Int) = "day_${day}_milestone_shown"

        // Quick win celebration keys
        private const val KEY_FIRST_SESSION_CELEBRATED = "first_session_celebrated"
        private const val KEY_FIRST_UNDER_GOAL_CELEBRATED = "first_under_goal_celebrated"
    }

    /**
     * Start the 7-day quest
     * @param startDate The date when first goal was set ("yyyy-MM-dd")
     */
    fun startQuest(startDate: String) {
        prefs.edit()
            .putBoolean(KEY_QUEST_STARTED, true)
            .putString(KEY_QUEST_START_DATE, startDate)
            .apply()
    }

    /**
     * Check if quest is currently active
     * @return true if quest started but not completed
     */
    fun isQuestActive(): Boolean {
        val started = prefs.getBoolean(KEY_QUEST_STARTED, false)
        val completed = prefs.getBoolean(KEY_QUEST_COMPLETED, false)

        if (!started || completed) return false

        // Check if quest expired (more than 7 days since start)
        val startDate = prefs.getString(KEY_QUEST_START_DATE, null) ?: return false
        val currentDay = getCurrentQuestDay()

        return currentDay in 1..7
    }

    /**
     * Get current quest day (1-7)
     * @return Current day number, or 0 if quest not active
     */
    fun getCurrentQuestDay(): Int {
        if (!prefs.getBoolean(KEY_QUEST_STARTED, false)) return 0

        val startDateStr = prefs.getString(KEY_QUEST_START_DATE, null) ?: return 0

        try {
            val startDate = dateFormat.parse(startDateStr) ?: return 0
            val today = Date()

            val startCal = Calendar.getInstance().apply { time = startDate }
            val todayCal = Calendar.getInstance().apply { time = today }

            // Calculate days between start and today
            val daysDiff = daysBetween(startCal, todayCal)
            val currentDay = daysDiff + 1

            // Return 0 if beyond 7 days
            return if (currentDay in 1..7) currentDay else 0
        } catch (e: Exception) {
            return 0
        }
    }

    /**
     * Mark a quest day as completed
     * @param day The day number (1-7)
     */
    fun markDayCompleted(day: Int) {
        if (day in 1..7) {
            prefs.edit()
                .putBoolean(getDayMilestoneKey(day), true)
                .apply()
        }
    }

    /**
     * Check if a day milestone has been shown
     * @param day The day number (1-7)
     * @return true if milestone already shown
     */
    fun isDayMilestoneShown(day: Int): Boolean {
        return if (day in 1..7) {
            prefs.getBoolean(getDayMilestoneKey(day), false)
        } else {
            false
        }
    }

    /**
     * Mark the entire quest as complete
     * @param date The completion date ("yyyy-MM-dd")
     */
    fun markQuestComplete(date: String) {
        prefs.edit()
            .putBoolean(KEY_QUEST_COMPLETED, true)
            .putString(KEY_QUEST_COMPLETION_DATE, date)
            .apply()
    }

    /**
     * Check if quest has been completed
     * @return true if quest finished
     */
    fun isQuestCompleted(): Boolean {
        return prefs.getBoolean(KEY_QUEST_COMPLETED, false)
    }

    /**
     * Check if first session celebration was shown
     * @return true if already celebrated
     */
    fun isFirstSessionCelebrated(): Boolean {
        return prefs.getBoolean(KEY_FIRST_SESSION_CELEBRATED, false)
    }

    /**
     * Mark first session celebration as shown
     */
    fun markFirstSessionCelebrated() {
        prefs.edit()
            .putBoolean(KEY_FIRST_SESSION_CELEBRATED, true)
            .apply()
    }

    /**
     * Check if first under goal celebration was shown
     * @return true if already celebrated
     */
    fun isFirstUnderGoalCelebrated(): Boolean {
        return prefs.getBoolean(KEY_FIRST_UNDER_GOAL_CELEBRATED, false)
    }

    /**
     * Mark first under goal celebration as shown
     */
    fun markFirstUnderGoalCelebrated() {
        prefs.edit()
            .putBoolean(KEY_FIRST_UNDER_GOAL_CELEBRATED, true)
            .apply()
    }

    /**
     * Calculate days between two calendar dates
     */
    private fun daysBetween(start: Calendar, end: Calendar): Int {
        // Normalize to start of day
        val startDay = Calendar.getInstance().apply {
            time = start.time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endDay = Calendar.getInstance().apply {
            time = end.time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffInMillis = endDay.timeInMillis - startDay.timeInMillis
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }
}
