package net.opendasharchive.openarchive.features.settings

import android.content.Intent
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.switchPreference
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.ComposeAppBar
import net.opendasharchive.openarchive.features.settings.passcode.components.DefaultScaffold

@Composable
fun ProofModeScreen(
    onNavigateBack: () -> Unit
) {

    SaveAppTheme {


        DefaultScaffold(
            topAppBar = {
                ComposeAppBar(
                    title = stringResource(R.string.proofmode),
                    onNavigationAction = {
                        onNavigateBack()
                    }
                )
            },

            ) {

            ProofModeScreenContent()
        }
    }
}

@Composable
fun ProofModeScreenContent() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Please allow all permissions", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        }
    }

    val useProofModeKeyEncryption = remember { mutableStateOf(false) }

    val spannedText: Spanned = HtmlCompat.fromHtml(
        stringResource(
            R.string.prefs_use_proofmode_description,
            "https://www.google.com"
        ), HtmlCompat.FROM_HTML_MODE_COMPACT
    )

    // AnnotatedString Builder
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

    ProvidePreferenceLocals {
        val useProofModeKey = stringResource(R.string.pref_key_use_proof_mode)

        LazyColumn(modifier = Modifier.fillMaxSize()) {


            switchPreference(
                key = useProofModeKey,
                defaultValue = false,
                enabled = {
                    true
                },
                rememberState = {
                    useProofModeKeyEncryption
                },
                title = { Text(stringResource(R.string.prefs_use_proofmode_title)) },
                summary = { Text(stringResource(R.string.prefs_use_proofmode_summary)) }
            )

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(annotatedString, fontSize = 11.sp)
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
        ProofModeScreenContent()
    }
}