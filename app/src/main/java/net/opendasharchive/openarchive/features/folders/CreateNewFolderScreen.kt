package net.opendasharchive.openarchive.features.folders

import android.content.res.Configuration
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.ButtonData
import net.opendasharchive.openarchive.features.core.dialog.DialogConfig
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.CustomTextField
import org.koin.androidx.compose.koinViewModel

@Composable
fun CreateNewFolderScreen(
    viewModel: CreateNewFolderViewModel = koinViewModel(),
    onNavigateBackWithResult: (Long) -> Unit = {},
    onNavigateBackCanceled: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val dialogManager = (activity as? BaseActivity)?.dialogManager

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateNewFolderEvent.ShowSuccessDialog -> {
                    dialogManager?.showDialog(
                        DialogConfig(
                            type = DialogType.Success,
                            title = UiText.Resource(R.string.label_success_title),
                            message = UiText.Resource(R.string.create_folder_ok_message),
                            icon = UiImage.DrawableResource(R.drawable.ic_done),
                            positiveButton = ButtonData(
                                text = UiText.Resource(R.string.label_got_it),
                                action = { viewModel.navigateBackWithResult(event.projectId) }
                            ),
                            onDismissAction = { viewModel.navigateBackWithResult(event.projectId) }
                        )
                    )
                }

                is CreateNewFolderEvent.NavigateBackWithResult -> {
                    onNavigateBackWithResult(event.projectId)
                }

                is CreateNewFolderEvent.NavigateBackCanceled -> {
                    onNavigateBackCanceled()
                }

                is CreateNewFolderEvent.ShowError -> {
                    Toast.makeText(context, event.message.asString(context), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    CreateNewFolderScreenContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun CreateNewFolderScreenContent(
    state: CreateNewFolderState,
    onAction: (CreateNewFolderAction) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 64.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = stringResource(R.string.create_new_folder_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.create_new_folder_subtitle),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Folder name input
            CustomTextField(
                value = state.folderName,
                onValueChange = { onAction(CreateNewFolderAction.UpdateFolderName(it)) },
                placeholder = stringResource(R.string.create_new_folder_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focusManager.clearFocus()
                    if (state.isValid) {
                        onAction(CreateNewFolderAction.CreateFolder)
                    }
                }
            )

            // CC License section is hidden in XML (visibility="gone")
            // Commenting it out but keeping structure for future
            // CreativeCommonsLicenseContent(...)
        }

        // Button bar at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cancel button
            TextButton(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(ThemeDimensions.touchable),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(R.color.colorOnBackground)
                ),
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                onClick = { onAction(CreateNewFolderAction.Cancel) }
            ) {
                Text(
                    stringResource(R.string.lbl_Cancel),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Create button
            Button(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(ThemeDimensions.touchable),
                enabled = state.isValid,
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    disabledContainerColor = colorResource(R.color.grey_50),
                    disabledContentColor = colorResource(R.color.black),
                    contentColor = colorResource(R.color.black)
                ),
                onClick = { onAction(CreateNewFolderAction.CreateFolder) }
            ) {
                Text(
                    stringResource(R.string.create),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CreateNewFolderScreenPreview() {
    SaveAppTheme {
        CreateNewFolderScreenContent(
            state = CreateNewFolderState(
                folderName = "My New Folder",
                isValid = true
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateNewFolderScreenEmptyPreview() {
    DefaultScaffoldPreview {
        CreateNewFolderScreenContent(
            state = CreateNewFolderState(),
            onAction = {}
        )
    }
}
