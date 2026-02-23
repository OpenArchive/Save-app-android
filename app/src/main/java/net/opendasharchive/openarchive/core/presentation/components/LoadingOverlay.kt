package net.opendasharchive.openarchive.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A reusable full-screen loading overlay for Compose screens.
 * Displays a semi-transparent dimming background and a central progress indicator.
 * Blocks interaction with the underlying content.
 */
@Composable
fun LoadingOverlay(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
    indicatorColor: Color = MaterialTheme.colorScheme.tertiary
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Consume clicks to block underlying content
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = indicatorColor,
            strokeWidth = 4.dp
        )
    }
}
