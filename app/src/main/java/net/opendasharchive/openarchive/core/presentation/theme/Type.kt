package net.opendasharchive.openarchive.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import net.opendasharchive.openarchive.R

// Define Montserrat FontFamily
val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat_thin, FontWeight.Thin), // 100
    Font(R.font.montserrat_extra_light, FontWeight.ExtraLight), // 200
    Font(R.font.montserrat_light, FontWeight.Light), // 300
    Font(R.font.montserrat_regular, FontWeight.Normal), // 400
    Font(R.font.montserrat_medium, FontWeight.Medium), // 500
    Font(R.font.montserrat_semi_bold, FontWeight.SemiBold), // 600
    Font(R.font.montserrat_bold, FontWeight.Bold), // 700
    Font(R.font.montserrat_extra_bold, FontWeight.ExtraBold), // 800
    Font(R.font.montserrat_black, FontWeight.Black) // 900
)

// Define Montserrat Italic FontFamily
val MontserratItalicFontFamily = FontFamily(
    Font(R.font.montserrat_thin_italic, FontWeight.Thin), // 100
    Font(R.font.montserrat_extra_light_italic, FontWeight.ExtraLight), // 200
    Font(R.font.montserrat_light_italic, FontWeight.Light), // 300
    Font(R.font.montserrat_italic, FontWeight.Normal), // 400
    Font(R.font.montserrat_medium_italic, FontWeight.Medium), // 500
    Font(R.font.montserrat_semi_bold_italic, FontWeight.SemiBold), // 600
    Font(R.font.montserrat_bold_italic, FontWeight.Bold), // 700
    Font(R.font.montserrat_extra_bold_italic, FontWeight.ExtraBold), // 800
    Font(R.font.montserrat_black_italic, FontWeight.Black) // 900
)


val Typography = Typography(
    headlineSmall = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold // 600
    ),
    bodyLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold // 600
    ),
    bodyMedium = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium // 500
    ),
    bodySmall = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium // 500
    ),
    labelMedium = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium // 500
    ),
    titleLarge = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 22.sp, // Adjust according to UI needs
        fontWeight = FontWeight.Normal // Default for TitleLarge
    ),
    titleMedium = TextStyle(
        fontFamily = MontserratFontFamily,
        fontSize = 18.sp, // Adjust according to UI needs
        fontWeight = FontWeight.Medium // 500
    )
)
