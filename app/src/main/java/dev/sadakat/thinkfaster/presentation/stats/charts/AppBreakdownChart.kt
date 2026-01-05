package dev.sadakat.thinkfaster.presentation.stats.charts

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing

/**
 * Donut chart showing app usage breakdown
 * Displays Facebook and Instagram usage with visual legend
 */
@Composable
fun AppBreakdownChart(
    appUsageMap: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Filter and prepare data
    val totalMinutes = appUsageMap.values.sum()

    if (totalMinutes == 0) {
        // Empty state
        AppBreakdownEmptyState(modifier = modifier)
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "App Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "How you spent your time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Donut chart
            DonutChartView(
                context = context,
                appUsageMap = appUsageMap,
                totalMinutes = totalMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Legend with breakdown
            AppBreakdownLegend(
                context = context,
                appUsageMap = appUsageMap,
                totalMinutes = totalMinutes
            )
        }
    }
}

/**
 * Donut chart view using MPAndroidChart
 */
@Composable
private fun DonutChartView(
    context: Context,
    appUsageMap: Map<String, Int>,
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    val entries = remember(appUsageMap) {
        appUsageMap.map { (app, minutes) ->
            val label = getApplicationName(context, app)
            PieEntry(minutes.toFloat(), label)
        }
    }

    val colors = remember {
        listOf(
            Color(0xFF1877F2).toArgb(), // Facebook blue
            Color(0xFFE4405F).toArgb()  // Instagram pink/red
        )
    }

    // Get theme-aware text color
    val textColor = MaterialTheme.colorScheme.onSurface

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawEntryLabels(false)
                isRotationEnabled = false
                setTouchEnabled(false)

                // Donut hole configuration
                isDrawHoleEnabled = true
                holeRadius = 60f
                transparentCircleRadius = 65f
                setHoleColor(android.graphics.Color.TRANSPARENT)

                // Center text
                setDrawCenterText(true)
                centerText = "${totalMinutes}m\nTotal"
                setCenterTextSize(18f)
                setCenterTextColor(textColor.toArgb())
            }
        },
        update = { chart ->
            val dataSet = PieDataSet(entries, "").apply {
                this.colors = colors
                sliceSpace = 2f
                selectionShift = 5f
                valueTextSize = 14f
                valueTextColor = android.graphics.Color.WHITE

                // Value formatter to show minutes and percentage
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val percentage = (value / totalMinutes * 100).toInt()
                        return "${value.toInt()}m\n($percentage%)"
                    }
                }
            }

            chart.data = PieData(dataSet)
            chart.invalidate()
        }
    )
}

/**
 * Legend showing app breakdown with icons and percentages
 */
@Composable
private fun AppBreakdownLegend(
    context: Context,
    appUsageMap: Map<String, Int>,
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        appUsageMap.entries.sortedByDescending { it.value }.forEach { (app, minutes) ->
            val appName = getApplicationName(context, app)

            val percentage = if (totalMinutes > 0) {
                (minutes.toFloat() / totalMinutes * 100).toInt()
            } else 0

            val color = when (appName) {
                "Facebook" -> Color(0xFF1877F2)
                "Instagram" -> Color(0xFFE4405F)
                else -> MaterialTheme.colorScheme.primary
            }

            LegendItem(
                appName = appName,
                minutes = minutes,
                percentage = percentage,
                color = color
            )
        }
    }
}

/**
 * Individual legend item
 */
@Composable
private fun LegendItem(
    appName: String,
    minutes: Int,
    percentage: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .then(
                        Modifier.padding(0.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                        drawCircle(color = color)
                    }
                }
            }

            // App name
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Time and percentage
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${minutes}m",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "($percentage%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Empty state when there's no app usage data
 */
@Composable
private fun AppBreakdownEmptyState(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📱",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "No usage data yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = "Start using your tracked apps to see breakdown",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Get application name from package name using PackageManager
 * Returns the actual app name as shown in the launcher
 */
private fun getApplicationName(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        // Fallback to package name if app not found
        packageName
    }
}
