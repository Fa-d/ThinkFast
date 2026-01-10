package dev.sadakat.thinkfaster.service

import android.content.Context
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before as JUnitBefore
import org.junit.Test

/**
 * Comprehensive tests for ContextDetector to find bugs.
 */
class ContextDetectorTest {

    private lateinit var contextDetector: ContextDetector
    private val mockContext = mockk<Context>(relaxUnitFun = true)

    @JUnitBefore
    fun setup() {
        contextDetector = ContextDetector(mockContext)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should return true for base intervention check`() {
        val result = contextDetector.shouldShowIntervention()
        assertTrue("Should allow intervention", result.shouldShowIntervention)
        assertNull("Reason should be null when allowed", result.reason)
    }

    @Test
    fun `should allow reminder with 2 minutes session`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.REMINDER,
            sessionDurationMs = 2 * 60 * 1000L
        )
        assertTrue("Should allow with 2 min session", result.shouldShowIntervention)
    }

    @Test
    fun `should allow reminder with 3 minutes session`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.REMINDER,
            sessionDurationMs = 3 * 60 * 1000L
        )
        assertTrue("Should allow with 3 min session", result.shouldShowIntervention)
    }

    @Test
    fun `should block reminder with 90 seconds session`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.REMINDER,
            sessionDurationMs = 90 * 1000L
        )
        assertFalse("Should block with 90 sec session", result.shouldShowIntervention)
        assertTrue("Reason mentions too short",
            result.reason?.contains("too short") == true)
    }

    @Test
    fun `should block reminder with 1 minute 59 seconds session`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.REMINDER,
            sessionDurationMs = 119 * 1000L
        )
        assertFalse("Should block with 1:59 session", result.shouldShowIntervention)
    }

    @Test
    fun `should allow with zero session duration`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.REMINDER,
            sessionDurationMs = 0L
        )
        assertTrue("Zero duration should be allowed (no minimum)", result.shouldShowIntervention)
    }

    @Test
    fun `should allow with negative session duration`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.REMINDER,
            sessionDurationMs = -1000L
        )
        assertTrue("Negative duration should be allowed (treated as no minimum)", result.shouldShowIntervention)
    }

    @Test
    fun `should allow reminder with 5 minutes session`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )
        assertTrue("Should allow reminder", result.shouldShowIntervention)
    }

    @Test
    fun `should block reminder with 1 minute session`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.REMINDER,
            sessionDurationMs = 60 * 1000L
        )
        assertFalse("Should block short reminder", result.shouldShowIntervention)
    }

    @Test
    fun `should allow timer with 5 minutes session`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.TIMER,
            sessionDurationMs = 5 * 60 * 1000L
        )
        assertTrue("Should allow timer with 5 min session", result.shouldShowIntervention)
    }

    @Test
    fun `should allow timer with 10 minutes session`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.TIMER,
            sessionDurationMs = 10 * 60 * 1000L
        )
        assertTrue("Should allow timer with 10 min session", result.shouldShowIntervention)
    }

    @Test
    fun `should block timer with 4 minutes session`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.TIMER,
            sessionDurationMs = 4 * 60 * 1000L
        )
        assertFalse("Should block timer with 4 min", result.shouldShowIntervention)
        assertTrue("Reason mentions longer session",
            result.reason?.contains("longer session") == true)
    }

    @Test
    fun `should block timer with 4 min 59 sec session`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.TIMER,
            sessionDurationMs = 299 * 1000L
        )
        assertFalse("Should block timer with 4:59 session", result.shouldShowIntervention)
    }

    @Test
    fun `should block timer with 3 minutes session`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.TIMER,
            sessionDurationMs = 3 * 60 * 1000L
        )
        assertFalse("Timer requires 5 min or more", result.shouldShowIntervention)
        assertTrue("Reason mentions timer requirement",
            result.reason?.contains("Timer requires") == true)
    }

    @Test
    fun `should allow reminder but block timer for 3 min session`() {
        val reminderResult = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.REMINDER,
            sessionDurationMs = 3 * 60 * 1000L
        )

        val timerResult = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.TIMER,
            sessionDurationMs = 3 * 60 * 1000L
        )

        assertTrue("Reminder should be allowed", reminderResult.shouldShowIntervention)
        assertFalse("Timer should be blocked", timerResult.shouldShowIntervention)
    }

    @Test
    fun `should handle very large session duration`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.REMINDER,
            sessionDurationMs = Long.MAX_VALUE
        )
        assertTrue("Should handle large duration", result.shouldShowIntervention)
    }

    @Test
    fun `should allow reminder at exactly 2 minutes`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.REMINDER,
            sessionDurationMs = 2 * 60 * 1000L
        )
        assertTrue("Reminder allowed at exactly 2 min", result.shouldShowIntervention)
    }

    @Test
    fun `should allow timer at exactly 5 minutes`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.TIMER,
            sessionDurationMs = 5 * 60 * 1000L
        )
        assertTrue("Timer allowed at exactly 5 min", result.shouldShowIntervention)
    }

    @Test
    fun `should provide meaningful reason for blocking`() {
        val result = contextDetector.shouldShowInterventionType(
            interventionType = ContextDetector.InterventionType.TIMER,
            sessionDurationMs = 2 * 60 * 1000L
        )
        assertNotNull("Reason should not be null", result.reason)
        assertTrue("Reason contains session info",
            result.reason?.contains("120s") == true || result.reason?.contains("2 min") == true)
    }
}
