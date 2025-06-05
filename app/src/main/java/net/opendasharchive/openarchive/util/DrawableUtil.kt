package net.opendasharchive.openarchive.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

object DrawableUtil {
    fun createCircularTextDrawable(initial: String, backgroundColor: Int): Bitmap {
        val size = 100 // size of the bitmap
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            color = backgroundColor // No conflict here
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = Color.WHITE // Setting a new color for the text
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD

        val xPos = size / 2f
        val yPos = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2)
        canvas.drawText(initial, xPos, yPos, paint)

        return bitmap
    }
}