package dev.sadakat.thinkfaster.presentation.overlay.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enhanced quote content with decorative styling
 * Phase 3.4: Improved visual presentation for inspirational quotes
 *
 * Features:
 * - Large decorative quotation marks in background
 * - Better typography with serif font
 * - Small caps author attribution
 * - Improved spacing and visual hierarchy
 */
@Composable
fun EnhancedQuoteContent(
    quote: String,
    author: String,
    textColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Decorative opening quote mark
            Text(
                text = "",
                fontSize = 120.sp,
                fontFamily = FontFamily.Serif,
                color = textColor.copy(alpha = 0.1f),
                lineHeight = 80.sp,
                modifier = Modifier
                    .offset(y = 20.dp)
                    .alpha(0.15f)
            )

            Spacer(modifier = Modifier.height((-40).dp)) // Overlap with quote text

            // Quote text
            Text(
                text = quote,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                color = textColor,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Author attribution with decorative line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Decorative divider
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .alpha(0.3f)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = secondaryTextColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Author name (small caps effect with larger first letter)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "— ",
                        fontSize = 16.sp,
                        color = secondaryTextColor.copy(alpha = 0.95f),
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = author.uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Serif,
                        color = secondaryTextColor.copy(alpha = 0.8f),
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height((-20).dp)) // Overlap for closing quote

            // Decorative closing quote mark
            Text(
                text = "",
                fontSize = 120.sp,
                fontFamily = FontFamily.Serif,
                color = textColor.copy(alpha = 0.1f),
                lineHeight = 80.sp,
                modifier = Modifier
                    .offset(y = (-20).dp)
                    .alpha(0.15f)
            )
        }
    }
}
