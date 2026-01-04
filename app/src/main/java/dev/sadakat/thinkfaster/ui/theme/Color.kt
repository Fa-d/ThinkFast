package dev.sadakat.thinkfaster.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// LEGACY COLORS (Kept for backward compatibility)
// ============================================================================

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ============================================================================
// UNIFIED APP COLORS
// ============================================================================

/**
 * Unified color system for ThinkFast
 *
 * Consolidates all app colors into a single, well-organized structure:
 * - Primary (Blue) - Brand colors
 * - Secondary (Purple) - Accent colors
 * - Semantic - Success, Warning, Error, Info
 * - Progress - On track, approaching, over limit states
 * - Streak - Milestone colors for streaks
 * - Gradients - Pre-defined color gradients
 *
 * See docs/Colors.md for detailed usage guidelines.
 */
object AppColors {

    // ========================================================================
    // PRIMARY PALETTE (Blue - Brand)
    // ========================================================================

    object Primary {
        /** Primary blue - Blue 500 */
        val Default = Color(0xFF2196F3)

        /** Primary blue dark - Blue 800 */
        val Dark = Color(0xFF1565C0)

        /** Primary blue light - Blue 300 (for dark mode) */
        val Light = Color(0xFF64B5F6)

        /** Primary blue container - Blue 50 */
        val Container = Color(0xFFE3F2FD)

        /** On primary color - White */
        val OnPrimary = Color(0xFFFFFFFF)

        /** On primary container - Blue 900 */
        val OnContainer = Color(0xFF0D47A1)
    }

    // ========================================================================
    // SECONDARY PALETTE (Purple - Accent)
    // ========================================================================

    object Secondary {
        /** Secondary purple - Purple 500 */
        val Default = Color(0xFF9C27B0)

        /** Secondary purple dark - Purple 800 */
        val Dark = Color(0xFF7B1FA2)

        /** Secondary purple light - Purple 300 (for dark mode) */
        val Light = Color(0xFFBA68C8)

        /** Secondary purple container - Purple 50 */
        val Container = Color(0xFFF3E5F5)

        /** On secondary color - White */
        val OnSecondary = Color(0xFFFFFFFF)

        /** On secondary container - Purple 900 */
        val OnContainer = Color(0xFF4A148C)
    }

    // ========================================================================
    // SEMANTIC COLORS (Status & Feedback)
    // ========================================================================

    object Semantic {

        object Success {
            /** Success green - Green 500 */
            val Default = Color(0xFF4CAF50)

            /** Success green light - Green 300 */
            val Light = Color(0xFF81C784)

            /** Success green dark - Green 700 */
            val Dark = Color(0xFF388E3C)

            /** Success container - Green 50 */
            val Container = Color(0xFFE8F5E9)

            /** On success - White */
            val OnSuccess = Color(0xFFFFFFFF)

            /** On success container - Green 900 */
            val OnContainer = Color(0xFF1B5E20)
        }

        object Warning {
            /** Warning orange - Orange 500 */
            val Default = Color(0xFFFF9800)

            /** Warning orange light - Orange 300 */
            val Light = Color(0xFFFFB74D)

            /** Warning orange dark - Orange 700 */
            val Dark = Color(0xFFF57C00)

            /** Warning container - Orange 50 */
            val Container = Color(0xFFFFF3E0)

            /** On warning - White */
            val OnWarning = Color(0xFFFFFFFF)

            /** On warning container - Orange 900 */
            val OnContainer = Color(0xFFE65100)
        }

        object Error {
            /** Error red - Red 500 */
            val Default = Color(0xFFF44336)

            /** Error red light - Red 300 */
            val Light = Color(0xFFE57373)

            /** Error red dark - Red 700 */
            val Dark = Color(0xFFD32F2F)

            /** Error container - Red 50 */
            val Container = Color(0xFFFFEBEE)

            /** On error - White */
            val OnError = Color(0xFFFFFFFF)

            /** On error container - Red 900 */
            val OnContainer = Color(0xFFB71C1C)
        }

        object Info {
            /** Info blue - Blue 500 */
            val Default = Color(0xFF2196F3)

            /** Info blue light - Blue 300 */
            val Light = Color(0xFF64B5F6)

            /** Info blue dark - Blue 700 */
            val Dark = Color(0xFF1976D2)

            /** Info container - Blue 50 */
            val Container = Color(0xFFE3F2FD)

            /** On info - White */
            val OnInfo = Color(0xFFFFFFFF)

            /** On info container - Blue 900 */
            val OnContainer = Color(0xFF0D47A1)
        }
    }

    // ========================================================================
    // PROGRESS STATES
    // ========================================================================

    object Progress {
        /** Green - On track, under limit (0-75%) */
        val OnTrack = Semantic.Success.Default

        /** Yellow/Orange - Approaching limit (75-100%) */
        val Approaching = Color(0xFFFFA726)

        /** Red - Over limit (>100%) */
        val OverLimit = Color(0xFFFF5252)

        /** Blue - Neutral/Info states */
        val Neutral = Semantic.Info.Default

        /** Purple - Achievements and milestones */
        val Achievement = Secondary.Default

        /**
         * Get progress color based on percentage
         *
         * @param percentage Usage percentage (0-100+)
         * @return Semantic color: Green (0-75%), Orange (75-100%), Red (>100%)
         */
        fun getColorForPercentage(percentage: Int): Color {
            return when {
                percentage <= 75 -> OnTrack
                percentage <= 100 -> Approaching
                else -> OverLimit
            }
        }
    }

    // ========================================================================
    // STREAK MILESTONE COLORS
    // ========================================================================

    object Streak {
        /** Orange - Starting streaks (1-6 days) */
        val Day1to6 = Color(0xFFFF9800)

        /** Deep Orange - Building momentum (7-13 days) */
        val Day7to13 = Color(0xFFFF5722)

        /** Red - Getting serious (14-29 days) */
        val Day14to29 = Color(0xFFF44336)

        /** Purple - Major achievement! (30+ days) */
        val Day30Plus = Secondary.Default

        /**
         * Get streak color based on days
         *
         * @param days Current streak days
         * @return Color that intensifies with streak length
         */
        fun getColorForStreak(days: Int): Color {
            return when {
                days < 7 -> Day1to6
                days < 14 -> Day7to13
                days < 30 -> Day14to29
                else -> Day30Plus
            }
        }
    }

    // ========================================================================
    // GRADIENTS
    // ========================================================================

    object Gradients {
        /**
         * Primary gradient (Blue → Purple)
         * Usage: PrimaryButton background, hero sections
         */
        fun primary() = listOf(
            Primary.Default.copy(alpha = 0.9f),
            Secondary.Default.copy(alpha = 0.9f)
        )

        /**
         * Success gradient (Light Green → Green)
         * Usage: Achievement celebrations, positive feedback
         */
        fun success() = listOf(
            Semantic.Success.Light.copy(alpha = 0.8f),
            Semantic.Success.Default
        )

        /**
         * Warning gradient (Light Orange → Orange)
         * Usage: Approaching limit indicators
         */
        fun warning() = listOf(
            Semantic.Warning.Light.copy(alpha = 0.8f),
            Semantic.Warning.Default
        )

        /**
         * Error gradient (Light Red → Red)
         * Usage: Over limit warnings, destructive actions
         */
        fun error() = listOf(
            Semantic.Error.Light.copy(alpha = 0.8f),
            Semantic.Error.Default
        )

        /**
         * Helper to create linear gradient brush
         */
        fun primaryBrush() = Brush.linearGradient(primary())
        fun successBrush() = Brush.linearGradient(success())
        fun warningBrush() = Brush.linearGradient(warning())
        fun errorBrush() = Brush.linearGradient(error())
    }

    // ========================================================================
    // SPECIAL COLORS
    // ========================================================================

    /** Streak fire emoji color - Orange-red fire */
    val StreakFire = Color(0xFFFF6B35)
}

// ============================================================================
// DEPRECATED COLOR OBJECTS (For backward compatibility)
// ============================================================================

/**
 * @deprecated Use AppColors.Primary instead
 *
 * Migration:
 * - PrimaryColors.Blue500 → AppColors.Primary.Default
 * - PrimaryColors.Blue800 → AppColors.Primary.Dark
 * - PrimaryColors.Blue300 → AppColors.Primary.Light
 * - PrimaryColors.Blue50 → AppColors.Primary.Container
 * - PrimaryColors.OnPrimary → AppColors.Primary.OnPrimary
 * - PrimaryColors.OnPrimaryDark → AppColors.Primary.OnContainer
 */
@Deprecated(
    message = "Use AppColors.Primary instead",
    replaceWith = ReplaceWith("AppColors.Primary", "dev.sadakat.thinkfaster.ui.theme.AppColors")
)
object PrimaryColors {
    @Deprecated("Use AppColors.Primary.Default", ReplaceWith("AppColors.Primary.Default"))
    val Blue500 = AppColors.Primary.Default

    @Deprecated("Use AppColors.Primary.Dark", ReplaceWith("AppColors.Primary.Dark"))
    val Blue800 = AppColors.Primary.Dark

    @Deprecated("Use AppColors.Primary.Light", ReplaceWith("AppColors.Primary.Light"))
    val Blue300 = AppColors.Primary.Light

    @Deprecated("Use AppColors.Primary.Container", ReplaceWith("AppColors.Primary.Container"))
    val Blue50 = AppColors.Primary.Container

    @Deprecated("Use AppColors.Primary.OnPrimary", ReplaceWith("AppColors.Primary.OnPrimary"))
    val OnPrimary = AppColors.Primary.OnPrimary

    @Deprecated("Use AppColors.Primary.OnContainer", ReplaceWith("AppColors.Primary.OnContainer"))
    val OnPrimaryDark = AppColors.Primary.OnContainer
}

/**
 * @deprecated Use AppColors.Secondary instead
 *
 * Migration:
 * - SecondaryColors.Purple500 → AppColors.Secondary.Default
 * - SecondaryColors.Purple800 → AppColors.Secondary.Dark
 * - SecondaryColors.Purple300 → AppColors.Secondary.Light
 */
@Deprecated(
    message = "Use AppColors.Secondary instead",
    replaceWith = ReplaceWith("AppColors.Secondary", "dev.sadakat.thinkfaster.ui.theme.AppColors")
)
object SecondaryColors {
    @Deprecated("Use AppColors.Secondary.Default", ReplaceWith("AppColors.Secondary.Default"))
    val Purple500 = AppColors.Secondary.Default

    @Deprecated("Use AppColors.Secondary.Dark", ReplaceWith("AppColors.Secondary.Dark"))
    val Purple800 = AppColors.Secondary.Dark

    @Deprecated("Use AppColors.Secondary.Light", ReplaceWith("AppColors.Secondary.Light"))
    val Purple300 = AppColors.Secondary.Light

    @Deprecated("Use AppColors.Secondary.Container", ReplaceWith("AppColors.Secondary.Container"))
    val Purple50 = AppColors.Secondary.Container

    @Deprecated("Use AppColors.Secondary.OnSecondary", ReplaceWith("AppColors.Secondary.OnSecondary"))
    val OnSecondary = AppColors.Secondary.OnSecondary

    @Deprecated("Use AppColors.Secondary.OnContainer", ReplaceWith("AppColors.Secondary.OnContainer"))
    val OnSecondaryDark = AppColors.Secondary.OnContainer
}

/**
 * @deprecated Use AppColors.Semantic instead
 *
 * Migration:
 * - SemanticColors.Success → AppColors.Semantic.Success.Default
 * - SemanticColors.Warning → AppColors.Semantic.Warning.Default
 * - SemanticColors.Error → AppColors.Semantic.Error.Default
 * - SemanticColors.Info → AppColors.Semantic.Info.Default
 */
@Deprecated(
    message = "Use AppColors.Semantic instead",
    replaceWith = ReplaceWith("AppColors.Semantic", "dev.sadakat.thinkfaster.ui.theme.AppColors")
)
object SemanticColors {
    @Deprecated("Use AppColors.Semantic.Success.Default", ReplaceWith("AppColors.Semantic.Success.Default"))
    val Success = AppColors.Semantic.Success.Default

    @Deprecated("Use AppColors.Semantic.Success.Light", ReplaceWith("AppColors.Semantic.Success.Light"))
    val SuccessDark = AppColors.Semantic.Success.Light

    @Deprecated("Use AppColors.Semantic.Success.Container", ReplaceWith("AppColors.Semantic.Success.Container"))
    val SuccessLight = AppColors.Semantic.Success.Container

    @Deprecated("Use AppColors.Semantic.Success.OnSuccess", ReplaceWith("AppColors.Semantic.Success.OnSuccess"))
    val OnSuccess = AppColors.Semantic.Success.OnSuccess

    @Deprecated("Use AppColors.Semantic.Success.OnContainer", ReplaceWith("AppColors.Semantic.Success.OnContainer"))
    val OnSuccessDark = AppColors.Semantic.Success.OnContainer

    @Deprecated("Use AppColors.Semantic.Warning.Default", ReplaceWith("AppColors.Semantic.Warning.Default"))
    val Warning = AppColors.Semantic.Warning.Default

    @Deprecated("Use AppColors.Semantic.Warning.Light", ReplaceWith("AppColors.Semantic.Warning.Light"))
    val WarningDark = AppColors.Semantic.Warning.Light

    @Deprecated("Use AppColors.Semantic.Warning.Container", ReplaceWith("AppColors.Semantic.Warning.Container"))
    val WarningLight = AppColors.Semantic.Warning.Container

    @Deprecated("Use AppColors.Semantic.Warning.OnWarning", ReplaceWith("AppColors.Semantic.Warning.OnWarning"))
    val OnWarning = AppColors.Semantic.Warning.OnWarning

    @Deprecated("Use AppColors.Semantic.Warning.OnContainer", ReplaceWith("AppColors.Semantic.Warning.OnContainer"))
    val OnWarningDark = AppColors.Semantic.Warning.OnContainer

    @Deprecated("Use AppColors.Semantic.Error.Default", ReplaceWith("AppColors.Semantic.Error.Default"))
    val Error = AppColors.Semantic.Error.Default

    @Deprecated("Use AppColors.Semantic.Error.Light", ReplaceWith("AppColors.Semantic.Error.Light"))
    val ErrorDark = AppColors.Semantic.Error.Light

    @Deprecated("Use AppColors.Semantic.Error.Container", ReplaceWith("AppColors.Semantic.Error.Container"))
    val ErrorLight = AppColors.Semantic.Error.Container

    @Deprecated("Use AppColors.Semantic.Error.OnError", ReplaceWith("AppColors.Semantic.Error.OnError"))
    val OnError = AppColors.Semantic.Error.OnError

    @Deprecated("Use AppColors.Semantic.Error.OnContainer", ReplaceWith("AppColors.Semantic.Error.OnContainer"))
    val OnErrorDark = AppColors.Semantic.Error.OnContainer

    @Deprecated("Use AppColors.Semantic.Info.Default", ReplaceWith("AppColors.Semantic.Info.Default"))
    val Info = AppColors.Semantic.Info.Default

    @Deprecated("Use AppColors.Semantic.Info.Light", ReplaceWith("AppColors.Semantic.Info.Light"))
    val InfoDark = AppColors.Semantic.Info.Light

    @Deprecated("Use AppColors.Semantic.Info.Container", ReplaceWith("AppColors.Semantic.Info.Container"))
    val InfoLight = AppColors.Semantic.Info.Container

    @Deprecated("Use AppColors.Semantic.Info.OnInfo", ReplaceWith("AppColors.Semantic.Info.OnInfo"))
    val OnInfo = AppColors.Semantic.Info.OnInfo

    @Deprecated("Use AppColors.Semantic.Info.OnContainer", ReplaceWith("AppColors.Semantic.Info.OnContainer"))
    val OnInfoDark = AppColors.Semantic.Info.OnContainer
}

// ============================================================================
// INTERVENTION COLORS (Specialized, Keep Separate)
// ============================================================================

/**
 * Intervention overlay color scheme
 *
 * These colors are research-backed for psychological impact:
 * - Warm colors (amber/orange) → Caution without alarm
 * - Cool colors (blue) → Calm reflection
 * - Green → Nature, breathing, positive action
 * - Red → Urgency (used sparingly)
 *
 * NOTE: These colors are specialized for intervention overlays only.
 * Do NOT use for general app UI - use AppColors instead.
 */
object InterventionColors {

    // BACKGROUND COLORS (by content type)

    /**
     * Gentle reminder - Warm amber
     * Use for: Initial reminder overlay, general reflection questions
     * Psychology: Attention-getting without being alarming
     */
    val GentleReminderBackground = Color(0xFFFFF4E6)  // Light warm amber
    val GentleReminderBackgroundDark = Color(0xFF2D2420)  // Muted warm brown

    /**
     * Reflection - Calm blue
     * Use for: Reflection questions, emotional awareness
     * Psychology: Promotes calm, thoughtful consideration
     */
    val ReflectionBackground = Color(0xFFE3F2FD)  // Calm light blue
    val ReflectionBackgroundDark = Color(0xFF1A2538)  // Muted deep blue

    /**
     * Breathing - Nature green
     * Use for: Breathing exercises
     * Psychology: Associated with nature, calm, restoration
     */
    val BreathingBackground = Color(0xFFE8F5E9)  // Light nature green
    val BreathingBackgroundDark = Color(0xFF1A2F22)  // Muted deep forest green

    /**
     * Stats - Neutral purple
     * Use for: Usage statistics, data display
     * Psychology: Neutral, informative
     */
    val StatsBackground = Color(0xFFF3E5F5)  // Light purple
    val StatsBackgroundDark = Color(0xFF2D1F3D)  // Muted deep purple

    /**
     * Timer alert - Soft red
     * Use for: 10-minute timer alerts, time alternatives
     * Psychology: Urgency without panic
     */
    val TimerAlertBackground = Color(0xFFFFEBEE)  // Soft red/pink
    val TimerAlertBackgroundDark = Color(0xFF3D2028)  // Muted deep red

    /**
     * Urgent alert - Deep red
     * Use for: Extended sessions (15+ min), over-goal warnings
     * Psychology: Strong urgency, action required
     */
    val UrgentAlertBackground = Color(0xFFFFCDD2)  // Deeper pink/red
    val UrgentAlertBackgroundDark = Color(0xFF4A1F24)  // Muted dark red

    /**
     * Emotional appeal - Deep amber
     * Use for: Late-night warnings, compulsive behavior warnings
     * Psychology: Serious but supportive
     */
    val EmotionalAppealBackground = Color(0xFFFFE0B2)  // Deep amber
    val EmotionalAppealBackgroundDark = Color(0xFF3D2818)  // Muted deep amber

    /**
     * Quote - Soft purple
     * Use for: Inspirational quotes
     * Psychology: Wisdom, contemplation
     */
    val QuoteBackground = Color(0xFFEDE7F6)  // Soft purple
    val QuoteBackgroundDark = Color(0xFF251A42)  // Muted deep purple

    /**
     * Gamification - Vibrant blue
     * Use for: Streak celebrations, achievement challenges
     * Psychology: Excitement, achievement
     */
    val GamificationBackground = Color(0xFFE1F5FE)  // Bright light blue
    val GamificationBackgroundDark = Color(0xFF0A2A3D)  // Muted deep ocean blue

    /**
     * Activity suggestion - Soft teal/cyan
     * Use for: Time-of-day specific activity suggestions
     * Psychology: Action-oriented, positive, refreshing
     */
    val ActivitySuggestionBackground = Color(0xFFE0F2F1)  // Light cyan/teal
    val ActivitySuggestionBackgroundDark = Color(0xFF1A2F2E)  // Muted deep teal

    // BUTTON COLORS

    /**
     * Go Back button - Positive green
     * Psychology: Positive reinforcement for healthy choice
     */
    val GoBackButton = Color(0xFF4CAF50)  // Material Green 500
    val GoBackButtonDark = Color(0xFF66BB6A)  // Material Green 400 - better contrast

    /**
     * Proceed button - Neutral gray
     * Psychology: Less prominent, discourages clicking
     */
    val ProceedButton = Color(0xFF757575)  // Material Gray 600
    val ProceedButtonDark = Color(0xFFBDBDBD)  // Material Gray 400 - better contrast

    // TEXT COLORS

    /**
     * Primary text on intervention overlays
     * Dark colors optimized for better contrast on dark backgrounds
     */
    val InterventionTextPrimary = Color(0xFF212121)  // Near black
    val InterventionTextPrimaryDark = Color(0xFFF5F5F5)  // Off-white for better contrast

    /**
     * Secondary text (subtexts, explanations)
     */
    val InterventionTextSecondary = Color(0xFF616161)  // Medium gray
    val InterventionTextSecondaryDark = Color(0xFFB0BEC5)  // Light blue-gray for softer contrast

    // BRAND COLORS (for app identification)

    /**
     * Facebook brand blue
     */
    val Facebook = Color(0xFF1877F2)

    /**
     * Instagram gradient colors
     */
    val InstagramPink = Color(0xFFE4405F)
    val InstagramOrange = Color(0xFFF77737)
    val InstagramPurple = Color(0xFFC13584)

    // ACCENT COLORS (for intervention content)

    @Deprecated("Use AppColors.Semantic.Success.Default", ReplaceWith("AppColors.Semantic.Success.Default"))
    val Success = AppColors.Semantic.Success.Default

    @Deprecated("Use AppColors.Semantic.Success.Light", ReplaceWith("AppColors.Semantic.Success.Light"))
    val SuccessDark = AppColors.Semantic.Success.Light

    @Deprecated("Use AppColors.Semantic.Warning.Default", ReplaceWith("AppColors.Semantic.Warning.Default"))
    val Warning = AppColors.Semantic.Warning.Default

    @Deprecated("Use AppColors.Semantic.Warning.Light", ReplaceWith("AppColors.Semantic.Warning.Light"))
    val WarningDark = AppColors.Semantic.Warning.Light

    @Deprecated("Use AppColors.Semantic.Error.Default", ReplaceWith("AppColors.Semantic.Error.Default"))
    val Error = AppColors.Semantic.Error.Default

    @Deprecated("Use AppColors.Semantic.Error.Light", ReplaceWith("AppColors.Semantic.Error.Light"))
    val ErrorDark = AppColors.Semantic.Error.Light

    @Deprecated("Use AppColors.Semantic.Info.Default", ReplaceWith("AppColors.Semantic.Info.Default"))
    val GoalLine = AppColors.Semantic.Info.Default

    @Deprecated("Use AppColors.Semantic.Info.Light", ReplaceWith("AppColors.Semantic.Info.Light"))
    val GoalLineDark = AppColors.Semantic.Info.Light
}
