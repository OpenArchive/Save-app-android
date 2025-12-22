package net.opendasharchive.openarchive.features.core.dialog

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.core.UiColor
import net.opendasharchive.openarchive.features.core.UiImage
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.asUiColor

// --------------------------------------------------------------------
// 1. Dialog Types
// --------------------------------------------------------------------
enum class DialogType {
    Success, Error, Warning, Info, Custom
}

// --------------------------------------------------------------------
// 2. The unified dialog configuration model.
// --------------------------------------------------------------------
data class DialogConfig(
    val type: DialogType,
    val title: UiText,
    val message: UiText,
    val icon: UiImage? = null,
    val iconColor: UiColor? = null,
    val positiveButton: ButtonData? = null,
    val neutralButton: ButtonData? = null,
    val destructiveButton: ButtonData? = null,
    val showCheckbox: Boolean = false,
    val checkboxText: UiText? = null,
    val onCheckboxChanged: (Boolean) -> Unit = {},
    val backgroundColor: UiColor? = null,
    val cornerRadius: Dp? = null,
    val onDismissAction: (() -> Unit)? = null,
)

// --------------------------------------------------------------------
// 3. Button configuration
// --------------------------------------------------------------------
data class ButtonData(
    val text: UiText,
    val action: () -> Unit = {},
)

// --------------------------------------------------------------------
// 4. DSL marker and ButtonBuilder DSL
// --------------------------------------------------------------------
@DslMarker
annotation class DialogDsl

@DialogDsl
class ButtonBuilder {
    var text: UiText? = null
    var action: () -> Unit = {}

    fun build(defaultText: UiText): ButtonData =
        ButtonData(text = text ?: defaultText, action = action)
}

// --------------------------------------------------------------------
// 5. DSL Builder for DialogConfig
// --------------------------------------------------------------------
@DialogDsl
class DialogBuilder {
    // Basic settings
    var type: DialogType = DialogType.Info
    var icon: UiImage? = null
    var title: UiText? = null
    var message: UiText? = null
    var iconColor: UiColor? = null
    var backgroundColor: UiColor? = null
    var cornerRadius: Dp? = null

    // Buttons (initially null)
    private var _positiveButton: ButtonData? = null
    private var _neutralButton: ButtonData? = null
    private var _destructiveButton: ButtonData? = null

    // Checkbox options
    var showCheckbox: Boolean = false
    var checkboxText: UiText? = null
    var onCheckboxChanged: (Boolean) -> Unit = {}

    private var _onDismissAction: (() -> Unit)? = null

    // Button DSL functions – simple and concise
    fun positiveButton(block: ButtonBuilder.() -> Unit) {
        _positiveButton = ButtonBuilder().apply(block)
            .build(defaultText = defaultPositiveTextFor(type))
    }

    fun neutralButton(block: ButtonBuilder.() -> Unit) {
        _neutralButton = ButtonBuilder().apply(block)
            .build(defaultText = defaultNeutralText())
    }

    fun destructiveButton(block: ButtonBuilder.() -> Unit) {
        _destructiveButton = ButtonBuilder().apply(block)
            .build(defaultText = defaultDestructiveText())
    }

    fun onDismissAction(block: () -> Unit) {
        _onDismissAction = block
    }

    // Default texts based on type.
    private fun defaultPositiveTextFor(type: DialogType): UiText = when (type) {
        DialogType.Success -> UiText.Resource(R.string.lbl_ok)
        DialogType.Error -> UiText.Resource(R.string.lbl_retry)
        DialogType.Warning -> UiText.Resource(R.string.lbl_ok)
        DialogType.Info -> UiText.Resource(R.string.lbl_got_it)
        DialogType.Custom -> UiText.Resource(R.string.lbl_ok)
    }
    private fun defaultNeutralText(): UiText = UiText.Resource(R.string.lbl_Cancel)
    private fun defaultDestructiveText(): UiText = UiText.Resource(R.string.lbl_Cancel)

    // -------------------------------
    // 5a. Compose build() – use MaterialTheme defaults.
    // -------------------------------
    @Composable
    fun build(): DialogConfig {

        if (icon == null) {
            icon = when (type) {
                DialogType.Success -> UiImage.DrawableResource(R.drawable.ic_done)
                DialogType.Error -> UiImage.DynamicVector(Icons.Outlined.Error)
                DialogType.Warning -> UiImage.DynamicVector(Icons.Default.Warning)
                DialogType.Info -> UiImage.DynamicVector(Icons.Filled.Info)
                DialogType.Custom -> null
            }
        }

        val finalIconColor = iconColor ?: when (type) {
            DialogType.Error -> MaterialTheme.colorScheme.error.asUiColor()
            DialogType.Warning -> MaterialTheme.colorScheme.tertiary.asUiColor()
            else -> MaterialTheme.colorScheme.onBackground.asUiColor()
        }
        val finalBackgroundColor = backgroundColor ?: MaterialTheme.colorScheme.surfaceVariant.asUiColor()
        val finalCornerRadius = cornerRadius ?: 12.dp
        val finalTitle = title ?: when (type) {
            DialogType.Success -> UiText.Resource(R.string.label_success_title)
            DialogType.Error -> UiText.Resource(R.string.error)
            DialogType.Warning -> UiText.Resource(R.string.label_warning_title)
            DialogType.Info -> UiText.Resource(R.string.label_info_title)
            DialogType.Custom -> UiText.Dynamic("")
        }

        return DialogConfig(
            type = type,
            title = finalTitle,
            message = message ?: UiText.Dynamic(""),
            icon = icon,
            iconColor = finalIconColor,
            positiveButton = _positiveButton, //?: ButtonData(defaultPositiveTextFor(type)),
            neutralButton = _neutralButton,
            destructiveButton = _destructiveButton,
            showCheckbox = showCheckbox,
            checkboxText = checkboxText,
            onCheckboxChanged = onCheckboxChanged,
            backgroundColor = finalBackgroundColor,
            cornerRadius = finalCornerRadius,
            onDismissAction = _onDismissAction
        )
    }

    // -------------------------------
    // 5b. View build() – use ContextCompat to get resource colors.
    // -------------------------------
    fun build(resourceProvider: ResourceProvider): DialogConfig {

        if (icon == null) {

            icon = when (type) {
                DialogType.Success -> UiImage.DrawableResource(R.drawable.ic_done)
                DialogType.Error -> UiImage.DynamicVector(Icons.Outlined.Error)
                DialogType.Warning -> UiImage.DynamicVector(Icons.Default.Warning)
                DialogType.Info -> UiImage.DynamicVector(Icons.Filled.Info)
                DialogType.Custom -> null
            }
        }

        // Convert resource colors (ints) to Compose Colors.
        val finalIconColor = iconColor ?: when (type) {
            DialogType.Error -> resourceProvider.getColor(R.color.colorError).asUiColor()
            else -> resourceProvider.getColor(R.color.colorTertiary).asUiColor()
        }
        val finalBackgroundColor = backgroundColor ?: resourceProvider.getColor(R.color.colorSurface).asUiColor()
        val finalCornerRadius = cornerRadius ?: 12.dp
        val finalTitle = title ?: when (type) {
            DialogType.Success -> UiText.Resource(R.string.label_success_title)
            DialogType.Error -> UiText.Resource(R.string.error)
            DialogType.Warning -> UiText.Resource(R.string.label_warning_title)
            DialogType.Info -> UiText.Resource(R.string.label_info_title)
            DialogType.Custom -> UiText.Dynamic("")
        }

        return DialogConfig(
            type = type,
            title = finalTitle,
            message = message ?: UiText.Dynamic(""),
            icon = icon,
            iconColor = finalIconColor,
            positiveButton = _positiveButton, //?: ButtonData(defaultPositiveTextFor(type)),
            neutralButton = _neutralButton,
            destructiveButton = _destructiveButton,
            onDismissAction = _onDismissAction,
            showCheckbox = showCheckbox,
            checkboxText = checkboxText,
            onCheckboxChanged = onCheckboxChanged,
            backgroundColor = finalBackgroundColor,
            cornerRadius = finalCornerRadius
        )
    }
}

// --------------------------------------------------------------------
// 6. Extension functions on DialogStateManager for showing dialogs
// --------------------------------------------------------------------

// --- Compose extension: allows calling showDialog { ... } in a @Composable block.
@Composable
fun DialogStateManager.showDialogCompose(block: DialogBuilder.() -> Unit) {
    val config = DialogBuilder().apply(block).build()
    showDialog(config)
}

// --- View extension: pass a Context so that resource colors are used.
fun DialogStateManager.showDialog(resourceProvider: ResourceProvider = this.requireResourceProvider(), block: DialogBuilder.() -> Unit) {
    val config = DialogBuilder().apply(block).build(resourceProvider)
    showDialog(config)
}


// --------------------------------------------------------------------
// 7. Helper functions for common dialog types
// --------------------------------------------------------------------

// Compose helper for a success dialog.
@Composable
fun DialogStateManager.showSuccessDialog(
    message: String,
    title: String = "",  // if empty, default title is used
    onPositive: () -> Unit = {}
) {
    showDialog {
        type = DialogType.Success
        this.message = UiText.Dynamic(message)
        if (title.isNotEmpty()) this.title = UiText.Dynamic(title)
        positiveButton {
            text = UiText.Resource(R.string.lbl_ok)
            action = onPositive
        }
    }
}

// View helper for an info/hint dialog.
fun DialogStateManager.showSuccessDialog(
    @StringRes title: Int?,
    @StringRes message: Int,
    @StringRes positiveButtonText: Int? = null,
    icon: UiImage? = null,
    onDone: () -> Unit = {},
    onDismissed: () -> Unit = {}
) {
    val resourceProvider = this.requireResourceProvider()

    showDialog(resourceProvider) {
        type = DialogType.Success
        if (icon != null) this.icon = icon
        this.iconColor = resourceProvider.getColor(R.color.colorTertiary).asUiColor()
        if (title != null) this.title = UiText.Resource(title)
        this.message = UiText.Resource(message)
        positiveButton {
            text = UiText.Resource(positiveButtonText ?: R.string.lbl_got_it)
            action = onDone
        }
        onDismissAction {
            onDismissed()
        }
    }
}

// View helper for an error dialog.
fun DialogStateManager.showErrorDialog(
    message: String,
    title: String = "",
    onDismiss: () -> Unit = {}
) {
    val resourceProvider = this.requireResourceProvider()

    showDialog(resourceProvider) {
        type = DialogType.Error
        this.message = UiText.Dynamic(message)
        if (title.isNotEmpty()) this.title = UiText.Dynamic(title)

        positiveButton {
            text = UiText.Resource(R.string.lbl_ok)
            action = onDismiss
        }
    }
}

// View helper for an info/hint dialog.
fun DialogStateManager.showInfoDialog(
    message: UiText,
    title: UiText?,
    icon: UiImage? = null,
    onDone: () -> Unit = {},
) {
    val resourceProvider = this.requireResourceProvider()

    showDialog(resourceProvider) {
        type = DialogType.Info
        this.icon = icon
        this.iconColor = resourceProvider.getColor(R.color.colorTertiary).asUiColor()
        this.title = title
        this.message = message
        positiveButton {
            text = UiText.Resource(R.string.lbl_ok)
            action = onDone
        }
    }
}

// View helper for an info/hint dialog.
fun DialogStateManager.showWarningDialog(
    title: UiText?,
    message: UiText,
    icon: UiImage? = null,
    positiveButtonText: UiText? = null,
    onDone: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val resourceProvider = this.requireResourceProvider()

    showDialog(resourceProvider) {
        type = DialogType.Warning
        this.title = title
        this.icon = icon
        iconColor = resourceProvider.getColor(R.color.colorTertiary).asUiColor()
        this.message = message
        positiveButton {
            text = positiveButtonText ?: UiText.Resource(R.string.lbl_got_it)
            action = onDone
        }
        destructiveButton {
            text = UiText.Resource(R.string.lbl_Cancel)
            action = onCancel
        }
    }
}

// For Destructive Actions confirmation (Removing folder or server etc)
fun DialogStateManager.showDestructiveDialog(
    title: UiText?,
    message: UiText,
    icon: UiImage? = null,
    positiveButtonText: UiText? = null,
    onDone: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val resourceProvider = this.requireResourceProvider()

    showDialog(resourceProvider) {
        type = DialogType.Warning
        this.title = title
        this.icon = icon
        this.message = message
        positiveButton {
            text = positiveButtonText ?: UiText.Resource(R.string.lbl_got_it)
            action = onDone
        }
        destructiveButton {
            text = UiText.Resource(R.string.lbl_Cancel)
            action = onCancel
        }
    }
}


/**
 * ResourceProvider is an abstraction that lets you look up colors and vector icons
 * without passing a Context every time.
 */
interface ResourceProvider {
    fun getColor(@ColorRes colorRes: Int): Color
    fun getVector(@DrawableRes drawableRes: Int): ImageVector?
}

/**
 * A simple implementation that uses an Android Context.
 * You can instantiate this once (for example in your BaseActivity) and pass it
 * to your DialogStateManager.
 */
class DefaultResourceProvider(private val context: Context) : ResourceProvider {
    override fun getColor(@ColorRes colorRes: Int): Color {
        // ContextCompat.getColor returns an int (the ARGB value); we wrap it in Compose’s Color.
        return Color(ContextCompat.getColor(context, colorRes))
    }

    override fun getVector(@DrawableRes drawableRes: Int): ImageVector? {
        // For a real application you might have a more elaborate mapping.
        // In this simple example, if the drawable resource equals R.drawable.ic_info,
        // we return Icons.Filled.Info; otherwise, return null.
        return when (drawableRes) {
            R.drawable.ic_info -> Icons.Filled.Info
            else -> null
        }
    }
}