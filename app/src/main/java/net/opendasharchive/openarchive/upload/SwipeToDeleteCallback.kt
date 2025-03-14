package net.opendasharchive.openarchive.upload

import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import kotlin.math.max
import kotlin.math.roundToInt

abstract class SwipeToDeleteCallback(context: Context?): ItemTouchHelper.Callback() {

    private val mBackground = if (context != null)
        ColorDrawable(ContextCompat.getColor(context, R.color.colorDanger)) else null

    private val mIcon = if (context != null)
        ContextCompat.getDrawable(context, R.drawable.ic_delete) else null

    private val mIconColor = if (context != null)
        ContextCompat.getColor(context, R.color.colorOnBackground) else 0

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {

            return  makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val iv = viewHolder.itemView

        val cancelled = dX == 0f && !isCurrentlyActive
        if (cancelled) {
            return super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false)
        }

        mBackground?.setBounds(iv.right + dX.toInt(), iv.top, iv.right, iv.bottom)
        mBackground?.draw(c)

        val height = ((mIcon?.intrinsicHeight ?: 0) * 0.75).roundToInt()
        val width = ((mIcon?.intrinsicWidth ?: 0) * 0.75).roundToInt()
        val margin = (iv.height - height) / 2
        val left = max(iv.right + dX.toInt(), iv.right - margin - width)
        val top = iv.top + (iv.height - height) / 2
        val right = iv.right - margin
        val bottom = top + height

        @Suppress("DEPRECATION")
        mIcon?.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN)

        mIcon?.setBounds(left, top, right, bottom)
        mIcon?.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.7f
    }
}