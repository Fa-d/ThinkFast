package dev.sadakat.thinkfaster.domain.model

data class NotificationData(
    val title: String,
    val message: String,
    val type: NotificationType
)

enum class NotificationType {
    MORNING_INTENTION,
    EVENING_REVIEW,
    STREAK_WARNING,
    NON_USAGE_ENCOURAGEMENT
}
