package net.opendasharchive.openarchive.features.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.features.onboarding.StartDestination
import net.opendasharchive.openarchive.features.settings.app_masking.AppMaskingActivity
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupActivity
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import net.opendasharchive.openarchive.util.extensions.getVersionName
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SettingsFragment : PreferenceFragmentCompat() {

    private val appConfig by inject<AppConfig>()

    private val passcodeRepository by inject<PasscodeRepository>()

    private val dialogManager: DialogStateManager by activityViewModel()


    private var passcodePreference: SwitchPreferenceCompat? = null

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val passcodeEnabled = result.data?.getBooleanExtra("passcode_enabled", false) ?: false
            passcodePreference?.isChecked = passcodeEnabled
        } else {
            passcodePreference?.isChecked = false
        }
    }

//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return ComposeView(requireContext()).apply {
//            // Dispose of the Composition when the view's LifecycleOwner
//            // is destroyed
//            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
//            setContent {
//                Theme {
//                    SettingsScreen()
//                }
//            }
//        }
//    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.prefs_general, rootKey)


        passcodePreference = findPreference(Prefs.PASSCODE_ENABLED)

        passcodePreference?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled) {
                // Launch PasscodeSetupActivity
                val intent = Intent(context, PasscodeSetupActivity::class.java)
                activityResultLauncher.launch(intent)
            } else {
                // Show confirmation dialog
                dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                    type = DialogType.Warning
                    title = UiText.StringResource(R.string.disable_passcode_dialog_title)
                    message = UiText.StringResource(R.string.disable_passcode_dialog_msg)
                    positiveButton {
                        text = UiText.StringResource(R.string.answer_yes)
                        action = {
                            passcodeRepository.clearPasscode()
                            passcodePreference?.isChecked = false

                            // Update the FLAG_SECURE dynamically
                            (activity as? BaseActivity)?.updateScreenshotPrevention()
                        }
                    }
                    neutralButton {
                        action = {
                            passcodePreference?.isChecked = true
                        }
                    }
                }
            }
            // Return false to avoid the preference updating immediately
            false
        }

        findPreference<Preference>(Prefs.PROHIBIT_SCREENSHOTS)?.setOnPreferenceClickListener { _ ->
            if (activity is BaseActivity) {
                // make sure this gets settings change gets applied instantly
                // (all other activities rely on the hook in BaseActivity.onResume())
                (activity as BaseActivity).updateScreenshotPrevention()
            }

            true
        }

        // Check if app masking is enabled in the app config
        if (appConfig.appMaskingEnabled) {
            getPrefByKey<Preference>(R.string.pref_app_masking)?.setOnPreferenceClickListener {
                startActivity(Intent(context, AppMaskingActivity::class.java))
                true
            }
        } else {
            // Remove the app masking preference if the feature is disabled
            findPreference<Preference>(getString(R.string.pref_app_masking))?.isVisible = false
        }

        getPrefByKey<Preference>(R.string.pref_media_servers)?.setOnPreferenceClickListener {
            val intent = Intent(context, SpaceSetupActivity::class.java)
            intent.putExtra(SpaceSetupActivity.LABEL_START_DESTINATION, StartDestination.SPACE_LIST.name)
            startActivity(intent)
            true
        }

        getPrefByKey<Preference>(R.string.pref_media_folders)?.setOnPreferenceClickListener {
            val intent = Intent(context, SpaceSetupActivity::class.java)
            intent.putExtra(SpaceSetupActivity.LABEL_START_DESTINATION, StartDestination.ARCHIVED_FOLDER_LIST.name)
            intent.putExtra(FoldersFragment.EXTRA_SHOW_ARCHIVED, true)
            startActivity(intent)
            true
        }

        getPrefByKey<Preference>(R.string.pref_key_proof_mode)?.setOnPreferenceClickListener {
            startActivity(Intent(context, ProofModeSettingsActivity::class.java))
            true
        }

        findPreference<Preference>(Prefs.USE_TOR)?.setOnPreferenceChangeListener { _, newValue ->
            Prefs.useTor = (newValue as Boolean)
            //torViewModel.updateTorServiceState()
            true
        }

        getPrefByKey<SwitchPreferenceCompat>(R.string.pref_key_use_tor)?.apply {
            isEnabled = true

            setOnPreferenceClickListener {
                dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                    type = DialogType.Info
                    iconColor = dialogManager.requireResourceProvider().getColor(R.color.colorTertiary)
                    title = UiText.StringResource(R.string.tor_disabled_title)
                    message = UiText.StringResource(R.string.tor_disabled_message)
                    positiveButton {
                        text = UiText.StringResource(R.string.tor_download_btn_label)
                        action = {
                            // Launch the Tor download activity
                            val intent = Intent(Intent.ACTION_VIEW, Prefs.TOR_DOWNLOAD_URL)
                            startActivity(intent)
                        }
                    }
                    neutralButton {
                        text = UiText.StringResource(android.R.string.cancel)
                    }
                }
                true
            }

            setOnPreferenceChangeListener { _, newValue ->
                false
            }
        }

        findPreference<Preference>(Prefs.THEME)?.setOnPreferenceChangeListener { _, newValue ->
            Theme.set(Theme.get(newValue as? String))
            true
        }

        // Retrieve the switch preference
        val darkModeSwitch = getPrefByKey<SwitchPreferenceCompat>(R.string.pref_key_use_dark_mode)

        // Get the saved dark mode preference
        val isDarkModeEnabled = Prefs.getBoolean(getString(R.string.pref_key_use_dark_mode), false)

        // Set the switch state based on the saved preference
        darkModeSwitch?.isChecked = isDarkModeEnabled

        getPrefByKey<SwitchPreferenceCompat>(R.string.pref_key_use_dark_mode)?.setOnPreferenceChangeListener { pref, newValue ->
            val useDarkMode = newValue as Boolean
            val theme = if (useDarkMode) Theme.DARK else Theme.LIGHT
            Theme.set(theme)
            // Save the preference
            Prefs.putBoolean(getString(R.string.pref_key_use_dark_mode), useDarkMode)
            true
        }

        findPreference<Preference>(Prefs.UPLOAD_WIFI_ONLY)?.setOnPreferenceChangeListener { _, newValue ->
            val intent =
                Intent(Prefs.UPLOAD_WIFI_ONLY).apply { putExtra("value", newValue as Boolean) }
            // Replace with shared ViewModel + LiveData
            // LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
            true
        }

        val packageManager = requireActivity().packageManager
        val versionText = packageManager.getVersionName(requireActivity().packageName)

        getPrefByKey<Preference>(R.string.pref_key_app_version)?.summary = versionText
    }

    private fun <T : Preference> getPrefByKey(key: Int): T? {
        return findPreference(getString(key))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}