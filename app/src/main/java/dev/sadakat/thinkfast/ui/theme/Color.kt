package dev.sadakat.thinkfast.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

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
    val GentleReminderBackgroundDark = Color(0xFF3E2723)  // Dark warm brown

    /**
     * Reflection - Calm blue
     * Use for: Reflection questions, emotional awareness
     * Psychology: Promotes calm, thoughtful consideration
     */
    val ReflectionBackground = Color(0xFFE3F2FD)  // Calm light blue
    val ReflectionBackgroundDark = Color(0xFF1A237E)  // Deep blue

    /**
     * Breathing - Nature green
     * Use for: Breathing exercises
     * Psychology: Associated with nature, calm, restoration
     */
    val BreathingBackground = Color(0xFFE8F5E9)  // Light nature green
    val BreathingBackgroundDark = Color(0xFF1B5E20)  // Deep forest green

    /**
     * Stats - Neutral purple
     * Use for: Usage statistics, data display
     * Psychology: Neutral, informative
     */
    val StatsBackground = Color(0xFFF3E5F5)  // Light purple
    val StatsBackgroundDark = Color(0xFF4A148C)  // Deep purple

    /**
     * Timer alert - Soft red
     * Use for: 10-minute timer alerts, time alternatives
     * Psychology: Urgency without panic
     */
    val TimerAlertBackground = Color(0xFFFFEBEE)  // Soft red/pink
    val TimerAlertBackgroundDark = Color(0xFFB71C1C)  // Deep red

    /**
     * Urgent alert - Deep red
     * Use for: Extended sessions (15+ min), over-goal warnings
     * Psychology: Strong urgency, action required
     */
    val UrgentAlertBackground = Color(0xFFFFCDD2)  // Deeper pink/red
    val UrgentAlertBackgroundDark = Color(0xFF8B0000)  // Dark red

    /**
     * Emotional appeal - Deep amber
     * Use for: Late-night warnings, compulsive behavior warnings
     * Psychology: Serious but supportive
     */
    val EmotionalAppealBackground = Color(0xFFFFE0B2)  // Deep amber
    val EmotionalAppealBackgroundDark = Color(0xFFE65100)  // Deep orange

    /**
     * Quote - Soft purple
     * Use for: Inspirational quotes
     * Psychology: Wisdom, contemplation
     */
    val QuoteBackground = Color(0xFFEDE7F6)  // Soft purple
    val QuoteBackgroundDark = Color(0xFF311B92)  // Deep purple

    /**
     * Gamification - Vibrant blue
     * Use for: Streak celebrations, achievement challenges
     * Psychology: Excitement, achievement
     */
    val GamificationBackground = Color(0xFFE1F5FE)  // Bright light blue
    val GamificationBackgroundDark = Color(0xFF01579B)  // Deep ocean blue

    // BUTTON COLORS

    /**
     * Go Back button - Positive green
     * Psychology: Positive reinforcement for healthy choice
     */
    val GoBackButton = Color(0xFF4CAF50)  // Material Green 500
    val GoBackButtonDark = Color(0xFF81C784)  // Material Green 300

    /**
     * Proceed button - Neutral gray
     * Psychology: Less prominent, discourages clicking
     */
    val ProceedButton = Color(0xFF757575)  // Material Gray 600
    val ProceedButtonDark = Color(0xFF9E9E9E)  // Material Gray 500

    // TEXT COLORS

    /**
     * Primary text on intervention overlays
     */
    val InterventionTextPrimary = Color(0xFF212121)  // Near black
    val InterventionTextPrimaryDark = Color(0xFFE0E0E0)  // Light gray

    /**
     * Secondary text (subtexts, explanations)
     */
    val InterventionTextSecondary = Color(0xFF616161)  // Medium gray
    val InterventionTextSecondaryDark = Color(0xFFBDBDBD)  // Light gray

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