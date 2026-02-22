package net.opendasharchive.openarchive.services.snowbird.presentation.qrscanner

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.components.QRScanner
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles
import net.opendasharchive.openarchive.features.core.ComposeAppBar
import net.opendasharchive.openarchive.services.snowbird.util.SnowbirdQRDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onQrCodeScanned: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Launcher for picking QR image from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(it)?.use { input ->
                            BitmapFactory.decodeStream(input)
                        }?.let { bitmap ->
                            SnowbirdQRDecoder.decodeFromBitmap(bitmap)
                        }
                    }.getOrNull()
                }

                if (!decoded.isNullOrBlank()) {
                    onQrCodeScanned(decoded)
                } else {
                    Toast.makeText(
                        context,
                        "No QR code found in selected image.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            ComposeAppBar(
                title = stringResource(R.string.scan_qr_code),
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { galleryLauncher.launch("image/*") }
                ) {
                    Text(
                        text = stringResource(R.string.open_from_gallery),
                        style = SaveTextStyles.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.qr_scanner_instruction),
                style = SaveTextStyles.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // QR Scanner - Large centered square
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                QRScanner(
                    modifier = Modifier.fillMaxSize(),
                    onQrCodeScanned = { result ->
                        onQrCodeScanned(result)
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1.5f))
        }
    }
}
