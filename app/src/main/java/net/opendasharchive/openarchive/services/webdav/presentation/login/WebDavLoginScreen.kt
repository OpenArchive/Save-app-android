package net.opendasharchive.openarchive.services.webdav.presentation.login

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.ThemeColors
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.services.internetarchive.presentation.login.CustomSecureField
import net.opendasharchive.openarchive.services.internetarchive.presentation.login.CustomTextField
import net.opendasharchive.openarchive.util.NetworkUtils

@Composable
fun WebDavLoginScreen(
    viewModel: WebDavLoginViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->

        }
    }

    WebDavContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun WebDavContent(
    state: WebDavLoginState,
    onAction: (WebDavLoginAction) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 100.dp)
        ) {

            // Header section
            WebDavHeader(
                modifier = Modifier
                    .padding(top = 48.dp, bottom = 24.dp)
                    .padding(end = 24.dp)
            )


            // Server Info Section
            Text(
                text = stringResource(R.string.server_info),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Server URL field
            CustomTextField(
                value = state.serverUrl,
                onValueChange = {
                    onAction(WebDavLoginAction.ClearError)
                    onAction(WebDavLoginAction.UpdateServerUrl(it))
                },
                placeholder = stringResource(R.string.enter_url),
                isError = state.serverError != null,
                isLoading = state.isLoading,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
                onImeAction = {
                    usernameFocusRequester.requestFocus()
                },
                onFocusChange = { isFocused ->
                    if (!isFocused) {
                        onAction(WebDavLoginAction.FixServerUrl)
                    }
                }
            )

            Spacer(modifier = Modifier.height(ThemeDimensions.spacing.medium))

            // Account Section
            Text(
                text = stringResource(R.string.account),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
            )

            // Username field
            CustomTextField(
                value = state.username,
                onValueChange = {
                    onAction(WebDavLoginAction.ClearError)
                    onAction(WebDavLoginAction.UpdateUsername(it))
                },
                placeholder = stringResource(R.string.prompt_username),
                isError = state.usernameError != null || state.isCredentialsError,
                isLoading = state.isLoading,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                onImeAction = {
                    passwordFocusRequester.requestFocus()
                },
                modifier = Modifier.focusRequester(usernameFocusRequester)
            )

            Spacer(modifier = Modifier.height(ThemeDimensions.spacing.medium))

            // Password field
            CustomSecureField(
                value = state.password,
                onValueChange = {
                    onAction(WebDavLoginAction.ClearError)
                    onAction(WebDavLoginAction.UpdatePassword(it))
                },
                placeholder = stringResource(R.string.prompt_password),
                isError = state.passwordError != null || state.isCredentialsError,
                isLoading = state.isLoading,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focusManager.clearFocus()
                },
                modifier = Modifier.focusRequester(passwordFocusRequester)
            )

            // Error hint
            AnimatedVisibility(
                visible = state.isCredentialsError,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = stringResource(R.string.error_incorrect_username_or_password),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Button bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            TextButton(
                modifier = Modifier
                    .heightIn(ThemeDimensions.touchable)
                    .weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(R.color.colorOnBackground)
                ),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                onClick = { onAction(WebDavLoginAction.Cancel) }
            ) {
                Text(
                    stringResource(R.string.back),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Next/Authenticate button
            Button(
                modifier = Modifier
                    .heightIn(ThemeDimensions.touchable)
                    .weight(1f),
                enabled = !state.isLoading && state.isFormValid,
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    disabledContainerColor = colorResource(R.color.grey_50),
                    disabledContentColor = colorResource(R.color.black),
                    contentColor = colorResource(R.color.black)
                ),
                onClick = {
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        onAction(WebDavLoginAction.Authenticate)
                    } else {
                        Toast.makeText(context, R.string.error_no_internet, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = ThemeColors.material.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        stringResource(R.string.action_next),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }


        // Loading overlay
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.transparent_loading_overlay)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun WebDavHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.colorBackgroundSpaceIcon))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(32.dp),
                painter = painterResource(id = R.drawable.ic_private_server),
                contentDescription = stringResource(R.string.private_server),
                colorFilter = ColorFilter.tint(colorResource(R.color.colorTertiary))
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stringResource(R.string.save_connects_to_webdav_compatible_servers_only_such_as_nextcloud_and_owncloud),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 32.dp)
        )
    }
}

// Previews
@Preview(showBackground = true, name = "WebDav New Server")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "WebDav New Server Dark"
)
@Composable
private fun WebDavNewServerPreview() {
    DefaultScaffoldPreview {
        WebDavContent(
            state = WebDavLoginState(
                serverUrl = "",
                username = "",
                password = ""
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, name = "WebDav New Server Filled")
@Composable
private fun WebDavNewServerFilledPreview() {
    DefaultScaffoldPreview {
        WebDavContent(
            state = WebDavLoginState(
                serverUrl = "https://cloud.example.com",
                username = "user@example.com",
                password = "password123"
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, name = "WebDav New Server Error")
@Composable
private fun WebDavNewServerErrorPreview() {
    DefaultScaffoldPreview {
        WebDavContent(
            state = WebDavLoginState(
                serverUrl = "https://cloud.example.com",
                username = "user@example.com",
                password = "wrongpassword",
                isCredentialsError = true,
                usernameError = UiText.Dynamic(" "),
                passwordError = UiText.Dynamic(" ")
            ),
            onAction = {}
        )
    }
}


@Preview(showBackground = true, name = "WebDav Loading")
@Composable
private fun WebDavLoadingPreview() {
    DefaultScaffoldPreview {
        WebDavContent(
            state = WebDavLoginState(
                serverUrl = "https://cloud.example.com",
                username = "user@example.com",
                password = "password123",
                isLoading = true
            ),
            onAction = {}
        )
    }
}
