package dev.sadakat.thinkfast.domain.model

data class UsageEvent(
    val id: Long = 0,
    val sessionId: Long,
    val eventType: String,
    val timestamp: Long,
    val metadata: String? = null
)
