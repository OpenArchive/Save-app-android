package net.opendasharchive.openarchive.features.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.res.stringResource
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySettingsContainerBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupActivity
import net.opendasharchive.openarchive.services.tor.TorStatus
import net.opendasharchive.openarchive.services.tor.TorViewModel
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.android.ext.android.inject


class GeneralSettingsActivity : BaseActivity() {

    class Fragment : PreferenceFragmentCompat() {

        private val passcodeRepository by inject<PasscodeRepository>()

        private var passcodePreference: SwitchPreferenceCompat? = null

        private val torViewModel: TorViewModel by viewModel()

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

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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
                    AlertDialog.Builder(requireContext())
                        .setTitle("Disable Passcode")
                        .setMessage("Are you sure you want to disable the passcode?")
                        .setPositiveButton("Yes") { _, _ ->
                            passcodeRepository.clearPasscode()
                            passcodePreference?.isChecked = false

                            // Update the FLAG_SECURE dynamically
                            (activity as? BaseActivity)?.updateScreenshotPrevention()
                        }
                        .setNegativeButton("No") { _, _ ->
                            passcodePreference?.isChecked = true
                        }
                        .show()
                }
                // Return false to avoid the preference updating immediately
                false
            }

            findPreference<Preference>("proof_mode")?.setOnPreferenceClickListener {
                startActivity(Intent(context, ProofModeSettingsActivity::class.java))
                true
            }

            findPreference<Preference>(Prefs.THEME)?.setOnPreferenceChangeListener { _, newValue ->
                Theme.set(requireActivity(), Theme.get(newValue as? String))

                true
            }

            findPreference<Preference>(Prefs.PROHIBIT_SCREENSHOTS)?.setOnPreferenceClickListener { _ ->
                if (activity is BaseActivity) {
                    // make sure this gets settings change gets applied instantly
                    // (all other activities rely on the hook in BaseActivity.onResume())
                    (activity as BaseActivity).updateScreenshotPrevention()
                }

                true
            }


            val useTorPref = findPreference<SwitchPreferenceCompat>(Prefs.USE_TOR)
            val torStatusPref = findPreference<EditTextPreference>("tor_status")

            val setUseTorText: (TorStatus, Boolean) -> Unit = { torStatus, enabled ->
                if (torStatus == TorStatus.CONNECTED) {
                    if (enabled) {
                        torStatusPref?.setSummary(R.string.prefs_use_tor_enabled)
                    } else {
                        torStatusPref?.setSummary(R.string.prefs_use_tor_ready)
                    }
                } else {
                    if (enabled) {
                        torStatusPref?.setSummary(R.string.prefs_use_tor_disabled)
                    } else {
                        torStatusPref?.setSummary(R.string.prefs_use_tor_not_ready)
                    }
                }
            }

            this.lifecycleScope.launch {
                torViewModel.torStatus.collect { torStatus ->
                    setUseTorText(torStatus, useTorPref?.isChecked == true)
                }
            }
            useTorPref?.apply {
                setUseTorText(torViewModel.torStatus.value, isChecked)
                setOnPreferenceChangeListener { _, newValue ->
                    val enabled = newValue as Boolean
                    torViewModel.toggleTorServiceState(requireActivity(), enabled)
                    setUseTorText(torViewModel.torStatus.value, enabled)
                    true
                }
            }

            findPreference<OpenOrbotPreference>("open_orbot")?.setOnOpenOrbotListener {
                torViewModel.requestOpenOrInstallOrbot(requireActivity())
            }
        }

        override fun onResume() {
            super.onResume()
            torViewModel.requestTorStatus()
        }
    }


    private lateinit var mBinding: ActivitySettingsContainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivitySettingsContainerBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupToolbar(title = getString(R.string.general))

        supportFragmentManager
            .beginTransaction()
            .replace(mBinding.container.id, Fragment())
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}