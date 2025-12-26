package dev.sadakat.thinkfast.domain.model

data class UsageSession(
    val id: Long = 0,
    val targetApp: String,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val duration: Long,
    val wasInterrupted: Boolean = false,
    val interruptionType: String? = null,
    val date: String  // YYYY-MM-DD
)
