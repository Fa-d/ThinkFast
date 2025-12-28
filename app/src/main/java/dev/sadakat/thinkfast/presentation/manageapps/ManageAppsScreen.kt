package dev.sadakat.thinkfast.presentation.manageapps

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.thinkfast.domain.model.AppCategory
import dev.sadakat.thinkfast.domain.model.GoalProgress
import dev.sadakat.thinkfast.domain.model.InstalledAppInfo
import dev.sadakat.thinkfast.domain.model.TrackedApp
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * Screen for managing tracked apps and setting goals
 * Redesigned with filter chips instead of tabs for better UX
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAppsScreen(
    onBack: () -> Unit,
    viewModel: ManageAppsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AppFilter.TRACKED) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with count
            AppLimitHeader(
                currentCount = uiState.trackedApps.size,
                maxCount = 10,
                modifier = Modifier.padding(16.dp)
            )

            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Filter chips
            FilterChipsRow(
                selectedFilter = selectedFilter,
                trackedAppsCount = uiState.trackedApps.size,
                onFilterSelected = { selectedFilter = it }
            )

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        AppsContent(
                            filter = selectedFilter,
                            uiState = uiState,
                            searchQuery = searchQuery,
                            onAddApp = viewModel::addApp,
                            onRemoveApp = viewModel::removeApp,
                            onToggleGoalExpansion = viewModel::toggleGoalExpansion,
                            onSetGoal = viewModel::setGoal
                        )
                    }
                }
            }

            // Error/Success messages
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            uiState.successMessage?.let { success ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(success)
                }
            }
        }
    }
}

/**
 * Filter type for the apps list
 */
private enum class AppFilter(val displayName: String) {
    TRACKED("Tracked"),
    POPULAR("Popular"),
    ALL("All Apps")
}

/**
 * Filter chips row for switching between app lists
 */
@Composable
private fun FilterChipsRow(
    selectedFilter: AppFilter,
    trackedAppsCount: Int,
    onFilterSelected: (AppFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppFilter.values().forEach { filter ->
            val isSelected = selectedFilter == filter
            val count = when (filter) {
                AppFilter.TRACKED -> trackedAppsCount
                else -> null
            }

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(filter.displayName)
                        if (count != null) {
                            Text(
                                text = "($count)",
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

/**
 * Main content based on selected filter
 */
@Composable
private fun AppsContent(
    filter: AppFilter,
    uiState: ManageAppsUiState,
    searchQuery: String,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onToggleGoalExpansion: (String) -> Unit,
    onSetGoal: (String, Int) -> Unit
) {
    when (filter) {
        AppFilter.TRACKED -> TrackedAppsContent(
            installedApps = uiState.installedApps,
            trackedApps = uiState.trackedApps,
            goalProgressMap = uiState.goalProgressMap,
            expandedAppForGoal = uiState.expandedAppForGoal,
            isSavingGoal = uiState.isSavingGoal,
            searchQuery = searchQuery,
            onRemoveApp = onRemoveApp,
            onToggleGoalExpansion = onToggleGoalExpansion,
            onSetGoal = onSetGoal
        )
        AppFilter.POPULAR -> PopularAppsContent(
            curatedApps = uiState.curatedApps,
            trackedApps = uiState.trackedApps,
            searchQuery = searchQuery,
            onAddApp = onAddApp,
            onRemoveApp = onRemoveApp,
            isLimitReached = uiState.isLimitReached,
            installedApps = uiState.installedApps
        )
        AppFilter.ALL -> AllAppsContent(
            installedApps = uiState.installedApps,
            trackedApps = uiState.trackedApps,
            goalProgressMap = uiState.goalProgressMap,
            searchQuery = searchQuery,
            onAddApp = onAddApp,
            onRemoveApp = onRemoveApp,
            isLimitReached = uiState.isLimitReached
        )
    }
}

/**
 * Tracked apps content - shows only tracked apps with goal management
 */
@Composable
private fun TrackedAppsContent(
    installedApps: List<InstalledAppInfo>,
    trackedApps: List<String>,
    goalProgressMap: Map<String, GoalProgress?>,
    expandedAppForGoal: String?,
    isSavingGoal: Boolean,
    searchQuery: String,
    onRemoveApp: (String) -> Unit,
    onToggleGoalExpansion: (String) -> Unit,
    onSetGoal: (String, Int) -> Unit
) {
    val trackedAppDetails = installedApps.filter { it.packageName in trackedApps }
    val filteredApps = if (searchQuery.isBlank()) {
        trackedAppDetails
    } else {
        trackedAppDetails.filter {
            it.appName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (filteredApps.isEmpty()) {
            item {
                EmptyTrackedAppsState()
            }
        }

        items(filteredApps) { app ->
            val progress = goalProgressMap[app.packageName]
            TrackedAppCard(
                appName = app.appName,
                packageName = app.packageName,
                icon = app.icon,
                goalProgress = progress,
                isExpanded = expandedAppForGoal == app.packageName,
                isSavingGoal = isSavingGoal,
                onRemoveClick = { onRemoveApp(app.packageName) },
                onToggleExpanded = { onToggleGoalExpansion(app.packageName) },
                onSetGoal = { onSetGoal(app.packageName, it) }
            )
        }
    }
}

/**
 * Popular apps content - shows curated apps by category
 */
@Composable
private fun PopularAppsContent(
    curatedApps: Map<AppCategory, List<TrackedApp>>,
    trackedApps: List<String>,
    searchQuery: String,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    isLimitReached: Boolean,
    installedApps: List<InstalledAppInfo>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLimitReached) {
            item {
                LimitReachedWarning()
            }
        }

        curatedApps.forEach { (category, apps) ->
            val filteredApps = if (searchQuery.isBlank()) {
                apps
            } else {
                apps.filter {
                    it.appName.contains(searchQuery, ignoreCase = true)
                }
            }

            if (filteredApps.isNotEmpty()) {
                item {
                    CategorySection(
                        category = category,
                        apps = filteredApps,
                        trackedApps = trackedApps,
                        onAddApp = onAddApp,
                        onRemoveApp = onRemoveApp,
                        isLimitReached = isLimitReached,
                        installedApps = installedApps
                    )
                }
            }
        }

        val hasResults = curatedApps.any { (_, apps) ->
            apps.any { it.appName.contains(searchQuery, ignoreCase = true) }
        }

        if (!hasResults) {
            item {
                if (searchQuery.isNotBlank()) {
                    NoResultsState(searchQuery)
                } else if (curatedApps.isEmpty()) {
                    EmptyAppsState()
                }
            }
        }
    }
}

/**
 * All apps content - shows flat list of installed apps
 */
@Composable
private fun AllAppsContent(
    installedApps: List<InstalledAppInfo>,
    trackedApps: List<String>,
    goalProgressMap: Map<String, GoalProgress?>,
    searchQuery: String,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    isLimitReached: Boolean
) {
    val filteredApps = if (searchQuery.isBlank()) {
        installedApps
    } else {
        installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isLimitReached) {
            item {
                LimitReachedWarning()
            }
        }

        items(filteredApps) { app ->
            val progress = goalProgressMap[app.packageName]
            AppSelectionRow(
                appName = app.appName,
                packageName = app.packageName,
                icon = app.icon,
                isTracked = trackedApps.contains(app.packageName),
                hasGoal = progress != null,
                goalMinutes = progress?.goal?.dailyLimitMinutes,
                onToggle = {
                    if (trackedApps.contains(app.packageName)) {
                        onRemoveApp(app.packageName)
                    } else {
                        onAddApp(app.packageName)
                    }
                },
                isLimitReached = isLimitReached
            )
        }

        if (filteredApps.isEmpty()) {
            item {
                if (searchQuery.isNotBlank()) {
                    NoResultsState(searchQuery)
                } else {
                    EmptyAppsState()
                }
            }
        }
    }
}

/**
 * Header showing current tracked apps count and limit
 */
@Composable
private fun AppLimitHeader(
    currentCount: Int,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Track the apps you want to limit",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "($currentCount/$maxCount apps)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (currentCount >= maxCount) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}

/**
 * Search bar for filtering apps
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search apps...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}

/**
 * Card for a tracked app with goal management
 */
@Composable
private fun TrackedAppCard(
    appName: String,
    packageName: String,
    icon: Drawable?,
    goalProgress: GoalProgress?,
    isExpanded: Boolean,
    isSavingGoal: Boolean,
    onRemoveClick: () -> Unit,
    onToggleExpanded: () -> Unit,
    onSetGoal: (Int) -> Unit
) {
    var sliderValue by remember(goalProgress?.goal?.dailyLimitMinutes) {
        mutableStateOf(goalProgress?.goal?.dailyLimitMinutes?.toFloat() ?: 30f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App header row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
            ) {
                // Top row: icon + name + actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App icon and name
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // App icon
                        if (icon != null) {
                            Image(
                                bitmap = icon.toBitmap(48, 48).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = appName.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // App name and goal info
                        Column {
                            Text(
                                text = appName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (goalProgress != null) {
                                Text(
                                    text = "${goalProgress.formatTodayUsage()} • Goal: ${goalProgress.goal.dailyLimitMinutes}min",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "No goal set",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Action buttons (outside clickable area to avoid conflicts)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onRemoveClick) {
                            Text("Remove")
                        }
                        IconButton(onClick = onToggleExpanded) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Set Goal"
                            )
                        }
                    }
                }

                // Progress bar below app info
                if (goalProgress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { (goalProgress.percentageUsed / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f),
                            color = when (goalProgress.getProgressColor()) {
                                dev.sadakat.thinkfast.domain.model.ProgressColor.GREEN ->
                                    MaterialTheme.colorScheme.primary
                                dev.sadakat.thinkfast.domain.model.ProgressColor.YELLOW ->
                                    MaterialTheme.colorScheme.tertiary
                                dev.sadakat.thinkfast.domain.model.ProgressColor.ORANGE ->
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                dev.sadakat.thinkfast.domain.model.ProgressColor.RED ->
                                    MaterialTheme.colorScheme.error
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "${goalProgress.percentageUsed.toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (goalProgress.getProgressColor()) {
                                dev.sadakat.thinkfast.domain.model.ProgressColor.GREEN ->
                                    MaterialTheme.colorScheme.primary
                                dev.sadakat.thinkfast.domain.model.ProgressColor.YELLOW ->
                                    MaterialTheme.colorScheme.tertiary
                                dev.sadakat.thinkfast.domain.model.ProgressColor.ORANGE ->
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                dev.sadakat.thinkfast.domain.model.ProgressColor.RED ->
                                    MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }

            // Goal setting panel (expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()

                    // Streak info (if goal exists)
                    if (goalProgress != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = goalProgress.goal.getStreakMessage(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = goalProgress.goal.getLongestStreakMessage(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Progress card
                    if (goalProgress != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (goalProgress.getProgressColor()) {
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = goalProgress.getStatusMessage(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = goalProgress.formatRemainingTime(),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Goal slider
                    Text(
                        text = "Daily Limit: ${sliderValue.roundToInt()} minutes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 5f..180f,
                        steps = 34,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "5 min",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "180 min",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Update button
                    Button(
                        onClick = { onSetGoal(sliderValue.roundToInt()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        enabled = !isSavingGoal && sliderValue.roundToInt() != (goalProgress?.goal?.dailyLimitMinutes),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSavingGoal) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (goalProgress != null) "Update Goal" else "Set Goal",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Category section for popular apps
 */
@Composable
private fun CategorySection(
    category: AppCategory,
    apps: List<TrackedApp>,
    trackedApps: List<String>,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    isLimitReached: Boolean,
    installedApps: List<InstalledAppInfo>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Category header
        Text(
            text = category.displayName,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Individual app cards
        apps.forEach { app ->
            // Find the app icon from installed apps
            val appIcon = installedApps.find { it.packageName == app.packageName }?.icon

            AppSelectionRow(
                appName = app.appName,
                packageName = app.packageName,
                icon = appIcon,
                isTracked = trackedApps.contains(app.packageName),
                hasGoal = false,
                goalMinutes = null,
                onToggle = {
                    if (trackedApps.contains(app.packageName)) {
                        onRemoveApp(app.packageName)
                    } else {
                        onAddApp(app.packageName)
                    }
                },
                isLimitReached = isLimitReached
            )
        }
    }
}

/**
 * Row for selecting/deselecting an app
 * Modern card-based design inspired by Things 3 and Material Design 3
 */
@Composable
private fun AppSelectionRow(
    appName: String,
    packageName: String,
    icon: Drawable?,
    isTracked: Boolean,
    hasGoal: Boolean,
    goalMinutes: Int?,
    onToggle: () -> Unit,
    isLimitReached: Boolean
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTracked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                // Use background color to blend with screen, avoiding multi-layer look
                MaterialTheme.colorScheme.background
            }
        ),
        border = if (isTracked) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        } else {
            // Add subtle border for definition in light theme
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp, // Remove elevation for flatter look
            pressedElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Icon + App info
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Selection checkbox (inspired by Things 3)
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Checkbox circle
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
                                            radius = size.minDimension / 2
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Checkmark icon
                        if (isTracked) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Tracked",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // App icon
                if (icon != null) {
                    Image(
                        bitmap = icon.toBitmap(48, 48).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(42.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appName.firstOrNull()?.toString()?.uppercase() ?: "?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // App name and info
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = appName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (hasGoal && goalMinutes != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱",
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${goalMinutes}min/day",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Right side: Status indicator
            when {
                isTracked -> {
                    // Animated check badge
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .animateContentSize()
                    ) {
                        Text(
                            text = "✓ Tracked",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                isLimitReached -> {
                    // Limit reached indicator
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Limit",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    // Add indicator with arrow
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state for tracked apps tab
 */
@Composable
private fun EmptyTrackedAppsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📱",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No apps tracked yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Switch to \"Popular\" or \"All\" filter to add apps",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Warning card when limit is reached
 */
@Composable
private fun LimitReachedWarning() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = "⚠️ Maximum limit reached (10/10 apps). Remove an app to add more.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 14.sp
        )
    }
}

/**
 * Empty state when search returns no results
 */
@Composable
private fun NoResultsState(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔍",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No apps found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try a different search term",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Empty state when no apps are available
 */
@Composable
private fun EmptyAppsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📱",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No apps available",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Install some apps to get started",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
