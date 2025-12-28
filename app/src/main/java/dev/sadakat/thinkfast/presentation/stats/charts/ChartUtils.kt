package dev.sadakat.thinkfast.presentation.stats.charts

import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import dev.sadakat.thinkfast.domain.model.UsageSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Shared utilities for chart implementations
 */
object ChartColors {
    const val GOAL_GREEN = 0xFF4CAF50.toInt()
    const val OVER_GOAL_RED = 0xFFFF5252.toInt()
    const val GRID_LIGHT_GRAY = 0xFFE0E0E0.toInt()

    // Dynamic color palette for tracked apps
    val APP_COLORS = listOf(
        0xFF1877F2.toInt(),  // Blue (Facebook)
        0xFFE4405F.toInt(),  // Pink (Instagram)
        0xFF1DA1F2.toInt(),  // Twitter Blue
        0xFFFF0000.toInt(),  // YouTube Red
        0xFF25D366.toInt(),  // WhatsApp Green
        0xFFFF6600.toInt(),  // Reddit Orange
        0xFF0077B5.toInt(),  // LinkedIn Blue
        0xFFFFFC00.toInt(),  // Snapchat Yellow
        0xFFE60023.toInt(),  // Pinterest Red
        0xFF00AFF0.toInt()   // TikTok Cyan
    )

    /**
     * Get color for an app by its index in tracked apps list
     */
    fun getColorForApp(index: Int): Int {
        return APP_COLORS[index % APP_COLORS.size]
    }
}

/**
 * Time period definitions for usage patterns
 */
enum class TimePeriod(val label: String, val hourRange: IntRange) {
    NIGHT("Night (0-6)", 0..5),
    MORNING("Morning (6-12)", 6..11),
    AFTERNOON("Afternoon (12-18)", 12..17),
    EVENING("Evening (18-24)", 18..23)
}

/**
 * Get hour from timestamp
 */
fun getHourFromTimestamp(timestamp: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar.get(Calendar.HOUR_OF_DAY)
}

/**
 * Get day of week from timestamp (1 = Monday, 7 = Sunday)
 */
fun getDayOfWeek(timestamp: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    // Convert Sunday=1 to Sunday=7
    return if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
}

/**
 * Get day of month from timestamp
 */
fun getDayOfMonth(timestamp: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar.get(Calendar.DAY_OF_MONTH)
}

/**
 * Get time period for a given timestamp
 */
fun getTimePeriod(timestamp: Long): TimePeriod {
    val hour = getHourFromTimestamp(timestamp)
    return TimePeriod.values().first { hour in it.hourRange }
}

/**
 * Format hour for display (e.g., "14:00")
 */
fun formatHour(hour: Int): String {
    return String.format(Locale.getDefault(), "%02d:00", hour)
}

/**
 * Format day of week for display (e.g., "Mon")
 */
fun formatDayOfWeek(day: Int): String {
    return when (day) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> ""
    }
}

/**
 * Value formatter for hour labels
 */
class HourValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return formatHour(value.toInt())
    }
}

/**
 * Value formatter for day of week labels
 */
class DayOfWeekValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return formatDayOfWeek(value.toInt())
    }
}

/**
 * Value formatter for day of month labels
 */
class DayOfMonthValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return value.toInt().toString()
    }
}

/**
 * Value formatter for minutes display (e.g., "45m")
 */
class MinutesValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return if (value >= 1f) "${value.toInt()}m" else ""
    }
}

/**
 * Configure common X-axis styling
 */
fun XAxis.applyCommonStyling() {
    position = XAxis.XAxisPosition.BOTTOM
    setDrawGridLines(false)
    setDrawAxisLine(true)
    textSize = 10f
    granularity = 1f
}

/**
 * Configure common Y-axis styling
 */
fun YAxis.applyCommonStyling() {
    setDrawGridLines(true)
    setDrawAxisLine(true)
    axisMinimum = 0f
    textSize = 10f
    gridColor = ChartColors.GRID_LIGHT_GRAY
}

/**
 * Data class for aggregated usage by time slot
 * Now supports any number of tracked apps dynamically
 */
data class TimeSlotUsage(
    val timeSlot: Float,
    val appUsage: Map<String, Float>  // Map of packageName to minutes
) {
    val totalMinutes: Float
        get() = appUsage.values.sum()
}

/**
 * Aggregate sessions by hour of day
 */
fun aggregateSessionsByHour(sessions: List<UsageSession>): List<TimeSlotUsage> {
    val hourlyData = mutableMapOf<Int, MutableMap<String, Float>>()

    // Initialize all 24 hours with empty maps
    for (hour in 0..23) {
        hourlyData[hour] = mutableMapOf()
    }

    // Aggregate sessions by app
    sessions.forEach { session ->
        val hour = getHourFromTimestamp(session.startTimestamp)
        val durationMinutes = session.duration / 60000f

        val appMap = hourlyData[hour] ?: mutableMapOf()
        appMap[session.targetApp] = (appMap[session.targetApp] ?: 0f) + durationMinutes
        hourlyData[hour] = appMap
    }

    return hourlyData.map { (hour, appUsage) ->
        TimeSlotUsage(hour.toFloat(), appUsage)
    }.sortedBy { it.timeSlot }
}

/**
 * Aggregate sessions by day of week
 */
fun aggregateSessionsByDayOfWeek(sessions: List<UsageSession>): List<TimeSlotUsage> {
    val dailyData = mutableMapOf<Int, MutableMap<String, Float>>()

    // Initialize all 7 days with empty maps
    for (day in 1..7) {
        dailyData[day] = mutableMapOf()
    }

    // Aggregate sessions by app
    sessions.forEach { session ->
        val day = getDayOfWeek(session.startTimestamp)
        val durationMinutes = session.duration / 60000f

        val appMap = dailyData[day] ?: mutableMapOf()
        appMap[session.targetApp] = (appMap[session.targetApp] ?: 0f) + durationMinutes
        dailyData[day] = appMap
    }

    return dailyData.map { (day, appUsage) ->
        TimeSlotUsage(day.toFloat(), appUsage)
    }.sortedBy { it.timeSlot }
}

/**
 * Aggregate sessions by day of month
 */
fun aggregateSessionsByDayOfMonth(sessions: List<UsageSession>): List<TimeSlotUsage> {
    val dailyData = mutableMapOf<Int, MutableMap<String, Float>>()

    // Get the range of days in the sessions
    if (sessions.isEmpty()) return emptyList()

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = sessions.first().startTimestamp
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Initialize all days with empty maps
    for (day in 1..daysInMonth) {
        dailyData[day] = mutableMapOf()
    }

    // Aggregate sessions by app
    sessions.forEach { session ->
        val day = getDayOfMonth(session.startTimestamp)
        val durationMinutes = session.duration / 60000f

        val appMap = dailyData[day] ?: mutableMapOf()
        appMap[session.targetApp] = (appMap[session.targetApp] ?: 0f) + durationMinutes
        dailyData[day] = appMap
    }

    return dailyData.map { (day, appUsage) ->
        TimeSlotUsage(day.toFloat(), appUsage)
    }.sortedBy { it.timeSlot }
}

/**
 * Aggregate sessions by time period
 */
fun aggregateSessionsByTimePeriod(sessions: List<UsageSession>): Map<TimePeriod, Map<String, Float>> {
    val periodData = mutableMapOf<TimePeriod, MutableMap<String, Float>>()

    // Initialize all periods with empty maps
    TimePeriod.entries.forEach { period ->
        periodData[period] = mutableMapOf()
    }

    // Aggregate sessions by app
    sessions.forEach { session ->
        val period = getTimePeriod(session.startTimestamp)
        val durationMinutes = session.duration / 60000f

        val appMap = periodData[period] ?: mutableMapOf()
        appMap[session.targetApp] = (appMap[session.targetApp] ?: 0f) + durationMinutes
        periodData[period] = appMap
    }

    return periodData
}
