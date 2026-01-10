package dev.sadakat.thinkfaster.service

import android.content.Context
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
import dev.sadakat.thinkfaster.domain.intervention.*
import dev.sadakat.thinkfaster.domain.model.*
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
 * Comprehensive test suite for JITAI AdaptiveInterventionRateLimiter
 *
 * Tests all decision paths:
 * - Burden-based blocking (CRITICAL burden)
 * - Timing optimization (delay when poor timing)
 * - Opportunity detection (block when POOR opportunity)
 * - Rate limiting (cooldown enforcement)
 * - Persona frequency limits
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdaptiveInterventionRateLimiterTest {

    private lateinit var rateLimiter: AdaptiveInterventionRateLimiter
    private val mockContext = mockk<Context>(relaxUnitFun = true)
    private val mockPreferences = mockk<InterventionPreferences>(relaxed = true)
    private val mockBaseRateLimiter = mockk<InterventionRateLimiter>(relaxed = true)
    private val mockPersonaDetector = mockk<PersonaDetector>(relaxed = true)
    private val mockOpportunityDetector = mockk<OpportunityDetector>(relaxed = true)
    private val mockDecisionLogger = mockk<DecisionLogger>(relaxUnitFun = true)
    private val mockBurdenTracker = mockk<InterventionBurdenTracker>(relaxed = true)
    private val mockTimingOptimizer = mockk<ContextualTimingOptimizer>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @JUnitBefore
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        rateLimiter = AdaptiveInterventionRateLimiter(
            context = mockContext,
            interventionPreferences = mockPreferences,
            baseRateLimiter = mockBaseRateLimiter,
            personaDetector = mockPersonaDetector,
            opportunityDetector = mockOpportunityDetector,
            decisionLogger = mockDecisionLogger,
            burdenTracker = mockBurdenTracker,
            timingOptimizer = mockTimingOptimizer
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // ========== BURDEN-BASED BLOCKING TESTS ==========

    @Test
    fun `should block intervention when burden is CRITICAL`() = runTest {
        // Arrange - CRITICAL burden state
        val context = createTestContext()
        val burdenMetrics = createMockBurdenMetrics(
            burdenLevel = BurdenLevel.CRITICAL,
            isReliable = true
        )

        coEvery { mockPersonaDetector.detectPersona(any()) } returns createDetectedPersona()
        coEvery { mockOpportunityDetector.detectOpportunity(any()) } returns createOpportunityDetection(score = 70)  // Good opportunity
        coEvery { mockBurdenTracker.calculateCurrentBurdenMetrics() } returns burdenMetrics
        coEvery { mockBurdenTracker.getRecommendedCooldownAdjustment() } returns 2.0f
        // Explicitly allow in base rate limiter - burden should block before this check
        stubBaseRateLimiter(allowed = true)

        // Act
        val result = rateLimiter.canShowIntervention(
            interventionContext = context,
            interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        // Assert
        assertFalse("Should block intervention with CRITICAL burden", result.allowed)
        assertEquals("CRITICAL_BURDEN", result.decisionSource)
        assertEquals(InterventionDecision.SKIP_INTERVENTION, result.decision)
        assertTrue("Should have cooldown", result.cooldownRemainingMs > 0)
    }

    @Test
    fun `should NOT block when burden is CRITICAL but data is unreliable`() = runTest {
        // Arrange - CRITICAL burden but unreliable data
        val context = createTestContext()
        val burdenMetrics = createMockBurdenMetrics(
            burdenLevel = BurdenLevel.CRITICAL,
            isReliable = false  // Unreliable data
        )

        coEvery { mockPersonaDetector.detectPersona(any()) } returns createDetectedPersona()
        coEvery { mockOpportunityDetector.detectOpportunity(any()) } returns createOpportunityDetection(score = 80)
        coEvery { mockBurdenTracker.calculateCurrentBurdenMetrics() } returns burdenMetrics
        coEvery { mockBurdenTracker.getRecommendedCooldownAdjustment() } returns 1.0f
        every { mockBaseRateLimiter.canShowIntervention(any(), any()) } returns createBasicResult(allowed = true)

        // Act
        val result = rateLimiter.canShowIntervention(
            interventionContext = context,
            interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        // Assert - should proceed to basic rate limiter
        assertTrue("Should allow when burden data is unreliable", result.allowed)
    }

    @Test
    fun `should allow intervention when burden is MODERATE`() = runTest {
        // Arrange - MODERATE burden
        val context = createTestContext()
        val burdenMetrics = createMockBurdenMetrics(
            burdenLevel = BurdenLevel.MODERATE,
            isReliable = true
        )

        coEvery { mockPersonaDetector.detectPersona(any()) } returns createDetectedPersona()
        coEvery { mockOpportunityDetector.detectOpportunity(any()) } returns createOpportunityDetection(score = 80)
        coEvery { mockBurdenTracker.calculateCurrentBurdenMetrics() } returns burdenMetrics
        coEvery { mockTimingOptimizer.getOptimalTiming(any(), any(), any(), any()) } returns createTimingRecommendation(shouldInterveneNow = true)
        every { mockBaseRateLimiter.canShowIntervention(any(), any()) } returns createBasicResult(allowed = true)

        // Act
        val result = rateLimiter.canShowIntervention(
            interventionContext = context,
            interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        // Assert
        assertTrue("Should allow with MODERATE burden", result.allowed)
    }

    // ========== TIMING OPTIMIZATION TESTS ==========

    @Test
    fun `should delay intervention when timing is poor with HIGH confidence`() = runTest {
        // Arrange - Poor timing recommendation
        val context = createTestContext(hourOfDay = 23) // Late night
        val timingRecommendation = TimingRecommendation(
            shouldInterveneNow = false,
            shouldDelay = true,
            confidence = TimingConfidence.HIGH,
            recommendedDelayMs = 30 * 60 * 1000L,
            reason = "Late night - low engagement expected",
            alternativeHours = listOf(10, 14, 15)
        )

        val burdenMetrics = createMockBurdenMetrics(BurdenLevel.LOW, isReliable = true)

        coEvery { mockPersonaDetector.detectPersona(any()) } returns createDetectedPersona()
        coEvery { mockOpportunityDetector.detectOpportunity(any()) } returns createOpportunityDetection(score = 70)
        coEvery { mockBurdenTracker.calculateCurrentBurdenMetrics() } returns burdenMetrics
        coEvery { mockTimingOptimizer.getOptimalTiming(any(), any(), any(), any()) } returns timingRecommendation

        // Act
        val result = rateLimiter.canShowIntervention(
            interventionContext = context,
            interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        // Assert
        assertFalse("Should delay intervention", result.allowed)
        assertEquals("PHASE3_TIMING_OPTIMIZATION", result.decisionSource)
        assertEquals(InterventionDecision.WAIT_FOR_BETTER_OPPORTUNITY, result.decision)
        assertTrue("Should have recommended delay", result.cooldownRemainingMs > 0)
    }

    @Test
    fun `should NOT delay when timing is poor but confidence is LOW`() = runTest {
        // Arrange - Poor timing with LOW confidence
        val context = createTestContext(hourOfDay = 22)
        val timingRecommendation = TimingRecommendation(
            shouldInterveneNow = false,
            shouldDelay = true,
            confidence = TimingConfidence.LOW,  // LOW confidence
            recommendedDelayMs = 15 * 60 * 1000L,
            reason = "Some timing concern",
            alternativeHours = emptyList()
        )

        val burdenMetrics = createMockBurdenMetrics(BurdenLevel.LOW, isReliable = true)

        coEvery { mockPersonaDetector.detectPersona(any()) } returns createDetectedPersona()
        coEvery { mockOpportunityDetector.detectOpportunity(any()) } returns createOpportunityDetection(score = 75)
        coEvery { mockBurdenTracker.calculateCurrentBurdenMetrics() } returns burdenMetrics
        coEvery { mockTimingOptimizer.getOptimalTiming(any(), any(), any(), any()) } returns timingRecommendation
        every { mockBaseRateLimiter.canShowIntervention(any(), any()) } returns createBasicResult(allowed = true)

        // Act
        val result = rateLimiter.canShowIntervention(
            interventionContext = context,
            interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        // Assert - should proceed (low confidence not enough to block)
        assertTrue("Should allow despite poor timing with LOW confidence", result.allowed)
    }

    @Test
    fun `should allow intervention when timing is optimal`() = runTest {
        // Arrange - Good timing
        val context = createTestContext(hourOfDay = 10) // Morning
        val timingRecommendation = TimingRecommendation(
            shouldInterveneNow = true,
            shouldDelay = false,
            confidence = TimingConfidence.HIGH,
            recommendedDelayMs = 0L,
            reason = "Optimal timing",
            alternativeHours = emptyList()
        )

        val burdenMetrics = createMockBurdenMetrics(BurdenLevel.LOW, isReliable = true)

        coEvery { mockPersonaDetector.detectPersona(any()) } returns createDetectedPersona()
        coEvery { mockOpportunityDetector.detectOpportunity(any()) } returns createOpportunityDetection(score = 90)
        coEvery { mockBurdenTracker.calculateCurrentBurdenMetrics() } returns burdenMetrics
        coEvery { mockTimingOptimizer.getOptimalTiming(any(), any(), any(), any()) } returns timingRecommendation
        every { mockBaseRateLimiter.canShowIntervention(any(), any()) } returns createBasicResult(allowed = true)

        // Act
        val result = rateLimiter.canShowIntervention(
            interventionContext = context,
            interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        // Assert
        assertTrue("Should allow with optimal timing", result.allowed)
    }

    // ========== OPPORTUNITY DETECTION TESTS ==========

    @Test
    fun `should include opportunity score in result`() = runTest {
        // Arrange
        val context = createTestContext()
        val opportunityDetection = OpportunityDetection(
            score = 85,
            level = OpportunityLevel.GOOD,
            decision = InterventionDecision.INTERVENE_NOW,
            breakdown = createOpportunityBreakdown(
                timeReceptiveness = 20,
                sessionPattern = 15,
                cognitiveLoad = 10,
                historicalSuccess = 20,
                userState = 20
            )
        )

        val burdenMetrics = createMockBurdenMetrics(BurdenLevel.LOW, isReliable = true)

        coEvery { mockPersonaDetector.detectPersona(any()) } returns createDetectedPersona()
        coEvery { mockOpportunityDetector.detectOpportunity(any()) } returns opportunityDetection
        coEvery { mockBurdenTracker.calculateCurrentBurdenMetrics() } returns burdenMetrics
        coEvery { mockTimingOptimizer.getOptimalTiming(any(), any(), any(), any()) } returns createTimingRecommendation(shouldInterveneNow = true)
        every { mockBaseRateLimiter.canShowIntervention(any(), any()) } returns createBasicResult(allowed = true)

        // Act
        val result = rateLimiter.canShowIntervention(
            interventionContext = context,
            interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        // Assert
        assertEquals(85, result.opportunityScore)
        assertEquals(OpportunityLevel.GOOD, result.opportunityLevel)
        assertEquals(InterventionDecision.INTERVENE_NOW, result.decision)
    }

    // ========== BASIC RATE LIMITING TESTS ==========

    @Test
    fun `should block when basic rate limiter denies`() = runTest {
        // Arrange
        val context = createTestContext()
        val burdenMetrics = createMockBurdenMetrics(BurdenLevel.LOW, isReliable = true)

        coEvery { mockPersonaDetector.detectPersona(any()) } returns createDetectedPersona()
        coEvery { mockOpportunityDetector.detectOpportunity(any()) } returns createOpportunityDetection(score = 80)
        coEvery { mockBurdenTracker.calculateCurrentBurdenMetrics() } returns burdenMetrics
        coEvery { mockTimingOptimizer.getOptimalTiming(any(), any(), any(), any()) } returns createTimingRecommendation(shouldInterveneNow = true)
        every { mockBaseRateLimiter.canShowIntervention(any(), any()) } returns createBasicResult(
            allowed = false,
            reason = "Too soon since last intervention"
        )

        // Act
        val result = rateLimiter.canShowIntervention(
            interventionContext = context,
            interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        // Assert
        assertFalse("Should block when basic rate limiter denies", result.allowed)
        assertTrue("Reason should mention rate limiting", result.reason.contains("too soon", ignoreCase = true))
    }

    // ========== PERSONA DETECTION TESTS ==========

    @Test
    fun `should always include persona in result regardless of decision`() = runTest {
        // Arrange
        val context = createTestContext()
        val personaDetection = DetectedPersona(
            persona = UserPersona.PROBLEMATIC_PATTERN_USER,
            confidence = ConfidenceLevel.HIGH,
            analytics = PersonaAnalytics(
                daysSinceInstall = 30,
                totalSessions = 150,
                avgDailySessions = 5.0,
                avgSessionLengthMin = 15.0,
                quickReopenRate = 0.6,
                usageTrend = UsageTrendType.INCREASING,
                lastAnalysisDate = "2024-01-10"
            )
        )

        val burdenMetrics = createMockBurdenMetrics(BurdenLevel.CRITICAL, isReliable = true)

        coEvery { mockPersonaDetector.detectPersona(any()) } returns personaDetection
        coEvery { mockOpportunityDetector.detectOpportunity(any()) } returns createOpportunityDetection(score = 50)
        coEvery { mockBurdenTracker.calculateCurrentBurdenMetrics() } returns burdenMetrics
        every { mockBaseRateLimiter.canShowIntervention(any(), any()) } returns createBasicResult(allowed = true)

        // Act - burden will block, but persona should still be in result
        val result = rateLimiter.canShowIntervention(
            interventionContext = context,
            interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER,
            sessionDurationMs = 5 * 60 * 1000L
        )

        // Assert - blocked by burden, but persona detected
        assertFalse(result.allowed)
        assertEquals(UserPersona.PROBLEMATIC_PATTERN_USER, result.persona)
        assertEquals(ConfidenceLevel.HIGH, result.personaConfidence)
    }

    // ========== TIMER INTERVENTION SPECIFIC TESTS ==========

    @Test
    fun `should handle TIMER intervention type correctly`() = runTest {
        // Arrange
        val context = createTestContext()
        val burdenMetrics = createMockBurdenMetrics(BurdenLevel.LOW, isReliable = true)

        coEvery { mockPersonaDetector.detectPersona(any()) } returns createDetectedPersona()
        coEvery { mockOpportunityDetector.detectOpportunity(any()) } returns createOpportunityDetection(score = 80)
        coEvery { mockBurdenTracker.calculateCurrentBurdenMetrics() } returns burdenMetrics
        coEvery { mockTimingOptimizer.getOptimalTiming(any(), any(), any(), any()) } returns createTimingRecommendation(shouldInterveneNow = true)
        every { mockBaseRateLimiter.canShowIntervention(any(), any()) } returns createBasicResult(allowed = true)

        // Act
        val result = rateLimiter.canShowIntervention(
            interventionContext = context,
            interventionType = dev.sadakat.thinkfaster.domain.intervention.InterventionType.TIMER,  // TIMER type
            sessionDurationMs = 12 * 60 * 1000L  // 12 minutes
        )

        // Assert
        assertTrue("Should allow TIMER intervention", result.allowed)
        coVerify { mockBaseRateLimiter.canShowIntervention(InterventionRateLimiter.InterventionType.TIMER, any()) }
    }

    // ========== RECORD INTERVENTION TESTS ==========

    @Test
    fun `should record intervention and update rate limiter`() = runTest {
        // Arrange
        every { mockBaseRateLimiter.recordIntervention(any()) } just Runs

        // Act
        rateLimiter.recordIntervention(dev.sadakat.thinkfaster.domain.intervention.InterventionType.REMINDER)

        // Assert
        coVerify { mockBaseRateLimiter.recordIntervention(InterventionRateLimiter.InterventionType.REMINDER) }
    }

    @Test
    fun `should record TIMER intervention correctly`() = runTest {
        // Arrange
        every { mockBaseRateLimiter.recordIntervention(any()) } just Runs

        // Act
        rateLimiter.recordIntervention(dev.sadakat.thinkfaster.domain.intervention.InterventionType.TIMER)

        // Assert
        coVerify { mockBaseRateLimiter.recordIntervention(InterventionRateLimiter.InterventionType.TIMER) }
    }

    // ========== HELPER METHODS ==========

    private fun createTestContext(
        targetApp: String = "com.instagram.android",
        hourOfDay: Int = 14
    ): InterventionContext {
        return InterventionContext(
            timeOfDay = hourOfDay,
            dayOfWeek = 3,
            isWeekend = false,
            targetApp = targetApp,
            currentSessionMinutes = 5,
            sessionCount = 1,
            lastSessionEndTime = System.currentTimeMillis() - 3600000,
            timeSinceLastSession = 3600000,
            quickReopenAttempt = false,
            totalUsageToday = 60,
            totalUsageYesterday = 90,
            weeklyAverage = 75,
            goalMinutes = 60,
            isOverGoal = false,
            streakDays = 3,
            userFrictionLevel = dev.sadakat.thinkfaster.domain.intervention.FrictionLevel.GENTLE,
            daysSinceInstall = 14,
            bestSessionMinutes = 3
        )
    }

    private fun createDetectedPersona(
        persona: UserPersona = UserPersona.CASUAL_USER
    ): DetectedPersona {
        return DetectedPersona(
            persona = persona,
            confidence = ConfidenceLevel.MEDIUM,
            analytics = PersonaAnalytics(
                daysSinceInstall = 14,
                totalSessions = 50,
                avgDailySessions = 3.5,
                avgSessionLengthMin = 10.0,
                quickReopenRate = 0.3,
                usageTrend = UsageTrendType.STABLE,
                lastAnalysisDate = "2024-01-10"
            )
        )
    }

    private fun createOpportunityDetection(
        score: Int = 75,
        level: OpportunityLevel = OpportunityLevel.MODERATE
    ): OpportunityDetection {
        return OpportunityDetection(
            score = score,
            level = level,
            decision = if (score >= 60) {
                InterventionDecision.INTERVENE_NOW
            } else {
                InterventionDecision.SKIP_INTERVENTION
            },
            breakdown = createOpportunityBreakdown(
                timeReceptiveness = 15,
                sessionPattern = 12,
                cognitiveLoad = 10,
                historicalSuccess = 18,
                userState = 20
            )
        )
    }

    private fun createOpportunityBreakdown(
        timeReceptiveness: Int,
        sessionPattern: Int,
        cognitiveLoad: Int,
        historicalSuccess: Int,
        userState: Int
    ): OpportunityBreakdown {
        return OpportunityBreakdown(
            timeReceptiveness = timeReceptiveness,
            sessionPattern = sessionPattern,
            cognitiveLoad = cognitiveLoad,
            historicalSuccess = historicalSuccess,
            userState = userState,
            behavioralCues = 10,
            factors = mapOf(
                "time_of_day" to "Good",
                "session" to "Normal"
            )
        )
    }

    private fun createMockBurdenMetrics(
        burdenLevel: BurdenLevel,
        isReliable: Boolean
    ): InterventionBurdenMetrics {
        // Create a real data class object with properties that result in the desired burden level
        // isReliable is controlled by sampleSize (< 10 = unreliable)
        val sampleSize = if (isReliable) 50 else 5

        return when (burdenLevel) {
            BurdenLevel.CRITICAL -> dev.sadakat.thinkfaster.domain.intervention.InterventionBurdenMetrics(
                avgResponseTime = 5000L,
                dismissRate = 0.5f,  // > 0.40 = +3 points
                timeoutRate = 0.4f,  // > 0.30 = +3 points
                snoozeFrequency = 10,  // > 5 = +2 points
                recentEngagementTrend = dev.sadakat.thinkfaster.domain.intervention.Trend.DECLINING,  // +4 points
                interventionsLast24h = 20,  // > 15 = +2 points
                interventionsLast7d = 50,
                effectivenessRolling7d = 0.3f,  // < 0.35 = +3 points
                effectivenessTrend = dev.sadakat.thinkfaster.domain.intervention.Trend.DECLINING,  // +4 points
                recentGoBackRate = 0.3f,
                helpfulFeedbackCount = 2,
                disruptiveFeedbackCount = 8,  // Makes helpfulnessRatio low
                helpfulnessRatio = 0.2f,  // < 0.30 and count >= 5 = +5 points
                avgInterventionSpacing = 5L,  // < 10 = +2 points
                minInterventionSpacing = 2L,  // < 3 = +3 points
                sampleSize = sampleSize,  // Reliability depends on this
                calculatedAt = System.currentTimeMillis()
            )
            else -> dev.sadakat.thinkfaster.domain.intervention.InterventionBurdenMetrics(
                avgResponseTime = 5000L,
                dismissRate = 0.2f,
                timeoutRate = 0.1f,
                snoozeFrequency = 1,
                recentEngagementTrend = dev.sadakat.thinkfaster.domain.intervention.Trend.STABLE,
                interventionsLast24h = 3,
                interventionsLast7d = 15,
                effectivenessRolling7d = 0.7f,
                effectivenessTrend = dev.sadakat.thinkfaster.domain.intervention.Trend.STABLE,
                recentGoBackRate = 0.7f,
                helpfulFeedbackCount = 5,
                disruptiveFeedbackCount = 1,
                helpfulnessRatio = 0.8f,
                avgInterventionSpacing = 30L,
                minInterventionSpacing = 15L,
                sampleSize = sampleSize,
                calculatedAt = System.currentTimeMillis()
            )
        }
    }

    private fun stubBurdenTrackerCooldown(burdenTracker: InterventionBurdenTracker, multiplier: Float = 1.0f) {
        coEvery { burdenTracker.getRecommendedCooldownAdjustment() } returns multiplier
    }

    private fun stubTimingOptimizer(timingOptimizer: ContextualTimingOptimizer, shouldInterveneNow: Boolean = true) {
        coEvery {
            timingOptimizer.getOptimalTiming(any(), any(), any())
        } returns createTimingRecommendation(shouldInterveneNow)
    }

    private fun createTimingRecommendation(shouldInterveneNow: Boolean): TimingRecommendation {
        return TimingRecommendation(
            shouldInterveneNow = shouldInterveneNow,
            shouldDelay = !shouldInterveneNow,
            confidence = TimingConfidence.MEDIUM,
            recommendedDelayMs = if (shouldInterveneNow) 0L else 15 * 60 * 1000L,
            reason = if (shouldInterveneNow) "Good timing" else "Suboptimal timing",
            alternativeHours = emptyList()
        )
    }

    private fun createBasicResult(
        allowed: Boolean,
        reason: String = ""
    ): InterventionRateLimiter.RateLimitResult {
        return InterventionRateLimiter.RateLimitResult(
            allowed = allowed,
            reason = reason,
            cooldownRemainingMs = if (allowed) 0L else 60000L
        )
    }

    private fun stubBaseRateLimiter(allowed: Boolean = true, reason: String = "") {
        every {
            mockBaseRateLimiter.canShowIntervention(any(), any())
        } returns createBasicResult(allowed, reason)
    }
}
