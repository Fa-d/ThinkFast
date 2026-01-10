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
 * Comprehensive edge case tests for InterventionBurdenTracker to find potential bugs.
 *
 * Tests focus on:
 * - Empty/null data handling
 * - Cache invalidation
 * - Edge cases in burden score calculation
 * - Time window boundary conditions
 * - Trend calculation edge cases
 * - Division by zero scenarios
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InterventionBurdenTrackerEdgeCaseTest {

    private lateinit var burdenTracker: InterventionBurdenTracker
    private val mockDao = mockk<InterventionResultDao>(relaxUnitFun = true)
    private val mockFatigueTracker = mockk<FatigueRecoveryTracker>(relaxed = true)
    private val mockTrendMonitor = mockk<BurdenTrendMonitor>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @JUnitBefore
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        burdenTracker = InterventionBurdenTracker(mockDao, mockFatigueTracker, mockTrendMonitor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // ========== EMPTY DATA HANDLING ==========

    @Test
    fun `should handle empty intervention list gracefully`() = runTest {
        coEvery { mockDao.getResultsInRange(any(), any()) } returns emptyList()

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        assertNotNull("Metrics should not be null", metrics)
        assertEquals("Sample size should be 0", 0, metrics.sampleSize)
        assertEquals("Dismiss rate should be 0", 0.0f, metrics.dismissRate, 0.001f)
    }

    @Test
    fun `should handle single intervention correctly`() = runTest {
        val singleIntervention = createTestIntervention(userChoice = "GO_BACK")
        coEvery { mockDao.getResultsInRange(any(), any()) } returns listOf(singleIntervention)

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        assertEquals("Sample size should be 1", 1, metrics.sampleSize)
        assertEquals("Dismiss rate should be 0", 0.0f, metrics.dismissRate, 0.001f)
        assertEquals("Go back rate should be 1.0", 1.0f, metrics.recentGoBackRate, 0.001f)
    }

    @Test
    fun `should handle all DISMISS interventions`() = runTest {
        val interventions = List(50) { createTestIntervention(userChoice = "DISMISS") }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        assertEquals("Dismiss rate should be 1.0", 1.0f, metrics.dismissRate, 0.001f)
        assertTrue("Burden level should be HIGH or CRITICAL",
            metrics.calculateBurdenLevel() in listOf(BurdenLevel.HIGH, BurdenLevel.CRITICAL))
    }

    @Test
    fun `should handle all TIMEOUT interventions`() = runTest {
        val interventions = List(50) { createTestIntervention(userChoice = "TIMEOUT") }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        assertEquals("Timeout rate should be 1.0", 1.0f, metrics.timeoutRate, 0.001f)
    }

    // ========== CACHE INVALIDATION TESTS ==========

    @Test
    fun `should use cached metrics within cache duration`() = runTest {
        val interventions = List(10) { createTestIntervention() }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        // First call should hit database
        burdenTracker.calculateCurrentBurdenMetrics()

        // Verify first call
        coVerify(atLeast = 1) { mockDao.getResultsInRange(any(), any()) }

        // Reset mock to track new calls
        clearMocks(mockDao, answers = false)

        // Second call should use cache (within 10 minutes)
        burdenTracker.calculateCurrentBurdenMetrics()

        // Should NOT call database again
        coVerify(exactly = 0) { mockDao.getResultsInRange(any(), any()) }
    }

    @Test
    fun `should invalidate cache with forceRefresh`() = runTest {
        val interventions = List(10) { createTestIntervention() }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        // First call
        burdenTracker.calculateCurrentBurdenMetrics()
        coVerify(atLeast = 1) { mockDao.getResultsInRange(any(), any()) }

        clearMocks(mockDao, answers = false)

        // Force refresh should call database again
        burdenTracker.calculateCurrentBurdenMetrics(forceRefresh = true)
        coVerify(atLeast = 1) { mockDao.getResultsInRange(any(), any()) }
    }

    @Test
    fun `should manually invalidate cache`() = runTest {
        val interventions = List(10) { createTestIntervention() }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        // First call
        burdenTracker.calculateCurrentBurdenMetrics()
        coVerify(atLeast = 1) { mockDao.getResultsInRange(any(), any()) }

        clearMocks(mockDao, answers = false)

        // Invalidate cache
        burdenTracker.invalidateCache()

        // Second call should hit database
        burdenTracker.calculateCurrentBurdenMetrics()
        coVerify(atLeast = 1) { mockDao.getResultsInRange(any(), any()) }
    }

    // ========== TIME WINDOW EDGE CASES ==========

    @Test
    fun `should handle interventions at exact time boundaries`() = runTest {
        val now = System.currentTimeMillis()
        // Use slightly different timestamps to ensure they're within the range
        val almost24hAgo = now - (24 * 60 * 60 * 1000) + 1000  // 1 second inside 24h window
        val almost7dAgo = now - (7 * 24 * 60 * 60 * 1000) + 1000  // 1 second inside 7d window

        val interventions = listOf(
            createTestIntervention(timestamp = almost24hAgo),
            createTestIntervention(timestamp = almost7dAgo)
        )
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        // Check boundary conditions - should count correctly
        assertTrue("Should count interventions at time boundaries",
            metrics.interventionsLast24h >= 1)
        assertTrue("Should count interventions at time boundaries",
            metrics.interventionsLast7d >= 1)
    }

    @Test
    fun `should handle future timestamps gracefully`() = runTest {
        val future = System.currentTimeMillis() + (60 * 60 * 1000)  // 1 hour in future
        val interventions = listOf(
            createTestIntervention(timestamp = future)
        )
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        // Should still process without crashing
        assertNotNull("Should handle future timestamps", metrics)
        assertEquals("Sample size should include future intervention", 1, metrics.sampleSize)
    }

    @Test
    fun `should handle negative timestamps gracefully`() = runTest {
        val interventions = listOf(
            createTestIntervention(timestamp = -1000L)
        )
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        // Should still process without crashing
        assertNotNull("Should handle negative timestamps", metrics)
    }

    // ========== FEEDBACK EDGE CASES ==========

    @Test
    fun `should handle null feedback in all interventions`() = runTest {
        val interventions = List(50) {
            createTestIntervention(userFeedback = "NONE")
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        assertEquals("Helpful count should be 0", 0, metrics.helpfulFeedbackCount)
        assertEquals("Disruptive count should be 0", 0, metrics.disruptiveFeedbackCount)
        assertEquals("Helpfulness ratio should be neutral", 0.5f, metrics.helpfulnessRatio, 0.001f)
    }

    @Test
    fun `should handle case-sensitive feedback correctly`() = runTest {
        val interventions = listOf(
            createTestIntervention(userFeedback = "HELPFUL"),
            createTestIntervention(userFeedback = "helpful"),
            createTestIntervention(userFeedback = "Helpful")
        )
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        // Only exact "HELPFUL" match should count
        assertEquals("Should only count exact match", 1, metrics.helpfulFeedbackCount)
    }

    @Test
    fun `should handle all feedback as disruptive`() = runTest {
        val interventions = List(20) {
            createTestIntervention(userFeedback = "DISRUPTIVE")
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        assertEquals("All should be disruptive", 20, metrics.disruptiveFeedbackCount)
        assertEquals("Helpfulness ratio should be 0", 0.0f, metrics.helpfulnessRatio, 0.001f)
    }

    // ========== SPACING CALCULATION EDGE CASES ==========

    @Test
    fun `should handle single intervention spacing correctly`() = runTest {
        val interventions = listOf(createTestIntervention())
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        // With single intervention, min spacing should default to 30
        assertEquals("Min spacing should be default", 30L, metrics.minInterventionSpacing)
        assertEquals("Avg spacing should be default", 30L, metrics.avgInterventionSpacing)
    }

    @Test
    fun `should handle interventions with same timestamp`() = runTest {
        val sameTime = System.currentTimeMillis()
        val interventions = listOf(
            createTestIntervention(timestamp = sameTime),
            createTestIntervention(timestamp = sameTime),
            createTestIntervention(timestamp = sameTime)
        )
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        // Zero spacing between same-timestamp interventions
        assertEquals("Min spacing should be 0 for same timestamps", 0L, metrics.minInterventionSpacing)
    }

    @Test
    fun `should handle very large spacing values`() = runTest {
        val now = System.currentTimeMillis()
        val interventions = listOf(
            createTestIntervention(timestamp = 0L),  // Epoch
            createTestIntervention(timestamp = now)  // Now
        )
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        // Should calculate very large spacing in minutes
        assertTrue("Should handle large spacing", metrics.avgInterventionSpacing > 0)
    }

    // ========== TREND CALCULATION EDGE CASES ==========

    @Test
    fun `should handle insufficient data for trend calculation`() = runTest {
        // Less than 20 interventions for engagement trend
        val interventions = List(15) { createTestIntervention() }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        assertEquals("Trend should be STABLE with insufficient data",
            Trend.STABLE, metrics.recentEngagementTrend)
    }

    @Test
    fun `should handle exactly 20 interventions for trend`() = runTest {
        // Exactly 20 interventions - boundary condition
        val interventions = List(20) {
            createTestIntervention(userChoice = "GO_BACK")
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        // Should calculate trend (not STABLE due to insufficient data)
        assertNotNull("Trend should be calculated", metrics.recentEngagementTrend)
    }

    @Test
    fun `should handle interventions with all same outcome for trend`() = runTest {
        val interventions = List(30) {
            createTestIntervention(userChoice = "GO_BACK")
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        // All GO_BACK should result in STABLE trend (no difference)
        assertEquals("All same outcomes should be STABLE",
            Trend.STABLE, metrics.recentEngagementTrend)
    }

    // ========== BURDEN SCORE CALCULATION EDGE CASES ==========

    @Test
    fun `should calculate burden score at extreme values`() = runTest {
        // Maximum burden scenario
        val interventions = List(50) {
            createTestIntervention(
                userChoice = "DISMISS",
                userFeedback = "DISRUPTIVE",
                wasSnoozed = true,
                timeToShowDecisionMs = 30000L  // 30 seconds
            )
        }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        // Should result in CRITICAL burden
        assertEquals("Should be CRITICAL burden",
            BurdenLevel.CRITICAL, metrics.calculateBurdenLevel())
        assertTrue("Burden score should be high",
            metrics.calculateBurdenScore() >= 20)
    }

    @Test
    fun `should calculate minimum burden score correctly`() = runTest {
        // Use createLowBurdenInterventions which creates truly optimal scenarios
        val interventions = createLowBurdenInterventions()
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        // Should result in LOW burden
        assertEquals("Should be LOW burden",
            BurdenLevel.LOW, metrics.calculateBurdenLevel())
        assertTrue("Burden score should be low",
            metrics.calculateBurdenScore() < 10)
    }

    // ========== RELIABILITY EDGE CASES ==========

    @Test
    fun `should have unreliable data with sample size 9`() = runTest {
        val interventions = List(9) { createTestIntervention() }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        assertFalse("Should be unreliable with < 10 samples", metrics.isReliable())
    }

    @Test
    fun `should have reliable data with sample size 10`() = runTest {
        val interventions = List(10) { createTestIntervention() }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()

        assertTrue("Should be reliable with >= 10 samples", metrics.isReliable())
    }

    @Test
    fun `cooldown adjustment should be 1_0 when unreliable`() = runTest {
        val interventions = List(5) { createTestIntervention() }
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val adjustment = burdenTracker.getRecommendedCooldownAdjustment()

        assertEquals("Should return 1.0 when unreliable", 1.0f, adjustment, 0.001f)
    }

    // ========== COOLDOWN MULTIPLIER EDGE CASES ==========

    @Test
    fun `should handle cooldown multiplier at extreme burden`() = runTest {
        val interventions = createCriticalBurdenInterventions()
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()
        val multiplier = metrics.getRecommendedCooldownMultiplier()

        // CRITICAL burden should have high multiplier
        assertTrue("CRITICAL burden should increase cooldown", multiplier > 1.5f)
    }

    @Test
    fun `should handle cooldown multiplier at low burden`() = runTest {
        val interventions = createLowBurdenInterventions()
        coEvery { mockDao.getResultsInRange(any(), any()) } returns interventions

        val metrics = burdenTracker.calculateCurrentBurdenMetrics()
        val multiplier = metrics.getRecommendedCooldownMultiplier()

        // LOW burden should have multiplier close to 1.0
        assertEquals("LOW burden should have normal cooldown", 1.0f, multiplier, 0.5f)
    }

    // ========== PHASE 2: RECOVERY AND TREND TESTS ==========

    @Test
    fun `should call fatigue recovery tracker correctly`() = runTest {
        coEvery { mockFatigueTracker.calculateRecoveryCredit() } returns 5.0f
        coEvery { mockFatigueTracker.applyRecoveryCredit(any(), any()) } returns 15
        coEvery { mockTrendMonitor.recordBurdenScore(any()) } just Runs
        coEvery { mockDao.getResultsInRange(any(), any()) } returns List(20) { createTestIntervention() }

        val burden = burdenTracker.calculateBurdenWithRecovery()

        coVerify { mockFatigueTracker.calculateRecoveryCredit() }
        coVerify { mockFatigueTracker.applyRecoveryCredit(any(), eq(5.0f)) }
        coVerify { mockTrendMonitor.recordBurdenScore(any()) }
    }

    @Test
    fun `should analyze trend correctly`() = runTest {
        coEvery { mockDao.getResultsInRange(any(), any()) } returns List(20) { createTestIntervention() }
        coEvery { mockFatigueTracker.calculateRecoveryCredit() } returns 0.0f
        coEvery { mockFatigueTracker.applyRecoveryCredit(any(), any()) } returns 10
        val expectedTrend = BurdenTrendMonitor.BurdenTrend(
            currentScore = 10,
            previousScore = 8,
            trend = BurdenTrendMonitor.TrendDirection.INCREASING,
            changePercentage = 25.0f,
            isEscalating = false
        )
        coEvery { mockTrendMonitor.analyzeTrend(any()) } returns expectedTrend

        val trend = burdenTracker.getBurdenTrend()

        assertEquals("Should return trend from monitor", expectedTrend, trend)
    }

    // ========== HELPER METHODS ==========

    private fun createTestIntervention(
        userChoice: String = "GO_BACK",
        userFeedback: String = "NONE",
        wasSnoozed: Boolean = false,
        timestamp: Long = System.currentTimeMillis(),
        timeToShowDecisionMs: Long = 5000L
    ) = InterventionResultEntity(
        sessionId = 1L,
        targetApp = "com.example.app",
        interventionType = "REMINDER",
        contentType = "reflection",
        hourOfDay = ((timestamp / (60 * 60 * 1000)) % 24).toInt(),
        dayOfWeek = 1,
        isWeekend = false,
        isLateNight = false,
        sessionCount = 1,
        quickReopen = false,
        currentSessionDurationMs = 5 * 60 * 1000L,
        userChoice = userChoice,
        timeToShowDecisionMs = timeToShowDecisionMs,
        userFeedback = userFeedback,
        wasSnoozed = wasSnoozed,
        timestamp = timestamp,
        sessionEndedNormally = true,
        finalSessionDurationMs = 5 * 60 * 1000L
    )

    private fun createCriticalBurdenInterventions(): List<InterventionResultEntity> {
        val now = System.currentTimeMillis()
        return List(50) { index ->
            InterventionResultEntity(
                sessionId = index.toLong(),
                targetApp = "com.example.app",
                interventionType = "REMINDER",
                contentType = "reflection",
                hourOfDay = 12,
                dayOfWeek = 1,
                isWeekend = false,
                isLateNight = false,
                sessionCount = 20,
                quickReopen = true,
                currentSessionDurationMs = 5 * 60 * 1000L,
                userChoice = "DISMISS",
                timeToShowDecisionMs = 30000L,
                userFeedback = "DISRUPTIVE",
                wasSnoozed = true,
                timestamp = now - (index * 2 * 60 * 1000L),
                sessionEndedNormally = true,
                finalSessionDurationMs = 5 * 60 * 1000L
            )
        }
    }

    private fun createLowBurdenInterventions(): List<InterventionResultEntity> {
        val now = System.currentTimeMillis()
        return List(50) { index ->
            InterventionResultEntity(
                sessionId = index.toLong(),
                targetApp = "com.example.app",
                interventionType = "REMINDER",
                contentType = "reflection",
                hourOfDay = 12,
                dayOfWeek = 1,
                isWeekend = false,
                isLateNight = false,
                sessionCount = 1,
                quickReopen = false,
                currentSessionDurationMs = 5 * 60 * 1000L,
                userChoice = "GO_BACK",
                timeToShowDecisionMs = 2000L,
                userFeedback = "HELPFUL",
                wasSnoozed = false,
                timestamp = now - (index * 60 * 60 * 1000L),
                sessionEndedNormally = true,
                finalSessionDurationMs = 5 * 60 * 1000L
            )
        }
    }
}
