package net.opendasharchive.openarchive.core.presentation.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.CustomBottomNavBinding

class CustomBottomNavBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var onMyMediaClick: (() -> Unit)? = null
    var onSettingsClick: (() -> Unit)? = null

    var onAddClick: (() -> Unit)? = null
    var onAddLongClick: (() -> Unit)? = null

    // Inflate the layout
    private var binding: CustomBottomNavBinding =
        CustomBottomNavBinding.inflate(LayoutInflater.from(context), this, true)

    init {


        // Set up click listeners
        binding.myMediaButton.setOnClickListener { onMyMediaClick?.invoke() }
        binding.settingsButton.setOnClickListener { onSettingsClick?.invoke() }

        binding.addButton.setOnClickListener { onAddClick?.invoke() }


        binding.myMediaLabel.setOnClickListener {
            // perform click + play ripple animation
            binding.myMediaButton.isPressed = true
            binding.myMediaButton.isPressed = false
            binding.myMediaButton.performClick()
        }

        binding.settingsLabel.setOnClickListener {
            // perform click + play ripple animation
            binding.settingsButton.isPressed = true
            binding.settingsButton.isPressed = false
            binding.settingsButton.performClick()
        }
    }

    /**
     * Updates the highlighted state of the navigation bar buttons.
     */
    fun updateSelectedItem(isSettings: Boolean) {
        if (isSettings) {
            binding.myMediaButton.setIconResource(R.drawable.outline_perm_media_24)
            binding.settingsButton.setIconResource(R.drawable.ic_settings_filled)
        } else {
            binding.myMediaButton.setIconResource(R.drawable.perm_media_24px)
            binding.settingsButton.setIconResource(R.drawable.ic_settings)
        }
    }

    fun setAddButtonLongClickEnabled() {
        binding.addButton.setOnLongClickListener {
            onAddLongClick?.invoke()
            true
        }
    }
}