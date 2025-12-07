package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions

@Composable
fun CustomTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    placeholder: String? = null,
    isError: Boolean = false,
    isLoading: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onImeAction: (() -> Unit)? = null,
) {

    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.tertiary,
        backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
    )
    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        OutlinedTextField(
            modifier = modifier
                .fillMaxWidth()
                .let { mod ->
                    onFocusChange?.let { callback ->
                        mod.onFocusChanged { callback(it.isFocused) }
                    } ?: mod
                },
            value = value,
            enabled = !isLoading && enabled,
            onValueChange = onValueChange,
            placeholder = {
                placeholder?.let {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colorResource(R.color.colorOnSurfaceVariant)
                        )
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = keyboardType,
                imeAction = imeAction,
                platformImeOptions = PlatformImeOptions(),
                showKeyboardOnFocus = true,
                hintLocales = null
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onImeAction?.invoke()
                },
                onNext = {
                    onImeAction?.invoke()
                },
                onGo = {
                    onImeAction?.invoke()
                },
                onSearch = {
                    onImeAction?.invoke()
                },
                onSend = {
                    onImeAction?.invoke()
                }
            ),
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.tertiary,
                //focusedIndicatorColor = Color.Transparent,
                //unfocusedIndicatorColor = Color.Transparent,
            ),
        )
    }
}