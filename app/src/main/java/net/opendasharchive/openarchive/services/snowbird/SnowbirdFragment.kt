package net.opendasharchive.openarchive.services.snowbird

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.runtime.collectAsState
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
import androidx.navigation.fragment.findNavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.flow.collectLatest
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.SaveTextStyles
import net.opendasharchive.openarchive.core.presentation.theme.ThemeColors
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.extensions.getQueryParameter
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.main.QRScannerActivity
import net.opendasharchive.openarchive.services.snowbird.service.ServiceStatus
import net.opendasharchive.openarchive.services.snowbird.service.SnowbirdService
import org.koin.androidx.viewmodel.ext.android.viewModel

class SnowbirdFragment : BaseSnowbirdFragment() {

    private val snowbirdGroupViewModel: SnowbirdGroupViewModel by viewModel()

    private val qrCodeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val scanResult = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
        if (scanResult != null) {
            if (scanResult.contents != null) {
                processScannedData(scanResult.contents)
            }
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImageForQR(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val onJoinGroup: () -> Unit = {
            showQRScanOptions()
        }

        val onCreateGroup = {
            val action =
                SnowbirdFragmentDirections.actionFragmentSnowbirdToFragmentSnowbirdCreateGroup()
            findNavController().navigate(action)
        }

        val onMyGroups = {
            val action =
                SnowbirdFragmentDirections.actionFragmentSnowbirdToFragmentSnowbirdGroupList()
            findNavController().navigate(action)
        }

        return ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SaveAppTheme {

                    LaunchedEffect(Unit) {
                        snowbirdGroupViewModel.uiState.collectLatest { state ->
                            handleGroupStateUpdate(state)
                        }
                    }

                    // Observe QR Scanner result
                    val navController = findNavController()
                    navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>(SnowbirdQRScannerFragment.QR_RESULT_KEY)
                        ?.observe(viewLifecycleOwner) { result ->
                            if (result != null) {
                                processScannedData(result)
                                navController.currentBackStackEntry?.savedStateHandle?.remove<String>(SnowbirdQRScannerFragment.QR_RESULT_KEY)
                            }
                        }

                    SnowbirdScreen(
                        onJoinGroup = onJoinGroup,
                        onCreateGroup = onCreateGroup,
                        onMyGroups = onMyGroups,
                        onServerToggle = { enabled ->
                            if (enabled) {
                                requireContext().startForegroundService(Intent(requireContext(), SnowbirdService::class.java))
                            } else {
                                requireContext().stopService(Intent(requireContext(), SnowbirdService::class.java))
                            }
                        }
                    )
                }
            }
        }
    }

    private fun handleGroupStateUpdate(state: SnowbirdGroupState) {
        handleLoadingStatus(state.isLoading)
        AppLogger.d("group state = $state")
        state.error?.let { error ->
            handleError(error)
        }
    }

    private fun showQRScanOptions() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Info
            title = UiText.Dynamic("Scan QR Code")
            message = UiText.Dynamic("Choose how you want to scan the QR code")
            positiveButton {
                text = UiText.Dynamic("Camera")
                action = { startQRScanner() }
            }
            neutralButton {
                text = UiText.Dynamic("Gallery")
                action = { startImagePicker() }
            }
        }
    }

    private fun startImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun startQRScanner() {
        val action = SnowbirdFragmentDirections.actionFragmentSnowbirdToSnowbirdQrScanner()
        findNavController().navigate(action)
    }

    private fun processImageForQR(imageUri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val qrContent = decodeQRFromBitmap(bitmap)
                if (qrContent != null) {
                    processScannedData(qrContent)
                } else {
                    showQRNotFoundDialog()
                }
            } else {
                showQRNotFoundDialog()
            }
        } catch (e: Exception) {
            AppLogger.e("Error processing image for QR: ${e.message}")
            showQRNotFoundDialog()
        }
    }

    private fun decodeQRFromBitmap(bitmap: Bitmap): String? {
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        
        val reader = MultiFormatReader()
        val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))
        reader.setHints(hints)
        
        return try {
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            null
        }
    }

    private fun showQRNotFoundDialog() {
        dialogManager.showDialog(dialogManager.requireResourceProvider()) {
            type = DialogType.Warning
            title = UiText.Dynamic("No QR Code Found")
            message = UiText.Dynamic("Could not find a valid QR code in the selected image. Please try another image.")
            positiveButton {
                text = UiText.Resource(R.string.lbl_ok)
            }
        }
    }

    private fun processScannedData(uriString: String) {
        val name = uriString.getQueryParameter("name")

        if (name == null) {
            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Warning
                title = UiText.Dynamic("Oops!")
                message = UiText.Dynamic("Unable to determine group name from QR code.")
                positiveButton {
                    text = UiText.Resource(R.string.lbl_ok)
                }
            }
            return
        }


        val action = SnowbirdFragmentDirections
            .actionFragmentSnowbirdToFragmentSnowbirdJoinGroup(dwebGroupKey = uriString)
        findNavController().navigate(action)

    }

    override fun getToolbarTitle(): String {
        return "DWeb Storage"
    }
}

@Composable
fun SnowbirdScreen(
    onJoinGroup: () -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onMyGroups: () -> Unit = {},
    onServerToggle: (Boolean) -> Unit = {}
) {
    // Observe server status
    val serverStatus by SnowbirdService.serviceStatus.collectAsState()

    // Get navigation bar insets for edge-to-edge support
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // Use a scrollable Column to mimic ScrollView + LinearLayout
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
            onClick = onJoinGroup
        )

        DwebOptionItem(
            title = "Create group",
            subtitle = "Create a new group via Dweb",
            onClick = onCreateGroup
        )

        DwebOptionItem(
            title = "My groups",
            subtitle = "View and manage your groups",
            onClick = onMyGroups
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
            serverStatus = serverStatus,
            onToggle = onServerToggle
        )

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
private fun SnowbirdScreenPreview() {
    DefaultScaffoldPreview {
        SnowbirdScreen()
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
                .background(ThemeColors.material.surfaceDim,)
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
            modifier = Modifier.padding(start = ThemeDimensions.spacing.medium, end = ThemeDimensions.spacing.xlarge)
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
    SaveAppTheme {
        SpaceAuthHeader()
    }
}

@Preview
@Composable
private fun DwebServerPreferencePreview() {
    SaveAppTheme {
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