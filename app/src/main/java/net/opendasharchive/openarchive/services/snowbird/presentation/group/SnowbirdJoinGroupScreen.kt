package net.opendasharchive.openarchive.services.snowbird.presentation.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.components.QRScanner
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.extensions.getQueryParameter
import net.opendasharchive.openarchive.services.internetarchive.presentation.login.CustomTextField
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupAction
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupState
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.presentation.qrscanner.QRScannerScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun SnowbirdJoinGroupScreen(
    initialUri: String,
    onScanQr: () -> Unit,
    onCancel: () -> Unit,
    viewModel: SnowbirdGroupViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SnowbirdJoinGroupScreenContent(
        state = state,
        initialUri = initialUri,
        onAction = viewModel::onAction,
        onScanQr = onScanQr,
        onCancel = onCancel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnowbirdJoinGroupScreenContent(
    state: SnowbirdGroupState,
    initialUri: String,
    onAction: (SnowbirdGroupAction) -> Unit,
    onScanQr: () -> Unit,
    onCancel: () -> Unit
) {
    var uriString by remember { mutableStateOf(initialUri) }
    var repoName by remember { mutableStateOf("") }
    var scannedGroupName by remember { mutableStateOf(initialUri.getQueryParameter("name") ?: "") }
    val focusManager = LocalFocusManager.current
    val repoFocusRequester = remember { FocusRequester() }

    // Update local state when scannedUri changes in ViewModel state
    LaunchedEffect(state.scannedUri) {
        if (state.scannedUri.isNotBlank()) {
            uriString = state.scannedUri
            scannedGroupName = state.scannedUri.getQueryParameter("name") ?: ""
        }
    }

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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dweb_join_group_screen_title),
                        style = SaveTextStyles.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back_ios),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = stringResource(R.string.qr_scanner_instruction),
                    style = SaveTextStyles.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // QR Scanner - Constrained window
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    QRScanner(
                        modifier = Modifier.fillMaxSize(),
                        onQrCodeScanned = { result ->
                            uriString = result
                            scannedGroupName = result.getQueryParameter("name") ?: "Scanned Group"
                        }
                    )
                }

                if (scannedGroupName.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.joining_group_label, scannedGroupName),
                        style = SaveTextStyles.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                CustomTextField(
                    value = uriString,
                    onValueChange = { 
                        uriString = it
                        scannedGroupName = it.getQueryParameter("name") ?: ""
                    },
                    placeholder = stringResource(R.string.dweb_join_group_group_name),
                    isLoading = state.isLoading,
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                    onImeAction = { repoFocusRequester.requestFocus() }
                )

                Spacer(modifier = Modifier.height(24.dp))

                CustomTextField(
                    value = repoName,
                    onValueChange = { repoName = it },
                    placeholder = stringResource(R.string.dweb_join_group_repo_name),
                    isLoading = state.isLoading,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    onImeAction = { focusManager.clearFocus() },
                    modifier = Modifier.focusRequester(repoFocusRequester)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.dweb_join_group_screen_description),
                    style = SaveTextStyles.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                TextButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    enabled = !state.isLoading
                ) {
                    Text(
                        text = stringResource(R.string.open_from_gallery),
                        style = SaveTextStyles.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(120.dp)) // Padding for bottom button bar
            }

            // Bottom Action Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .height(ThemeDimensions.touchable)
                            .weight(1f),
                        enabled = !state.isLoading && uriString.isNotBlank() && repoName.isNotBlank(),
                        shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            disabledContainerColor = colorResource(R.color.grey_50),
                            disabledContentColor = colorResource(R.color.black),
                            contentColor = colorResource(R.color.black)
                        ),
                        onClick = { onAction(SnowbirdGroupAction.JoinGroupWithRepo(uriString, repoName)) }
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        } else {
                            Text(
                                stringResource(R.string.next),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SnowbirdJoinGroupScreenPreview() {
    SaveAppTheme {
        SnowbirdJoinGroupScreenContent(
            state = SnowbirdGroupState(),
            initialUri = "",
            onAction = {},
            onScanQr = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SnowbirdJoinGroupScreenLoadingPreview() {
    DefaultScaffoldPreview {
        SnowbirdJoinGroupScreenContent(
            state = SnowbirdGroupState(isLoading = true),
            initialUri = "save-veilid://join?name=TestGroup",
            onAction = {},
            onScanQr = {},
            onCancel = {}
        )
    }
}
