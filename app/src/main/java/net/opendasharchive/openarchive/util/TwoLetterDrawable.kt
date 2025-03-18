package net.opendasharchive.openarchive.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import net.opendasharchive.openarchive.R

class TwoLetterDrawable(
    private val text: String,
    private val textColor: Int,
    private val backgroundColor: Int,
    private val textSize: Float,
    private val cornerRadius: Float = 12f
) : Drawable() {

    init {
        require(text == "RO" || text == "RW") { "Only 'RO' or 'RW' are supported" }
    }

    companion object {
        fun ReadOnly(context: Context) =
            TwoLetterDrawable(
                text = "RO",
                textColor = ContextCompat.getColor(context, android.R.color.white),
                backgroundColor = ContextCompat.getColor(context, R.color.c23_teal),
                textSize = context.resources.getDimension(R.dimen.letter_text_size)
            )

        fun ReadWrite(context: Context) =
            TwoLetterDrawable(
                text = "RW",
                textColor = ContextCompat.getColor(context, android.R.color.white),
                backgroundColor = ContextCompat.getColor(context, R.color.c23_teal),
                textSize = context.resources.getDimension(R.dimen.letter_text_size)
            )
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }

    private val roundRect = RectF()

    override fun draw(canvas: Canvas) {
        // Draw rounded background
        roundRect.set(bounds)
        canvas.drawRoundRect(roundRect, cornerRadius, cornerRadius, backgroundPaint)

        // Calculate the center point of the bounds
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()

        // Adjust text size to fit within the bounds
        val size = calculateOptimalTextSize(bounds.width(), bounds.height())
        paint.textSize = size

        // Draw text
        paint.color = textColor

        // Use the Paint's FontMetrics to calculate vertical centering
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val textOffset = textHeight / 2 - fontMetrics.bottom

        canvas.drawText(text, centerX, centerY + textOffset, paint)
    }

    private fun calculateOptimalTextSize(width: Int, height: Int): Float {
        var lo = 1f
        var hi = width.coerceAtMost(height).toFloat()
        val threshold = 0.5f

        while (hi - lo > threshold) {
            val size = (lo + hi) / 2
            paint.textSize = size
            if (paint.measureText(text) < width * 0.8f &&
                (paint.fontMetrics.bottom - paint.fontMetrics.top) < height * 0.8f) {
                lo = size
            } else {
                hi = size
            }
        }

        return lo
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        backgroundPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        backgroundPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}