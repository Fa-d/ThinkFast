package dev.sadakat.thinkfaster.presentation.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.theme.AppColors

/**
 * Data class for streak and consistency metrics
 */
data class StreakConsistency(
    val currentStreak: Int,
    val daysMetGoal: Int,
    val totalDays: Int
)

/**
 * Visual card showing streak and consistency side-by-side
 * Left: Fire emoji + streak count
 * Right: Checkmark + consistency count
 */
@Composable
fun StreakConsistencyCard(
    streakConsistency: StreakConsistency,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Left: Streak section
            StreakSection(
                streak = streakConsistency.currentStreak,
                modifier = Modifier.weight(1f)
            )

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            )

            // Right: Consistency section
            ConsistencySection(
                daysMetGoal = streakConsistency.daysMetGoal,
                totalDays = streakConsistency.totalDays,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Streak section with fire emoji and count
 */
@Composable
private fun StreakSection(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val streakColor = AppColors.Streak.getColorForStreak(streak)

    Box(
        modifier = modifier
            .background(
                color = streakColor.copy(alpha = 0.1f),
                shape = Shapes.card
            )
            .padding(Spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Fire emoji
            Text(
                text = "🔥",
                fontSize = 48.sp
            )

            // Streak count
            Text(
                text = streak.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Label
            Text(
                text = if (streak == 1) "day streak" else "day streak",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Consistency section with checkmark icon and days count
 */
@Composable
private fun ConsistencySection(
    daysMetGoal: Int,
    totalDays: Int,
    modifier: Modifier = Modifier
) {
    val consistencyColor = AppColors.Progress.OnTrack

    Box(
        modifier = modifier
            .background(
                color = consistencyColor.copy(alpha = 0.1f),
                shape = Shapes.card
            )
            .padding(Spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Checkmark icon (using text emoji for simplicity)
            Text(
                text = "✓",
                fontSize = 48.sp,
                color = consistencyColor
            )

            // Days count
            Text(
                text = "$daysMetGoal of $totalDays",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Label
            Text(
                text = "days met goal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
