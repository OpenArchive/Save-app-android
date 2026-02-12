package net.opendasharchive.openarchive.services.snowbird.presentation.dashboard

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.components.LoadingOverlay
import net.opendasharchive.openarchive.core.presentation.theme.DefaultBoxPreview
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles
import net.opendasharchive.openarchive.core.presentation.theme.ThemeColors
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerSheet
import net.opendasharchive.openarchive.services.snowbird.presentation.base.BaseSnowbirdFragment
import net.opendasharchive.openarchive.services.snowbird.presentation.group.SnowbirdQRScannerFragment
import net.opendasharchive.openarchive.services.snowbird.service.ServiceStatus
import net.opendasharchive.openarchive.services.snowbird.service.SnowbirdService
import org.koin.androidx.viewmodel.ext.android.viewModel

class SnowbirdFragment : BaseSnowbirdFragment() {

    private val viewModel: SnowbirdDashboardViewModel by viewModel()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onAction(
                SnowbirdDashboardAction.ImagePickedForQR(
                    it,
                    requireContext()
                )
            )
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onAction(
                SnowbirdDashboardAction.ImagePickedForQR(
                    it,
                    requireContext()
                )
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SaveAppTheme {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        viewModel.events.collectLatest { event ->
                            handleDashboardEvent(event)
                        }
                    }

                    // Observe QR Scanner result from navigation
                    val navController = findNavController()
                    navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>(
                        SnowbirdQRScannerFragment.QR_RESULT_KEY
                    )
                        ?.observe(viewLifecycleOwner) { result ->
                            if (result != null) {
                                viewModel.onAction(SnowbirdDashboardAction.QRResultScanned(result))
                                navController.currentBackStackEntry?.savedStateHandle?.remove<String>(
                                    SnowbirdQRScannerFragment.QR_RESULT_KEY
                                )
                            }
                        }

                    SnowbirdDashboardScreen(
                        state = state,
                        onAction = viewModel::onAction
                    )

                    if (state.showContentPicker) {
                        ContentPickerSheet(
                            title = "Scan QR Code",
                            onDismiss = { viewModel.onAction(SnowbirdDashboardAction.ContentPickerDismissed) },
                            onMediaPicked = { type ->
                                when (type) {
                                    AddMediaType.GALLERY -> {
                                        imagePickerLauncher.launch("image/*")
                                        viewModel.onAction(SnowbirdDashboardAction.ContentPickerDismissed)
                                    }

                                    AddMediaType.FILES -> {
                                        filePickerLauncher.launch(arrayOf("image/*"))
                                        viewModel.onAction(SnowbirdDashboardAction.ContentPickerDismissed)
                                    }

                                    AddMediaType.CAMERA -> {
                                        // Use our dedicated QR Scanner for camera
                                        viewModel.onAction(SnowbirdDashboardAction.MediaPicked(type))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun handleDashboardEvent(event: SnowbirdDashboardEvent) {
        when (event) {
            is SnowbirdDashboardEvent.NavigateToCreateGroup -> {
                val action =
                    SnowbirdFragmentDirections.Companion.actionFragmentSnowbirdToFragmentSnowbirdCreateGroup()
                findNavController().navigate(action)
            }

            is SnowbirdDashboardEvent.NavigateToGroupList -> {
                val action =
                    SnowbirdFragmentDirections.Companion.actionFragmentSnowbirdToFragmentSnowbirdGroupList()
                findNavController().navigate(action)
            }

            is SnowbirdDashboardEvent.NavigateToJoinGroup -> {
                val action =
                    SnowbirdFragmentDirections.Companion.actionFragmentSnowbirdToFragmentSnowbirdJoinGroup(
                        dwebGroupKey = event.groupKey
                    )
                findNavController().navigate(action)
            }

            is SnowbirdDashboardEvent.NavigateToScanner -> {
                val action = SnowbirdFragmentDirections.Companion.actionFragmentSnowbirdToSnowbirdQrScanner()
                findNavController().navigate(action)
            }

            is SnowbirdDashboardEvent.ShowMessage -> Unit
            is SnowbirdDashboardEvent.ToggleServer -> {
                if (event.enabled) {
                    requireContext().startForegroundService(
                        Intent(
                            requireContext(),
                            SnowbirdService::class.java
                        )
                    )
                } else {
                    requireContext().stopService(
                        Intent(
                            requireContext(),
                            SnowbirdService::class.java
                        )
                    )
                }
            }
        }
    }

    override fun getToolbarTitle(): String = "DWeb Storage"
}

@Composable
fun SnowbirdDashboardScreen(
    state: SnowbirdDashboardState,
    onAction: (SnowbirdDashboardAction) -> Unit
) {
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

@Preview
@Composable
private fun SnowbirdDashboardScreenPreview() {
    DefaultScaffoldPreview {
        SnowbirdDashboardScreen(
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
                colorFilter = tint(colorResource(id = R.color.colorTertiary))
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

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SpaceAuthHeaderPreview() {
    DefaultBoxPreview {
        SpaceAuthHeader()
    }
}

@Preview
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