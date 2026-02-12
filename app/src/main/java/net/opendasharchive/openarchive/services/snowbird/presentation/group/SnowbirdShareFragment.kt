package net.opendasharchive.openarchive.services.snowbird.presentation.group

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.extensions.asQRCode
import net.opendasharchive.openarchive.services.snowbird.presentation.base.BaseSnowbirdFragment
import net.opendasharchive.openarchive.util.ShareUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

class SnowbirdShareFragment: BaseSnowbirdFragment() {

    private val viewModel: SnowbirdShareViewModel by viewModel()
    private val args: SnowbirdShareFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SnowbirdShareScreen(
                    groupKey = args.dwebGroupKey,
                    onCancel = { activity?.onBackPressedDispatcher?.onBackPressed() },
                    onShareQr = { qrContent, groupName ->
                        shareQrExternally(qrContent, groupName)
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_share, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_share -> {
                        viewModel.onAction(SnowbirdShareAction.ShareQrImage)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun shareQrExternally(qrContent: String, groupName: String) {
        val bitmap = qrContent.asQRCode(size = 1024)
        val uri = ShareUtils.saveBitmapToCache(requireContext(), bitmap, "snowbird_qr_${System.currentTimeMillis()}.png")
        
        if (uri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Join my Snowbird Group: $groupName")
                putExtra(Intent.EXTRA_TEXT, "Scan this QR code to join my Snowbird group '$groupName' in Open Archive.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Group QR Code"))
        }
    }

    override fun getToolbarTitle(): String {
        return "Share DWeb Storage Group"
    }
}