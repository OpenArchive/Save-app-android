package net.opendasharchive.openarchive.features.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.CleanInsightsManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.infrastructure.client.enqueueResult
import net.opendasharchive.openarchive.databinding.ActivitySettingsContainerBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.services.tor.TorViewModel
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import okhttp3.Request
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.getValue

class GeneralSettingsActivity: BaseActivity() {

    class Fragment: PreferenceFragmentCompat() {

        private val torViewModel: TorViewModel by viewModel()

        private var mCiConsentPref: SwitchPreferenceCompat? = null


        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs_general, rootKey)

            findPreference<Preference>(Prefs.USE_TOR)?.setOnPreferenceChangeListener { _, newValue ->
                torViewModel.toggleTorServiceState()
                true
            }

            this.lifecycleScope.launch {
                torViewModel.torStatus.collect { torStatus ->
                    findPreference<Preference>("tor_status")?.setSummary(
                        torStatus.toString().lowercase()
                    )
                }
            }


            findPreference<Preference>("proof_mode")?.setOnPreferenceClickListener {
                startActivity(Intent(context, ProofModeSettingsActivity::class.java))
                true
            }

            findPreference<Preference>(Prefs.THEME)?.setOnPreferenceChangeListener { _, newValue ->
                Theme.set(Theme.get(newValue as? String))

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

//            mCiConsentPref = findPreference("health_checks")
//
//            mCiConsentPref?.setOnPreferenceChangeListener { _, newValue ->
//                if (newValue as? Boolean == false) {
//                    CleanInsightsManager.deny()
//                }
//                else {
//                    startActivity(Intent(context, ConsentActivity::class.java))
//                }
//
//                true
//            }
        }

        override fun onResume() {
            super.onResume()

            mCiConsentPref?.isChecked = CleanInsightsManager.hasConsent()
        }
    }


    private lateinit var mBinding: ActivitySettingsContainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivitySettingsContainerBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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