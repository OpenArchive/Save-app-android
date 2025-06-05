package net.opendasharchive.openarchive.util

import android.app.Activity
import android.view.ViewGroup
import androidx.fragment.app.Fragment

object FullScreenOverlayManager {
    private var overlay: FullScreenDimmingOverlay? = null

    fun show(activity: Activity) {
        if (overlay == null) {
            overlay = FullScreenDimmingOverlay(activity)
            (activity.window.decorView as ViewGroup).addView(overlay)
        }
        overlay?.show()
    }

    fun hide() {
        overlay?.hide()
    }

    fun show(fragment: Fragment) {
        fragment.activity?.let { show(it) }
    }
}

object FullScreenOverlayCreateGroupManager {
    private var overlay: FullScreenCreateGroupDimmingOverlay? = null

    fun show(activity: Activity) {
        if (overlay == null) {
            overlay = FullScreenCreateGroupDimmingOverlay(activity)
            (activity.window.decorView as ViewGroup).addView(overlay)
        }
        overlay?.show()
    }

    fun hide() {
        overlay?.hide()
    }

    fun show(fragment: Fragment) {
        fragment.activity?.let { show(it) }
    }
}