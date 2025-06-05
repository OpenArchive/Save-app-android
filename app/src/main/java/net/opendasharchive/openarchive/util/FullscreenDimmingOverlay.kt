package net.opendasharchive.openarchive.util

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import net.opendasharchive.openarchive.R

class FullScreenDimmingOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var cancelButton: MaterialButton

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_progress_dialog, this, true)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        cancelButton = findViewById(R.id.cancel_button)

        cancelButton.setOnClickListener {
            Utility.showMaterialPrompt(
                context,
                title = "Confirm",
                message = "Do you want to cancel?",
                positiveButtonText = "Yes",
                negativeButtonText = "No") { affirm ->
                if (affirm) {
                    // TODO: Cancel the offending event
                    hide()
                }
            }
        }
    }

    fun show() {
        if (!isVisible) {
            alpha = 0f
            isVisible = true
            animate().alpha(1f).setDuration(200).start()
        }
    }

    fun hide() {
        if (isVisible) {
            animate().alpha(0f).setDuration(200).withEndAction {
                isVisible = false
            }.start()
        }
    }
}

class FullScreenCreateGroupDimmingOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var cancelButton: MaterialButton

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_progress_create_group_dialog, this, true)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        cancelButton = findViewById(R.id.cancel_button)

        cancelButton.setOnClickListener {
            Utility.showMaterialPrompt(
                context,
                title = "Confirm",
                message = "Do you want to cancel?",
                positiveButtonText = "Yes",
                negativeButtonText = "No") { affirm ->
                if (affirm) {
                    // TODO: Cancel the offending event
                    hide()
                }
            }
        }
    }

    fun show() {
        if (!isVisible) {
            alpha = 0f
            isVisible = true
            animate().alpha(1f).setDuration(200).start()
        }
    }

    fun hide() {
        if (isVisible) {
            animate().alpha(0f).setDuration(200).withEndAction {
                isVisible = false
            }.start()
        }
    }
}