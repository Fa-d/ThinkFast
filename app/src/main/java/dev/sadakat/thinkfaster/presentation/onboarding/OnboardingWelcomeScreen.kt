package dev.sadakat.thinkfaster.presentation.onboarding

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.ui.theme.getMaxContentWidth
import androidx.navigation.NavController
import dev.sadakat.thinkfaster.presentation.navigation.Screen
import org.koin.androidx.compose.koinViewModel

/**
 * Onboarding Step 1 - Welcome and value proposition
 * Shows app value, features, and privacy promise
 * User can proceed to goal setup or skip to home with warning
 */
@Composable
fun OnboardingWelcomeScreen(
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val sharedState = remember { OnboardingSharedState.getInstance() }

    // Preload apps in background when Welcome screen appears
    LaunchedEffect(Unit) {
        android.util.Log.d("OnboardingWelcomeScreen", "Preloading apps for onboarding...")
        sharedState.loadInstalledApps(context)
    }

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
            // Progress indicator: Step 1 of 6
            OnboardingProgressIndicator(
                currentStep = 1,
                totalSteps = 6,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome content (without fillMaxSize to allow buttons to show)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                WelcomePage()
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        // Navigate to Step 2: Goal Setup
                        navController.navigate(Screen.OnboardingGoals.route)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Skip button
        /*    TextButton(
                onClick = {
                    // Mark onboarding as skipped (not completed)
                    viewModel.skipOnboarding(context)
                    // Navigate to Home with setup banner
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.OnboardingWelcome.route) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Skip for now")
            }*/
            }
        }
    }
}

/**
 * Progress indicator for onboarding flow
 * Shows current step out of total steps
 */
@Composable
fun OnboardingProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { step ->
            val stepNumber = step + 1
            StepDot(
                isCompleted = stepNumber < currentStep,
                isCurrent = stepNumber == currentStep,
                stepNumber = stepNumber
            )
        }
    }
}

/**
 * Individual step dot with completion state
 */
@Composable
fun StepDot(
    isCompleted: Boolean,
    isCurrent: Boolean,
    stepNumber: Int,
    modifier: Modifier = Modifier
) {
    val dotWidth = if (isCurrent) 24.dp else 8.dp
    val dotHeight = 8.dp
    val color = when {
        isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        isCurrent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .width(dotWidth)
            .height(dotHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}
