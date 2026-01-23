package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.ThemeColors
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.util.NetworkUtils

@Composable
fun InternetArchiveLoginScreen(
    viewModel: InternetArchiveLoginViewModel,
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {

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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }

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
            placeholder = stringResource(R.string.prompt_email),
            isError = state.isUsernameError,
            isLoading = state.isBusy,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = {
                passwordFocusRequester.requestFocus()
            }
        )

        Spacer(Modifier.height(ThemeDimensions.spacing.large))

        CustomSecureField(
            value = state.password,
            onValueChange = {
                onAction(InternetArchiveLoginAction.ErrorClear)
                onAction(InternetArchiveLoginAction.UpdatePassword(it))
            },
            placeholder = stringResource(R.string.prompt_password),
            isError = state.isPasswordError,
            isLoading = state.isBusy,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = {
                focusManager.clearFocus()
            },
            modifier = Modifier.focusRequester(passwordFocusRequester)
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
                onClick = {
                    launcher.launch(
                        Intent(
                            Intent.ACTION_VIEW, "https://archive.org/account/signup".toUri()
                        )
                    )
                }
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
