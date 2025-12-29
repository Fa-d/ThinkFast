package dev.sadakat.thinkfast.domain.usecase.streaks

import android.content.Context
import android.util.Log
import dev.sadakat.thinkfast.data.preferences.StreakFreezePreferences
import dev.sadakat.thinkfast.domain.repository.GoalRepository
import dev.sadakat.thinkfast.domain.repository.StreakRecoveryRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository
import dev.sadakat.thinkfast.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Enhanced streak update use case with freeze and recovery support
 * Replaces UpdateStreakUseCase with additional features:
 * - Streak freeze activation/deactivation
 * - Recovery tracking when streaks break
 * - Recovery milestone notifications
 * - Streak broken notifications
 *
 * Should be run daily (midnight) by DailyStatsAggregatorWorker
 */
class UpdateStreakWithRecoveryUseCase(
    private val goalRepository: GoalRepository,
    private val usageRepository: UsageRepository,
    private val streakRecoveryRepository: StreakRecoveryRepository,
    private val freezePreferences: StreakFreezePreferences,
    private val notificationHelper: NotificationHelper,
    private val context: Context
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Update streaks for all active goals based on yesterday's performance
     * This should be called at the start of a new day to evaluate the previous day
     */
    suspend operator fun invoke() {
        val yesterday = getYesterdayDate()
        val allGoals = goalRepository.getAllGoals()

        Log.d(TAG, "Updating streaks for ${allGoals.size} goals for date: $yesterday")

        allGoals.forEach { goal ->
            updateStreakForGoal(goal.targetApp, yesterday)
        }

        Log.d(TAG, "Streak updates complete")
    }

    /**
     * Update streak for a specific goal with freeze and recovery logic
     *
     * Flow:
     * 1. Calculate if goal was met (usage ≤ limit)
     * 2. Check if freeze active for this date
     * 3. If freeze active:
     *    - Don't change streak
     *    - Deactivate freeze
     *    - Log freeze usage
     * 4. Else if goal met:
     *    - Increment streak
     *    - Update recovery progress if in recovery
     *    - Check if recovery complete (show celebration)
     * 5. Else (goal NOT met):
     *    - Capture previous streak
     *    - Start recovery
     *    - Send "streak broken" notification
     *    - Reset streak to 0
     */
    private suspend fun updateStreakForGoal(targetApp: String, date: String) {
        val goal = goalRepository.getGoalByApp(targetApp) ?: return

        // Calculate if goal was met
        val sessions = usageRepository.getSessionsByAppInRange(
            targetApp = targetApp,
            startDate = date,
            endDate = date
        )
        val totalUsageMillis = sessions.sumOf { it.duration }
        val usageMinutes = (totalUsageMillis / 1000 / 60).toInt()
        val goalMet = usageMinutes <= goal.dailyLimitMinutes

        Log.d(
            TAG,
            "$targetApp on $date: usage=$usageMinutes min, limit=${goal.dailyLimitMinutes} min, " +
                    "goalMet=$goalMet, currentStreak=${goal.currentStreak}"
        )

        // Check for active freeze
        val hasActiveFreeze = freezePreferences.hasActiveFreezeForApp(targetApp)
        val freezeDate = freezePreferences.getFreezeActivationDate(targetApp)

        if (hasActiveFreeze && freezeDate == date) {
            // Freeze is active for this date - don't change streak, deactivate freeze
            freezePreferences.deactivateFreezeForApp(targetApp)
            Log.d(TAG, "Freeze used for $targetApp on $date - streak preserved at ${goal.currentStreak}")
            return
        }

        if (goalMet) {
            // Goal met - increment streak
            val newStreak = goal.currentStreak + 1
            goalRepository.updateCurrentStreak(targetApp, newStreak)
            goalRepository.updateBestStreakIfNeeded(targetApp, newStreak)
            Log.d(TAG, "$targetApp streak incremented: ${goal.currentStreak} → $newStreak")

            // Update recovery progress if in recovery
            val recovery = streakRecoveryRepository.getRecoveryByApp(targetApp)
            if (recovery != null && !recovery.isRecoveryComplete) {
                val newRecoveryDays = recovery.currentRecoveryDays + 1
                streakRecoveryRepository.updateRecoveryProgress(targetApp, newRecoveryDays)
                Log.d(TAG, "$targetApp recovery progress: ${recovery.currentRecoveryDays} → $newRecoveryDays")

                val recoveryTarget = recovery.calculateRecoveryTarget()

                // Check if recovery complete
                if (newRecoveryDays >= recoveryTarget) {
                    streakRecoveryRepository.completeRecovery(targetApp, getTodayDate())
                    Log.d(TAG, "$targetApp recovery complete! Reached $newRecoveryDays/$recoveryTarget days")

                    // Show "back on track" notification
                    notificationHelper.showRecoveryCompleteNotification(
                        context = context,
                        targetApp = targetApp,
                        previousStreak = recovery.previousStreak,
                        daysToRecover = newRecoveryDays
                    )
                } else if (recovery.isRecoveryMilestone(newRecoveryDays)) {
                    // Show recovery milestone notification
                    Log.d(TAG, "$targetApp recovery milestone: $newRecoveryDays days")
                    notificationHelper.showRecoveryMilestoneNotification(
                        context = context,
                        targetApp = targetApp,
                        daysRecovered = newRecoveryDays,
                        targetDays = recoveryTarget
                    )
                }
            }
        } else {
            // Goal NOT met - streak breaks
            val previousStreak = goal.currentStreak

            if (previousStreak > 0) {
                // Only create recovery if there was a streak to lose
                streakRecoveryRepository.startRecovery(
                    targetApp = targetApp,
                    previousStreak = previousStreak,
                    startDate = getTodayDate()
                )
                Log.d(TAG, "$targetApp streak broken! Starting recovery from $previousStreak days")

                // Send streak broken notification
                notificationHelper.showStreakBrokenNotification(
                    context = context,
                    targetApp = targetApp,
                    previousStreak = previousStreak
                )
            } else {
                Log.d(TAG, "$targetApp goal not met, but no streak to break (was already at 0)")
            }

            // Reset streak to 0
            goalRepository.resetCurrentStreak(targetApp)
        }
    }

    /**
     * Force check today's progress (for real-time feedback)
     * @param targetApp Package name of the app
     * @return true if currently meeting goal, false otherwise
     */
    suspend fun checkTodayProgress(targetApp: String): Boolean {
        val today = dateFormatter.format(Date())
        val goal = goalRepository.getGoalByApp(targetApp) ?: return false

        val sessions = usageRepository.getSessionsByAppInRange(
            targetApp = targetApp,
            startDate = today,
            endDate = today
        )

        val totalUsageMillis = sessions.sumOf { it.duration }
        val usageMinutes = (totalUsageMillis / 1000 / 60).toInt()

        return usageMinutes <= goal.dailyLimitMinutes
    }

    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return dateFormatter.format(calendar.time)
    }

    private fun getTodayDate(): String {
        return dateFormatter.format(Date())
    }

    companion object {
        private const val TAG = "UpdateStreakWithRecovery"
    }
}
