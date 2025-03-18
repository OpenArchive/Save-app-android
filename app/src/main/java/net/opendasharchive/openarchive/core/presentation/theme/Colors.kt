package net.opendasharchive.openarchive.core.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import net.opendasharchive.openarchive.R

private val c23_nav_drawer_night = Color(0xff101010)
private val c23_darker_grey = Color(0xff212021)
private val c23_dark_grey = Color(0xff333333)
private val c23_darker_medium_grey = Color(0xff434343)
private val c23_medium_grey = Color(0xff696666)
private val c23_grey = Color(0xff9f9f9f)
private val c23_grey_50 = Color(0xff777979)
private val c23_light_grey = Color(0xffe3e3e4)
private val c23_teal_100 = Color(0xff00ffeb) // h=175,3 s=100 v=100 -->
private val c23_teal_90 = Color(0xff00e7d5) // v=90.6 -->
private val c23_teal_80 = Color(0xff00cebe) // v=80.6 -->
private val c23_teal = Color(0xff00b4a6) // v=70.6 -->
private val c23_teal_60 = Color(0xff009b8f) // v=60.6 -->
private val c23_teal_50 = Color(0xff008177) // v=50.6 -->
private val c23_teal_40 = Color(0xff00685f) // v=40.6 -->
private val c23_teal_30 = Color(0xff004e48) // v=30.6 -->
private val c23_teal_20 = Color(0xff003530) // v=20.6 -->
private val c23_teal_10 = Color(0xff001b19) // v=10.6 -->
private val c23_powder_blue = Color(0xffaae6e1)

private val inputBackgroundLight = Color(0xfffffbf0)
private val inputBackgroundDark = Color(0xff212021)
private val darkPrimary = Color(0xff000A0A)

@Immutable
data class ColorTheme(
    val material: ColorScheme,
    val primaryDark: Color = c23_teal_40,
    val primaryBright: Color = c23_powder_blue,

    val disabledContainer: Color = c23_teal_20,
    val onDisabledContainer: Color = c23_light_grey,
)

@Composable
internal fun lightColorScheme() = ColorTheme(
    material = lightColorScheme(

        primary = colorResource(R.color.colorPrimary),
        onPrimary = Color.Black,
        primaryContainer = colorResource(R.color.colorPrimaryContainer),
        onPrimaryContainer = colorResource(R.color.colorOnPrimaryContainer),

        secondary = colorResource(R.color.colorSecondary),
        onSecondary = colorResource(R.color.colorOnSecondary),
        secondaryContainer = colorResource(R.color.colorSecondaryContainer),
        onSecondaryContainer = colorResource(R.color.colorOnSecondaryContainer),

        tertiary = colorResource(R.color.colorTertiary),
        onTertiary = colorResource(R.color.colorSecondary),
        tertiaryContainer = c23_powder_blue,
        onTertiaryContainer = Color.Black,

        error = Color.Red,
        onError = Color.Black,
        errorContainer = Color.Red,
        onErrorContainer = Color.Black,

        background = colorResource(R.color.colorBackground),
        onBackground = colorResource(R.color.colorOnBackground),

        surface = Color.White,
        onSurface = colorResource(R.color.colorOnSurface),
        surfaceVariant = c23_grey,
        onSurfaceVariant = c23_darker_medium_grey,

        outline = Color.Black,
        inverseOnSurface = Color.White,
        inverseSurface = c23_dark_grey,
        inversePrimary = Color.Black,
        surfaceTint = c23_teal,
        outlineVariant = c23_darker_grey,
        scrim = c23_light_grey,
        surfaceBright = c23_light_grey,
        surfaceContainer = Color.White,
        surfaceContainerHighest = inputBackgroundLight,
        surfaceDim = c23_light_grey
    ),
)

@Composable
internal fun darkColorScheme() = ColorTheme(
    material = darkColorScheme(

        primary = colorResource(R.color.colorPrimary),
        onPrimary = Color.White,
        primaryContainer = colorResource(R.color.colorPrimaryContainer),
        onPrimaryContainer = colorResource(R.color.colorOnPrimaryContainer),

        secondary = colorResource(R.color.colorSecondary),
        onSecondary = colorResource(R.color.colorOnSecondary),
        secondaryContainer = colorResource(R.color.colorSecondaryContainer),
        onSecondaryContainer = colorResource(R.color.colorOnSecondaryContainer),

        tertiary = colorResource(R.color.colorTertiary),
        onTertiary = colorResource(R.color.colorSecondary),
        tertiaryContainer = c23_powder_blue,
        onTertiaryContainer = Color.Black,

        error = Color.Red,
        onError = Color.Black,
        errorContainer = Color.Red,
        onErrorContainer = Color.Black,

        background = colorResource(R.color.colorBackground),
        onBackground = colorResource(R.color.colorOnBackground),

        surface = c23_darker_grey,
        onSurface = Color.White,
        surfaceVariant = c23_dark_grey,
        onSurfaceVariant = c23_light_grey,

        outline = Color.White,
        inverseSurface = c23_light_grey,
        inverseOnSurface = Color.Black,
        inversePrimary = Color.White,
        surfaceTint = c23_teal,
        outlineVariant = c23_light_grey,
        scrim = c23_light_grey,
        surfaceBright = c23_grey,
        surfaceContainer = c23_medium_grey,
        surfaceContainerHighest = inputBackgroundDark,
        surfaceDim = c23_dark_grey
    ),
)

val LocalColors = staticCompositionLocalOf<ColorTheme> {
    error("LocalColors not provided. Wrap your composables in the Theme function.")
}