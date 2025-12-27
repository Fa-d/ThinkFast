package dev.sadakat.thinkfast.presentation.stats.charts

import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.UsageSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Shared utilities for chart implementations
 */
object ChartColors {
    const val FACEBOOK_BLUE = 0xFF1877F2.toInt()
    const val INSTAGRAM_PINK = 0xFFE4405F.toInt()
    const val GOAL_GREEN = 0xFF4CAF50.toInt()
    const val OVER_GOAL_RED = 0xFFFF5252.toInt()
    const val GRID_LIGHT_GRAY = 0xFFE0E0E0.toInt()
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
 */
data class TimeSlotUsage(
    val timeSlot: Float,
    val facebookMinutes: Float,
    val instagramMinutes: Float
) {
    val totalMinutes: Float
        get() = facebookMinutes + instagramMinutes
}

/**
 * Aggregate sessions by hour of day
 */
fun aggregateSessionsByHour(sessions: List<UsageSession>): List<TimeSlotUsage> {
    val hourlyData = mutableMapOf<Int, Pair<Float, Float>>()

    // Initialize all 24 hours with 0
    for (hour in 0..23) {
        hourlyData[hour] = Pair(0f, 0f)
    }

    // Aggregate sessions
    sessions.forEach { session ->
        val hour = getHourFromTimestamp(session.startTimestamp)
        val durationMinutes = session.duration / 60000f

        val (fbMinutes, igMinutes) = hourlyData[hour] ?: Pair(0f, 0f)

        hourlyData[hour] = when (session.targetApp) {
            AppTarget.FACEBOOK.packageName -> Pair(fbMinutes + durationMinutes, igMinutes)
            AppTarget.INSTAGRAM.packageName -> Pair(fbMinutes, igMinutes + durationMinutes)
            else -> Pair(fbMinutes, igMinutes)
        }
    }

    return hourlyData.map { (hour, data) ->
        TimeSlotUsage(hour.toFloat(), data.first, data.second)
    }.sortedBy { it.timeSlot }
}

/**
 * Aggregate sessions by day of week
 */
fun aggregateSessionsByDayOfWeek(sessions: List<UsageSession>): List<TimeSlotUsage> {
    val dailyData = mutableMapOf<Int, Pair<Float, Float>>()

    // Initialize all 7 days with 0
    for (day in 1..7) {
        dailyData[day] = Pair(0f, 0f)
    }

    // Aggregate sessions
    sessions.forEach { session ->
        val day = getDayOfWeek(session.startTimestamp)
        val durationMinutes = session.duration / 60000f

        val (fbMinutes, igMinutes) = dailyData[day] ?: Pair(0f, 0f)

        dailyData[day] = when (session.targetApp) {
            AppTarget.FACEBOOK.packageName -> Pair(fbMinutes + durationMinutes, igMinutes)
            AppTarget.INSTAGRAM.packageName -> Pair(fbMinutes, igMinutes + durationMinutes)
            else -> Pair(fbMinutes, igMinutes)
        }
    }

    return dailyData.map { (day, data) ->
        TimeSlotUsage(day.toFloat(), data.first, data.second)
    }.sortedBy { it.timeSlot }
}

/**
 * Aggregate sessions by day of month
 */
fun aggregateSessionsByDayOfMonth(sessions: List<UsageSession>): List<TimeSlotUsage> {
    val dailyData = mutableMapOf<Int, Pair<Float, Float>>()

    // Get the range of days in the sessions
    if (sessions.isEmpty()) return emptyList()

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = sessions.first().startTimestamp
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Initialize all days with 0
    for (day in 1..daysInMonth) {
        dailyData[day] = Pair(0f, 0f)
    }

    // Aggregate sessions
    sessions.forEach { session ->
        val day = getDayOfMonth(session.startTimestamp)
        val durationMinutes = session.duration / 60000f

        val (fbMinutes, igMinutes) = dailyData[day] ?: Pair(0f, 0f)

        dailyData[day] = when (session.targetApp) {
            AppTarget.FACEBOOK.packageName -> Pair(fbMinutes + durationMinutes, igMinutes)
            AppTarget.INSTAGRAM.packageName -> Pair(fbMinutes, igMinutes + durationMinutes)
            else -> Pair(fbMinutes, igMinutes)
        }
    }

    return dailyData.map { (day, data) ->
        TimeSlotUsage(day.toFloat(), data.first, data.second)
    }.sortedBy { it.timeSlot }
}

/**
 * Aggregate sessions by time period
 */
fun aggregateSessionsByTimePeriod(sessions: List<UsageSession>): Map<TimePeriod, Pair<Float, Float>> {
    val periodData = mutableMapOf<TimePeriod, Pair<Float, Float>>()

    // Initialize all periods with 0
    TimePeriod.values().forEach { period ->
        periodData[period] = Pair(0f, 0f)
    }

    // Aggregate sessions
    sessions.forEach { session ->
        val period = getTimePeriod(session.startTimestamp)
        val durationMinutes = session.duration / 60000f

        val (fbMinutes, igMinutes) = periodData[period] ?: Pair(0f, 0f)

        periodData[period] = when (session.targetApp) {
            AppTarget.FACEBOOK.packageName -> Pair(fbMinutes + durationMinutes, igMinutes)
            AppTarget.INSTAGRAM.packageName -> Pair(fbMinutes, igMinutes + durationMinutes)
            else -> Pair(fbMinutes, igMinutes)
        }
    }

    return periodData
}
