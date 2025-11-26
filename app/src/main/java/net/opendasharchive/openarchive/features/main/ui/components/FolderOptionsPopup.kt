package net.opendasharchive.openarchive.features.main.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.R

@Composable
fun FolderOptionsPopup(
    expanded: Boolean = false,
    onDismissRequest: () -> Unit,
    onRenameFolder: () -> Unit,
    onSelectMedia: () -> Unit,
    onRemoveFolder: () -> Unit
) {

    DropdownMenu(
        modifier = Modifier,
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {

        Column(modifier = Modifier.padding(8.dp)) {

            DropdownMenuItem(
                onClick = onRenameFolder,
                text = { Text(stringResource(R.string.lbl_rename_folder)) }
            )
            DropdownMenuItem(
                onClick = onSelectMedia,
                text = { Text(stringResource(R.string.lbl_select_media)) }
            )
            DropdownMenuItem(
                onClick = onRemoveFolder,
                text = { Text(stringResource(R.string.lbl_remove_folder)) }
            )
        }
    }

}

@Preview
@Composable
private fun FolderOptionsPopupPreview() {

    FolderOptionsPopup(
        expanded = true,
        onDismissRequest = {},
        onRenameFolder = {},
        onSelectMedia = {},
        onRemoveFolder = {}
    )

}