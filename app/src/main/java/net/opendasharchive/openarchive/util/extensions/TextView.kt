package net.opendasharchive.openarchive.util.extensions

import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.widget.TextView
import androidx.core.content.ContextCompat

enum class Position {
    Start, Top, End, Bottom;

    fun <T> get(list: List<T>): T? {
        return if (list.size > ordinal) list[ordinal] else null
    }
}

fun TextView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawables: List<Drawable?>) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(
        Position.Start.get(drawables),
        Position.Top.get(drawables),
        Position.End.get(drawables),
        Position.Bottom.get(drawables))
}

fun TextView.scaleAndTintDrawable(position: Position, scale: Double = 1.0, tint: Boolean = true) {
    setDrawable(compoundDrawablesRelative[position.ordinal], position, scale, tint)
}

fun TextView.setDrawable(id: Int, position: Position, scale: Double = 1.0, tint: Boolean = true) {
    setDrawable(ContextCompat.getDrawable(context, id), position, scale, tint)
}

fun TextView.setDrawable(drawable: Drawable?, position: Position, scale: Double = 1.0, tint: Boolean = true) {
    @Suppress("NAME_SHADOWING")
    val drawable = drawable?.scaled(scale, context)

    if (tint) drawable?.tint(currentTextColor)

    val list = compoundDrawablesRelative.toMutableList()
    list[position.ordinal] = drawable

    setCompoundDrawablesRelativeWithIntrinsicBounds(list)
}

fun TextView.styleAsLink() {
    setText(SpannableString(text).apply {
        setSpan(URLSpan(""), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }, TextView.BufferType.SPANNABLE)

    isClickable = true
    isFocusable = true
}