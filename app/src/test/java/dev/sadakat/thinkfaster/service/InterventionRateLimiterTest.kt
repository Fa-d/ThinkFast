package dev.sadakat.thinkfaster.service

import android.content.Context
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before as JUnitBefore
import org.junit.Test

/**
 * Comprehensive tests for InterventionRateLimiter to find bugs.
 */
class InterventionRateLimiterTest {

    private lateinit var rateLimiter: InterventionRateLimiter
    private val mockContext = mockk<Context>(relaxUnitFun = true)
    private val mockPreferences = mockk<InterventionPreferences>(relaxed = true)

    @JUnitBefore
    fun setup() {
        rateLimiter = InterventionRateLimiter(mockContext, mockPreferences)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should block intervention with session duration less than 2 minutes`() {
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 90 * 1000L
        )

        assertFalse("Should block short session", result.allowed)
        assertTrue("Reason mentions too short",
            result.reason.contains("too short"))
        assertEquals("Should have 30 sec remaining", 30 * 1000L, result.cooldownRemainingMs)
    }

    @Test
    fun `should allow intervention with session duration equal to 2 minutes`() {
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 2 * 60 * 1000L
        )

        assertTrue("Should allow with 2 min session", result.allowed)
    }

    @Test
    fun `should block during global cooldown`() {
        val now = System.currentTimeMillis()
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns now - 2 * 60 * 1000L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertFalse("Should block during global cooldown", result.allowed)
        assertTrue("Reason mentions global cooldown",
            result.reason.contains("Global cooldown"))
        // Account for timing variation - should be approximately 3 minutes
        assertTrue("Should have ~3 min remaining",
            result.cooldownRemainingMs in (175 * 1000L)..(185 * 1000L))
    }

    @Test
    fun `should respect cooldown multiplier`() {
        val now = System.currentTimeMillis()
        every { mockPreferences.getCooldownMultiplier() } returns 2.0f
        every { mockPreferences.getLastInterventionTime() } returns now - 4 * 60 * 1000L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertFalse("Should block with 2x multiplier", result.allowed)
        assertTrue("Reason mentions multiplier",
            result.reason.contains("2.0x multiplier") || result.reason.contains("multiplier"))
    }

    @Test
    fun `should allow when global cooldown has passed`() {
        val now = System.currentTimeMillis()
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns now - 6 * 60 * 1000L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertTrue("Should allow after 5 min global cooldown", result.allowed)
    }

    @Test
    fun `should block reminder during reminder cooldown`() {
        val now = System.currentTimeMillis()
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastReminderTime() } returns now - 5 * 60 * 1000L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertFalse("Should block during reminder cooldown", result.allowed)
        assertTrue("Reason mentions REMINDER cooldown",
            result.reason.contains("REMINDER cooldown"))
    }

    @Test
    fun `should allow timer when reminder is on cooldown`() {
        val now = System.currentTimeMillis()
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastReminderTime() } returns now - 5 * 60 * 1000L
        every { mockPreferences.getLastTimerTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.TIMER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertTrue("Timer should be allowed when reminder is on cooldown", result.allowed)
    }

    @Test
    fun `should block timer during timer cooldown`() {
        val now = System.currentTimeMillis()
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastTimerTime() } returns now - 10 * 60 * 1000L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.TIMER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertFalse("Should block during timer cooldown", result.allowed)
        assertTrue("Reason mentions TIMER cooldown",
            result.reason.contains("TIMER cooldown"))
    }

    @Test
    fun `should block when hourly limit reached`() {
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 4
        every { mockPreferences.getInterventionCountToday() } returns 5

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertFalse("Should block at hourly limit", result.allowed)
        assertTrue("Reason mentions hourly limit",
            result.reason.contains("Hourly limit"))
        assertTrue("Should provide time until next hour",
            result.cooldownRemainingMs > 0)
    }

    @Test
    fun `should allow when below hourly limit`() {
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 3
        every { mockPreferences.getInterventionCountToday() } returns 5

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertTrue("Should allow below hourly limit", result.allowed)
    }

    @Test
    fun `should block when daily limit reached`() {
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 2
        every { mockPreferences.getInterventionCountToday() } returns 20

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertFalse("Should block at daily limit", result.allowed)
        assertTrue("Reason mentions daily limit",
            result.reason.contains("Daily limit"))
        assertTrue("Should provide time until midnight",
            result.cooldownRemainingMs > 0)
    }

    @Test
    fun `should allow when below daily limit`() {
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 2
        every { mockPreferences.getInterventionCountToday() } returns 19

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertTrue("Should allow below daily limit", result.allowed)
    }

    @Test
    fun `should record reminder intervention correctly`() {
        rateLimiter.recordIntervention(InterventionRateLimiter.InterventionType.REMINDER)

        verify { mockPreferences.setLastInterventionTime(any()) }
        verify { mockPreferences.setLastReminderTime(any()) }
        verify { mockPreferences.incrementInterventionCount() }
    }

    @Test
    fun `should record timer intervention correctly`() {
        rateLimiter.recordIntervention(InterventionRateLimiter.InterventionType.TIMER)

        verify { mockPreferences.setLastInterventionTime(any()) }
        verify { mockPreferences.setLastTimerTime(any()) }
        verify { mockPreferences.incrementInterventionCount() }
    }

    @Test
    fun `should escalate cooldown by 1_5x`() {
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f

        rateLimiter.escalateCooldown()

        verify { mockPreferences.setCooldownMultiplier(1.5f) }
    }

    @Test
    fun `should not exceed 3_0x max cooldown multiplier`() {
        every { mockPreferences.getCooldownMultiplier() } returns 2.5f

        rateLimiter.escalateCooldown()

        verify { mockPreferences.setCooldownMultiplier(3.0f) }
    }

    @Test
    fun `should reset cooldown to 1_0x`() {
        every { mockPreferences.getCooldownMultiplier() } returns 2.0f

        rateLimiter.resetCooldown()

        verify { mockPreferences.setCooldownMultiplier(1.0f) }
    }

    @Test
    fun `should handle extreme cooldown multiplier`() {
        val now = System.currentTimeMillis()
        every { mockPreferences.getCooldownMultiplier() } returns 100.0f
        every { mockPreferences.getLastInterventionTime() } returns now - 10 * 60 * 1000L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertFalse("Should block with extreme multiplier", result.allowed)
        assertTrue("Should have large remaining cooldown",
            result.cooldownRemainingMs > 400 * 60 * 1000L)
    }

    @Test
    fun `should allow at exactly global cooldown duration`() {
        val now = System.currentTimeMillis()
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns now - 5 * 60 * 1000L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertTrue("Should allow at exactly global cooldown", result.allowed)
    }

    @Test
    fun `should allow at exactly reminder cooldown duration`() {
        val now = System.currentTimeMillis()
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastReminderTime() } returns now - 10 * 60 * 1000L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertTrue("Should allow at exactly reminder cooldown", result.allowed)
    }

    @Test
    fun `should allow at exactly timer cooldown duration`() {
        val now = System.currentTimeMillis()
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastTimerTime() } returns now - 15 * 60 * 1000L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.TIMER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertTrue("Should allow at exactly timer cooldown", result.allowed)
    }

    @Test
    fun `should check session duration before global cooldown`() {
        every { mockPreferences.getCooldownMultiplier() } returns 1.0f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 30 * 1000L
        )

        assertFalse("Should block on session duration first", result.allowed)
        assertTrue("Reason mentions session duration",
            result.reason.contains("too short"))
    }

    @Test
    fun `should handle float cooldown multiplier correctly`() {
        every { mockPreferences.getCooldownMultiplier() } returns 1.5f
        every { mockPreferences.getLastInterventionTime() } returns 0L
        every { mockPreferences.getLastReminderTime() } returns 0L
        every { mockPreferences.getInterventionCountLastHour() } returns 0
        every { mockPreferences.getInterventionCountToday() } returns 0

        val result = rateLimiter.canShowIntervention(
            interventionType = InterventionRateLimiter.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        assertTrue("Should handle float multiplier", result.allowed)
    }
}
