package net.opendasharchive.openarchive.features.folders

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLightDark
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.ButtonData
import net.opendasharchive.openarchive.features.core.dialog.DialogConfig
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.folders.Folder
import net.opendasharchive.openarchive.util.DateUtils
import org.koin.androidx.compose.koinViewModel

@Composable
fun BrowseFolderScreen(
    viewModel: BrowseFoldersViewModel = koinViewModel(),
    onNavigateBackWithResult: (Long) -> Unit = {},
    onFolderSelected: (Folder?) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val dialogManager = (activity as? BaseActivity)?.dialogManager

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowseFoldersEvent.ShowSuccessDialog -> {
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

                is BrowseFoldersEvent.NavigateBackWithResult -> {
                    onNavigateBackWithResult(event.projectId)
                }
            }
        }
    }

    // Notify parent about selected folder for toolbar menu
    LaunchedEffect(state.selectedFolder) {
        onFolderSelected(state.selectedFolder)
    }

    BrowseFolderScreenContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun BrowseFolderScreenContent(
    state: BrowseFoldersState,
    onAction: (BrowseFoldersAction) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            state.folders.isEmpty() -> {
                Text(
                    text = stringResource(R.string.no_more_folders),
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(state.folders, key = { it.name }) { folder ->
                        BrowseFolderItem(
                            folder = folder,
                            isSelected = state.selectedFolder == folder,
                            onClick = { onAction(BrowseFoldersAction.SelectFolder(folder)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrowseFolderItem(
    folder: Folder,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        colorResource(R.color.colorTertiary)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(8.dp)
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_folder_new),
            contentDescription = null,
            tint = colorResource(R.color.colorOnBackground)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp, horizontal = 16.dp)
        ) {
            Text(
                text = folder.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Timestamp is hidden in XML (visibility="gone"), but keeping structure for future
            // Uncomment below if you want to show timestamp:
            // Text(
            //     text = java.text.SimpleDateFormat.getDateTimeInstance(
            //         java.text.SimpleDateFormat.LONG,
            //         java.text.SimpleDateFormat.MEDIUM
            //     ).format(folder.modified.toJavaDate()),
            //     fontSize = 15.sp,
            //     color = MaterialTheme.colorScheme.onSurfaceVariant
            // )
        }

        // Always render the checkmark icon to reserve space, but control visibility
        Icon(
            painter = painterResource(R.drawable.ic_done),
            contentDescription = if (isSelected) stringResource(R.string.lbl_select_media) else null,
            tint = colorResource(R.color.colorOnBackground),
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
                .alpha(if (isSelected) 1f else 0f)
        )
    }
}

@PreviewLightDark
@Composable
private fun BrowseFolderScreenPreview() {
    SaveAppTheme {
        BrowseFolderScreenContent(
            state = BrowseFoldersState(
                folders = listOf(
                    Folder(name = "Documents", modified = DateUtils.nowDateTime),
                    Folder(name = "Photos", modified = DateUtils.nowDateTime),
                    Folder(name = "Videos", modified = DateUtils.nowDateTime),
                    Folder(name = "Downloads", modified = DateUtils.nowDateTime),
                    Folder(name = "Projects", modified = DateUtils.nowDateTime),
                ),
                selectedFolder = Folder(name = "Photos", modified = DateUtils.nowDateTime)
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrowseFolderScreenEmptyPreview() {
    DefaultScaffoldPreview {
        BrowseFolderScreenContent(
            state = BrowseFoldersState(
                folders = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BrowseFolderScreenLoadingPreview() {
    DefaultScaffoldPreview {
        BrowseFolderScreenContent(
            state = BrowseFoldersState(
                folders = emptyList(),
                isLoading = true
            ),
            onAction = {}
        )
    }
}
