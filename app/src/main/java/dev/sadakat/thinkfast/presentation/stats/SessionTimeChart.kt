package dev.sadakat.thinkfast.presentation.stats

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import dev.sadakat.thinkfast.R
import dev.sadakat.thinkfast.domain.model.UsageSession
import dev.sadakat.thinkfast.presentation.stats.charts.ChartColors
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Time period enum for chart formatting
 */
enum class ChartPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

/**
 * Data class representing a session data point with normalized position
 */
data class SessionDataPoint(
    val timestamp: Long,
    val durationMinutes: Float,
    val appName: String
)

/**
 * Group sessions by app and sort by timestamp
 */
data class GroupedSessions(
    val appSessions: Map<String, List<SessionDataPoint>>,  // Map of packageName to sessions
    val minTimestamp: Long,
    val maxTimestamp: Long
)

/**
 * Group and prepare session data for charting
 */
fun prepareSessionData(sessions: List<UsageSession>): GroupedSessions {
    // Group sessions by app
    val appSessions = sessions
        .filter { it.duration > 0 }
        .groupBy { it.targetApp }
        .mapValues { (packageName, sessions) ->
            sessions
                .sortedBy { it.startTimestamp }
                .map { session ->
                    SessionDataPoint(
                        timestamp = session.startTimestamp,
                        durationMinutes = session.duration / 60000f, // Convert to minutes
                        appName = packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
                    )
                }
        }

    val allTimestamps = sessions.map { it.startTimestamp }
    val minTimestamp = allTimestamps.minOrNull() ?: 0L
    val maxTimestamp = allTimestamps.maxOrNull() ?: 0L

    return GroupedSessions(
        appSessions = appSessions,
        minTimestamp = minTimestamp,
        maxTimestamp = maxTimestamp
    )
}

/**
 * Composable that displays a line chart showing session duration over time
 * for all tracked apps with curved lines
 */
@Composable
fun SessionDurationTimeChart(
    sessions: List<UsageSession>,
    period: ChartPeriod,
    modifier: Modifier = Modifier
) {
    val groupedData = remember(sessions) { prepareSessionData(sessions) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        factory = { context ->
            LineChart(context).apply {
                setupChart(this, period)
            }
        },
        update = { chart ->
            updateChartData(chart, groupedData, period)
        }
    )
}

/**
 * Setup the chart appearance and behavior
 */
private fun setupChart(chart: LineChart, period: ChartPeriod) {
    chart.apply {
        // Basic settings
        val description = Description()
        description.text = "Session Duration Over Time"
        description.textSize = 12f
        this.description = description

        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(true)
        setPinchZoom(true)
        setDrawGridBackground(false)
        setDrawBorders(false)

        // X Axis setup (Time) - format based on period
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(true)
            setDrawAxisLine(true)
            granularity = 1f
            textSize = 11f
            labelCount = when (period) {
                ChartPeriod.DAILY -> 8  // Show every 3 hours
                ChartPeriod.WEEKLY -> 7  // Show each day
                ChartPeriod.MONTHLY -> 10 // Show ~3 day intervals
            }
            valueFormatter = TimeAxisValueFormatter(period)
        }

        // Right Y Axis - disable
        axisRight.isEnabled = false

        // Left Y Axis (Duration in minutes)
        axisLeft.apply {
            setDrawGridLines(true)
            setDrawAxisLine(true)
            axisMinimum = 0f
            textSize = 12f
            granularity = 1f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}m"
                }
            }
        }

        // Legend setup
        legend.apply {
            isEnabled = true
            textSize = 12f
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
            xEntrySpace = 10f
            yEntrySpace = 5f
        }

        // Extra offsets
        setExtraOffsets(10f, 10f, 10f, 20f)
    }
}

/**
 * Update the chart with new data
 */
private fun updateChartData(
    chart: LineChart,
    groupedData: GroupedSessions,
    period: ChartPeriod
) {
    // Normalize timestamps to position values based on period
    val (xMin, xMax) = when (period) {
        ChartPeriod.DAILY -> 0f to 24f  // Hours
        ChartPeriod.WEEKLY -> 1f to 7f  // Days of week
        ChartPeriod.MONTHLY -> 1f to 31f  // Days of month
    }

    // Convert timestamps to x-axis positions
    fun timestampToXPosition(timestamp: Long): Float {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        return when (period) {
            ChartPeriod.DAILY -> {
                // Hour of day (0-24)
                calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60f
            }
            ChartPeriod.WEEKLY -> {
                // Day of week (1-7, where 1=Monday)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                when (dayOfWeek) {
                    Calendar.MONDAY -> 1f
                    Calendar.TUESDAY -> 2f
                    Calendar.WEDNESDAY -> 3f
                    Calendar.THURSDAY -> 4f
                    Calendar.FRIDAY -> 5f
                    Calendar.SATURDAY -> 6f
                    Calendar.SUNDAY -> 7f
                    else -> 1f
                }
            }
            ChartPeriod.MONTHLY -> {
                // Day of month (1-31)
                calendar.get(Calendar.DAY_OF_MONTH) + calendar.get(Calendar.HOUR_OF_DAY) / 24f
            }
        }
    }

    // Create datasets for each app dynamically
    val dataSets = groupedData.appSessions.entries.mapIndexed { index, (packageName, sessions) ->
        val entries = sessions.map { point ->
            Entry(timestampToXPosition(point.timestamp), point.durationMinutes)
        }

        val appColor = ChartColors.getColorForApp(index)
        val appName = packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName

        LineDataSet(entries, appName).apply {
            color = appColor
            setCircleColor(appColor)
            lineWidth = 3f
            circleRadius = 5f
            setCircleHoleColor(appColor)
            circleHoleRadius = 2f
            setDrawCircleHole(true)
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER  // Curved lines
            cubicIntensity = 0.5f  // Increased for more dramatic curves
            valueTextSize = 10f
            setDrawValues(false)
        }
    }

    // Create line data with all datasets
    val lineData = LineData(dataSets)
    chart.data = lineData

    // Set x-axis range based on period
    chart.xAxis.axisMinimum = xMin
    chart.xAxis.axisMaximum = xMax

    // Calculate max duration for y-axis with some padding
    val allDurations = groupedData.appSessions.values.flatMap { sessions ->
        sessions.map { it.durationMinutes }
    }
    if (allDurations.isNotEmpty()) {
        val maxDuration = allDurations.maxOrNull() ?: 0f
        chart.axisLeft.axisMaximum = if (maxDuration > 0) maxDuration * 1.2f else 10f
    }

    // Refresh chart
    chart.notifyDataSetChanged()
    chart.invalidate()
}

/**
 * Custom ValueFormatter for time axis based on period
 */
class TimeAxisValueFormatter(private val period: ChartPeriod) : ValueFormatter() {
    private val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    override fun getFormattedValue(value: Float): String {
        return when (period) {
            ChartPeriod.DAILY -> {
                // Format as HH:00
                val hour = value.toInt()
                String.format("%02d:00", hour.coerceIn(0, 23))
            }
            ChartPeriod.WEEKLY -> {
                // Format as day name (Mon, Tue, etc.)
                val dayIndex = value.toInt().coerceIn(1, 7)
                val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                days[dayIndex - 1]
            }
            ChartPeriod.MONTHLY -> {
                // Format as day number (1, 2, 3, etc.)
                val day = value.toInt().coerceIn(1, 31)
                day.toString()
            }
        }
    }
}

/**
 * Custom MarkerView for displaying session details when tapping on data points
 *
 * Note: Currently commented out as it's not being used in setupChart.
 * To implement properly, would need to pass chart reference to access datasets
 * via highlight.dataSetIndex since Highlight doesn't expose dataSet directly.
 */
/*
class SessionChartMarkerView(
    context: Context,
    layoutResource: Int,
    private val period: ChartPeriod
) : MarkerView(context, layoutResource) {

    private val tvApp: TextView = findViewById(R.id.tv_marker_app)
    private val tvDuration: TextView = findViewById(R.id.tv_marker_duration)
    private val tvTime: TextView = findViewById(R.id.tv_marker_time)

    override fun refreshContent(e: Entry, highlight: Highlight) {
        // Would need chart reference to get dataset:
        // val dataset = chart.data.getDataSetByIndex(highlight.dataSetIndex) as? LineDataSet
        // val appName = dataset?.label ?: "App"

        tvApp.text = "App" // placeholder
        tvDuration.text = "Duration: ${e.y.toInt()} min"

        // Format time based on period
        val timeLabel = when (period) {
            ChartPeriod.DAILY -> {
                val hour = e.x.toInt()
                String.format("%02d:00", hour.coerceIn(0, 23))
            }
            ChartPeriod.WEEKLY -> {
                val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val dayIndex = e.x.toInt().coerceIn(1, 7)
                days[dayIndex - 1]
            }
            ChartPeriod.MONTHLY -> {
                "Day ${e.x.toInt()}"
            }
        }
        tvTime.text = timeLabel

        super.refreshContent(e, highlight)
    }
}
*/
