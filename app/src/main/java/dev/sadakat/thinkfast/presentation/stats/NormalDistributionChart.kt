package dev.sadakat.thinkfast.presentation.stats

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import dev.sadakat.thinkfast.domain.model.UsageSession
import kotlin.math.ln
import kotlin.math.sqrt
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * Data class representing a point on the normal distribution curve
 */
data class DistributionPoint(
    val durationMinutes: Double,
    val frequency: Double
)

/**
 * Calculate normal distribution statistics from session data
 */
data class NormalDistributionStats(
    val mean: Double,
    val standardDeviation: Double,
    val distributionPoints: List<DistributionPoint>,
    val sessionCount: Int,
    val minDuration: Double,
    val maxDuration: Double
)

/**
 * Calculate normal distribution from session durations
 */
fun calculateNormalDistribution(sessions: List<UsageSession>): NormalDistributionStats {
    if (sessions.isEmpty()) {
        return NormalDistributionStats(0.0, 0.0, emptyList(), 0, 0.0, 0.0)
    }

    // Convert durations to minutes
    val durationsInMinutes = sessions.map { it.duration / 60000.0 }.filter { it > 0 }

    if (durationsInMinutes.isEmpty()) {
        return NormalDistributionStats(0.0, 0.0, emptyList(), 0, 0.0, 0.0)
    }

    val count = durationsInMinutes.size
    val mean = durationsInMinutes.average()
    val variance = durationsInMinutes.map { (it - mean) * (it - mean) }.average()
    val stdDev = sqrt(variance)

    val minDuration = durationsInMinutes.minOrNull() ?: 0.0
    val maxDuration = durationsInMinutes.maxOrNull() ?: 0.0

    // Generate points for the normal distribution curve
    // Use a range from mean - 4*stdDev to mean + 4*stdDev (covers 99.99% of data)
    val rangeStart = maxOf(0.0, mean - 4 * stdDev)
    val rangeEnd = mean + 4 * stdDev
    val step = (rangeEnd - rangeStart) / 100

    val distributionPoints = mutableListOf<DistributionPoint>()
    var x = rangeStart
    while (x <= rangeEnd) {
        val probability = normalDistributionProbability(x, mean, stdDev)
        distributionPoints.add(DistributionPoint(x, probability))
        x += step
    }

    return NormalDistributionStats(
        mean = mean,
        standardDeviation = stdDev,
        distributionPoints = distributionPoints,
        sessionCount = count,
        minDuration = minDuration,
        maxDuration = maxDuration
    )
}

/**
 * Calculate probability density function for normal distribution
 */
private fun normalDistributionProbability(x: Double, mean: Double, stdDev: Double): Double {
    if (stdDev == 0.0) return 0.0
    val exponent = -((x - mean) * (x - mean)) / (2 * stdDev * stdDev)
    val coefficient = 1.0 / (stdDev * sqrt(2 * Math.PI))
    return coefficient * kotlin.math.exp(exponent)
}

/**
 * Composable that displays a normal distribution curve of session durations using MPAndroidChart
 */
@Composable
fun NormalDistributionChart(
    sessions: List<UsageSession>,
    modifier: Modifier = Modifier,
    lineColor: ComposeColor = ComposeColor(0xFF6366F1),
    fillColor: ComposeColor = ComposeColor(0x406366F1)
) {
    val stats = remember(sessions) { calculateNormalDistribution(sessions) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp),
        factory = { context ->
            LineChart(context).apply {
                setupChart(this)
            }
        },
        update = { chart ->
            updateChartData(chart, stats, lineColor, fillColor)
        }
    )
}

/**
 * Setup the chart appearance and behavior
 */
private fun setupChart(chart: LineChart) {
    chart.apply {
        // Basic settings
        description.isEnabled = false
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(true)
        setPinchZoom(true)
        setDrawGridBackground(false)

        // X Axis setup
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(true)
            granularity = 1f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}m"
                }
            }
        }

        // Right Y Axis
        axisRight.isEnabled = false

        // Left Y Axis
        axisLeft.apply {
            setDrawGridLines(true)
            setDrawAxisLine(true)
            axisMinimum = 0f
        }

        // Legend
        legend.isEnabled = true
        legend.textSize = 12f
    }
}

/**
 * Update the chart with new data
 */
private fun updateChartData(
    chart: LineChart,
    stats: NormalDistributionStats,
    lineColor: ComposeColor,
    fillColor: ComposeColor
) {
    if (stats.sessionCount == 0) {
        chart.clear()
        chart.invalidate()
        return
    }

    // Create entries for the normal distribution curve
    val entries = stats.distributionPoints.map { point ->
        Entry(point.durationMinutes.toFloat(), point.frequency.toFloat())
    }

    // Create dataset
    val dataSet = LineDataSet(entries, "Session Duration Distribution").apply {
        color = lineColor.toArgb()
        setCircleColor(lineColor.toArgb())
        lineWidth = 2.5f
        circleRadius = 4f
        setDrawCircleHole(false)
        setDrawCircles(false)
        setDrawFilled(true)
        fillDrawable = null
        mode = LineDataSet.Mode.CUBIC_BEZIER
        cubicIntensity = 0.1f
    }

    // Create line data
    val lineData = LineData(dataSet)
    lineData.setDrawValues(false)

    chart.data = lineData

    // Add mean line
    chart.axisLeft.removeAllLimitLines()
    val meanLine = LimitLine(stats.mean.toFloat(), "Mean: %.1fm".format(stats.mean))
    meanLine.lineColor = android.graphics.Color.RED
    meanLine.lineWidth = 1.5f
    meanLine.setTextColor(android.graphics.Color.RED)
    meanLine.textSize = 12f
    meanLine.enableDashedLine(5f, 5f, 0f)
    chart.axisLeft.addLimitLine(meanLine)

    // Add standard deviation markers
    val stdDev1 = LimitLine((stats.mean + stats.standardDeviation).toFloat(), "+1 SD")
    stdDev1.lineColor = android.graphics.Color.parseColor("#FF9800")
    stdDev1.lineWidth = 1f
    stdDev1.setTextColor(android.graphics.Color.parseColor("#FF9800"))
    stdDev1.textSize = 10f
    stdDev1.enableDashedLine(3f, 3f, 0f)
    chart.axisLeft.addLimitLine(stdDev1)

    val stdDevMinus1 = LimitLine((stats.mean - stats.standardDeviation).toFloat(), "-1 SD")
    stdDevMinus1.lineColor = android.graphics.Color.parseColor("#FF9800")
    stdDevMinus1.lineWidth = 1f
    stdDevMinus1.setTextColor(android.graphics.Color.parseColor("#FF9800"))
    stdDevMinus1.textSize = 10f
    stdDevMinus1.enableDashedLine(3f, 3f, 0f)
    chart.axisLeft.addLimitLine(stdDevMinus1)

    // Set visible range
    chart.xAxis.axisMinimum = 0f
    chart.xAxis.axisMaximum = ((stats.mean + 4 * stats.standardDeviation).toFloat().coerceAtLeast(
        stats.maxDuration.toFloat() * 1.2f
    ))

    // Refresh chart
    chart.notifyDataSetChanged()
    chart.invalidate()
}
