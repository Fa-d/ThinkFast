package dev.sadakat.thinkfaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * @deprecated This object has been consolidated into AppColors.Progress
 *
 * Migration guide:
 * - ProgressColors.OnTrack → AppColors.Progress.OnTrack
 * - ProgressColors.Approaching → AppColors.Progress.Approaching
 * - ProgressColors.OverLimit → AppColors.Progress.OverLimit
 * - ProgressColors.Info → AppColors.Semantic.Info.Default
 * - ProgressColors.Achievement → AppColors.Progress.Achievement
 * - ProgressColors.getColorForProgress() → AppColors.Progress.getColorForPercentage()
 * - ProgressColors.Streak → AppColors.Streak
 *
 * See docs/Colors.md for the unified color system documentation.
 */
@Deprecated(
    message = "Use AppColors.Progress instead",
    replaceWith = ReplaceWith("AppColors.Progress", "dev.sadakat.thinkfaster.ui.theme.AppColors")
)
object ProgressColors {
    /**
     * Green - On track, under limit (0-75%)
     */
    @Deprecated("Use AppColors.Progress.OnTrack", ReplaceWith("AppColors.Progress.OnTrack"))
    val OnTrack = AppColors.Progress.OnTrack

    @Deprecated("Use AppColors.Semantic.Success.Light", ReplaceWith("AppColors.Semantic.Success.Light"))
    val OnTrackLight = AppColors.Semantic.Success.Light

    @Deprecated("Use AppColors.Semantic.Success.Dark", ReplaceWith("AppColors.Semantic.Success.Dark"))
    val OnTrackDark = AppColors.Semantic.Success.Dark

    /**
     * Yellow/Orange - Approaching limit (75-100%)
     */
    @Deprecated("Use AppColors.Progress.Approaching", ReplaceWith("AppColors.Progress.Approaching"))
    val Approaching = AppColors.Progress.Approaching

    @Deprecated("Use AppColors.Semantic.Warning.Light", ReplaceWith("AppColors.Semantic.Warning.Light"))
    val ApproachingLight = AppColors.Semantic.Warning.Light

    @Deprecated("Use AppColors.Semantic.Warning.Dark", ReplaceWith("AppColors.Semantic.Warning.Dark"))
    val ApproachingDark = AppColors.Semantic.Warning.Dark

    /**
     * Red - Over limit (>100%)
     */
    @Deprecated("Use AppColors.Progress.OverLimit", ReplaceWith("AppColors.Progress.OverLimit"))
    val OverLimit = AppColors.Progress.OverLimit

    @Deprecated("Use AppColors.Semantic.Error.Light", ReplaceWith("AppColors.Semantic.Error.Light"))
    val OverLimitLight = AppColors.Semantic.Error.Light

    @Deprecated("Use AppColors.Semantic.Error.Dark", ReplaceWith("AppColors.Semantic.Error.Dark"))
    val OverLimitDark = AppColors.Semantic.Error.Dark

    /**
     * Blue - Neutral/Info states
     */
    @Deprecated("Use AppColors.Semantic.Info.Default", ReplaceWith("AppColors.Semantic.Info.Default"))
    val Info = AppColors.Semantic.Info.Default

    @Deprecated("Use AppColors.Semantic.Info.Light", ReplaceWith("AppColors.Semantic.Info.Light"))
    val InfoLight = AppColors.Semantic.Info.Light

    @Deprecated("Use AppColors.Semantic.Info.Dark", ReplaceWith("AppColors.Semantic.Info.Dark"))
    val InfoDark = AppColors.Semantic.Info.Dark

    /**
     * Purple - Achievements and milestones
     */
    @Deprecated("Use AppColors.Progress.Achievement", ReplaceWith("AppColors.Progress.Achievement"))
    val Achievement = AppColors.Progress.Achievement

    @Deprecated("Use AppColors.Secondary.Light", ReplaceWith("AppColors.Secondary.Light"))
    val AchievementLight = AppColors.Secondary.Light

    @Deprecated("Use AppColors.Secondary.Dark", ReplaceWith("AppColors.Secondary.Dark"))
    val AchievementDark = AppColors.Secondary.Dark

    /**
     * Get progress color based on percentage
     */
    @Deprecated(
        "Use AppColors.Progress.getColorForPercentage()",
        ReplaceWith("AppColors.Progress.getColorForPercentage(percentage)")
    )
    fun getColorForProgress(percentage: Int): Color {
        return AppColors.Progress.getColorForPercentage(percentage)
    }

    /**
     * Get progress color with light variant based on percentage
     */
    @Deprecated("Use AppColors.Semantic variants", ReplaceWith("AppColors.Semantic.Success.Light"))
    fun getLightColorForProgress(percentage: Int): Color {
        return when {
            percentage <= 75 -> AppColors.Semantic.Success.Light
            percentage <= 100 -> AppColors.Semantic.Warning.Light
            else -> AppColors.Semantic.Error.Light
        }
    }

    /**
     * Get progress color with dark variant based on percentage
     */
    @Deprecated("Use AppColors.Semantic variants", ReplaceWith("AppColors.Semantic.Success.Dark"))
    fun getDarkColorForProgress(percentage: Int): Color {
        return when {
            percentage <= 75 -> AppColors.Semantic.Success.Dark
            percentage <= 100 -> AppColors.Semantic.Warning.Dark
            else -> AppColors.Semantic.Error.Dark
        }
    }

    /**
     * Streak milestone colors (for fire emoji backgrounds)
     */
    @Deprecated(
        "Use AppColors.Streak instead",
        ReplaceWith("AppColors.Streak", "dev.sadakat.thinkfaster.ui.theme.AppColors")
    )
    object Streak {
        @Deprecated("Use AppColors.Streak.Day1to6", ReplaceWith("AppColors.Streak.Day1to6"))
        val Day1to6 = AppColors.Streak.Day1to6

        @Deprecated("Use AppColors.Streak.Day7to13", ReplaceWith("AppColors.Streak.Day7to13"))
        val Day7to13 = AppColors.Streak.Day7to13

        @Deprecated("Use AppColors.Streak.Day14to29", ReplaceWith("AppColors.Streak.Day14to29"))
        val Day14to29 = AppColors.Streak.Day14to29

        @Deprecated("Use AppColors.Streak.Day30Plus", ReplaceWith("AppColors.Streak.Day30Plus"))
        val Day30Plus = AppColors.Streak.Day30Plus

        @Deprecated(
            "Use AppColors.Streak.getColorForStreak()",
            ReplaceWith("AppColors.Streak.getColorForStreak(days)")
        )
        fun getColorForStreak(days: Int): Color {
            return AppColors.Streak.getColorForStreak(days)
        }
    }

    /**
     * Goal progress colors
     */
    @Deprecated("Use AppColors.Progress or AppColors.Semantic", ReplaceWith("AppColors.Progress"))
    object Goal {
        @Deprecated("Use MaterialTheme.colorScheme.surfaceVariant", ReplaceWith("Color(0xFF9E9E9E)"))
        val NotSet = Color(0xFF9E9E9E) // Gray

        @Deprecated("Use AppColors.Semantic.Info.Default", ReplaceWith("AppColors.Semantic.Info.Default"))
        val InProgress = AppColors.Semantic.Info.Default

        @Deprecated("Use AppColors.Progress.OnTrack", ReplaceWith("AppColors.Progress.OnTrack"))
        val Achieved = AppColors.Progress.OnTrack

        @Deprecated("Use AppColors.Progress.OverLimit", ReplaceWith("AppColors.Progress.OverLimit"))
        val Exceeded = AppColors.Progress.OverLimit
    }
}
