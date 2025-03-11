package net.opendasharchive.openarchive.util

import android.content.Context
import android.content.DialogInterface
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import net.opendasharchive.openarchive.R

@Deprecated("Move to common BaseDialog implementation using Jetpack Compose")
class AlertHelper {

    class Button(
        val type: Type = Type.Positive,
        val title: Int = R.string.lbl_ok,
        val listener: ((DialogInterface, Int) -> Unit)? = null
    ) {
        enum class Type {
            Positive, Negative, Neutral
        }
    }

    companion object {
        fun show(
            context: Context, message: Int?, title: Int? = R.string.error,
            icon: Int? = null, buttons: List<Button>? = listOf(Button())
        ) {
            build(context, message, title, icon, buttons).show()
        }

        fun build(
            context: Context, message: Int?, title: Int? = R.string.error,
            icon: Int? = null, buttons: List<Button>? = listOf(Button())
        ): AlertDialog.Builder {
            return build(
                context, if (message != null) context.getString(message) else null, title,
                icon, buttons
            )
        }

        fun show(
            context: Context, message: String? = null, title: Int? = R.string.error,
            icon: Int? = null, buttons: List<Button>? = listOf(Button())
        ) {
            build(context, message, title, icon, buttons).show()
        }

        fun build(
            context: Context, message: String? = null, title: Int? = R.string.error,
            icon: Int? = null, buttons: List<Button>? = listOf(Button())
        ): AlertDialog.Builder {
            val builder =
                AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogTheme))

            if (message != null) builder.setMessage(message)
            if (title != null) builder.setTitle(title)
            if (icon != null) builder.setIcon(icon)

            var cancellable = false

            for (button in buttons ?: emptyList()) {
                if (button.listener == null) cancellable = true

                when (button.type) {
                    Button.Type.Positive -> builder.setPositiveButton(button.title, button.listener)
                    Button.Type.Negative -> builder.setNegativeButton(button.title, button.listener)
                    Button.Type.Neutral -> builder.setNeutralButton(button.title, button.listener)
                }
            }

            builder.setCancelable(cancellable)

            return builder
        }

        fun positiveButton(
            title: Int = R.string.lbl_ok,
            listener: ((DialogInterface, Int) -> Unit)? = null
        ): Button {
            return Button(Button.Type.Positive, title, listener)
        }

        fun neutralButton(
            title: Int = R.string.lbl_Cancel,
            listener: ((DialogInterface, Int) -> Unit)? = null
        ): Button {
            return Button(Button.Type.Neutral, title, listener)
        }

        fun negativeButton(
            title: Int = R.string.lbl_Cancel,
            listener: ((DialogInterface, Int) -> Unit)? = null
        ): Button {
            return Button(Button.Type.Negative, title, listener)
        }
    }
}