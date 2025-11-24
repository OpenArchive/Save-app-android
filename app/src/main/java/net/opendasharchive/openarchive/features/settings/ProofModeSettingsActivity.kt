package net.opendasharchive.openarchive.features.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySettingsContainerBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.C2paHelper
import net.opendasharchive.openarchive.util.Hbks
import net.opendasharchive.openarchive.util.Prefs
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import javax.crypto.SecretKey

class ProofModeSettingsActivity : BaseActivity() {

    class Fragment : PreferenceFragmentCompat() {

        private val enrollBiometrics =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                findPreference<SwitchPreferenceCompat>(Prefs.USE_C2PA_KEY_ENCRYPTION)?.let {
                    MainScope().launch {
                        enableC2paKeyEncryption(it)
                    }
                }
            }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs_proof_mode, rootKey)

            val c2paSwitch = findPreference<SwitchPreferenceCompat>(Prefs.USE_C2PA)

            // Check if permission is granted
            val hasPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                c2paSwitch?.isChecked = false // Uncheck if permission not granted
                Prefs.putBoolean(Prefs.USE_C2PA, false)
                Toast.makeText(requireContext(), getString(R.string.phone_permission_required), Toast.LENGTH_LONG).show()
            } else {
                c2paSwitch?.isChecked = Prefs.getBoolean(Prefs.USE_C2PA, false)
            }

            getPrefByKey<SwitchPreferenceCompat>(R.string.pref_key_use_proof_mode)?.setOnPreferenceChangeListener { preference, newValue ->
                if (newValue as Boolean) {
                    PermissionX.init(this)
                        .permissions( Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                        .onExplainRequestReason { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", activity?.packageName, null)
                            intent.data = uri
                            activity?.startActivity(intent)
                        }
                        .request { allGranted, _, _ ->
                            if (!allGranted) {
                                (preference as? SwitchPreferenceCompat)?.isChecked = false
                                Toast.makeText(
                                    activity,
                                    "Please allow all permissions",
                                    Toast.LENGTH_LONG
                                ).show()
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", activity?.packageName, null)
                                intent.data = uri
                                activity?.startActivity(intent)
                            } else {
                                (preference as? SwitchPreferenceCompat)?.isChecked = true
                            }
                        }
                }

                true
            }

            val pkePreference =
                findPreference<SwitchPreferenceCompat>(Prefs.USE_C2PA_KEY_ENCRYPTION)
            val activity = activity
            val availability = Hbks.deviceAvailablity(requireContext())

            if (activity != null && availability !is Hbks.Availability.Unavailable) {
                pkePreference?.isSingleLineTitle = false

                pkePreference?.setTitle(
                    when (Hbks.biometryType(activity)) {
                        Hbks.BiometryType.StrongBiometry -> R.string.prefs_proofmode_key_encryption_title_biometrics

                        Hbks.BiometryType.DeviceCredential -> R.string.prefs_proofmode_key_encryption_title_passcode

                        else -> R.string.prefs_proofmode_key_encryption_title_all
                    }
                )

                pkePreference?.setOnPreferenceChangeListener { _, newValue ->
                    if (newValue as Boolean) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && availability is Hbks.Availability.Enroll) {
                            enrollBiometrics.launch(Hbks.enrollIntent(availability.type))
                        } else {
                            enableC2paKeyEncryption(pkePreference)
                        }
                    } else {
                        if (Prefs.c2paEncryptedPrivateKey != null) {
                            Prefs.c2paEncryptedPrivateKey = null

                            Hbks.removeKey()

                            C2paHelper.restartApp(activity)
                        }
                    }

                    true
                }
            } else {
                pkePreference?.isVisible = false
            }
        }

        private fun enableC2paKeyEncryption(pkePreference: SwitchPreferenceCompat) {

            val key = Hbks.loadKey() ?: Hbks.createKey()

            if (key != null && Prefs.c2paEncryptedPrivateKey == null) {
                createPassphrase(key, activity) {
                    if (it != null) {
                        C2paHelper.removeCertificates(requireContext())

                        // We need to kill the app and restart,
                        // to re-initialize C2PA with the new encrypted key.
                        C2paHelper.restartApp(requireActivity())
                    } else {
                        Hbks.removeKey()

                        pkePreference.isChecked = false
                    }
                }
            } else {
                // What??  shouldn't happen if enrolled with a PIN or Fingerprint
            }
        }


        private fun <T: Preference> getPrefByKey(key: Int): T? {
            return findPreference(getString(key))
        }
    }

    private lateinit var mBinding: ActivitySettingsContainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivitySettingsContainerBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupToolbar(getString(R.string.proofmode))


        supportFragmentManager
            .beginTransaction()
            .replace(mBinding.container.id, Fragment())
            .commit()

//        setContent {

//        }


        val learnModeInfo =
            getString(R.string.prefs_use_proofmode_description, getString(R.string.intro_link_verify))


        val spannedText: Spanned =
            HtmlCompat.fromHtml(learnModeInfo, HtmlCompat.FROM_HTML_MODE_COMPACT)

        mBinding.proofModeLearnMode.text = spannedText

        mBinding.proofModeLearnMode.movementMethod =
            LinkMovementMethod.getInstance() // Enable link clicks

        mBinding.infoCardText.text = HtmlCompat.fromHtml(
            getString(R.string.proof_mode_warning_text),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {

        fun shareCertificate(activity: Activity) {
            try {
                val certPem = C2paHelper.getCertificatePem(activity)

                if (!certPem.isNullOrEmpty()) {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, certPem)
                    activity.startActivity(intent)
                }
            } catch (ioe: IOException) {
                Timber.d("error publishing certificate")
            }
        }

        private fun createPassphrase(
            key: SecretKey,
            activity: FragmentActivity?,
            completed: (passphrase: String?) -> Unit
        ) {
            val passphrase = UUID.randomUUID().toString()

            Hbks.encrypt(passphrase, key, activity) { ciphertext, _ ->
                if (ciphertext == null) {
                    return@encrypt completed(null)
                }

                Prefs.c2paEncryptedPrivateKey = ciphertext

                Hbks.decrypt(
                    Prefs.c2paEncryptedPrivateKey,
                    key,
                    activity
                ) { decryptedPassphrase, _ ->
                    if (decryptedPassphrase == null || decryptedPassphrase != passphrase) {
                        Prefs.c2paEncryptedPrivateKey = null

                        return@decrypt completed(null)
                    }

                    completed(passphrase)
                }
            }
        }
    }
}


