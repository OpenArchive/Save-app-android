package net.opendasharchive.openarchive.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.google.android.material.snackbar.Snackbar

private object ViewHelper {
    const val ANIMATION_DURATION: Long = 250 // ms

    fun hide(view: View, visibility: Int, animate: Boolean) {
        if (animate && view.isVisible) {
            view.animate()
                .alpha(0f)
                .setDuration(ANIMATION_DURATION)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = visibility
                        view.alpha = 1f

                        view.animate().setListener(null)
                    }
                })
        }
        else {
            view.visibility = visibility
        }
    }
}


fun View.show(animate: Boolean = false) {
    if (isVisible) return

    if (animate) {
        alpha = 0f
        visibility = View.VISIBLE

        animate().alpha(1f).duration = ViewHelper.ANIMATION_DURATION
    }
    else {
        visibility = View.VISIBLE
    }
}

fun View.hide(animate: Boolean = false) {
    ViewHelper.hide(this, View.GONE, animate)
}

fun View.cloak(animate: Boolean = false) {
    ViewHelper.hide(this, View.INVISIBLE, animate)
}

fun View.toggle(state: Boolean? = null, animate: Boolean = false) {
    if (state ?: !isVisible) {
        show(animate)
    }
    else {
        hide(animate)
    }
}

fun View.disableAnimation(around: () -> Unit) {
    val p = parent as? ViewGroup

    val original = p?.layoutTransition
    p?.layoutTransition = null

    around()

    p?.layoutTransition = original
}

val View.isVisible: Boolean
    get() = visibility == View.VISIBLE

fun View.makeSnackBar(message: CharSequence, duration: Int = Snackbar.LENGTH_INDEFINITE): Snackbar {
    return Snackbar.make(this, message, duration)
}
fun View.getMeasurments(): Pair<Int, Int> {
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    val width = measuredWidth
    val height = measuredHeight
    return width to height
}

fun View.propagateClickToParent() {
    var parent = this.parent as? View
    while (parent != null && !parent.isClickable) {
        parent = parent.parent as? View
    }
    parent?.performClick()
}

fun View.showKeyboard() {
    if (requestFocus()) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}