package net.opendasharchive.openarchive.features.folders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import org.koin.androidx.compose.koinViewModel
import java.util.Date

@Composable
fun BrowseFolderScreen(
    viewModel: BrowseFoldersViewModel = koinViewModel()
) {

    val navController = LocalView.current.findNavController()


    val folders by viewModel.folders.observeAsState()


    BrowseFolderScreenContent(
        folders = folders ?: emptyList()
    )
}


@Composable
fun BrowseFolderScreenContent(
    folders: List<Folder>
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {

        items(folders) { folder ->
            BrowseFolderItem(folder) { }
        }
    }

}

@Composable
fun BrowseFolderItem(
    folder: Folder,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Icon(painter = painterResource(R.drawable.ic_folder_new), contentDescription = null)
            Text(folder.name)
        }
    }
}

@Preview
@Composable
private fun BrowseFolderScreenPreview() {
    DefaultScaffoldPreview {
        BrowseFolderScreenContent(
            folders = listOf(
                Folder(name = "Elelan", modified = Date()),
                Folder(name = "Save", modified = Date()),
                Folder(name = "Downloads", modified = Date()),
                Folder(name = "Trip", modified = Date()),
                Folder(name = "Wedding", modified = Date()),
            )
        )
    }
}