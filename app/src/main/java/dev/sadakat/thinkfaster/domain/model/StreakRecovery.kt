package dev.sadakat.thinkfaster.domain.model

data class StreakRecovery(
    val targetApp: String,
    val previousStreak: Int,
    val recoveryStartDate: String,
    val currentRecoveryDays: Int,
    val isRecoveryComplete: Boolean,
    val recoveryCompletedDate: String?,
    val notificationShown: Boolean,
    val timestamp: Long
) {
    /**
     * Calculate recovery target: 50% of previous streak OR 7 days, whichever is lower
     * Example: 20-day streak → recover in 7 days
     *          6-day streak → recover in 3 days
     */
    fun calculateRecoveryTarget(): Int {
        val halfStreak = (previousStreak / 2).coerceAtLeast(1)
        return halfStreak.coerceAtMost(7)
    }

    /**
     * Get recovery progress as a float (0.0 to 1.0)
     */
    fun getRecoveryProgress(): Float {
        val targetDays = calculateRecoveryTarget()
        return (currentRecoveryDays.toFloat() / targetDays).coerceIn(0f, 1f)
    }

    /**
     * Check if current day count is a recovery milestone (1, 3, 7, 14 days)
     */
    fun isRecoveryMilestone(days: Int): Boolean {
        return days in listOf(1, 3, 7, 14)
    }

    /**
     * Get contextual recovery message based on current progress
     */
    fun getRecoveryMessage(): String {
        val target = calculateRecoveryTarget()
        val remaining = target - currentRecoveryDays

        return when {
            isRecoveryComplete -> "You're back on track! 🎉"
            currentRecoveryDays == 0 -> "Your $previousStreak-day streak was amazing! You're 1 day away from getting back on track."
            remaining == 1 -> "Just 1 day until you're back on track!"
            remaining > 1 -> "$remaining days until you're back on track!"
            else -> "Almost there! Keep going!"
        }
    }

    /**
     * Get shortened message for cards
     */
    fun getShortMessage(): String {
        val target = calculateRecoveryTarget()
        val remaining = target - currentRecoveryDays

        return when {
            isRecoveryComplete -> "Back on track!"
            currentRecoveryDays == 0 -> "Start your comeback today"
            remaining == 1 -> "1 more day!"
            else -> "$remaining more days"
        }
    }
}

data class StreakFreezeStatus(
    val freezesAvailable: Int,
    val maxFreezes: Int,
    val hasActiveFreeze: Boolean,
    val freezeActivationDate: String?,
    val canUseFreeze: Boolean
) {
    /**
     * Get display text for freeze count
     */
    fun getFreezeCountText(): String = "$freezesAvailable/$maxFreezes"

    /**
     * Check if user is out of freezes
     */
    fun isOutOfFreezes(): Boolean = freezesAvailable <= 0
}
