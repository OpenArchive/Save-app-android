package net.opendasharchive.openarchive.features.settings

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
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
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.ComposeAppBar
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold
import net.opendasharchive.openarchive.util.Hbks
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.ProofModeHelper
import java.util.UUID
import javax.crypto.SecretKey

@Composable
fun ProofModeSettingsScreen(
    onNavigateBack: () -> Unit
) {

    val context = LocalContext.current
    if (LocalInspectionMode.current) {
        PreviewProofModeScreen()
        return
    }

    SaveAppTheme {

        val sharedPreferences = remember {
            PreferenceManager.getDefaultSharedPreferences(context)
        }
        val preferenceFlow = remember(sharedPreferences) {
            createFilteredPreferenceFlow(sharedPreferences)
        }

        ProvidePreferenceLocals(flow = preferenceFlow, theme = savePreferenceTheme()) {

            val fragmentActivity = context as? FragmentActivity

            val useProofModeState = rememberPreferenceState(
                key = Prefs.USE_PROOFMODE,
                defaultValue = Prefs.getBoolean(Prefs.USE_PROOFMODE, false)
            )
            val keyEncryptionState = rememberPreferenceState(
                key = Prefs.USE_PROOFMODE_KEY_ENCRYPTION,
                defaultValue = Prefs.useProofModeKeyEncryption
            )


            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                val granted = result.values.all { it }
                if (granted) {
                    useProofModeState.value = true
                    Prefs.putBoolean(Prefs.USE_PROOFMODE, true)
                } else {
                    useProofModeState.value = false
                    Prefs.putBoolean(Prefs.USE_PROOFMODE, false)
                    Toast.makeText(
                        context,
                        R.string.phone_permission_required,
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }

            val enrollLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (enableProofModeKeyEncryption(fragmentActivity)) {
                        keyEncryptionState.value = true
                    } else {
                        keyEncryptionState.value = false
                    }
                }






            ProofModeSettingsScreenContent(
                useProofModeState = useProofModeState,
                onToggleProofMode = { isEnabled ->
                    if (isEnabled) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    } else {
                        useProofModeState.value = false
                        Prefs.putBoolean(Prefs.USE_PROOFMODE, false)
                    }
                },
                onOpenUrl = {

                },
                onNavigateBack = onNavigateBack
            )
        }
    }
}

@Composable
fun ProofModeSettingsScreenContent(
    useProofModeState: MutableState<Boolean>,
    onToggleProofMode: (Boolean) -> Unit,
    onOpenUrl: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {

    val spannedText: Spanned = HtmlCompat.fromHtml(
        stringResource(
            R.string.prefs_use_proofmode_description,
            "https://proofmode.org/"
        ), HtmlCompat.FROM_HTML_MODE_COMPACT
    )

    val annotatedString = buildAnnotatedString {
        append(spannedText.toString())
        spannedText.getSpans(0, spannedText.length, URLSpan::class.java)
            .forEach { urlSpan ->
                val start = spannedText.getSpanStart(urlSpan)
                val end = spannedText.getSpanEnd(urlSpan)
                addStringAnnotation(
                    tag = "URL",
                    annotation = urlSpan.url,
                    start = start,
                    end = end
                )
                addStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.tertiary,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = start,
                    end = end
                )
            }
    }

    DefaultScaffold(
        title = stringResource(R.string.proofmode),
        onNavigateBack = onNavigateBack
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {

            item(key = "use_proofmode_switch") {
                SwitchPreference(
                    title = { PreferenceTitle(stringResource(R.string.prefs_use_proofmode_title)) },
                    state = useProofModeState,
                    onToggle = onToggleProofMode,
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .padding(start = 16.dp, end = 32.dp)
                ) {
                    Text(
                        text = annotatedString,
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
                                Icons.Outlined.Info,
                                tint = MaterialTheme.colorScheme.error,
                                contentDescription = null
                            )
                            Text(
                                text = AnnotatedString.fromHtml(
                                    stringResource(R.string.proof_mode_warning_text),
                                    linkStyles = TextLinkStyles(
                                        style = SpanStyle(
                                            textDecoration = TextDecoration.Underline,
                                            fontStyle = FontStyle.Italic,
                                            color = Color.Blue
                                        )
                                    )
                                ),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }

}

@Preview
@Composable
private fun ProofModeScreenPreview() {
    DefaultScaffoldPreview {
        PreviewProofModeScreen()
    }
}

@Composable
private fun PreviewProofModeScreen() {

    val previewFlow = remember {
        MutableStateFlow<Preferences>(
            MapPreferences(
                mapOf(
                    Prefs.USE_PROOFMODE to false
                )
            )
        )
    }

    val useProofModeState = rememberPreferenceState(
        key = Prefs.USE_PROOFMODE,
        defaultValue = false
    )

    ProvidePreferenceLocals(
        flow = previewFlow,
        theme = savePreferenceTheme()
    ) {
        ProofModeSettingsScreenContent(
            useProofModeState = useProofModeState,
            onToggleProofMode = {},
            onOpenUrl = {}
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
