package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import kotlinx.coroutines.delay
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeColors
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.util.NetworkUtils
import org.koin.androidx.compose.koinViewModel


class InternetArchiveLoginFragment : BaseFragment(), ToolbarConfigurable {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setContent {
                SaveAppTheme {
                    InternetArchiveLoginScreen(
                        onLoginSuccess = { spaceId ->
                            val action =
                                InternetArchiveLoginFragmentDirections.actionFragmentInternetArchiveLoginToFragmentSetupLicense(
                                    spaceId = spaceId,
                                    isEditing = false,
                                    spaceType = Space.Type.INTERNET_ARCHIVE
                                )
                            findNavController().navigate(action)
                        },
                        onCancel = {
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
    }

    override fun getToolbarTitle() = getString(R.string.internet_archive)
    override fun shouldShowBackButton() = true
}

@Composable
private fun InternetArchiveLoginScreen(
    onLoginSuccess: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: InternetArchiveLoginViewModel = koinViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is InternetArchiveLoginEvent.NavigateToSignup -> {
                    launcher.launch(
                        Intent(
                            Intent.ACTION_VIEW, "https://archive.org/account/signup".toUri()
                        )
                    )
                }

                is InternetArchiveLoginEvent.NavigateBack -> onCancel()

                is InternetArchiveLoginEvent.LoginSuccess -> {
                    onLoginSuccess(event.spaceId)
                }

                is InternetArchiveLoginEvent.LoginError -> {
                    // Error handling can be done here if needed
                }
            }
        }
    }

    InternetArchiveLoginContent(state, viewModel::onAction)
}

@Composable
private fun InternetArchiveLoginContent(
    state: InternetArchiveLoginState,
    onAction: (InternetArchiveLoginAction) -> Unit
) {

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp, bottom = 16.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        InternetArchiveHeader(
            modifier = Modifier
                .padding(vertical = 48.dp)
                .padding(end = 24.dp)
        )



        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                Text(
                    stringResource(R.string.account),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        CustomTextField(
            value = state.username,
            onValueChange = {
                onAction(InternetArchiveLoginAction.ErrorClear)
                onAction(InternetArchiveLoginAction.UpdateUsername(it))
            },
            label = stringResource(R.string.label_username),
            placeholder = stringResource(R.string.prompt_email),
            isError = state.isUsernameError,
            isLoading = state.isBusy,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        )

        Spacer(Modifier.height(ThemeDimensions.spacing.large))

        CustomSecureField(
            value = state.password,
            onValueChange = {
                onAction(InternetArchiveLoginAction.ErrorClear)
                onAction(InternetArchiveLoginAction.UpdatePassword(it))
            },
            label = stringResource(R.string.label_password),
            placeholder = stringResource(R.string.prompt_password),
            isError = state.isPasswordError,
            isLoading = state.isBusy,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            AnimatedVisibility(
                visible = state.isLoginError,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = stringResource(R.string.error_incorrect_email_or_password),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(ThemeDimensions.spacing.large))
        Row(
            modifier = Modifier
                .padding(top = ThemeDimensions.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.prompt_no_account),
                style = MaterialTheme.typography.bodyLarge.copy( // reuse your themed style
                    color = ThemeColors.material.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
            )
            TextButton(
                modifier = Modifier.heightIn(ThemeDimensions.touchable),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                ),
                onClick = { onAction(InternetArchiveLoginAction.CreateLogin) }
            ) {
                Text(
                    text = stringResource(R.string.label_create_login),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                modifier = Modifier
                    .padding(8.dp)
                    .heightIn(ThemeDimensions.touchable)
                    .weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(R.color.colorOnBackground)
                ),
                enabled = !state.isBusy,
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                onClick = { onAction(InternetArchiveLoginAction.Cancel) }) {
                Text(stringResource(R.string.back), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier
                    .padding(8.dp)
                    .heightIn(ThemeDimensions.touchable)
                    .weight(1f),
                enabled = !state.isBusy && state.isValid,
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    disabledContainerColor = colorResource(R.color.grey_50),
                    disabledContentColor = colorResource(R.color.black),
                    contentColor = colorResource(R.color.black)
                ),
                onClick = {
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        onAction(InternetArchiveLoginAction.Login)
                    } else {
                        Toast.makeText(context, R.string.error_no_internet, Toast.LENGTH_LONG)
                            .show()
                    }
                },
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(color = ThemeColors.material.primary)
                } else {
                    Text(
                        stringResource(R.string.next),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
@Preview
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
private fun InternetArchiveLoginPreview() {
    DefaultScaffoldPreview {
        InternetArchiveLoginContent(
            state = InternetArchiveLoginState(
                username = "",
                password = "",
                isLoginError = true,
                isPasswordError = true,
                isUsernameError = true
            ),
            onAction = {}
        )
    }
}

@Composable
fun CustomTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            fontFamily = MontserratFontFamily
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

@Composable
fun CustomSecureField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean = false,
    isLoading: Boolean = false,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
) {

    var showPassword by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        enabled = !isLoading,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    fontFamily = MontserratFontFamily
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


@Composable
fun ButtonBar(
    modifier: Modifier = Modifier,
    backButtonText: UiText = UiText.StringResource(R.string.back),
    nextButtonText: UiText = UiText.StringResource(R.string.next),
    isBackEnabled: Boolean = false,
    isNextEnabled: Boolean = false,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            modifier = Modifier
                .padding(8.dp)
                .heightIn(ThemeDimensions.touchable)
                .weight(1f),
            colors = ButtonDefaults.textButtonColors(
                contentColor = colorResource(R.color.colorOnBackground)
            ),
            enabled = isBackEnabled,
            shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
            onClick = onBack
        ) {
            Text(backButtonText.asString())
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            modifier = Modifier
                .padding(8.dp)
                .heightIn(ThemeDimensions.touchable)
                .weight(1f),
            enabled = isNextEnabled,
            shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                disabledContainerColor = colorResource(R.color.grey_50),
                disabledContentColor = colorResource(R.color.extra_light_grey)//MaterialTheme.colorScheme.onBackground
            ),
            onClick = onNext,
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = ThemeColors.material.primary)
            } else {
                Text(
                    nextButtonText.asString(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
