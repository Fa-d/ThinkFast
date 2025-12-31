package dev.sadakat.thinkfaster.analytics

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.util.Log

/**
 * Observes app lifecycle to track session analytics
 *
 * Uses ProcessLifecycleOwner to detect:
 * - App foreground (session start)
 * - App background (session end)
 *
 * Registered in ThinkFasterApplication.onCreate()
 */
class AppLifecycleObserver(
    private val analyticsManager: AnalyticsManager
) : DefaultLifecycleObserver {

    private var sessionStartTime: Long = 0L

    /**
     * Called when app comes to foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        sessionStartTime = System.currentTimeMillis()
        Log.d(TAG, "App entered foreground - session started")
    }

    /**
     * Called when app goes to background
     * Tracks session end with duration
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)

        if (sessionStartTime > 0) {
            val sessionDuration = System.currentTimeMillis() - sessionStartTime
            analyticsManager.trackSessionEnd(sessionDuration)
            Log.d(TAG, "App entered background - session ended (duration: ${sessionDuration}ms)")
        }
    }

    companion object {
        private const val TAG = "AppLifecycleObserver"
    }
}
