package dev.sadakat.thinkfast.presentation.stats.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dev.sadakat.thinkfast.domain.model.UsageSession
import dev.sadakat.thinkfast.presentation.stats.ChartPeriod

/**
 * Composable that displays a stacked bar chart showing usage breakdown over time
 * Displays Facebook and Instagram usage stacked in each bar
 */
@Composable
fun StackedBarUsageChart(
    sessions: List<UsageSession>,
    period: ChartPeriod,
    modifier: Modifier = Modifier
) {
    val chartData = remember(sessions, period) {
        prepareStackedBarData(sessions, period)
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        factory = { context ->
            BarChart(context).apply {
                setupBarChart(this, period)
            }
        },
        update = { chart ->
            updateBarChartData(chart, chartData, period)
        }
    )
}

/**
 * Prepare stacked bar data based on the period
 */
private fun prepareStackedBarData(
    sessions: List<UsageSession>,
    period: ChartPeriod
): List<TimeSlotUsage> {
    return when (period) {
        ChartPeriod.DAILY -> aggregateSessionsByHour(sessions)
        ChartPeriod.WEEKLY -> aggregateSessionsByDayOfWeek(sessions)
        ChartPeriod.MONTHLY -> aggregateSessionsByDayOfMonth(sessions)
    }
}

/**
 * Setup the bar chart appearance and behavior
 */
private fun setupBarChart(chart: BarChart, period: ChartPeriod) {
    chart.apply {
        // Basic settings
        description.isEnabled = false
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(period == ChartPeriod.MONTHLY) // Enable zoom for monthly view
        setPinchZoom(period == ChartPeriod.MONTHLY)
        setDrawGridBackground(false)
        setDrawBarShadow(false)
        isHighlightFullBarEnabled = true

        // Bar settings
        setDrawValueAboveBar(false)
        setFitBars(true)

        // X Axis setup
        xAxis.apply {
            applyCommonStyling()
            valueFormatter = when (period) {
                ChartPeriod.DAILY -> HourValueFormatter()
                ChartPeriod.WEEKLY -> DayOfWeekValueFormatter()
                ChartPeriod.MONTHLY -> DayOfMonthValueFormatter()
            }
            labelCount = when (period) {
                ChartPeriod.DAILY -> 8  // Show every 3 hours
                ChartPeriod.WEEKLY -> 7  // Show all days
                ChartPeriod.MONTHLY -> 10  // Show ~10 labels
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
            form = Legend.LegendForm.SQUARE
            formSize = 10f
            textSize = 12f
            xEntrySpace = 15f
        }
    }
}

/**
 * Update the bar chart with new data
 */
private fun updateBarChartData(
    chart: BarChart,
    data: List<TimeSlotUsage>,
    period: ChartPeriod
) {
    if (data.isEmpty() || data.all { it.totalMinutes == 0f }) {
        chart.clear()
        chart.invalidate()
        return
    }

    // Create bar entries with stacked values [Facebook, Instagram]
    val barEntries = data.map { usage ->
        BarEntry(
            usage.timeSlot,
            floatArrayOf(usage.facebookMinutes, usage.instagramMinutes)
        )
    }

    // Create dataset
    val dataSet = BarDataSet(barEntries, "").apply {
        // Set colors for stacked bars
        colors = listOf(ChartColors.FACEBOOK_BLUE, ChartColors.INSTAGRAM_PINK)

        // Labels for legend
        stackLabels = arrayOf("Facebook", "Instagram")

        // Visual settings
        setDrawValues(true)
        valueTextSize = 9f
        valueFormatter = MinutesValueFormatter()

        // Highlight settings
        highLightAlpha = 100
    }

    // Create bar data
    val barData = BarData(dataSet).apply {
        // Bar width adjustment based on period
        barWidth = when (period) {
            ChartPeriod.DAILY -> 0.7f
            ChartPeriod.WEEKLY -> 0.8f
            ChartPeriod.MONTHLY -> 0.85f
        }
    }

    chart.data = barData

    // Set visible range for monthly view (scrollable)
    if (period == ChartPeriod.MONTHLY) {
        chart.setVisibleXRangeMaximum(10f)
        chart.moveViewToX(data.size.toFloat())
    }

    // Refresh chart
    chart.notifyDataSetChanged()
    chart.invalidate()
}
