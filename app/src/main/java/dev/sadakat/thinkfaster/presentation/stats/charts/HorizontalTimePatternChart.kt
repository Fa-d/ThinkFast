package dev.sadakat.thinkfaster.presentation.stats.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.domain.model.UsageSession

/**
 * Simplified composable that displays top 3 peak usage times with emojis
 * Shows which times of day have the highest usage in a clean, visual format
 */
@Composable
fun HorizontalTimePatternChart(
    sessions: List<UsageSession>,
    modifier: Modifier = Modifier
) {
    val peakTimes = remember(sessions) {
        prepareTimePatternData(sessions)
    }

    // Check if chart has data to display
    val hasData = peakTimes.isNotEmpty() && peakTimes.any { it.totalMinutes > 0f }

    if (!hasData) {
        // Empty state
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
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
                    text = "No time patterns to display yet",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Top 3 peaks with emojis
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            peakTimes.take(3).forEachIndexed { index, peakData ->
                PeakTimeCard(
                    rank = index + 1,
                    period = peakData.period,
                    totalMinutes = peakData.totalMinutes,
                    maxMinutes = peakTimes.firstOrNull()?.totalMinutes ?: 1f
                )
            }
        }
    }
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
 * Card component for displaying a single peak time
 */
@Composable
private fun PeakTimeCard(
    rank: Int,
    period: TimePeriod,
    totalMinutes: Float,
    maxMinutes: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (rank) {
                1 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                2 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge with emoji
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .background(
                        color = when (rank) {
                            1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            2 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = getTimePeriodEmoji(period),
                        fontSize = 20.sp
                    )
                    Text(
                        text = "#$rank",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Period label and usage info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = period.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatMinutes(totalMinutes),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress indicator
            Box(
                modifier = Modifier.width(80.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                LinearProgressIndicator(
                    progress = { totalMinutes / maxMinutes },
                    modifier = Modifier.height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

/**
 * Get emoji for time period
 */
private fun getTimePeriodEmoji(period: TimePeriod): String {
    return when (period) {
        TimePeriod.NIGHT -> "🌙"
        TimePeriod.MORNING -> "🌅"
        TimePeriod.AFTERNOON -> "☀️"
        TimePeriod.EVENING -> "🌆"
    }
}

/**
 * Format minutes for display
 */
private fun formatMinutes(minutes: Float): String {
    return when {
        minutes >= 60 -> {
            val hours = (minutes / 60).toInt()
            val mins = (minutes % 60).toInt()
            if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
        }
        else -> "${minutes.toInt()}m"
    }
}

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
