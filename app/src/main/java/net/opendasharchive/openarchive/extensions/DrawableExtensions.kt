package net.opendasharchive.openarchive.extensions

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.util.TypedValue
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.max
import kotlin.math.roundToInt

fun Drawable.scaled(biggerSideDipLength: Int, context: Context): Drawable {
    val length = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        biggerSideDipLength.toFloat(), context.resources.displayMetrics)

    return scaled(length.toDouble() / max(intrinsicWidth, intrinsicHeight), context)
}

fun Drawable.scaled(factor: Double, context: Context): Drawable {
    if (factor == 1.0) return this

    return scaled((intrinsicWidth * factor).roundToInt(),
        (intrinsicHeight * factor).roundToInt(),
        context)
}

fun Drawable.scaled(width: Int, height: Int, context: Context): Drawable {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        LayerDrawable(arrayOf(this)).also {
            it.setLayerSize(0, width, height)
        }
    }
    else {
        BitmapDrawable(context.resources, toBitmap(width, height))
    }
}

fun Drawable.tint(color: Int): Drawable {
    colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)

    return this
}

fun Drawable.clone(): Drawable? {
    return constantState?.newDrawable()?.mutate()
}