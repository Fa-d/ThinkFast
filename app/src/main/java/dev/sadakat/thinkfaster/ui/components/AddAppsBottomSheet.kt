package dev.sadakat.thinkfaster.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.domain.model.AppCategory
import dev.sadakat.thinkfaster.domain.model.InstalledAppInfo
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing

/**
 * Add Apps Bottom Sheet - Full-height bottom sheet for adding apps to track
 *
 * Shows all installed apps with icons, organized by category
 * Users can tap to add/remove apps from tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppsBottomSheet(
    installedApps: List<InstalledAppInfo>,
    trackedApps: List<String>,
    isLimitReached: Boolean,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true  // Full height only
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = Shapes.bottomSheet
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
                    text = "Add Apps",
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

            // Limit warning
            if (isLimitReached) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = Shapes.button
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Maximum 10 apps allowed. Remove an app first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            // Search bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true,
                shape = Shapes.inputField,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Apps list
            val filteredApps = if (searchQuery.isBlank()) {
                installedApps
            } else {
                installedApps.filter {
                    it.appName.contains(searchQuery, ignoreCase = true)
                }
            }

            // Group by category for better organization
            val appsByCategory = filteredApps.groupBy { it.category }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Show categories in order
                AppCategory.values().forEach { category ->
                    val categoryApps = appsByCategory[category]
                    if (!categoryApps.isNullOrEmpty()) {
                        // Category header
                        item {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = Spacing.xs)
                            )
                        }

                        // Apps in this category
                        items(categoryApps) { app ->
                            val isTracked = trackedApps.contains(app.packageName)
                            AddAppListItem(
                                app = app,
                                isTracked = isTracked,
                                isLimitReached = isLimitReached,
                                onToggle = {
                                    if (isTracked) {
                                        onRemoveApp(app.packageName)
                                    } else {
                                        onAddApp(app.packageName)
                                    }
                                }
                            )
                        }

                        // Spacing after category
                        item {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                        }
                    }
                }

                // No results
                if (filteredApps.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Spacer(modifier = Modifier.height(48.dp))
                            Text(
                                text = "🔍",
                                fontSize = 40.sp
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "No apps found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = "Try a different search term",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * List item for adding/removing an app
 * Shows app icon, name, and status
 */
@Composable
private fun AddAppListItem(
    app: InstalledAppInfo,
    isTracked: Boolean,
    isLimitReached: Boolean,
    onToggle: () -> Unit
) {
    val canAdd = !isLimitReached && !isTracked
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canAdd || isTracked) {
                    Modifier
                        .scale(if (isPressed) 0.98f else 1f)
                        .clickable(
                            onClick = onToggle,
                            interactionSource = interactionSource,
                            indication = null
                        )
                } else {
                    Modifier
                }
            ),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = if (isTracked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.background
            }
        ),
        border = if (isTracked) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Checkbox + App icon + App name
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Checkbox
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (isTracked) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = CircleShape
                            )
                            .then(
                                if (!isTracked) {
                                    Modifier.drawBehind {
                                        drawCircle(
                                            color = outlineColor,
                                            radius = size.width / 2
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTracked) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Tracked",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // App icon
                AppIconView(
                    drawable = app.icon,
                    appName = app.appName,
                    size = 42.dp
                )

                // App name
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            // Right side: Status
            when {
                isTracked -> {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = Shapes.chip
                    ) {
                        Text(
                            text = "✓ Tracked",
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                isLimitReached -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = Shapes.chip
                    ) {
                        Text(
                            text = "Limit",
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
