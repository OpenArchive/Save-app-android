package net.opendasharchive.openarchive.features.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.Spanned
import android.text.style.URLSpan
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.Preferences
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.rememberPreferenceState
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.util.Hbks
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.ProofModeHelper
import java.util.UUID
import javax.crypto.SecretKey

@Composable
fun ProofModeSettingsScreen(
    viewModel: ProofModeSettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        viewModel.onPermissionsResult(granted)
        if (!granted) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    val enrollLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val fragmentActivity = context as? FragmentActivity
        val success = enableProofModeKeyEncryption(fragmentActivity)
        viewModel.setKeyEncryptionEnabled(success)
    }

    LaunchedEffect(Unit) {
        viewModel.setBiometricTitle(
            when (Hbks.biometryType(context)) {
                Hbks.BiometryType.StrongBiometry -> R.string.prefs_proofmode_key_encryption_title_biometrics
                Hbks.BiometryType.DeviceCredential -> R.string.prefs_proofmode_key_encryption_title_passcode
                else -> R.string.prefs_proofmode_key_encryption_title_all
            }
        )

        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProofModeSettingsEvent.ShowToast -> {
                    Toast.makeText(context, event.messageResId, Toast.LENGTH_LONG).show()
                }

                is ProofModeSettingsEvent.RequestPermissions -> {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }

                is ProofModeSettingsEvent.EnrollBiometrics -> {
                    val availability = Hbks.deviceAvailablity(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && availability is Hbks.Availability.Enroll) {
                        enrollLauncher.launch(Hbks.enrollIntent(availability.type))
                    } else {
                        val fragmentActivity = context as? FragmentActivity
                        val success = enableProofModeKeyEncryption(fragmentActivity)
                        viewModel.setKeyEncryptionEnabled(success)
                    }
                }

                is ProofModeSettingsEvent.RestartApp -> {
                    (context as? Activity)?.let {
                        ProofModeHelper.restartApp(it)
                    }
                }
            }
        }
    }

    val sharedPreferences = remember {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    val preferenceFlow = remember(sharedPreferences) {
        createFilteredPreferenceFlow(sharedPreferences)
    }

    ProvidePreferenceLocals(flow = preferenceFlow, theme = savePreferenceTheme()) {
        ProofModeSettingsContent(
            state = state,
            onAction = { action ->
                if (action is ProofModeSettingsAction.OpenUrl) {
                    uriHandler.openUri(action.url)
                } else {
                    viewModel.onAction(action)
                }
            }
        )
    }
}

@Composable
fun ProofModeSettingsContent(
    state: ProofModeSettingsUiState,
    onAction: (ProofModeSettingsAction) -> Unit
) {
    val useProofModeState = rememberPreferenceState(
        key = Prefs.USE_PROOFMODE,
        defaultValue = state.isProofModeEnabled
    )
    val keyEncryptionState = rememberPreferenceState(
        key = Prefs.USE_PROOFMODE_KEY_ENCRYPTION,
        defaultValue = state.isKeyEncryptionEnabled
    )

    val spanStyles = TextLinkStyles(
        style = SpanStyle(
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.tertiary
        )
    )

    val description = AnnotatedString.fromHtml(
        htmlString = stringResource(
            R.string.prefs_use_proofmode_description,
            "https://proofmode.org/"
        ),
        linkStyles = spanStyles
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp)
    ) {
        item(key = "use_proofmode_switch") {
            SwitchPreference(
                title = { PreferenceTitle(stringResource(R.string.prefs_use_proofmode_title)) },
                state = useProofModeState,
                onToggle = { onAction(ProofModeSettingsAction.ToggleProofMode(it)) },
            )
        }

        // Hidden for now as requested
        val showKeyEncryption = false
        if (showKeyEncryption) {
            item(key = "key_encryption_switch") {
                SwitchPreference(
                    title = { PreferenceTitle(stringResource(state.biometricTitleResId)) },
                    state = keyEncryptionState,
                    onToggle = { onAction(ProofModeSettingsAction.ToggleKeyEncryption(it)) },
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .padding(start = 16.dp, end = 32.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.splashBackground)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info_outline),
                            tint = MaterialTheme.colorScheme.error,
                            contentDescription = null
                        )
                        Text(
                            text = AnnotatedString.fromHtml(
                                stringResource(R.string.proof_mode_warning_text),
                                linkStyles = spanStyles
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ProofModeScreenPreview() {
    DefaultScaffoldPreview { PreviewProofModeScreen() }
}

@Composable
private fun PreviewProofModeScreen() {
    val previewFlow = remember {
        MutableStateFlow<Preferences>(
            MapPreferences(
                mapOf(
                    Prefs.USE_PROOFMODE to false,
                    Prefs.USE_PROOFMODE_KEY_ENCRYPTION to false
                )
            )
        )
    }

    ProvidePreferenceLocals(
        flow = previewFlow,
        theme = savePreferenceTheme()
    ) {
        ProofModeSettingsContent(
            state = ProofModeSettingsUiState(),
            onAction = {}
        )
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun createFilteredPreferenceFlow(
    sharedPreferences: SharedPreferences
): MutableStateFlow<Preferences> {
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

private fun SharedPreferences.toSupportedPreferences(): Preferences {
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

private fun enableProofModeKeyEncryption(activity: FragmentActivity?): Boolean {
    if (activity == null) return false
    val key = Hbks.loadKey() ?: Hbks.createKey()
    if (key != null && Prefs.proofModeEncryptedPassphrase == null) {
        createPassphrase(key, activity) { passphrase ->
            if (passphrase != null) {
                ProofModeHelper.removePgpKey(activity)
                ProofModeHelper.restartApp(activity)
            } else {
                Hbks.removeKey()
            }
        }
        return true
    }
    return Prefs.proofModeEncryptedPassphrase != null
}

private fun createPassphrase(
    key: SecretKey,
    activity: FragmentActivity,
    completed: (passphrase: String?) -> Unit
) {
    val passphrase = UUID.randomUUID().toString()
    Hbks.encrypt(passphrase, key, activity) { ciphertext, _ ->
        if (ciphertext == null) {
            return@encrypt completed(null)
        }
        Prefs.proofModeEncryptedPassphrase = ciphertext
        Hbks.decrypt(Prefs.proofModeEncryptedPassphrase, key, activity) { decrypted, _ ->
            if (decrypted == null || decrypted != passphrase) {
                Prefs.proofModeEncryptedPassphrase = null
                return@decrypt completed(null)
            }
            completed(passphrase)
        }
    }
}
