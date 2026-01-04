package dev.sadakat.thinkfaster.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

/**
 * Reusable App Icon View
 *
 * Displays an app icon in a circular shape with optional fallback letter
 * Can be used across the app for consistent icon rendering
 *
 * @param drawable The app icon drawable
 * @param appName App name for fallback letter (used when drawable is null)
 * @param size Size of the icon (default 48dp)
 * @param contentDescription Optional content description for accessibility
 * @param modifier Optional modifier
 */
@Composable
fun AppIconView(
    drawable: Drawable?,
    appName: String = "",
    size: Dp = 48.dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    if (drawable != null) {
        val bitmap = remember(drawable) { drawable.toBitmap() }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        // Placeholder with fallback letter when no icon is available
        // Matches iOS reference pattern
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName.firstOrNull()?.toString()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (size.value / 2.5).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
