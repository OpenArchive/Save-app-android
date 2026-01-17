package net.opendasharchive.openarchive.features.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.services.tor.TorServiceManager
import net.opendasharchive.openarchive.services.tor.TorStatus
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.ButtonData
import net.opendasharchive.openarchive.features.core.dialog.DialogConfig
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.features.onboarding.StartDestination
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupActivity
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import net.opendasharchive.openarchive.util.extensions.getVersionName
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SettingsFragment : PreferenceFragmentCompat() {

    private val passcodeRepository by inject<PasscodeRepository>()
    private val analyticsManager: AnalyticsManager by inject()
    private val torServiceManager: TorServiceManager by inject()
    private val dialogManager: DialogStateManager by activityViewModel()

    private var passcodePreference: SwitchPreferenceCompat? = null
    private var torPreference: SwitchPreferenceCompat? = null

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

    // Callback to invoke after notification permission result
    private var notificationPermissionCallback: (() -> Unit)? = null

    // Launcher for notification permission (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationPermissionCallback?.invoke()
        } else {
            // Permission denied - still start Tor but warn user
            AppLogger.w("Notification permission denied - Tor will run without notification")
            notificationPermissionCallback?.invoke()
        }
        notificationPermissionCallback = null
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

        findPreference<Preference>(Prefs.PROHIBIT_SCREENSHOTS)?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            Prefs.prohibitScreenshots = enabled

            // Add breadcrumb for crash analysis
            AppLogger.breadcrumb("Feature Toggled", "screenshot_prevention: $enabled")

            // Track feature toggle
            lifecycleScope.launch {
                analyticsManager.trackFeatureToggled("screenshot_prevention", enabled)
            }

            if (activity is BaseActivity) {
                // make sure this gets settings change gets applied instantly
                // (all other activities rely on the hook in BaseActivity.onResume())
                (activity as BaseActivity).updateScreenshotPrevention()
            }

            true
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

        findPreference<Preference>("c2pa_settings")?.setOnPreferenceClickListener {
            startActivity(Intent(context, C2paSettingsActivity::class.java))
            true
        }

        // Setup embedded Tor toggle
        torPreference = getPrefByKey<SwitchPreferenceCompat>(R.string.pref_key_use_tor)
        torPreference?.apply {
            isEnabled = true
            isChecked = Prefs.useTor

            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean

                if (enabled) {
                    // Request notification permission first (Android 13+), then start Tor
                    checkNotificationPermission {
                        Prefs.useTor = true

                        // Add breadcrumb for crash analysis
                        AppLogger.breadcrumb("Feature Toggled", "tor: true")

                        // Track feature toggle
                        lifecycleScope.launch {
                            analyticsManager.trackFeatureToggled("tor", true)
                        }

                        // Start the embedded Tor service
                        // Toggle state will be updated by observeTorStatus() when verified
                        torServiceManager.start()
                    }
                    // Return false - toggle state is controlled by status observer
                    false
                } else {
                    Prefs.useTor = false

                    // Add breadcrumb for crash analysis
                    AppLogger.breadcrumb("Feature Toggled", "tor: false")

                    // Track feature toggle
                    lifecycleScope.launch {
                        analyticsManager.trackFeatureToggled("tor", false)
                    }

                    // Stop the embedded Tor service
                    torServiceManager.stop()
                    // Return false - toggle state is controlled by status observer
                    false
                }
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

            // Add breadcrumb for crash analysis
            AppLogger.breadcrumb("Feature Toggled", "dark_mode: $useDarkMode")

            // Track feature toggle
            lifecycleScope.launch {
                analyticsManager.trackFeatureToggled("dark_mode", useDarkMode)
            }

            true
        }

        findPreference<Preference>(Prefs.UPLOAD_WIFI_ONLY)?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            Prefs.uploadWifiOnly = enabled

            // Add breadcrumb for crash analysis
            AppLogger.breadcrumb("Feature Toggled", "wifi_only_upload: $enabled")

            // Track feature toggle
            lifecycleScope.launch {
                analyticsManager.trackFeatureToggled("wifi_only_upload", enabled)
            }

            val intent =
                Intent(Prefs.UPLOAD_WIFI_ONLY).apply { putExtra("value", enabled) }
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
        val savedScrollY = Prefs.getInt("settings_scroll_position", 0)
        scrollTo(savedScrollY)

        // Observe Tor status and update preference summary
        observeTorStatus()
    }

    private fun observeTorStatus() {
        lifecycleScope.launch {
            torServiceManager.torStatus.collectLatest { status ->
                when (status) {
                    is TorStatus.Idle -> {
                        torPreference?.isEnabled = true
                        torPreference?.isChecked = false
                        torPreference?.summary = getString(R.string.prefs_use_tor_summary)
                    }
                    is TorStatus.Starting -> {
                        // Disable toggle while connecting
                        torPreference?.isEnabled = false
                        torPreference?.isChecked = false
                        torPreference?.summary = getString(R.string.tor_status_connecting)
                    }
                    is TorStatus.On -> {
                        // Still connecting (verifying) - keep toggle disabled and unchecked
                        torPreference?.isEnabled = false
                        torPreference?.isChecked = false
                        torPreference?.summary = getString(R.string.tor_status_connecting)
                        // Trigger verification
                        verifyTorConnection()
                    }
                    is TorStatus.Verified -> {
                        // Now fully connected - enable toggle and show as checked
                        torPreference?.isEnabled = true
                        torPreference?.isChecked = true
                        val info = status.info
                        torPreference?.summary = if (info.exitCountry != null) {
                            getString(R.string.tor_status_verified_with_country, info.exitIp, info.exitCountry)
                        } else {
                            getString(R.string.tor_status_verified, info.exitIp)
                        }
                    }
                    is TorStatus.Off -> {
                        torPreference?.isEnabled = true
                        torPreference?.isChecked = false
                        torPreference?.summary = getString(R.string.prefs_use_tor_summary)
                    }
                    is TorStatus.Error -> {
                        torPreference?.isEnabled = true
                        torPreference?.isChecked = false
                        torPreference?.summary = getString(R.string.tor_status_error, status.message)
                    }
                }
            }
        }
    }

    /**
     * Check notification permission (for Android 13+) and invoke the callback.
     * On older Android versions or if permission is already granted, callback is invoked immediately.
     */
    private fun checkNotificationPermission(onComplete: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                // Permission already granted
                onComplete()
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Show rationale dialog explaining why we need notification permission
                dialogManager.showDialog(
                    config = DialogConfig(
                        type = DialogType.Warning,
                        title = UiText.StringResource(R.string.tor_notification_permission_title),
                        message = UiText.StringResource(R.string.tor_notification_permission_message),
                        positiveButton = ButtonData(
                            text = UiText.StringResource(R.string.lbl_ok),
                            action = {
                                notificationPermissionCallback = onComplete
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        ),
                        neutralButton = ButtonData(
                            text = UiText.StringResource(R.string.lbl_Cancel),
                            action = {
                                // User cancelled - still start Tor without notification
                                onComplete()
                            }
                        )
                    )
                )
            } else {
                // Request permission directly
                notificationPermissionCallback = onComplete
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // For Android versions below 13, no notification permission is needed
            onComplete()
        }
    }

    private fun verifyTorConnection() {
        lifecycleScope.launch {
            val result = torServiceManager.verifyTorConnection()
            if (!result.isUsingTor && result.error != null) {
                AppLogger.w("Tor verification failed: ${result.error}")
            }
        }
    }

        override fun onResume() {
        super.onResume()
        val savedScrollY = Prefs.getInt("settings_scroll_position", 0)
        scrollTo(savedScrollY)
    }

    private fun scrollTo(savedScrollY: Int) {
        // Post to ensure RecyclerView is fully laid out with items
        listView.post {
            val currentScrollY = listView.computeVerticalScrollOffset()
            val scrollDelta = savedScrollY - currentScrollY
            AppLogger.i("SettingsFragment - scrolling from $currentScrollY to $savedScrollY (delta: $scrollDelta)")
            listView.scrollBy(0, scrollDelta)
        }
    }

    override fun onPause() {
        super.onPause()

        // Save current scroll position to Prefs
        val scrollY = listView.computeVerticalScrollOffset()
        AppLogger.i("SettingsFragment onPause - saving scroll position: $scrollY")
        Prefs.putInt("settings_scroll_position", scrollY)
    }
}