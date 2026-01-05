package dev.sadakat.thinkfaster.presentation.onboarding

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.ui.theme.getMaxContentWidth
import androidx.navigation.NavController
import dev.sadakat.thinkfaster.presentation.navigation.Screen
import org.koin.androidx.compose.koinViewModel

/**
 * Onboarding Step 2 - Goal setup
 * User sets their daily social media usage goal
 * Creates investment before requesting invasive permissions
 */
@Composable
fun OnboardingGoalsScreen(
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val maxWidth = getMaxContentWidth()
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
            // Progress indicator: Step 2 of 6
            OnboardingProgressIndicator(
                currentStep = 2,
                totalSteps = 6,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Goal setup content (without fillMaxSize to allow buttons to show)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                GoalSetupPage(
                    selectedGoalMinutes = uiState.selectedGoalMinutes,
                    onGoalChange = viewModel::updateGoalMinutes
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                OutlinedButton(
                    onClick = {
                        navController.popBackStack()
                    },
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

                // Continue button
                Button(
                    onClick = {
                        // Save goal selection (don't create Goal entities yet)
                        viewModel.saveGoalSelection(uiState.selectedGoalMinutes)
                        // Navigate to Step 3: Usage Permission
                        navController.navigate(Screen.OnboardingPermissionUsage.route)
                    },
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
}
