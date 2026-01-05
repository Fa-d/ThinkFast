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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.domain.model.UsageSession
import java.util.Calendar

/**
 * Simplified timeline view showing goal progress over time
 * Displays a scrollable timeline of daily goal compliance
 */
@Composable
fun GoalProgressTimeline(
    sessions: List<UsageSession>,
    dailyGoalMinutes: Int?,
    modifier: Modifier = Modifier
) {
    // If no goal is set, show prompt
    if (dailyGoalMinutes == null || dailyGoalMinutes == 0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
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

    val timelineData = remember(sessions, dailyGoalMinutes) {
        prepareTimelineData(sessions, dailyGoalMinutes)
    }

    if (timelineData.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎯",
                    fontSize = 36.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start tracking to see your goal timeline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Show last 14 days in a scrollable timeline
    val recentData = timelineData.take(14).reversed()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Recent Progress",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Timeline row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recentData.forEach { dayData ->
                DayProgressIndicator(
                    dayData = dayData,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            LegendItem(color = 0xFF4CAF50, label = "Under")
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(color = 0xFFFFC107, label = "Close")
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(color = 0xFFFF5252, label = "Over")
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(color = 0xFFBDBDBD, label = "No data")
        }
    }
}

/**
 * Individual day progress indicator
 */
@Composable
private fun DayProgressIndicator(
    dayData: DayProgressData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Day label
        Text(
            text = dayData.dayLabel,
            fontSize = 10.sp,
            fontWeight = if (dayData.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (dayData.isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        // Progress circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (dayData.isToday) {
                        Modifier.background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (dayData.complianceStatus) {
                ComplianceStatus.NO_DATA -> {
                    // Gray circle for no data
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = androidx.compose.ui.graphics.Color(0xFFBDBDBD).copy(alpha = 0.5f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "—",
                            fontSize = 16.sp,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {
                    val progressColor = when (dayData.complianceStatus) {
                        ComplianceStatus.UNDER_GOAL -> 0xFF4CAF50
                        ComplianceStatus.CLOSE_TO_GOAL -> 0xFFFFC107
                        ComplianceStatus.OVER_GOAL -> 0xFFFF5252
                        else -> 0xFFBDBDBD
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = androidx.compose.ui.graphics.Color(progressColor),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (dayData.complianceStatus) {
                                ComplianceStatus.UNDER_GOAL -> "✓"
                                ComplianceStatus.CLOSE_TO_GOAL -> "~"
                                ComplianceStatus.OVER_GOAL -> "✗"
                                else -> "—"
                            },
                            fontSize = 16.sp,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Minutes
        Text(
            text = when (dayData.complianceStatus) {
                ComplianceStatus.NO_DATA -> "—"
                else -> "${dayData.totalMinutes}m"
            },
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * Legend item
 */
@Composable
private fun LegendItem(color: Long, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = androidx.compose.ui.graphics.Color(color),
                    shape = CircleShape
                )
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Data class for daily progress
 */
private data class DayProgressData(
    val date: Long,
    val dayLabel: String,
    val totalMinutes: Int,
    val complianceStatus: ComplianceStatus,
    val isToday: Boolean
)

/**
 * Compliance status enum
 */
private enum class ComplianceStatus {
    UNDER_GOAL,
    CLOSE_TO_GOAL,
    OVER_GOAL,
    NO_DATA
}

/**
 * Prepare timeline data from sessions
 */
private fun prepareTimelineData(
    sessions: List<UsageSession>,
    dailyGoalMinutes: Int
): List<DayProgressData> {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    val currentYear = calendar.get(Calendar.YEAR)

    // Group sessions by day
    val dailyMap = mutableMapOf<Int, Float>() // day of year -> total minutes

    sessions.forEach { session ->
        val sessionCalendar = Calendar.getInstance()
        sessionCalendar.timeInMillis = session.startTimestamp
        val dayOfYear = sessionCalendar.get(Calendar.DAY_OF_YEAR)
        val sessionYear = sessionCalendar.get(Calendar.YEAR)

        // Only consider current year
        if (sessionYear == currentYear) {
            val durationMinutes = session.duration / 60000f
            dailyMap[dayOfYear] = (dailyMap[dayOfYear] ?: 0f) + durationMinutes
        }
    }

    // Generate data for last 30 days (or all available days)
    val result = mutableListOf<DayProgressData>()
    val daysToShow = minOf(30, today) // Don't go beyond day 1

    for (i in daysToShow downTo 0) {
        val dayOfYear = today - i
        val totalMinutes = dailyMap[dayOfYear]?.toInt() ?: 0
        val complianceStatus = when {
            totalMinutes == 0 -> ComplianceStatus.NO_DATA
            totalMinutes <= dailyGoalMinutes * 0.9 -> ComplianceStatus.UNDER_GOAL
            totalMinutes <= dailyGoalMinutes * 1.1 -> ComplianceStatus.CLOSE_TO_GOAL
            else -> ComplianceStatus.OVER_GOAL
        }

        val dayLabel = when (i) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> {
                val labelCalendar = Calendar.getInstance()
                labelCalendar.set(Calendar.DAY_OF_YEAR, dayOfYear)
                val dayOfWeek = labelCalendar.get(Calendar.DAY_OF_WEEK)
                when (dayOfWeek) {
                    Calendar.MONDAY -> "Mon"
                    Calendar.TUESDAY -> "Tue"
                    Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"
                    Calendar.FRIDAY -> "Fri"
                    Calendar.SATURDAY -> "Sat"
                    Calendar.SUNDAY -> "Sun"
                    else -> ""
                }
            }
        }

        result.add(
            DayProgressData(
                date = 0L,
                dayLabel = dayLabel,
                totalMinutes = totalMinutes,
                complianceStatus = complianceStatus,
                isToday = i == 0
            )
        )
    }

    return result.sortedByDescending { it.isToday }
}
