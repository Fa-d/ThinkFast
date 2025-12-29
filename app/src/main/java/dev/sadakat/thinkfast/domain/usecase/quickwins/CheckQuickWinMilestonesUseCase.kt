package dev.sadakat.thinkfast.domain.usecase.quickwins

import dev.sadakat.thinkfast.data.preferences.OnboardingQuestPreferences
import dev.sadakat.thinkfast.domain.model.QuickWinType
import dev.sadakat.thinkfast.domain.repository.GoalRepository
import dev.sadakat.thinkfast.domain.repository.UsageRepository

/**
 * CheckQuickWinMilestonesUseCase - Determines which quick win celebration to show
 * First-Week Retention Feature - Phase 2: Business Logic
 *
 * Checks in priority order which celebration should be shown based on current state.
 * Returns the highest priority celebration, or null if no celebrations to show.
 */
class CheckQuickWinMilestonesUseCase(
    private val usageRepository: UsageRepository,
    private val goalRepository: GoalRepository,
    private val questPreferences: OnboardingQuestPreferences
) {
    suspend operator fun invoke(): QuickWinType? {
        // Check in priority order (most important first)

        // 1. First session ever
        if (!questPreferences.isFirstSessionCelebrated()) {
            val todayUsage = getTodayTotalUsage()
            if (todayUsage > 0) {
                return QuickWinType.FIRST_SESSION
            }
        }

        // 2. First session under goal
        if (!questPreferences.isFirstUnderGoalCelebrated()) {
            val todayUsage = getTodayTotalUsage()
            val goals = goalRepository.getAllGoals()
            val combinedGoal = goals.sumOf { it.dailyLimitMinutes }

            if (todayUsage > 0 && (todayUsage / 1000 / 60) < combinedGoal) {
                return QuickWinType.FIRST_UNDER_GOAL
            }
        }

        // Day 1 & 2 complete checks happen in AchievementWorker (after midnight)

        return null
    }

    private suspend fun getTodayTotalUsage(): Long {
        val facebookUsage = usageRepository.getTodayUsageForApp("com.facebook.katana")
        val instagramUsage = usageRepository.getTodayUsageForApp("com.instagram.android")
        return facebookUsage + instagramUsage
    }
}
