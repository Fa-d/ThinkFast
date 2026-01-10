package dev.sadakat.thinkfaster.domain.intervention

import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before as JUnitBefore
import org.junit.Test

/**
 * Comprehensive edge case tests for RewardCalculator to find potential bugs.
 *
 * Tests focus on:
 * - Boundary conditions
 * - Null handling
 * - Reward clamping to [0.0, 1.0]
 * - Edge cases in session duration handling
 * - Edge cases in reopen delay calculations
 * - Extreme value handling
 */
class RewardCalculatorEdgeCaseTest {

    private lateinit var calculator: RewardCalculator

    @JUnitBefore
    fun setup() {
        calculator = RewardCalculator()
    }

    // ========== BASE REWARD EDGE CASES ==========

    @Test
    fun `should handle unknown user choice gracefully`() {
        val result = calculator.calculateReward(
            userChoice = "UNKNOWN_CHOICE"
        )

        assertEquals("Unknown choice should default to 0.5", 0.5f, result, 0.001f)
    }

    @Test
    fun `should handle empty string user choice`() {
        val result = calculator.calculateReward(
            userChoice = ""
        )

        assertEquals("Empty choice should default to 0.5", 0.5f, result, 0.001f)
    }

    @Test
    fun `should handle null user choice (if nullable param existed)`() {
        // Note: current implementation doesn't allow null userChoice
        // This test documents current behavior
        val result = calculator.calculateReward(
            userChoice = "null_value"
        )

        assertNotNull("Reward should not be null", result)
    }

    // ========== FEEDBACK EDGE CASES ==========

    @Test
    fun `should handle unknown feedback type gracefully`() {
        val result = calculator.calculateReward(
            userChoice = "GO_BACK",
            feedback = "UNKNOWN_FEEDBACK"
        )

        // GO_BACK = 1.0, unknown feedback should add 0.0
        assertEquals("Unknown feedback should not affect reward", 1.0f, result, 0.001f)
    }

    @Test
    fun `should handle empty feedback string`() {
        val result = calculator.calculateReward(
            userChoice = "GO_BACK",
            feedback = ""
        )

        assertEquals("Empty feedback should not affect reward", 1.0f, result, 0.001f)
    }

    @Test
    fun `should handle case-sensitive feedback correctly`() {
        // Use CONTINUE (0.3 base) instead of GO_BACK to see the bonus without clamping
        val resultLower = calculator.calculateReward(
            userChoice = "CONTINUE",
            feedback = "helpful"  // lowercase - should not match
        )
        val resultUpper = calculator.calculateReward(
            userChoice = "CONTINUE",
            feedback = "HELPFUL"  // uppercase - should match
        )

        // "helpful" (lowercase) should NOT get bonus → 0.3
        // "HELPFUL" (uppercase) should get bonus → 0.5
        assertEquals("Lowercase 'helpful' should give 0.3", 0.3f, resultLower, 0.001f)
        assertEquals("Uppercase 'HELPFUL' should give 0.5", 0.5f, resultUpper, 0.001f)
    }

    // ========== REWARD CLAMPING TESTS (CRITICAL FOR BUGS) ==========

    @Test
    fun `should clamp maximum reward to 1_0 even with all bonuses`() {
        // GO_BACK (1.0) + HELPFUL (+0.2) + Session ended (+0.1) + No quick reopen (+0.1) = 1.4
        val result = calculator.calculateReward(
            userChoice = "GO_BACK",
            feedback = "HELPFUL",
            sessionContinued = false,  // Session ended = bonus
            sessionDurationAfter = 3 * 60 * 1000L,  // 3 min = bonus
            quickReopen = false,
            reopenDelayMs = 10 * 60 * 1000L  // 10 min = bonus
        )

        assertEquals("Reward should be clamped to 1.0 maximum", 1.0f, result, 0.001f)
    }

    @Test
    fun `should clamp minimum reward to 0_0 even with all penalties`() {
        // GO_BACK (1.0) + DISRUPTIVE (-0.3) + Quick reopen (-0.2) + Extended session (-0.1)
        // But we need a base that can go negative - use CONTINUE (0.3)
        // 0.3 + (-0.3) + (-0.2) + (-0.1) = -0.3
        val result = calculator.calculateReward(
            userChoice = "CONTINUE",
            feedback = "DISRUPTIVE",
            sessionContinued = true,
            sessionDurationAfter = 20 * 60 * 1000L,  // 20 min = extended session penalty
            quickReopen = true
        )

        assertEquals("Reward should be clamped to 0.0 minimum", 0.0f, result, 0.001f)
    }

    @Test
    fun `should handle TIMEOUT with DISRUPTIVE feedback correctly`() {
        // TIMEOUT (0.1) + DISRUPTIVE (-0.3) = -0.2 → should clamp to 0.0
        val result = calculator.calculateReward(
            userChoice = "TIMEOUT",
            feedback = "DISRUPTIVE"
        )

        assertEquals("Should clamp negative reward to 0.0", 0.0f, result, 0.001f)
    }

    // ========== SESSION DURATION EDGE CASES ==========

    @Test
    fun `should handle exact threshold durations correctly`() {
        // Test exactly 5 minutes (should get bonus)
        val exactlyFiveMin = calculator.calculateReward(
            userChoice = "DISMISS",
            sessionDurationAfter = 5 * 60 * 1000L  // Exactly 5 min
        )

        // Test exactly 15 minutes (should get penalty)
        val exactlyFifteenMin = calculator.calculateReward(
            userChoice = "DISMISS",
            sessionDurationAfter = 15 * 60 * 1000L  // Exactly 15 min
        )

        assertEquals("Exactly 5 min should get bonus", 0.1f, exactlyFiveMin, 0.001f)
        assertEquals("Exactly 15 min should NOT get penalty (not > 15)", 0.0f, exactlyFifteenMin, 0.001f)
    }

    @Test
    fun `should handle zero session duration`() {
        val result = calculator.calculateReward(
            userChoice = "GO_BACK",
            sessionDurationAfter = 0L
        )

        assertEquals("Zero duration should get short session bonus", 1.0f, result, 0.001f)
    }

    @Test
    fun `should handle negative session duration gracefully`() {
        val result = calculator.calculateReward(
            userChoice = "GO_BACK",
            sessionDurationAfter = -1000L
        )

        // Negative duration <= 5 min threshold, so should get bonus
        // But this might be a bug - negative duration doesn't make sense
        assertEquals("Negative duration handled (potential bug)", 1.0f, result, 0.001f)
    }

    @Test
    fun `should handle very large session duration`() {
        val result = calculator.calculateReward(
            userChoice = "GO_BACK",
            sessionDurationAfter = Long.MAX_VALUE
        )

        // BUG/BEHAVIOR: Extended session penalty applies even with extreme values
        // GO_BACK (1.0) + extended session penalty (-0.1) = 0.9
        // This might be intended behavior - penalty should apply
        assertEquals("Large duration gets penalty", 0.9f, result, 0.001f)
    }

    // ========== REOPEN DELAY EDGE CASES ==========

    @Test
    fun `should handle exact threshold reopen delays correctly`() {
        // Exactly 2 minutes = should get penalty (not < 2)
        val exactlyTwoMin = calculator.calculateReward(
            userChoice = "GO_BACK",
            reopenDelayMs = 2 * 60 * 1000L
        )

        // Exactly 5 minutes = should NOT get bonus (not > 5)
        val exactlyFiveMin = calculator.calculateReward(
            userChoice = "GO_BACK",
            reopenDelayMs = 5 * 60 * 1000L
        )

        assertEquals("Exactly 2 min should NOT get quick reopen penalty", 1.0f, exactlyTwoMin, 0.001f)
        assertEquals("Exactly 5 min should NOT get no-reopen bonus", 1.0f, exactlyFiveMin, 0.001f)
    }

    @Test
    fun `should handle zero reopen delay`() {
        val result = calculator.calculateReward(
            userChoice = "GO_BACK",
            reopenDelayMs = 0L
        )

        assertEquals("Zero delay should get quick reopen penalty", 0.8f, result, 0.001f)
    }

    @Test
    fun `should handle negative reopen delay`() {
        val result = calculator.calculateReward(
            userChoice = "GO_BACK",
            reopenDelayMs = -1000L
        )

        // Negative delay < 2 min, so gets penalty
        // But this is semantically wrong - negative delay doesn't make sense
        assertEquals("Negative delay handled (potential bug)", 0.8f, result, 0.001f)
    }

    @Test
    fun `should handle both quickReopen boolean and reopenDelayMs together`() {
        // When both are provided, penalty might be applied twice
        val result = calculator.calculateReward(
            userChoice = "GO_BACK",
            quickReopen = true,
            reopenDelayMs = 30 * 1000L  // 30 seconds = quick
        )

        // GO_BACK (1.0) + quickReopen penalty (-0.2) + reopenDelayMs penalty (-0.2)
        // This might double-apply the penalty - potential bug
        assertEquals("Check for double penalty application", 0.6f, result, 0.001f)
    }

    // ========== INTERACTION BETWEEN PARAMETERS ==========

    @Test
    fun `should handle conflicting sessionContinued values`() {
        // sessionContinued = false (bonus) but sessionDurationAfter = long (penalty)
        val result = calculator.calculateReward(
            userChoice = "GO_BACK",
            sessionContinued = false,  // Bonus
            sessionDurationAfter = 20 * 60 * 1000L  // Penalty
        )

        // 1.0 + 0.1 (session ended) - 0.1 (extended session) = 1.0
        assertEquals("Conflicting signals should balance", 1.0f, result, 0.001f)
    }

    @Test
    fun `should handle DISMISS with all positive modifiers`() {
        // DISMISS (0.0) + all bonuses
        val result = calculator.calculateReward(
            userChoice = "DISMISS",
            feedback = "HELPFUL",
            sessionContinued = false,
            sessionDurationAfter = 3 * 60 * 1000L,
            quickReopen = false,
            reopenDelayMs = 10 * 60 * 1000L
        )

        // 0.0 + 0.2 + 0.1 + 0.1 + 0.1 = 0.5
        assertEquals("DISMISS with bonuses should still be moderate", 0.5f, result, 0.001f)
    }

    // ========== BINARY REWARD TESTS ==========

    @Test
    fun `binary reward should only return 0 or 1`() {
        val goBackReward = calculator.calculateBinaryReward("GO_BACK")
        val continueReward = calculator.calculateBinaryReward("CONTINUE")
        val dismissReward = calculator.calculateBinaryReward("DISMISS")
        val timeoutReward = calculator.calculateBinaryReward("TIMEOUT")

        assertEquals("GO_BACK should be 1.0", 1.0f, goBackReward, 0.001f)
        assertEquals("CONTINUE should be 0.0", 0.0f, continueReward, 0.001f)
        assertEquals("DISMISS should be 0.0", 0.0f, dismissReward, 0.001f)
        assertEquals("TIMEOUT should be 0.0", 0.0f, timeoutReward, 0.001f)
    }

    // ========== IS SUCCESSFUL OUTCOME TESTS ==========

    @Test
    fun `isSuccessfulOutcome should handle null feedback correctly`() {
        val goBackResult = calculator.isSuccessfulOutcome("GO_BACK", null)
        val continueResult = calculator.isSuccessfulOutcome("CONTINUE", null)

        assertTrue("GO_BACK should be successful without feedback", goBackResult)
        assertFalse("CONTINUE should not be successful without HELPFUL feedback", continueResult)
    }

    @Test
    fun `isSuccessfulOutcome should be case sensitive for feedback`() {
        val lowerResult = calculator.isSuccessfulOutcome("CONTINUE", "helpful")
        val upperResult = calculator.isSuccessfulOutcome("CONTINUE", "HELPFUL")

        assertFalse("Lowercase 'helpful' should not be successful", lowerResult)
        assertTrue("Uppercase 'HELPFUL' should be successful", upperResult)
    }

    // ========== NORMALIZED REWARD TESTS ==========

    @Test
    fun `normalized reward should clamp to valid range`() {
        val result = calculator.calculateNormalizedReward(
            baseReward = 0.9f,
            confidence = 0.0f  // Maximum exploration bonus
        )

        // 0.9 + (1.0 - 0.0) * 0.1 = 1.0
        assertEquals("Should clamp to 1.0", 1.0f, result, 0.001f)
    }

    @Test
    fun `normalized reward should handle zero base reward`() {
        val result = calculator.calculateNormalizedReward(
            baseReward = 0.0f,
            confidence = 0.0f
        )

        // 0.0 + 0.1 = 0.1
        assertEquals("Zero base with exploration should be 0.1", 0.1f, result, 0.001f)
    }

    @Test
    fun `normalized reward should not go negative`() {
        val result = calculator.calculateNormalizedReward(
            baseReward = 0.0f,
            confidence = 2.0f  // Invalid confidence > 1.0
        )

        // (1.0 - 2.0) * 0.1 = -0.1, so 0.0 + (-0.1) = -0.1 → should clamp to 0.0
        // But the calculation doesn't handle invalid confidence values
        assertTrue("Result should be >= 0", result >= 0.0f)
    }

    // ========== EXPLAIN REWARD TESTS ==========

    @Test
    fun `explainReward should handle unknown choices`() {
        val explanation = calculator.explainReward(
            userChoice = "UNKNOWN",
            feedback = null
        )

        assertNotNull("Explanation should not be null", explanation)
        assertTrue("Should mention unknown choice", explanation.contains("Unknown"))
    }

    // ========== FLOATING POINT PRECISION TESTS ==========

    @Test
    fun `should handle floating point precision correctly`() {
        val result1 = calculator.calculateReward(userChoice = "GO_BACK")
        val result2 = calculator.calculateReward(userChoice = "GO_BACK")

        // Same inputs should give same outputs
        assertEquals("Results should be consistent", result1, result2, 0.0f)
    }

    // ========== COMBINED EDGE CASE SCENARIOS ==========

    @Test
    fun `should handle all null parameters except userChoice`() {
        val result = calculator.calculateReward(
            userChoice = "GO_BACK",
            feedback = null,
            sessionContinued = null,
            sessionDurationAfter = null,
            quickReopen = null,
            reopenDelayMs = null
        )

        assertEquals("All nulls should just give base reward", 1.0f, result, 0.001f)
    }

    @Test
    fun `should handle maximum penalties scenario`() {
        // DISMISS (0.0) + DISRUPTIVE (-0.3) + quickReopen (-0.2) + extended session (-0.1)
        val result = calculator.calculateReward(
            userChoice = "DISMISS",
            feedback = "DISRUPTIVE",
            quickReopen = true,
            sessionDurationAfter = 16 * 60 * 1000L  // > 15 min
        )

        assertEquals("Should clamp to minimum", 0.0f, result, 0.001f)
    }

    @Test
    fun `should handle near-boundary values correctly`() {
        // Test 2999ms vs 3000ms (just under 2 min threshold)
        val justUnder = calculator.calculateReward(
            userChoice = "GO_BACK",
            reopenDelayMs = 119999L  // 1 min 59.999 sec
        )

        // Test 3001ms (just over 2 min threshold)
        val justOver = calculator.calculateReward(
            userChoice = "GO_BACK",
            reopenDelayMs = 120001L  // 2 min 0.001 sec
        )

        assertEquals("Just under 2 min should get penalty", 0.8f, justUnder, 0.001f)
        assertEquals("Just over 2 min should NOT get penalty", 1.0f, justOver, 0.001f)
    }
}
