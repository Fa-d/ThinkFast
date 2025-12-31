package dev.sadakat.thinkfaster.presentation.stats.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dev.sadakat.thinkfaster.domain.model.UsageSession
import dev.sadakat.thinkfaster.presentation.stats.ChartPeriod

/**
 * Composable that displays a stacked bar chart showing usage breakdown over time
 * Displays Facebook and Instagram usage stacked in each bar
 * Phase 1.3: Added empty state handling
 * Phase 4.1: Added responsive chart heights
 */
@Composable
fun StackedBarUsageChart(
    sessions: List<UsageSession>,
    period: ChartPeriod,
    modifier: Modifier = Modifier
) {
    // Phase 4.1: Responsive chart height based on screen size
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val chartHeight = when {
        screenHeight < 600.dp -> 240.dp  // Small screens: reduce by 20%
        screenHeight > 800.dp -> 330.dp  // Large screens: increase by 10%
        else -> 300.dp  // Medium screens: default
    }

    // Get theme-aware colors
    val textColor = MaterialTheme.colorScheme.onSurface.hashCode()
    val gridColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f).hashCode()

    val chartData = remember(sessions, period) {
        prepareStackedBarData(sessions, period)
    }

    // Check if chart has data to display
    val hasData = chartData.isNotEmpty() && chartData.any { it.totalMinutes > 0f }

    if (!hasData) {
        // Empty state
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(chartHeight),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📊",
                    fontSize = 36.sp
                )
                Text(
                    text = "No usage data for this period",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        // Chart with data
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(chartHeight),
            factory = { context ->
                BarChart(context).apply {
                    setupBarChart(this, period, textColor, gridColor)
                }
            },
            update = { chart ->
                updateBarChartData(chart, chartData, period, textColor)
            }
        )
    }
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
private fun setupBarChart(chart: BarChart, period: ChartPeriod, textColor: Int, gridColor: Int) {
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
            applyCommonStyling(textColor)
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
            applyCommonStyling(textColor, gridColor)
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
            this.textColor = textColor
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
    period: ChartPeriod,
    textColor: Int
) {
    if (data.isEmpty() || data.all { it.totalMinutes == 0f }) {
        chart.clear()
        chart.invalidate()
        return
    }

    // Get all unique apps across all time slots (sorted for consistency)
    val allApps = data.flatMap { it.appUsage.keys }.distinct().sorted()

    if (allApps.isEmpty()) {
        chart.clear()
        chart.invalidate()
        return
    }

    // Create bar entries with stacked values for each app
    val barEntries = data.map { usage ->
        // Build stacked values array in consistent app order
        val stackedValues = allApps.map { app ->
            usage.appUsage[app] ?: 0f
        }.toFloatArray()

        BarEntry(usage.timeSlot, stackedValues)
    }

    // Create dataset
    val dataSet = BarDataSet(barEntries, "").apply {
        // Set colors for stacked bars (one color per app)
        colors = allApps.mapIndexed { index, _ ->
            ChartColors.getColorForApp(index)
        }

        // Labels for legend (extract simple app names from package names)
        stackLabels = allApps.map { packageName ->
            packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
        }.toTypedArray()

        // Visual settings
        setDrawValues(true)
        valueTextSize = 9f
        valueTextColor = textColor
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
