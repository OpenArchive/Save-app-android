package net.opendasharchive.openarchive.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLightDark
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.services.internetarchive.presentation.login.CustomTextField

@Composable
fun FolderDetailScreen(
    viewModel: FolderDetailViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FolderDetailScreenContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun FolderDetailScreenContent(
    state: FolderDetailState,
    onAction: (FolderDetailAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Label
        Text(
            text = stringResource(R.string.folder_name),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(R.color.colorOnBackground),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Folder name text field
        CustomTextField(
            value = state.folderName,
            onValueChange = { onAction(FolderDetailAction.UpdateFolderName(it)) },
            placeholder = stringResource(R.string.folder_name),
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            enabled = !state.isArchived
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Buttons container centered
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Archive/Unarchive button
            TextButton(
                onClick = {
                    if (state.isArchived) {
                        onAction(FolderDetailAction.UnarchiveProject)
                    } else {
                        onAction(FolderDetailAction.ArchiveProject)
                    }
                },
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = if (state.isArchived) {
                        stringResource(R.string.action_unarchive_project)
                    } else {
                        stringResource(R.string.action_archive_project)
                    },
                    fontSize = 18.sp,
                    color = colorResource(R.color.colorOnPrimaryContainer)
                )
            }

            // Remove from app button
            Text(
                text = stringResource(R.string.remove_from_app),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.red_bg),
                modifier = Modifier
                    .wrapContentWidth()
                    .clickable { onAction(FolderDetailAction.ShowRemoveDialog) }
                    .padding(16.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FolderDetailScreenPreview() {
    SaveAppTheme {
        FolderDetailScreenContent(
            state = FolderDetailState(
                projectId = 1L,
                folderName = "My Folder",
                isArchived = true,
            ),
            onAction = {}
        )
    }
}