package dev.sadakat.thinkfaster.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class StreakFreezePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Freeze inventory management
    fun getFreezesAvailable(): Int = prefs.getInt(KEY_FREEZES_AVAILABLE, DEFAULT_MONTHLY_FREEZES)

    fun setFreezesAvailable(count: Int) {
        prefs.edit { putInt(KEY_FREEZES_AVAILABLE, count) }
    }

    fun useFreeze(): Boolean {
        val available = getFreezesAvailable()
        return if (available > 0) {
            setFreezesAvailable(available - 1)
            true
        } else {
            false
        }
    }

    // Monthly reset tracking
    fun getLastResetMonth(): String = prefs.getString(KEY_LAST_RESET_MONTH, "") ?: ""

    fun setLastResetMonth(yearMonth: String) {
        prefs.edit().putString(KEY_LAST_RESET_MONTH, yearMonth).apply()
    }

    // Active freeze tracking (per app)
    fun hasActiveFreezeForApp(targetApp: String): Boolean {
        return prefs.getBoolean("$KEY_FREEZE_ACTIVE_PREFIX$targetApp", false)
    }

    fun activateFreezeForApp(targetApp: String, date: String) {
        prefs.edit().apply {
            putBoolean("$KEY_FREEZE_ACTIVE_PREFIX$targetApp", true)
            putString("$KEY_FREEZE_DATE_PREFIX$targetApp", date)
            apply()
        }
    }

    fun deactivateFreezeForApp(targetApp: String) {
        prefs.edit().apply {
            remove("$KEY_FREEZE_ACTIVE_PREFIX$targetApp")
            remove("$KEY_FREEZE_DATE_PREFIX$targetApp")
            apply()
        }
    }

    fun getFreezeActivationDate(targetApp: String): String? {
        return prefs.getString("$KEY_FREEZE_DATE_PREFIX$targetApp", null)
    }

    // Premium readiness
    fun getMaxMonthlyFreezes(): Int {
        // Future: check premium status here
        // For now, return free tier amount
        return prefs.getInt(KEY_MAX_FREEZES, DEFAULT_MONTHLY_FREEZES)
    }

    fun setMaxMonthlyFreezes(count: Int) {
        prefs.edit().putInt(KEY_MAX_FREEZES, count).apply()
    }

    companion object {
        private const val PREFS_NAME = "streak_freeze_prefs"
        private const val KEY_FREEZES_AVAILABLE = "freezes_available"
        private const val KEY_LAST_RESET_MONTH = "last_reset_month"
        private const val KEY_MAX_FREEZES = "max_monthly_freezes"
        private const val KEY_FREEZE_ACTIVE_PREFIX = "freeze_active_"
        private const val KEY_FREEZE_DATE_PREFIX = "freeze_date_"
        const val DEFAULT_MONTHLY_FREEZES = 3  // Start with 3/month for free tier
    }
}
