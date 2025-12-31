package dev.sadakat.thinkfaster.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.sadakat.thinkfaster.presentation.navigation.Screen
import org.koin.androidx.compose.koinViewModel

/**
 * Onboarding screen with 3 pages
 * Page 0: Welcome + Value Prop
 * Page 1: Permission explanation
 * Page 2: Goal setup
 */
@Composable
fun OnboardingScreen(
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to home when onboarding is completed
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            navController.navigate(Screen.PermissionRequest.route) {
                popUpTo(Screen.Onboarding.route) {
                    inclusive = true
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Page indicator dots
            PageIndicator(
                currentPage = uiState.currentPage,
                totalPages = 3,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Animated page content
            AnimatedContent(
                targetState = uiState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                transitionSpec = {
                    if (targetState > initialState) {
                        // Forward navigation
                        (slideInHorizontally(animationSpec = tween(300)) { width -> width } +
                                fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(300)) { width -> -width } +
                                        fadeOut(animationSpec = tween(300)))
                    } else {
                        // Backward navigation
                        (slideInHorizontally(animationSpec = tween(300)) { width -> -width } +
                                fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(300)) { width -> width } +
                                        fadeOut(animationSpec = tween(300)))
                    }
                },
                label = "onboarding_page"
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> PermissionPage()
                    2 -> GoalSetupPage(
                        selectedGoalMinutes = uiState.selectedGoalMinutes,
                        onGoalChange = viewModel::updateGoalMinutes
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (show on pages 1 and 2)
                if (uiState.currentPage > 0) {
                    OutlinedButton(
                        onClick = viewModel::previousPage,
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back")
                    }
                } else {
                    // Placeholder to maintain spacing
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Next / Get Started button
                Button(
                    onClick = {
                        if (uiState.currentPage < 2) {
                            viewModel.nextPage()
                        } else {
                            // Complete onboarding on last page
                            viewModel.completeOnboarding(context, uiState.selectedGoalMinutes)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = when (uiState.currentPage) {
                            0 -> "Get Started"
                            1 -> "Next"
                            else -> "Complete Setup"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Skip button (only on first 2 pages)
            if (uiState.currentPage < 2) {
                TextButton(
                    onClick = {
                        viewModel.skipOnboarding(context)
                        navController.navigate(Screen.PermissionRequest.route) {
                            popUpTo(Screen.Onboarding.route) {
                                inclusive = true
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Skip for now")
                }
            }
        }
    }
}

/**
 * Page 0: Welcome and value proposition
 */
@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡",
                fontSize = 44.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title
        Text(
            text = "Take Control of\nYour Screen Time",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle
        Text(
            text = "Build a healthier relationship with social media through mindful interventions.",
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Feature cards
        FeatureCard(
            icon = Icons.Default.Notifications,
            title = "Gentle Reminders",
            description = "Smart alerts that help you pause and reflect"
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureCard(
            icon = Icons.Default.Info,
            title = "Track Your Progress",
            description = "Visualize your usage and celebrate improvements"
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureCard(
            icon = Icons.Default.Lock,
            title = "Privacy First",
            description = "All data stays on your device, completely private"
        )
    }
}

/**
 * Feature card component
 */
@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Page 1: Permission explanation
 */
@Composable
fun PermissionPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🔒",
                fontSize = 44.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title
        Text(
            text = "Why We Need Permissions",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Explanation
        Text(
            text = "To help you mindfully use social media, ThinkFast needs a few permissions:",
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Permission cards
        PermissionCard(
            emoji = "📊",
            title = "Usage Stats",
            description = "To track Facebook and Instagram usage and show you insights",
            importance = "Essential"
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionCard(
            emoji = "🔔",
            title = "Display Over Other Apps",
            description = "To show gentle reminders when you open social media",
            importance = "Essential"
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionCard(
            emoji = "📱",
            title = "Notifications",
            description = "To celebrate your wins and share daily progress (optional)",
            importance = "Optional"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Your data never leaves your device. Everything is stored locally and privately.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

/**
 * Permission card component
 */
@Composable
fun PermissionCard(
    emoji: String,
    title: String,
    description: String,
    importance: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 10.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = importance,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (importance == "Essential") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (importance == "Essential") {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Page 2: Goal setup
 */
@Composable
fun GoalSetupPage(
    selectedGoalMinutes: Int,
    onGoalChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎯",
                fontSize = 44.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title
        Text(
            text = "Set Your Daily Goal",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle
        Text(
            text = "How much time do you want to spend on social media each day?",
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Goal display card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$selectedGoalMinutes",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "minutes per day",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = selectedGoalMinutes.toFloat(),
                onValueChange = { onGoalChange(it.toInt()) },
                valueRange = 15f..180f,
                steps = 32, // 15, 20, 25, ..., 180 (in 5-min increments)
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Slider labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "15 min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = "180 min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Research-based recommendations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "💡 Recommended: 60 minutes",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Research shows that limiting social media to ~1 hour per day significantly improves wellbeing and reduces anxiety.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Note about changing later
        Text(
            text = "Don't worry, you can adjust this anytime in Settings",
            style = MaterialTheme.typography.bodySmall.copy(
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

/**
 * Page indicator dots
 */
@Composable
fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalPages) { page ->
            Box(
                modifier = Modifier
                    .size(
                        width = if (page == currentPage) 24.dp else 8.dp,
                        height = 8.dp
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (page == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            )
        }
    }
}
