package net.opendasharchive.openarchive.services.webdav.presentation.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
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
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.services.internetarchive.presentation.login.CustomSecureField
import net.opendasharchive.openarchive.services.internetarchive.presentation.login.CustomTextField
import net.opendasharchive.openarchive.services.common.license.CreativeCommonsLicenseContent
import net.opendasharchive.openarchive.services.common.license.LicenseCallbacks
import net.opendasharchive.openarchive.services.common.license.LicenseState

@Composable
fun WebDavDetailScreen(
    viewModel: WebDavDetailViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->

        }
    }

    WebDavContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun WebDavContent(
    state: WebDavDetailState,
    onAction: (WebDavDetailAction) -> Unit,
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 100.dp)
        ) {

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
                    // Do Nothing
                },
                placeholder = stringResource(R.string.enter_url),
                enabled = false,
            )

            Spacer(modifier = Modifier.height(ThemeDimensions.spacing.medium))

            // Name field (only in edit mode)

            CustomTextField(
                value = state.name,
                onValueChange = { onAction(WebDavDetailAction.UpdateName(it)) },
                placeholder = stringResource(R.string.server_name_optional),
                enabled = true,
                isLoading = state.isLoading,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focusManager.clearFocus()
                    // Trigger save when user presses Done on keyboard if there are any unsaved changes
                    if (state.hasUnsavedChanges) {
                        onAction(WebDavDetailAction.SaveChanges)
                    }
                }
            )

            Spacer(modifier = Modifier.height(ThemeDimensions.spacing.large))


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
                    // Do nothing
                },
                placeholder = stringResource(R.string.prompt_username),
                enabled = false,
            )

            Spacer(modifier = Modifier.height(ThemeDimensions.spacing.medium))

            // Password field
            CustomSecureField(
                value = state.password,
                onValueChange = {
                    // Do nothing
                },
                placeholder = stringResource(R.string.prompt_password),
                enabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            )


            // License Section (only in edit mode)
            Spacer(modifier = Modifier.height(ThemeDimensions.spacing.large))

            Text(
                text = stringResource(R.string.license_label),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            CreativeCommonsLicenseContent(
                licenseState = LicenseState(
                    ccEnabled = state.ccEnabled,
                    allowRemix = state.allowRemix,
                    requireShareAlike = state.requireShareAlike,
                    allowCommercial = state.allowCommercial,
                    cc0Enabled = state.cc0Enabled,
                    licenseUrl = state.licenseUrl
                ),
                licenseCallbacks = object :
                    LicenseCallbacks {
                    override fun onCcEnabledChange(enabled: Boolean) {
                        focusManager.clearFocus()
                        onAction(WebDavDetailAction.UpdateCcEnabled(enabled))
                    }

                    override fun onAllowRemixChange(allowed: Boolean) {
                        focusManager.clearFocus()
                        onAction(WebDavDetailAction.UpdateAllowRemix(allowed))
                    }

                    override fun onRequireShareAlikeChange(required: Boolean) {
                        focusManager.clearFocus()
                        onAction(WebDavDetailAction.UpdateRequireShareAlike(required))
                    }

                    override fun onAllowCommercialChange(allowed: Boolean) {
                        focusManager.clearFocus()
                        onAction(WebDavDetailAction.UpdateAllowCommercial(allowed))
                    }

                    override fun onCc0EnabledChange(enabled: Boolean) {
                        focusManager.clearFocus()
                        onAction(WebDavDetailAction.UpdateCc0Enabled(enabled))
                    }
                },
                ccLabelText = stringResource(R.string.set_creative_commons_license_for_all_folders_on_this_server)
            )

            // Remove button (edit mode)
            Spacer(modifier = Modifier.height(ThemeDimensions.spacing.large))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { onAction(WebDavDetailAction.RemoveSpace) },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(R.color.red_bg)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.remove_from_app),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
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

// Previews
@Preview(showBackground = true, name = "WebDav Edit Mode")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "WebDav Edit Mode Dark"
)
@Composable
private fun WebDavEditModePreview() {
    DefaultScaffoldPreview {
        WebDavContent(
            state = WebDavDetailState(
                spaceId = 1L,
                serverUrl = "https://cloud.example.com/remote.php/webdav/",
                username = "user@example.com",
                password = "password123",
                name = "My Cloud Server",
                originalName = "My Cloud Server",
                ccEnabled = true,
                allowRemix = true,
                requireShareAlike = true,
                allowCommercial = false,
                licenseUrl = "https://creativecommons.org/licenses/by-nc-sa/4.0/"
            ),
            onAction = {}
        )
    }
}