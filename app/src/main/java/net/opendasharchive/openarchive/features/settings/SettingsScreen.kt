package net.opendasharchive.openarchive.features.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.PreferenceTheme
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.preferenceTheme
import me.zhanghai.compose.preference.rememberPreferenceState
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.analytics.api.AnalyticsManager
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles
import net.opendasharchive.openarchive.features.core.BaseComposeActivity
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository
import net.opendasharchive.openarchive.features.settings.passcode.passcode_entry.PasscodeEntryActivity
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupActivity
import net.opendasharchive.openarchive.services.tor.TorServiceManager
import net.opendasharchive.openarchive.services.tor.TorStatus
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import net.opendasharchive.openarchive.util.extensions.getVersionName
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onNavigateToSpaceList: () -> Unit = {},
    onNavigateToArchivedFolders: () -> Unit = {},
    onNavigateToCache: () -> Unit = {},
    onNavigateToC2pa: () -> Unit = {},
) {
    val dialogManager: DialogStateManager = koinInject()
    val context = LocalContext.current

    if (LocalInspectionMode.current) {
        PreviewSettingsScreen()
        return
    }

    val sharedPreferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val preferenceFlow = remember(sharedPreferences) { createFilteredPreferenceFlow(sharedPreferences) }
    val analyticsManager: AnalyticsManager = koinInject()
    val passcodeRepository: PasscodeRepository = koinInject()
    val torServiceManager: TorServiceManager = koinInject()

    val coroutineScope = rememberCoroutineScope()
    val appVersion = remember { context.packageManager.getVersionName(context.packageName) }

    val torStatus by torServiceManager.torStatus.collectAsStateWithLifecycle()

    // Trigger Tor verification when the service reports it is bootstrapped (TorStatus.On).
    val isTorBootstrapped = torStatus is TorStatus.On
    LaunchedEffect(isTorBootstrapped) {
        if (isTorBootstrapped) {
            torServiceManager.verifyTorConnection()
        }
    }

    // Derive toggle appearance from the actual Tor status.
    val torChecked = torStatus is TorStatus.Verified
    val torInteractive = torStatus !is TorStatus.Starting && torStatus !is TorStatus.On

    val torSummary = when (val s = torStatus) {
        is TorStatus.Starting, is TorStatus.On ->
            stringResource(R.string.tor_status_connecting)
        is TorStatus.Verified -> {
            val info = s.info
            if (info.exitCountry != null)
                stringResource(R.string.tor_status_verified_with_country, info.exitIp, info.exitCountry)
            else
                stringResource(R.string.tor_status_verified, info.exitIp)
        }
        is TorStatus.Error ->
            stringResource(R.string.tor_status_error, s.message)
        else -> stringResource(R.string.prefs_use_tor_summary)
    }

    // Launcher for notification permission required by the Tor foreground service (Android 13+).
    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Start Tor whether the user granted or denied – same behaviour as the old fragment.
        Prefs.useTor = true
        AppLogger.breadcrumb("Feature Toggled", "tor: true")
        coroutineScope.launch { analyticsManager.trackFeatureToggled("tor", true) }
        torServiceManager.start()
    }

    fun startTor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        Prefs.useTor = true
        AppLogger.breadcrumb("Feature Toggled", "tor: true")
        coroutineScope.launch { analyticsManager.trackFeatureToggled("tor", true) }
        torServiceManager.start()
    }

    fun stopTor() {
        Prefs.useTor = false
        AppLogger.breadcrumb("Feature Toggled", "tor: false")
        coroutineScope.launch { analyticsManager.trackFeatureToggled("tor", false) }
        torServiceManager.stop()
    }

    ProvidePreferenceLocals(flow = preferenceFlow, theme = savePreferenceTheme()) {

        val settingStrings = SettingsStrings(
            titleSecure = stringResource(R.string.pref_title_secure),
            titleArchive = stringResource(R.string.pref_title_archive),
            titleVerify = stringResource(R.string.intro_header_verify),
            titleEncrypt = stringResource(R.string.intro_header_encrypt),
            titleGeneral = stringResource(R.string.general),
            titlePasscode = stringResource(R.string.passcode_lock_app),
            titleWifiOnly = stringResource(R.string.only_upload_media_when_you_are_connected_to_wi_fi),
            titleMediaServers = stringResource(R.string.pref_title_media_servers),
            summaryMediaServers = stringResource(R.string.pref_summary_media_servers),
            titleMediaFolders = stringResource(R.string.pref_title_media_folders),
            summaryMediaFolders = stringResource(R.string.pref_summary_media_folders),
            titleC2pa = stringResource(R.string.c2pa_content_authenticity),
            summaryC2pa = stringResource(R.string.prefs_use_c2pa_summary),
            titleTor = stringResource(R.string.prefs_use_tor_title),
            titleDarkMode = stringResource(R.string.pref_title_dark_mode),
            titleLanguage = stringResource(R.string.pref_title_language),
            summaryLanguage = stringResource(R.string.pref_summary_language),
            titleAbout = stringResource(R.string.save_by_open_archive),
            summaryAbout = stringResource(R.string.discover_the_save_app),
            titlePrivacy = stringResource(R.string.pref_title_privacy_policy),
            summaryPrivacy = stringResource(R.string.pref_summary_privacy_policy),
            titleVersion = stringResource(R.string.pref_title_version),
        )

        val passcodeState = rememberPreferenceState(key = Prefs.PASSCODE_ENABLED, defaultValue = Prefs.passcodeEnabled)
        val wifiOnlyState = rememberPreferenceState(key = Prefs.UPLOAD_WIFI_ONLY, defaultValue = Prefs.uploadWifiOnly)
        val darkModeKey = stringResource(R.string.pref_key_use_dark_mode)
        val darkModeState = rememberPreferenceState(key = darkModeKey, defaultValue = Prefs.getBoolean(darkModeKey, false))

        val passcodeLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val passcodeEnabled =
                    result.resultCode == Activity.RESULT_OK &&
                        (result.data?.getBooleanExtra(PasscodeSetupActivity.EXTRA_PASSCODE_ENABLED, false) ?: false)
                passcodeState.value = passcodeEnabled
                (context as? BaseComposeActivity)?.updateScreenshotPrevention()
            }

        // Launched when the user wants to DISABLE the passcode — requires verifying current passcode first
        val passcodeVerifyLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    passcodeRepository.clearPasscode()
                    passcodeState.value = false
                    (context as? BaseComposeActivity)?.updateScreenshotPrevention()
                } else {
                    // Verification failed or cancelled — revert toggle
                    passcodeState.value = true
                }
            }

        SettingsScreenContent(
            strings = settingStrings,
            appVersion = appVersion,
            passcodeState = passcodeState,
            wifiOnlyState = wifiOnlyState,
            torChecked = torChecked,
            torInteractive = torInteractive,
            torSummary = torSummary,
            darkModeState = darkModeState,
            onPasscodeToggle = { newValue ->
                if (newValue) {
                    passcodeLauncher.launch(Intent(context, PasscodeSetupActivity::class.java))
                } else {
                    passcodeState.value = true
                    passcodeVerifyLauncher.launch(
                        Intent(context, PasscodeEntryActivity::class.java).apply {
                            putExtra(PasscodeEntryActivity.EXTRA_VERIFY_MODE, true)
                        }
                    )
                }
            },
            onWifiOnlyToggle = { newValue ->
                wifiOnlyState.value = newValue
                Prefs.uploadWifiOnly = newValue
                AppLogger.breadcrumb("Feature Toggled", "wifi_only_upload: $newValue")
                coroutineScope.launch { analyticsManager.trackFeatureToggled("wifi_only_upload", newValue) }
            },
            onTorToggle = { enable ->
                if (enable) startTor() else stopTor()
            },
            onDarkModeToggle = { enabled ->
                darkModeState.value = enabled
                Theme.set(if (enabled) Theme.DARK else Theme.LIGHT)
                Prefs.putBoolean(darkModeKey, enabled)
                AppLogger.breadcrumb("Feature Toggled", "dark_mode: $enabled")
                coroutineScope.launch { analyticsManager.trackFeatureToggled("dark_mode", enabled) }
            },
            onLanguageClick = { openAppLanguageSettings(context) },
            onMediaServersClick = onNavigateToSpaceList,
            onMediaFoldersClick = onNavigateToArchivedFolders,
            onC2paClick = onNavigateToC2pa,
            onAboutClick = { openUrl(context, "https://open-archive.org/save") },
            onPrivacyClick = { openUrl(context, "https://open-archive.org/privacy") },
            onNavigateToCache = onNavigateToCache,
        )
    }
}

@Composable
private fun SettingsScreenContent(
    strings: SettingsStrings,
    appVersion: String,
    passcodeState: MutableState<Boolean>,
    wifiOnlyState: MutableState<Boolean>,
    torChecked: Boolean,
    torInteractive: Boolean,
    torSummary: String,
    darkModeState: MutableState<Boolean>,
    onPasscodeToggle: (Boolean) -> Unit,
    onWifiOnlyToggle: (Boolean) -> Unit,
    onTorToggle: (Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    onMediaServersClick: () -> Unit,
    onMediaFoldersClick: () -> Unit,
    onC2paClick: () -> Unit,
    onAboutClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onNavigateToCache: () -> Unit,
) {
    val rowModifier = Modifier.fillMaxWidth()

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // ── Secure ────────────────────────────────────────────────────────────
        preferenceCategory(key = "category_secure", title = { PreferenceCategoryTitle(text = strings.titleSecure) })
        item(key = "passcode") {
            SwitchPreference(
                state = passcodeState,
                title = { PreferenceTitle(text = strings.titlePasscode, maxLines = 2) },
                modifier = rowModifier,
                onToggle = onPasscodeToggle,
            )
        }
        sectionDivider("divider_secure")

        // ── Archive ───────────────────────────────────────────────────────────
        preferenceCategory(key = "category_archive", title = { PreferenceCategoryTitle(text = strings.titleArchive) })
        item(key = "wifi_only") {
            SwitchPreference(
                state = wifiOnlyState,
                title = { PreferenceTitle(text = strings.titleWifiOnly, maxLines = 2) },
                modifier = rowModifier,
                onToggle = onWifiOnlyToggle,
            )
        }
        preference(
            key = "media_servers",
            title = { PreferenceTitle(text = strings.titleMediaServers) },
            summary = { PreferenceSummary(text = strings.summaryMediaServers) },
            modifier = rowModifier,
            onClick = onMediaServersClick,
        )
        preference(
            key = "media_folders",
            title = { PreferenceTitle(text = strings.titleMediaFolders) },
            summary = { PreferenceSummary(text = strings.summaryMediaFolders) },
            modifier = rowModifier,
            onClick = onMediaFoldersClick,
        )
        sectionDivider("divider_archive")

        // ── Verify ────────────────────────────────────────────────────────────
        preferenceCategory(key = "category_verify", title = { PreferenceCategoryTitle(text = strings.titleVerify) })
        preference(
            key = "c2pa_settings",
            title = { PreferenceTitle(text = strings.titleC2pa) },
            summary = { PreferenceSummary(text = strings.summaryC2pa) },
            modifier = rowModifier,
            onClick = onC2paClick,
        )
        sectionDivider("divider_verify")

        // ── Encrypt ───────────────────────────────────────────────────────────
        preferenceCategory(key = "category_encrypt", title = { PreferenceCategoryTitle(text = strings.titleEncrypt) })
        item(key = "tor") {
            TorSwitchPreference(
                checked = torChecked,
                enabled = torInteractive,
                title = { PreferenceTitle(text = strings.titleTor, maxLines = 2) },
                summary = { PreferenceSummary(text = torSummary) },
                modifier = rowModifier,
                onToggle = onTorToggle,
            )
        }
        sectionDivider("divider_encrypt")

        // ── General ───────────────────────────────────────────────────────────
        preferenceCategory(key = "category_general", title = { PreferenceCategoryTitle(text = strings.titleGeneral) })
        item(key = "dark_mode") {
            SwitchPreference(
                state = darkModeState,
                title = { PreferenceTitle(text = strings.titleDarkMode) },
                modifier = rowModifier,
                onToggle = onDarkModeToggle,
            )
        }
        preference(
            key = "language",
            title = { PreferenceTitle(text = strings.titleLanguage) },
            summary = { PreferenceSummary(text = strings.summaryLanguage) },
            modifier = rowModifier,
            onClick = onLanguageClick,
        )
        preference(
            key = "about",
            title = { PreferenceTitle(text = strings.titleAbout) },
            summary = { PreferenceSummary(text = strings.summaryAbout) },
            modifier = rowModifier,
            onClick = onAboutClick,
        )
        preference(
            key = "privacy",
            title = { PreferenceTitle(text = strings.titlePrivacy) },
            summary = { PreferenceSummary(text = strings.summaryPrivacy) },
            modifier = rowModifier,
            onClick = onPrivacyClick,
        )
        preference(
            key = "version",
            title = { PreferenceTitle(text = strings.titleVersion) },
            summary = { PreferenceSummary(text = appVersion) },
            modifier = rowModifier,
            enabled = false,
        )
    }
}

// ── Data ──────────────────────────────────────────────────────────────────────

private data class SettingsStrings(
    val titleSecure: String,
    val titleArchive: String,
    val titleVerify: String,
    val titleEncrypt: String,
    val titleGeneral: String,
    val titlePasscode: String,
    val titleWifiOnly: String,
    val titleMediaServers: String,
    val summaryMediaServers: String,
    val titleMediaFolders: String,
    val summaryMediaFolders: String,
    val titleC2pa: String,
    val summaryC2pa: String,
    val titleTor: String,
    val titleDarkMode: String,
    val titleLanguage: String,
    val summaryLanguage: String,
    val titleAbout: String,
    val summaryAbout: String,
    val titlePrivacy: String,
    val summaryPrivacy: String,
    val titleVersion: String,
)

// ── Composables ───────────────────────────────────────────────────────────────

/**
 * A Switch preference whose checked/enabled state is driven by external values rather than
 * a MutableState. Used for the Tor toggle which derives its state from TorServiceManager.
 */
@Composable
private fun TorSwitchPreference(
    checked: Boolean,
    enabled: Boolean,
    title: @Composable () -> Unit,
    summary: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onToggle: (Boolean) -> Unit,
) {
    Preference(
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = { onToggle(!checked) },
        modifier = modifier,
        widgetContainer = {
            val theme = me.zhanghai.compose.preference.LocalPreferenceTheme.current
            Switch(
                checked = checked,
                onCheckedChange = { onToggle(it) },
                enabled = enabled,
                modifier = Modifier.padding(start = theme.horizontalSpacing, end = 24.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                )
            )
        }
    )
}

@Composable
private fun PreferenceCategoryTitle(text: String) {
    Text(
        text = text,
        maxLines = 1,
        style = SaveTextStyles.titleLarge,
        color = colorResource(id = R.color.colorTertiary),
    )
}

@Composable
fun PreferenceTitle(text: String, maxLines: Int = 1) {
    Text(
        text = text,
        maxLines = maxLines,
        style = SaveTextStyles.bodyLarge,
        color = colorResource(id = R.color.colorOnBackground),
    )
}

@Composable
private fun PreferenceSummary(text: String) {
    Text(
        text = text,
        maxLines = 2,
        style = SaveTextStyles.bodySmallEmphasis,
        color = colorResource(id = R.color.colorOnSurfaceVariant),
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = colorResource(id = R.color.colorDivider).copy(alpha = 0.5f),
    )
}

private fun LazyListScope.sectionDivider(key: String) {
    item(key = key) { SettingsDivider() }
}

@Composable
fun SwitchPreference(
    state: MutableState<Boolean>,
    title: @Composable () -> Unit,
    summary: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onToggle: (Boolean) -> Unit = { newValue -> state.value = newValue },
) {
    val value by state
    Preference(
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = { onToggle(!value) },
        modifier = modifier,
        widgetContainer = {
            val theme = me.zhanghai.compose.preference.LocalPreferenceTheme.current
            Switch(
                checked = value,
                onCheckedChange = { onToggle(it) },
                enabled = enabled,
                modifier = Modifier.padding(start = theme.horizontalSpacing, end = 24.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                )
            )
        }
    )
}

@Composable
fun savePreferenceTheme(): PreferenceTheme {
    val categoryColor = colorResource(id = R.color.colorTertiary)
    val titleColor = colorResource(id = R.color.colorOnBackground)
    val summaryColor = colorResource(id = R.color.colorOnSurfaceVariant)
    return preferenceTheme(
        categoryPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
        categoryColor = categoryColor,
        categoryTextStyle = SaveTextStyles.titleLarge,
        padding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalSpacing = 16.dp,
        verticalSpacing = 16.dp,
        iconContainerMinWidth = 0.dp,
        titleColor = titleColor,
        titleTextStyle = SaveTextStyles.bodyLarge,
        summaryColor = summaryColor,
        summaryTextStyle = SaveTextStyles.bodySmallEmphasis,
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}

private fun openAppLanguageSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
    context.startActivity(intent)
}

// ── Preference flow ───────────────────────────────────────────────────────────

@OptIn(DelicateCoroutinesApi::class)
private fun createFilteredPreferenceFlow(
    sharedPreferences: SharedPreferences,
): MutableStateFlow<me.zhanghai.compose.preference.Preferences> {
    val initialPreferences = sharedPreferences.toSupportedPreferences()
    return MutableStateFlow(initialPreferences).also { flow ->
        GlobalScope.launch(Dispatchers.Main.immediate) {
            flow.drop(1).collect { prefs ->
                sharedPreferences.edit {
                    for ((key, value) in prefs.asMap()) {
                        when (value) {
                            is Boolean -> putBoolean(key, value)
                            is Int -> putInt(key, value)
                            is Float -> putFloat(key, value)
                            is String -> putString(key, value)
                            is Set<*> ->
                                @Suppress("UNCHECKED_CAST")
                                putStringSet(key, value.filterIsInstance<String>().toSet())
                        }
                    }
                }
            }
        }
    }
}

private fun SharedPreferences.toSupportedPreferences(): me.zhanghai.compose.preference.Preferences {
    val entries = try {
        all ?: emptyMap()
    } catch (_: Exception) {
        emptyMap()
    }
    return MapPreferences(
        entries.mapNotNull { (key, value) ->
            when (value) {
                is Boolean, is Int, is Float, is String -> key to value
                is Set<*> -> key to value.filterIsInstance<String>().toSet()
                else -> null
            }
        }.toMap()
    )
}

private inline fun SharedPreferences.edit(action: SharedPreferences.Editor.() -> Unit) {
    edit().apply {
        action()
        apply()
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview
@Composable
private fun SettingsScreenPreview() {
    DefaultScaffoldPreview {
        PreviewSettingsScreen()
    }
}

@Composable
private fun PreviewSettingsScreen() {
    val previewFlow = remember {
        MutableStateFlow<me.zhanghai.compose.preference.Preferences>(
            MapPreferences(
                mapOf(
                    Prefs.PASSCODE_ENABLED to false,
                    Prefs.UPLOAD_WIFI_ONLY to true,
                    Prefs.USE_TOR to false,
                    "pref_key_use_dark_mode" to false,
                )
            )
        )
    }
    ProvidePreferenceLocals(flow = previewFlow, theme = savePreferenceTheme()) {
        SettingsScreenContent(
            strings = SettingsStrings(
                titleSecure = "Secure",
                titleArchive = "Archive",
                titleVerify = "Verify",
                titleEncrypt = "Encrypt",
                titleGeneral = "General",
                titlePasscode = "Lock app with passcode",
                titleWifiOnly = "Only upload media when you are connected to Wi-Fi",
                titleMediaServers = "Media Servers",
                summaryMediaServers = "Add or remove media servers",
                titleMediaFolders = "Media Folders",
                summaryMediaFolders = "Manage your archived folders",
                titleC2pa = "C2PA Content Authenticity",
                summaryC2pa = "Generate cryptographic content credentials",
                titleTor = "Turn on Onion Routing",
                titleDarkMode = "Switch to dark mode",
                titleLanguage = "App language",
                summaryLanguage = "Change the language for this app",
                titleAbout = "Save by Open Archive",
                summaryAbout = "Discover the Save app",
                titlePrivacy = "Terms & Privacy Policy",
                summaryPrivacy = "Tap to view our Terms & Privacy Policy",
                titleVersion = "Version",
            ),
            appVersion = "0.0.0",
            passcodeState = rememberPreferenceState(key = Prefs.PASSCODE_ENABLED, defaultValue = false),
            wifiOnlyState = rememberPreferenceState(key = Prefs.UPLOAD_WIFI_ONLY, defaultValue = true),
            torChecked = false,
            torInteractive = true,
            torSummary = "Transfer via the Tor Network only",
            darkModeState = rememberPreferenceState(key = "pref_key_use_dark_mode", defaultValue = false),
            onPasscodeToggle = {},
            onWifiOnlyToggle = {},
            onTorToggle = {},
            onDarkModeToggle = {},
            onLanguageClick = {},
            onMediaServersClick = {},
            onMediaFoldersClick = {},
            onC2paClick = {},
            onAboutClick = {},
            onPrivacyClick = {},
            onNavigateToCache = {},
        )
    }
}
