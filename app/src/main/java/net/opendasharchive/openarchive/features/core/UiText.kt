package net.opendasharchive.openarchive.features.core

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiText {

    data class DynamicString(val value: String) : UiText()
    data class StringResource(@StringRes val resId: Int) : UiText()

    fun asString(context: android.content.Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId)
        }
    }

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId)
        }
    }
}

fun @receiver:StringRes Int.asUiText(): UiText {
    return UiText.StringResource(this)
}

fun String.asUiText(): UiText {
    return UiText.DynamicString(this)
}