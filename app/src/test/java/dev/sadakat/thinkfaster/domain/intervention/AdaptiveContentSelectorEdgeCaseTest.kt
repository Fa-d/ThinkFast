package dev.sadakat.thinkfaster.domain.intervention

import dev.sadakat.thinkfaster.data.local.database.dao.InterventionResultDao
import dev.sadakat.thinkfaster.data.local.database.entities.InterventionResultEntity
import dev.sadakat.thinkfaster.data.preferences.InterventionPreferences
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
 * Comprehensive edge case tests for AdaptiveContentSelector to find potential bugs.
 *
 * Tests focus on:
 * - Context-based arm exclusion logic
 * - State consistency (interventionArmMap)
 * - Recording outcomes correctly
 * - Null handling in optional dependencies
 * - Frequency multiplier calculations
 * - Content effectiveness summary
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdaptiveContentSelectorEdgeCaseTest {

    private lateinit var selector: AdaptiveContentSelector
    private val mockThompsonSampling = mockk<ThompsonSamplingEngine>(relaxed = true)
    private val mockRewardCalculator = mockk<RewardCalculator>(relaxed = true)
    private val mockDao = mockk<InterventionResultDao>(relaxed = true)
    private val mockTimingOptimizer = mockk<ContextualTimingOptimizer>(relaxed = true)
    private val mockPreferences = mockk<InterventionPreferences>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @JUnitBefore
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        selector = AdaptiveContentSelector(
            mockThompsonSampling,
            mockRewardCalculator,
            mockDao,
            null,  // timingPatternLearner
            mockTimingOptimizer
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // ========== CONTEXT-BASED EXCLUSION TESTS ==========

    @Test
    fun `should select content type successfully`() = runTest {
        val context = createTestContext()
        val expectedSelection = ArmSelection(
            armId = ThompsonSamplingEngine.ARM_REFLECTION,
            confidence = 0.8f,
            strategy = "thompson_sampling"
        )
        coEvery { mockThompsonSampling.selectArm(any()) } returns expectedSelection

        val selection = selector.selectContentType(context)

        assertNotNull("Should return selection", selection)
        assertEquals("Should match expected arm", expectedSelection.armId, selection.contentType)
    }

    @Test
    fun `should handle late night context correctly`() = runTest {
        val context = createTestContext(timeOfDay = 23)  // Late night

        val expectedSelection = ArmSelection(
            armId = ThompsonSamplingEngine.ARM_REFLECTION,
            confidence = 0.5f,
            strategy = "thompson_sampling"
        )
        coEvery { mockThompsonSampling.selectArm(any()) } returns expectedSelection

        val selection = selector.selectContentType(context)

        assertNotNull("Should handle late night", selection)
        coVerify { mockThompsonSampling.selectArm(match { excludedArms ->
            // BREATHING and GAMIFICATION should be excluded at late night
            excludedArms.contains(ThompsonSamplingEngine.ARM_BREATHING) &&
            excludedArms.contains(ThompsonSamplingEngine.ARM_GAMIFICATION)
        }) }
    }

    @Test
    fun `should handle early morning context correctly`() = runTest {
        val context = createTestContext(timeOfDay = 4)  // Early morning

        val expectedSelection = ArmSelection(
            armId = ThompsonSamplingEngine.ARM_REFLECTION,
            confidence = 0.5f,
            strategy = "thompson_sampling"
        )
        coEvery { mockThompsonSampling.selectArm(any()) } returns expectedSelection

        val selection = selector.selectContentType(context)

        assertNotNull("Should handle early morning", selection)
        coVerify { mockThompsonSampling.selectArm(match { excludedArms ->
            // USAGE_STATS and EMOTIONAL should be excluded at early morning
            excludedArms.contains(ThompsonSamplingEngine.ARM_USAGE_STATS) &&
            excludedArms.contains(ThompsonSamplingEngine.ARM_EMOTIONAL)
        }) }
    }

    @Test
    fun `should handle problematic user persona correctly`() = runTest {
        val context = createTestContext()
        val persona = dev.sadakat.thinkfaster.domain.model.UserPersona.PROBLEMATIC_PATTERN_USER

        val expectedSelection = ArmSelection(
            armId = ThompsonSamplingEngine.ARM_REFLECTION,
            confidence = 0.5f,
            strategy = "thompson_sampling"
        )
        coEvery { mockThompsonSampling.selectArm(any()) } returns expectedSelection

        val selection = selector.selectContentType(context, persona)

        assertNotNull("Should handle problematic user", selection)
        coVerify { mockThompsonSampling.selectArm(match { excludedArms ->
            // QUOTE and GAMIFICATION should be excluded for problematic user
            excludedArms.contains(ThompsonSamplingEngine.ARM_QUOTE) &&
            excludedArms.contains(ThompsonSamplingEngine.ARM_GAMIFICATION)
        }) }
    }

    @Test
    fun `should handle casual user persona correctly`() = runTest {
        val context = createTestContext()
        val persona = dev.sadakat.thinkfaster.domain.model.UserPersona.CASUAL_USER

        val expectedSelection = ArmSelection(
            armId = ThompsonSamplingEngine.ARM_REFLECTION,
            confidence = 0.5f,
            strategy = "thompson_sampling"
        )
        coEvery { mockThompsonSampling.selectArm(any()) } returns expectedSelection

        val selection = selector.selectContentType(context, persona)

        assertNotNull("Should handle casual user", selection)
        coVerify { mockThompsonSampling.selectArm(match { excludedArms ->
            // EMOTIONAL should be excluded for casual user
            excludedArms.contains(ThompsonSamplingEngine.ARM_EMOTIONAL)
        }) }
    }

    @Test
    fun `should handle new user persona correctly`() = runTest {
        val context = createTestContext()
        val persona = dev.sadakat.thinkfaster.domain.model.UserPersona.NEW_USER

        val expectedSelection = ArmSelection(
            armId = ThompsonSamplingEngine.ARM_REFLECTION,
            confidence = 0.5f,
            strategy = "thompson_sampling"
        )
        coEvery { mockThompsonSampling.selectArm(any()) } returns expectedSelection

        val selection = selector.selectContentType(context, persona)

        assertNotNull("Should handle new user", selection)
        coVerify { mockThompsonSampling.selectArm(match { excludedArms ->
            // EMOTIONAL and USAGE_STATS should be excluded for new user
            excludedArms.contains(ThompsonSamplingEngine.ARM_EMOTIONAL) &&
            excludedArms.contains(ThompsonSamplingEngine.ARM_USAGE_STATS)
        }) }
    }

    @Test
    fun `should handle first session correctly`() = runTest {
        val context = createTestContext(sessionCount = 1)  // First session

        val expectedSelection = ArmSelection(
            armId = ThompsonSamplingEngine.ARM_REFLECTION,
            confidence = 0.5f,
            strategy = "thompson_sampling"
        )
        coEvery { mockThompsonSampling.selectArm(any()) } returns expectedSelection

        val selection = selector.selectContentType(context)

        assertNotNull("Should handle first session", selection)
        coVerify { mockThompsonSampling.selectArm(match { excludedArms ->
            // USAGE_STATS should be excluded on first session
            excludedArms.contains(ThompsonSamplingEngine.ARM_USAGE_STATS)
        }) }
    }

    @Test
    fun `should handle quick reopen correctly`() = runTest {
        val context = createTestContext(quickReopenAttempt = true)

        val expectedSelection = ArmSelection(
            armId = ThompsonSamplingEngine.ARM_REFLECTION,
            confidence = 0.5f,
            strategy = "thompson_sampling"
        )
        coEvery { mockThompsonSampling.selectArm(any()) } returns expectedSelection

        val selection = selector.selectContentType(context)

        assertNotNull("Should handle quick reopen", selection)
        coVerify { mockThompsonSampling.selectArm(match { excludedArms ->
            // QUOTE and GAMIFICATION should be excluded on quick reopen
            excludedArms.contains(ThompsonSamplingEngine.ARM_QUOTE) &&
            excludedArms.contains(ThompsonSamplingEngine.ARM_GAMIFICATION)
        }) }
    }

    @Test
    fun `should handle poor opportunity correctly`() = runTest {
        val context = createTestContext()
        val opportunity = OpportunityDetection(
            score = 20,
            level = dev.sadakat.thinkfaster.domain.model.OpportunityLevel.POOR,
            decision = dev.sadakat.thinkfaster.domain.model.InterventionDecision.SKIP_INTERVENTION,
            breakdown = createTestBreakdown()
        )

        val expectedSelection = ArmSelection(
            armId = ThompsonSamplingEngine.ARM_REFLECTION,
            confidence = 0.5f,
            strategy = "thompson_sampling"
        )
        coEvery { mockThompsonSampling.selectArm(any()) } returns expectedSelection

        val selection = selector.selectContentType(context, null, opportunity)

        assertNotNull("Should handle poor opportunity", selection)
        coVerify { mockThompsonSampling.selectArm(match { excludedArms ->
            // EMOTIONAL should be excluded with poor opportunity
            excludedArms.contains(ThompsonSamplingEngine.ARM_EMOTIONAL)
        }) }
    }

    // ========== STATE CONSISTENCY TESTS ==========

    @Test
    fun `should track intervention-arm mapping correctly`() = runTest {
        val interventionId = 123L
        val contentType = ThompsonSamplingEngine.ARM_REFLECTION

        selector.recordInterventionSelection(interventionId, contentType)

        // Verify the mapping is tracked (would need to verify through recordOutcome)
        coEvery { mockThompsonSampling.updateArm(any(), any()) } just Runs
        coEvery { mockRewardCalculator.calculateReward(any(), any(), any(), any(), any()) } returns 1.0f

        selector.recordOutcome(
            interventionId = interventionId,
            userChoice = "GO_BACK"
        )

        coVerify { mockThompsonSampling.updateArm(contentType, 1.0f) }
    }

    @Test
    fun `should handle recordOutcome without prior selection`() = runTest {
        val interventionId = 999L  // Not recorded

        // Should not crash when outcome is recorded without prior selection
        selector.recordOutcome(
            interventionId = interventionId,
            userChoice = "GO_BACK"
        )

        // Should not call updateArm since there's no mapping
        coVerify(exactly = 0) { mockThompsonSampling.updateArm(any(), any()) }
    }

    @Test
    fun `should clean up intervention-arm mapping after outcome`() = runTest {
        val interventionId = 123L
        val contentType = ThompsonSamplingEngine.ARM_REFLECTION

        selector.recordInterventionSelection(interventionId, contentType)

        coEvery { mockThompsonSampling.updateArm(any(), any()) } just Runs
        coEvery { mockRewardCalculator.calculateReward(any(), any(), any(), any(), any()) } returns 1.0f

        // First outcome
        selector.recordOutcome(interventionId, userChoice = "GO_BACK")
        coVerify(atLeast = 1) { mockThompsonSampling.updateArm(contentType, any()) }

        // Reset mock
        clearMocks(mockThompsonSampling)

        // Second outcome with same ID should not call updateArm (mapping was cleaned up)
        selector.recordOutcome(interventionId, userChoice = "DISMISS")
        coVerify(exactly = 0) { mockThompsonSampling.updateArm(any(), any()) }
    }

    // ========== NULL HANDLING TESTS ==========

    @Test
    fun `should handle null persona correctly`() = runTest {
        val context = createTestContext()

        val expectedSelection = ArmSelection(
            armId = ThompsonSamplingEngine.ARM_REFLECTION,
            confidence = 0.5f,
            strategy = "thompson_sampling"
        )
        coEvery { mockThompsonSampling.selectArm(any()) } returns expectedSelection

        val selection = selector.selectContentType(context, null, null)

        assertNotNull("Should return selection with null persona", selection)
    }

    @Test
    fun `should handle null opportunity correctly`() = runTest {
        val context = createTestContext()

        val expectedSelection = ArmSelection(
            armId = ThompsonSamplingEngine.ARM_REFLECTION,
            confidence = 0.5f,
            strategy = "thompson_sampling"
        )
        coEvery { mockThompsonSampling.selectArm(any()) } returns expectedSelection

        val selection = selector.selectContentType(context, null, null)

        assertNotNull("Should return selection with null opportunity", selection)
    }

    // ========== FREQUENCY MULTIPLIER TESTS ==========

    @Test
    fun `should return 1_0 frequency with insufficient data`() = runTest {
        coEvery { mockThompsonSampling.getAllArmStats() } returns emptyList()

        val multiplier = selector.getFrequencyMultiplier()

        assertEquals("Should return 1.0 with insufficient data", 1.0f, multiplier, 0.001f)
    }

    @Test
    fun `should return 0_8 frequency with high effectiveness`() = runTest {
        // Create stats with high success rate (70%)
        val stats = listOf(
            createArmStats(totalPulls = 100, estimatedSuccessRate = 0.7f),
            createArmStats(totalPulls = 50, estimatedSuccessRate = 0.6f)
        )
        coEvery { mockThompsonSampling.getAllArmStats() } returns stats

        val multiplier = selector.getFrequencyMultiplier()

        // High effectiveness (60%+) = 0.8x frequency (shorter cooldowns)
        assertEquals("Should return 0.8 with high effectiveness", 0.8f, multiplier, 0.001f)
    }

    @Test
    fun `should return 1_5 frequency with very low effectiveness`() = runTest {
        // Create stats with low success rate (20%)
        val stats = listOf(
            createArmStats(totalPulls = 100, estimatedSuccessRate = 0.2f),
            createArmStats(totalPulls = 50, estimatedSuccessRate = 0.25f)
        )
        coEvery { mockThompsonSampling.getAllArmStats() } returns stats

        val multiplier = selector.getFrequencyMultiplier()

        // Very low effectiveness (<30%) = 1.5x frequency (longer cooldowns)
        assertEquals("Should return 1.5 with very low effectiveness", 1.5f, multiplier, 0.001f)
    }

    // ========== CONTENT EFFECTIVENESS TESTS ==========

    @Test
    fun `should return empty effectiveness with no data`() = runTest {
        coEvery { mockThompsonSampling.getAllArmStats() } returns emptyList()

        val effectiveness = selector.getContentEffectiveness()

        assertTrue("Should be empty with no data", effectiveness.isEmpty())
    }

    @Test
    fun `should sort effectiveness by success rate descending`() = runTest {
        val stats = listOf(
            createArmStats(armId = ThompsonSamplingEngine.ARM_REFLECTION, estimatedSuccessRate = 0.8f),
            createArmStats(armId = ThompsonSamplingEngine.ARM_QUOTE, estimatedSuccessRate = 0.3f),
            createArmStats(armId = ThompsonSamplingEngine.ARM_BREATHING, estimatedSuccessRate = 0.5f)
        )
        coEvery { mockThompsonSampling.getAllArmStats() } returns stats

        val effectiveness = selector.getContentEffectiveness()

        assertEquals("Should have 3 items", 3, effectiveness.size)
        assertEquals("Should be sorted by success rate descending",
            ThompsonSamplingEngine.ARM_REFLECTION, effectiveness[0].contentType)
        assertEquals("Lowest should be last",
            ThompsonSamplingEngine.ARM_QUOTE, effectiveness[2].contentType)
    }

    // ========== RESET LEARNING TESTS ==========

    @Test
    fun `should clear arm map on reset`() = runTest {
        val interventionId = 123L
        selector.recordInterventionSelection(interventionId, ThompsonSamplingEngine.ARM_REFLECTION)

        coEvery { mockThompsonSampling.resetAllArms() } just Runs

        selector.resetLearning()

        coVerify { mockThompsonSampling.resetAllArms() }

        // After reset, recording outcome should not update arm (map was cleared)
        clearMocks(mockThompsonSampling)
        coEvery { mockThompsonSampling.updateArm(any(), any()) } just Runs
        coEvery { mockRewardCalculator.calculateReward(any(), any(), any(), any(), any()) } returns 1.0f

        selector.recordOutcome(interventionId, userChoice = "GO_BACK")
        coVerify(exactly = 0) { mockThompsonSampling.updateArm(any(), any()) }
    }

    // ========== TIMING RECOMMENDATION TESTS ==========

    @Test
    fun `should return null timing recommendation when optimizer is null`() = runTest {
        // Create selector with null timing optimizer
        val selectorNoTiming = AdaptiveContentSelector(
            mockThompsonSampling,
            mockRewardCalculator,
            mockDao,
            null,  // timingPatternLearner
            null   // contextualTimingOptimizer
        )

        val timing = selectorNoTiming.recommendTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        assertNull("Should return null when timing optimizer is null", timing)
    }

    @Test
    fun `should return timing recommendation from optimizer`() = runTest {
        val expectedTiming = TimingRecommendation(
            shouldInterveneNow = true,
            shouldDelay = false,
            recommendedDelayMs = 0L,
            reason = "Good timing",
            confidence = TimingConfidence.HIGH,
            alternativeHours = emptyList()
        )
        coEvery { mockTimingOptimizer.getOptimalTiming(any(), any(), any()) } returns expectedTiming

        val timing = selector.recommendTiming(
            targetApp = "com.example.app",
            currentHour = 12,
            isWeekend = false
        )

        assertNotNull("Should return timing recommendation", timing)
        assertEquals("Should match expected timing", expectedTiming, timing)
    }

    // ========== HELPER METHODS ==========

    private fun createTestContext(
        timeOfDay: Int = 12,
        sessionCount: Int = 2,
        quickReopenAttempt: Boolean = false
    ) = InterventionContext(
        timeOfDay = timeOfDay,
        dayOfWeek = 2,
        isWeekend = false,
        targetApp = "com.example.app",
        currentSessionMinutes = 5,
        sessionCount = sessionCount,
        lastSessionEndTime = System.currentTimeMillis() - 3600000,
        timeSinceLastSession = 3600000,
        quickReopenAttempt = quickReopenAttempt,
        totalUsageToday = 60L,
        totalUsageYesterday = 60L,
        weeklyAverage = 60L,
        goalMinutes = null,
        isOverGoal = false,
        streakDays = 1,
        userFrictionLevel = FrictionLevel.GENTLE,
        daysSinceInstall = 5,
        bestSessionMinutes = 5
    )

    private fun createTestBreakdown() = OpportunityBreakdown(
        timeReceptiveness = 10,
        sessionPattern = 10,
        cognitiveLoad = 10,
        historicalSuccess = 10,
        userState = 10,
        behavioralCues = 10,
        factors = emptyMap()
    )

    private fun createArmStats(
        armId: String = ThompsonSamplingEngine.ARM_REFLECTION,
        totalPulls: Int = 50,
        estimatedSuccessRate: Float = 0.5f
    ) = dev.sadakat.thinkfaster.domain.intervention.ArmStats(
        armId = armId,
        alpha = 10.0,
        beta = 10.0,
        totalPulls = totalPulls,
        estimatedSuccessRate = estimatedSuccessRate,
        uncertainty = 0.1f
    )
}
