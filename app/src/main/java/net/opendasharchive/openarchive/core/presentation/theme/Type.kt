package net.opendasharchive.openarchive.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import net.opendasharchive.openarchive.R

// Define Montserrat FontFamily
val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat_thin, weight = FontWeight.Thin, style = FontStyle.Normal), // 100
    Font(R.font.montserrat_extra_light, weight = FontWeight.ExtraLight, style = FontStyle.Normal), // 200
    Font(R.font.montserrat_light, weight = FontWeight.Light, style = FontStyle.Normal), // 300
    Font(R.font.montserrat_regular, weight = FontWeight.Normal, style = FontStyle.Normal), // 400
    Font(R.font.montserrat_medium, weight = FontWeight.Medium, style = FontStyle.Normal), // 500
    Font(R.font.montserrat_semi_bold, weight = FontWeight.SemiBold, style = FontStyle.Normal), // 600
    Font(R.font.montserrat_bold, weight = FontWeight.Bold, style = FontStyle.Normal), // 700
    Font(R.font.montserrat_extra_bold, weight = FontWeight.ExtraBold, style = FontStyle.Normal), // 800
    Font(R.font.montserrat_black, weight = FontWeight.Black, style = FontStyle.Normal), // 900

    Font(R.font.montserrat_thin_italic, weight = FontWeight.Thin, style = FontStyle.Italic), // 100
    Font(R.font.montserrat_extra_light_italic, weight = FontWeight.ExtraLight, style = FontStyle.Italic), // 200
    Font(R.font.montserrat_light_italic, weight = FontWeight.Light, style = FontStyle.Italic), // 300
    Font(R.font.montserrat_italic, weight = FontWeight.Normal, style = FontStyle.Italic), // 400
    Font(R.font.montserrat_medium_italic, weight = FontWeight.Medium, style = FontStyle.Italic), // 500
    Font(R.font.montserrat_semi_bold_italic, weight = FontWeight.SemiBold, style = FontStyle.Italic), // 600
    Font(R.font.montserrat_bold_italic, weight = FontWeight.Bold, style = FontStyle.Italic), // 700
    Font(R.font.montserrat_extra_bold_italic, weight = FontWeight.ExtraBold, style = FontStyle.Italic), // 800
    Font(R.font.montserrat_black_italic, weight = FontWeight.Black, style = FontStyle.Italic) // 900
)


// Updated Typography to integrate with your 6 Figma text styles
val Typography = Typography(
    // Display styles - Keep Material Design defaults with Montserrat
    displayLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Normal
    ),
    displayMedium = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Normal
    ),
    displaySmall = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Normal
    ),

    // Headlines - Keep Material defaults
    headlineLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = SaveTextStyles.headlineSmall,   // 24sp, ExtraBold

    // Titles - Map to your Figma styles
    titleLarge = SaveTextStyles.titleLarge,    // 18sp, SemiBold
    titleMedium = SaveTextStyles.titleMedium,  // 16sp, SemiBold
    titleSmall = SaveTextStyles.bodyLarge,     // 14sp, Medium

    // Body text - Map to your Figma styles
    bodyLarge = SaveTextStyles.bodyLarge,      // 14sp, Medium
    bodyMedium = SaveTextStyles.bodyLarge,     // 14sp, Medium (reuse)
    bodySmall = SaveTextStyles.bodySmall,      // 11sp, Medium

    // Labels - Map to your Figma styles
    labelLarge = SaveTextStyles.labelLarge,    // 14sp, Medium (for links/actions)
    labelMedium = SaveTextStyles.labelMedium,    // 13sp, Italic Medium
    labelSmall = SaveTextStyles.bodySmall      // 11sp, Medium
)
