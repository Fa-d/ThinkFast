package dev.sadakat.thinkfaster.domain.usecase.quest

import dev.sadakat.thinkfaster.data.preferences.OnboardingQuestPreferences
import dev.sadakat.thinkfaster.domain.model.OnboardingQuest
import dev.sadakat.thinkfaster.domain.repository.GoalRepository

/**
 * GetOnboardingQuestStatusUseCase - Returns current quest status for UI display
 * First-Week Retention Feature - Phase 2: Business Logic
 *
 * Auto-starts quest if goals exist but quest not started.
 * Returns OnboardingQuest with current progress and next milestone.
 */
class GetOnboardingQuestStatusUseCase(
    private val questPreferences: OnboardingQuestPreferences,
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(): OnboardingQuest {
        // Auto-start quest if goals exist but quest not started
        val goals = goalRepository.getAllGoals()
        if (goals.isNotEmpty() && !questPreferences.isQuestActive() && !questPreferences.isQuestCompleted()) {
            val earliestGoal = goals.minByOrNull { it.startDate }
            earliestGoal?.let {
                questPreferences.startQuest(it.startDate)
            }
        }

        val currentDay = questPreferences.getCurrentQuestDay()
        val daysCompleted = (currentDay - 1).coerceAtLeast(0)

        return OnboardingQuest(
            isActive = questPreferences.isQuestActive(),
            currentDay = currentDay,
            totalDays = 7,
            daysCompleted = daysCompleted,
            progressPercentage = daysCompleted / 7f,
            isCompleted = questPreferences.isQuestCompleted(),
            nextMilestone = if (currentDay in 1..6) {
                "Complete today to unlock Day ${currentDay + 1} reward!"
            } else null
        )
    }
}
