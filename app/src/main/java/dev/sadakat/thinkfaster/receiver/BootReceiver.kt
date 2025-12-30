package dev.sadakat.thinkfaster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dev.sadakat.thinkfaster.service.UsageMonitorService

/**
 * Broadcast receiver that automatically restarts the UsageMonitorService
 * when the device boots up
 *
 * Requires RECEIVE_BOOT_COMPLETED permission in AndroidManifest.xml
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Boot completed or package replaced - starting UsageMonitorService")

                // Check if monitoring should be enabled (could add preference check here)
                if (shouldStartMonitoring(context)) {
                    startMonitoringService(context)
                }
            }
        }
    }

    private fun shouldStartMonitoring(context: Context): Boolean {
        // For now, always start monitoring after boot
        // In the future, could check SharedPreferences for user preference
        // e.g., "auto_start_monitoring" setting
        return true
    }

    private fun startMonitoringService(context: Context) {
        try {
            val serviceIntent = Intent(context, UsageMonitorService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ requires startForegroundService
                context.startForegroundService(serviceIntent)
                Log.d(TAG, "UsageMonitorService started via startForegroundService")
            } else {
                context.startService(serviceIntent)
                Log.d(TAG, "UsageMonitorService started via startService")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start UsageMonitorService on boot", e)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
