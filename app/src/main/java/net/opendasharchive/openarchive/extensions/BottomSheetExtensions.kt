package net.opendasharchive.openarchive.extensions

import android.annotation.SuppressLint
import android.content.res.Resources
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.opendasharchive.openarchive.R

fun Fragment.showBottomSheetDialog(
    @LayoutRes layout: Int,
    @IdRes textViewToSet: Int? = null,
    textToSet: String? = null,
    fullScreen: Boolean = true,
    expand: Boolean = true
) {
    val dialog = BottomSheetDialog(context!!)
    dialog.setOnShowListener {
        val bottomSheet: FrameLayout = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet) ?: return@setOnShowListener
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        if (fullScreen && bottomSheet.layoutParams != null) { showFullScreenBottomSheet(bottomSheet) }

        if (!expand) return@setOnShowListener

        bottomSheet.setBackgroundResource(android.R.color.transparent)
        expandBottomSheet(bottomSheetBehavior)
    }

    @SuppressLint("InflateParams") // dialog does not need a root view here
    val sheetView = layoutInflater.inflate(layout, null)
    textViewToSet?.also {
        sheetView.findViewById<TextView>(it).text = textToSet
    }

//    sheetView.findViewById<ImageView>(R.id.closeButton)?.setOnClickListener {
//        dialog.dismiss()
//    }

    dialog.setContentView(sheetView)
    dialog.show()
}

private fun showFullScreenBottomSheet(bottomSheet: FrameLayout) {
    val layoutParams = bottomSheet.layoutParams
    layoutParams.height = Resources.getSystem().displayMetrics.heightPixels
    bottomSheet.layoutParams = layoutParams
}

private fun expandBottomSheet(bottomSheetBehavior: BottomSheetBehavior<FrameLayout>) {
    bottomSheetBehavior.skipCollapsed = true
    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
}