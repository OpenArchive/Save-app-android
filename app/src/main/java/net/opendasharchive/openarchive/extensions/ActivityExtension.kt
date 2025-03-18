package net.opendasharchive.openarchive.extensions

import android.app.Activity
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity

fun Activity.onBackButtonPressed(callback: () -> Boolean) {
    (this as? FragmentActivity)?.onBackPressedDispatcher?.addCallback(
        this,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If callback() returns false, we let the system handle back
                // If callback() returns true, we override it (and do nothing else)
                if (!callback()) {
                    remove()
                    this@onBackButtonPressed
                        .onBackPressedDispatcher
                        .onBackPressed()
                }
            }
        }
    )
}