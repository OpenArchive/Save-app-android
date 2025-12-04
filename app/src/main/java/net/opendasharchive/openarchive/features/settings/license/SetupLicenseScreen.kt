package net.opendasharchive.openarchive.features.settings.license

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import androidx.compose.ui.res.colorResource
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.CustomTextField
import net.opendasharchive.openarchive.services.webdav.CreativeCommonsLicenseContent
import net.opendasharchive.openarchive.services.webdav.LicenseCallbacks
import net.opendasharchive.openarchive.services.webdav.LicenseState
import net.opendasharchive.openarchive.services.webdav.WebDavAction
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupLicenseScreenContent(
    state: SetupLicenseState,
    onAction: (SetupLicenseAction) -> Unit
) {

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = if (state.isEditing) 16.dp else 100.dp)
        ) {
            // Content section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Description text (hidden in edit mode)
                if (!state.isEditing) {
                    Text(
                        text = stringResource(R.string.name_your_server),
                        modifier = Modifier.padding(24.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Server name input
                CustomTextField(
                    value = state.serverName,
                    onValueChange = { onAction(SetupLicenseAction.UpdateServerName(it)) },
                    placeholder = stringResource(R.string.server_name_optional),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                )


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
                            onAction(SetupLicenseAction.UpdateCcEnabled(enabled))
                        }

                        override fun onAllowRemixChange(allowed: Boolean) {
                            onAction(SetupLicenseAction.UpdateAllowRemix(allowed))
                        }

                        override fun onRequireShareAlikeChange(required: Boolean) {
                            onAction(SetupLicenseAction.UpdateRequireShareAlike(required))
                        }

                        override fun onAllowCommercialChange(allowed: Boolean) {
                            onAction(SetupLicenseAction.UpdateAllowCommercial(allowed))
                        }

                        override fun onCc0EnabledChange(enabled: Boolean) {
                            onAction(SetupLicenseAction.UpdateCc0Enabled(enabled))
                        }
                    },
                    ccLabelText = stringResource(R.string.set_creative_commons_license_for_all_folders_on_this_server)
                )
            }


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Spacer(modifier = Modifier.weight(1f))

                    // Next button with rounded corners
                    Button(
                        onClick = { onAction(SetupLicenseAction.Next) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp), // Match Material button rounding
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.colorTertiary)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.action_next),
                            fontWeight = FontWeight.Medium // Match XML textFontWeight="500"
                        )
                    }
                }

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

