package dev.sadakat.thinkfaster.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.sadakat.thinkfaster.presentation.home.PerAppGoalUiModel
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.theme.isLandscape
import dev.sadakat.thinkfaster.ui.theme.shouldUseTwoColumnLayout
import dev.sadakat.thinkfaster.util.HapticFeedback

/**
 * Goal Editor Bottom Sheet - Full-height bottom sheet for editing a single app's goal
 *
 * Layout (from top to bottom):
 * - Cancel button (top-right)
 * - App icon (large, centered)
 * - App name (centered)
 * - Current goal display (centered)
 * - Spacer
 * - Time display (large, centered)
 * - Slider (15-180 min)
 * - Save button (full width, at bottom)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorBottomSheet(
    app: PerAppGoalUiModel,
    onDismiss: () -> Unit,
    onSaveGoal: (Int) -> Unit,
    progressText: String? = null  // Optional progress indicator for sequential goal setting
) {
    val context = LocalContext.current
    val useDialog = shouldUseTwoColumnLayout()
    val landscapeMode = isLandscape()

    // Initialize slider value with current goal or default 60 minutes
    var sliderValue by remember(app.dailyLimitMinutes) {
        mutableStateOf((app.dailyLimitMinutes ?: 60).toFloat())
    }

    if (useDialog) {
        // Use Dialog for landscape tablets
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = Shapes.bottomSheet
            ) {
                GoalEditorContent(
                    app = app,
                    sliderValue = sliderValue,
                    onSliderValueChange = { sliderValue = it },
                    onDismiss = onDismiss,
                    onSaveGoal = onSaveGoal,
                    progressText = progressText,
                    context = context,
                    useDialog = true,
                    landscapeMode = landscapeMode
                )
            }
        }
    } else {
        // Use ModalBottomSheet for portrait phones
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = !landscapeMode  // Allow partial expansion in landscape
        )

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = Shapes.bottomSheet
        ) {
            GoalEditorContent(
                app = app,
                sliderValue = sliderValue,
                onSliderValueChange = { sliderValue = it },
                onDismiss = onDismiss,
                onSaveGoal = onSaveGoal,
                progressText = progressText,
                context = context,
                useDialog = false,
                landscapeMode = landscapeMode
            )
        }
    }
}

@Composable
private fun GoalEditorContent(
    app: PerAppGoalUiModel,
    sliderValue: Float,
    onSliderValueChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onSaveGoal: (Int) -> Unit,
    progressText: String?,
    context: android.content.Context,
    useDialog: Boolean,
    landscapeMode: Boolean
) {
    if (landscapeMode) {
        // Landscape layout - 2 columns vertical flow
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Top row: App info (left) + Cancel button (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress indicator
                if (progressText != null) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // App info (left side)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconView(
                        drawable = app.appIcon,
                        appName = app.appName,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (app.dailyLimitMinutes != null) {
                                "Current: ${app.dailyLimitMinutes} min"
                            } else {
                                "Set a goal"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Cancel button (right)
                TextButton(onClick = {
                    HapticFeedback.light(context)
                    onDismiss()
                }) {
                    Text("Cancel", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Two-column grid for slider and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Left column - Slider value
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${sliderValue.toInt()}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right column - Slider
                Column(
                    modifier = Modifier.weight(2f)
                ) {
                    Text(
                        text = "Daily goal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = onSliderValueChange,
                        valueRange = 15f..180f,
                        steps = 32,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "15m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "180m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Save button (full width)
            Button(
                onClick = {
                    HapticFeedback.success(context)
                    onSaveGoal(sliderValue.toInt())
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Save Goal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        // Portrait / Dialog layout - vertical arrangement (original)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(Spacing.lg)
        ) {
            // Cancel button (top-right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Progress indicator (if setting goals for multiple apps)
                if (progressText != null) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                TextButton(onClick = {
                    HapticFeedback.light(context)
                    onDismiss()
                }) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // App icon (centered, large)
            AppIconView(
                drawable = app.appIcon,
                appName = app.appName,
                size = 80.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // App name (centered)
            Text(
                text = app.appName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Current goal (centered)
            Text(
                text = if (app.dailyLimitMinutes != null) {
                    "Current goal: ${app.dailyLimitMinutes} min/day"
                } else {
                    "Set a daily goal for this app"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Slider value display (large, centered)
            Text(
                text = "${sliderValue.toInt()} min",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = "Daily goal",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Slider
            Slider(
                value = sliderValue,
                onValueChange = onSliderValueChange,
                valueRange = 15f..180f,
                steps = 32,  // 5-minute increments
                modifier = Modifier.fillMaxWidth()
            )

            // Slider range labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "15 min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "180 min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Save button
            Button(
                onClick = {
                    HapticFeedback.success(context)
                    onSaveGoal(sliderValue.toInt())
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "Save Goal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
        }
    }
}

