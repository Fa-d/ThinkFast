package dev.sadakat.thinkfaster.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sadakat.thinkfaster.presentation.home.PerAppGoalUiModel
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.ui.theme.AppColors

/**
 * Manage Apps Bottom Sheet - Full-height bottom sheet showing detailed list of tracked apps
 *
 * Shows:
 * - Title "Manage Apps" at top with close button
 * - List of tracked apps with detailed info (icon, name, goal, usage, progress, streak)
 * - Swipe left to delete on each app item with smooth Material 3 animation
 * - "Add More Apps" button at bottom
 *
 * Uses Material 3's SwipeToDismissBox for reliable swipe-to-delete with proper state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAppsBottomSheet(
    trackedApps: List<PerAppGoalUiModel>,
    onAppClick: (PerAppGoalUiModel) -> Unit,
    onDismiss: () -> Unit,
    onAddAppsClick: () -> Unit,
    onRemoveApp: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true  // Full height only
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(Spacing.lg)
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manage Apps",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Apps list
            if (trackedApps.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "No apps tracked yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Spacing.md))

                    Button(onClick = onAddAppsClick) {
                        Text("Add Apps to Track")
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                // List of apps with swipe-to-delete
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(
                        items = trackedApps,
                        key = { it.packageName }  // Unique key for proper state management
                    ) { app ->
                        SwipeToDeleteItem(
                            app = app,
                            onDelete = { onRemoveApp(app.packageName) },
                            onClick = { onAppClick(app) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(durationMillis = 250),
                                fadeOutSpec = tween(durationMillis = 250),
                                placementSpec = tween(durationMillis = 250)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Add more apps button
                OutlinedButton(
                    onClick = {
                        onAddAppsClick()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add More Apps")
                }
            }
        }
    }
}

/**
 * Swipe to delete wrapper using Material 3's SwipeToDismissBox
 * Provides smooth, reliable swipe-to-delete with proper state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    app: PerAppGoalUiModel,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // User swiped left to delete
                    onDelete()
                    true  // Confirm the dismissal
                }
                else -> false  // Don't dismiss on other swipe directions
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,  // Only allow left swipe (EndToStart)
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // Red background with delete icon revealed during swipe
            val backgroundColor = lerp(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.error,
                dismissState.progress
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)  // Match card's corner radius
                    .background(backgroundColor)
                    .padding(horizontal = Spacing.lg),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete app",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) {
        // The actual app item content
        ManageAppListItem(
            app = app,
            onClick = onClick
        )
    }
}

/**
 * Individual app item for Manage Apps bottom sheet
 */
@Composable
private fun ManageAppListItem(
    app: PerAppGoalUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Top row: icon, name, streak badge, edit button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // App icon
                AppIconView(
                    drawable = app.appIcon,
                    appName = app.appName,
                    size = 48.dp
                )

                // App name and goal
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.dailyLimitMinutes != null) {
                            Text(
                                text = "${app.dailyLimitMinutes} min/day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "No goal set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Streak badge
                        if (app.currentStreak > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(app.streakColor.copy(alpha = 0.15f))
                                    .padding(horizontal = Spacing.xs, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🔥",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize
                                )
                                Text(
                                    text = "${app.currentStreak}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = app.streakColor
                                )
                            }
                        }
                    }
                }

                // Edit button (arrow icon)
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Edit goal",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Usage info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${app.todayUsageMinutes} min used today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (app.percentageUsed != null) {
                    Text(
                        text = "${app.percentageUsed}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Progress.getColorForPercentage(app.percentageUsed)
                    )
                }
            }

            // Progress bar
            if (app.dailyLimitMinutes != null) {
                val progress = (app.percentageUsed?.coerceAtMost(100) ?: 0) / 100f
                val progressColor =
                    AppColors.Progress.getColorForPercentage(app.percentageUsed ?: 0)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}
