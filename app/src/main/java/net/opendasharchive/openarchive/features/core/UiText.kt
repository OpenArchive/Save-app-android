package net.opendasharchive.openarchive.features.core

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Represents user-visible text without requiring a Context in ViewModels.
 * Marked @Immutable to allow Compose to skip recomposition when state hasn't changed.
 */
@Immutable
sealed interface UiText {

    @Immutable
    data class Dynamic(val value: String) : UiText

    @Immutable // @param: targets constructor to avoid compiler warnings
    data class Resource(@param:StringRes val resId: Int, val args: List<UiTextArg> = emptyList()) : UiText

    @Immutable
    data class PluralResource(@param:PluralsRes val resId: Int, val quantity: Int, val args: List<UiTextArg> = emptyList()) : UiText

    /**
     * Resolves the [UiText] into a String in a non-composable context (e.g., Unit Tests).
     */
    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(resId, *args.toFormatArgs(context))
        is PluralResource -> context.resources.getQuantityString(resId, quantity, *args.toFormatArgs(context))
    }
}

/**
 * Type-safe formatting arguments. Supports nesting another [UiText] inside a resource.
 */
@Immutable
sealed interface UiTextArg {
    @Immutable data class Str(val value: String) : UiTextArg
    @Immutable data class Num(val value: Number) : UiTextArg // Int, Long, Double
    @Immutable data class Nested(val value: UiText) : UiTextArg
}

/**
 * Resolves [UiText] inside a Composable function.
 */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> stringResource(resId, *args.toFormatArgs())
    is UiText.PluralResource -> pluralStringResource(resId, quantity, *args.toFormatArgs())
}

@Composable
private fun List<UiTextArg>.toFormatArgs(): Array<Any> = map { arg ->
    when (arg) {
        is UiTextArg.Str -> arg.value
        is UiTextArg.Num -> arg.value
        is UiTextArg.Nested -> arg.value.asString()
    }
}.toTypedArray()

private fun List<UiTextArg>.toFormatArgs(context: android.content.Context): Array<Any> = map { arg ->
    when (arg) {
        is UiTextArg.Str -> arg.value
        is UiTextArg.Num -> arg.value
        is UiTextArg.Nested -> arg.value.asString(context)
    }
}.toTypedArray()


// Helper Extensions
fun String.asUiText() = UiText.Dynamic(this)
fun @receiver:StringRes Int.asUiText(vararg args: UiTextArg) = UiText.Resource(this, args.toList())