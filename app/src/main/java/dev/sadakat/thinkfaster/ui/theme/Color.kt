package dev.sadakat.thinkfaster.ui.theme

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
// PRIMARY PALETTE (BLUE - Based on iOS reference)
// ============================================================================

/**
 * Blue primary colors - Main brand color
 */
object PrimaryColors {
    /** Primary blue - Blue 500 */
    val Blue500 = Color(0xFF2196F3)

    /** Primary blue dark - Blue 800 */
    val Blue800 = Color(0xFF1565C0)

    /** Primary blue dark - Blue 300 (for dark mode) */
    val Blue300 = Color(0xFF64B5F6)

    /** Primary blue container - Blue 50 */
    val Blue50 = Color(0xFFE3F2FD)

    /** On primary color - White */
    val OnPrimary = Color(0xFFFFFFFF)

    /** On primary dark - Blue 900 */
    val OnPrimaryDark = Color(0xFF0D47A1)
}

/**
 * Secondary palette (Purple)
 */
object SecondaryColors {
    /** Secondary purple - Purple 500 */
    val Purple500 = Color(0xFF9C27B0)

    /** Secondary purple dark - Purple 800 */
    val Purple800 = Color(0xFF7B1FA2)

    /** Secondary purple light - Purple 300 (for dark mode) */
    val Purple300 = Color(0xFFBA68C8)

    /** Secondary purple container - Purple 50 */
    val Purple50 = Color(0xFFF3E5F5)

    /** On secondary color - White */
    val OnSecondary = Color(0xFFFFFFFF)

    /** On secondary dark - Purple 900 */
    val OnSecondaryDark = Color(0xFF4A148C)
}

// ============================================================================
// SEMANTIC COLORS
// ============================================================================

/**
 * Semantic colors for status and feedback
 */
object SemanticColors {

    /**
     * Success/Achievement - Green
     */
    val Success = Color(0xFF4CAF50)  // Green 500
    val SuccessDark = Color(0xFF81C784)  // Green 300 (for dark mode)
    val SuccessLight = Color(0xFFE8F5E9)  // Green 50 (container)
    val OnSuccess = Color(0xFFFFFFFF)  // White text
    val OnSuccessDark = Color(0xFF1B5E20)  // Green 900 text

    /**
     * Warning/Caution - Orange
     */
    val Warning = Color(0xFFFF9800)  // Orange 500
    val WarningDark = Color(0xFFFFB74D)  // Orange 400 (for dark mode)
    val WarningLight = Color(0xFFFFF3E0)  // Orange 50 (container)
    val OnWarning = Color(0xFFFFFFFF)  // White text
    val OnWarningDark = Color(0xFFE65100)  // Orange 900 text

    /**
     * Error/Over limit - Red
     */
    val Error = Color(0xFFF44336)  // Red 500
    val ErrorDark = Color(0xFFE57373)  // Red 300 (for dark mode)
    val ErrorLight = Color(0xFFFFEBEE)  // Red 50 (container)
    val OnError = Color(0xFFFFFFFF)  // White text
    val OnErrorDark = Color(0xFFB71C1C)  // Red 900 text

    /**
     * Informational - Blue
     */
    val Info = Color(0xFF2196F3)  // Blue 500
    val InfoDark = Color(0xFF64B5F6)  // Blue 300 (for dark mode)
    val InfoLight = Color(0xFFE3F2FD)  // Blue 50 (container)
    val OnInfo = Color(0xFFFFFFFF)  // White text
    val OnInfoDark = Color(0xFF0D47A1)  // Blue 900 text
}

/**
 * Intervention overlay color scheme
 *
 * These colors are research-backed for psychological impact:
 * - Warm colors (amber/orange) → Caution without alarm
 * - Cool colors (blue) → Calm reflection
 * - Green → Nature, breathing, positive action
 * - Red → Urgency (used sparingly)
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

    // ACCENT COLORS

    /**
     * Success/Achievement
     */
    val Success = Color(0xFF4CAF50)  // Green
    val SuccessDark = Color(0xFF81C784)

    /**
     * Warning/Caution
     */
    val Warning = Color(0xFFFF9800)  // Orange
    val WarningDark = Color(0xFFFFB74D)

    /**
     * Error/Over limit
     */
    val Error = Color(0xFFF44336)  // Red
    val ErrorDark = Color(0xFFE57373)

    /**
     * Goal line/target
     */
    val GoalLine = Color(0xFF2196F3)  // Blue
    val GoalLineDark = Color(0xFF64B5F6)
}

/**
 * App-specific semantic colors
 */
object AppColors {
    /**
     * Progress states
     */
    val OnTrack = InterventionColors.Success  // Green - under goal
    val ApproachingLimit = InterventionColors.Warning  // Yellow - near goal
    val OverLimit = InterventionColors.Error  // Red - over goal

    /**
     * Streak fire emoji color
     */
    val StreakFire = Color(0xFFFF6B35)  // Orange-red fire
}