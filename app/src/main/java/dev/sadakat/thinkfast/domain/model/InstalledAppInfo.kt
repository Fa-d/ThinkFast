package dev.sadakat.thinkfast.domain.model

import android.graphics.drawable.Drawable

/**
 * Information about an installed app on the device
 * Used for displaying apps in the selection screen
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,  // Nullable for safety if icon loading fails
    val category: AppCategory,
    val isInstalled: Boolean = true  // False if app was tracked but later uninstalled
)
