package dev.sadakat.thinkfast.presentation.manageapps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sadakat.thinkfast.domain.model.AppCategory
import dev.sadakat.thinkfast.domain.model.InstalledAppInfo
import dev.sadakat.thinkfast.domain.model.TrackedApp
import org.koin.androidx.compose.koinViewModel

/**
 * Screen for managing tracked apps
 * Allows users to select up to 10 apps to track with searchable and categorized lists
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAppsScreen(
    onBack: () -> Unit,
    viewModel: ManageAppsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Popular") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("All Apps") }
                )
            }

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                selectedTab == 0 -> {
                    PopularAppsTab(
                        curatedApps = uiState.curatedApps,
                        trackedApps = uiState.trackedApps,
                        searchQuery = searchQuery,
                        onAddApp = viewModel::addApp,
                        onRemoveApp = viewModel::removeApp,
                        isLimitReached = uiState.isLimitReached
                    )
                }
                else -> {
                    AllAppsTab(
                        installedApps = uiState.installedApps,
                        trackedApps = uiState.trackedApps,
                        searchQuery = searchQuery,
                        onAddApp = viewModel::addApp,
                        onRemoveApp = viewModel::removeApp,
                        isLimitReached = uiState.isLimitReached
                    )
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
 * Popular apps tab with categorized curated apps
 */
@Composable
private fun PopularAppsTab(
    curatedApps: Map<AppCategory, List<TrackedApp>>,
    trackedApps: List<String>,
    searchQuery: String,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    isLimitReached: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLimitReached) {
            item {
                LimitReachedWarning()
            }
        }

        curatedApps.forEach { (category, apps) ->
            // Filter by search
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
                        isLimitReached = isLimitReached
                    )
                }
            }
        }

        // Empty state messages
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
 * All apps tab with flat list of installed apps
 */
@Composable
private fun AllAppsTab(
    installedApps: List<InstalledAppInfo>,
    trackedApps: List<String>,
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLimitReached) {
            item {
                LimitReachedWarning()
            }
        }

        items(filteredApps) { app ->
            AppSelectionRow(
                appName = app.appName,
                packageName = app.packageName,
                icon = app.icon,
                isTracked = trackedApps.contains(app.packageName),
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

        // Empty state messages
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
 * Category section for popular tab
 */
@Composable
private fun CategorySection(
    category: AppCategory,
    apps: List<TrackedApp>,
    trackedApps: List<String>,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    isLimitReached: Boolean
) {
    Column {
        Text(
            text = category.displayName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                apps.forEachIndexed { index, app ->
                    AppSelectionRow(
                        appName = app.appName,
                        packageName = app.packageName,
                        icon = null, // Could enhance to load icons
                        isTracked = trackedApps.contains(app.packageName),
                        onToggle = {
                            if (trackedApps.contains(app.packageName)) {
                                onRemoveApp(app.packageName)
                            } else {
                                onAddApp(app.packageName)
                            }
                        },
                        isLimitReached = isLimitReached
                    )

                    if (index < apps.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * Row for selecting/deselecting an app
 */
@Composable
private fun AppSelectionRow(
    appName: String,
    packageName: String,
    icon: Drawable?,
    isTracked: Boolean,
    onToggle: () -> Unit,
    isLimitReached: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isTracked || !isLimitReached,
                onClick = onToggle
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                    modifier = Modifier.size(40.dp)
                )
            } else {
                // Placeholder icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp)
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

            Text(
                text = appName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Action button
        if (isTracked) {
            AssistChip(
                onClick = onToggle,
                label = { Text("Tracked") },
                leadingIcon = {
                    Text("✓", fontSize = 14.sp)
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        } else {
            Button(
                onClick = onToggle,
                enabled = !isLimitReached,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Add")
            }
        }
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
