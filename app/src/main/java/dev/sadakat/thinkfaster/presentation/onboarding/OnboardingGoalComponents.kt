package dev.sadakat.thinkfaster.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.domain.model.InstalledAppInfo
import dev.sadakat.thinkfaster.ui.components.AppIconView
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing
import dev.sadakat.thinkfaster.util.HapticFeedback

/**
 * Individual app grid item for selection
 *
 * @param app The app info to display
 * @param isSelected Whether this app is currently selected
 * @param onClick Callback when tapped
 * @param modifier Modifier
 */
@Composable
fun AppGridItem(
    app: InstalledAppInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f),
        label = "app_grid_item_scale"
    )

    // Border color animation
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(200),
        label = "app_grid_item_border_color"
    )

    // Fixed border width (no animation to avoid jitter)
    val borderWidth = if (isSelected) 3.dp else 0.5.dp

    // Haptic feedback on press
    LaunchedEffect(isPressed) {
        if (isPressed) {
            HapticFeedback.selection(context)
        }
    }

    // Clickable container - handles all taps
    Box(
        modifier = modifier
            .size(64.dp)
            .scale(scale)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        // Main container for icon + border
        Box(modifier = Modifier.size(56.dp)) {
            // Draw border using a stroke approach with Canvas
            // Border ring
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(borderColor)
            ) {
                // Inner content (icon) - slightly smaller to show border
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .padding(borderWidth)
                        .clip(CircleShape)
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AppIconView(
                        drawable = app.icon,
                        appName = app.appName,
                        size = 50.dp
                    )
                }
            }

            // Checkmark overlay when selected (drawn last so it's on top)
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                )
            }
        }
    }
}

/**
 * App selection grid with 4 columns
 *
 * @param apps List of installed apps
 * @param selectedApps Set of package names that are selected
 * @param onAppToggle Callback when an app is tapped
 * @param isLoading Whether apps are currently loading
 * @param modifier Modifier
 */
@Composable
fun AppSelectionGrid(
    apps: List<InstalledAppInfo>,
    selectedApps: Set<String>,
    onAppToggle: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        // Loading skeleton
        LoadingAppGrid(modifier = modifier)
    } else if (apps.isEmpty()) {
        // Empty state
        EmptyAppGridState(modifier = modifier)
    } else {
        // App grid - using regular Column with FlowRow for vertical scroll compatibility
        AppGridContent(
            apps = apps,
            selectedApps = selectedApps,
            onAppToggle = onAppToggle,
            modifier = modifier
        )
    }
}

/**
 * App grid content using regular layout (compatible with vertical scrolling)
 * Creates a simple grid layout with items per row calculated from screen width
 */
@Composable
private fun AppGridContent(
    apps: List<InstalledAppInfo>,
    selectedApps: Set<String>,
    onAppToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val itemSize = 64.dp
    val spacing = 12.dp
    val padding = 32.dp // 16dp on each side

    // Calculate how many items fit per row
    val availableWidth = screenWidthDp - padding
    val itemsPerRow = ((availableWidth + spacing) / (itemSize + spacing)).toInt().coerceAtLeast(1)

    // Split apps into rows
    val rows = apps.chunked(itemsPerRow)

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowApps ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowApps.forEach { app ->
                    val isSelected = app.packageName in selectedApps
                    AppGridItem(
                        app = app,
                        isSelected = isSelected,
                        onClick = { onAppToggle(app.packageName) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space with empty boxes if needed
                val remainingSlots = itemsPerRow - rowApps.size
                repeat(remainingSlots) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Loading skeleton for app grid
 */
@Composable
private fun LoadingAppGrid(modifier: Modifier = Modifier) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val itemSize = 64.dp
    val spacing = 12.dp
    val padding = 32.dp

    val availableWidth = screenWidthDp - padding
    val itemsPerRow = ((availableWidth + spacing) / (itemSize + spacing)).toInt().coerceAtLeast(1)

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) { // Show 3 rows of loading skeletons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(itemsPerRow) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state when no apps are found
 */
@Composable
private fun EmptyAppGridState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📱",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(bottom = Spacing.md)
            )
            Text(
                text = "No apps found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Check logcat for debug info",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
    }
}

/**
 * Counter showing number of selected apps
 *
 * @param count Number of selected apps
 * @param modifier Modifier
 */
@Composable
fun SelectedAppsCounter(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count == 1) "$count app selected" else "$count apps selected",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Goal time slider with display card
 *
 * @param selectedMinutes Currently selected minutes
 * @param onTimeChange Callback when slider value changes
 * @param modifier Modifier
 */
@Composable
fun GoalTimeSlider(
    selectedMinutes: Int,
    onTimeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large time display
            Text(
                text = "$selectedMinutes",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "minutes per day",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Slider
            Slider(
                value = selectedMinutes.toFloat(),
                onValueChange = { onTimeChange(it.toInt()) },
                valueRange = 15f..180f,
                steps = 32,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Goal time slider with selected apps counter integrated
 *
 * @param selectedCount Number of selected apps
 * @param selectedMinutes Currently selected minutes
 * @param onTimeChange Callback when slider value changes
 * @param modifier Modifier
 */
@Composable
fun GoalTimeSliderWithCounter(
    selectedCount: Int,
    selectedMinutes: Int,
    onTimeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selected apps counter (at the top)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCount == 1) {
                        "$selectedCount app selected"
                    } else {
                        "$selectedCount apps selected"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large time display
            Text(
                text = "$selectedMinutes",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "minutes per day",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Slider
            Slider(
                value = selectedMinutes.toFloat(),
                onValueChange = { onTimeChange(it.toInt()) },
                valueRange = 15f..180f,
                steps = 32,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}



/**
 * Error card for app loading failure
 */
@Composable
fun AppLoadErrorCard(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Couldn't load apps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            androidx.compose.material3.TextButton(onClick = onRetry) {
                Text("Retry", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
