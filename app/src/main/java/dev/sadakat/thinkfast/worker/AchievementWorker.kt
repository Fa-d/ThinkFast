package dev.sadakat.thinkfast.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sadakat.thinkfast.domain.repository.GoalRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.util.HapticFeedback
import dev.sadakat.thinkfast.util.NotificationHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Worker that checks for achievements and sends notifications
 * Runs daily after DailyStatsAggregatorWorker updates streaks
 *
 * Phase 1.5: Quick Wins - Achievement notifications
 */
class AchievementWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val goalRepository: GoalRepository by inject()
    private val usageRepository: UsageRepository by inject()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "AchievementWorker started")

            // Check achievements for yesterday (streak was just updated for yesterday)
            val yesterday = getYesterdayDate()
            checkAndNotifyAchievements(yesterday)

            Log.d(TAG, "AchievementWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "AchievementWorker failed", e)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Check for achievements and send appropriate notifications
     */
    private suspend fun checkAndNotifyAchievements(date: String) {
        // Get all active goals
        val allGoals = goalRepository.getAllGoals()

        if (allGoals.isEmpty()) {
            Log.d(TAG, "No goals set, skipping achievement check")
            return
        }

        var totalUsageMinutes = 0
        var totalGoalMinutes = 0
        var maxStreak = 0

        // Check each goal
        allGoals.forEach { goal ->
            // Get sessions for the date
            val sessions = usageRepository.getSessionsByAppInRange(
                targetApp = goal.targetApp,
                startDate = date,
                endDate = date
            )

            // Calculate usage
            val totalUsageMillis = sessions.sumOf { it.duration }
            val usageMinutes = (totalUsageMillis / 1000 / 60).toInt()

            // Accumulate totals
            totalUsageMinutes += usageMinutes
            totalGoalMinutes += goal.dailyLimitMinutes

            // Track max streak
            if (goal.currentStreak > maxStreak) {
                maxStreak = goal.currentStreak
            }

            Log.d(TAG, "Goal for ${goal.targetApp}: $usageMinutes/${goal.dailyLimitMinutes} min, streak: ${goal.currentStreak}")
        }

        // Determine if overall goal was met (combined usage under combined limit)
        val goalMet = totalUsageMinutes <= totalGoalMinutes

        if (goalMet) {
            Log.d(TAG, "Goal achieved! Usage: $totalUsageMinutes/$totalGoalMinutes min, Streak: $maxStreak")

            // Check if this is a special milestone
            when {
                maxStreak == 1 -> {
                    // First-time achievement
                    NotificationHelper.showFirstTimeAchievementNotification(context)
                    Log.d(TAG, "Sent first-time achievement notification")
                }
                NotificationHelper.isStreakMilestone(maxStreak) -> {
                    // Milestone streak (3, 7, 14, 30 days)
                    NotificationHelper.showStreakMilestoneNotification(context, maxStreak)
                    Log.d(TAG, "Sent streak milestone notification for $maxStreak days")
                }
                else -> {
                    // Regular daily achievement
                    NotificationHelper.showDailyAchievementNotification(
                        context,
                        totalUsageMinutes,
                        totalGoalMinutes,
                        maxStreak
                    )
                    Log.d(TAG, "Sent daily achievement notification")
                }
            }
        } else {
            Log.d(TAG, "Goal not met: $totalUsageMinutes/$totalGoalMinutes min")
        }
    }

    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return dateFormatter.format(calendar.time)
    }

    companion object {
        private const val TAG = "AchievementWorker"
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "daily_achievement_check"
    }
}
