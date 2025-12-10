package net.opendasharchive.openarchive.features.settings.license

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import net.opendasharchive.openarchive.core.presentation.components.PrimaryButton
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.CustomTextField
import net.opendasharchive.openarchive.services.webdav.CreativeCommonsLicenseContent
import net.opendasharchive.openarchive.services.webdav.LicenseCallbacks
import net.opendasharchive.openarchive.services.webdav.LicenseState
import net.opendasharchive.openarchive.services.webdav.WebDavAction
import org.koin.androidx.compose.koinViewModel

@Composable
fun SetupLicenseScreen(
    onNext: () -> Unit = {},
    onCancel: () -> Unit = {},
    viewModel: SetupLicenseViewModel = koinViewModel()
) {

    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SetupLicenseEvent.NavigateNext -> onNext()
                is SetupLicenseEvent.NavigateBack -> onCancel()
            }
        }
    }

    SetupLicenseScreenContent(
        state  = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun SetupLicenseScreenContent(
    state: SetupLicenseState,
    onAction: (SetupLicenseAction) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 80.dp), // Add bottom padding for button
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Description text (hidden in edit mode)
            if (!state.isEditing) {
                val descriptionText = when (state.spaceType) {
                    Space.Type.INTERNET_ARCHIVE -> stringResource(R.string.choose_license)
                    else -> stringResource(R.string.name_your_server)
                }

                Text(
                    text = descriptionText,
                    modifier = Modifier.padding(24.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Server name input (hidden for Internet Archive)
            if (state.spaceType != Space.Type.INTERNET_ARCHIVE) {
                CustomTextField(
                    value = state.serverName,
                    onValueChange = { onAction(SetupLicenseAction.UpdateServerName(it)) },
                    placeholder = stringResource(R.string.server_name_optional),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    onImeAction = { focusManager.clearFocus() }
                )
            }

            // Creative Commons License Section
            CreativeCommonsLicenseContent(
                licenseState = LicenseState(
                    ccEnabled = state.ccEnabled,
                    allowRemix = state.allowRemix,
                    requireShareAlike = state.requireShareAlike,
                    allowCommercial = state.allowCommercial,
                    cc0Enabled = state.cc0Enabled,
                    licenseUrl = state.licenseUrl
                ),
                licenseCallbacks = object : LicenseCallbacks {
                    override fun onCcEnabledChange(enabled: Boolean) {
                        focusManager.clearFocus()
                        onAction(SetupLicenseAction.UpdateCcEnabled(enabled))
                    }

                    override fun onAllowRemixChange(allowed: Boolean) {
                        focusManager.clearFocus()
                        onAction(SetupLicenseAction.UpdateAllowRemix(allowed))
                    }

                    override fun onRequireShareAlikeChange(required: Boolean) {
                        focusManager.clearFocus()
                        onAction(SetupLicenseAction.UpdateRequireShareAlike(required))
                    }

                    override fun onAllowCommercialChange(allowed: Boolean) {
                        focusManager.clearFocus()
                        onAction(SetupLicenseAction.UpdateAllowCommercial(allowed))
                    }

                    override fun onCc0EnabledChange(enabled: Boolean) {
                        focusManager.clearFocus()
                        onAction(SetupLicenseAction.UpdateCc0Enabled(enabled))
                    }
                },
                ccLabelText = stringResource(R.string.set_creative_commons_license_for_all_folders_on_this_server)
            )
        }

        // Button bar at bottom - overlaid on top
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            PrimaryButton(
                text = stringResource(R.string.action_next),
                onClick = { onAction(SetupLicenseAction.Next) },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WebDavSetupLicenseScreenPreview() {
    SaveAppTheme {
        SetupLicenseScreenContent(
            state = SetupLicenseState(
                ccEnabled = true
            ),
            onAction = {}
        )
    }
}

