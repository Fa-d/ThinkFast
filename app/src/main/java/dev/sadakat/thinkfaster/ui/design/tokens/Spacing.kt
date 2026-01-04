package dev.sadakat.thinkfaster.ui.design.tokens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens for ThinkFast design system.
 *
 * Based on iOS reference with a consistent 4dp baseline scale.
 * Provides semantic spacing values for consistent layouts across the app.
 */
object Spacing {

    // ============================================================================
    // BASE SPACING SCALE
    // ============================================================================

    /** Extra small spacing - 4dp. Use for: tiny gaps, icon padding */
    val xs: Dp = 4.dp

    /** Small spacing - 8dp. Use for: small gaps, tight spacing */
    val sm: Dp = 8.dp

    /** Medium spacing - 16dp. Use for: default padding, card padding */
    val md: Dp = 16.dp

    /** Large spacing - 24dp. Use for: section spacing, large padding */
    val lg: Dp = 24.dp

    /** Extra large spacing - 32dp. Use for: extra large gaps */
    val xl: Dp = 32.dp

    /** Extra extra large spacing - 48dp. Use for: hero section spacing */
    val xxl: Dp = 48.dp

    // ============================================================================
    // COMPONENT PADDING
    // ============================================================================

    /**
     * Button padding values
     */
    object Button {
        /** Horizontal padding for primary/secondary buttons */
        val horizontal: Dp = 24.dp

        /** Vertical padding for primary/secondary buttons */
        val vertical: Dp = 16.dp

        /** Padding for text buttons */
        val textButtonHorizontal: Dp = 12.dp

        /** Gap between multiple buttons */
        val gap: Dp = sm
    }

    /**
     * Card padding values
     */
    object Card {
        /** Padding inside standard cards */
        val padding: Dp = md

        /** Gap between stacked cards */
        val gap: Dp = 12.dp
    }

    /**
     * List padding values
     */
    object List {
        /** Horizontal padding for list items */
        val horizontal: Dp = md

        /** Vertical spacing between list items */
        val itemGap: Dp = sm

        /** Vertical spacing between sections */
        val sectionGap: Dp = lg
    }

    /**
     * Dialog padding values
     */
    object Dialog {
        /** Padding inside dialogs */
        val padding: Dp = lg

        /** Spacing between dialog sections */
        val sectionGap: Dp = md
    }

    /**
     * Bottom Sheet padding values
     */
    object BottomSheet {
        /** Padding inside bottom sheets */
        val padding: Dp = lg

        /** Spacing between sections */
        val sectionGap: Dp = md

        /** Drag handle offset from top */
        val handleTopOffset: Dp = sm
    }

    /**
     * Input field padding values
     */
    object Input {
        /** Horizontal padding inside text fields */
        val horizontal: Dp = md

        /** Vertical padding inside text fields */
        val vertical: Dp = sm

        /** Spacing between label and input */
        val labelSpacing: Dp = sm
    }

    /**
     * Screen padding values
     */
    object Screen {
        /** Horizontal padding for screen edges */
        val horizontal: Dp = md

        /** Vertical padding for screen edges */
        val vertical: Dp = md

        /** Spacing between major sections */
        val sectionGap: Dp = lg

        /** Spacing between cards */
        val cardGap: Dp = sm
    }

    // ============================================================================
    // PADDING VALUES
    // ============================================================================

    /** No padding */
    val none: Dp = 0.dp

    /** Extra small padding - 4dp */
    val extraSmall: Dp = xs

    /** Small padding - 8dp */
    val small: Dp = sm

    /** Medium padding - 16dp */
    val medium: Dp = md

    /** Large padding - 24dp */
    val large: Dp = lg

    /** Extra large padding - 32dp */
    val extraLarge: Dp = xl

    // ============================================================================
    // ARRANGEMENTS
    // ============================================================================

    /** Horizontal arrangement with extra small spacing */
    val horizontalArrangementXS: Arrangement.Horizontal = Arrangement.spacedBy(xs)

    /** Horizontal arrangement with small spacing */
    val horizontalArrangementSM: Arrangement.Horizontal = Arrangement.spacedBy(sm)

    /** Horizontal arrangement with medium spacing */
    val horizontalArrangementMD: Arrangement.Horizontal = Arrangement.spacedBy(md)

    /** Horizontal arrangement with large spacing */
    val horizontalArrangementLG: Arrangement.Horizontal = Arrangement.spacedBy(lg)

    /** Vertical arrangement with extra small spacing */
    val verticalArrangementXS: Arrangement.Vertical = Arrangement.spacedBy(xs)

    /** Vertical arrangement with small spacing */
    val verticalArrangementSM: Arrangement.Vertical = Arrangement.spacedBy(sm)

    /** Vertical arrangement with medium spacing */
    val verticalArrangementMD: Arrangement.Vertical = Arrangement.spacedBy(md)

    /** Vertical arrangement with large spacing */
    val verticalArrangementLG: Arrangement.Vertical = Arrangement.spacedBy(lg)

    /** Vertical arrangement with extra large spacing */
    val verticalArrangementXL: Arrangement.Vertical = Arrangement.spacedBy(xl)

    // ============================================================================
    // PADDING VALUES HELPER
    // ============================================================================

    /**
     * Creates PaddingValues with all sides set to the same value
     */
    fun all(value: Dp): PaddingValues = PaddingValues(value)

    /**
     * Creates PaddingValues with horizontal and vertical values
     */
    fun symmetric(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): PaddingValues =
        PaddingValues(horizontal, vertical)

    /**
     * Creates PaddingValues with different values for each side
     */
    fun only(
        start: Dp = 0.dp,
        top: Dp = 0.dp,
        end: Dp = 0.dp,
        bottom: Dp = 0.dp
    ): PaddingValues = PaddingValues(start, top, end, bottom)
}
