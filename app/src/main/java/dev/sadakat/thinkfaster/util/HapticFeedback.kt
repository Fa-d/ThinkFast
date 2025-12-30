package dev.sadakat.thinkfaster.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptic feedback utility for key events
 * Phase 1.4: Visual Polish - Haptic feedback
 */
object HapticFeedback {

    /**
     * Provide light haptic feedback for UI interactions
     */
    fun light(context: Context) {
        vibrate(context, 10L)
    }

    /**
     * Provide medium haptic feedback for notifications
     */
    fun medium(context: Context) {
        vibrate(context, 25L)
    }

    /**
     * Provide heavy haptic feedback for important events
     */
    fun heavy(context: Context) {
        vibrate(context, 50L)
    }

    /**
     * Success haptic pattern (short-long)
     */
    fun success(context: Context) {
        vibratePattern(context, longArrayOf(0, 30, 50, 60))
    }

    /**
     * Error/Warning haptic pattern (long-short-short)
     */
    fun warning(context: Context) {
        vibratePattern(context, longArrayOf(0, 60, 40, 30, 40, 30))
    }

    /**
     * Celebration haptic pattern (multiple short bursts)
     */
    fun celebration(context: Context) {
        vibratePattern(context, longArrayOf(0, 20, 30, 20, 30, 20, 30, 40))
    }

    /**
     * Streak milestone haptic (escalating pattern)
     */
    fun streakMilestone(context: Context) {
        vibratePattern(context, longArrayOf(0, 40, 50, 50, 50, 60, 50, 80))
    }

    /**
     * Generic vibration helper
     */
    private fun vibrate(context: Context, durationMs: Long) {
        val vibrator = getVibrator(context) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    /**
     * Pattern vibration helper
     */
    private fun vibratePattern(context: Context, pattern: LongArray) {
        val vibrator = getVibrator(context) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    /**
     * Get vibrator service
     */
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
