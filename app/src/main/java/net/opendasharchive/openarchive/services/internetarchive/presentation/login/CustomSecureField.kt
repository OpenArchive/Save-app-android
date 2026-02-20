package net.opendasharchive.openarchive.services.internetarchive.presentation.login

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions

@Composable
fun CustomSecureField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    placeholder: String,
    isError: Boolean = false,
    isLoading: Boolean = false,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onImeAction: (() -> Unit)? = null,
) {

    var showPassword by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        enabled = !isLoading && enabled,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = colorResource(R.color.colorOnSurfaceVariant)
                )
            )
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
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        isError = isError,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
            cursorColor = MaterialTheme.colorScheme.tertiary
            //focusedIndicatorColor = Color.Transparent,
            //unfocusedIndicatorColor = Color.Transparent,
        ),
        trailingIcon = {
            IconButton(
                enabled = !isLoading,
                modifier = Modifier.sizeIn(ThemeDimensions.touchable),
                onClick = { showPassword = !showPassword }) {

                val (iconRes, cd) =
                    if (showPassword) {
                        R.drawable.ic_visibility_off to
                                "Hide password" // ideally a stringResource(...)
                    } else {
                        R.drawable.ic_visibility to
                                "Show password"
                    }

                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = cd
                )
            }
        },
    )
}