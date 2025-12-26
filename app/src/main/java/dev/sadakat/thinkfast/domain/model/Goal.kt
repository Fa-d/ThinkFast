package dev.sadakat.thinkfast.domain.model

data class Goal(
    val targetApp: String,
    val dailyLimitMinutes: Int,
    val startDate: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
