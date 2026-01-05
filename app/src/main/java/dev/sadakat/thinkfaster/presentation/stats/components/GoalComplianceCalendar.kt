package dev.sadakat.thinkfaster.presentation.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Goal compliance calendar showing monthly view of goal achievement
 * Phase 5: Visual month-at-a-glance compliance tracking
 * Phase 4.3: Added month navigation
 *
 * Visual indicators:
 * - Green: Goal met
 * - Red: Goal not met
 * - Gray: No data or no goal set
 * - Today: Border highlight
 */
@Composable
fun GoalComplianceCalendar(
    complianceData: Map<String, Boolean?>,  // date -> met goal? (null = no data)
    modifier: Modifier = Modifier,
    monthOffset: Int = 0,  // Phase 4.3: 0 = current month, -1 = previous, +1 = next
    onPreviousMonth: () -> Unit = {},  // Phase 4.3: Navigation callback
    onNextMonth: () -> Unit = {}  // Phase 4.3: Navigation callback
) {
    val calendar = Calendar.getInstance()
    // Phase 4.3: Apply month offset
    calendar.add(Calendar.MONTH, monthOffset)

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    val displayMonth = SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Phase 4.3: Enhanced header with navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous month button
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Month display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Goal Compliance",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = displayMonth,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Next month button
                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend (matching GoalProgressTimeline style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LegendItem(color = 0xFF4CAF50, label = "Under")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = 0xFFFF5252, label = "Over")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = 0xFFBDBDBD, label = "No data")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day labels (S M T W T F S)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid - Phase 4.3: Use calendar with month offset
            // Fixed height to avoid infinite constraints in LazyColumn
            val calendarDays = generateCalendarDays(calendar)

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),  // Fixed height: 6 rows of ~40dp cells + spacing
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                userScrollEnabled = false  // Disable nested scrolling
            ) {
                items(calendarDays) { day ->
                    CalendarDayCell(
                        day = day,
                        isToday = day.date == today,
                        metGoal = complianceData[day.date],
                        isCurrentMonth = day.isCurrentMonth
                    )
                }
            }

            // Compliance summary
            Spacer(modifier = Modifier.height(16.dp))
            ComplianceSummary(complianceData)
        }
    }
}

/**
 * Single day cell in the calendar - styled like GoalProgressTimeline
 */
@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isToday: Boolean,
    metGoal: Boolean?,
    isCurrentMonth: Boolean
) {
    // Determine compliance status (similar to GoalProgressTimeline)
    val complianceStatus = when {
        metGoal == true -> ComplianceStatus.UNDER_GOAL
        metGoal == false -> ComplianceStatus.OVER_GOAL
        else -> null
    }

    val progressColor = when (complianceStatus) {
        ComplianceStatus.UNDER_GOAL -> 0xFF4CAF50
        ComplianceStatus.OVER_GOAL -> 0xFFFF5252
        null -> 0xFFBDBDBD
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .then(
                if (isToday) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (complianceStatus != null && isCurrentMonth) {
            // Colored circle with status symbol (like GoalProgressTimeline)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(progressColor).copy(
                            alpha = if (isCurrentMonth) 1f else 0.3f
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (complianceStatus) {
                        ComplianceStatus.UNDER_GOAL -> "✓"
                        ComplianceStatus.OVER_GOAL -> "✗"
                    },
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // No data or not current month - show gray circle with day number
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(0xFFBDBDBD).copy(
                            alpha = if (isCurrentMonth) 0.3f else 0.1f
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.dayOfMonth.toString(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (isCurrentMonth) 0.5f else 0.3f
                    )
                )
            }
        }

        // Today indicator dot
        if (isToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .size(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Compliance status enum (matching GoalProgressTimeline)
 */
private enum class ComplianceStatus {
    UNDER_GOAL,
    OVER_GOAL
}

/**
 * Legend item showing color and label (matching GoalProgressTimeline style)
 */
@Composable
private fun LegendItem(
    color: Long,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(color))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Summary statistics below the calendar
 */
@Composable
private fun ComplianceSummary(complianceData: Map<String, Boolean?>) {
    val totalDays = complianceData.size
    val metGoalDays = complianceData.values.count { it == true }
    val missedGoalDays = complianceData.values.count { it == false }
    val complianceRate = if (totalDays > 0) {
        (metGoalDays.toFloat() / totalDays * 100).toInt()
    } else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SummaryItem(
            label = "Compliance",
            value = "$complianceRate%",
            icon = "🎯"
        )
        SummaryItem(
            label = "Met Goal",
            value = "$metGoalDays days",
            icon = "✅"
        )
        SummaryItem(
            label = "Missed",
            value = "$missedGoalDays days",
            icon = "❌"
        )
    }
}

/**
 * Summary statistic item
 */
@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Data class for calendar day
 */
private data class CalendarDay(
    val date: String,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean
)

/**
 * Generate calendar days for display
 * @param showCurrentMonth If true, shows current month. If false, shows last 30 days
 */
/**
 * Phase 4.3: Updated to accept calendar with month offset
 */
private fun generateCalendarDays(calendar: Calendar): List<CalendarDay> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val days = mutableListOf<CalendarDay>()

    // Get month from provided calendar (which has monthOffset applied)
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    // Set to first day of month
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    // Get day of week for first day (1 = Sunday, 7 = Saturday)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    // Add empty days for padding at start
    val paddingDays = firstDayOfWeek - 1
    calendar.add(Calendar.DAY_OF_MONTH, -paddingDays)

    // Generate 42 days (6 weeks) to fill the grid
    repeat(42) {
        val date = dateFormat.format(calendar.time)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val isCurrentMonth = month == currentMonth

        days.add(
            CalendarDay(
                date = date,
                dayOfMonth = dayOfMonth,
                isCurrentMonth = isCurrentMonth
            )
        )

        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }

    return days
}
