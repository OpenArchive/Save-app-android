package net.opendasharchive.openarchive.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun SaveAppTheme(
    content: @Composable () -> Unit
) {
    val isDarkTheme by rememberUpdatedState(newValue = isSystemInDarkTheme())

    val colors = if (isDarkTheme) darkColorScheme() else lightColorScheme()

    val dimensions = getThemeDimensions(isDarkTheme)

    CompositionLocalProvider(
        LocalDimensions provides dimensions,
        LocalColors provides colors,
    ) {
        MaterialTheme(
            colorScheme = colors.material,
            content = content,
            shapes = Shapes,
            typography = Typography,
        )
    }
}


val ThemeColors: ColorTheme @Composable get() = LocalColors.current
val ThemeDimensions: DimensionsTheme @Composable get() = LocalDimensions.current
