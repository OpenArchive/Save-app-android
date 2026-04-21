package net.opendasharchive.openarchive.features.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import net.opendasharchive.openarchive.features.onboarding.components.HtmlText
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.core.ComposeAppBar
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold
import net.opendasharchive.openarchive.util.Prefs

@Composable
fun C2paScreen(
    onNavigateBack: () -> Unit
) {
    SaveAppTheme {
        DefaultScaffold(
            topAppBar = {
                ComposeAppBar(
                    title = stringResource(R.string.c2pa_content_authenticity),
                    onNavigateBack = {
                        onNavigateBack()
                    }
                )
            },
        ) {
            C2paScreenContent()
        }
    }
}

@Composable
fun C2paScreenContent() {
    val context = LocalContext.current

    var useC2pa by remember {
        mutableStateOf(Prefs.useC2pa)
    }

    val msgAutoDisabled = stringResource(R.string.c2pa_auto_disabled)
    val msgEnabled = stringResource(R.string.c2pa_enabled)
    val msgLocationDenied = stringResource(R.string.c2pa_location_permission_denied)

    // Check if location permission is granted
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Check permission when screen is shown
    LaunchedEffect(Unit) {
        if (useC2pa && !hasLocationPermission()) {
            // C2PA is enabled but permission is missing - auto-disable
            useC2pa = false
            Prefs.useC2pa = false
            Toast.makeText(context, msgAutoDisabled, Toast.LENGTH_LONG).show()
            AppLogger.w("[C2PA] Auto-disabled due to missing location permission")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val phoneStateGranted = results[Manifest.permission.READ_PHONE_STATE] == true

        if (locationGranted) {
            // Location is required; phone state is optional (enables cell tower data)
            useC2pa = true
            Prefs.useC2pa = true
            if (phoneStateGranted) {
                AppLogger.d("[C2PA] Enabled with location + cell tower data")
            } else {
                AppLogger.d("[C2PA] Enabled with location only (cell tower data unavailable)")
            }
            Toast.makeText(context, msgEnabled, Toast.LENGTH_SHORT).show()
        } else {
            // Location is required — disable C2PA if denied
            useC2pa = false
            Prefs.useC2pa = false
            Toast.makeText(context, msgLocationDenied, Toast.LENGTH_LONG).show()
            AppLogger.w("[C2PA] Disabled due to location permission denial")
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.prefs_use_c2pa_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.prefs_use_c2pa_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useC2pa,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                    ),
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (hasLocationPermission()) {
                                // Location already granted — enable immediately, then also
                                // request phone state if not yet granted (for cell tower data)
                                useC2pa = true
                                Prefs.useC2pa = true
                                AppLogger.d("[C2PA] Enabled (location already granted)")
                                if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.READ_PHONE_STATE
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(
                                        arrayOf(Manifest.permission.READ_PHONE_STATE)
                                    )
                                }
                            } else {
                                // Request location (required) + phone state (optional) together
                                AppLogger.d("[C2PA] Requesting location + phone state permissions")
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.READ_PHONE_STATE
                                    )
                                )
                            }
                        } else {
                            useC2pa = false
                            Prefs.useC2pa = false
                            AppLogger.d("[C2PA] Disabled by user")
                        }
                    }
                )
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                HtmlText(
                    textRes = R.string.prefs_use_c2pa_description,
                    linkRes = R.string.c2pa_learn_more_url,
                    fontSize = 11.sp,
                    linkColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp)
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
                                stringResource(R.string.c2pa_warning_text),
                                linkStyles = TextLinkStyles(
                                    style = SpanStyle(
                                        textDecoration = TextDecoration.Underline,
                                        fontStyle = FontStyle.Italic,
                                        color = Color.Blue
                                    )
                                )
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun C2paScreenPreview() {
    DefaultScaffoldPreview {
        C2paScreenContent()
    }
}
