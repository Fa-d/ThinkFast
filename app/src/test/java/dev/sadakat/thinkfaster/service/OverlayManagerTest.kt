package dev.sadakat.thinkfaster.service

import android.content.Context
import dev.sadakat.thinkfaster.domain.model.OverlayStyle
import dev.sadakat.thinkfaster.domain.model.UserSettings
import dev.sadakat.thinkfaster.domain.repository.SettingsRepository
import dev.sadakat.thinkfaster.presentation.overlay.CompactReminderOverlayWindow
import dev.sadakat.thinkfaster.presentation.overlay.CompactTimerOverlayWindow
import dev.sadakat.thinkfaster.presentation.overlay.ReminderOverlayWindow
import dev.sadakat.thinkfaster.presentation.overlay.TimerOverlayWindow
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before as JUnitBefore
import org.junit.Test

/**
 * Comprehensive tests for OverlayManager to find bugs.
 */
class OverlayManagerTest {

    private lateinit var overlayManager: OverlayManager
    private val mockContext = mockk<Context>(relaxUnitFun = true)
    private val mockReminderOverlay = mockk<ReminderOverlayWindow>(relaxUnitFun = true)
    private val mockTimerOverlay = mockk<TimerOverlayWindow>(relaxUnitFun = true)
    private val mockCompactReminderOverlay = mockk<CompactReminderOverlayWindow>(relaxUnitFun = true)
    private val mockCompactTimerOverlay = mockk<CompactTimerOverlayWindow>(relaxUnitFun = true)
    private val mockSettingsRepository = mockk<SettingsRepository>()

    @JUnitBefore
    fun setup() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()
        overlayManager = OverlayManager(
            mockContext,
            mockReminderOverlay,
            mockTimerOverlay,
            mockCompactReminderOverlay,
            mockCompactTimerOverlay,
            mockSettingsRepository
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ========== SHOW REMINDER TESTS ==========

    @Test
    fun `should show reminder overlay when no overlay is showing`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings(
            overlayStyle = OverlayStyle.FULLSCREEN
        )

        val result = overlayManager.showReminder(123L, "com.example.app")

        assertTrue("Should show reminder", result)
        verify { mockReminderOverlay.show(123L, "com.example.app") }
        verify(exactly = 0) { mockCompactReminderOverlay.show(any(), any()) }
        assertEquals("Current overlay should be REMINDER",
            OverlayManager.OverlayType.REMINDER, overlayManager.getCurrentOverlayType())
    }

    @Test
    fun `should show compact reminder overlay when compact mode is enabled`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings(
            overlayStyle = OverlayStyle.COMPACT
        )

        val result = overlayManager.showReminder(123L, "com.example.app")

        assertTrue("Should show reminder", result)
        verify { mockCompactReminderOverlay.show(123L, "com.example.app") }
        verify(exactly = 0) { mockReminderOverlay.show(any(), any()) }
    }

    @Test
    fun `should not show reminder when reminder is already showing`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // First call shows reminder
        overlayManager.showReminder(123L, "com.example.app")

        // Reset mocks
        clearMocks(mockReminderOverlay, mockCompactReminderOverlay)

        // Second call should not show
        val result = overlayManager.showReminder(123L, "com.example.app")

        assertFalse("Should not show duplicate reminder", result)
        verify(exactly = 0) { mockReminderOverlay.show(any(), any()) }
        verify(exactly = 0) { mockCompactReminderOverlay.show(any(), any()) }
    }

    @Test
    fun `should dismiss timer and show reminder when timer is showing`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // First show timer
        overlayManager.showTimer(123L, "com.example.app", 1000L, 5000L)

        // Reset mocks
        clearMocks(mockTimerOverlay, mockCompactTimerOverlay, mockReminderOverlay, mockCompactReminderOverlay)
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // Now show reminder - should dismiss timer
        val result = overlayManager.showReminder(456L, "com.example.app")

        assertTrue("Should show reminder after dismissing timer", result)
        verify { mockTimerOverlay.dismiss() }
        verify { mockCompactTimerOverlay.dismiss() }
    }

    // ========== SHOW TIMER TESTS ==========

    @Test
    fun `should show timer overlay when no overlay is showing`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings(
            overlayStyle = OverlayStyle.FULLSCREEN
        )

        val result = overlayManager.showTimer(123L, "com.example.app", 1000L, 5000L)

        assertTrue("Should show timer", result)
        verify { mockTimerOverlay.show(123L, "com.example.app", 1000L, 5000L) }
        verify(exactly = 0) { mockCompactTimerOverlay.show(any(), any(), any(), any()) }
        assertEquals("Current overlay should be TIMER",
            OverlayManager.OverlayType.TIMER, overlayManager.getCurrentOverlayType())
    }

    @Test
    fun `should show compact timer overlay when compact mode is enabled`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings(
            overlayStyle = OverlayStyle.COMPACT
        )

        val result = overlayManager.showTimer(123L, "com.example.app", 1000L, 5000L)

        assertTrue("Should show timer", result)
        verify { mockCompactTimerOverlay.show(123L, "com.example.app", 1000L, 5000L) }
        verify(exactly = 0) { mockTimerOverlay.show(any(), any(), any(), any()) }
    }

    @Test
    fun `should not show timer when timer is already showing`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // First call shows timer
        overlayManager.showTimer(123L, "com.example.app", 1000L, 5000L)

        // Reset mocks
        clearMocks(mockTimerOverlay, mockCompactTimerOverlay)

        // Second call should not show
        val result = overlayManager.showTimer(123L, "com.example.app", 1000L, 5000L)

        assertFalse("Should not show duplicate timer", result)
        verify(exactly = 0) { mockTimerOverlay.show(any(), any(), any(), any()) }
        verify(exactly = 0) { mockCompactTimerOverlay.show(any(), any(), any(), any()) }
    }

    @Test
    fun `should dismiss reminder and show timer when reminder is showing`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // First show reminder
        overlayManager.showReminder(123L, "com.example.app")

        // Reset mocks
        clearMocks(mockReminderOverlay, mockCompactReminderOverlay, mockTimerOverlay, mockCompactTimerOverlay)
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // Now show timer - should dismiss reminder
        val result = overlayManager.showTimer(456L, "com.example.app", 1000L, 5000L)

        assertTrue("Should show timer after dismissing reminder", result)
        verify { mockReminderOverlay.dismiss() }
        verify { mockCompactReminderOverlay.dismiss() }
    }

    // ========== DISMISS ALL TESTS ==========

    @Test
    fun `should dismiss all overlays`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // Show reminder
        overlayManager.showReminder(123L, "com.example.app")

        // Reset mocks
        clearMocks(mockReminderOverlay, mockCompactReminderOverlay, mockTimerOverlay, mockCompactTimerOverlay)

        // Dismiss all
        overlayManager.dismissAll()

        verify { mockReminderOverlay.dismiss() }
        verify { mockCompactReminderOverlay.dismiss() }
        verify { mockTimerOverlay.dismiss() }
        verify { mockCompactTimerOverlay.dismiss() }
        assertNull("Current overlay should be null", overlayManager.getCurrentOverlayType())
        assertFalse("No overlay should be showing", overlayManager.isAnyOverlayShowing())
    }

    @Test
    fun `should dismiss all overlays when timer is showing`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // Show timer
        overlayManager.showTimer(123L, "com.example.app", 1000L, 5000L)

        // Reset mocks
        clearMocks(mockReminderOverlay, mockCompactReminderOverlay, mockTimerOverlay, mockCompactTimerOverlay)

        // Dismiss all
        overlayManager.dismissAll()

        verify { mockReminderOverlay.dismiss() }
        verify { mockTimerOverlay.dismiss() }
    }

    // ========== ON REMINDER DISMISSED TESTS ==========

    @Test
    fun `should clear overlay state when reminder is dismissed`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // Show reminder
        overlayManager.showReminder(123L, "com.example.app")

        // Reset mocks
        clearMocks(mockReminderOverlay, mockCompactReminderOverlay)

        // Call onReminderDismissed
        overlayManager.onReminderDismissed()

        verify { mockReminderOverlay.dismiss() }
        verify { mockCompactReminderOverlay.dismiss() }
        assertNull("Current overlay should be null", overlayManager.getCurrentOverlayType())
    }

    @Test
    fun `should not clear overlay state when timer is showing and reminder dismissed is called`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // Show timer
        overlayManager.showTimer(123L, "com.example.app", 1000L, 5000L)

        // Reset mocks
        clearMocks(mockReminderOverlay, mockCompactReminderOverlay)

        // Call onReminderDismissed
        overlayManager.onReminderDismissed()

        // Should not dismiss timer overlays
        verify(exactly = 0) { mockTimerOverlay.dismiss() }
        verify(exactly = 0) { mockCompactTimerOverlay.dismiss() }
        assertEquals("Current overlay should still be TIMER",
            OverlayManager.OverlayType.TIMER, overlayManager.getCurrentOverlayType())
    }

    // ========== ON TIMER DISMISSED TESTS ==========

    @Test
    fun `should clear overlay state when timer is dismissed`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // Show timer
        overlayManager.showTimer(123L, "com.example.app", 1000L, 5000L)

        // Reset mocks
        clearMocks(mockTimerOverlay, mockCompactTimerOverlay)

        // Call onTimerDismissed
        overlayManager.onTimerDismissed()

        verify { mockTimerOverlay.dismiss() }
        verify { mockCompactTimerOverlay.dismiss() }
        assertNull("Current overlay should be null", overlayManager.getCurrentOverlayType())
    }

    @Test
    fun `should not clear overlay state when reminder is showing and timer dismissed is called`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // Show reminder
        overlayManager.showReminder(123L, "com.example.app")

        // Reset mocks
        clearMocks(mockTimerOverlay, mockCompactTimerOverlay)

        // Call onTimerDismissed
        overlayManager.onTimerDismissed()

        // Should not dismiss reminder overlays
        verify(exactly = 0) { mockReminderOverlay.dismiss() }
        verify(exactly = 0) { mockCompactReminderOverlay.dismiss() }
        assertEquals("Current overlay should still be REMINDER",
            OverlayManager.OverlayType.REMINDER, overlayManager.getCurrentOverlayType())
    }

    // ========== IS ANY OVERLAY SHOWING TESTS ==========

    @Test
    fun `should return false when no overlay is showing`() {
        assertFalse("No overlay should be showing", overlayManager.isAnyOverlayShowing())
    }

    @Test
    fun `should return true when reminder is showing`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        overlayManager.showReminder(123L, "com.example.app")

        assertTrue("Reminder overlay should be showing", overlayManager.isAnyOverlayShowing())
    }

    @Test
    fun `should return true when timer is showing`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        overlayManager.showTimer(123L, "com.example.app", 1000L, 5000L)

        assertTrue("Timer overlay should be showing", overlayManager.isAnyOverlayShowing())
    }

    @Test
    fun `should return false after dismissing all overlays`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        overlayManager.showReminder(123L, "com.example.app")
        overlayManager.dismissAll()

        assertFalse("No overlay should be showing after dismiss", overlayManager.isAnyOverlayShowing())
    }

    // ========== GET CURRENT OVERLAY TYPE TESTS ==========

    @Test
    fun `should return null when no overlay is showing`() {
        assertNull("Current overlay type should be null", overlayManager.getCurrentOverlayType())
    }

    @Test
    fun `should return REMINDER when reminder is showing`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        overlayManager.showReminder(123L, "com.example.app")

        assertEquals("Current overlay should be REMINDER",
            OverlayManager.OverlayType.REMINDER, overlayManager.getCurrentOverlayType())
    }

    @Test
    fun `should return TIMER when timer is showing`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        overlayManager.showTimer(123L, "com.example.app", 1000L, 5000L)

        assertEquals("Current overlay should be TIMER",
            OverlayManager.OverlayType.TIMER, overlayManager.getCurrentOverlayType())
    }

    // ========== SHOW DEBUG REMINDER TESTS ==========

    @Test
    fun `should show debug reminder in fullscreen mode`() {
        overlayManager.showDebugReminder(isCompactMode = false)

        verify { mockReminderOverlay.show(-1L, "com.example.test") }
        verify(exactly = 0) { mockCompactReminderOverlay.show(any(), any()) }
    }

    @Test
    fun `should show debug reminder in compact mode`() {
        overlayManager.showDebugReminder(isCompactMode = true)

        verify { mockCompactReminderOverlay.show(-1L, "com.example.test") }
        verify(exactly = 0) { mockReminderOverlay.show(any(), any()) }
    }

    @Test
    fun `should dismiss existing overlay before showing debug reminder`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        // First show normal reminder
        overlayManager.showReminder(123L, "com.example.app")

        // Reset mocks
        clearMocks(mockReminderOverlay, mockCompactReminderOverlay, mockTimerOverlay, mockCompactTimerOverlay)

        // Show debug reminder - should dismiss all first
        overlayManager.showDebugReminder(isCompactMode = false)

        verify { mockReminderOverlay.dismiss() }
        verify { mockCompactReminderOverlay.dismiss() }
        verify { mockTimerOverlay.dismiss() }
        verify { mockCompactTimerOverlay.dismiss() }
    }

    // ========== OVERLAY STYLE SWITCHING TESTS ==========

    @Test
    fun `should switch from fullscreen to compact mode`() {
        // Start with fullscreen
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings(
            overlayStyle = OverlayStyle.FULLSCREEN
        )
        overlayManager.showReminder(123L, "com.example.app")
        verify { mockReminderOverlay.show(123L, "com.example.app") }

        // Dismiss and switch to compact
        overlayManager.dismissAll()
        clearMocks(mockReminderOverlay, mockCompactReminderOverlay)

        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings(
            overlayStyle = OverlayStyle.COMPACT
        )
        overlayManager.showReminder(456L, "com.example.app")

        verify { mockCompactReminderOverlay.show(456L, "com.example.app") }
    }

    // ========== EDGE CASES ==========

    @Test
    fun `should handle multiple dismiss all calls safely`() {
        every { mockSettingsRepository.getSettingsOnce() } returns UserSettings()

        overlayManager.showReminder(123L, "com.example.app")
        overlayManager.dismissAll()
        overlayManager.dismissAll()
        overlayManager.dismissAll()

        // Should not throw any exceptions
        assertNull("Current overlay should be null", overlayManager.getCurrentOverlayType())
    }

    @Test
    fun `should handle reminder dismissed callback when no overlay is showing`() {
        // Should not throw exception
        overlayManager.onReminderDismissed()
        assertNull("Current overlay should be null", overlayManager.getCurrentOverlayType())
    }

    @Test
    fun `should handle timer dismissed callback when no overlay is showing`() {
        // Should not throw exception
        overlayManager.onTimerDismissed()
        assertNull("Current overlay should be null", overlayManager.getCurrentOverlayType())
    }
}
