package net.opendasharchive.openarchive.features.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun StyledTitleText(
    text: String,
    modifier: Modifier = Modifier
) {
    // Use theme typography to get Montserrat font family
    val baseStyle = MaterialTheme.typography.displayLarge

    // Match the horizontal LinearLayout with weightSum="1000"
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Spacer with weight 65 of 1000
        Spacer(modifier = Modifier.weight(65f))

        // Text with weight 870 of 1000
        val styledText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                append(text.firstOrNull()?.toString() ?: "")
            }
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                append(text.drop(1))
            }
        }

        BasicText(
            text = styledText,
            modifier = Modifier
                .weight(870f)
                .fillMaxHeight()
                .wrapContentSize(Alignment.CenterStart)
                .graphicsLayer {
                    scaleX = 1.15f
                    scaleY = 1.15f
                },
            style = baseStyle.copy(
                fontSize = 60.sp,
                fontWeight = FontWeight.Black, // textFontWeight="900"
                lineHeight = 60.sp // Tighter line height to match XML
            ),
            autoSize = TextAutoSize.StepBased(
                minFontSize = 30.sp,
                maxFontSize = 60.sp
            ),
            maxLines = 1
        )
    }
}