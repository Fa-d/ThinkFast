package dev.sadakat.thinkfaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Complete Material 3 Typography Scale
 *
 * Provides 13 semantic text styles covering all content hierarchy needs:
 * - Display styles (57sp, 45sp, 36sp) - Hero content
 * - Headline styles (32sp, 28sp, 24sp) - Major sections
 * - Title styles (22sp, 16sp, 14sp) - Subsections
 * - Body styles (16sp, 14sp, 12sp) - Content
 * - Label styles (14sp, 12sp, 11sp) - UI elements
 *
 * See docs/Typography.md for detailed usage guidelines.
 */
val Typography = Typography(
    // ========================================================================
    // DISPLAY STYLES - Hero content, major headlines
    // ========================================================================
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // ========================================================================
    // HEADLINE STYLES - Screen titles, dialog headers
    // ========================================================================
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // ========================================================================
    // TITLE STYLES - Card headers, list section headers
    // ========================================================================
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ========================================================================
    // BODY STYLES - Primary content, descriptions, paragraphs
    // ========================================================================
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // ========================================================================
    // LABEL STYLES - Buttons, chips, tabs, UI elements
    // ========================================================================
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * iOS-inspired semantic typography extensions
 *
 * Provides familiar naming for developers coming from iOS, mapping to Material 3 styles.
 *
 * Usage:
 * ```
 * Text("Caption text", style = AppTypography.caption)
 * Text("Body text", style = AppTypography.body)
 * Text("Large Title", style = AppTypography.largeTitle)
 * ```
 *
 * See docs/Typography.md for iOS → Material 3 mapping details.
 */
object AppTypography {
    /** Caption text - 12sp (Maps to bodySmall) */
    val caption: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.bodySmall

    /** Footnote text - 12sp (Maps to labelMedium) */
    val footnote: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.labelMedium

    /** Body text - 16sp (Maps to bodyLarge) */
    val body: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.bodyLarge

    /** Headline text - 16sp Medium (Maps to titleMedium) */
    val headline: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.titleMedium

    /** Title 3 - 22sp (Maps to titleLarge) */
    val title3: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.titleLarge

    /** Title 2 - 24sp (Maps to headlineSmall) */
    val title2: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.headlineSmall

    /** Title 1 - 28sp (Maps to headlineMedium) */
    val title1: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.headlineMedium

    /** Large title - 36sp (Maps to displaySmall) */
    val largeTitle: TextStyle
        @Composable get() = androidx.compose.material3.MaterialTheme.typography.displaySmall
}

/**
 * Intervention overlay typography styles
 *
 * Research-backed font choices:
 * - Serif fonts → Contemplative, thoughtful (best for reflection)
 * - Monospace → Precise, factual (best for statistics)
 * - Sans-serif → Clear, direct (best for general content)
 */
object InterventionTypography {

    /**
     * Reflection questions - Serif for contemplative feel
     * Psychology: Serif fonts encourage slower, more thoughtful reading
     */
    val ReflectionQuestion = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )

    val ReflectionSubtext = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    )

    /**
     * Stats display - Monospace for precise data
     * Psychology: Monospace conveys precision and factual information
     */
    val StatsNumber = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = 0.sp
    )

    val StatsLabel = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.4.sp
    )

    val StatsMessage = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.15.sp
    )

    /**
     * General intervention content - Sans-serif for clarity
     */
    val InterventionTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )

    val InterventionMessage = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )

    val InterventionSubtext = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    )

    /**
     * Breathing exercise - Calm, centered
     */
    val BreathingInstruction = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )

    val BreathingPhase = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.15.sp
    )

    /**
     * Quote - Elegant serif
     */
    val QuoteText = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )

    val QuoteAuthor = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    )

    /**
     * Time alternatives - Clear, factual
     */
    val TimeAlternativePrefix = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.15.sp
    )

    val TimeAlternativeActivity = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )

    /**
     * Emotional appeals - Strong, direct
     */
    val EmotionalAppealMessage = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    )

    val EmotionalAppealSubtext = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.15.sp
    )

    /**
     * Gamification - Energetic, exciting
     */
    val GamificationChallenge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )

    val GamificationReward = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.15.sp
    )

    /**
     * Buttons
     */
    val ButtonText = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    )

    val ButtonTextSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    /**
     * App name display
     */
    val AppName = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    )
}