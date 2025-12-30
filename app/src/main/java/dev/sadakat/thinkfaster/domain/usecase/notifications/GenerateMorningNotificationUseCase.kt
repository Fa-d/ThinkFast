package dev.sadakat.thinkfaster.domain.usecase.notifications

import dev.sadakat.thinkfaster.domain.model.NotificationData
import dev.sadakat.thinkfaster.domain.model.NotificationType
import dev.sadakat.thinkfaster.domain.repository.GoalRepository

class GenerateMorningNotificationUseCase(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(): NotificationData {
        val goals = goalRepository.getAllGoals()
        val maxStreak = goals.maxOfOrNull { it.currentStreak } ?: 0

        val message = when {
            maxStreak == 0 -> "Good morning! Set your intention for the day. What's your social media goal?"
            maxStreak < 7 -> "Good morning! Your streak is at $maxStreak ${if (maxStreak == 1) "day" else "days"}. Keep it going! 🔥"
            else -> "Good morning, champion! You're on a $maxStreak-day streak. Stay strong today! 💪"
        }

        return NotificationData(
            title = "Morning Intention 🌅",
            message = message,
            type = NotificationType.MORNING_INTENTION
        )
    }
}
