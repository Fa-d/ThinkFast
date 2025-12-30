package dev.sadakat.thinkfaster.util

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {

    /**
     * Check if the app has PACKAGE_USAGE_STATS permission
     * This permission is special and requires user to grant it in Settings
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if the app has SYSTEM_ALERT_WINDOW permission (overlay permission)
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Check if the app has POST_NOTIFICATIONS permission (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Notifications are allowed by default on older versions
            true
        }
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasUsageStatsPermission(context) &&
                hasOverlayPermission(context) &&
                hasNotificationPermission(context)
    }

    /**
     * Get an intent to open the Usage Stats settings screen
     */
    fun getUsageStatsPermissionIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    /**
     * Get an intent to open the Overlay permission settings screen
     */
    fun getOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    /**
     * Get an intent to open the app's settings screen
     */
    fun getAppSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    }

    /**
     * Check which permissions are missing
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()

        if (!hasUsageStatsPermission(context)) {
            missing.add("Usage Stats")
        }

        if (!hasOverlayPermission(context)) {
            missing.add("Display Over Other Apps")
        }

        if (!hasNotificationPermission(context)) {
            missing.add("Notifications")
        }

        return missing
    }

    /**
     * Get a user-friendly description for why each permission is needed
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            "Usage Stats" -> "Required to detect when you open Facebook or Instagram and track your usage time"
            "Display Over Other Apps" -> "Required to show reminder overlays when you open social media apps"
            "Notifications" -> "Required to keep the monitoring service running in the background"
            else -> "Required for the app to function properly"
        }
    }
}
