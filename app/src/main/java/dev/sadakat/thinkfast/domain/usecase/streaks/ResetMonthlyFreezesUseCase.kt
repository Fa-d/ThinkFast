package dev.sadakat.thinkfast.domain.usecase.streaks

import android.util.Log
import dev.sadakat.thinkfast.data.preferences.StreakFreezePreferences

/**
 * Use case for resetting monthly freeze inventory
 * Called by DailyStatsAggregatorWorker every midnight
 * Only resets if we're in a new month
 */
class ResetMonthlyFreezesUseCase(
    private val freezePreferences: StreakFreezePreferences
) {
    suspend operator fun invoke(currentYearMonth: String) {
        val lastResetMonth = freezePreferences.getLastResetMonth()

        // Only reset if we're in a new month
        if (lastResetMonth != currentYearMonth) {
            val maxFreezes = freezePreferences.getMaxMonthlyFreezes()
            freezePreferences.setFreezesAvailable(maxFreezes)
            freezePreferences.setLastResetMonth(currentYearMonth)
            Log.d(
                TAG,
                "Monthly freezes reset to $maxFreezes for $currentYearMonth " +
                        "(was last reset in: $lastResetMonth)"
            )
        } else {
            Log.d(TAG, "No freeze reset needed - still in month $currentYearMonth")
        }
    }

    companion object {
        private const val TAG = "ResetMonthlyFreezes"
    }
}
