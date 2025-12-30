package dev.sadakat.thinkfaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette for progress states
 * Phase 1.4: Visual Polish - Progress state colors
 */
object ProgressColors {
    /**
     * Green - On track, under limit (0-75%)
     */
    val OnTrack = Color(0xFF4CAF50)
    val OnTrackLight = Color(0xFF81C784)
    val OnTrackDark = Color(0xFF388E3C)

    /**
     * Yellow/Orange - Approaching limit (75-100%)
     */
    val Approaching = Color(0xFFFFA726)
    val ApproachingLight = Color(0xFFFFB74D)
    val ApproachingDark = Color(0xFFF57C00)

    /**
     * Red - Over limit (>100%)
     */
    val OverLimit = Color(0xFFFF5252)
    val OverLimitLight = Color(0xFFFF8A80)
    val OverLimitDark = Color(0xFFD32F2F)

    /**
     * Blue - Neutral/Info states
     */
    val Info = Color(0xFF2196F3)
    val InfoLight = Color(0xFF64B5F6)
    val InfoDark = Color(0xFF1976D2)

    /**
     * Purple - Achievements and milestones
     */
    val Achievement = Color(0xFF9C27B0)
    val AchievementLight = Color(0xFFBA68C8)
    val AchievementDark = Color(0xFF7B1FA2)

    /**
     * Get progress color based on percentage
     */
    fun getColorForProgress(percentage: Int): Color {
        return when {
            percentage <= 75 -> OnTrack
            percentage <= 100 -> Approaching
            else -> OverLimit
        }
    }

    /**
     * Get progress color with light variant based on percentage
     */
    fun getLightColorForProgress(percentage: Int): Color {
        return when {
            percentage <= 75 -> OnTrackLight
            percentage <= 100 -> ApproachingLight
            else -> OverLimitLight
        }
    }

    /**
     * Get progress color with dark variant based on percentage
     */
    fun getDarkColorForProgress(percentage: Int): Color {
        return when {
            percentage <= 75 -> OnTrackDark
            percentage <= 100 -> ApproachingDark
            else -> OverLimitDark
        }
    }

    /**
     * Streak milestone colors (for fire emoji backgrounds)
     */
    object Streak {
        val Day1to6 = Color(0xFFFF9800) // Orange
        val Day7to13 = Color(0xFFFF5722) // Deep Orange
        val Day14to29 = Color(0xFFF44336) // Red
        val Day30Plus = Color(0xFF9C27B0) // Purple (special!)

        fun getColorForStreak(days: Int): Color {
            return when {
                days < 7 -> Day1to6
                days < 14 -> Day7to13
                days < 30 -> Day14to29
                else -> Day30Plus
            }
        }
    }

    /**
     * Goal progress colors
     */
    object Goal {
        val NotSet = Color(0xFF9E9E9E) // Gray
        val InProgress = Info
        val Achieved = OnTrack
        val Exceeded = OverLimit
    }
}
