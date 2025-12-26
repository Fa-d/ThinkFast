package dev.sadakat.thinkfast.service

import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.UsageEvent
import dev.sadakat.thinkfast.domain.model.UsageSession
import dev.sadakat.thinkfast.util.Constants
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetectorTest {

    private lateinit var sessionDetector: SessionDetector
    private val mockRepository = mockk<dev.sadakat.thinkfast.domain.repository.UsageRepository>(relaxUnitFun = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionDetector = SessionDetector(mockRepository, CoroutineScope(testDispatcher))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onAppInForeground with no active session starts new session`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        val sessionId = 1L
        coEvery { mockRepository.insertSession(any()) } returns sessionId
        coEvery { mockRepository.insertEvent(any()) } returns Unit

        var sessionStarted: SessionDetector.SessionState? = null
        sessionDetector.onSessionStart = { sessionStarted = it }

        // Act
        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)

        // Advance dispatcher to process coroutines
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify { mockRepository.insertSession(any()) }
        coVerify {
            mockRepository.insertEvent(match {
                it.sessionId == sessionId &&
                it.eventType == Constants.EVENT_APP_OPENED &&
                it.timestamp == timestamp
            })
        }

        assertNotNull(sessionStarted)
        assertEquals(AppTarget.FACEBOOK, sessionStarted!!.targetApp)
        assertEquals(timestamp, sessionStarted!!.startTimestamp)
        assertEquals(timestamp, sessionStarted!!.lastActiveTimestamp)
        assertFalse(sessionStarted!!.hasShownTenMinuteAlert)
    }

    @Test
    fun `onAppInForeground with same app within gap threshold continues session`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        val sessionId = 1L
        coEvery { mockRepository.insertSession(any()) } returns sessionId
        coEvery { mockRepository.insertEvent(any()) } returns Unit

        var sessionStarted: SessionDetector.SessionState? = null
        sessionDetector.onSessionStart = { sessionStarted = it }

        // Start initial session
        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - same app comes back within 30 seconds
        val newTimestamp = timestamp + 15000 // 15 seconds later
        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, newTimestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - only one session should have been created
        coVerify(exactly = 1) { mockRepository.insertSession(any()) }

        // Session should have been updated
        val currentSession = sessionDetector.getCurrentSession()
        assertNotNull(currentSession)
        assertEquals(newTimestamp, currentSession!!.lastActiveTimestamp)
    }

    @Test
    fun `onAppInForeground with same app after gap threshold starts new session`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        val sessionId1 = 1L
        val sessionId2 = 2L
        coEvery { mockRepository.insertSession(any()) } returnsMany listOf(sessionId1, sessionId2)
        coEvery { mockRepository.insertEvent(any()) } returns Unit
        coEvery {
            mockRepository.endSession(
                sessionId = any(),
                endTimestamp = any(),
                wasInterrupted = any(),
                interruptionType = any()
            )
        } returns Unit

        var sessionsStarted = mutableListOf<SessionDetector.SessionState>()
        var sessionsEnded = mutableListOf<SessionDetector.SessionState>()
        sessionDetector.onSessionStart = { sessionsStarted.add(it) }
        sessionDetector.onSessionEnd = { sessionsEnded.add(it) }

        // Start initial session
        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - same app comes back after 30+ seconds
        val newTimestamp = timestamp + Constants.SESSION_GAP_THRESHOLD + 1000 // 31 seconds later
        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, newTimestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - two sessions should have been created
        coVerify(exactly = 2) { mockRepository.insertSession(any()) }
        coVerify(exactly = 1) {
            mockRepository.endSession(
                sessionId = sessionId1,
                endTimestamp = any<Long>(),
                wasInterrupted = false,
                interruptionType = null
            )
        }

        assertEquals(2, sessionsStarted.size)
        assertEquals(1, sessionsEnded.size)
    }

    @Test
    fun `onAppInForeground with different app starts new session`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        val sessionId1 = 1L
        val sessionId2 = 2L
        coEvery { mockRepository.insertSession(any()) } returnsMany listOf(sessionId1, sessionId2)
        coEvery { mockRepository.insertEvent(any()) } returns Unit
        coEvery {
            mockRepository.endSession(
                sessionId = any(),
                endTimestamp = any(),
                wasInterrupted = any(),
                interruptionType = any()
            )
        } returns Unit

        var sessionsStarted = mutableListOf<SessionDetector.SessionState>()
        var sessionsEnded = mutableListOf<SessionDetector.SessionState>()
        sessionDetector.onSessionStart = { sessionsStarted.add(it) }
        sessionDetector.onSessionEnd = { sessionsEnded.add(it) }

        // Start Facebook session
        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - Instagram comes to foreground
        val newTimestamp = timestamp + 5000 // 5 seconds later
        sessionDetector.onAppInForeground(AppTarget.INSTAGRAM, newTimestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { mockRepository.insertSession(any()) }
        assertEquals(2, sessionsStarted.size)
        assertEquals(1, sessionsEnded.size)

        assertEquals(AppTarget.FACEBOOK, sessionsStarted[0].targetApp)
        assertEquals(AppTarget.INSTAGRAM, sessionsStarted[1].targetApp)
    }

    @Test
    fun `session exceeding 10 minutes triggers alert`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        val sessionId = 1L
        coEvery { mockRepository.insertSession(any()) } returns sessionId
        coEvery { mockRepository.insertEvent(any()) } returns Unit
        coEvery { mockRepository.updateSession(any()) } returns Unit

        var alertTriggered: SessionDetector.SessionState? = null
        sessionDetector.onTenMinuteAlert = { alertTriggered = it }

        // Start session
        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - continue session multiple times to reach 10+ minutes
        // We need to stay within the 30-second gap threshold
        var currentTime = timestamp + 20000 // 20 seconds later (within threshold)
        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, currentTime)
        testDispatcher.scheduler.advanceUntilIdle()

        currentTime += 20000 // 40 seconds total (within threshold)
        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, currentTime)
        testDispatcher.scheduler.advanceUntilIdle()

        // Add more updates until we exceed 10 minutes, staying within 30-second threshold
        val tenMinutesPlus = timestamp + Constants.TEN_MINUTES_MILLIS + 1000
        var accumulatedTime = currentTime
        while (accumulatedTime < tenMinutesPlus) {
            accumulatedTime += 20000 // Add 20 seconds each time
            sessionDetector.onAppInForeground(AppTarget.FACEBOOK, accumulatedTime)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // Assert
        val currentSession = sessionDetector.getCurrentSession()
        assertNotNull(currentSession)
        assertTrue(currentSession!!.hasShownTenMinuteAlert)
        assertNotNull(alertTriggered)
    }

    @Test
    fun `checkForSessionTimeout with no active session does nothing`() {
        // Act
        sessionDetector.checkForSessionTimeout(System.currentTimeMillis())

        // Assert - no interactions with repository
        coVerify { mockRepository wasNot Called }
    }

    @Test
    fun `checkForSessionTimeout within gap threshold does not end session`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        coEvery { mockRepository.insertSession(any()) } returns 1L
        coEvery { mockRepository.insertEvent(any()) } returns Unit

        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - check timeout after 10 seconds
        val checkTime = timestamp + 10000
        sessionDetector.checkForSessionTimeout(checkTime)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - session should still be active
        assertNotNull(sessionDetector.getCurrentSession())
        coVerify(exactly = 0) {
            mockRepository.endSession(
                sessionId = any(),
                endTimestamp = any(),
                wasInterrupted = any(),
                interruptionType = any()
            )
        }
    }

    @Test
    fun `checkForSessionTimeout after gap threshold ends session`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        val sessionId = 1L
        coEvery { mockRepository.insertSession(any()) } returns sessionId
        coEvery { mockRepository.insertEvent(any()) } returns Unit
        coEvery {
            mockRepository.endSession(
                sessionId = any(),
                endTimestamp = any(),
                wasInterrupted = any(),
                interruptionType = any()
            )
        } returns Unit

        var sessionEnded: SessionDetector.SessionState? = null
        sessionDetector.onSessionEnd = { sessionEnded = it }

        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - check timeout after 30+ seconds
        val checkTime = timestamp + Constants.SESSION_GAP_THRESHOLD + 1000
        sessionDetector.checkForSessionTimeout(checkTime)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - session should be ended
        assertNull(sessionDetector.getCurrentSession())
        assertNotNull(sessionEnded)
        coVerify {
            mockRepository.endSession(
                sessionId = sessionId,
                endTimestamp = any(),
                wasInterrupted = false,
                interruptionType = null
            )
        }
    }

    @Test
    fun `forceEndSession ends session immediately`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        val sessionId = 1L
        coEvery { mockRepository.insertSession(any()) } returns sessionId
        coEvery { mockRepository.insertEvent(any()) } returns Unit
        coEvery {
            mockRepository.endSession(
                sessionId = any(),
                endTimestamp = any(),
                wasInterrupted = any(),
                interruptionType = any()
            )
        } returns Unit

        var sessionEnded: SessionDetector.SessionState? = null
        sessionDetector.onSessionEnd = { sessionEnded = it }

        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        val endTimestamp = timestamp + 5000
        sessionDetector.forceEndSession(endTimestamp, wasInterrupted = true, "screen_off")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertNull(sessionDetector.getCurrentSession())
        assertNotNull(sessionEnded)
        coVerify {
            mockRepository.endSession(
                sessionId = sessionId,
                endTimestamp = endTimestamp,
                wasInterrupted = true,
                interruptionType = "screen_off"
            )
        }
    }

    @Test
    fun `getCurrentSessionDuration returns correct duration`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        coEvery { mockRepository.insertSession(any()) } returns 1L
        coEvery { mockRepository.insertEvent(any()) } returns Unit

        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - wait a bit and check duration
        Thread.sleep(100)
        val duration = sessionDetector.getCurrentSessionDuration()

        // Assert
        assertTrue(duration >= 100) // At least 100ms should have passed
    }

    @Test
    fun `hasActiveSession returns true when session is active`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        coEvery { mockRepository.insertSession(any()) } returns 1L
        coEvery { mockRepository.insertEvent(any()) } returns Unit

        // Act
        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertTrue(sessionDetector.hasActiveSession())
    }

    @Test
    fun `hasActiveSession returns false when no session is active`() {
        // Assert - no session started
        assertFalse(sessionDetector.hasActiveSession())
    }

    @Test
    fun `reset clears current session`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        coEvery { mockRepository.insertSession(any()) } returns 1L
        coEvery { mockRepository.insertEvent(any()) } returns Unit

        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(sessionDetector.hasActiveSession())

        // Act
        sessionDetector.reset()

        // Assert
        assertFalse(sessionDetector.hasActiveSession())
        assertNull(sessionDetector.getCurrentSession())
    }

    @Test
    fun `session shorter than minimum duration is deleted`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        val sessionId = 1L
        coEvery { mockRepository.insertSession(any()) } returns sessionId
        coEvery { mockRepository.insertEvent(any()) } returns Unit
        coEvery {
            mockRepository.endSession(
                sessionId = any(),
                endTimestamp = any(),
                wasInterrupted = any(),
                interruptionType = any()
            )
        } returns Unit

        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - end session after only 3 seconds (below 5 second threshold)
        val endTimestamp = timestamp + 3000
        sessionDetector.forceEndSession(endTimestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - session should be ended but marked as too short
        assertNull(sessionDetector.getCurrentSession())
        coVerify {
            mockRepository.endSession(
                sessionId = sessionId,
                endTimestamp = endTimestamp,
                wasInterrupted = false,
                interruptionType = null
            )
        }
    }

    @Test
    fun `should not trigger 10-minute alert twice`() = runTest {
        // Arrange
        val timestamp = System.currentTimeMillis()
        val sessionId = 1L
        coEvery { mockRepository.insertSession(any()) } returns sessionId
        coEvery { mockRepository.insertEvent(any()) } returns Unit
        coEvery { mockRepository.updateSession(any()) } returns Unit

        var alertCount = 0
        sessionDetector.onTenMinuteAlert = { alertCount++ }

        // Start session
        sessionDetector.onAppInForeground(AppTarget.FACEBOOK, timestamp)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act - continue session multiple times beyond 10 minutes, staying within 30-second threshold
        var currentTime = timestamp
        val tenMinutesPlus = timestamp + Constants.TEN_MINUTES_MILLIS + 60000 // 11 minutes

        // Add time in increments of 20 seconds to stay within the 30-second threshold
        while (currentTime < tenMinutesPlus) {
            currentTime += 20000
            sessionDetector.onAppInForeground(AppTarget.FACEBOOK, currentTime)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // Continue beyond 12 minutes
        val twelveMinutes = timestamp + Constants.TEN_MINUTES_MILLIS + 120000
        while (currentTime < twelveMinutes) {
            currentTime += 20000
            sessionDetector.onAppInForeground(AppTarget.FACEBOOK, currentTime)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // Assert - only one alert should have been triggered
        assertEquals(1, alertCount)
    }
}
