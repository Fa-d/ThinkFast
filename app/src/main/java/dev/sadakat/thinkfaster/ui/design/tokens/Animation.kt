package dev.sadakat.thinkfaster.ui.design.tokens

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing as ComposeEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp

/**
 * Animation tokens for Intently design system.
 *
 * Based on iOS reference with consistent duration and easing values.
 * Provides semantic animation specs for consistent motion across the app.
 */
object Animation {

    // ============================================================================
    // DURATION SCALE
    // ============================================================================

    /**
     * Duration tokens in milliseconds
     */
    object Duration {
        /** Instant duration - 50ms. Use for: instant feedback */
        const val Instant: Int = 50

        /** Fast duration - 200ms. Use for: button presses, toggles */
        const val Fast: Int = 200

        /** Normal duration - 300ms. Use for: screen transitions, fades */
        const val Normal: Int = 300

        /** Slow duration - 500ms. Use for: complex animations, layouts */
        const val Slow: Int = 500

        /** Slower duration - 800ms. Use for: celebrations, special effects */
        const val Slower: Int = 800
    }

    // ============================================================================
    // EASING CURVES
    // ============================================================================

    /**
     * Easing tokens
     */
    object Easing {
        /**
         * Standard easing - Cubic Bezier (0.4, 0.0, 0.2, 1.0).
         * Use for: standard transitions
         */
        val Standard: ComposeEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

        /**
         * Emphasized easing - Cubic Bezier (0.0, 0.0, 0.2, 1.0).
         * Use for: enter/exit animations
         */
        val Emphasized: ComposeEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

        /**
         * Decelerate easing - Cubic Bezier (0.0, 0.0, 0.2, 1.0).
         * Use for: elements leaving screen
         */
        val Decelerate: ComposeEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

        /**
         * Accelerate easing - Cubic Bezier (0.4, 0.0, 1.0, 1.0).
         * Use for: elements entering screen
         */
        val Accelerate: ComposeEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

        /**
         * Linear easing.
         * Use for: progress bars, loaders
         */
        val Linear: ComposeEasing = LinearEasing

        /**
         * Material FastOutSlowIn easing.
         * Use for: general purpose animations
         */
        val FastOutSlowIn: ComposeEasing = FastOutSlowInEasing

        /**
         * Material FastOutLinearIn easing.
         * Use for: elements entering with emphasis
         */
        val FastOutLinearIn: ComposeEasing = FastOutLinearInEasing

        /**
         * Material LinearOutSlowIn easing.
         * Use for: elements leaving with emphasis
         */
        val LinearOutSlowIn: ComposeEasing = LinearOutSlowInEasing
    }

    // ============================================================================
    // SCALE VALUES
    // ============================================================================

    /**
     * Scale values for press animations (iOS-style)
     */
    object Scale {
        /** Scale down value for button press (0.95) */
        const val PressDown: Float = 0.95f

        /** Scale up value for button release (1.0) */
        const val PressUp: Float = 1.0f
    }

    // ============================================================================
    // SPRING SPECIFICATIONS
    // ============================================================================

    /**
     * Predefined spring animation specs for common use cases
     */
    object Spring {
        /** Default spring - balanced response */
        val Default: SpringSpec<Float> = spring(
            dampingRatio = 0.8f,
            stiffness = Stiffness.High
        )

        /** Bouncy spring - playful animations */
        val Bouncy: SpringSpec<Float> = spring(
            dampingRatio = DampingRatio.Bouncy,
            stiffness = Stiffness.Medium
        )

        /** Smooth spring - no overshoot */
        val Smooth: SpringSpec<Float> = spring(
            dampingRatio = DampingRatio.NoBounce,
            stiffness = Stiffness.Low
        )
    }

    /**
     * Spring damping ratios for bounce effect
     */
    object DampingRatio {
        /** No bounce - critically damped */
        const val NoBounce: Float = 1f

        /** Low bounce - slight overshoot */
        const val LowBounce: Float = 0.9f

        /** Medium bounce - moderate overshoot */
        const val MediumBounce: Float = 0.7f

        /** High bounce - bouncy animation */
        const val Bouncy: Float = 0.5f
    }

    /**
     * Spring stiffness for how quickly animation completes
     */
    object Stiffness {
        /** Very low stiffness - slow spring */
        const val VeryLow: Float = 200f

        /** Low stiffness - relaxed spring */
        const val Low: Float = 400f

        /** Medium stiffness - default spring */
        const val Medium: Float = 600f

        /** High stiffness - snappy spring */
        const val High: Float = 800f

        /** Very high stiffness - instant spring */
        const val VeryHigh: Float = 1000f
    }

    // ============================================================================
    // ANIMATION SPECS
    // ============================================================================

    /**
     * Predefined animation specs for common use cases
     */

    /**
     * Fade in animation spec.
     * Duration: 300ms, Easing: Standard
     */
    val FadeInSpec: AnimationSpec<Float> = tween(
        durationMillis = Duration.Normal,
        easing = Easing.Standard
    )

    /**
     * Fade out animation spec.
     * Duration: 300ms, Easing: Standard
     */
    val FadeOutSpec: AnimationSpec<Float> = tween(
        durationMillis = Duration.Normal,
        easing = Easing.Standard
    )

    /**
     * Slide in animation spec.
     * Duration: 300ms, Easing: Emphasized
     */
    val SlideInSpec: AnimationSpec<Dp> = tween(
        durationMillis = Duration.Normal,
        easing = Easing.Emphasized
    )

    /**
     * Slide out animation spec.
     * Duration: 300ms, Easing: Emphasized
     */
    val SlideOutSpec: AnimationSpec<Dp> = tween(
        durationMillis = Duration.Normal,
        easing = Easing.Emphasized
    )

    /**
     * Button press animation spec.
     * Spring: damping 0.8, stiffness 800
     */
    val ButtonPressSpec: SpringSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = Stiffness.High
    )

    /**
     * Card appearance animation spec.
     * Duration: 300ms, Easing: Emphasized
     */
    val CardAppearanceSpec: AnimationSpec<Float> = tween(
        durationMillis = Duration.Normal,
        easing = Easing.Emphasized
    )

    /**
     * Dialog show/hide animation spec.
     * Duration: 300ms, Easing: Emphasized
     */
    val DialogSpec: AnimationSpec<Float> = tween(
        durationMillis = Duration.Normal,
        easing = Easing.Emphasized
    )

    /**
     * Progress update animation spec.
     * Duration: 200ms, Easing: Linear
     */
    val ProgressUpdateSpec: AnimationSpec<Float> = tween(
        durationMillis = Duration.Fast,
        easing = Easing.Linear
    )

    /**
     * Celebration bounce animation spec.
     * Spring: damping 0.5, stiffness 400
     */
    val CelebrationBounceSpec: SpringSpec<Float> = spring(
        dampingRatio = DampingRatio.Bouncy,
        stiffness = Stiffness.Low
    )

    /**
     * Achievement pop animation spec.
     * Spring: damping 0.6, stiffness 600
     */
    val AchievementPopSpec: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = Stiffness.Medium
    )

    /**
     * Loading shimmer animation spec.
     * Duration: 1500ms, Easing: Linear, Infinite
     */
    val ShimmerSpec: AnimationSpec<Float> = infiniteRepeatable(
        animation = tween(
            durationMillis = 1500,
            easing = Easing.Linear
        ),
        repeatMode = RepeatMode.Restart
    )

    /**
     * Pulse indicator animation spec.
     * Duration: 1000ms, Easing: Linear, Reversable
     */
    val PulseSpec: AnimationSpec<Float> = infiniteRepeatable(
        animation = tween(
            durationMillis = 1000,
            easing = Easing.Linear
        ),
        repeatMode = RepeatMode.Reverse
    )

    /**
     * Stagger animation spec for list items.
     * Duration: 300ms, Easing: Emphasized
     */
    val StaggerSpec: AnimationSpec<Float> = tween(
        durationMillis = Duration.Normal,
        easing = Easing.Emphasized
    )

    /**
     * Scale in animation spec.
     * Duration: 200ms, Easing: Emphasized
     */
    val ScaleInSpec: AnimationSpec<Float> = tween(
        durationMillis = Duration.Fast,
        easing = Easing.Emphasized
    )

    /**
     * Scale out animation spec.
     * Duration: 200ms, Easing: Accelerate
     */
    val ScaleOutSpec: AnimationSpec<Float> = tween(
        durationMillis = Duration.Fast,
        easing = Easing.Accelerate
    )

    // ============================================================================
    // HELPER FUNCTIONS
    // ============================================================================

    /**
     * Creates a tween animation spec with standard easing.
     *
     * @param duration Duration in milliseconds (default: 300ms)
     * @param easing Easing function (default: Standard)
     */
    fun tweenSpec(
        duration: Int = Duration.Normal,
        easing: ComposeEasing = Easing.Standard
    ): TweenSpec<Float> = tween(durationMillis = duration, easing = easing)

    /**
     * Creates a spring animation spec.
     *
     * @param dampingRatio Damping ratio (default: 0.8 - low bounce)
     * @param stiffness Stiffness (default: 800 - snappy)
     */
    fun springSpec(
        dampingRatio: Float = 0.8f,
        stiffness: Float = Stiffness.High
    ): SpringSpec<Float> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    /**
     * Creates a repeatable animation spec.
     *
     * @param duration Duration of one iteration
     * @param repeatMode How to repeat (Restart or Reverse)
     * @param iterations Number of iterations (default: 3)
     */
    fun repeatableSpec(
        duration: Int = Duration.Normal,
        repeatMode: RepeatMode = RepeatMode.Restart,
        iterations: Int = 3
    ): AnimationSpec<Float> = repeatable(
        iterations = iterations,
        animation = tween(durationMillis = duration),
        repeatMode = repeatMode
    )

    /**
     * Creates an infinite repeatable animation spec.
     *
     * @param duration Duration of one iteration
     * @param repeatMode How to repeat (Restart or Reverse)
     */
    fun infiniteRepeatableSpec(
        duration: Int = 1000,
        repeatMode: RepeatMode = RepeatMode.Restart
    ): AnimationSpec<Float> = infiniteRepeatable(
        animation = tween(durationMillis = duration),
        repeatMode = repeatMode
    )

    // ============================================================================
    // ANIMATION DELAYS
    // ============================================================================

    /**
     * Delay tokens for staggered animations
     */
    object Delay {
        /** No delay */
        const val None: Int = 0

        /** Short delay - 50ms */
        const val Short: Int = 50

        /** Medium delay - 100ms */
        const val Medium: Int = 100

        /** Long delay - 150ms */
        const val Long: Int = 150

        /** Extra long delay - 200ms */
        const val ExtraLong: Int = 200
    }

    // ============================================================================
    // ACCESSIBILITY
    // ============================================================================

    /**
     * Animation specs for users who prefer reduced motion.
     * These specs use instant duration with no easing.
     */
    object ReducedMotion {
        /** Instant animation for reduced motion preference */
        val InstantSpec: AnimationSpec<Float> = tween(
            durationMillis = Duration.Instant,
            easing = Easing.Linear
        )

        /**
         * Returns the appropriate animation spec based on reduced motion preference.
         *
         * @param reducedMotion Whether user prefers reduced motion
         * @param normalSpec Normal animation spec to use if reduced motion is disabled
         */
        fun spec(reducedMotion: Boolean, normalSpec: AnimationSpec<Float>): AnimationSpec<Float> =
            if (reducedMotion) InstantSpec else normalSpec
    }
}

// ============================================================================
// TYPE ALIASES FOR CONVENIENCE
// ============================================================================

/** Type alias for float animation spec */
typealias FloatAnimationSpec = AnimationSpec<Float>

/** Type alias for DP animation spec */
typealias DpAnimationSpec = AnimationSpec<Dp>

/** Type alias for size animation spec */
typealias SizeAnimationSpec = AnimationSpec<androidx.compose.ui.geometry.Size>
