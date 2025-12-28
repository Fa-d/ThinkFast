package dev.sadakat.thinkfast.presentation.stats.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import dev.sadakat.thinkfast.domain.model.UsageSession

/**
 * Composable that displays a horizontal bar chart showing usage patterns by time period
 * Shows which times of day have the highest usage
 */
@Composable
fun HorizontalTimePatternChart(
    sessions: List<UsageSession>,
    modifier: Modifier = Modifier
) {
    val chartData = remember(sessions) {
        prepareTimePatternData(sessions)
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp),
        factory = { context ->
            HorizontalBarChart(context).apply {
                setupHorizontalBarChart(this)
            }
        },
        update = { chart ->
            updateHorizontalBarChartData(chart, chartData)
        }
    )
}

/**
 * Data class for time pattern display
 */
private data class TimePeriodData(
    val period: TimePeriod,
    val appUsage: Map<String, Float>,
    val totalMinutes: Float
)

/**
 * Prepare time pattern data aggregated by time period
 */
private fun prepareTimePatternData(sessions: List<UsageSession>): List<TimePeriodData> {
    val periodMap = aggregateSessionsByTimePeriod(sessions)

    return periodMap.map { (period, appUsage) ->
        TimePeriodData(
            period = period,
            appUsage = appUsage,
            totalMinutes = appUsage.values.sum()
        )
    }.sortedByDescending { it.totalMinutes } // Sort by total usage (highest first)
}

/**
 * Value formatter for time period labels
 */
private class TimePeriodValueFormatter(
    private val periods: List<TimePeriod>
) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val index = value.toInt()
        return if (index in periods.indices) {
            periods[index].label
        } else ""
    }
}

/**
 * Setup the horizontal bar chart appearance and behavior
 */
private fun setupHorizontalBarChart(chart: HorizontalBarChart) {
    chart.apply {
        // Basic settings
        description.isEnabled = false
        setTouchEnabled(true)
        isDragEnabled = false
        setScaleEnabled(false)
        setPinchZoom(false)
        setDrawGridBackground(false)
        setDrawBarShadow(false)
        isHighlightFullBarEnabled = true

        // Bar settings
        setDrawValueAboveBar(true)
        setFitBars(true)

        // X Axis (values - horizontal)
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(true)
            setDrawAxisLine(true)
            axisMinimum = 0f
            textSize = 10f
            gridColor = ChartColors.GRID_LIGHT_GRAY
            valueFormatter = MinutesValueFormatter()
        }

        // Y Axis (categories - vertical)
        axisLeft.apply {
            setDrawGridLines(false)
            setDrawAxisLine(true)
            textSize = 10f
            granularity = 1f
        }

        // Right Y Axis - disable
        axisRight.isEnabled = false

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
 * Update the horizontal bar chart with new data
 */
private fun updateHorizontalBarChartData(
    chart: HorizontalBarChart,
    data: List<TimePeriodData>
) {
    if (data.isEmpty() || data.all { it.totalMinutes == 0f }) {
        chart.clear()
        chart.invalidate()
        return
    }

    // Get all unique apps across all time periods (sorted for consistency)
    val allApps = data.flatMap { it.appUsage.keys }.distinct().sorted()

    if (allApps.isEmpty()) {
        chart.clear()
        chart.invalidate()
        return
    }

    // Create bar entries with stacked values for each app
    // Use index as x-value since we'll use a custom formatter for labels
    val barEntries = data.mapIndexed { index, periodData ->
        // Build stacked values array in consistent app order
        val stackedValues = allApps.map { app ->
            periodData.appUsage[app] ?: 0f
        }.toFloatArray()

        BarEntry(index.toFloat(), stackedValues)
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
        valueTextSize = 10f
        valueFormatter = MinutesValueFormatter()

        // Highlight settings
        highLightAlpha = 100
    }

    // Create bar data
    val barData = BarData(dataSet).apply {
        barWidth = 0.8f
    }

    chart.data = barData

    // Set Y-axis labels to time period names
    chart.axisLeft.valueFormatter = TimePeriodValueFormatter(data.map { it.period })
    chart.axisLeft.setLabelCount(data.size, true)

    // Refresh chart
    chart.notifyDataSetChanged()
    chart.invalidate()
}
