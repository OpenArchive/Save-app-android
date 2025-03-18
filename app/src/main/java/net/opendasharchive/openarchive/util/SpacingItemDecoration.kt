package net.opendasharchive.openarchive.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount

        outRect.left = spacing
        outRect.right = spacing
        outRect.top = spacing
        // Set bottom spacing only if it's not the last item
        if (position < itemCount - 1) {
            outRect.bottom = spacing
        } else {
            outRect.bottom = 0 // 0 spacing for the last item
        }
    }
}