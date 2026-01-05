package dev.sadakat.thinkfaster.presentation.themeappearance

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.ui.theme.ThemeMode
import dev.sadakat.thinkfaster.ui.theme.getMaxContentWidth
import dev.sadakat.thinkfaster.util.ThemePreferences

/**
 * Theme and Appearance screen
 * Contains all theme-related settings in one place
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeAppearanceScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme & Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            val maxWidth = getMaxContentWidth()
            LazyColumn(
                modifier = Modifier.widthIn(max = maxWidth),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎨",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "Customize Your Look",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personalize the app's appearance to match your style",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        }
        }
    }
}

/**
 * Theme mode selection card with radio buttons
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
