package dev.sadakat.thinkfaster.presentation.widget

import android.content.Context
import dev.sadakat.thinkfaster.R
import dev.sadakat.thinkfaster.domain.model.ProgressColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Widget data stored in SharedPreferences
 */
data class WidgetData(
    val totalUsedMinutes: Int,
    val totalGoalMinutes: Int,
    val percentageUsed: Int,
    val isOverLimit: Boolean,
    val lastUpdateTimestamp: Long = System.currentTimeMillis()
) {
    fun formatWidgetText(): String {
        val usedText = when {
            totalUsedMinutes >= 60 -> {
                val hours = totalUsedMinutes / 60
                val mins = totalUsedMinutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
            else -> "${totalUsedMinutes}m"
        }
        val goalText = when {
            totalGoalMinutes >= 60 -> {
                val hours = totalGoalMinutes / 60
                val mins = totalGoalMinutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
            else -> "${totalGoalMinutes}m"
        }
        return "$usedText / $goalText"
    }

    fun getStatusMessage(context: Context): String {
        return when {
            totalGoalMinutes == 0 -> context.getString(R.string.widget_no_goals)
            isOverLimit -> context.getString(R.string.widget_over_limit)
            percentageUsed >= 90 -> context.getString(R.string.widget_almost_limit)
            percentageUsed >= 75 -> context.getString(R.string.widget_doing_well)
            percentageUsed >= 50 -> context.getString(R.string.widget_on_track)
            totalUsedMinutes == 0 -> context.getString(R.string.widget_no_usage)
            else -> context.getString(R.string.widget_great_start)
        }
    }

    fun getRemainingMinutes(): Int {
        return max(0, totalGoalMinutes - totalUsedMinutes)
    }

    fun formatRemainingTime(context: Context): String {
        val remaining = getRemainingMinutes()
        if (remaining == 0 || isOverLimit) {
            return context.getString(R.string.widget_no_remaining_time)
        }
        val remainingText = when {
            remaining >= 60 -> {
                val hours = remaining / 60
                val mins = remaining % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
            else -> "${remaining}m"
        }
        return remainingText
    }

    fun formatLastUpdate(context: Context): String {
        val now = System.currentTimeMillis()
        val diffMinutes = ((now - lastUpdateTimestamp) / 1000 / 60).toInt()

        return when {
            diffMinutes < 1 -> context.getString(R.string.widget_updated_now)
            diffMinutes < 60 -> context.getString(R.string.widget_updated_minutes_ago, diffMinutes)
            else -> {
                val hours = diffMinutes / 60
                context.getString(R.string.widget_updated_hours_ago, hours)
            }
        }
    }

    fun getProgressColor(): ProgressColor {
        return when {
            totalGoalMinutes == 0 -> ProgressColor.GREEN
            isOverLimit -> ProgressColor.RED
            percentageUsed >= 90 -> ProgressColor.ORANGE
            percentageUsed >= 75 -> ProgressColor.YELLOW
            else -> ProgressColor.GREEN
        }
    }

    companion object {
        fun empty() = WidgetData(0, 0, 0, false, System.currentTimeMillis())

        private const val PREFS_NAME = "widget_data"
        private const val KEY_USED = "total_used_minutes"
        private const val KEY_GOAL = "total_goal_minutes"
        private const val KEY_PERCENTAGE = "percentage_used"
        private const val KEY_OVER_LIMIT = "is_over_limit"
        private const val KEY_LAST_UPDATE = "last_update"

        fun save(context: Context, data: WidgetData) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putInt(KEY_USED, data.totalUsedMinutes)
                putInt(KEY_GOAL, data.totalGoalMinutes)
                putInt(KEY_PERCENTAGE, data.percentageUsed)
                putBoolean(KEY_OVER_LIMIT, data.isOverLimit)
                putLong(KEY_LAST_UPDATE, data.lastUpdateTimestamp)
                apply()
            }
        }

        fun load(context: Context): WidgetData {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return WidgetData(
                totalUsedMinutes = prefs.getInt(KEY_USED, 0),
                totalGoalMinutes = prefs.getInt(KEY_GOAL, 0),
                percentageUsed = prefs.getInt(KEY_PERCENTAGE, 0),
                isOverLimit = prefs.getBoolean(KEY_OVER_LIMIT, false),
                lastUpdateTimestamp = prefs.getLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            )
        }
    }
}

/**
 * Update widget data - call this from the main app when usage data changes
 */
suspend fun updateWidgetData(context: Context, totalUsedMinutes: Int, totalGoalMinutes: Int) {
    val percentageUsed = if (totalGoalMinutes > 0) {
        ((totalUsedMinutes.toFloat() / totalGoalMinutes.toFloat()) * 100).toInt()
    } else {
        0
    }
    val isOverLimit = totalGoalMinutes > 0 && totalUsedMinutes > totalGoalMinutes

    val data = WidgetData(
        totalUsedMinutes = totalUsedMinutes,
        totalGoalMinutes = totalGoalMinutes,
        percentageUsed = percentageUsed,
        isOverLimit = isOverLimit,
        lastUpdateTimestamp = System.currentTimeMillis()
    )

    WidgetData.save(context, data)
    triggerGlanceWidgetUpdateAsync(context)
}

/**
 * Refresh widget data from database and update widget
 * Call this when the app opens or when goals are updated
 */
suspend fun refreshWidgetData(
    context: Context,
    goalRepository: dev.sadakat.thinkfaster.domain.repository.GoalRepository,
    usageRepository: dev.sadakat.thinkfaster.domain.repository.UsageRepository
) {
    try {
        val goals = goalRepository.getAllGoals()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        var totalUsedMinutes = 0
        var totalGoalMinutes = 0

        for (goal in goals) {
            totalGoalMinutes += goal.dailyLimitMinutes
            val sessions = usageRepository.getSessionsByAppInRange(
                targetApp = goal.targetApp,
                startDate = today,
                endDate = today
            )
            totalUsedMinutes += (sessions.sumOf { it.duration } / 1000 / 60).toInt()
        }

        updateWidgetData(context, totalUsedMinutes, totalGoalMinutes)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Convenience function to refresh widget data - for use from places without repository access
 */
fun refreshWidgetData(context: Context) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            // Try to get repositories from Koin
            val goalRepository = org.koin.core.context.GlobalContext.getOrNull()
                ?.get<dev.sadakat.thinkfaster.domain.repository.GoalRepository>()
            val usageRepository = org.koin.core.context.GlobalContext.getOrNull()
                ?.get<dev.sadakat.thinkfaster.domain.repository.UsageRepository>()

            if (goalRepository != null && usageRepository != null) {
                refreshWidgetData(context, goalRepository, usageRepository)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
