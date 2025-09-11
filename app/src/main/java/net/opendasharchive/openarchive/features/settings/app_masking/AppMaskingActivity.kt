package net.opendasharchive.openarchive.features.settings.app_masking

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityAppMaskingBinding
import net.opendasharchive.openarchive.features.core.BaseActivity

class AppMaskingActivity : BaseActivity() {

    private lateinit var _binding: ActivityAppMaskingBinding
    private val binding: ActivityAppMaskingBinding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityAppMaskingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar(getString(R.string.label_app_masking))

        supportFragmentManager.beginTransaction()
            .replace(binding.maskingSettingsContainer.id, AppMaskingSettingsFragment())
            .commit()
    }

    /**
     * Fragment that handles the "Save App" vs "Masked App" toggles.
     */
    class AppMaskingSettingsFragment : PreferenceFragmentCompat() {

        // Track if we are programmatically updating checkboxes, to avoid infinite loops
        private var isUpdating = false

        private lateinit var saveAppPref: CheckBoxPreference
        private lateinit var maskAppPref: CheckBoxPreference
        private lateinit var aliasStatusPref: Preference // to display which alias is active

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

            setPreferencesFromResource(R.xml.prefs_app_masking, rootKey)

            saveAppPref = findPreference("save_app")!!
            maskAppPref = findPreference("mask_app")!!

            // Initialize the checkboxes based on what was last saved
            initCheckboxStates()

            // Listen to toggles
            saveAppPref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    enableSaveAlias()
                    // Return true to let Preference store the new value
                    true
                } else {
                    // If user is unchecking “Save App”, see if we must force it or let them check the other
                    if (!maskAppPref.isChecked) {
                        // Force at least one to be true, or do your logic
                        // e.g. re-check it or do nothing
                        false
                    } else {
                        true
                    }
                }
            }

            maskAppPref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    enableMaskAlias()
                    true
                } else {
                    if (!saveAppPref.isChecked) {
                        false
                    } else {
                        true
                    }
                }
            }
        }

        private fun initCheckboxStates() {
            // Figure out which alias is active
            val currentAlias = AppMaskingUtils.getCurrentAlias(requireContext())
            val saveAlias = "${requireContext().packageName}.alias.SaveAlias"
            val maskAlias = "${requireContext().packageName}.alias.MaskAlias"

            isUpdating = true
            when (currentAlias) {
                saveAlias -> {
                    saveAppPref.isChecked = true
                    maskAppPref.isChecked = false
                }
                maskAlias -> {
                    saveAppPref.isChecked = false
                    maskAppPref.isChecked = true
                }
                else -> {
                    // If you have no stored preference, default to Save
                    saveAppPref.isChecked = true
                    maskAppPref.isChecked = false
                }
            }
            isUpdating = false
        }

        private fun enableSaveAlias() {
            if (isUpdating) return
            isUpdating = true

            // Turn off the other
            maskAppPref.isChecked = false

            // Enable the SAVE alias
            val saveAlias = "${requireContext().packageName}.alias.SaveAlias"
            AppMaskingUtils.setLauncherActivityAlias(requireContext(), saveAlias)

            isUpdating = false
        }

        private fun enableMaskAlias() {
            if (isUpdating) return
            isUpdating = true

            // Turn off the other
            saveAppPref.isChecked = false

            // Enable the MASK alias
            val maskAlias = "${requireContext().packageName}.alias.MaskAlias"
            AppMaskingUtils.setLauncherActivityAlias(requireContext(), maskAlias)

            isUpdating = false
        }

    }
}