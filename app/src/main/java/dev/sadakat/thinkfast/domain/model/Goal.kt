package dev.sadakat.thinkfast.domain.model

data class Goal(
    val id: String = "current_goal",
    val dailyLimitMinutes: Int,
    val targetApp: String,
    val startDate: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)
