package dev.sadakat.thinkfaster.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import dev.sadakat.thinkfaster.util.HapticFeedback
import kotlinx.coroutines.delay

/**
 * Quick Win Celebration Dialogs
 * First-Week Retention Feature - Phase 4: UI Components
 *
 * Four celebration dialogs for early progress milestones:
 * - First Session Tracked (Day 1)
 * - First Session Under Goal (Day 1)
 * - Day 1 Complete (midnight notification follow-up)
 * - Day 2 Complete (building momentum)
 */

/**
 * First Session Celebration - Day 1 immediate feedback
 */
@Composable
fun FirstSessionCelebration(
    show: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(show) {
        if (show) {
            HapticFeedback.success(context)
            delay(3000)
            onDismiss()
        }
    }

    CelebrationDialog(
        show = show,
        emoji = "🌱",
        title = "First Session Tracked!",
        message = "You're off to a great start. Every journey begins with a single step.",
        onDismiss = onDismiss
    )
}

/**
 * First Under Goal Celebration - Day 1 positive reinforcement
 */
@Composable
fun FirstUnderGoalCelebration(
    show: Boolean,
    goalMinutes: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(show) {
        if (show) {
            HapticFeedback.success(context)
            delay(3000)
            onDismiss()
        }
    }

    CelebrationDialog(
        show = show,
        emoji = "🎯",
        title = "Under Your Goal!",
        message = "You stayed under $goalMinutes minutes. This is how progress happens!",
        onDismiss = onDismiss
    )
}

/**
 * Day One Complete Celebration - Midnight notification follow-up
 */
@Composable
fun DayOneCelebration(
    show: Boolean,
    usageMinutes: Int,
    goalMinutes: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(show) {
        if (show) {
            HapticFeedback.celebration(context)
            delay(3000)
            onDismiss()
        }
    }

    CelebrationDialog(
        show = show,
        emoji = "✨",
        title = "Day 1 Complete!",
        message = "You tracked $usageMinutes min (goal: $goalMinutes). Tomorrow, let's keep the momentum going!",
        onDismiss = onDismiss
    )
}

/**
 * Day Two Complete Celebration - Building momentum
 */
@Composable
fun DayTwoCelebration(
    show: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(show) {
        if (show) {
            HapticFeedback.celebration(context)
            delay(3000)
            onDismiss()
        }
    }

    CelebrationDialog(
        show = show,
        emoji = "🔥",
        title = "2 Days in a Row!",
        message = "You're building a habit! Consistency is the key to lasting change.",
        onDismiss = onDismiss
    )
}
