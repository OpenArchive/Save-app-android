package net.opendasharchive.openarchive.features.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.core.UiText
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
import org.koin.androidx.compose.koinViewModel
import me.zhanghai.compose.preference.LocalPreferenceTheme
import net.opendasharchive.openarchive.core.logger.AppLogger
import org.koin.compose.koinInject

@Composable
@Suppress("UnusedParameter")
fun SettingsScreen(
    onNavigateToCache: () -> Unit = {}
) {
    val context = LocalContext.current
    if (LocalInspectionMode.current) {
        SettingsScreenPreviewContent()
        return
    }
    val sharedPreferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val preferenceFlow = remember(sharedPreferences) {
        createFilteredPreferenceFlow(sharedPreferences)
    }
    val analyticsManager: AnalyticsManager = koinInject()
    val passcodeRepository: PasscodeRepository = koinInject()
    val dialogManager: DialogStateManager = koinViewModel()
    val coroutineScope = rememberCoroutineScope()
    val packageManager = context.packageManager
    val appVersion = remember { packageManager.getVersionName(context.packageName) }

    ProvidePreferenceLocals(
        flow = preferenceFlow,
        theme = settingsPreferenceTheme()
    ) {
        val passcodeKey = stringResource(R.string.pref_app_passcode)
        val wifiOnlyKey = Prefs.UPLOAD_WIFI_ONLY
        val mediaServersKey = stringResource(R.string.pref_media_servers)
        val mediaFoldersKey = stringResource(R.string.pref_media_folders)
        val proofModeKey = stringResource(R.string.pref_key_proof_mode)
        val torKey = stringResource(R.string.pref_key_use_tor)
        val darkModeKey = stringResource(R.string.pref_key_use_dark_mode)
        val aboutKey = stringResource(R.string.pref_key_about_app)
        val privacyKey = stringResource(R.string.pref_key_privacy_policy)
        val appVersionKey = stringResource(R.string.pref_key_app_version)

        val passcodeState = rememberPreferenceState(key = passcodeKey, defaultValue = Prefs.passcodeEnabled)
        val wifiOnlyState = rememberPreferenceState(key = wifiOnlyKey, defaultValue = Prefs.uploadWifiOnly)
        val torState = rememberPreferenceState(key = torKey, defaultValue = Prefs.useTor)
        val darkModeState = rememberPreferenceState(key = darkModeKey, defaultValue = Prefs.getBoolean(darkModeKey, false))
        val itemModifier = Modifier.fillMaxWidth()

        val passcodeLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val passcodeEnabled =
                    result.resultCode == Activity.RESULT_OK &&
                        (result.data?.getBooleanExtra(PasscodeSetupActivity.EXTRA_PASSCODE_ENABLED, false) ?: false)
                passcodeState.value = passcodeEnabled
                (context as? BaseActivity)?.updateScreenshotPrevention()
            }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Secure
            preferenceCategory(
                key = "category_secure",
                title = { PreferenceCategoryTitle(text = stringResource(R.string.pref_title_secure)) }
            )

            item(key = passcodeKey) {
                whiteThumbSwitchPreference(
                    key = passcodeKey,
                    state = passcodeState,
                    title = { PreferenceTitle(text = stringResource(R.string.passcode_lock_app), maxLines = 2) },
                    modifier = itemModifier,
                    onToggle = { newValue ->
                        if (newValue) {
                            passcodeLauncher.launch(Intent(context, PasscodeSetupActivity::class.java))
                        } else {
                            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                                type = DialogType.Warning
                                title = UiText.StringResource(R.string.disable_passcode_dialog_title)
                                message = UiText.StringResource(R.string.disable_passcode_dialog_msg)
                                positiveButton {
                                    text = UiText.StringResource(R.string.answer_yes)
                                    action = {
                                        passcodeRepository.clearPasscode()
                                        passcodeState.value = false
                                        (context as? BaseActivity)?.updateScreenshotPrevention()
                                    }
                                }
                                neutralButton {
                                    action = { passcodeState.value = true }
                                }
                            }
                        }
                    }
                )
            }
            
            item(key = "divider_secure") { SettingsDivider() }

            // Archive
            preferenceCategory(
                key = "category_archive",
                title = { PreferenceCategoryTitle(text = stringResource(R.string.pref_title_archive)) }
            )

            item(key = wifiOnlyKey) {
                whiteThumbSwitchPreference(
                    key = wifiOnlyKey,
                    state = wifiOnlyState,
                    title = {
                        PreferenceTitle(
                            text = stringResource(R.string.only_upload_media_when_you_are_connected_to_wi_fi),
                            maxLines = 2
                        )
                    },
                    modifier = itemModifier,
                ) { newValue ->
                    wifiOnlyState.value = newValue
                    Prefs.uploadWifiOnly = newValue
                    AppLogger.breadcrumb("Feature Toggled", "wifi_only_upload: $newValue")
                    coroutineScope.launch {
                        analyticsManager.trackFeatureToggled("wifi_only_upload", newValue)
                    }
                }
            }

            preference(
                key = mediaServersKey,
                title = { PreferenceTitle(text = stringResource(R.string.pref_title_media_servers)) },
                summary = { PreferenceSummary(text = stringResource(R.string.pref_summary_media_servers)) },
                modifier = itemModifier,
                onClick = {
                    val intent = Intent(context, SpaceSetupActivity::class.java)
                    intent.putExtra(SpaceSetupActivity.LABEL_START_DESTINATION, StartDestination.SPACE_LIST.name)
                    context.startActivity(intent)
                }
            )

            preference(
                key = mediaFoldersKey,
                title = { PreferenceTitle(text = stringResource(R.string.pref_title_media_folders)) },
                summary = { PreferenceSummary(text = stringResource(R.string.pref_summary_media_folders)) },
                modifier = itemModifier,
                onClick = {
                    val intent = Intent(context, SpaceSetupActivity::class.java)
                    intent.putExtra(SpaceSetupActivity.LABEL_START_DESTINATION, StartDestination.ARCHIVED_FOLDER_LIST.name)
                    intent.putExtra(FoldersFragment.EXTRA_SHOW_ARCHIVED, true)
                    context.startActivity(intent)
                }
            )
            item(key = "divider_archive") { SettingsDivider() }

            // Verify
            preferenceCategory(
                key = "category_verify",
                title = { PreferenceCategoryTitle(text = stringResource(R.string.intro_header_verify)) }
            )
            preference(
                key = proofModeKey,
                title = { PreferenceTitle(text = stringResource(R.string.proofmode), maxLines = 2) },
                modifier = itemModifier,
                onClick = { context.startActivity(Intent(context, ProofModeSettingsActivity::class.java)) }
            )
            item(key = "divider_verify") { SettingsDivider() }

            // Encrypt
            preferenceCategory(
                key = "category_encrypt",
                title = { PreferenceCategoryTitle(text = stringResource(R.string.intro_header_encrypt)) }
            )
            item(key = torKey) {
                whiteThumbSwitchPreference(
                    key = torKey,
                    state = torState,
                    title = { PreferenceTitle(text = stringResource(R.string.prefs_use_tor_title), maxLines = 2) },
                    summary = { PreferenceSummary(text = stringResource(R.string.prefs_use_tor_summary)) },
                    enabled = true,
                    modifier = itemModifier
                ) {
                    dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                        type = DialogType.Info
                        iconColor = dialogManager.requireResourceProvider().getColor(R.color.colorTertiary)
                        title = UiText.StringResource(R.string.tor_disabled_title)
                        message = UiText.StringResource(R.string.tor_disabled_message)
                        positiveButton {
                            text = UiText.StringResource(R.string.tor_download_btn_label)
                            action = {
                                val intent = Intent(Intent.ACTION_VIEW, Prefs.TOR_DOWNLOAD_URL)
                                context.startActivity(intent)
                            }
                        }
                        neutralButton {
                            text = UiText.StringResource(android.R.string.cancel)
                        }
                    }
                }
            }
            item(key = "divider_encrypt") { SettingsDivider() }

            // General
            preferenceCategory(
                key = "category_general",
                title = { PreferenceCategoryTitle(text = stringResource(R.string.general)) }
            )
            item(key = darkModeKey) {
                whiteThumbSwitchPreference(
                    key = darkModeKey,
                    state = darkModeState,
                    title = { PreferenceTitle(text = "Switch to dark mode") },
                    modifier = itemModifier,
                ) { enabled ->
                    darkModeState.value = enabled
                    val theme = if (enabled) Theme.DARK else Theme.LIGHT
                    Theme.set(theme)
                    Prefs.putBoolean(darkModeKey, enabled)
                    AppLogger.breadcrumb("Feature Toggled", "dark_mode: $enabled")
                    coroutineScope.launch {
                        analyticsManager.trackFeatureToggled("dark_mode", enabled)
                    }
                }
            }

            preference(
                key = aboutKey,
                title = { PreferenceTitle(text = stringResource(R.string.save_by_open_archive)) },
                summary = { PreferenceSummary(text = stringResource(R.string.discover_the_save_app)) },
                modifier = itemModifier,
                onClick = { openUrl(context, "https://open-archive.org/save") }
            )
            preference(
                key = privacyKey,
                title = { PreferenceTitle(text = stringResource(R.string.pref_title_privacy_policy)) },
                summary = { PreferenceSummary(text = stringResource(R.string.pref_summary_privacy_policy)) },
                modifier = itemModifier,
                onClick = { openUrl(context, "https://open-archive.org/privacy") }
            )
            preference(
                key = appVersionKey,
                title = { PreferenceTitle(text = stringResource(R.string.pref_title_version)) },
                summary = { PreferenceSummary(text = appVersion) },
                modifier = itemModifier,
            )
        }
    }
}

// Helper function for opening URLs
private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

@Composable
private fun PreferenceCategoryTitle(text: String) {
    Text(
        text = text,
        maxLines = 1
    )
}

@Composable
private fun PreferenceTitle(text: String, maxLines: Int = 1) {
    Text(text = text, maxLines = maxLines)
}

@Composable
private fun PreferenceSummary(text: String) {
    Text(text = text, maxLines = 1)
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth(),
        thickness = 0.5.dp,
        color = colorResource(id = R.color.colorDivider).copy(alpha = 0.5f)
    )
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun whiteThumbSwitchPreference(
    key: String,
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
            val theme = LocalPreferenceTheme.current
            val onTrack = colorResource(id = R.color.colorTertiary)
            val offTrack = colorResource(id = R.color.c23_grey_30)
            val offThumb = colorResource(id = R.color.c23_medium_grey)
            Switch(
                checked = value,
                onCheckedChange = { onToggle(it) },
                enabled = enabled,
                modifier = Modifier.padding(start = theme.horizontalSpacing, end = 8.dp),
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        uncheckedThumbColor = offThumb,
                        checkedTrackColor = onTrack,
                        uncheckedTrackColor = offTrack,
                        checkedBorderColor = onTrack,
                        uncheckedBorderColor = offTrack,
                    )
            )
        }
    )
}

@Composable
private fun settingsPreferenceTheme(): PreferenceTheme {
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
        SettingsScreenPreviewContent()
    }
}

@Composable
private fun SettingsScreenPreviewContent() {
    val context = LocalContext.current
    val previewFlow = remember {
        MutableStateFlow<me.zhanghai.compose.preference.Preferences>(
            MapPreferences(
                mapOf(
                    context.getString(R.string.pref_app_passcode) to false,
                    Prefs.UPLOAD_WIFI_ONLY to true,
                    context.getString(R.string.pref_key_proof_mode) to false,
                    context.getString(R.string.pref_key_use_tor) to false,
                    context.getString(R.string.pref_key_use_dark_mode) to false,
                    context.getString(R.string.pref_media_servers) to "",
                    context.getString(R.string.pref_media_folders) to "",
                    context.getString(R.string.pref_key_about_app) to "",
                    context.getString(R.string.pref_key_privacy_policy) to "",
                    context.getString(R.string.pref_key_app_version) to ""
                )
            )
        )
    }
    ProvidePreferenceLocals(
        flow = previewFlow,
        theme = settingsPreferenceTheme()
    ) {
        val passcodeState = rememberPreferenceState(key = stringResource(R.string.pref_app_passcode), defaultValue = false)
        val wifiOnlyState = rememberPreferenceState(key = Prefs.UPLOAD_WIFI_ONLY, defaultValue = true)
        val torState = rememberPreferenceState(key = stringResource(R.string.pref_key_use_tor), defaultValue = false)
        val darkModeState = rememberPreferenceState(key = stringResource(R.string.pref_key_use_dark_mode), defaultValue = false)
        val itemModifier = Modifier.fillMaxWidth()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Secure
            preferenceCategory(
                key = "category_secure_preview",
                title = { PreferenceCategoryTitle(text = stringResource(R.string.pref_title_secure)) }
            )
            item(key = "passcode_preview") {
                whiteThumbSwitchPreference(
                    key = "passcode_preview",
                    state = passcodeState,
                    title = { PreferenceTitle(text = stringResource(R.string.passcode_lock_app), maxLines = 2) },
                    modifier = itemModifier,
                )
            }
            item(key = "divider_secure_preview") { SettingsDivider() }

            // Archive
            preferenceCategory(
                key = "category_archive_preview",
                title = { PreferenceCategoryTitle(text = stringResource(R.string.pref_title_archive)) }
            )
            item(key = "wifi_only_preview") {
                whiteThumbSwitchPreference(
                    key = "wifi_only_preview",
                    state = wifiOnlyState,
                    title = {
                        PreferenceTitle(
                            text = stringResource(R.string.only_upload_media_when_you_are_connected_to_wi_fi),
                            maxLines = 2
                        )
                    },
                    modifier = itemModifier,
                )
            }
            preference(
                key = "media_servers_preview",
                title = { PreferenceTitle(text = stringResource(R.string.pref_title_media_servers)) },
                summary = { PreferenceSummary(text = stringResource(R.string.pref_summary_media_servers)) },
                modifier = itemModifier,
                onClick = {}
            )
            preference(
                key = "media_folders_preview",
                title = { PreferenceTitle(text = stringResource(R.string.pref_title_media_folders)) },
                summary = { PreferenceSummary(text = stringResource(R.string.pref_summary_media_folders)) },
                modifier = itemModifier,
                onClick = {}
            )
            item(key = "divider_archive_preview") { SettingsDivider() }

            // Verify
            preferenceCategory(
                key = "category_verify_preview",
                title = { PreferenceCategoryTitle(text = stringResource(R.string.intro_header_verify)) }
            )
            preference(
                key = "proof_mode_preview",
                title = { PreferenceTitle(text = stringResource(R.string.proofmode), maxLines = 2) },
                modifier = itemModifier,
                onClick = {}
            )
            item(key = "divider_verify_preview") { SettingsDivider() }

            // Encrypt
            preferenceCategory(
                key = "category_encrypt_preview",
                title = { PreferenceCategoryTitle(text = stringResource(R.string.intro_header_encrypt)) }
            )
            item(key = "tor_preview") {
                whiteThumbSwitchPreference(
                    key = "tor_preview",
                    state = torState,
                    title = { PreferenceTitle(text = stringResource(R.string.prefs_use_tor_title), maxLines = 2) },
                    summary = { PreferenceSummary(text = stringResource(R.string.prefs_use_tor_summary)) },
                    modifier = itemModifier,
                )
            }
            item(key = "divider_encrypt_preview") { SettingsDivider() }

            // General
            preferenceCategory(
                key = "category_general_preview",
                title = { PreferenceCategoryTitle(text = stringResource(R.string.general)) }
            )
            item(key = "dark_mode_preview") {
                whiteThumbSwitchPreference(
                    key = "dark_mode_preview",
                    state = darkModeState,
                    title = { PreferenceTitle(text = "Switch to dark mode") },
                    modifier = itemModifier,
                )
            }
            preference(
                key = "about_preview",
                title = { PreferenceTitle(text = stringResource(R.string.save_by_open_archive)) },
                summary = { PreferenceSummary(text = stringResource(R.string.discover_the_save_app)) },
                modifier = itemModifier,
                onClick = {}
            )
            preference(
                key = "privacy_preview",
                title = { PreferenceTitle(text = stringResource(R.string.pref_title_privacy_policy)) },
                summary = { PreferenceSummary(text = stringResource(R.string.pref_summary_privacy_policy)) },
                modifier = itemModifier,
                onClick = {}
            )
            preference(
                key = "version_preview",
                title = { PreferenceTitle(text = stringResource(R.string.pref_title_version)) },
                summary = { PreferenceSummary(text = "0.0.0") },
                modifier = itemModifier,
                enabled = false
            )
        }
    }
}
