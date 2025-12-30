package dev.sadakat.thinkfaster.domain.model

data class DailyStats(
    val date: String,  // YYYY-MM-DD
    val targetApp: String,
    val totalDuration: Long,
    val sessionCount: Int,
    val longestSession: Long,
    val averageSession: Long,
    val alertsShown: Int = 0,
    val alertsProceeded: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
