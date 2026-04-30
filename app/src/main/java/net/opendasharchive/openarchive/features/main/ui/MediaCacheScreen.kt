package net.opendasharchive.openarchive.features.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import net.opendasharchive.openarchive.R
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
fun MediaCacheScreen(onNavigateBack: () -> Unit) {

    val context = LocalContext.current
    val cacheDir = File(context.filesDir, "media_temp")
    val files = remember { cacheDir.listFiles()?.map { it.toMediaFile() } ?: emptyList() }

    Scaffold(
topBar ={
    TopAppBar(
        title = { Text("Media Cache") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(painter = painterResource(R.drawable.ic_arrow_back_ios), contentDescription = null)
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
                    painter = painterResource(R.drawable.ic_folder),
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
                    painter = painterResource(R.drawable.ic_pdf),
                    contentDescription = file.name,
                    modifier = Modifier.size(48.dp)
                )
            }

            else -> {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
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