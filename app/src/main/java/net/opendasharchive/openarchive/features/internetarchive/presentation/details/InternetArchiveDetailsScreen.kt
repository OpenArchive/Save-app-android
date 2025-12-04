package net.opendasharchive.openarchive.features.internetarchive.presentation.details

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.CustomTextField
import net.opendasharchive.openarchive.services.webdav.CreativeCommonsLicenseContent
import net.opendasharchive.openarchive.services.webdav.LicenseCallbacks
import net.opendasharchive.openarchive.services.webdav.LicenseState
import org.koin.androidx.compose.koinViewModel


class InternetArchiveDetailFragment : BaseFragment(), ToolbarConfigurable {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setContent {
                SaveAppTheme {
                    InternetArchiveDetailsScreen(
                        onNavigateBack = {
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
private fun InternetArchiveDetailsScreen(
    viewModel: InternetArchiveDetailsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {

    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is InternetArchiveDetailsEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    val context = LocalContext.current
    val activity = context as FragmentActivity
    val dialogManager = (activity as BaseActivity).dialogManager
    InternetArchiveDetailsContent(state, viewModel::onAction, dialogManager)
}

@Composable
private fun InternetArchiveDetailsContent(
    state: InternetArchiveDetailsState,
    onAction: (InternetArchiveDetailsAction) -> Unit,
    dialogManager: DialogStateManager? = null
) {

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = stringResource(R.string.account),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )

            CustomTextField(
                placeholder = stringResource(R.string.label_username),
                value = state.userName,
                onValueChange = {},
                enabled = false,
            )

            CustomTextField(
                placeholder = stringResource(R.string.label_screen_name),
                value = state.screenName,
                onValueChange = {},
                enabled = false,
            )

            CustomTextField(
                placeholder = stringResource(R.string.label_email),
                value = state.email,
                onValueChange = {},
                enabled = false,
            )

            Text(
                text = stringResource(R.string.license_label),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Creative Commons License integration - now using ViewModel state
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
                        onAction(InternetArchiveDetailsAction.UpdateCcEnabled(enabled))
                    }

                    override fun onAllowRemixChange(allowed: Boolean) {
                        onAction(InternetArchiveDetailsAction.UpdateAllowRemix(allowed))
                    }

                    override fun onRequireShareAlikeChange(required: Boolean) {
                        onAction(InternetArchiveDetailsAction.UpdateRequireShareAlike(required))
                    }

                    override fun onAllowCommercialChange(allowed: Boolean) {
                        onAction(InternetArchiveDetailsAction.UpdateAllowCommercial(allowed))
                    }

                    override fun onCc0EnabledChange(enabled: Boolean) {
                        onAction(InternetArchiveDetailsAction.UpdateCc0Enabled(enabled))
                    }
                },
                ccLabelText = stringResource(R.string.set_creative_commons_license_for_all_folders_on_this_server)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        dialogManager?.showDialog(dialogManager.requireResourceProvider()) {
                            title = UiText.StringResource(R.string.remove_from_app)
                            message =
                                UiText.StringResource(R.string.are_you_sure_you_want_to_remove_this_server_from_the_app)
                            icon = UiImage.DrawableResource(R.drawable.ic_trash)
                            destructiveButton {
                                text = UiText.StringResource(R.string.lbl_remove)
                                action = {
                                    onAction(InternetArchiveDetailsAction.Remove)
                                }
                            }

                            neutralButton {
                                text = UiText.StringResource(R.string.action_cancel)
                                action = {
                                    //dismiss
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(R.color.red_bg)
                    )
                ) {
                    Text(
                        stringResource(id = R.string.remove_from_app),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
private fun InternetArchiveScreenPreview() {
    DefaultScaffoldPreview {
        InternetArchiveDetailsContent(
            state = InternetArchiveDetailsState(
                email = "abc@example.com",
                userName = "@abc_name",
                screenName = "ABC Name",
                license = "https://creativecommons.org/licenses/by-nc-sa/4.0/"
            ),
            onAction = {},
            dialogManager = null
        )
    }
}
