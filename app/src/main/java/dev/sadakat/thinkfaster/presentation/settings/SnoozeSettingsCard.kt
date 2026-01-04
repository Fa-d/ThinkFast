package dev.sadakat.thinkfaster.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.design.tokens.Spacing

/**
 * Snooze Settings Card - Opens bottom sheet to configure snooze
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeSettingsCard(
    snoozeActive: Boolean,
    remainingMinutes: Int,
    viewModel: GoalViewModel
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showBottomSheet = true },
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = if (snoozeActive)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementSM
                ) {
                    Text(text = "⏸️", fontSize = 24.sp)
                    Text(
                        text = "Snooze Reminders",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (snoozeActive)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = "Temporarily pause interventions. Tap to configure duration.",
                    fontSize = 13.sp,
                    color = if (snoozeActive)
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                // Show remaining time if active
                if (snoozeActive && remainingMinutes > 0) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Spacing.horizontalArrangementSM
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$remainingMinutes minutes remaining",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // Status indicator
            Surface(
                shape = Shapes.button,
                color = if (snoozeActive)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (snoozeActive) "Active" else "Off",
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (snoozeActive)
                        MaterialTheme.colorScheme.onTertiary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = Spacing.md)
                            .width(32.dp)
                            .height(Spacing.xs),
                        shape = RoundedCornerShape(2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    ) {}
                }
            }
        ) {
            SnoozeBottomSheetContent(
                snoozeActive = snoozeActive,
                remainingMinutes = remainingMinutes,
                onToggleSnooze = { enabled, duration ->
                    viewModel.toggleSnooze(enabled, duration)
                },
                onSetDuration = { duration ->
                    viewModel.setSnoozeDuration(duration)
                    showBottomSheet = false
                }
            )
        }
    }
}

/**
 * Bottom sheet content for snooze settings
 */
@Composable
internal fun SnoozeBottomSheetContent(
    snoozeActive: Boolean,
    remainingMinutes: Int,
    onToggleSnooze: (Boolean, Int) -> Unit,
    onSetDuration: (Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    var selectedDuration by remember { mutableStateOf(if (snoozeActive) remainingMinutes else 10) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = screenHeight * 0.85f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg)
            .padding(bottom = Spacing.xl),
        verticalArrangement = Spacing.verticalArrangementMD
    ) {
        // Title
        Column {
            Text(
                text = "Snooze Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pause all intervention reminders",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Toggle Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.button,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Enable Snooze",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = if (snoozeActive) "Reminders are paused" else "Reminders are active",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = snoozeActive,
                    onCheckedChange = { enabled ->
                        onToggleSnooze(enabled, selectedDuration)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                        checkedTrackColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }
        }

        // Duration Selection
        if (snoozeActive || true) { // Always show duration options
            Column(
                verticalArrangement = Spacing.verticalArrangementMD
            ) {
                Text(
                    text = "Snooze Duration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Duration chips
                val durations = listOf(
                    10 to "10 min",
                    15 to "15 min",
                    30 to "30 min",
                    45 to "45 min",
                    60 to "1 hr",
                    120 to "Chilling"
                )

                durations.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Spacing.horizontalArrangementSM
                    ) {
                        row.forEach { (minutes, label) ->
                            val isSelected = selectedDuration == minutes
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDuration = minutes
                                    if (snoozeActive) {
                                        // If already active, update duration immediately
                                        onSetDuration(minutes)
                                    }
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                        // Add empty space for incomplete rows
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Active snooze info
        if (snoozeActive && remainingMinutes > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.button,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Spacing.horizontalArrangementMD
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Active Snooze",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "$remainingMinutes minutes remaining",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Help text
        Text(
            text = "💡 While snoozed, no intervention reminders will appear. This is useful for focused work sessions.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}
