package net.opendasharchive.openarchive.features.onboarding.components

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat

@Composable
fun HtmlText(
    textRes: Int,
    modifier: Modifier = Modifier,
    linkRes: Int? = null,
    color: Color = MaterialTheme.colorScheme.onBackground,
    linkColor: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: TextUnit = 16.sp,
) {
    val context = LocalContext.current
    val text = stringResource(textRes)
    val linkText = linkRes?.let { stringResource(it) }

    if (linkText != null && text.contains("%1\$s")) {
        // Format text with link, just like XML version does
        val formattedText = text.format(linkText)

        // Use HtmlCompat.fromHtml() like XML version for perfect parity
        val spanned = HtmlCompat.fromHtml(
            formattedText,
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        // Convert Spanned to AnnotatedString
        val annotatedString = spannedToAnnotatedString(spanned, color, linkColor)

        // Use BasicText instead of deprecated ClickableText
        BasicText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.Normal
            ),
            modifier = modifier
        )
    } else {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal)
        )
    }
}

// Convert Android Spanned (from HtmlCompat.fromHtml) to Compose AnnotatedString
// This ensures perfect parity with XML version which also uses HtmlCompat.fromHtml
private fun spannedToAnnotatedString(
    spanned: Spanned,
    color: Color,
    linkColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        val text = spanned.toString()
        append(text)

        // Apply default color first so link/span styles can override it
        addStyle(
            SpanStyle(color = color),
            start = 0,
            end = text.length
        )

        // Get all spans from the Spanned text
        val spans = spanned.getSpans(0, spanned.length, Any::class.java)

        for (span in spans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)

            when (span) {
                // Handle URL spans (links)
                is URLSpan -> {
                    addLink(
                        LinkAnnotation.Url(
                            url = span.url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        ),
                        start = start,
                        end = end
                    )
                }
                // Handle bold text
                is StyleSpan -> {
                    when (span.style) {
                        Typeface.BOLD -> {
                            addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold),
                                start = start,
                                end = end
                            )
                        }
                        Typeface.ITALIC -> {
                            addStyle(
                                SpanStyle(fontStyle = FontStyle.Italic),
                                start = start,
                                end = end
                            )
                        }
                        Typeface.BOLD_ITALIC -> {
                            addStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic
                                ),
                                start = start,
                                end = end
                            )
                        }
                    }
                }
                // Handle underline
                is UnderlineSpan -> {
                    addStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        start = start,
                        end = end
                    )
                }
            }
        }

    }
}