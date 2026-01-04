package dev.sadakat.thinkfaster.service

import android.content.Context
import android.os.SystemClock
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.util.ErrorLogger
import java.util.Calendar

/**
 * Rate limiter for interventions to prevent overwhelming users
 * Tracks intervention history and enforces cooldown periods
 *
 * Key features:
 * - Global cooldown between any interventions (5 min default)
 * - Type-specific cooldowns (reminders vs timers)
 * - Session duration filtering (skip very short sessions)
 * - Hourly and daily intervention limits
 * - Adaptive cooldowns that escalate with dismissal patterns
 */
class InterventionRateLimiter(
    private val context: Context,
    private val interventionPreferences: InterventionPreferences
) {

    // Cooldown configuration (in milliseconds)
    private val GLOBAL_COOLDOWN_MS = 5 * 60 * 1000L  // 5 minutes between ANY interventions
    private val REMINDER_COOLDOWN_MS = 10 * 60 * 1000L  // 10 minutes between reminders
    private val TIMER_COOLDOWN_MS = 15 * 60 * 1000L  // 15 minutes between timer alerts
    private val SESSION_COOLDOWN_MS = 2 * 60 * 1000L  // 2 minutes (don't interrupt very short sessions)

    // Maximum interventions per time window
    private val MAX_INTERVENTIONS_PER_HOUR = 4
    private val MAX_INTERVENTIONS_PER_DAY = 20

    /**
     * Result of rate limit check
     */
    data class RateLimitResult(
        val allowed: Boolean,
        val reason: String,
        val cooldownRemainingMs: Long = 0
    )

    /**
     * Intervention types
     */
    enum class InterventionType {
        REMINDER, TIMER
    }

    /**
     * Check if we can show an intervention right now
     *
     * @param interventionType Type of intervention (reminder or timer)
     * @param sessionDurationMs Duration of current session in milliseconds
     * @return RateLimitResult indicating if intervention is allowed and why
     */
    fun canShowIntervention(
        interventionType: InterventionType,
        sessionDurationMs: Long
    ): RateLimitResult {

        // Check 1: Session too short (skip interventions for quick checks)
        if (sessionDurationMs < SESSION_COOLDOWN_MS) {
            return RateLimitResult(
                allowed = false,
                reason = "Session too short (${sessionDurationMs / 1000}s < 2min)",
                cooldownRemainingMs = SESSION_COOLDOWN_MS - sessionDurationMs
            )
        }

        // Check 2: Global cooldown (any intervention)
        val lastInterventionTime = interventionPreferences.getLastInterventionTime()
        val timeSinceLastIntervention = System.currentTimeMillis() - lastInterventionTime
        val cooldownMultiplier = interventionPreferences.getCooldownMultiplier()
        val adjustedGlobalCooldown = (GLOBAL_COOLDOWN_MS * cooldownMultiplier).toLong()

        if (timeSinceLastIntervention < adjustedGlobalCooldown) {
            val remaining = adjustedGlobalCooldown - timeSinceLastIntervention
            return RateLimitResult(
                allowed = false,
                reason = "Global cooldown active (${remaining / 1000}s remaining, ${cooldownMultiplier}x multiplier)",
                cooldownRemainingMs = remaining
            )
        }

        // Check 3: Type-specific cooldown
        val lastTypeTime = when (interventionType) {
            InterventionType.REMINDER -> interventionPreferences.getLastReminderTime()
            InterventionType.TIMER -> interventionPreferences.getLastTimerTime()
        }

        val typeCooldown = when (interventionType) {
            InterventionType.REMINDER -> REMINDER_COOLDOWN_MS
            InterventionType.TIMER -> TIMER_COOLDOWN_MS
        }

        val timeSinceLastType = System.currentTimeMillis() - lastTypeTime
        if (timeSinceLastType < typeCooldown) {
            val remaining = typeCooldown - timeSinceLastType
            return RateLimitResult(
                allowed = false,
                reason = "${interventionType.name} cooldown active (${remaining / 1000}s remaining)",
                cooldownRemainingMs = remaining
            )
        }

        // Check 4: Hourly limit
        val interventionsThisHour = interventionPreferences.getInterventionCountLastHour()
        if (interventionsThisHour >= MAX_INTERVENTIONS_PER_HOUR) {
            return RateLimitResult(
                allowed = false,
                reason = "Hourly limit reached ($interventionsThisHour/$MAX_INTERVENTIONS_PER_HOUR)",
                cooldownRemainingMs = getTimeUntilNextHour()
            )
        }

        // Check 5: Daily limit
        val interventionsToday = interventionPreferences.getInterventionCountToday()
        if (interventionsToday >= MAX_INTERVENTIONS_PER_DAY) {
            return RateLimitResult(
                allowed = false,
                reason = "Daily limit reached ($interventionsToday/$MAX_INTERVENTIONS_PER_DAY)",
                cooldownRemainingMs = getTimeUntilMidnight()
            )
        }

        // All checks passed
        ErrorLogger.info(
            "Rate limit checks passed for ${interventionType.name} - session: ${sessionDurationMs / 1000}s, " +
                    "hourly: $interventionsThisHour/$MAX_INTERVENTIONS_PER_HOUR, " +
                    "daily: $interventionsToday/$MAX_INTERVENTIONS_PER_DAY",
            context = "InterventionRateLimiter.canShowIntervention"
        )
        return RateLimitResult(allowed = true, reason = "Rate limit checks passed")
    }

    /**
     * Record that an intervention was shown
     * Updates timestamps and counters
     */
    fun recordIntervention(interventionType: InterventionType) {
        val now = System.currentTimeMillis()

        interventionPreferences.setLastInterventionTime(now)

        when (interventionType) {
            InterventionType.REMINDER -> interventionPreferences.setLastReminderTime(now)
            InterventionType.TIMER -> interventionPreferences.setLastTimerTime(now)
        }

        interventionPreferences.incrementInterventionCount()

        ErrorLogger.info(
            "Recorded ${interventionType.name} intervention at $now",
            context = "InterventionRateLimiter.recordIntervention"
        )
    }

    /**
     * Increase cooldown multiplier when user repeatedly dismisses
     * Escalates: 1.0x → 1.5x → 2.25x → 3.0x (max)
     */
    fun escalateCooldown() {
        val current = interventionPreferences.getCooldownMultiplier()
        val escalated = (current * 1.5f).coerceAtMost(3.0f)
        interventionPreferences.setCooldownMultiplier(escalated)

        ErrorLogger.info(
            "Cooldown escalated: ${current}x → ${escalated}x",
            context = "InterventionRateLimiter.escalateCooldown"
        )
    }

    /**
     * Reset cooldown multiplier when user engages positively
     * (e.g., chooses "Go Back" instead of dismissing)
     */
    fun resetCooldown() {
        val current = interventionPreferences.getCooldownMultiplier()
        interventionPreferences.setCooldownMultiplier(1.0f)

        ErrorLogger.info(
            "Cooldown reset: ${current}x → 1.0x (positive engagement)",
            context = "InterventionRateLimiter.resetCooldown"
        )
    }

    /**
     * Get milliseconds until the next hour starts
     */
    private fun getTimeUntilNextHour(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis - System.currentTimeMillis()
    }

    /**
     * Get milliseconds until midnight (next day)
     */
    private fun getTimeUntilMidnight(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis - System.currentTimeMillis()
    }
}
