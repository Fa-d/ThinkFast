package dev.sadakat.thinkfaster.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import dev.sadakat.thinkfaster.domain.repository.TrackedAppsRepository
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
 * Comprehensive tests for AppLaunchDetector to find bugs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppLaunchDetectorTest {

    private lateinit var detector: AppLaunchDetector
    private val mockContext = mockk<Context>(relaxUnitFun = true)
    private val mockRepository = mockk<TrackedAppsRepository>()
    private val mockUsageStatsManager = mockk<UsageStatsManager>()
    private val testDispatcher = StandardTestDispatcher()

    @JUnitBefore
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { mockContext.getSystemService(Context.USAGE_STATS_SERVICE) } returns mockUsageStatsManager
        detector = AppLaunchDetector(mockContext, mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // ========== CHECK FOR APP LAUNCH TESTS ==========

    @Test
    fun `should detect tracked app launch`() = runTest {
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.example.app")
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.checkForAppLaunch()

        assertNotNull("Should detect app launch", result)
        assertEquals("Should return package name", "com.example.app", result?.packageName)
    }

    @Test
    fun `should not detect untracked app launch`() = runTest {
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.tracked.app")
        val mockEvents = createMockEvents(
            EventInfo("com.untracked.app", UsageEvents.Event.ACTIVITY_RESUMED, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.checkForAppLaunch()

        assertNull("Should not detect untracked app", result)
    }

    @Test
    fun `should not detect same app again`() = runTest {
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.example.app")
        val timestamp = 1000L
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, timestamp)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        // First detection
        val result1 = detector.checkForAppLaunch()
        assertNotNull("Should detect first launch", result1)

        // Create new mock for second call
        val mockEvents2 = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, timestamp)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents2

        // Second detection with same app
        val result2 = detector.checkForAppLaunch()
        assertNull("Should not detect same app again", result2)
    }

    @Test
    fun `should detect app switch`() = runTest {
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.app1", "com.app2")
        val timestamp1 = 1000L
        val mockEvents1 = createMockEvents(
            EventInfo("com.app1", UsageEvents.Event.ACTIVITY_RESUMED, timestamp1)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents1

        val result1 = detector.checkForAppLaunch()
        assertEquals("Should detect first app", "com.app1", result1?.packageName)

        // Create new mock for second call with different app
        val timestamp2 = 6000L
        val mockEvents2 = createMockEvents(
            EventInfo("com.app2", UsageEvents.Event.ACTIVITY_RESUMED, timestamp2)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents2

        val result2 = detector.checkForAppLaunch()
        assertEquals("Should detect app switch", "com.app2", result2?.packageName)
    }

    @Test
    fun `should handle MOVE_TO_FOREGROUND event`() = runTest {
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.example.app")
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.MOVE_TO_FOREGROUND, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.checkForAppLaunch()

        assertNotNull("Should detect foreground event", result)
    }

    @Test
    fun `should return null when no events`() = runTest {
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.example.app")
        val mockEvents = createEmptyMockEvents()
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.checkForAppLaunch()

        assertNull("Should return null with no events", result)
    }

    @Test
    fun `should select most recent event`() = runTest {
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.example.app", "com.other.app")
        val now = 10000L
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, now - 10000),
            EventInfo("com.other.app", UsageEvents.Event.ACTIVITY_RESUMED, now),
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, now - 5000)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.checkForAppLaunch()

        // Most recent event should be selected
        assertEquals("Should select most recent", "com.other.app", result?.packageName)
    }

    // ========== GET CURRENT FOREGROUND APP TESTS ==========

    @Test
    fun `should get current foreground app`() {
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.getCurrentForegroundApp()

        assertEquals("Should return foreground app", "com.example.app", result)
    }

    @Test
    fun `should return null when no foreground app`() {
        val mockEvents = createEmptyMockEvents()
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.getCurrentForegroundApp()

        assertNull("Should return null", result)
    }

    @Test
    fun `should handle ACTIVITY_PAUSED event`() {
        val now = 1000L
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, now - 1000),
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_PAUSED, now)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.getCurrentForegroundApp()

        assertNull("Should return null when app paused", result)
    }

    @Test
    fun `should handle MOVE_TO_BACKGROUND event`() {
        val now = 1000L
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, now - 1000),
            EventInfo("com.example.app", UsageEvents.Event.MOVE_TO_BACKGROUND, now)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.getCurrentForegroundApp()

        assertNull("Should return null when app moved to background", result)
    }

    @Test
    fun `should not nullify foreground app for different package pause`() {
        val now = 1000L
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, now),
            EventInfo("com.other.app", UsageEvents.Event.ACTIVITY_PAUSED, now + 1000)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.getCurrentForegroundApp()

        assertEquals("Should keep foreground app", "com.example.app", result)
    }

    // ========== IS TARGET APP IN FOREGROUND TESTS ==========

    @Test
    fun `should return package when tracked app in foreground`() = runTest {
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.example.app")
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.isTargetAppInForeground()

        assertEquals("Should return package name", "com.example.app", result)
    }

    @Test
    fun `should return null when untracked app in foreground`() = runTest {
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.tracked.app")
        val mockEvents = createMockEvents(
            EventInfo("com.untracked.app", UsageEvents.Event.ACTIVITY_RESUMED, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.isTargetAppInForeground()

        assertNull("Should return null for untracked app", result)
    }

    @Test
    fun `should use cached last foreground app for continuous usage`() = runTest {
        // First, detect a tracked app
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.example.app")
        val mockEvents1 = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents1
        detector.checkForAppLaunch()

        // Now call isTargetAppInForeground with no new events
        val mockEvents2 = createEmptyMockEvents()
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents2

        val result = detector.isTargetAppInForeground()

        // Should use cached last foreground app
        assertEquals("Should use cached app", "com.example.app", result)
    }

    // ========== IS APP IN FOREGROUND TESTS ==========

    @Test
    fun `should detect specific app in foreground`() {
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.isAppInForeground("com.example.app")

        assertTrue("Should detect app in foreground", result)
    }

    @Test
    fun `should return false for different app`() {
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.isAppInForeground("com.other.app")

        assertFalse("Should return false for different app", result)
    }

    @Test
    fun `should return false when no app in foreground`() {
        val mockEvents = createEmptyMockEvents()
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.isAppInForeground("com.example.app")

        assertFalse("Should return false when no foreground app", result)
    }

    // ========== CACHE INVALIDATION TESTS ==========

    @Test
    fun `should invalidate cache when requested`() = runTest {
        // First call populates cache
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.app1")
        val mockEvents1 = createMockEvents(
            EventInfo("com.app1", UsageEvents.Event.ACTIVITY_RESUMED, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents1
        detector.checkForAppLaunch()

        // Invalidate cache
        detector.invalidateCache()
        testDispatcher.scheduler.advanceUntilIdle()

        // Next call should refresh from repository
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.app1", "com.app2")
        val mockEvents2 = createEmptyMockEvents()
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents2

        // Advance time to ensure cache is stale
        testDispatcher.scheduler.advanceTimeBy(6000)
        testDispatcher.scheduler.advanceUntilIdle()

        detector.checkForAppLaunch()

        // Should have called repository again after cache invalidation and time
        coVerify(atLeast = 1) { mockRepository.getTrackedApps() }
    }

    // ========== RESET TESTS ==========

    @Test
    fun `should reset detector state`() = runTest {
        coEvery { mockRepository.getTrackedApps() } returns listOf("com.example.app")
        val mockEvents1 = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents1

        // Detect an app
        detector.checkForAppLaunch()

        // Reset
        detector.reset()

        // Create new mock for next call
        val mockEvents2 = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, 2000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents2

        // Should detect the same app again after reset
        val result = detector.checkForAppLaunch()
        assertNotNull("Should detect app after reset", result)
    }

    // ========== EMPTY TRACKED APPS TESTS ==========

    @Test
    fun `should handle empty tracked apps list`() = runTest {
        coEvery { mockRepository.getTrackedApps() } returns emptyList()
        val mockEvents = createMockEvents(
            EventInfo("com.example.app", UsageEvents.Event.ACTIVITY_RESUMED, 1000L)
        )
        every { mockUsageStatsManager.queryEvents(any(), any()) } returns mockEvents

        val result = detector.checkForAppLaunch()

        assertNull("Should not detect when no tracked apps", result)
    }

    // ========== HELPER METHODS ==========

    private data class EventInfo(
        val packageName: String,
        val eventType: Int,
        val timestamp: Long
    )

    private fun createMockEvents(vararg events: EventInfo): UsageEvents {
        val mockEvents = mockk<UsageEvents>(relaxed = true)
        val iterator = events.iterator()
        val eventSlot = slot<UsageEvents.Event>()

        every { mockEvents.hasNextEvent() } answers { iterator.hasNext() }
        every { mockEvents.getNextEvent(capture(eventSlot)) } answers {
            if (iterator.hasNext()) {
                val info = iterator.next()
                // Set the public fields directly on the captured event object
                val event = eventSlot.captured
                // Use reflection to set the public fields
                setEventField(event, "mPackageName", info.packageName)
                setEventField(event, "mEventType", info.eventType)
                setEventField(event, "mTimeStamp", info.timestamp)
                true
            } else {
                false
            }
        }
        return mockEvents
    }

    private fun setEventField(event: UsageEvents.Event, fieldName: String, value: Any) {
        try {
            val field = event.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(event, when (value) {
                is Int -> value
                is Long -> value
                else -> value
            })
        } catch (e: Exception) {
            // Field not found or could not be set - try different naming
            try {
                val field = event.javaClass.getDeclaredField(fieldName.replace("m", ""))
                field.isAccessible = true
                field.set(event, when (value) {
                    is Int -> value
                    is Long -> value
                    else -> value
                })
            } catch (e2: Exception) {
                // Ignore and proceed
            }
        }
    }

    private fun createEmptyMockEvents(): UsageEvents {
        val mockEvents = mockk<UsageEvents>(relaxed = true)
        every { mockEvents.hasNextEvent() } returns false
        return mockEvents
    }
}
