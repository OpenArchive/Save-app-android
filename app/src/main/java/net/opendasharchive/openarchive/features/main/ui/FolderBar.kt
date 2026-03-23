package net.opendasharchive.openarchive.features.main.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.domain.VaultType
import net.opendasharchive.openarchive.core.presentation.theme.MontserratFontFamily
import net.opendasharchive.openarchive.features.main.ui.components.SpaceIcon
import java.text.NumberFormat

// Folder Bar Composable
@Composable
internal fun FolderBar(
    state: FolderBarState,
    menu: ImmutableList<FolderMenuItem> = defaultFolderMenu(state.projectName != null),
    onIntent: (FolderBarIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state.mode) {
            FolderBarMode.INFO -> FolderBarInfoMode(
                state = state,
                menu = menu,
                onIntent = onIntent,
            )

            FolderBarMode.SELECTION -> FolderBarSelectionMode(
                selectedCount = state.selectedCount,
                onCancel = { onIntent(FolderBarIntent.CancelSelection) },
                onDelete = { onIntent(FolderBarIntent.DeleteSelectedMediaRequest) },
            )

            FolderBarMode.EDIT -> FolderBarEditMode(
                initialName = state.projectName ?: "",
                onCancel = { onIntent(FolderBarIntent.CancelEdit) },
                onSave = { onIntent(FolderBarIntent.SaveName(it)) }
            )
        }
    }
}

@Immutable
data class FolderBarState(
    val mode: FolderBarMode,
    val spaceType: VaultType? = null,
    val projectName: String? = null,
    val totalMediaCount: Int = 0,
    val selectedCount: Int = 0,
    val showOptionsPopup: Boolean = false,
    val canShowOptions: Boolean = true, // optional
)

sealed interface FolderBarIntent {
    data object OptionsOpened : FolderBarIntent
    data object OptionsDismissed : FolderBarIntent

    // menu actions
    data object SelectMedia : FolderBarIntent
    data object RenameFolder : FolderBarIntent
    data object ToggleArchive : FolderBarIntent
    data object RemoveFolder : FolderBarIntent

    // selection mode actions
    data object CancelSelection : FolderBarIntent
    data object DeleteSelectedMediaRequest : FolderBarIntent

    // edit mode actions
    data object CancelEdit : FolderBarIntent
    data class SaveName(val name: String) : FolderBarIntent
}

@Immutable
sealed class FolderMenuItem(
    @param:StringRes val titleRes: Int,
    val intent: FolderBarIntent,
) {
    data object SelectMedia : FolderMenuItem(
        titleRes = R.string.lbl_select_media,
        intent = FolderBarIntent.SelectMedia
    )

    data object Rename : FolderMenuItem(
        titleRes = R.string.lbl_rename_folder,
        intent = FolderBarIntent.RenameFolder
    )

    data object Archive : FolderMenuItem(
        titleRes = R.string.popup_archive_folder,
        intent = FolderBarIntent.ToggleArchive
    )

    data object Remove : FolderMenuItem(
        titleRes = R.string.popup_remove_folder,
        intent = FolderBarIntent.RemoveFolder
    )
}

fun defaultFolderMenu(hasProject: Boolean): ImmutableList<FolderMenuItem> =
    if (!hasProject) persistentListOf()
    else persistentListOf(
        FolderMenuItem.SelectMedia,
        FolderMenuItem.Rename,
        FolderMenuItem.Archive,
        FolderMenuItem.Remove
    )

@Composable
private fun RowScope.FolderBarInfoMode(
    state: FolderBarState,
    menu: ImmutableList<FolderMenuItem>,
    onIntent: (FolderBarIntent) -> Unit,
) {
    val hasProject = state.projectName != null
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Space Icon
        state.spaceType?.let {

            SpaceIcon(
                type = it,
                modifier = Modifier.size(28.dp)
            )

            // Arrow
            Icon(
                painter = painterResource(R.drawable.keyboard_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = colorResource(R.color.colorOnBackground)
            )

            // Folder Name
            Text(
                text = state.projectName ?: "",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MontserratFontFamily),
                modifier = Modifier.weight(1f, fill = false)
            )
        }


    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (hasProject) {
            Box {
                // Edit Button
                IconButton(
                    onClick = { onIntent(FolderBarIntent.OptionsOpened) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_folder),
                        contentDescription = stringResource(R.string.edit),
                        tint = colorResource(R.color.colorTertiary),
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = state.showOptionsPopup,
                    onDismissRequest = { onIntent(FolderBarIntent.OptionsDismissed) }
                ) {
                    menu.forEach { item ->
                        val enabled = item !is FolderMenuItem.SelectMedia || state.totalMediaCount > 0
                        DropdownMenuItem(
                            text = { Text(stringResource(id = item.titleRes)) },
                            enabled = enabled,
                            onClick = {
                                onIntent(FolderBarIntent.OptionsDismissed)
                                onIntent(item.intent)
                            }
                        )
                    }
                }
            }
            // Count Pill
            Box(
                modifier = Modifier
                    .background(
                        colorResource(R.color.colorPillTransparent),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = NumberFormat.getInstance().format(state.totalMediaCount),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RowScope.FolderBarSelectionMode(
    selectedCount: Int,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close Button
        IconButton(
            onClick = onCancel,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.action_cancel),
                tint = colorResource(R.color.colorOnBackground)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // "Select Media" Text
        Text(
            text = stringResource(R.string.lbl_select_media),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MontserratFontFamily)
        )
    }

    // Remove Button (only show if items selected)
    if (selectedCount > 0) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onDelete() }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_trash),
                contentDescription = null,
                tint = colorResource(R.color.colorError),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.lbl_remove),
                color = colorResource(R.color.colorError),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun FolderBarEditMode(
    initialName: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    var folderName by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(0, initialName.length)
            )
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        // Show keyboard
        keyboard?.show()
    }

    fun closeImeAndClearFocus() {
        focusManager.clearFocus()
        keyboard?.hide()

        // fallback (optional but reliable) using a real token
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close Button
        IconButton(
            onClick = {
                closeImeAndClearFocus()
                onCancel()
            },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.action_cancel),
                tint = colorResource(R.color.colorOnBackground)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Text Input
        OutlinedTextField(
            value = folderName,
            onValueChange = { folderName = it },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = MontserratFontFamily),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    closeImeAndClearFocus()
                    onSave(folderName.text)
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            )
        )
    }
}