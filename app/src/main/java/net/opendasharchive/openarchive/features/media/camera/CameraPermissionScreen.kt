package net.opendasharchive.openarchive.features.media.camera

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme

@Composable
fun CameraPermissionScreen(
    modifier: Modifier = Modifier,
    isCameraPermissionPermanentlyDenied: Boolean = false,
    isAudioPermissionPermanentlyDenied: Boolean = false,
    needsAudioPermission: Boolean = false,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (isCameraPermissionPermanentlyDenied)
                    stringResource(R.string.camera_access_blocked)
                else
                    stringResource(R.string.camera_permission_required),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = when {
                    isCameraPermissionPermanentlyDenied -> {
                        val audioText = if (needsAudioPermission && isAudioPermissionPermanentlyDenied)
                            stringResource(R.string.camera_and_microphone) else ""
                        stringResource(R.string.camera_access_permanently_denied, audioText)
                    }
                    needsAudioPermission -> {
                        stringResource(R.string.camera_microphone_permission_description)
                    }
                    else -> {
                        stringResource(R.string.camera_permission_description)
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            // Action buttons
            if (isCameraPermissionPermanentlyDenied) {
                // If permanently denied, show only Open Settings and Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                        )
                    ) {
                        Text(stringResource(R.string.lbl_Cancel), color = Color.White)
                    }
                    
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(stringResource(R.string.open_settings))
                        }
                    }
                }
            } else {
                // Normal permission request flow
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                        )
                    ) {
                        Text(stringResource(R.string.lbl_Cancel), color = Color.White)
                    }

                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.grant_permission),
                            textAlign = TextAlign.Center,
                            color = colorResource(R.color.black)
                        )
                    }
                }
            }
            
            // Show additional settings link only if not permanently denied
            if (!isCameraPermissionPermanentlyDenied) {
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(
                    onClick = onOpenSettings
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = stringResource(R.string.open_app_settings),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CameraPermissionScreenPreview() {
    SaveAppTheme {
        CameraPermissionScreen(
            isCameraPermissionPermanentlyDenied = false,
            needsAudioPermission = true,
            onRequestPermissions = {},
            onOpenSettings = {},
            onCancel = {}
        )
    }
}