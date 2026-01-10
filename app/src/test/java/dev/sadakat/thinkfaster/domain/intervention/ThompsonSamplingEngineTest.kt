package dev.sadakat.thinkfaster.domain.intervention

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
 * Test suite for Thompson Sampling Reinforcement Learning Engine
 *
 * Tests:
 * - Arm selection with sufficient data
 * - Arm selection with no data (exploration)
 * - Reward updates
 * - Confidence interval calculations
 * - Data sufficiency checks
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThompsonSamplingEngineTest {

    private lateinit var engine: ThompsonSamplingEngine
    private val mockPreferences = mockk<InterventionPreferences>(relaxUnitFun = true)
    private val testDispatcher = StandardTestDispatcher()

    // Use a real map to persist state between calls
    private val stateStorage = mutableMapOf<String, String>()

    @JUnitBefore
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock preferences to actually persist state
        every { mockPreferences.getString(any(), any()) } answers {
            val key = firstArg<String>()
            stateStorage[key] ?: secondArg()
        }
        every { mockPreferences.setString(any(), any()) } answers {
            val key = firstArg<String>()
            val value = secondArg<String>()
            stateStorage[key] = value
        }

        engine = ThompsonSamplingEngine(mockPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stateStorage.clear()
        clearAllMocks()
    }

    // ========== ARM SELECTION TESTS ==========

    @Test
    fun `should select arm and return selection details`() = runTest {
        // Act - select arm (should work with no data)
        val selection = engine.selectArm()

        // Assert
        assertNotNull("Should return a selection", selection)
        assertNotNull("Should have an arm ID", selection.armId)
        assertTrue("Confidence should be between 0 and 1", selection.confidence >= 0f && selection.confidence <= 1f)
        assertEquals("Should be Thompson Sampling strategy", "thompson_sampling", selection.strategy)
    }

    @Test
    fun `should exclude specified arms from selection`() = runTest {
        // Arrange - exclude some arms
        val excludedArms = setOf(
            ThompsonSamplingEngine.ARM_BREATHING,
            ThompsonSamplingEngine.ARM_GAMIFICATION
        )

        // Act
        val selection = engine.selectArm(excludedArms)

        // Assert
        assertNotNull(selection)
        assertFalse("Should not select excluded arm", excludedArms.contains(selection.armId))
    }

    @Test
    fun `should handle all arms excluded gracefully`() = runTest {
        // Arrange - exclude ALL arms
        val allArms = setOf(
            ThompsonSamplingEngine.ARM_REFLECTION,
            ThompsonSamplingEngine.ARM_TIME_ALTERNATIVE,
            ThompsonSamplingEngine.ARM_BREATHING,
            ThompsonSamplingEngine.ARM_USAGE_STATS,
            ThompsonSamplingEngine.ARM_EMOTIONAL,
            ThompsonSamplingEngine.ARM_QUOTE,
            ThompsonSamplingEngine.ARM_GAMIFICATION,
            ThompsonSamplingEngine.ARM_ACTIVITY
        )

        // Act - should not crash, should return fallback
        val selection = engine.selectArm(allArms)

        // Assert - should return fallback (reflection)
        assertNotNull("Should handle gracefully", selection)
        assertEquals(ThompsonSamplingEngine.ARM_REFLECTION, selection.armId)
    }

    // ========== REWARD UPDATE TESTS ==========

    @Test
    fun `should update arm with positive reward`() = runTest {
        // Arrange
        val armId = ThompsonSamplingEngine.ARM_REFLECTION
        engine.updateArm(armId, reward = 1.0f)
        val initialStats = engine.getArmStats(armId)

        // Act - update with success (reward = 1.0)
        engine.updateArm(armId, reward = 1.0f)

        // Assert
        val updatedStats = engine.getArmStats(armId)
        assertNotNull("Stats should exist", updatedStats)
        assertTrue("Alpha should increase", updatedStats!!.alpha > initialStats!!.alpha)
        assertEquals("Beta should stay same", initialStats.beta, updatedStats.beta, 0.001)
    }

    @Test
    fun `should update arm with negative reward`() = runTest {
        // Arrange
        val armId = ThompsonSamplingEngine.ARM_QUOTE
        engine.updateArm(armId, reward = 1.0f)
        val initialStats = engine.getArmStats(armId)

        // Act - update with failure (reward = 0.0)
        engine.updateArm(armId, reward = 0.0f)

        // Assert
        val updatedStats = engine.getArmStats(armId)
        assertNotNull("Stats should exist", updatedStats)
        assertEquals("Alpha should stay same", initialStats!!.alpha, updatedStats!!.alpha, 0.001)
        assertTrue("Beta should increase", updatedStats.beta > initialStats.beta)
    }

    @Test
    fun `should update arm with partial reward`() = runTest {
        // Arrange
        val armId = ThompsonSamplingEngine.ARM_ACTIVITY
        engine.updateArm(armId, reward = 1.0f)
        val initialStats = engine.getArmStats(armId)

        // Act - update with partial success (reward = 0.5)
        engine.updateArm(armId, reward = 0.5f)

        // Assert - with Beta distribution, reward adds to alpha, (1-reward) adds to beta
        val updatedStats = engine.getArmStats(armId)
        assertNotNull("Stats should exist", updatedStats)
        assertTrue("Alpha should increase", updatedStats!!.alpha > initialStats!!.alpha)
        assertTrue("Beta should increase", updatedStats.beta > initialStats.beta)
    }

    @Test
    fun `should track total pulls correctly`() = runTest {
        // Arrange
        val armId = ThompsonSamplingEngine.ARM_EMOTIONAL
        engine.updateArm(armId, reward = 1.0f)
        val initialStats = engine.getArmStats(armId)

        // Act - update multiple times
        engine.updateArm(armId, reward = 1.0f)
        engine.updateArm(armId, reward = 0.0f)
        engine.updateArm(armId, reward = 0.5f)

        // Assert
        val updatedStats = engine.getArmStats(armId)
        assertEquals("Should track additional pulls", initialStats!!.totalPulls + 3, updatedStats!!.totalPulls)
    }

    // ========== LEARNING BEHAVIOR TESTS ==========

    @Test
    fun `should prefer successful arms after learning`() = runTest {
        // Arrange - make one arm very successful
        val successfulArm = ThompsonSamplingEngine.ARM_REFLECTION
        val poorArm = ThompsonSamplingEngine.ARM_QUOTE

        // Train the successful arm
        repeat(20) {
            engine.updateArm(successfulArm, reward = 1.0f)
        }

        // Train the poor arm
        repeat(20) {
            engine.updateArm(poorArm, reward = 0.0f)
        }

        // Act - select multiple times and count
        val selections = mutableMapOf<String, Int>()
        repeat(100) {
            val selection = engine.selectArm(setOf(
                ThompsonSamplingEngine.ARM_TIME_ALTERNATIVE,
                ThompsonSamplingEngine.ARM_BREATHING,
                ThompsonSamplingEngine.ARM_USAGE_STATS,
                ThompsonSamplingEngine.ARM_EMOTIONAL,
                ThompsonSamplingEngine.ARM_GAMIFICATION,
                ThompsonSamplingEngine.ARM_ACTIVITY
            ))
            selections[selection.armId] = selections.getOrDefault(selection.armId, 0) + 1
        }

        // Assert - successful arm should be selected more often
        val successfulCount = selections[successfulArm] ?: 0
        val poorCount = selections[poorArm] ?: 0
        assertTrue(
            "Successful arm should be selected more often ($successfulCount vs $poorCount)",
            successfulCount > poorCount * 2  // At least 2x more
        )
    }

    @Test
    fun `should explore when all arms have similar performance`() = runTest {
        // Arrange - give all arms similar performance
        val arms = listOf(
            ThompsonSamplingEngine.ARM_REFLECTION,
            ThompsonSamplingEngine.ARM_TIME_ALTERNATIVE,
            ThompsonSamplingEngine.ARM_BREATHING
        )

        arms.forEach { arm ->
            repeat(10) {
                engine.updateArm(arm, reward = 0.5f)  // Same for all
            }
        }

        // Act - select multiple times
        val selections = mutableMapOf<String, Int>()
        repeat(100) {
            val selection = engine.selectArm(setOf(
                ThompsonSamplingEngine.ARM_USAGE_STATS,
                ThompsonSamplingEngine.ARM_EMOTIONAL,
                ThompsonSamplingEngine.ARM_QUOTE,
                ThompsonSamplingEngine.ARM_GAMIFICATION,
                ThompsonSamplingEngine.ARM_ACTIVITY
            ))
            selections[selection.armId] = selections.getOrDefault(selection.armId, 0) + 1
        }

        // Assert - selections should be somewhat distributed (not all on one arm)
        val maxCount = selections.values.maxOrNull() ?: 0
        val minCount = selections.values.minOrNull() ?: 0
        val ratio = if (minCount > 0) maxCount.toFloat() / minCount else Float.MAX_VALUE

        assertTrue("Should explore different arms (ratio: $ratio)", ratio < 5.0f)
    }

    // ========== DATA SUFFICIENCY TESTS ==========

    @Test
    fun `should report insufficient data with no pulls`() = runTest {
        // Act - check with no data
        val hasData = engine.hasSufficientData()

        // Assert
        assertFalse("Should not have sufficient data with no pulls", hasData)
    }

    @Test
    fun `should report sufficient data after pulls`() = runTest {
        // Arrange - add some pulls across arms
        repeat(30) {
            val arm = ThompsonSamplingEngine.ARM_REFLECTION
            engine.updateArm(arm, reward = 0.7f)
        }

        // Act
        val hasData = engine.hasSufficientData()

        // Assert
        assertTrue("Should have sufficient data after pulls", hasData)
    }

    // ========== STATS RETRIEVAL TESTS ==========

    @Test
    fun `should return stats for all arms`() = runTest {
        // Arrange - update all arms so they have stats
        ThompsonSamplingEngine.ALL_ARMS.forEach { armId ->
            engine.updateArm(armId, reward = 0.5f)
        }

        // Act
        val allStats = engine.getAllArmStats()

        // Assert
        assertEquals("Should have 8 arms after updates", 8, allStats.size)

        val armIds = allStats.map { it.armId }.toSet()
        assertTrue("Should have REFLECTION arm", armIds.contains(ThompsonSamplingEngine.ARM_REFLECTION))
        assertTrue("Should have QUOTE arm", armIds.contains(ThompsonSamplingEngine.ARM_QUOTE))
        assertTrue("Should have GAMIFICATION arm", armIds.contains(ThompsonSamplingEngine.ARM_GAMIFICATION))
    }

    @Test
    fun `should return null for non-existent arm`() = runTest {
        // Act
        val stats = engine.getArmStats("non_existent_arm")

        // Assert
        assertNull("Should return null for non-existent arm", stats)
    }

    @Test
    fun `should calculate estimated success rate correctly`() = runTest {
        // Arrange
        val armId = ThompsonSamplingEngine.ARM_BREATHING
        // 10 pulls: 7 success, 3 failure
        repeat(7) { engine.updateArm(armId, reward = 1.0f) }
        repeat(3) { engine.updateArm(armId, reward = 0.0f) }

        // Act
        val stats = engine.getArmStats(armId)

        // Assert - Beta(1+7, 1+3) = Beta(8, 4), mean = 8/(8+4) = 8/12 = 0.667
        val expectedRate = 8.0 / (8 + 4)
        assertNotNull("Stats should exist", stats)
        assertEquals("Estimated rate should match Beta mean", expectedRate.toFloat(), stats!!.estimatedSuccessRate, 0.01f)
    }

    @Test
    fun `should calculate uncertainty correctly`() = runTest {
        // Arrange
        val armId = ThompsonSamplingEngine.ARM_USAGE_STATS

        // Act - with no data
        val initialStats = engine.getArmStats(armId)
        val initialUncertainty = initialStats?.uncertainty ?: Float.MAX_VALUE

        // Add data
        repeat(50) { engine.updateArm(armId, reward = 0.6f) }
        val updatedStats = engine.getArmStats(armId)
        val updatedUncertainty = updatedStats?.uncertainty ?: Float.MAX_VALUE

        // Assert - more data = lower uncertainty
        assertTrue("Uncertainty should decrease with more data", updatedUncertainty < initialUncertainty)
    }

    // ========== CONFIDENCE INTERVAL TESTS ==========

    @Test
    fun `should calculate confidence interval`() = runTest {
        // Arrange
        val armId = ThompsonSamplingEngine.ARM_ACTIVITY
        repeat(20) { engine.updateArm(armId, reward = 0.7f) }

        // Act
        val stats = engine.getArmStats(armId)
        val ci = stats?.getConfidenceInterval()

        // Assert
        assertNotNull("Should have confidence interval", ci)
        assertTrue("Lower bound should be >= 0", ci!!.first >= 0f)
        assertTrue("Upper bound should be <= 1", ci.second <= 1f)
        assertTrue("Lower should be less than upper", ci.first < ci.second)
        assertTrue("Estimated rate should be within CI", stats!!.estimatedSuccessRate in ci.first..ci.second)
    }

    // ========== RESET TESTS ==========

    @Test
    fun `should reset all arms to initial state`() = runTest {
        // Arrange - add some data
        repeat(10) { engine.updateArm(ThompsonSamplingEngine.ARM_REFLECTION, reward = 1.0f) }
        repeat(5) { engine.updateArm(ThompsonSamplingEngine.ARM_QUOTE, reward = 0.0f) }

        // Act
        engine.resetAllArms()

        // Assert - after reset, stats should be back to initial
        // Note: getArmStats returns null for arms with no pulls
        val reflectionStats = engine.getArmStats(ThompsonSamplingEngine.ARM_REFLECTION)
        assertNull("Reset arm should have no stats", reflectionStats)
    }

    // ========== STRATEGY DETECTION TESTS ==========

    @Test
    fun `should have higher confidence with more data`() = runTest {
        // Arrange - reset to ensure clean state
        engine.resetAllArms()
        stateStorage.clear()

        // Make one arm very successful with lots of data
        val armId = ThompsonSamplingEngine.ARM_REFLECTION
        repeat(100) { engine.updateArm(armId, reward = 0.95f) }

        // Act
        val selection = engine.selectArm()

        // Assert - with high confidence, confidence should be >= 0.7
        assertTrue("Should have high confidence with lots of data", selection.confidence >= 0.7f)
    }

    @Test
    fun `should have low confidence with no data`() = runTest {
        // Act - no data added (fresh engine)
        val selection = engine.selectArm()

        // Assert
        assertTrue("Should have low confidence with no data", selection.confidence < 0.5f)
    }

    // ========== INVALID INPUT TESTS ==========

    @Test
    fun `should reject invalid arm ID`() = runTest {
        // Act & Assert - should throw exception
        var exceptionThrown = false
        try {
            engine.updateArm("invalid_arm_id", reward = 0.5f)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Should throw exception for invalid arm ID", exceptionThrown)
    }

    @Test
    fun `should reject reward outside range`() = runTest {
        // Act & Assert - should throw exception for reward > 1.0
        var exceptionThrown = false
        try {
            engine.updateArm(ThompsonSamplingEngine.ARM_REFLECTION, reward = 1.5f)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Should throw exception for reward > 1.0", exceptionThrown)

        // Act & Assert - should throw exception for reward < 0.0
        exceptionThrown = false
        try {
            engine.updateArm(ThompsonSamplingEngine.ARM_REFLECTION, reward = -0.1f)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Should throw exception for reward < 0.0", exceptionThrown)
    }
}
