package net.opendasharchive.openarchive.features.main.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.features.media.PreviewActivity
import net.opendasharchive.openarchive.upload.BroadcastManager
import net.opendasharchive.openarchive.upload.UploadManagerActivity
import org.koin.androidx.compose.koinViewModel

/**
 * A data class representing one “section” (i.e. one Collection and its list of Media).
 * (Here we wrap the list of media in a mutableStateListOf so that updates trigger recomposition.)
 */
data class CollectionSection(
    val collection: Collection,
    val media: SnapshotStateList<Media> = mutableStateListOf<Media>().apply { addAll(collection.media) }
)

@Composable
fun MainMediaScreen(
    projectId: Long,
) {
    val context = LocalContext.current

    // State holding our list of sections (each collection with its media)
    val sections = remember { mutableStateListOf<CollectionSection>() }
    // Flag to track if any media is “selected” (for deletion)
    var isSelecting by remember { mutableStateOf(false) }
    // State to control showing the “delete confirmation” dialog.
    var showDeleteDialog by remember { mutableStateOf(false) }
    // State to control showing an error/retry dialog for a media item.
    var errorDialogData by remember { mutableStateOf<Media?>(null) }


    // Handle broadcast messages
    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = BroadcastManager.getAction(intent) ?: return
                when (action) {
                    BroadcastManager.Action.Change -> {
                        // Extract extras from the intent (assuming these keys are provided)
                        val collectionId = intent.getLongExtra("collectionId", -1)
                        val mediaId = intent.getLongExtra("mediaId", -1)
                        val progress = intent.getIntExtra("progress", 0)
                        val isUploaded = intent.getBooleanExtra("isUploaded", false)
                        if (collectionId != -1L && mediaId != -1L) {
                            handler.post {
                                updateMediaItem(
                                    sections = sections,
                                    collectionId = collectionId,
                                    mediaId = mediaId,
                                    progress = progress,
                                    isUploaded = isUploaded
                                )
                            }
                        }
                    }

                    BroadcastManager.Action.Delete -> {
                        handler.post { refreshSections(projectId, sections) }
                    }
                }
            }
        }

        BroadcastManager.register(context, receiver)
        onDispose { BroadcastManager.unregister(context, receiver) }
    }

    LaunchedEffect(projectId) {
        refreshSections(projectId, sections)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (sections.isEmpty()) {
            WelcomeMessage()
        } else {
            // Use a LazyColumn to list each collection section vertically.
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sections, key = { it.collection.id }) { section ->
                    CollectionSectionView(
                        section = section,
                        onMediaClick = { media ->
                            handleMediaClick(context, media) { errorMedia ->
                                errorDialogData = errorMedia
                            }
                        },
                        onMediaLongPress = { media ->
                            // For selection (if needed)
                            toggleMediaSelection(media)
                        }
                    )
                }
            }
        }

        // Add floating action button or other UI elements if needed
    }
}

/** Shows a header with the collection’s upload date and media count */
@Composable
fun CollectionHeaderView(section: CollectionSection) {
    // For example, showing date and item count side by side:
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dateText = section.collection.uploadDate?.toGMTString() ?: "Unknown Date"
        Text(text = dateText, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${section.media.size} items",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

/** Renders one collection section: header and grid of media items. */
@Composable
fun CollectionSectionView(
    section: CollectionSection,
    onMediaClick: (Media) -> Unit,
    onMediaLongPress: (Media) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        CollectionHeaderView(section)
        // Render the media items as a grid of 4 columns.
        // We use a simple approach: chunk the media list into rows of 4.
        val rows = section.media.chunked(4)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowItems.forEach { media ->
                    MediaItemView(
                        media = media,
                        isSelected = media.selected,
                        onClick = { onMediaClick(media) },
                        onLongClick = { onMediaLongPress(media) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
                // Fill out the remaining cells (if any) in this row
                if (rowItems.size < 4) {
                    repeat(4 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/** Renders one media item as an image filling its box. */
@Composable
fun MediaItemView(
    media: Media,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = if (isSelected) 4.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
    ) {
        AsyncImage(
            model = media.fileUri,
            contentDescription = media.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        when (media.sStatus) {
            Media.Status.Uploading -> UploadProgress(media.uploadPercentage ?: 0)
            Media.Status.Error -> ErrorIndicator()
            else -> Unit
        }
    }
}


@Composable
fun UploadProgress(progress: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = progress / 100f,
            modifier = Modifier.size(48.dp),
            color = Color.White
        )
        Text(
            text = "$progress%",
            color = Color.White,
            modifier = Modifier.padding(top = 56.dp)
        )
    }
}

@Composable
fun ErrorIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun WelcomeMessage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = "Tap the button below to add media",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/** Refreshes the list of collections (with nonempty media) for the given project.
 * This runs on IO and updates the [sections] state on the main thread.
 */
private fun refreshSections(projectId: Long, sections: MutableList<CollectionSection>) {
    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
        val collections = Collection.getByProject(projectId)
        val newSections = collections.filter { it.media.isNotEmpty() }
            .map { CollectionSection(it) }
        withContext(Dispatchers.Main) {
            sections.clear()
            sections.addAll(newSections)
        }
    }
}


/** Updates one media item in one section (called when a broadcast “change” is received). */
private fun updateMediaItem(
    sections: List<CollectionSection>,
    collectionId: Long,
    mediaId: Long,
    progress: Int,
    isUploaded: Boolean
) {
    sections.find { it.collection.id == collectionId }?.let { section ->
        val idx = section.media.indexOfFirst { it.id == mediaId }
        if (idx != -1) {
            val media = section.media[idx]
            if (isUploaded) {
                media.status = Media.Status.Uploaded.id
            } else {
                media.uploadPercentage = progress
                media.status = Media.Status.Uploading.id
            }
            // Replace to trigger recomposition
            section.media[idx] = media
        }
    }
}

/** Toggles the selected state of the media item and saves it. */
private fun toggleMediaSelection(media: Media) {
    media.selected = !media.selected
    media.save()
}

/** Deletes any media items that are selected from all sections.
 * Also deletes the media from the database and posts a delete broadcast.
 */
private fun deleteSelected(sections: MutableList<CollectionSection>, context: Context) {
    sections.forEach { section ->
        // Work on a copy so we can remove items safely
        section.media.filter { it.selected }.toList().forEach { media ->
            section.media.remove(media)
            media.delete() // delete from database
            BroadcastManager.postDelete(context, media.id)
        }
    }
    // Remove sections that are now empty (do not delete the collection from DB here)
    sections.removeAll { it.media.isEmpty() }
}

/** Deletes a single media item (used when “remove” is chosen from the error dialog). */
private fun deleteMediaItem(sections: MutableList<CollectionSection>, media: Media) {
    sections.find { it.collection.id == media.collectionId }?.let { section ->
        section.media.remove(media)
        media.delete()
        // In a real app, you might also post a broadcast here
    }
}

/** Handles what happens when a media item is clicked (when not in selection mode).
 * Depending on its status and mime type, this either launches a preview, an upload manager,
 * or shows an error dialog.
 *
 * The onError lambda is called if the media is in an error state.
 */
private fun handleMediaClick(context: Context, media: Media, onError: (Media) -> Unit) {
    when (media.sStatus) {
        Media.Status.Local -> {
            // For images, start a preview
            if (media.mimeType.startsWith("image")) {
                PreviewActivity.start(context, media.projectId)
            }
        }

        Media.Status.Queued, Media.Status.Uploading -> {
            // Start the upload manager activity
            context.startActivity(Intent(context, UploadManagerActivity::class.java))
        }

        Media.Status.Error -> {
            // Show error dialog (retry/remove)
            onError(media)
        }

        else -> { /* no op */
        }
    }
}