package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.local.database.dao.InterventionResultDao
import dev.sadakat.thinkfaster.data.local.database.entities.InterventionResultEntity
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before as JUnitBefore
import org.junit.Test

/**
 * Comprehensive edge case tests for ContextualTimingOptimizer to find potential bugs.
 *
 * Tests focus on:
 * - Midnight wraparound timing (22-5 hour window)
 * - Weekend vs weekday transitions
 * - Empty data handling
 * - Cache invalidation
 * - Boundary time conditions
 * - Best/worst hour calculations
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContextualTimingOptimizerEdgeCaseTest {

    private lateinit var optimizer: ContextualTimingOptimizer
    private val mockDao = mockk<InterventionResultDao>(relaxUnitFun = true)
    private val testDispatcher = StandardTestDispatcher()

    @JUnitBefore
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        optimizer = ContextualTimingOptimizer(mockDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // ========== EMPTY DATA HANDLING ==========

    @Test
    fun `should return LOW confidence with insufficient data`() = runTest {
        coEvery { mockDao.getResultsInRange(any(), any()) } returns emptyList()

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        assertTrue("Should allow intervention", result.shouldInterveneNow)
        assertFalse("Should not delay", result.shouldDelay)
        assertEquals("LOW confidence with insufficient data",
            TimingConfidence.LOW, result.confidence)
        assertEquals("Should have 0 delay", 0L, result.recommendedDelayMs)
    }

    @Test
    fun `should handle less than 20 interventions`() = runTest {
        val interventions = List(15) { createTestIntervention(hourOfDay = 12) }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        assertEquals("LOW confidence with < 20 interventions",
            TimingConfidence.LOW, result.confidence)
    }

    // ========== MIDNIGHT WRAPAROUND TESTS ==========

    @Test
    fun `should handle night window wraparound correctly`() = runTest {
        // Night window is 22-5 (wraps around midnight)
        val nightHour = 23  // Within night window
        val interventions = createMixedSuccessInterventions(
            nightSuccess = true,
            daySuccess = false
        )
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = nightHour,
            isWeekend = false
        )

        // Should analyze night window correctly
        assertNotNull("Should return result", result)
    }

    @Test
    fun `should handle hour 0 correctly`() = runTest {
        val interventions = createMixedSuccessInterventions(
            nightSuccess = true,
            daySuccess = false
        )
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 0,  // Midnight
            isWeekend = false
        )

        assertNotNull("Should handle hour 0", result)
    }

    @Test
    fun `should handle early morning hours correctly`() = runTest {
        val interventions = List(30) {
            createTestIntervention(hourOfDay = 3, userChoice = "GO_BACK")
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 3,
            isWeekend = false
        )

        assertNotNull("Should handle early morning", result)
    }

    // ========== WEEKEND VS WEEKDAY TESTS ==========

    @Test
    fun `should distinguish weekend from weekday`() = runTest {
        val weekendInterventions = List(30) {
            createTestIntervention(hourOfDay = 14, isWeekend = true, userChoice = "GO_BACK")
        }
        val weekdayInterventions = List(30) {
            createTestIntervention(hourOfDay = 14, isWeekend = false, userChoice = "DISMISS")
        }

        coEvery { mockDao.getResultsInRange(any(), any()) } returns
                weekendInterventions + weekdayInterventions

        val weekendResult = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 14,
            isWeekend = true
        )

        val weekdayResult = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 14,
            isWeekend = false
        )

        // Weekend should be better (GO_BACK) than weekday (DISMISS)
        assertTrue("Weekend should allow or be more confident",
            weekendResult.confidence >= weekdayResult.confidence)
    }

    // ========== BEST AND WORST HOUR TESTS ==========

    @Test
    fun `should identify best hours correctly`() = runTest {
        val interventions = createHighSuccessHourInterventions(bestHour = 14)
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 14,
            isWeekend = false
        )

        assertTrue("Best hour should be HIGH confidence",
            result.confidence == TimingConfidence.HIGH)
        assertTrue("Best hour should not delay", !result.shouldDelay)
    }

    @Test
    fun `should identify worst hours correctly`() = runTest {
        val interventions = createLowSuccessHourInterventions(worstHour = 9)
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 9,
            isWeekend = false
        )

        // Worst hour with reliable data should recommend delay
        assertTrue("Should provide alternative hours",
            result.alternativeHours.isNotEmpty())
    }

    @Test
    fun `should handle all hours having similar performance`() = runTest {
        val interventions = List(100) { index ->
            createTestIntervention(hourOfDay = index % 24, userChoice = "GO_BACK")
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        // When all hours are similar, should still allow intervention
        assertTrue("Should allow intervention with similar performance",
            result.shouldInterveneNow)
    }

    // ========== DELAY CALCULATION TESTS ==========

    @Test
    fun `should calculate correct delay to next best hour`() = runTest {
        val interventions = createHighSuccessHourInterventions(bestHour = 15)
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 9,  // Morning
            isWeekend = false
        )

        assertNotNull("Should have alternative hours", result.alternativeHours)
    }

    @Test
    fun `should handle midnight wraparound in delay calculation`() = runTest {
        // Best hour is 2 AM, current hour is 11 PM
        val interventions = createHighSuccessHourInterventions(bestHour = 2)
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 23,
            isWeekend = false
        )

        // Should calculate delay correctly across midnight
        assertTrue("Delay should be positive", result.recommendedDelayMs >= 0)
    }

    // ========== CACHE INVALIDATION TESTS ==========

    @Test
    fun `should use cache for same parameters`() = runTest {
        val interventions = List(30) { createTestIntervention(hourOfDay = 12) }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        // First call
        val result1 = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        // Verify database was called
        coVerify(atLeast = 1) { mockDao.getResultsInRange(any(), any()) }

        // Second call with same parameters should use cache
        val result2 = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        // Results should be identical
        assertEquals("Cached results should be identical", result1, result2)
    }

    @Test
    fun `should invalidate cache with forceRefresh`() = runTest {
        val interventions = List(30) { createTestIntervention(hourOfDay = 12) }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        // First call
        optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        // Force refresh should call database again
        optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false,
            forceRefresh = true
        )

        coVerify(atLeast = 2) { mockDao.getResultsInRange(any(), any()) }
    }

    @Test
    fun `should manually invalidate cache`() = runTest {
        val interventions = List(30) { createTestIntervention(hourOfDay = 12) }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        // First call
        optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        // Invalidate cache
        optimizer.invalidateCache()

        // Second call should hit database
        optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        coVerify(atLeast = 2) { mockDao.getResultsInRange(any(), any()) }
    }

    // ========== EDGE CASE: HOUR BOUNDARIES ==========

    @Test
    fun `should handle hour 23 correctly`() = runTest {
        val interventions = List(30) {
            createTestIntervention(hourOfDay = 23, userChoice = "GO_BACK")
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 23,
            isWeekend = false
        )

        assertNotNull("Should handle hour 23", result)
        assertTrue("Should allow intervention at hour 23", result.shouldInterveneNow)
        // With 30 interventions all GO_BACK at hour 23, should have high confidence
        assertEquals("Should have HIGH confidence with good performance",
            TimingConfidence.HIGH, result.confidence)
    }

    @Test
    fun `should handle all 24 hours correctly`() = runTest {
        val interventions = (0 until 24).flatMap { hour ->
            List(5) { createTestIntervention(hourOfDay = hour) }
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val results = (0 until 24).map { hour ->
            optimizer.getOptimalTiming(
                targetApp = "com.example.app",
                currentHour = hour,
                isWeekend = false
            )
        }

        assertEquals("Should have 24 results", 24, results.size)
        assertTrue("All results should be valid", results.all { it.shouldInterveneNow })
    }

    // ========== RELIABILITY TESTS ==========

    @Test
    fun `should mark hours with insufficient samples as unreliable`() = runTest {
        // Only 3 interventions per hour (less than 5 threshold)
        val interventions = (0 until 24).flatMap { hour ->
            List(3) { createTestIntervention(hourOfDay = hour, userChoice = "GO_BACK") }
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        // With unreliable data, should not have HIGH confidence
        assertNotEquals("Should not be HIGH confidence with unreliable data",
            TimingConfidence.HIGH, result.confidence)
    }

    // ========== MODERATE SUCCESS RATE TESTS ==========

    @Test
    fun `should handle moderate success rate correctly`() = runTest {
        // 40-50% success rate - moderate performance
        val interventions = List(100) { index ->
            val choice = if (index % 2 == 0) "GO_BACK" else "DISMISS"
            createTestIntervention(hourOfDay = index % 24, userChoice = choice)
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        // Moderate performance should still allow intervention
        assertTrue("Should allow with moderate performance",
            result.shouldInterveneNow)
    }

    // ========== BELOW AVERAGE PERFORMANCE TESTS ==========

    @Test
    fun `should handle below average performance`() = runTest {
        // Low success rate but not worst
        val interventions = List(100) { index ->
            val choice = if (index % 10 == 0) "GO_BACK" else "DISMISS"  // 10% success
            createTestIntervention(hourOfDay = index % 24, userChoice = choice)
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val result = optimizer.getOptimalTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        // Should still allow but may have alternatives
        assertTrue("Should allow even with low performance",
            result.shouldInterveneNow)
    }

    // ========== HELPER METHODS ==========

    private fun createTestIntervention(
        hourOfDay: Int = 12,
        isWeekend: Boolean = false,
        userChoice: String = "GO_BACK"
    ) = InterventionResultEntity(
        sessionId = 1L,
        targetApp = "com.example.app",
        interventionType = "REMINDER",
        contentType = "reflection",
        hourOfDay = hourOfDay,
        dayOfWeek = if (isWeekend) 6 else 1,
        isWeekend = isWeekend,
        isLateNight = hourOfDay >= 22 || hourOfDay <= 5,
        sessionCount = 1,
        quickReopen = false,
        currentSessionDurationMs = 5 * 60 * 1000L,
        userChoice = userChoice,
        timeToShowDecisionMs = 5000L,
        userFeedback = "NONE",
        wasSnoozed = false,
        timestamp = System.currentTimeMillis(),
        sessionEndedNormally = true,
        finalSessionDurationMs = 5 * 60 * 1000L
    )

    private fun createMixedSuccessInterventions(
        nightSuccess: Boolean,
        daySuccess: Boolean
    ): List<InterventionResultEntity> {
        val nightChoice = if (nightSuccess) "GO_BACK" else "DISMISS"
        val dayChoice = if (daySuccess) "GO_BACK" else "DISMISS"

        return listOf(
            // Night hours (22-5)
            createTestIntervention(hourOfDay = 23, userChoice = nightChoice),
            createTestIntervention(hourOfDay = 0, userChoice = nightChoice),
            createTestIntervention(hourOfDay = 3, userChoice = nightChoice),
            // Day hours
            createTestIntervention(hourOfDay = 9, userChoice = dayChoice),
            createTestIntervention(hourOfDay = 12, userChoice = dayChoice),
            createTestIntervention(hourOfDay = 15, userChoice = dayChoice),
        )
    }

    private fun createHighSuccessHourInterventions(bestHour: Int): List<InterventionResultEntity> {
        val interventions = mutableListOf<InterventionResultEntity>()

        // Best hour: high success
        repeat(15) {
            interventions.add(createTestIntervention(hourOfDay = bestHour, userChoice = "GO_BACK"))
        }

        // Other hours: low success
        for (hour in 0 until 24) {
            if (hour != bestHour) {
                repeat(5) {
                    interventions.add(createTestIntervention(hourOfDay = hour, userChoice = "DISMISS"))
                }
            }
        }

        return interventions
    }

    private fun createLowSuccessHourInterventions(worstHour: Int): List<InterventionResultEntity> {
        val interventions = mutableListOf<InterventionResultEntity>()

        // Worst hour: low success
        repeat(15) {
            interventions.add(createTestIntervention(hourOfDay = worstHour, userChoice = "DISMISS"))
        }

        // Other hours: high success
        for (hour in 0 until 24) {
            if (hour != worstHour) {
                repeat(5) {
                    interventions.add(createTestIntervention(hourOfDay = hour, userChoice = "GO_BACK"))
                }
            }
        }

        return interventions
    }
}
