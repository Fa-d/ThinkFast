package dev.sadakat.thinkfaster.ui.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sadakat.thinkfaster.ui.design.tokens.Animation
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.util.HapticFeedback
import dev.sadakat.thinkfaster.ui.theme.AppColors

/**
 * Primary button with gradient background and iOS-inspired press animation
 *
 * Features:
 * - Gradient background (blue → purple)
 * - Scale animation on press (0.95 → 1.0)
 * - Haptic feedback
 * - Loading state with spinner
 * - Disabled state
 *
 * Usage:
 * ```
 * PrimaryButton(
 *     text = "Save Goal",
 *     onClick = { saveGoal() }
 * )
 * ```
 *
 * @param text Button label text
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 * @param isLoading Whether to show loading spinner instead of text
 * @param leadingIcon Optional composable for icon before text
 * @param hapticFeedback Whether to trigger haptic feedback on press (default: true)
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    hapticFeedback: Boolean = true
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // iOS-style scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) Animation.Scale.PressDown else Animation.Scale.PressUp,
        animationSpec = Animation.ButtonPressSpec,
        label = "button_scale"
    )

    // Haptic feedback on press
    LaunchedEffect(isPressed) {
        if (isPressed && hapticFeedback && enabled && !isLoading) {
            HapticFeedback.selection(context)
        }
    }

    Button(
        onClick = {
            if (hapticFeedback) {
                HapticFeedback.success(context)
            }
            onClick()
        },
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled && !isLoading,
        shape = Shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        contentPadding = PaddingValues(
            horizontal = Spacing.Button.horizontal,
            vertical = Spacing.Button.vertical
        ),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (enabled && !isLoading) {
                        Brush.linearGradient(AppColors.Gradients.primary())
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    },
                    shape = Shapes.button
                )
                .padding(vertical = Spacing.xs),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    horizontalArrangement = Spacing.horizontalArrangementSM,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon?.invoke()
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Secondary button with outlined style
 *
 * Features:
 * - Outlined border (1dp)
 * - Secondary background
 * - Destructive variant (red color)
 * - Scale animation on press
 * - Haptic feedback
 *
 * Usage:
 * ```
 * SecondaryButton(
 *     text = "Cancel",
 *     onClick = { dismiss() }
 * )
 *
 * SecondaryButton(
 *     text = "Delete",
 *     onClick = { delete() },
 *     isDestructive = true
 * )
 * ```
 *
 * @param text Button label text
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 * @param isDestructive Whether to use error/destructive styling (red)
 * @param leadingIcon Optional composable for icon before text
 * @param hapticFeedback Whether to trigger haptic feedback on press (default: true)
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    hapticFeedback: Boolean = true
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // iOS-style scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) Animation.Scale.PressDown else Animation.Scale.PressUp,
        animationSpec = Animation.ButtonPressSpec,
        label = "button_scale"
    )

    // Haptic feedback on press
    LaunchedEffect(isPressed) {
        if (isPressed && hapticFeedback && enabled) {
            HapticFeedback.selection(context)
        }
    }

    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
        isDestructive -> AppColors.Semantic.Error.Default
        else -> MaterialTheme.colorScheme.primary
    }

    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isDestructive -> AppColors.Semantic.Error.Default
        else -> MaterialTheme.colorScheme.primary
    }

    OutlinedButton(
        onClick = {
            if (hapticFeedback) {
                if (isDestructive) {
                    HapticFeedback.warning(context)
                } else {
                    HapticFeedback.success(context)
                }
            }
            onClick()
        },
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = Shapes.button,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = textColor
        ),
        contentPadding = PaddingValues(
            horizontal = Spacing.Button.horizontal,
            vertical = Spacing.Button.vertical
        ),
        interactionSource = interactionSource
    ) {
        Row(
            horizontalArrangement = Spacing.horizontalArrangementSM,
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

/**
 * Text button with minimal styling
 *
 * Features:
 * - No background or border
 * - Text only with minimal padding
 * - Scale animation on press
 * - Haptic feedback
 *
 * Usage:
 * ```
 * AppTextButton(
 *     text = "Learn More",
 *     onClick = { openHelp() }
 * )
 * ```
 *
 * @param text Button label text
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for the button
 * @param enabled Whether the button is enabled
 * @param hapticFeedback Whether to trigger haptic feedback on press (default: true)
 */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticFeedback: Boolean = true
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // iOS-style scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) Animation.Scale.PressDown else Animation.Scale.PressUp,
        animationSpec = Animation.ButtonPressSpec,
        label = "button_scale"
    )

    // Haptic feedback on press
    LaunchedEffect(isPressed) {
        if (isPressed && hapticFeedback && enabled) {
            HapticFeedback.light(context)
        }
    }

    TextButton(
        onClick = {
            if (hapticFeedback) {
                HapticFeedback.selection(context)
            }
            onClick()
        },
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = Shapes.textButton,
        contentPadding = PaddingValues(
            horizontal = Spacing.Button.textButtonHorizontal,
            vertical = Spacing.sm
        ),
        interactionSource = interactionSource
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
