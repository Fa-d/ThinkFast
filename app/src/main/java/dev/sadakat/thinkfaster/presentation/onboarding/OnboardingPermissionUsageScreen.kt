package dev.sadakat.thinkfaster.presentation.onboarding

import android.provider.Settings
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.sadakat.thinkfaster.util.PermissionHelper
import kotlinx.coroutines.delay

/**
 * Onboarding Step 3 - Usage Stats permission primer
 * Explains why we need usage stats permission
 * Auto-advances when permission is granted
 */
@Composable
fun OnboardingPermissionUsageScreen(
    navController: NavController,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(PermissionHelper.hasUsageStatsPermission(context))
    }
    var showCheckmark by remember { mutableStateOf(false) }

    // Poll for permission changes and handle initial state
    LaunchedEffect(Unit) {
        // Check if permission is already granted on entry
        if (hasPermission) {
            showCheckmark = true
            delay(800)
            navController.navigate(Screen.OnboardingPermissionOverlay.route)
        } else {
            // Poll for permission changes
            while (!hasPermission) {
                delay(2000) // Poll every 2 seconds
                val granted = PermissionHelper.hasUsageStatsPermission(context)
                if (granted && !hasPermission) {
                    hasPermission = true
                    showCheckmark = true
                    // Wait for checkmark animation, then auto-advance
                    delay(800)
                    navController.navigate(Screen.OnboardingPermissionOverlay.route)
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val maxWidth = getMaxContentWidth()
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .padding(24.dp)
            ) {
                // Progress indicator: Step 3 of 6
                OnboardingProgressIndicator(
                    currentStep = 3,
                    totalSteps = 6,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Permission primer content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Icon with checkmark overlay
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        // Main icon
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📊",
                                fontSize = 54.sp
                            )
                        }

                        // Checkmark overlay (when granted)
                        if (showCheckmark) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Permission granted",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Title
                    Text(
                        text = "Track Your Social Media Usage",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))


                    // Benefits list
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "This lets you:",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            BenefitItem("📈 See your daily usage trends")
                            BenefitItem("💡 Get insights into your habits")
                            BenefitItem("🎯 Track progress toward your goals")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Essential badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ESSENTIAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Privacy note
                    Text(
                        text = "✓ All data stays private on your device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Grant button
                if (hasPermission) {
                    // Permission granted - show continue button
                    Button(
                        onClick = {
                            navController.navigate(Screen.OnboardingPermissionOverlay.route)
                        },
                        modifier = Modifier.fillMaxWidth(),
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
                } else {
                    // Permission not granted - show grant button
                    Button(
                        onClick = {
                            // Launch usage stats settings
                            val intent =
                                android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Grant Usage Access",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Helper text
                    Text(
                        text = "You'll be taken to Settings. Find 'Intently' and toggle it ON.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun BenefitItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
