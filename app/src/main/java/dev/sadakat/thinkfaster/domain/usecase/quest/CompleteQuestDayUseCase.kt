package dev.sadakat.thinkfaster.domain.usecase.quest

import android.content.Context
import dev.sadakat.thinkfaster.analytics.AnalyticsManager
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.data.preferences.OnboardingQuestPreferences
import dev.sadakat.thinkfaster.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CompleteQuestDayUseCase - Marks a quest day as completed and triggers celebration
 * First-Week Retention Feature - Phase 2: Business Logic
 *
 * Called by AchievementWorker after midnight when goals are met.
 * Sends appropriate notification based on which day was completed.
 */
class CompleteQuestDayUseCase(
    private val questPreferences: OnboardingQuestPreferences,
    private val notificationHelper: NotificationHelper,
    private val context: Context,
    private val analyticsManager: AnalyticsManager,
    private val interventionPreferences: InterventionPreferences
) {
    operator fun invoke(day: Int, goalMet: Boolean) {
        if (!questPreferences.isQuestActive()) return
        if (questPreferences.isDayMilestoneShown(day)) return
        if (!goalMet) return  // Only celebrate if goal was met

        // Mark milestone shown
        questPreferences.markDayCompleted(day)

        // Track analytics
        val daysSinceInstall = interventionPreferences.getDaysSinceInstall()
        val stepName = when (day) {
            1 -> "day_1_first_goal"
            2 -> "day_2_building_momentum"
            7 -> "day_7_quest_complete"
            else -> "day_$day"
        }
        analyticsManager.trackQuestStepCompleted(stepName, daysSinceInstall)

        // Send notification based on day
        when (day) {
            1 -> notificationHelper.showQuestDayNotification(context, 1, "🌱", "Great start!")
            2 -> notificationHelper.showQuestDayNotification(context, 2, "💪", "Building momentum!")
            7 -> {
                questPreferences.markQuestComplete(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
                // Track quest completion analytics
                analyticsManager.trackQuestCompleted(daysSinceInstall)
                notificationHelper.showQuestCompleteNotification(context)
            }
            // Day 3-6 handled by existing streak milestone celebrations
        }
    }
}
