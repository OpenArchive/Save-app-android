package net.opendasharchive.openarchive.util.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Insets
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginRight
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import com.google.android.material.snackbar.Snackbar
import net.opendasharchive.openarchive.R

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
        } else {
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
    } else {
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
    } else {
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

fun View.applyEdgeToEdgeInsets(
    typeMask: Int = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime(),
    propagateInsets: Boolean = false,
    block: ViewGroup.MarginLayoutParams.(InsetsAccumulator) -> Unit
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(typeMask)

        val initialTop = if (view.getTag(R.id.initial_margin_top) != null) {
            view.getTag(R.id.initial_margin_top) as Int
        } else {
            view.setTag(R.id.initial_margin_top, view.marginTop)
            view.marginTop
        }

        val initialBottom = if (view.getTag(R.id.initial_margin_bottom) != null) {
            view.getTag(R.id.initial_margin_bottom) as Int
        } else {
            view.setTag(R.id.initial_margin_bottom, view.marginBottom)
            view.marginBottom
        }

        val initialStart = if (view.getTag(R.id.initial_margin_start) != null) {
            view.getTag(R.id.initial_margin_start) as Int
        } else {
            view.setTag(R.id.initial_margin_start, view.marginStart)
            view.marginStart
        }

        val initialEnd = if (view.getTag(R.id.initial_margin_end) != null) {
            view.getTag(R.id.initial_margin_end) as Int
        } else {
            view.setTag(R.id.initial_margin_end, view.marginEnd )
            view.marginEnd
        }

        val accumulator = InsetsAccumulator(
            initialTop = initialTop,
            insetTop = insets.top,
            initialBottom = initialBottom,
            insetBottom = insets.bottom,
            initialStart = initialStart,
            insetStart = insets.left,
            initialEnd = initialEnd,
            insetEnd = insets.right
        )

        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            apply { block(accumulator) }
        }

        if (propagateInsets) windowInsets else WindowInsetsCompat.CONSUMED
    }
}

fun View.applySideInsets(
    typeMask: Int = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime(),
    propagateInsets: Boolean = false,
) = applyEdgeToEdgeInsets { insets ->

    leftMargin = insets.left
    rightMargin = insets.right
}

data class InsetsAccumulator(
    private val initialTop: Int,
    private val insetTop: Int,
    private val initialBottom: Int,
    private val insetBottom: Int,
    private val initialStart: Int = 0,
    private val insetStart: Int = 0,
    private val initialEnd: Int = 0,
    private val insetEnd: Int = 0,
) {
    val top: Int
        get() = initialTop + insetTop

    val bottom: Int
        get() = initialBottom + insetBottom

    val left: Int
        get() = initialStart + insetStart

    val right: Int
        get() = initialEnd + insetEnd
}