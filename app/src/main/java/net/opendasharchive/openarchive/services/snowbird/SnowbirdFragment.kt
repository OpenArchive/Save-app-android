package net.opendasharchive.openarchive.services.snowbird

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.fragment.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.core.presentation.theme.DefaultScaffoldPreview
import net.opendasharchive.openarchive.core.presentation.theme.SaveAppTheme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeColors
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.extensions.getQueryParameter
import net.opendasharchive.openarchive.features.core.BaseFragment
import net.opendasharchive.openarchive.features.core.UiText
import net.opendasharchive.openarchive.features.core.dialog.DialogType
import net.opendasharchive.openarchive.features.core.dialog.showDialog
import net.opendasharchive.openarchive.features.main.QRScannerActivity

class SnowbirdFragment : BaseFragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val onJoinGroup: () -> Unit = {
            startQRScanner()
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
                        snowbirdGroupViewModel.groupState.collect { state ->
                            handleGroupStateUpdate(
                                state
                            )
                        }
                    }

                    SnowbirdScreen(
                        onJoinGroup = onJoinGroup,
                        onCreateGroup = onCreateGroup,
                        onMyGroups = onMyGroups
                    )
                }
            }
        }
    }

    private fun handleGroupStateUpdate(state: SnowbirdGroupViewModel.GroupState) {
        handleLoadingStatus(false)
        AppLogger.d("group state = $state")
        when (state) {
            is SnowbirdGroupViewModel.GroupState.Loading -> handleLoadingStatus(true)
            is SnowbirdGroupViewModel.GroupState.Error -> handleError(state.error)
            else -> Unit
        }
    }

    private fun startQRScanner() {
        val integrator = IntentIntegrator(requireActivity())
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan QR Code")
        integrator.setCameraId(0)  // Use the rear camera
        integrator.setBeepEnabled(false)
        integrator.setBarcodeImageEnabled(true)
        integrator.setCaptureActivity(QRScannerActivity::class.java)

        val scanningIntent = integrator.createScanIntent()

        qrCodeLauncher.launch(scanningIntent)
    }

    private fun processScannedData(uriString: String) {
        val name = uriString.getQueryParameter("name")

        if (name == null) {
            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Warning
                title = UiText.DynamicString("Oops!")
                message = UiText.DynamicString("Unable to determine group name from QR code.")
                positiveButton {
                    text = UiText.StringResource(R.string.lbl_ok)
                }
            }
            return
        }

        if (SnowbirdGroup.exists(name)) {
            dialogManager.showDialog(dialogManager.requireResourceProvider()) {
                type = DialogType.Warning
                title = UiText.DynamicString("Oops!")
                message = UiText.DynamicString("You have already joined this group.")
                positiveButton {
                    text = UiText.StringResource(R.string.lbl_ok)
                }
            }
            return
        }

        val action = SnowbirdFragmentDirections
            .actionFragmentSnowbirdToFragmentSnowbirdJoinGroup(dwebGroupKey = uriString)
        findNavController().navigate(action)

    }

    override fun getToolbarTitle(): String {
        return "DWeb Service"
    }
}

@Composable
fun SnowbirdScreen(
    onJoinGroup: () -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onMyGroups: () -> Unit = {}
) {
    // Use a scrollable Column to mimic ScrollView + LinearLayout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp, bottom = 16.dp)
            .padding(horizontal = 24.dp),
    ) {


        // Header texts
        SpaceAuthHeader(
            description = "Preserve your media on the decentralized web (DWeb).",
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
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
private fun SpaceAuthHeaderPreview() {
    SaveAppTheme {
        SpaceAuthHeader()
    }
}