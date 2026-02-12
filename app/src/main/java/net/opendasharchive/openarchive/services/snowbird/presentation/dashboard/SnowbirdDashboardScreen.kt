package net.opendasharchive.openarchive.services.snowbird.presentation.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.navigation.ResultEffect
import net.opendasharchive.openarchive.core.navigation.ResultEventBus
import net.opendasharchive.openarchive.core.presentation.components.LoadingOverlay
import net.opendasharchive.openarchive.core.presentation.theme.DefaultBoxPreview
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.PreviewLight
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles
import net.opendasharchive.openarchive.core.presentation.theme.ThemeColors
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerSheet
import net.opendasharchive.openarchive.services.snowbird.service.ServiceStatus

@Composable
fun SnowbirdDashboardScreen(
    viewModel: SnowbirdDashboardViewModel,
    resultBus: ResultEventBus
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 1. Gallery Launcher for QR code images
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.onAction(SnowbirdDashboardAction.ImagePickedForQR(it, context))
        }
    }

    // 2. File Launcher for QR code images
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.onAction(SnowbirdDashboardAction.ImagePickedForQR(it, context))
        }
    }

    // 3. Listen for results from QR Scanner screen
    ResultEffect<String>(resultKey = "qr_scan_result", resultBus = resultBus) { result ->
        viewModel.onAction(SnowbirdDashboardAction.QRResultScanned(result))
    }

    // 4. Handle ViewModel events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SnowbirdDashboardEvent.LaunchPicker -> {
                    when (event.type) {
                        AddMediaType.GALLERY -> {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }

                        AddMediaType.FILES -> {
                            fileLauncher.launch(arrayOf("image/*"))
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    SnowbirdDashboardContent(
        state = state,
        onAction = viewModel::onAction
    )

}

@Composable
fun SnowbirdDashboardContent(
    state: SnowbirdDashboardState,
    onAction: (SnowbirdDashboardAction) -> Unit
) {

    if (state.showContentPicker) {
        ContentPickerSheet(
            title = "Scan QR Code",
            onDismiss = {
                onAction(SnowbirdDashboardAction.ContentPickerDismissed)
            },
            onMediaTypeSelected = { type ->
                onAction(SnowbirdDashboardAction.MediaPicked(type))
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Get navigation bar insets for edge-to-edge support
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

        // Use a Column for content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = navigationBarPadding.calculateBottomPadding() + 16.dp),
        ) {

            // Header texts
            SpaceAuthHeader(
                description = "Preserve your media on the decentralized web (DWeb) Storage.",
                imagePainter = painterResource(R.drawable.ic_dweb),
                modifier = Modifier
                    .padding(vertical = 48.dp)
                    .padding(end = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // WebDav option
            DwebOptionItem(
                title = "Join group",
                subtitle = "Connect to existing group",
                onClick = { onAction(SnowbirdDashboardAction.JoinGroupClick) }
            )

            DwebOptionItem(
                title = "Create group",
                subtitle = "Create a new group via Dweb",
                onClick = { onAction(SnowbirdDashboardAction.CreateGroupClick) }
            )

            DwebOptionItem(
                title = "My groups",
                subtitle = "View and manage your groups",
                onClick = { onAction(SnowbirdDashboardAction.MyGroupsClick) }
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            // Server Control Section at the bottom using custom preference style
            DwebServerPreference(
                serverStatus = state.serverStatus,
                onToggle = { enabled -> onAction(SnowbirdDashboardAction.ToggleServer(enabled)) }
            )

            // Native Loading Overlay
            if (state.isLoading) {
                LoadingOverlay()
            }

        }
    }
}

@Composable
fun DwebServerPreference(
    serverStatus: ServiceStatus,
    onToggle: (Boolean) -> Unit
) {
    val isServerEnabled = serverStatus !is ServiceStatus.Stopped
    val isConnecting = serverStatus is ServiceStatus.Connecting

    // Summary text based on status
    val summaryText = when (serverStatus) {
        is ServiceStatus.Stopped -> "Enable to share and sync media"
        is ServiceStatus.Connecting -> "Connecting..."
        is ServiceStatus.Connected -> "Running on localhost:8080"
        is ServiceStatus.Failed -> "Failed to start. Try again."
    }

    // Custom preference-style UI
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isServerEnabled,
                enabled = !isConnecting,
                role = Role.Switch,
                onValueChange = onToggle
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title and Summary
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "DWeb Server",
                style = SaveTextStyles.bodyLarge,
                color = if (!isConnecting) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = summaryText,
                style = SaveTextStyles.bodySmallEmphasis,
                color = if (!isConnecting) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Switch with custom colors
        Switch(
            checked = isServerEnabled,
            onCheckedChange = null, // Handled by toggleable modifier
            enabled = !isConnecting,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@PreviewLight
@Composable
private fun SnowbirdDashboardContentPreview() {
    DefaultScaffoldPreview {
        SnowbirdDashboardContent(
            state = SnowbirdDashboardState(
                isLoading = false,
                serverStatus = ServiceStatus.Stopped
            ),
            onAction = {},
        )
    }
}

@Composable
fun DwebOptionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    // You can customize this look to match your original design
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.onBackground),
        shape = RoundedCornerShape(8.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {


            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .align(Alignment.Top)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )

                Text(
                    text = subtitle,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                )
            }

            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                painter = painterResource(R.drawable.ic_arrow_forward_ios),
                contentDescription = null,
            )
        }


    }
}


@Composable
fun SpaceAuthHeader(
    modifier: Modifier = Modifier,
    description: String = stringResource(id = R.string.internet_archive_description),
    imagePainter: Painter = painterResource(id = R.drawable.ic_internet_archive)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(ThemeColors.material.surfaceDim)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(30.dp),
                painter = imagePainter,
                contentDescription = "Space Image",
                colorFilter = ColorFilter.tint(colorResource(id = R.color.colorTertiary))
            )
        }

        Column(
            modifier = Modifier.padding(
                start = ThemeDimensions.spacing.medium,
                end = ThemeDimensions.spacing.xlarge
            )
        ) {
            Text(
                text = description,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ThemeColors.material.onSurfaceVariant,
            )
        }
    }
}

@PreviewLight
@Composable
private fun DwebServerPreferencePreview() {
    DefaultBoxPreview {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Stopped:", modifier = Modifier.padding(8.dp))
            DwebServerPreference(
                serverStatus = ServiceStatus.Stopped,
                onToggle = {}
            )

            HorizontalDivider()

            Text("Connecting:", modifier = Modifier.padding(8.dp))
            DwebServerPreference(
                serverStatus = ServiceStatus.Connecting,
                onToggle = {}
            )

            HorizontalDivider()

            Text("Connected:", modifier = Modifier.padding(8.dp))
            DwebServerPreference(
                serverStatus = ServiceStatus.Connected,
                onToggle = {}
            )

            HorizontalDivider()

            Text("Failed:", modifier = Modifier.padding(8.dp))
            DwebServerPreference(
                serverStatus = ServiceStatus.Failed(Throwable("Failed to start")),
                onToggle = {}
            )
        }
    }
}