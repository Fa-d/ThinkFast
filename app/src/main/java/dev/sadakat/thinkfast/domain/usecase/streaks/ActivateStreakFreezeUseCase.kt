package dev.sadakat.thinkfast.domain.usecase.streaks

import android.util.Log
import dev.sadakat.thinkfast.data.preferences.StreakFreezePreferences

/**
 * Use case for activating a streak freeze for a specific app
 *
 * Logic:
 * 1. Check if freezes available
 * 2. Check if app already has active freeze
 * 3. Deduct freeze from inventory
 * 4. Activate freeze for specific app + date
 */
class ActivateStreakFreezeUseCase(
    private val freezePreferences: StreakFreezePreferences
) {
    sealed class Result {
        data object Success : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(targetApp: String, currentDate: String): Result {
        return try {
            // Check if freeze available
            if (freezePreferences.getFreezesAvailable() <= 0) {
                Log.w(TAG, "Attempted to use freeze but none available")
                return Result.Error("No freezes available this month")
            }

            // Check if already has active freeze
            if (freezePreferences.hasActiveFreezeForApp(targetApp)) {
                Log.w(TAG, "Attempted to use freeze but one already active for $targetApp")
                return Result.Error("Freeze already active for this app")
            }

            // Use a freeze
            val success = freezePreferences.useFreeze()
            if (!success) {
                Log.e(TAG, "Failed to deduct freeze from inventory")
                return Result.Error("Failed to use freeze")
            }

            // Activate freeze for app
            freezePreferences.activateFreezeForApp(targetApp, currentDate)

            Log.d(
                TAG,
                "Freeze activated for $targetApp on $currentDate. " +
                        "Remaining freezes: ${freezePreferences.getFreezesAvailable()}"
            )

            Result.Success
        } catch (e: Exception) {
            Log.e(TAG, "Exception while activating freeze", e)
            Result.Error("Failed to activate freeze: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ActivateStreakFreeze"
    }
}
