package net.opendasharchive.openarchive.features.onboarding.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minFontSize: TextUnit = 12.sp,
    maxFontSize: TextUnit = 100.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Unspecified
) {
    // Use theme typography as base to get correct font family
    val baseStyle = MaterialTheme.typography.bodyLarge

    BasicText(
        text = text,
        modifier = modifier,
        autoSize = TextAutoSize.StepBased(
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
        ),
        maxLines = 1,
        style = baseStyle.copy(
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = minFontSize // Use tighter line height to match XML
        )
    )
}