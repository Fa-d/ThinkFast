package dev.sadakat.thinkfaster.presentation.onboarding

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.ui.theme.getMaxContentWidth
import androidx.navigation.NavController
import dev.sadakat.thinkfaster.presentation.navigation.Screen
import dev.sadakat.thinkfaster.ui.design.components.SecondaryButton
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import org.koin.androidx.compose.koinViewModel

/**
 * Onboarding Step 2 - Goal setup with app selection
 * User selects apps to track and sets their daily usage goal
 * Creates investment before requesting invasive permissions
 */
@Composable
fun OnboardingGoalsScreen(
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val sharedState = remember { OnboardingSharedState.getInstance() }

    // Collect state from shared state
    val installedApps by sharedState.installedApps.collectAsState()
    val selectedApps by sharedState.selectedApps.collectAsState()
    val isLoadingApps by sharedState.isLoadingApps.collectAsState()
    val selectedGoalMinutes by sharedState.selectedGoalMinutes.collectAsState()

    // Fallback: load apps if they weren't preloaded on Welcome screen
    LaunchedEffect(installedApps.isEmpty()) {
        if (installedApps.isEmpty()) {
            android.util.Log.d("OnboardingGoalsScreen", "Apps not preloaded, loading now...")
            sharedState.loadInstalledApps(context)
        } else {
            android.util.Log.d("OnboardingGoalsScreen", "Using ${installedApps.size} preloaded apps")
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        val maxWidth = getMaxContentWidth()
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            // Progress indicator: Step 2 of 6 (fixed at top)
            OnboardingProgressIndicator(
                currentStep = 2,
                totalSteps = 6,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title (fixed at top)
            Text(
                text = "Set Your Daily Goal",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // App grid (scrollable middle area, takes available space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AppSelectionGrid(
                    apps = installedApps,
                    selectedApps = selectedApps,
                    onAppToggle = { sharedState.toggleAppSelection(it) },
                    isLoading = isLoadingApps
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Goal time slider (fixed at bottom)
            GoalTimeSlider(
                selectedMinutes = selectedGoalMinutes,
                onTimeChange = { sharedState.updateGoalMinutes(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation buttons (fixed at bottom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                SecondaryButton(text = "Back", onClick = {
                    navController.popBackStack()
                }, leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                })

                // Continue button - enabled only when at least 1 app is selected
                Button(
                    onClick = {
                        viewModel.saveGoalSelection(selectedGoalMinutes)
                        navController.navigate(Screen.OnboardingPermissionUsage.route)
                    },
                    enabled = selectedApps.isNotEmpty(),
                    shape = Shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
