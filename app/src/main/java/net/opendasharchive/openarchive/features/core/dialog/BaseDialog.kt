package net.opendasharchive.openarchive.features.core.dialog

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import net.opendasharchive.openarchive.core.presentation.theme.DefaultBoxPreview
import net.opendasharchive.openarchive.features.core.BaseButton
import net.opendasharchive.openarchive.features.core.BaseDestructiveButton
import net.opendasharchive.openarchive.features.core.BaseNeutralButton
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.asUiImage

@Composable
fun BaseDialog(
    onDismiss: () -> Unit,
    icon: UiImage? = null,
    iconColor: Color? = null,
    title: String,
    message: String,
    hasCheckbox: Boolean = false,
    onCheckBoxStateChanged: (Boolean) -> Unit = {},
    checkBoxHint: String = "Do not show me this again",
    positiveButton: ButtonData? = null,
    neutralButton: ButtonData? = null,
    destructiveButton: ButtonData? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {

    val (isCheckedState, setCheckedState) = remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { onDismiss.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {


            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    icon?.let { icon ->
                        icon.asIcon(
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = iconColor ?: Color.Unspecified
                        ).invoke()

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    BaseDialogTitle(title)

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BaseDialogMessage(message)
                    }

                    if (hasCheckbox) {
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isCheckedState,
                                onCheckedChange = { isChecked ->
                                    setCheckedState(isChecked)
                                    onCheckBoxStateChanged.invoke(isChecked)
                                }
                            )

                            BaseDialogMessage(checkBoxHint)
                        }

                    }

                    Spacer(Modifier.height(24.dp))

                    positiveButton?.let { btn ->
                        Spacer(Modifier.height(4.dp))
                        BaseButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = btn.text.asString(),
                            onClick = {
                                btn.action()
                                onDismiss()
                            })
                    }

                    destructiveButton?.let { btn ->
                        Spacer(modifier = Modifier.height(4.dp))
                        BaseDestructiveButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                btn.action()
                                onDismiss()
                            },
                            text = btn.text.asString()
                        )
                    }

                    neutralButton?.let { btn ->
                        Spacer(modifier = Modifier.height(4.dp))
                        BaseNeutralButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = btn.text.asString(),
                            onClick = {
                                btn.action()
                                onDismiss()
                            })
                    }
                }

            }



    }
}

@Composable
fun BaseDialogTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = modifier
    )
}

@Composable
fun BaseDialogMessage(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        ),
        modifier = modifier
    )
}


class DialogStateManager(private val resourceProvider: ResourceProvider) : ViewModel() {
    private val _dialogConfig = mutableStateOf<DialogConfig?>(null)
    val dialogConfig: State<DialogConfig?> = _dialogConfig

    fun showDialog(config: DialogConfig) {
        _dialogConfig.value = config
    }

    fun dismissDialog() {
        _dialogConfig.value = null
    }

    /**
     * Helper to get the ResourceProvider. This will throw if one wasn’t provided.
     */
    fun requireResourceProvider(): ResourceProvider =
        resourceProvider
}

@Composable
fun DialogHost(dialogStateManager: DialogStateManager) {
    val currentDialog by dialogStateManager.dialogConfig

    currentDialog?.let { config ->
        BaseDialog(
            onDismiss = {
                dialogStateManager.dismissDialog()
            },
            icon = config.icon,
            iconColor = config.iconColor,
            title = config.title.asString(),
            message = config.message.asString(),
            positiveButton = config.positiveButton,
            neutralButton = config.neutralButton,
            destructiveButton = config.destructiveButton,
            hasCheckbox = config.showCheckbox,
            onCheckBoxStateChanged = { config.onCheckboxChanged(it) },
            checkBoxHint = config.checkboxText?.asString() ?: "",
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun BaseDialogPreview() {
    DefaultBoxPreview {

        BaseDialog(
            onDismiss = {},
            icon = Icons.Filled.Check.asUiImage(),
            iconColor = MaterialTheme.colorScheme.primary,
            title = "Success",
            message = "You have added a folder successfully",
            positiveButton = ButtonData(UiText.DynamicString("OK")),
        )

    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun WarningDialogPreview() {
    DefaultBoxPreview {

        BaseDialog(
            onDismiss = {},
            icon = Icons.Default.Warning.asUiImage(),
            iconColor = MaterialTheme.colorScheme.primary,
            title = "Warning",
            message = "Once uploaded, you will not be able to edit media",
            positiveButton = ButtonData(UiText.DynamicString("OK")),
            neutralButton = ButtonData(UiText.DynamicString("Cancel")),
            hasCheckbox = true,
            checkBoxHint = "Do not show me this again",
            onCheckBoxStateChanged = { },
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ErrorDialogPreview() {
    DefaultBoxPreview {

        BaseDialog(
            onDismiss = {},
            icon = Icons.Default.ErrorOutline.asUiImage(),
            iconColor = MaterialTheme.colorScheme.error,
            title = "Image upload unsuccessful",
            message = "Give a reason here? Lorem Ipsum text can go here if needed",
            positiveButton = ButtonData(UiText.DynamicString("Retry")),
            destructiveButton = ButtonData(UiText.DynamicString("Remove Image")),
        )
    }
}