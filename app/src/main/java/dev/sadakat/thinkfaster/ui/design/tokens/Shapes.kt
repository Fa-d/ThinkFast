package dev.sadakat.thinkfaster.ui.design.tokens

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shape tokens for ThinkFast design system.
 *
 * Based on iOS reference with a consistent corner radius scale.
 * Provides semantic shape values for consistent component styling.
 */
object Shapes {

    // ============================================================================
    // CORNER SIZE DATA CLASS
    // ============================================================================

    /**
     * Wrapper class for corner size values to provide type safety.
     */
    data class DpCornerSize(val value: Dp)

    // ============================================================================
    // CORNER RADIUS SCALE
    // ============================================================================

    /**
     * Small corner radius - 8dp.
     * Use for: buttons, input fields, small elements, chips
     */
    val sm: DpCornerSize = DpCornerSize(8.dp)

    /**
     * Medium corner radius - 12dp.
     * Use for: cards, chips, toggle buttons, primary buttons
     */
    val md: DpCornerSize = DpCornerSize(12.dp)

    /**
     * Large corner radius - 16dp.
     * Use for: large cards, bottom sheets
     */
    val lg: DpCornerSize = DpCornerSize(16.dp)

    /**
     * Extra large corner radius - 24dp.
     * Use for: dialogs, popups, modals
     */
    val xl: DpCornerSize = DpCornerSize(24.dp)

    /**
     * Full corner radius - 9999dp (effectively a circle).
     * Use for: pills, badges, circles, avatars
     */
    val full: DpCornerSize = DpCornerSize(9999.dp)

    // ============================================================================
    // COMPONENT SHAPES
    // ============================================================================

    /**
     * Shape for primary and secondary buttons.
     * Corner radius: 12dp
     */
    val button: CornerBasedShape = RoundedCornerShape(12.dp)

    /**
     * Shape for text buttons.
     * Corner radius: 8dp
     */
    val textButton: CornerBasedShape = RoundedCornerShape(8.dp)

    /**
     * Shape for standard cards.
     * Corner radius: 16dp
     */
    val card: CornerBasedShape = RoundedCornerShape(16.dp)

    /**
     * Shape for elevated cards.
     * Corner radius: 16dp
     */
    val elevatedCard: CornerBasedShape = RoundedCornerShape(16.dp)

    /**
     * Shape for bottom sheets.
     * Corner radius: 24dp (top only)
     */
    val bottomSheet: CornerBasedShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    /**
     * Shape for dialogs.
     * Corner radius: 24dp
     */
    val dialog: CornerBasedShape = RoundedCornerShape(24.dp)

    /**
     * Shape for chips, filter chips, and similar pill-shaped elements.
     * Corner radius: 8dp
     */
    val chip: CornerBasedShape = RoundedCornerShape(8.dp)

    /**
     * Shape for badges and indicators.
     * Corner radius: Full (circle/pill)
     */
    val badge: CornerBasedShape = RoundedCornerShape(9999.dp)

    /**
     * Shape for avatars and circular images.
     * Corner radius: Full (circle)
     */
    val avatar: Shape = RoundedCornerShape(9999.dp)

    /**
     * Shape for progress bars.
     * Corner radius: Full (pill shape)
     */
    val progressBar: CornerBasedShape = RoundedCornerShape(9999.dp)

    /**
     * Shape for progress indicators with subtle corners.
     * Corner radius: 2dp
     */
    val progressIndicator: CornerBasedShape = RoundedCornerShape(2.dp)

    /**
     * Shape for input fields and text fields.
     * Corner radius: 8dp
     */
    val inputField: CornerBasedShape = RoundedCornerShape(8.dp)

    /**
     * Shape for floating action buttons (FAB).
     * Corner radius: 16dp
     */
    val fab: CornerBasedShape = RoundedCornerShape(16.dp)

    /**
     * Shape for extended floating action buttons.
     * Corner radius: 16dp
     */
    val extendedFab: CornerBasedShape = RoundedCornerShape(16.dp)

    /**
     * Shape for navigation menu items.
     * Corner radius: 8dp
     */
    val navigationItem: CornerBasedShape = RoundedCornerShape(8.dp)

    /**
     * Shape for snackbars and toasts.
     * Corner radius: 8dp
     */
    val snackbar: CornerBasedShape = RoundedCornerShape(8.dp)

    /**
     * Shape for intervention overlays.
     * Corner radius: 16dp
     */
    val intervention: CornerBasedShape = RoundedCornerShape(16.dp)

    // ============================================================================
    // SPECIALIZED SHAPES
    // ============================================================================

    /**
     * Creates a shape with rounded top corners only.
     *
     * @param cornerRadius The corner radius for top corners
     */
    fun topRounded(cornerRadius: DpCornerSize = xl): CornerBasedShape =
        RoundedCornerShape(
            topStart = cornerRadius.value,
            topEnd = cornerRadius.value,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )

    /**
     * Creates a shape with rounded bottom corners only.
     *
     * @param cornerRadius The corner radius for bottom corners
     */
    fun bottomRounded(cornerRadius: DpCornerSize = xl): CornerBasedShape =
        RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = cornerRadius.value,
            bottomEnd = cornerRadius.value
        )

    /**
     * Creates a shape with custom corner radii.
     *
     * @param topLeft Top-left corner radius
     * @param topRight Top-right corner radius
     * @param bottomRight Bottom-right corner radius
     * @param bottomLeft Bottom-left corner radius
     */
    fun custom(
        topLeft: Dp = 0.dp,
        topRight: Dp = 0.dp,
        bottomRight: Dp = 0.dp,
        bottomLeft: Dp = 0.dp
    ): CornerBasedShape = RoundedCornerShape(
        topStart = topLeft,
        topEnd = topRight,
        bottomEnd = bottomRight,
        bottomStart = bottomLeft
    )

    // ============================================================================
    // EXTENSION PROPERTIES FOR EASY ACCESS
    // ============================================================================

    /**
     * Converts DpCornerSize to Dp for use in modifiers.
     */
    fun DpCornerSize.toDp(): Dp = value
}

// ============================================================================
// TYPE ALIASES FOR CONVENIENCE
// ============================================================================

/** Type alias for common button shape */
typealias ButtonShape = CornerBasedShape

/** Type alias for common card shape */
typealias CardShape = CornerBasedShape

/** Type alias for common dialog shape */
typealias DialogShape = CornerBasedShape
