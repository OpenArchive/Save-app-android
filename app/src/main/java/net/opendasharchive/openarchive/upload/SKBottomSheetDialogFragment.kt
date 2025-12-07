package net.opendasharchive.openarchive.upload

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager

open class SKBottomSheetDialogFragment : BottomSheetDialogFragment() {

    protected val dialogManager: DialogStateManager by activityViewModels()

    override fun onStart() {
        super.onStart()
        val sheetContainer = requireView().parent as? ViewGroup ?: return
        sheetContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            (dialogInterface as? BottomSheetDialog)?.let { bottomSheetDialog ->
                (bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)
                                as? FrameLayout)?.let { frameLayout ->

                    val behavior = BottomSheetBehavior.from(frameLayout)

                    // Set behavior attributes to allow collapsing and dismissing
                    // Set peekHeight to 90% of screen to show MainActivity content at top
                    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
                    behavior.peekHeight = (screenHeight * 0.95f).toInt()
                    behavior.maxHeight = (screenHeight * 0.95f).toInt()
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED // Initially expanded
                    behavior.isDraggable = false // Allow dragging
                    behavior.skipCollapsed = false // Enable collapse
                    behavior.isHideable = false // Allow dismissing

                    // Dismiss the dialog when hidden
                    behavior.addBottomSheetCallback(object :
                        BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                                dismiss()
                            }
                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) {
                            // Handle sliding behavior (optional)
                        }
                    })

                    // Handle edge-to-edge behavior
                    ViewCompat.setOnApplyWindowInsetsListener(frameLayout) { view, insets ->
                        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                        view.setPadding(0, systemBars.top, 0, systemBars.bottom)
                        insets
                    }
                }
            }
        }
        return dialog
    }
}