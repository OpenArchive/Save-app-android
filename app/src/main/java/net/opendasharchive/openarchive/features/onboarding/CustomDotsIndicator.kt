package net.opendasharchive.openarchive.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme

@Composable
fun CustomDotsIndicator(
    totalDots: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(totalDots) { index ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == selectedIndex)
                            MaterialTheme.colorScheme.onBackground
                        else
                            Color(0xFF666666) // c23_medium_grey equivalent
                    )
            )
        }
    }
}

@Preview(name = "Dots Indicator", showBackground = true)
@Composable
private fun CustomDotsIndicatorPreview() {
    SaveAppTheme {
        CustomDotsIndicator(
            totalDots = 4,
            selectedIndex = 1,
            modifier = Modifier.padding(16.dp)
        )
    }
}