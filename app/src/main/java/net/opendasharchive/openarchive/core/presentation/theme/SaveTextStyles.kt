package net.opendasharchive.openarchive.core.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Save App Text Styles - Based on Figma Design System
 * From "Save App 3.0" - 6 text styles only
 *
 * Usage Examples:
 * - SaveText.Text16pt("Welcome")
 * - Text("Custom", style = SaveTextStyles.text16pt)
 */

@Composable
fun TitleLarge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        style = SaveTextStyles.titleLarge.copy(color = color),
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}

@Composable
fun TitleMedium(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        style = SaveTextStyles.titleMedium.copy(color = color),
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}

@Composable
fun BodyLarge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        style = SaveTextStyles.bodyLarge.copy(color = color),
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}

@Composable
fun BodySmall(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        style = SaveTextStyles.bodySmall.copy(color = color),
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}

@Composable
fun LabelLarge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        style = SaveTextStyles.labelLarge.copy(color = color),
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}

@Composable
fun BodySmallEmphasis(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        style = SaveTextStyles.bodySmallEmphasis.copy(color = color),
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines
    )
}


/**
 * Raw TextStyle objects based on Figma "Save App 3.0"
 * Only the 6 text styles from the design system
 */
object SaveTextStyles {

    val headlineSmall = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = TextUnit.Unspecified,
        letterSpacing = 0.04.sp
    )

    // 18pt - SemiBold - For page titles, primary headers
    val titleBold = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.01.sp,
        lineHeight = 22.sp,
    )

    val titleLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.01.sp,
        lineHeight = 22.sp,
    )

    // 16pt - SemiBold - For section headers, card titles
    val titleMedium = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.01.sp,
        lineHeight = 20.sp,
    )

    // 14pt - Medium - Primary body text, descriptions
    val bodyLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.01.sp,
        lineHeight = 16.sp,
    )

    // 13sp Hint in text fields
    val labelMedium = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic
    )

    // 11pt - Medium - Secondary text, captions, metadata
    val bodySmall = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.01.sp,
        lineHeight = 14.sp,
    )

    // 14pt Link - Medium - Interactive elements, buttons, links
    val labelLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.01.sp,
        lineHeight = 16.sp,
    )

    // 11pt Italic - For subtle notes, disclaimers, hints
    val bodySmallEmphasis = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        letterSpacing = 0.01.sp,
        lineHeight = 14.sp,
    )
}