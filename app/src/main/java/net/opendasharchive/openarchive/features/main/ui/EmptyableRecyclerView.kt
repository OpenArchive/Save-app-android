package net.opendasharchive.openarchive.features.main.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class EmptyableRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var emptyView: View? = null
    private var emptyViewRes: Int = 0

    fun setEmptyView(view: View) {
        emptyView = view
        emptyViewRes = 0
        setupEmptyView()
    }

    fun setEmptyView(layoutResId: Int) {
        emptyViewRes = layoutResId
        emptyView = null
        setupEmptyView()
    }

    private fun setupEmptyView() {
        post {
            val targetParent = findSuitableParent()
            targetParent?.let { parentView ->
                emptyView?.let { parentView.removeView(it) }
                emptyView = when {
                    emptyViewRes != 0 -> {
                        val inflatedView = inflate(context, emptyViewRes, null)
                        parentView.addView(inflatedView, ViewGroup.LayoutParams(width, height))
                        inflatedView
                    }
                    emptyView != null -> {
                        parentView.addView(emptyView, ViewGroup.LayoutParams(width, height))
                        emptyView
                    }
                    else -> null
                }
                emptyView?.visibility = GONE
                checkIfEmpty()
            }
        }
    }

    private fun findSuitableParent(): ViewGroup? {
        var view: View? = this
        var parent: ViewGroup?
        while (view != null) {
            parent = view.parent as? ViewGroup
            if (parent is SwipeRefreshLayout) {
                // If the parent is SwipeRefreshLayout, we want to add the empty view to its parent
                return parent.parent as? ViewGroup
            } else if (parent != null && parent !is RecyclerView) {
                // Found a suitable parent that is not a RecyclerView
                return parent
            }
            view = parent
        }
        return null
    }

    private val observer = object : AdapterDataObserver() {
        override fun onChanged() {
            checkIfEmpty()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
        }
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(observer)

        super.setAdapter(adapter)

        adapter?.registerAdapterDataObserver(observer)
        checkIfEmpty()
    }

    private fun checkIfEmpty() {
        val adapter = adapter
        val emptyView = emptyView

        if (adapter != null && emptyView != null) {
            val emptyViewVisible = adapter.itemCount == 0
            emptyView.visibility = if (emptyViewVisible) VISIBLE else GONE
            visibility = if (emptyViewVisible) GONE else VISIBLE
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupEmptyView()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        emptyView?.let {
            it.layoutParams?.width = w
            it.layoutParams?.height = h
            it.requestLayout()
        }
    }
}