package net.opendasharchive.openarchive.services.webdav

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeColors
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.ToolbarConfigurable
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.ButtonData
import net.opendasharchive.openarchive.features.core.dialog.DialogConfig
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.core.dialog.showErrorDialog
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.CustomSecureField
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.CustomTextField
import net.opendasharchive.openarchive.util.NetworkUtils
import org.koin.androidx.compose.koinViewModel

class WebDavScreenFragment : BaseFragment(), ToolbarConfigurable {

    private val args: WebDavScreenFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SaveAppTheme {
                    WebDavScreen(
                        onNavigateToLicenseSetup = { spaceId ->
                            val action = WebDavScreenFragmentDirections
                                .actionFragmentWebDavToFragmentSetupLicense(
                                    spaceId = spaceId,
                                    spaceType = Space.Type.WEBDAV
                                )
                            findNavController().navigate(action)
                        },
                        onNavigateBack = {
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle back press with unsaved changes check
        if (args.spaceId != WebDavViewModel.ARG_VAL_NEW_SPACE) {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                // This will be handled by the Compose screen via events
                findNavController().popBackStack()
            }
        }
    }

    override fun getToolbarTitle(): String {
        return if (args.spaceId == WebDavViewModel.ARG_VAL_NEW_SPACE) {
            getString(R.string.private_server)
        } else {
            val space = Space.get(args.spaceId)
            when {
                space?.name?.isNotBlank() == true -> space.name
                space?.friendlyName?.isNotBlank() == true -> space.friendlyName
                else -> getString(R.string.private_server)
            }
        }
    }

    override fun shouldShowBackButton() = true
}

@Composable
private fun WebDavScreen(
    viewModel: WebDavViewModel = koinViewModel(),
    onNavigateToLicenseSetup: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as FragmentActivity
    val dialogManager = (activity as BaseActivity).dialogManager

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WebDavEvent.NavigateToLicenseSetup -> {
                    onNavigateToLicenseSetup(event.spaceId)
                }

                is WebDavEvent.NavigateBack -> {
                    onNavigateBack()
                }

                is WebDavEvent.ShowUnsavedChangesDialog -> {
                    dialogManager.showDialog(
                        DialogConfig(
                            type = DialogType.Warning,
                            title = UiText.StringResource(R.string.unsaved_changes),
                            message = UiText.StringResource(R.string.do_you_want_to_save),
                            icon = UiImage.DynamicVector(Icons.Default.Warning),
                            positiveButton = ButtonData(
                                text = UiText.StringResource(R.string.lbl_save),
                                action = { viewModel.onAction(WebDavAction.SaveChanges) }
                            ),
                            neutralButton = ButtonData(
                                text = UiText.StringResource(R.string.lbl_discard),
                                action = { viewModel.onAction(WebDavAction.DiscardChanges) }
                            )
                        )
                    )
                }

                is WebDavEvent.ShowRemoveConfirmationDialog -> {
                    dialogManager.showDialog(
                        DialogConfig(
                            type = DialogType.Warning,
                            title = UiText.StringResource(R.string.remove_from_app),
                            message = UiText.StringResource(R.string.are_you_sure_you_want_to_remove_this_server_from_the_app),
                            icon = UiImage.DrawableResource(R.drawable.ic_trash),
                            destructiveButton = ButtonData(
                                text = UiText.StringResource(R.string.lbl_remove),
                                action = { viewModel.onAction(WebDavAction.ConfirmRemoveSpace) }
                            ),
                            neutralButton = ButtonData(
                                text = UiText.StringResource(R.string.lbl_Cancel),
                                action = {}
                            )
                        )
                    )
                }

                is WebDavEvent.ShowSuccessDialog -> {
                    dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                        type = DialogType.Success
                        title = UiText.StringResource(R.string.label_success_title)
                        message = UiText.StringResource(R.string.msg_edit_server_success)
                        icon = UiImage.DrawableResource(R.drawable.ic_done)
                        positiveButton {
                            text = UiText.StringResource(R.string.lbl_got_it)
                            action = { onNavigateBack() }
                        }
                    }
                }

                is WebDavEvent.ShowError -> {
                    dialogManager.showErrorDialog(
                        message = event.message.asString(context),
                        title = context.getString(R.string.error)
                    )
                }
            }
        }
    }

    WebDavContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun WebDavContent(
    state: WebDavState,
    onAction: (WebDavAction) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 100.dp)
        ) {
            // Header section (only for new server)
            if (!state.isEditMode) {
                WebDavHeader(
                    modifier = Modifier
                        .padding(top = 48.dp, bottom = 24.dp)
                        .padding(end = 24.dp)
                )
            }

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
                    onAction(WebDavAction.ClearError)
                    onAction(WebDavAction.UpdateServerUrl(it))
                },
                label = stringResource(R.string.enter_url),
                placeholder = stringResource(R.string.enter_url),
                enabled = !state.isEditMode,
                isError = state.serverError != null,
                isLoading = state.isLoading,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
                onFocusChange = { isFocused ->
                    if (!isFocused && !state.isEditMode) {
                        onAction(WebDavAction.FixServerUrl)
                    }
                }
            )

            Spacer(modifier = Modifier.height(ThemeDimensions.spacing.medium))

            // Name field (only in edit mode)
            if (state.isEditMode) {
                CustomTextField(
                    value = state.name,
                    onValueChange = { onAction(WebDavAction.UpdateName(it)) },
                    label = stringResource(R.string.server_name_optional),
                    placeholder = stringResource(R.string.server_name_optional),
                    enabled = true,
                    isLoading = state.isLoading,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        // Trigger save when user presses Done on keyboard
                        if (state.isNameChanged) {
                            onAction(WebDavAction.SaveChanges)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(ThemeDimensions.spacing.large))
            }

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
                    onAction(WebDavAction.ClearError)
                    onAction(WebDavAction.UpdateUsername(it))
                },
                label = stringResource(R.string.prompt_username),
                placeholder = stringResource(R.string.prompt_username),
                enabled = !state.isEditMode,
                isError = state.usernameError != null || state.isCredentialsError,
                isLoading = state.isLoading,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(ThemeDimensions.spacing.medium))

            // Password field
            CustomSecureField(
                value = state.password,
                onValueChange = {
                    onAction(WebDavAction.ClearError)
                    onAction(WebDavAction.UpdatePassword(it))
                },
                label = stringResource(R.string.prompt_password),
                placeholder = stringResource(R.string.prompt_password),
                isError = state.passwordError != null || state.isCredentialsError,
                isLoading = state.isLoading || state.isEditMode,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
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

            // License Section (only in edit mode)
            if (state.isEditMode) {
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
                    licenseCallbacks = object : LicenseCallbacks {
                        override fun onCcEnabledChange(enabled: Boolean) {
                            onAction(WebDavAction.UpdateCcEnabled(enabled))
                        }

                        override fun onAllowRemixChange(allowed: Boolean) {
                            onAction(WebDavAction.UpdateAllowRemix(allowed))
                        }

                        override fun onRequireShareAlikeChange(required: Boolean) {
                            onAction(WebDavAction.UpdateRequireShareAlike(required))
                        }

                        override fun onAllowCommercialChange(allowed: Boolean) {
                            onAction(WebDavAction.UpdateAllowCommercial(allowed))
                        }

                        override fun onCc0EnabledChange(enabled: Boolean) {
                            onAction(WebDavAction.UpdateCc0Enabled(enabled))
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
                        onClick = { onAction(WebDavAction.RemoveSpace) },
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
        }

        // Button bar (only for new server)
        if (!state.isEditMode) {
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
                    onClick = { onAction(WebDavAction.Cancel) }
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
                            onAction(WebDavAction.Authenticate)
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
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "WebDav New Server Dark")
@Composable
private fun WebDavNewServerPreview() {
    DefaultScaffoldPreview {
        WebDavContent(
            state = WebDavState(
                isEditMode = false,
                serverUrl = "",
                username = "",
                password = ""
            ),
            onAction = {}
        )
    }
}

//@Preview(showBackground = true, name = "WebDav New Server Filled")
@Composable
private fun WebDavNewServerFilledPreview() {
    DefaultScaffoldPreview {
        WebDavContent(
            state = WebDavState(
                isEditMode = false,
                serverUrl = "https://cloud.example.com",
                username = "user@example.com",
                password = "password123"
            ),
            onAction = {}
        )
    }
}

//@Preview(showBackground = true, name = "WebDav New Server Error")
@Composable
private fun WebDavNewServerErrorPreview() {
    DefaultScaffoldPreview {
        WebDavContent(
            state = WebDavState(
                isEditMode = false,
                serverUrl = "https://cloud.example.com",
                username = "user@example.com",
                password = "wrongpassword",
                isCredentialsError = true,
                usernameError = UiText.DynamicString(" "),
                passwordError = UiText.DynamicString(" ")
            ),
            onAction = {}
        )
    }
}

//@Preview(showBackground = true, name = "WebDav Edit Mode")
//@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "WebDav Edit Mode Dark")
@Composable
private fun WebDavEditModePreview() {
    DefaultScaffoldPreview {
        WebDavContent(
            state = WebDavState(
                isEditMode = true,
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

//@Preview(showBackground = true, name = "WebDav Loading")
@Composable
private fun WebDavLoadingPreview() {
    DefaultScaffoldPreview {
        WebDavContent(
            state = WebDavState(
                isEditMode = false,
                serverUrl = "https://cloud.example.com",
                username = "user@example.com",
                password = "password123",
                isLoading = true
            ),
            onAction = {}
        )
    }
}
