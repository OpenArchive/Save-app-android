package net.opendasharchive.openarchive.features.main.ui

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import java.io.File

// MediaFile Data Class
data class MediaFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val type: FileType
)

// Enum to represent different file types
enum class FileType {
    IMAGE, VIDEO, PDF, FOLDER, UNKNOWN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCacheScreen(context: Context, onNavigateBack: () -> Unit) {
    val cacheDir = context.cacheDir
    val files = remember { cacheDir.listFiles()?.map { it.toMediaFile() } ?: emptyList() }

    Scaffold(
topBar ={
    TopAppBar(
        title = { Text("Media Cache") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        }
    )
}

    ) { paddingValues ->

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    CacheFileItem(file)
                }
            }
        }
    }

}

@Composable
fun CacheFileItem(file: MediaFile) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.LightGray)
            .padding(8.dp)
    ) {
        when {
            file.isDirectory -> {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = file.name,
                    modifier = Modifier.size(48.dp)
                )
            }

            file.type == FileType.IMAGE -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(file.path))
                        .scale(Scale.FILL)
                        .crossfade(true)
                        .build(),
                    contentDescription = file.name,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
            }

            file.type == FileType.VIDEO -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(file.path))
                        .scale(Scale.FIT)
                        .crossfade(true)
                        .build(),
                    contentDescription = file.name,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
            }

            file.type == FileType.PDF -> {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = file.name,
                    modifier = Modifier.size(48.dp)
                )
            }

            else -> {
                Icon(
                    imageVector = Icons.Default.QuestionMark,
                    contentDescription = file.name,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = file.name,
            maxLines = 1,
            modifier = Modifier.widthIn(max = 80.dp)
        )
    }
}


fun File.toMediaFile(): MediaFile {
    val fileType = when {
        isDirectory -> FileType.FOLDER
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true) -> FileType.IMAGE
        name.endsWith(".mp4", true) || name.endsWith(".mkv", true) || name.endsWith(".avi", true) -> FileType.VIDEO
        name.endsWith(".pdf", true) -> FileType.PDF
        else -> FileType.UNKNOWN
    }
    return MediaFile(name = name, path = absolutePath, isDirectory = isDirectory, type = fileType)
}