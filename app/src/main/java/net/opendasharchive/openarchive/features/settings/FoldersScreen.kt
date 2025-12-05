package net.opendasharchive.openarchive.features.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.db.Project
import org.koin.androidx.compose.koinViewModel
import java.util.Date

@Composable
fun FoldersScreen(
    viewModel: FoldersViewModel = koinViewModel(),
    onNavigateToFolderDetail: (Long) -> Unit = {},
    onNavigateToArchivedFolders: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh data on resume
    LaunchedEffect(Unit) {
        viewModel.onAction(FoldersAction.RefreshFolders)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FoldersEvent.NavigateToFolderDetail -> {
                    onNavigateToFolderDetail(event.projectId)
                }
                is FoldersEvent.NavigateToArchivedFolders -> {
                    onNavigateToArchivedFolders(event.spaceId)
                }
            }
        }
    }

    FoldersScreenContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun FoldersScreenContent(
    state: FoldersState,
    onAction: (FoldersAction) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (state.folders.isEmpty()) {
            // Empty state
            Text(
                text = stringResource(R.string.lbl_no_archived_folders),
                fontSize = 16.sp,
                color = colorResource(R.color.colorOnBackground),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(32.dp)
            )
        } else {
            // Folder list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = if (state.showArchivedMenuItem) {
                        // Button height + button padding + navigation bar insets
                        80.dp
                    } else {
                        // Just navigation bar insets when no button
                        0.dp
                    }
                )
            ) {
                items(state.folders) { folder ->
                    FolderItem(
                        folder = folder,
                        onClick = { onAction(FoldersAction.FolderClicked(folder)) }
                    )
                }

                // Add bottom spacer for navigation bar
                item {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    )
                }
            }
        }

        // View Archived button at bottom
        if (state.showArchivedMenuItem) {
            Button(
                onClick = { onAction(FoldersAction.ViewArchivedClicked) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .heightIn(ThemeDimensions.touchable),
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = colorResource(R.color.black)
                )
            ) {
                Text(
                    stringResource(R.string.view_archived_folders),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
fun FolderItem(
    folder: Project,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_folder_new),
            contentDescription = null,
            tint = colorResource(R.color.colorOnBackground),
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = folder.description ?: "",
            style = MaterialTheme.typography.titleLarge,
            color = colorResource(R.color.colorOnBackground),
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FoldersScreenPreview() {
    SaveAppTheme {
        FoldersScreenContent(
            state = FoldersState(
                folders = listOf(
                    Project("Folder 1", Date(), 1L),
                    Project("Folder 2", Date(), 1L),
                    Project("Very Long Folder Name That Should Wrap", Date(), 1L)
                ),
                isArchived = true,
                showArchivedMenuItem = true,
            ),
            onAction = {}
        )
    }
}
