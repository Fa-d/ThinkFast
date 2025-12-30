package dev.sadakat.thinkfaster.util

import android.content.Context
import dev.sadakat.thinkfaster.ui.theme.ThemeMode
import androidx.core.content.edit

/**
 * Helper class for managing theme preferences
 * Phase 1.4: Visual Polish - Dark mode support
 */
object ThemePreferences {
    private const val PREFS_NAME = "think_fast_theme"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    private const val KEY_AMOLED_DARK = "amoled_dark"

    /**
     * Save theme mode preference
     */
    fun saveThemeMode(context: Context, themeMode: ThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_THEME_MODE, themeMode.name)
            }
    }

    /**
     * Get saved theme mode preference
     */
    fun getThemeMode(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(KEY_THEME_MODE, ThemeMode.FOLLOW_SYSTEM.name)
        return try {
            ThemeMode.valueOf(themeName ?: ThemeMode.FOLLOW_SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.FOLLOW_SYSTEM
        }
    }

    /**
     * Save dynamic color preference
     */
    fun saveDynamicColor(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_DYNAMIC_COLOR, enabled)
            }
    }

    /**
     * Get dynamic color preference
     */
    fun getDynamicColor(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DYNAMIC_COLOR, true)
    }

    /**
     * Save AMOLED dark mode preference
     */
    fun saveAmoledDark(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_AMOLED_DARK, enabled)
            }
    }

    /**
     * Get AMOLED dark mode preference
     */
    fun getAmoledDark(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AMOLED_DARK, false)
    }
}
