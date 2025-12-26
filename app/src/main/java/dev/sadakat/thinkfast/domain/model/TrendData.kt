package dev.sadakat.thinkfast.domain.model

data class TrendData(
    val totalDuration: Long,
    val averageDailyDuration: Long,
    val trend: TrendDirection,
    val percentageChange: Float
)

enum class TrendDirection {
    INCREASING,
    DECREASING,
    STABLE
}
