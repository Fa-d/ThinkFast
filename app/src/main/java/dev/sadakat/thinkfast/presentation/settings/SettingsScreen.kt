package dev.sadakat.thinkfast.presentation.settings

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.sadakat.thinkfast.domain.model.AppTarget
import dev.sadakat.thinkfast.domain.model.GoalProgress
import dev.sadakat.thinkfast.ui.theme.ThemeMode
import dev.sadakat.thinkfast.util.ThemePreferences
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * Settings screen - goal management and app configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: GoalViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Goals") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(contentPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎯",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "Set Your Daily Goals",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Stay mindful with daily usage limits",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Success/Error messages
            item {
                uiState.successMessage?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "✓ $message",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            // Facebook goal
            item {
                GoalCard(
                    appName = "Facebook",
                    appIcon = "📘",
                    progress = uiState.facebookProgress,
                    isLoading = uiState.isLoading,
                    isSaving = uiState.isSaving,
                    onSetGoal = { viewModel.setFacebookGoal(it) }
                )
            }

            // Instagram goal
            item {
                GoalCard(
                    appName = "Instagram",
                    appIcon = "📷",
                    progress = uiState.instagramProgress,
                    isLoading = uiState.isLoading,
                    isSaving = uiState.isSaving,
                    onSetGoal = { viewModel.setInstagramGoal(it) }
                )
            }

            // App Settings section header
            item {
                Text(
                    text = "⚙️ App Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            // Timer alert duration setting
            item {
                TimerDurationCard(
                    currentDuration = uiState.appSettings.timerAlertMinutes,
                    onDurationChange = { viewModel.setTimerAlertDuration(it) }
                )
            }

            // Always show reminder toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🔔", fontSize = 24.sp)
                                Text(
                                    text = "Always Show Reminder",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Show reminder overlay every time you open Facebook or Instagram",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = uiState.appSettings.alwaysShowReminder,
                            onCheckedChange = { viewModel.setAlwaysShowReminder(it) }
                        )
                    }
                }
            }

            // Locked Mode toggle (Phase F)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.appSettings.lockedMode) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = if (uiState.appSettings.lockedMode) {
                        BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.error
                        )
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (uiState.appSettings.lockedMode) "🔒" else "🔓",
                                    fontSize = 24.sp
                                )
                                Text(
                                    text = "Locked Mode",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.appSettings.lockedMode) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.appSettings.lockedMode) {
                                    "Maximum friction - 10 second delay with countdown. You requested this extra control."
                                } else {
                                    "Enable maximum friction for extra self-control (10s delay)"
                                },
                                fontSize = 14.sp,
                                color = if (uiState.appSettings.lockedMode) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                lineHeight = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = uiState.appSettings.lockedMode,
                            onCheckedChange = { viewModel.setLockedMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.error,
                                checkedTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }

            // Theme & Appearance section header
            item {
                Text(
                    text = "🎨 Theme & Appearance",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            // Theme mode selector
            item {
                ThemeModeCard()
            }

            // Dynamic Color toggle (Android 12+)
            item {
                DynamicColorCard()
            }

            // AMOLED Dark Mode toggle
            item {
                AmoledDarkCard()
            }

            // Intervention Analytics (debug)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "📊", fontSize = 24.sp)
                                Text(
                                    text = "Intervention Analytics",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "View intervention effectiveness and content performance",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                lineHeight = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { navController.navigate("analytics") }
                        ) {
                            Text(
                                text = "→",
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Info section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ℹ️ How it works",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Set a daily limit for each app",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Build streaks by staying under your limit",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Streaks update daily at midnight",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Track your progress in real-time",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(
    appName: String,
    appIcon: String,
    progress: GoalProgress?,
    isLoading: Boolean,
    isSaving: Boolean,
    onSetGoal: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(progress?.goal?.dailyLimitMinutes?.toFloat() ?: 30f) }
    val currentLimit = progress?.goal?.dailyLimitMinutes ?: 30

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = appIcon, fontSize = 32.sp)
                    Text(
                        text = appName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (progress?.goal != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = progress.goal.getStreakMessage(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = progress.goal.getLongestStreakMessage(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Current progress (if goal exists)
            if (progress != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (progress.getProgressColor()) {
                            dev.sadakat.thinkfast.domain.model.ProgressColor.GREEN ->
                                MaterialTheme.colorScheme.primaryContainer
                            dev.sadakat.thinkfast.domain.model.ProgressColor.YELLOW ->
                                MaterialTheme.colorScheme.tertiaryContainer
                            dev.sadakat.thinkfast.domain.model.ProgressColor.ORANGE ->
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            dev.sadakat.thinkfast.domain.model.ProgressColor.RED ->
                                MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = progress.formatTodayUsage(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LinearProgressIndicator(
                            progress = (progress.percentageUsed / 100f).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = progress.getStatusMessage(),
                                fontSize = 14.sp
                            )
                            Text(
                                text = progress.formatRemainingTime(),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Divider()

            // Goal setting slider
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Daily Limit: ${sliderValue.roundToInt()} minutes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 5f..180f,
                    steps = 34, // 5-minute increments
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "5 min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "180 min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Set goal button
            Button(
                onClick = { onSetGoal(sliderValue.roundToInt()) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isSaving && sliderValue.roundToInt() != currentLimit,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (progress?.goal != null) "Update Goal" else "Set Goal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerDurationCard(
    currentDuration: Int,
    onDurationChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentDuration.toFloat()) }
    val hasChanged = sliderValue.roundToInt() != currentDuration

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "⏰", fontSize = 24.sp)
                Text(
                    text = "Timer Alert Duration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Get an alert after using the app continuously for this duration",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Current value display
            Text(
                text = "${sliderValue.roundToInt()} minutes",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            // Slider
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 1f..120f,
                steps = 118, // 1-minute increments
                modifier = Modifier.fillMaxWidth()
            )

            // Range labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1 min",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "120 min",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Apply button (only shown when value changed)
            if (hasChanged) {
                Button(
                    onClick = { onDurationChange(sliderValue.roundToInt()) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Apply",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Theme mode selection card with radio buttons
 * Phase 1.4: Visual Polish - Dark mode toggle
 */
@Composable
private fun ThemeModeCard() {
    val context = LocalContext.current
    var selectedThemeMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🌓", fontSize = 24.sp)
                Text(
                    text = "Theme Mode",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Choose your preferred app theme",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Theme options
            ThemeModeOption(
                label = "Light Mode",
                description = "Always use light theme",
                icon = "☀️",
                isSelected = selectedThemeMode == ThemeMode.LIGHT,
                onClick = {
                    selectedThemeMode = ThemeMode.LIGHT
                    ThemePreferences.saveThemeMode(context, ThemeMode.LIGHT)
                    (context as? Activity)?.recreate()
                }
            )

            ThemeModeOption(
                label = "Dark Mode",
                description = "Always use dark theme",
                icon = "🌙",
                isSelected = selectedThemeMode == ThemeMode.DARK,
                onClick = {
                    selectedThemeMode = ThemeMode.DARK
                    ThemePreferences.saveThemeMode(context, ThemeMode.DARK)
                    (context as? Activity)?.recreate()
                }
            )

            ThemeModeOption(
                label = "Follow System",
                description = "Match your device settings",
                icon = "⚙️",
                isSelected = selectedThemeMode == ThemeMode.FOLLOW_SYSTEM,
                onClick = {
                    selectedThemeMode = ThemeMode.FOLLOW_SYSTEM
                    ThemePreferences.saveThemeMode(context, ThemeMode.FOLLOW_SYSTEM)
                    (context as? Activity)?.recreate()
                }
            )
        }
    }
}

/**
 * Individual theme mode option with radio button
 */
@Composable
private fun ThemeModeOption(
    label: String,
    description: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 28.sp
                )
                Column {
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

/**
 * Dynamic Color toggle card (Android 12+)
 * Phase 1.5: Implementation of unused theme preference
 */
@Composable
private fun DynamicColorCard() {
    val context = LocalContext.current
    var dynamicColorEnabled by remember { mutableStateOf(ThemePreferences.getDynamicColor(context)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "🎨", fontSize = 28.sp)
                Column {
                    Text(
                        text = "Dynamic Colors",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Match your system wallpaper colors (Android 12+)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = dynamicColorEnabled,
                onCheckedChange = { enabled ->
                    dynamicColorEnabled = enabled
                    ThemePreferences.saveDynamicColor(context, enabled)
                    (context as? Activity)?.recreate()
                }
            )
        }
    }
}

/**
 * AMOLED Dark Mode toggle card
 * Phase 1.5: Implementation of unused theme preference
 */
@Composable
private fun AmoledDarkCard() {
    val context = LocalContext.current
    val currentThemeMode = ThemePreferences.getThemeMode(context)
    var amoledDarkEnabled by remember { mutableStateOf(ThemePreferences.getAmoledDark(context)) }

    // Only show this option if dark mode or follow system is selected
    val isDarkModeActive = currentThemeMode == ThemeMode.DARK || currentThemeMode == ThemeMode.FOLLOW_SYSTEM

    if (isDarkModeActive) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "⬛", fontSize = 28.sp)
                    Column {
                        Text(
                            text = "AMOLED Black",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Pure black background for dark mode (saves battery)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = amoledDarkEnabled,
                    onCheckedChange = { enabled ->
                        amoledDarkEnabled = enabled
                        ThemePreferences.saveAmoledDark(context, enabled)
                        (context as? Activity)?.recreate()
                    }
                )
            }
        }
    }
}
