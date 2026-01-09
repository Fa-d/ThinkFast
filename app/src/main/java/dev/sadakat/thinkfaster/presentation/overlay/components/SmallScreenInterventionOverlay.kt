package dev.sadakat.thinkfaster.presentation.overlay.components

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sadakat.thinkfaster.domain.model.InterventionContent
import dev.sadakat.thinkfaster.domain.model.InterventionFeedback
import dev.sadakat.thinkfaster.domain.intervention.FrictionLevel
import dev.sadakat.thinkfaster.ui.design.tokens.Shapes
import dev.sadakat.thinkfaster.ui.theme.InterventionColors
import dev.sadakat.thinkfaster.ui.theme.InterventionGradients
import dev.sadakat.thinkfaster.ui.theme.ResponsiveFontSize
import dev.sadakat.thinkfaster.ui.theme.ResponsivePadding
import dev.sadakat.thinkfaster.ui.theme.adaptiveAnimationSpec
import dev.sadakat.thinkfaster.util.ErrorLogger
import dev.sadakat.thinkfaster.util.InterventionStyling
import dev.sadakat.thinkfaster.ui.theme.GradientContrastUtils
import kotlinx.coroutines.delay

/**
 * Responsive spacing utilities for small screen overlays
 */
object SmallScreenSpacing {
    @Composable
    fun contentPadding() = 16.dp

    @Composable
    fun topPadding() = 8.dp

    @Composable
    fun betweenElements() = 10.dp

    @Composable
    fun betweenSections() = 16.dp

    // Total button area height including all elements:
    // - Padding top: 16dp
    // - Snooze button: 44dp
    // - Spacing: 8dp
    // - Go Back button: 50dp
    // - Spacing: 8dp
    // - Proceed button: 50dp
    // - Spacing: 8dp
    // - Footer text: ~14dp
    // - Padding bottom: 8dp
    // Total: ~206dp, using 200dp for safety
    @Composable
    fun buttonAreaHeight() = 200.dp
}

/**
 * Small screen optimized intervention overlay
 * Designed for devices with <360dp width to prevent content overflow and squashing
 *
 * Key features:
 * - Vertical scrolling for content overflow
 * - Responsive font sizing with small-screen scaling
 * - Vertically stacked action buttons (full width)
 * - Gradient scrim for button area visibility
 * - Proper spacing to prevent content/button overlap
 */
@Composable
fun SmallScreenInterventionOverlay(
    targetApp: String,
    context: Context,
    interventionContent: InterventionContent?,
    frictionLevel: FrictionLevel,
    showFeedbackPrompt: Boolean,
    snoozeDurationMinutes: Int,
    onGoBackClick: () -> Unit,
    onProceedClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onFeedbackReceived: (InterventionFeedback) -> Unit,
    onSkipFeedback: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    // Get actual app name from package name
    val appName = remember(targetApp) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(targetApp, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            targetApp.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: "App"
        }
    }

    // Get gradient background
    val backgroundGradient = remember(interventionContent, isDarkTheme) {
        InterventionGradients.getGradientForContent(
            interventionContent?.javaClass?.simpleName,
            isDarkTheme
        )
    }

    // Get styling
    val style = interventionContent?.let {
        InterventionStyling.getStyleForContent(it, isDarkTheme)
    } ?: run {
        dev.sadakat.thinkfaster.util.InterventionStyle(
            backgroundColor = Color.Transparent,
            textColor = Color.White,
            accentColor = InterventionColors.Success,
            primaryTextStyle = dev.sadakat.thinkfaster.ui.theme.InterventionTypography.InterventionMessage,
            secondaryTextStyle = dev.sadakat.thinkfaster.ui.theme.InterventionTypography.InterventionSubtext,
            secondaryTextColor = Color.White.copy(alpha = 0.7f),
            borderColor = Color.White.copy(alpha = 0.5f),
            containerColor = Color.White.copy(alpha = 0.15f),
            iconColor = Color.White
        )
    }

    // Animation states
    var visible by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    LaunchedEffect(interventionContent) {
        visible = true
    }

    // Main layout with background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .alpha(alpha)
            .scale(scale)
    ) {
        // Scrollable content area (top portion of screen)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .verticalScroll(scrollState)
                .padding(horizontal = SmallScreenSpacing.contentPadding())
                .padding(top = SmallScreenSpacing.topPadding())
                .padding(bottom = SmallScreenSpacing.buttonAreaHeight()), // Reserve space for buttons
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App name
            Text(
                text = appName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = style.textColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(SmallScreenSpacing.betweenSections()))

            // Intervention content
            interventionContent?.let { content ->
                SmallScreenContentRenderer(
                    content = content,
                    textColor = style.textColor,
                    secondaryTextColor = style.secondaryTextColor
                )
            }
        }

        // Fixed bottom button area with gradient scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(horizontal = SmallScreenSpacing.contentPadding())
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                if (showFeedbackPrompt) {
                    SmallScreenFeedbackPrompt(
                        onFeedback = onFeedbackReceived,
                        onDismiss = onSkipFeedback,
                        style = style
                    )
                } else {
                    // Snooze button
                    OutlinedButton(
                        onClick = onSnoozeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = style.textColor
                        ),
                        border = BorderStroke(1.dp, style.textColor.copy(alpha = 0.3f)),
                        shape = Shapes.button
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = style.textColor.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Snooze $snoozeDurationMinutes min",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = style.textColor
                        )
                    }

                    SmallScreenActionButtons(
                        frictionLevel = frictionLevel,
                        onGoBackClick = onGoBackClick,
                        onProceedClick = onProceedClick,
                        textColor = style.textColor,
                        secondaryTextColor = style.secondaryTextColor,
                        isDarkTheme = isDarkTheme
                    )

                    Text(
                        text = "Building mindful habits",
                        fontSize = 11.sp,
                        color = style.secondaryTextColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Content renderer optimized for small screens
 * Scales font sizes appropriately for narrow displays
 */
@Composable
private fun SmallScreenContentRenderer(
    content: InterventionContent,
    textColor: Color,
    secondaryTextColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (content) {
            is InterventionContent.ReflectionQuestion -> {
                Text(
                    text = content.question,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(SmallScreenSpacing.betweenSections()))

                Text(
                    text = content.subtext,
                    fontSize = 14.sp,
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 20.sp
                )
            }
            is InterventionContent.TimeAlternative -> {
                Text(
                    text = content.prefix,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(SmallScreenSpacing.betweenSections()))

                // Show only 1 alternative on small screens
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content.alternatives.take(1).forEach { alternative ->
                        Text(
                            text = "${alternative.emoji} ${alternative.activity}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        )
                    }
                }
            }
            is InterventionContent.BreathingExercise -> {
                Text(
                    text = content.instruction,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(SmallScreenSpacing.betweenSections()))

                CompactBreathingExercise(
                    variant = content.variant,
                    isDarkTheme = isSystemInDarkTheme(),
                    modifier = Modifier.size(160.dp)
                )
            }
            is InterventionContent.UsageStats -> {
                if (content.message.isNotEmpty()) {
                    Text(
                        text = content.message,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(SmallScreenSpacing.betweenSections()))
                }

                EnhancedUsageStatsContent(
                    todayMinutes = content.todayMinutes,
                    yesterdayMinutes = content.yesterdayMinutes,
                    weekAvgMinutes = content.weekAverage,
                    goalMinutes = content.goalMinutes,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor
                )
            }
            is InterventionContent.EmotionalAppeal -> {
                Text(
                    text = content.message,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(SmallScreenSpacing.betweenSections()))

                Text(
                    text = content.subtext,
                    fontSize = 14.sp,
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
            is InterventionContent.Quote -> {
                EnhancedQuoteContent(
                    quote = content.quote,
                    author = content.author,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor
                )
            }
            is InterventionContent.Gamification -> {
                EnhancedGamificationContent(
                    challenge = content.challenge,
                    reward = content.reward,
                    currentProgress = content.currentProgress,
                    target = content.target,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor
                )
            }
            is InterventionContent.ActivitySuggestion -> {
                EnhancedActivitySuggestionContent(
                    suggestion = content.suggestion,
                    emoji = content.emoji,
                    timeEstimate = getActivityTimeEstimate(content.suggestion),
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    onActivityCompleted = {}
                )
            }
        }
    }
}

/**
 * Vertically stacked action buttons for small screens
 * Full-width buttons with proper touch targets (50dp height)
 */
@Composable
private fun SmallScreenActionButtons(
    frictionLevel: FrictionLevel,
    onGoBackClick: () -> Unit,
    onProceedClick: () -> Unit,
    textColor: Color,
    secondaryTextColor: Color,
    isDarkTheme: Boolean
) {
    val backgroundAverage = Color(0xFF283593)

    val goBackColor = GradientContrastUtils.ensureButtonContrast(
        buttonColor = InterventionColors.GoBackButtonDark,
        backgroundColor = backgroundAverage,
        minContrast = 3.0f
    )
    val proceedColor = GradientContrastUtils.ensureButtonContrast(
        buttonColor = InterventionColors.ProceedButtonDark,
        backgroundColor = backgroundAverage,
        minContrast = 3.0f
    )

    var showButtons by remember { mutableStateOf(frictionLevel.delayMs == 0L) }
    var countdown by remember { mutableStateOf((frictionLevel.delayMs / 1000).toInt()) }

    // Countdown for delayed buttons
    LaunchedEffect(frictionLevel.delayMs) {
        if (frictionLevel.delayMs > 0) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            showButtons = true
        }
    }

    if (!showButtons) {
        // Show countdown during delay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (frictionLevel) {
                    FrictionLevel.MODERATE -> "Take a breath..."
                    FrictionLevel.FIRM -> "Pause. Reflect."
                    FrictionLevel.LOCKED -> "Focus mode active."
                    else -> "Please wait..."
                },
                fontSize = 13.sp,
                color = secondaryTextColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Circular countdown indicator
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = proceedColor,
                    strokeWidth = 3.dp
                )

                Text(
                    text = "${countdown}s",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Making conscious choices",
                fontSize = 11.sp,
                color = secondaryTextColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        // Vertically stacked buttons for small screens
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Go Back button - full width, filled, with home icon
            Button(
                onClick = onGoBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goBackColor
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Go Back",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Proceed button - full width, outlined
            OutlinedButton(
                onClick = onProceedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = proceedColor
                )
            ) {
                Text(
                    text = "Proceed",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = proceedColor
                )
            }
        }
    }
}

/**
 * Compact feedback prompt for small screens
 * Vertically arranged feedback options
 */
@Composable
private fun SmallScreenFeedbackPrompt(
    onFeedback: (InterventionFeedback) -> Unit,
    onDismiss: () -> Unit,
    style: dev.sadakat.thinkfaster.util.InterventionStyle
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = style.containerColor
        ),
        shape = Shapes.dialog
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Question text
            Text(
                text = "Was this well-timed?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = style.textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Thumbs up/down buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbs up button (Helpful)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedIconButton(
                        onClick = { onFeedback(InterventionFeedback.HELPFUL) },
                        modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            containerColor = style.containerColor,
                            contentColor = style.iconColor
                        ),
                        border = BorderStroke(1.dp, style.borderColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Helpful",
                            tint = style.iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Good",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = style.iconColor
                    )
                }

                // Thumbs down button (Disruptive)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedIconButton(
                        onClick = { onFeedback(InterventionFeedback.DISRUPTIVE) },
                        modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            containerColor = style.containerColor,
                            contentColor = style.iconColor
                        ),
                        border = BorderStroke(1.dp, style.borderColor)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ThumbDown,
                            contentDescription = "Disruptive",
                            tint = style.iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Bad",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = style.iconColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Skip button
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Skip",
                    fontSize = 12.sp,
                    color = style.secondaryTextColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
