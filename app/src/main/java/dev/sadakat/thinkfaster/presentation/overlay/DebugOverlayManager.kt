package dev.sadakat.thinkfaster.presentation.overlay

import android.content.Context
import dev.sadakat.thinkfaster.domain.repository.SettingsRepository
import dev.sadakat.thinkfaster.service.OverlayManager
import dev.sadakat.thinkfaster.util.ErrorLogger

/**
 * Singleton manager for showing debug intervention overlays
 * Used for testing intervention UI from the settings screen
 */
object DebugOverlayManager {
    private var overlayManager: OverlayManager? = null
    private var reminderOverlay: ReminderOverlayWindow? = null
    private var compactReminderOverlay: CompactReminderOverlayWindow? = null

    /**
     * Initialize the debug overlay manager
     * Call this from the service when overlays are created
     */
    fun initialize(
        manager: OverlayManager,
        reminder: ReminderOverlayWindow,
        compactReminder: CompactReminderOverlayWindow
    ) {
        overlayManager = manager
        reminderOverlay = reminder
        compactReminderOverlay = compactReminder
        ErrorLogger.info("DebugOverlayManager initialized", context = "DebugOverlayManager")
    }

    /**
     * Show a debug reminder overlay with the currently selected intervention type
     *
     * @param context Application context
     * @param settingsRepository Settings repository to get overlay style preference
     */
    fun showDebugReminder(context: Context, settingsRepository: SettingsRepository) {
        ErrorLogger.info("showDebugReminder called from settings", context = "DebugOverlayManager")

        // Check if overlay permission is granted
        if (!dev.sadakat.thinkfaster.util.PermissionHelper.hasOverlayPermission(context)) {
            ErrorLogger.warning(
                "Cannot show debug overlay - overlay permission not granted",
                context = "DebugOverlayManager"
            )
            return
        }

        val manager = overlayManager
        if (manager == null) {
            ErrorLogger.error(
                IllegalStateException("OverlayManager not initialized. Make sure the app is running."),
                "DebugOverlayManager not initialized",
                context = "DebugOverlayManager"
            )
            return
        }

        // Get overlay style preference
        val isCompactMode = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.Main.immediate) {
            settingsRepository.getSettingsOnce().overlayStyle == dev.sadakat.thinkfaster.domain.model.OverlayStyle.COMPACT
        }

        // Show the overlay
        manager.showDebugReminder(isCompactMode)
    }

    /**
     * Check if the debug overlay manager is initialized
     */
    fun isInitialized(): Boolean {
        return overlayManager != null
    }
}
