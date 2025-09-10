package net.opendasharchive.openarchive.services.webdav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.util.extensions.openBrowser

@Composable
fun CreativeCommonsLicenseContent(
    modifier: Modifier = Modifier,
    licenseState: LicenseState,
    licenseCallbacks: LicenseCallbacks,
    enabled: Boolean = true,
    ccLabelText: String = ""
) {
    val context = LocalContext.current

    // Main container - matches LinearLayout in content_cc.xml
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Main CC License Switch - matches RelativeLayout lines 18-41 in content_cc.xml
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ccLabelText.ifEmpty { stringResource(R.string.set_creative_commons_license_for_all_folders_on_this_server) },
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.width(16.dp))
            
            Switch(
                checked = licenseState.ccEnabled,
                onCheckedChange = licenseCallbacks::onCcEnabledChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = MaterialTheme.colorScheme.tertiary
                )
            )
        }

        // Show license options only when CC is enabled
        if (licenseState.ccEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // CC0 License Switch - waive all restrictions, requirements, and attribution (first option)
            LicenseOptionRow(
                text = stringResource(R.string.info_license_cc0),
                checked = licenseState.cc0Enabled,
                onCheckedChange = licenseCallbacks::onCc0EnabledChange,
                enabled = enabled
            )
            
            // Allow Remix Switch - matches RelativeLayout lines 44-68 in content_cc.xml
            LicenseOptionRow(
                text = stringResource(R.string.info_license_deriv),
                checked = licenseState.allowRemix,
                onCheckedChange = licenseCallbacks::onAllowRemixChange,
                enabled = enabled
            )

            // Require Share Alike Switch - matches RelativeLayout lines 71-95 in content_cc.xml
            LicenseOptionRow(
                text = stringResource(R.string.info_license_sharealike),
                checked = licenseState.requireShareAlike,
                onCheckedChange = licenseCallbacks::onRequireShareAlikeChange,
                enabled = enabled && licenseState.allowRemix && licenseState.ccEnabled
            )

            // Allow Commercial Use Switch - matches RelativeLayout lines 98-122 in content_cc.xml
            LicenseOptionRow(
                text = stringResource(R.string.info_license_comm),
                checked = licenseState.allowCommercial,
                onCheckedChange = licenseCallbacks::onAllowCommercialChange,
                enabled = enabled
            )
        }

        // Show license URL when CC is enabled
        if (licenseState.ccEnabled) {
            Spacer(modifier = Modifier.height(32.dp))

            // License URL - matches TextView lines 132-138 in content_cc.xml  
            licenseState.licenseUrl?.let { url ->
                Text(
                    text = url,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { context.openBrowser(url) }
                        .padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Learn More Link - matches TextView lines 140-147 in content_cc.xml
        Text(
            text = stringResource(R.string.learn_more_about_creative_commons),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.tertiary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { context.openBrowser("https://creativecommons.org/about/cclicenses/") }
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun LicenseOptionRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.tertiary
            )
        )
    }
}

@Preview(showBackground = true, name = "CC Collapsed (No License)")
@Composable
fun CreativeCommonsLicenseContentPreview() {
    SaveAppTheme {
        CreativeCommonsLicenseContent(
            licenseState = LicenseState(
                ccEnabled = false,
                allowRemix = true,
                requireShareAlike = false,
                allowCommercial = false,
                licenseUrl = null
            ),
            licenseCallbacks = object : LicenseCallbacks {
                override fun onCcEnabledChange(enabled: Boolean) {}
                override fun onAllowRemixChange(allowed: Boolean) {}
                override fun onRequireShareAlikeChange(required: Boolean) {}
                override fun onAllowCommercialChange(allowed: Boolean) {}
                override fun onCc0EnabledChange(enabled: Boolean) {}
            },
            enabled = true,
        )
    }
}

@Preview(showBackground = true, name = "CC Expanded with License")
@Composable
fun CreativeCommonsLicenseContentWithLicensePreview() {
    SaveAppTheme {
        CreativeCommonsLicenseContent(
            licenseState = LicenseState(
                ccEnabled = true,
                allowRemix = true,
                requireShareAlike = true,
                allowCommercial = false,
                licenseUrl = "https://creativecommons.org/licenses/by-nc-sa/4.0/"
            ),
            licenseCallbacks = object : LicenseCallbacks {
                override fun onCcEnabledChange(enabled: Boolean) {}
                override fun onAllowRemixChange(allowed: Boolean) {}
                override fun onRequireShareAlikeChange(required: Boolean) {}
                override fun onAllowCommercialChange(allowed: Boolean) {}
                override fun onCc0EnabledChange(enabled: Boolean) {}
            },
            enabled = true,
        )
    }
}