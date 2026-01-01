package dev.sadakat.thinkfaster.presentation.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.sadakat.thinkfaster.MainActivity
import dev.sadakat.thinkfaster.R
import dev.sadakat.thinkfaster.domain.model.ProgressColor

/**
 * Glance (Compose) widget for daily progress tracking
 *
 * Benefits over XML:
 * - Declarative UI
 * - Better Material 3 theming
 * - Easier responsive design with LocalSize
 * - Type-safe
 * - Shared composables with main app
 */
class DailyProgressGlanceWidget : GlanceAppWidget() {

    /**
     * Responsive size modes - widget adapts based on available space
     */
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(80.dp, 80.dp),   // Very small
            DpSize(120.dp, 120.dp), // Small
            DpSize(180.dp, 180.dp), // Medium
            DpSize(280.dp, 180.dp)  // Large
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load widget data
        val data = WidgetData.load(context)

        provideContent {
            GlanceTheme {
                WidgetContent(data)
            }
        }
    }
}

@Composable
private fun WidgetContent(data: WidgetData) {
    val context = LocalContext.current
    val size = LocalSize.current

    // Get theme info
    val isDarkTheme = context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    // Theme-aware background color
    val backgroundColor = if (isDarkTheme) {
        ColorProvider(Color(0xFF1A1A1A)) // Dark theme
    } else {
        ColorProvider(Color(0xFFFFFFFF)) // Light theme
    }

    // Theme-aware text colors
    val primaryTextColor = if (isDarkTheme) {
        ColorProvider(Color(0xFFE0E0E0)) // Light gray for dark mode
    } else {
        ColorProvider(Color(0xFF212121)) // Dark gray for light mode
    }

    val secondaryTextColor = if (isDarkTheme) {
        ColorProvider(Color(0xFFB0B0B0)) // Medium gray for dark mode
    } else {
        ColorProvider(Color(0xFF757575)) // Medium gray for light mode
    }

    val accentColor = if (isDarkTheme) {
        ColorProvider(Color(0xFF64B5F6)) // Light blue for dark mode
    } else {
        ColorProvider(Color(0xFF1976D2)) // Dark blue for light mode
    }

    // Dynamic sizing based on actual widget dimensions
    val minDimension = minOf(size.width.value, size.height.value)
    val maxDimension = maxOf(size.width.value, size.height.value)

    // Calculate scale factor (normalized to 180dp as baseline)
    val scaleFactor = (minDimension / 180f).coerceIn(0.4f, 1.5f)

    // Dynamically scale all elements
    val titleSize = (11f * scaleFactor).sp
    val progressCircleSize = (88f * scaleFactor).dp
    val percentageTextSize = (20f * scaleFactor).sp
    val statusTextSize = (14f * scaleFactor).sp
    val detailTextSize = (10f * scaleFactor).sp
    val detailValueSize = (12f * scaleFactor).sp
    val updateTextSize = (8f * scaleFactor).sp
    val updateValueSize = (10f * scaleFactor).sp

    // Dynamic spacing
    val spacingSmall = (4f * scaleFactor).dp
    val spacingMedium = (6f * scaleFactor).dp
    val spacingLarge = (8f * scaleFactor).dp
    val paddingOuter = (12f * scaleFactor).dp

    // Visibility thresholds based on size
    val showTitle = minDimension >= 100
    val showDetails = minDimension >= 120
    val showUpdateRow = minDimension >= 140
    val showRemainingTime = maxDimension >= 260 && minDimension >= 160

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Content container with padding and click
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(paddingOuter)
                .clickable(
                    actionStartActivity(
                        android.content.Intent(context, MainActivity::class.java)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Title (dynamically shown/hidden)
                if (showTitle) {
                    Text(
                        text = context.getString(R.string.widget_title),
                        style = TextStyle(
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold,
                            color = secondaryTextColor
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(spacingMedium))
                }

                // Progress Circle with Percentage (always shown)
                ProgressCircleWithPercentage(
                    data = data,
                    size = progressCircleSize,
                    percentageSize = percentageTextSize,
                    isDarkTheme = isDarkTheme,
                    textColor = primaryTextColor
                )

                Spacer(modifier = GlanceModifier.height(spacingMedium))

                // Status Message (always shown)
                Text(
                    text = data.getStatusMessage(context),
                    style = TextStyle(
                        fontSize = statusTextSize,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    ),
                    maxLines = 1
                )

                // Usage Details (dynamically shown/hidden)
                if (showDetails) {
                    Spacer(modifier = GlanceModifier.height(spacingLarge))
                    UsageDetails(data, detailTextSize, detailValueSize, spacingSmall, secondaryTextColor, primaryTextColor)
                }

                // Update time + Refresh (dynamically shown/hidden)
                if (showUpdateRow) {
                    Spacer(modifier = GlanceModifier.height(spacingLarge))
                    UpdateTimeRow(data, context, updateTextSize, updateValueSize, spacingSmall, secondaryTextColor, accentColor)
                }

                // Remaining time (dynamically shown/hidden)
                if (showRemainingTime) {
                    Spacer(modifier = GlanceModifier.height(spacingMedium))
                    RemainingTime(data, context, detailTextSize, spacingSmall, secondaryTextColor)
                }
            }
        }
    }
}

@Composable
private fun ProgressCircleWithPercentage(
    data: WidgetData,
    size: androidx.compose.ui.unit.Dp,
    percentageSize: androidx.compose.ui.unit.TextUnit,
    isDarkTheme: Boolean,
    textColor: ColorProvider
) {
    val context = LocalContext.current

    // Determine progress color based on usage
    val progressColor = when (data.getProgressColor()) {
        ProgressColor.GREEN -> Color(0xFF4CAF50)
        ProgressColor.YELLOW -> Color(0xFFFF9800)
        ProgressColor.ORANGE -> Color(0xFFFF5722)
        ProgressColor.RED -> Color(0xFFF44336)
    }

    // Background track color - adapts to theme
    val trackColor = if (isDarkTheme) {
        Color(0xFF2A2A2A) // Dark gray for dark mode
    } else {
        Color(0xFFE0E0E0) // Light gray for light mode
    }

    // Center circle color - matches gradient background
    val centerColor = if (isDarkTheme) {
        Color(0xFF121212) // Darker for dark mode
    } else {
        Color(0xFFF8F8F8) // Lighter for light mode
    }

    // Create progress arc bitmap
    val sizePx = (size.value * context.resources.displayMetrics.density).toInt()
    val strokeWidthPx = sizePx * 0.15f // 15% of size for stroke width
    val progressBitmap = createProgressArcBitmap(
        size = sizePx,
        percentage = data.percentageUsed,
        color = progressColor.toArgb(),
        strokeWidth = strokeWidthPx
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.size(size)
    ) {
        // Background circle track (theme-aware)
        Box(
            modifier = GlanceModifier
                .size(size)
                .background(ColorProvider(trackColor))
                .cornerRadius(size / 2),
            content = {}
        )

        // Inner circle to create ring effect (theme-aware)
        Box(
            modifier = GlanceModifier
                .size(size * 0.7f)
                .background(ColorProvider(centerColor))
                .cornerRadius(size / 2),
            content = {}
        )

        // Progress arc overlay
        Image(
            provider = ImageProvider(progressBitmap),
            contentDescription = "Progress ${data.percentageUsed}%",
            modifier = GlanceModifier.size(size)
        )

        // Percentage text overlay
        Text(
            text = "${data.percentageUsed}%",
            style = TextStyle(
                fontSize = percentageSize,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

/**
 * Creates a bitmap with a circular progress arc
 *
 * @param size The size of the bitmap in pixels
 * @param percentage The progress percentage (0-100)
 * @param color The color of the progress arc
 * @param strokeWidth The width of the arc stroke in pixels
 * @return Bitmap containing the progress arc
 */
private fun createProgressArcBitmap(
    size: Int,
    percentage: Int,
    color: Int,
    strokeWidth: Float
): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.ROUND
    }

    // Create rect for arc (inset by half stroke width to prevent clipping)
    val inset = strokeWidth / 2
    val rect = RectF(
        inset,
        inset,
        size - inset,
        size - inset
    )

    // Draw arc starting from top (-90 degrees) clockwise
    // Clamp percentage between 0 and 100 to prevent overflow
    val clampedPercentage = percentage.coerceIn(0, 100)
    val sweepAngle = (clampedPercentage / 100f) * 360f

    canvas.drawArc(rect, -90f, sweepAngle, false, paint)

    return bitmap
}

@Composable
private fun UsageDetails(
    data: WidgetData,
    labelSize: androidx.compose.ui.unit.TextUnit,
    valueSize: androidx.compose.ui.unit.TextUnit,
    spacing: androidx.compose.ui.unit.Dp,
    labelColor: ColorProvider,
    valueColor: ColorProvider
) {
    val usedText = formatMinutes(data.totalUsedMinutes)
    val goalText = formatMinutes(data.totalGoalMinutes)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Used:",
            style = TextStyle(
                fontSize = labelSize,
                color = labelColor
            )
        )
        Spacer(modifier = GlanceModifier.width(spacing))
        Text(
            text = usedText,
            style = TextStyle(
                fontSize = valueSize,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        )
        Spacer(modifier = GlanceModifier.width(spacing))
        Text(
            text = "/",
            style = TextStyle(
                fontSize = valueSize,
                color = labelColor
            )
        )
        Spacer(modifier = GlanceModifier.width(spacing))
        Text(
            text = "Goal:",
            style = TextStyle(
                fontSize = labelSize,
                color = labelColor
            )
        )
        Spacer(modifier = GlanceModifier.width(spacing))
        Text(
            text = goalText,
            style = TextStyle(
                fontSize = valueSize,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        )
    }
}

@Composable
private fun UpdateTimeRow(
    data: WidgetData,
    context: Context,
    labelSize: androidx.compose.ui.unit.TextUnit,
    valueSize: androidx.compose.ui.unit.TextUnit,
    spacing: androidx.compose.ui.unit.Dp,
    textColor: ColorProvider,
    accentColor: ColorProvider
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = "Updated:",
                style = TextStyle(
                    fontSize = labelSize,
                    color = textColor
                )
            )
            Text(
                text = data.formatLastUpdate(context),
                style = TextStyle(
                    fontSize = valueSize,
                    color = textColor
                )
            )
        }

        // Refresh button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .clickable(onClick = actionRunCallback<RefreshWidgetAction>())
                .padding(spacing * 1.5f)
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Refresh",
                modifier = GlanceModifier.size(valueSize.value.dp * 1.6f)
            )
            Spacer(modifier = GlanceModifier.width(spacing))
            Text(
                text = "Refresh",
                style = TextStyle(
                    fontSize = valueSize,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            )
        }
    }
}

@Composable
private fun RemainingTime(
    data: WidgetData,
    context: Context,
    textSize: androidx.compose.ui.unit.TextUnit,
    spacing: androidx.compose.ui.unit.Dp,
    textColor: ColorProvider
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Remaining:",
            style = TextStyle(
                fontSize = textSize,
                color = textColor
            )
        )
        Spacer(modifier = GlanceModifier.width(spacing))
        Text(
            text = data.formatRemainingTime(context),
            style = TextStyle(
                fontSize = textSize,
                color = textColor
            )
        )
    }
}

/**
 * Helper function to format minutes for display
 */
private fun formatMinutes(minutes: Int): String {
    return when {
        minutes >= 60 -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
        }
        else -> "${minutes}m"
    }
}

