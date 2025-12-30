package net.opendasharchive.openarchive.features.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogStateManager
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupActivity
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import net.opendasharchive.openarchive.util.extensions.getVersionName
import org.koin.compose.koinInject
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import net.opendasharchive.openarchive.features.core.UiColor
import net.opendasharchive.openarchive.features.main.ui.AppRoute

@Composable
fun SettingsScreen(
    onNavigateToSpaceList: () -> Unit = {},
    onNavigateToArchivedFolders: () -> Unit = {},
    onNavigateToCache: () -> Unit = {},
    onNavigateToProofMode: () -> Unit = {},
) {
    val dialogManager: DialogStateManager = koinInject<DialogStateManager>()
    val context = LocalContext.current
    if (LocalInspectionMode.current) {
        PreviewSettingsScreen()
        return
    }

    val sharedPreferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val preferenceFlow = remember(sharedPreferences) { createFilteredPreferenceFlow(sharedPreferences) }
    val analyticsManager: AnalyticsManager = koinInject()
    val passcodeRepository: PasscodeRepository = koinInject()

    val coroutineScope = rememberCoroutineScope()
    val appVersion = remember { context.packageManager.getVersionName(context.packageName) }

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
                titleProofMode = stringResource(R.string.proofmode),
                titleTor = stringResource(R.string.prefs_use_tor_title),
                summaryTor = stringResource(R.string.prefs_use_tor_summary),
                titleDarkMode = stringResource(R.string.pref_title_dark_mode),
                titleAbout = stringResource(R.string.save_by_open_archive),
                summaryAbout = stringResource(R.string.discover_the_save_app),
                titlePrivacy = stringResource(R.string.pref_title_privacy_policy),
                summaryPrivacy = stringResource(R.string.pref_summary_privacy_policy),
                titleVersion = stringResource(R.string.pref_title_version),
            )

        val passcodeState = rememberPreferenceState(key = Prefs.PASSCODE_ENABLED, defaultValue = Prefs.passcodeEnabled)
        val wifiOnlyState = rememberPreferenceState(key = Prefs.UPLOAD_WIFI_ONLY, defaultValue = Prefs.uploadWifiOnly)
        val torState = rememberPreferenceState(key = Prefs.USE_TOR, defaultValue = Prefs.useTor)
        val darkModeKey = stringResource(R.string.pref_key_use_dark_mode)
        val darkModeState = rememberPreferenceState(key = darkModeKey, defaultValue = Prefs.getBoolean(darkModeKey, false))

        val passcodeLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val passcodeEnabled =
                    result.resultCode == Activity.RESULT_OK &&
                        (result.data?.getBooleanExtra(PasscodeSetupActivity.EXTRA_PASSCODE_ENABLED, false) ?: false)
                passcodeState.value = passcodeEnabled
                (context as? BaseActivity)?.updateScreenshotPrevention()
            }

        SettingsScreenContent(
            strings = settingStrings,
            appVersion = appVersion,
            passcodeState = passcodeState,
            wifiOnlyState = wifiOnlyState,
            torState = torState,
            darkModeState = darkModeState,
            onPasscodeToggle = { newValue ->
                if (newValue) {
                    passcodeState.value = true
                    passcodeLauncher.launch(Intent(context, PasscodeSetupActivity::class.java))
                } else {
                    // Keep UI state on until the dialog result decides.
                    passcodeState.value = true
                    dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                        type = DialogType.Warning
                        title = UiText.Resource(R.string.disable_passcode_dialog_title)
                        message = UiText.Resource(R.string.disable_passcode_dialog_msg)
                        positiveButton {
                            text = UiText.Resource(R.string.answer_yes)
                            action = {
                                passcodeRepository.clearPasscode()
                                (context as? BaseActivity)?.updateScreenshotPrevention()
                                passcodeState.value = false
                            }
                        }
                        neutralButton { action = { passcodeState.value = true } }
                    }
                }
            },
            onWifiOnlyToggle = { newValue ->
                wifiOnlyState.value = newValue
                Prefs.uploadWifiOnly = newValue
                AppLogger.breadcrumb("Feature Toggled", "wifi_only_upload: $newValue")
                coroutineScope.launch { analyticsManager.trackFeatureToggled("wifi_only_upload", newValue) }
            },
            onTorToggle = {
                dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                    type = DialogType.Info
                    iconColor = UiColor.Resource(R.color.colorTertiary)
                    title = UiText.Resource(R.string.tor_disabled_title)
                    message = UiText.Resource(R.string.tor_disabled_message)
                    positiveButton {
                        text = UiText.Resource(R.string.tor_download_btn_label)
                        action = { context.startActivity(Intent(Intent.ACTION_VIEW, Prefs.TOR_DOWNLOAD_URL)) }
                    }
                    neutralButton { text = UiText.Resource(android.R.string.cancel) }
                }
            },
            onDarkModeToggle = { enabled ->
                darkModeState.value = enabled
                Theme.set(if (enabled) Theme.DARK else Theme.LIGHT)
                Prefs.putBoolean(darkModeKey, enabled)
                AppLogger.breadcrumb("Feature Toggled", "dark_mode: $enabled")
                coroutineScope.launch { analyticsManager.trackFeatureToggled("dark_mode", enabled) }
            },
            onMediaServersClick = {
                onNavigateToSpaceList()
            },
            onMediaFoldersClick = {
                onNavigateToArchivedFolders()
            },
            onProofModeClick = {
                onNavigateToProofMode()
            },
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
    torState: MutableState<Boolean>,
    darkModeState: MutableState<Boolean>,
    onPasscodeToggle: (Boolean) -> Unit,
    onWifiOnlyToggle: (Boolean) -> Unit,
    onTorToggle: () -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onMediaServersClick: () -> Unit,
    onMediaFoldersClick: () -> Unit,
    onProofModeClick: () -> Unit,
    onAboutClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onNavigateToCache: () -> Unit,
) {
    val rowModifier = Modifier.fillMaxWidth()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Secure
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

        // Archive
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

        // Verify
        preferenceCategory(key = "category_verify", title = { PreferenceCategoryTitle(text = strings.titleVerify) })
        preference(
            key = "proof_mode",
            title = { PreferenceTitle(text = strings.titleProofMode, maxLines = 2) },
            modifier = rowModifier,
            onClick = onProofModeClick,
        )
        sectionDivider("divider_verify")

        // Encrypt
        preferenceCategory(key = "category_encrypt", title = { PreferenceCategoryTitle(text = strings.titleEncrypt) })
        item(key = "tor") {
            SwitchPreference(
                state = torState,
                title = { PreferenceTitle(text = strings.titleTor, maxLines = 2) },
                summary = { PreferenceSummary(text = strings.summaryTor) },
                modifier = rowModifier,
                onToggle = {
                    torState.value = false
                    onTorToggle()
                },
            )
        }
        sectionDivider("divider_encrypt")

        // General
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
    val titleProofMode: String,
    val titleTor: String,
    val summaryTor: String,
    val titleDarkMode: String,
    val titleAbout: String,
    val summaryAbout: String,
    val titlePrivacy: String,
    val summaryPrivacy: String,
    val titleVersion: String,
)

// Helper function for opening URLs
private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

@Composable
private fun PreferenceCategoryTitle(text: String) {
    Text(text = text, maxLines = 1, style = SaveTextStyles.titleLarge, color = colorResource(id = R.color.colorTertiary))
}

@Composable
fun PreferenceTitle(text: String, maxLines: Int = 1) {
    Text(
        text = text,
        maxLines = maxLines,
        style = SaveTextStyles.bodyLarge,
        color = colorResource(id = R.color.colorOnBackground)
    )
}

@Composable
private fun PreferenceSummary(text: String) {
    Text(text = text, maxLines = 1, style = SaveTextStyles.bodySmallEmphasis, color = colorResource(id = R.color.colorOnSurfaceVariant))
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = colorResource(id = R.color.colorDivider).copy(alpha = 0.5f)
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
                    checkedTrackColor = MaterialTheme.colorScheme.tertiary
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

@OptIn(DelicateCoroutinesApi::class)
private fun createFilteredPreferenceFlow(
    sharedPreferences: SharedPreferences
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
            strings =
                SettingsStrings(
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
                    titleProofMode = "Proof Mode",
                    titleTor = "Use Tor",
                    summaryTor = "Enable Tor for encryption",
                    titleDarkMode = "Switch to dark mode",
                    titleAbout = "Save by Open Archive",
                    summaryAbout = "Discover the Save app",
                    titlePrivacy = "Terms & Privacy Policy",
                    summaryPrivacy = "Tap to view our Terms & Privacy Policy",
                    titleVersion = "Version",
                ),
            appVersion = "0.0.0",
            passcodeState = rememberPreferenceState(key = Prefs.PASSCODE_ENABLED, defaultValue = false),
            wifiOnlyState = rememberPreferenceState(key = Prefs.UPLOAD_WIFI_ONLY, defaultValue = true),
            torState = rememberPreferenceState(key = Prefs.USE_TOR, defaultValue = false),
            darkModeState = rememberPreferenceState(key = "pref_key_use_dark_mode", defaultValue = false),
            onPasscodeToggle = {},
            onWifiOnlyToggle = {},
            onTorToggle = {},
            onDarkModeToggle = {},
            onMediaServersClick = {},
            onMediaFoldersClick = {},
            onProofModeClick = {},
            onAboutClick = {},
            onPrivacyClick = {},
            onNavigateToCache = {},
        )
    }
}
