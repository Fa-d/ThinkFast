package dev.sadakat.thinkfaster.presentation.stats.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dev.sadakat.thinkfaster.domain.model.UsageSession
import dev.sadakat.thinkfaster.presentation.stats.ChartPeriod

/**
 * Composable that displays a goal progress chart
 * Shows daily usage line vs goal limit line
 */
@Composable
fun GoalProgressChart(
    sessions: List<UsageSession>,
    period: ChartPeriod,
    dailyGoalMinutes: Int?,
    modifier: Modifier = Modifier
) {
    // If no goal is set, show prompt
    if (dailyGoalMinutes == null || dailyGoalMinutes == 0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Set a daily goal in Settings to track your progress",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val chartData = remember(sessions, period, dailyGoalMinutes) {
        prepareGoalProgressData(sessions, period, dailyGoalMinutes)
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        factory = { context ->
            LineChart(context).apply {
                setupGoalChart(this, period)
            }
        },
        update = { chart ->
            updateGoalChartData(chart, chartData, period, dailyGoalMinutes)
        }
    )
}

/**
 * Data class for goal progress display
 */
private data class GoalProgressData(
    val usageEntries: List<Entry>,
    val isOverGoal: Boolean
)

/**
 * Prepare goal progress data based on the period
 */
private fun prepareGoalProgressData(
    sessions: List<UsageSession>,
    period: ChartPeriod,
    dailyGoalMinutes: Int
): GoalProgressData {
    val aggregatedData = when (period) {
        ChartPeriod.DAILY -> {
            // For daily view: cumulative usage by hour
            val hourlyData = aggregateSessionsByHour(sessions)
            var cumulative = 0f
            hourlyData.map { usage ->
                cumulative += usage.totalMinutes
                Entry(usage.timeSlot, cumulative)
            }
        }
        ChartPeriod.WEEKLY -> {
            // For weekly view: daily totals
            aggregateSessionsByDayOfWeek(sessions).map { usage ->
                Entry(usage.timeSlot, usage.totalMinutes)
            }
        }
        ChartPeriod.MONTHLY -> {
            // For monthly view: daily totals
            aggregateSessionsByDayOfMonth(sessions).map { usage ->
                Entry(usage.timeSlot, usage.totalMinutes)
            }
        }
    }

    // Check if any usage exceeds goal
    val isOverGoal = aggregatedData.any { it.y > dailyGoalMinutes }

    return GoalProgressData(
        usageEntries = aggregatedData,
        isOverGoal = isOverGoal
    )
}

/**
 * Setup the goal chart appearance and behavior
 */
private fun setupGoalChart(chart: LineChart, period: ChartPeriod) {
    chart.apply {
        // Basic settings
        description.isEnabled = false
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(period == ChartPeriod.MONTHLY)
        setPinchZoom(period == ChartPeriod.MONTHLY)
        setDrawGridBackground(false)

        // X Axis setup
        xAxis.apply {
            applyCommonStyling()
            valueFormatter = when (period) {
                ChartPeriod.DAILY -> HourValueFormatter()
                ChartPeriod.WEEKLY -> DayOfWeekValueFormatter()
                ChartPeriod.MONTHLY -> DayOfMonthValueFormatter()
            }
            labelCount = when (period) {
                ChartPeriod.DAILY -> 8
                ChartPeriod.WEEKLY -> 7
                ChartPeriod.MONTHLY -> 10
            }
        }

        // Right Y Axis - disable
        axisRight.isEnabled = false

        // Left Y Axis
        axisLeft.apply {
            applyCommonStyling()
            valueFormatter = MinutesValueFormatter()
        }

        // Legend
        legend.apply {
            isEnabled = true
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
            form = Legend.LegendForm.LINE
            formSize = 10f
            textSize = 12f
            xEntrySpace = 15f
        }
    }
}

/**
 * Update the goal chart with new data
 */
private fun updateGoalChartData(
    chart: LineChart,
    data: GoalProgressData,
    period: ChartPeriod,
    dailyGoalMinutes: Int
) {
    if (data.usageEntries.isEmpty()) {
        chart.clear()
        chart.invalidate()
        return
    }

    // Create usage line dataset
    val usageDataSet = LineDataSet(data.usageEntries, "Your Usage").apply {
        color = if (data.isOverGoal) ChartColors.OVER_GOAL_RED else ChartColors.GOAL_GREEN
        lineWidth = 3f
        setCircleColor(if (data.isOverGoal) ChartColors.OVER_GOAL_RED else ChartColors.GOAL_GREEN)
        circleRadius = 4f
        setDrawCircleHole(false)
        setDrawFilled(true)
        fillColor = if (data.isOverGoal) ChartColors.OVER_GOAL_RED else ChartColors.GOAL_GREEN
        fillAlpha = 50
        mode = LineDataSet.Mode.CUBIC_BEZIER
        cubicIntensity = 0.2f
        setDrawValues(false)
    }

    // Create line data
    val lineData = LineData(usageDataSet)
    chart.data = lineData

    // Add goal limit line
    chart.axisLeft.removeAllLimitLines()
    val goalLine = LimitLine(dailyGoalMinutes.toFloat(), "Daily Goal: ${dailyGoalMinutes}m")
    goalLine.lineColor = ChartColors.GOAL_GREEN
    goalLine.lineWidth = 2f
    goalLine.textColor = ChartColors.GOAL_GREEN
    goalLine.textSize = 11f
    goalLine.enableDashedLine(10f, 5f, 0f)
    chart.axisLeft.addLimitLine(goalLine)

    // Set visible range for monthly view
    if (period == ChartPeriod.MONTHLY) {
        chart.setVisibleXRangeMaximum(10f)
        chart.moveViewToX(data.usageEntries.size.toFloat())
    }

    // Refresh chart
    chart.notifyDataSetChanged()
    chart.invalidate()
}
