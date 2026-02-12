package net.opendasharchive.openarchive.services.snowbird.presentation.file

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.Evidence
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles


@Composable
fun SnowbirdFileListScreen(
    viewModel: SnowbirdFileViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SnowbirdFileListContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnowbirdFileListContent(
    state: SnowbirdFileState,
    onAction: (SnowbirdFileAction) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { onAction(SnowbirdFileAction.RefreshFiles) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.files.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No files found")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.files) { evidence ->
                    SnowbirdFileItem(
                        evidence = evidence,
                        onClick = { onAction(SnowbirdFileAction.DownloadFile(evidence)) }
                    )
                }
            }
        }
    }
}

@Composable
fun SnowbirdFileItem(
    evidence: Evidence,
    onClick: () -> Unit
) {
    val fileExtension = evidence.title.substringAfterLast(".", "").lowercase()
    val iconRes = when {
        isImageFile(fileExtension) -> R.drawable.ic_image
        isVideoFile(fileExtension) -> R.drawable.ic_videocam
        isAudioFile(fileExtension) -> R.drawable.ic_music
        else -> R.drawable.ic_description
    }

    Card(
        modifier = Modifier
            .height(140.dp)
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    if (!evidence.isDownloaded) {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_cloud_download_24),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                                .padding(2.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = evidence.title,
                    style = SaveTextStyles.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

private fun isImageFile(extension: String) = extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")
private fun isVideoFile(extension: String) = extension in listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "3gp")
private fun isAudioFile(extension: String) = extension in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "wma")

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SnowbirdFileListScreenPreview() {
    SaveAppTheme {
        SnowbirdFileListContent(
            state = SnowbirdFileState(
                files = listOf(
                    Evidence(title = "photo1.jpg", isDownloaded = true),
                    Evidence(title = "video1.mp4", isDownloaded = false),
                    Evidence(title = "audio1.mp3", isDownloaded = true),
                    Evidence(title = "doc1.pdf", isDownloaded = false),
                    Evidence(title = "photo2.png", isDownloaded = true)
                )
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SnowbirdFileListScreenEmptyPreview() {
    DefaultScaffoldPreview {
        SnowbirdFileListContent(
            state = SnowbirdFileState(files = emptyList()),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SnowbirdFileListScreenLoadingPreview() {
    DefaultScaffoldPreview {
        SnowbirdFileListContent(
            state = SnowbirdFileState(files = emptyList(), isLoading = true),
            onAction = {}
        )
    }
}
