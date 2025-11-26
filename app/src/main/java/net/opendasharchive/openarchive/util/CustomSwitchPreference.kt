package net.opendasharchive.openarchive.util

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.materialswitch.MaterialSwitch
import net.opendasharchive.openarchive.R

class CustomSwitchPreference @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SwitchPreferenceCompat(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        // Find the switch inside the custom layout
        val switchView = holder.findViewById(android.R.id.checkbox) as? MaterialSwitch

        switchView?.apply {
            isChecked = this@CustomSwitchPreference.isChecked
            setOnCheckedChangeListener { _, isChecked ->
                if (callChangeListener(isChecked)) {
                    persistBoolean(isChecked) // Save preference
                    this@CustomSwitchPreference.isChecked = isChecked
                }
            }
        }
    }
}