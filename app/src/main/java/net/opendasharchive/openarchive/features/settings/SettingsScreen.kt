package net.opendasharchive.openarchive.features.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.switchPreference
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview

@Composable
fun SettingsScreen(
    onNavigateToCache: () -> Unit = {}
) {

    val context = LocalContext.current

    ProvidePreferenceLocals {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Secure Category
            preferenceCategory(title = { Text("Secure") }, key = "secure")

            switchPreference(
                key = "pref_app_passcode",
                defaultValue = false,
                title = { Text("Lock app with passcode") },
                summary = { Text("6 digit passcode") })

            // Archive Category
            preferenceCategory(title = { Text("Archive") }, key = "archive")
            preference(
                key = "pref_media_servers",
                title = { Text("Media Servers") },
                summary = { Text("Add or remove media servers") })
            preference(
                key = "pref_media_folders",
                title = { Text("Media Folders") },
                summary = { Text("Add or remove media folders") })
            preference(
                key = "pref_media_cache",
                title = { Text("Media Cache") },
                summary = { Text("View media cache") },
                onClick = {
                    onNavigateToCache()
                }
            )

            // Verify Category
            preferenceCategory(title = { Text("Verify") }, key = "verify")
            preference(
                key = "proof_mode", title = { Text("Proof Mode") })

            // Encrypt Category
            preferenceCategory(title = { Text("Encrypt") }, key = "encrypt")
            switchPreference(
                key = "use_tor",
                defaultValue = false,
                title = { Text("Use Tor") },
                summary = { Text("Enable Tor for encryption") })

            // General Category
            preferenceCategory(title = { Text("General") }, key = "general")
            switchPreference(
                key = "upload_wifi_only",
                defaultValue = false,
                title = { Text("Upload over Wi-Fi only") },
                summary = { Text("Only upload media when connected to Wi-Fi") })
            listPreference(
                key = "theme",
                title = { Text("Theme") },
                summary = { Text("Choose app theme") },
                values = listOf(
                    "light" to "Light", "dark" to "Dark", "system" to "System Default"
                ),
                defaultValue = "system"
            )

            // About Category
            preferenceCategory(title = { Text("About") }, key = "about")
            preference(
                key = "about_app",
                title = { Text("Save by Open Archive") },
                summary = { Text("Tap to view about Save App") },
                onClick = {
                    // Handle URL intent
                    openUrl(context, "https://open-archive.org/save")
                })
            preference(
                key = "privacy_policy",
                title = { Text("Terms & Privacy Policy") },
                summary = { Text("Tap to view our Terms & Privacy Policy") },
                onClick = {
                    // Handle URL intent
                    openUrl(context, "https://open-archive.org/privacy")
                })
            preference(
                key = "app_version",
                title = { Text("Version") },
                summary = { Text("0.7.2.4783") },
                enabled = false
            )
        }
    }
}

// Helper function for opening URLs

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}


@Preview
@Composable
private fun SettingsScreenPreview() {
    DefaultScaffoldPreview {

        SettingsScreen()
    }
}