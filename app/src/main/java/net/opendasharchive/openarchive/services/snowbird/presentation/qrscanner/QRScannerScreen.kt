package net.opendasharchive.openarchive.services.snowbird.presentation.qrscanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.components.QRScanner
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles
import net.opendasharchive.openarchive.extensions.getQueryParameter
import net.opendasharchive.openarchive.features.core.ComposeAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onQrCodeScanned: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var scannedGroupName by remember { mutableStateOf("") }
    
    // Launcher for picking QR image from gallery
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            // TODO: Process image URI to decode QR code
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
